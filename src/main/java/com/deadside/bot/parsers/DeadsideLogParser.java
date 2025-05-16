package com.deadside.bot.parsers;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.sftp.SftpConnector;
import com.deadside.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Deadside.log files
 * Monitors server logs for various events like player joins/leaves, missions, airdrops, etc.
 */
public class DeadsideLogParser {
    private static final Logger logger = LoggerFactory.getLogger(DeadsideLogParser.class);
    private final JDA jda;
    private final GameServerRepository serverRepository;
    private final SftpConnector sftpConnector;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    // Map to keep track of last processed line for each server
    private final Map<String, Integer> lastLineProcessed = new HashMap<>();
    
    // Regex patterns for different event types
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\[(\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2}:\\d{3})\\]\\[\\s*\\d+\\]");
    private static final Pattern PLAYER_JOIN_PATTERN = Pattern.compile("LogSFPS: \\[Login\\] Player (.+?) connected");
    private static final Pattern PLAYER_LEAVE_PATTERN = Pattern.compile("LogSFPS: \\[Logout\\] Player (.+?) disconnected");
    private static final Pattern PLAYER_KILLED_PATTERN = Pattern.compile("LogSFPS: \\[Kill\\] (.+?) killed (.+?) with (.+?) at distance (\\d+)");
    private static final Pattern PLAYER_DIED_PATTERN = Pattern.compile("LogSFPS: \\[Death\\] (.+?) died from (.+?)");
    private static final Pattern AIRDROP_PATTERN = Pattern.compile("LogSFPS: AirDrop switched to (\\w+)");
    private static final Pattern HELI_CRASH_PATTERN = Pattern.compile("LogSFPS: Helicopter crash spawned at position (.+)");
    private static final Pattern TRADER_EVENT_PATTERN = Pattern.compile("LogSFPS: Trader event started at (.+)");
    private static final Pattern MISSION_PATTERN = Pattern.compile("LogSFPS: Mission (.+?) switched to (\\w+)");
    
    // Log parsing interval in seconds
    private static final int LOG_PARSE_INTERVAL = 60; // 1 minute
    
    public DeadsideLogParser(JDA jda, GameServerRepository serverRepository, SftpConnector sftpConnector) {
        this.jda = jda;
        this.serverRepository = serverRepository;
        this.sftpConnector = sftpConnector;
    }
    
    /**
     * Start the log parser scheduler
     */
    public void startScheduler() {
        logger.info("Starting Deadside log parser scheduler (interval: {} seconds)", LOG_PARSE_INTERVAL);
        scheduler.scheduleAtFixedRate(this::processAllServerLogs, 0, LOG_PARSE_INTERVAL, TimeUnit.SECONDS);
    }
    
    /**
     * Stop the log parser scheduler
     */
    public void stopScheduler() {
        logger.info("Stopping Deadside log parser scheduler");
        scheduler.shutdown();
    }
    
    /**
     * Process logs for all servers in the database
     */
    public void processAllServerLogs() {
        try {
            List<GameServer> servers = serverRepository.findAll();
            
            for (GameServer server : servers) {
                try {
                    // Skip servers without log channel configured
                    if (server.getLogChannelId() == 0) {
                        continue;
                    }
                    
                    parseServerLog(server);
                } catch (Exception e) {
                    logger.error("Error parsing logs for server {}: {}", server.getName(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Error in log parser scheduler: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Parse the log file for a specific server
     */
    private void parseServerLog(GameServer server) {
        String logPath = getServerLogPath(server);
        
        try {
            // Get the last line processed for this server
            int lastLine = lastLineProcessed.getOrDefault(server.getName(), 0);
            
            // Read new lines from the log file
            List<String> newLines;
            
            try {
                // Read lines after the last processed line
                newLines = sftpConnector.readLinesAfter(server, logPath, lastLine);
                
                if (newLines.isEmpty()) {
                    return;
                }
                
                // Update last processed line
                lastLineProcessed.put(server.getName(), lastLine + newLines.size());
                
                // Process new lines
                processLogLines(server, newLines);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("No such file")) {
                    logger.warn("Log file not found for server {}: {}", server.getName(), logPath);
                } else if (e.getMessage() != null && e.getMessage().contains("smaller than expected")) {
                    // Log rotation detected, reset counter
                    logger.info("Log rotation detected for server {}, resetting line counter", server.getName());
                    lastLineProcessed.put(server.getName(), 0);
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            logger.error("Error reading log file for server {}: {}", server.getName(), e.getMessage(), e);
        }
    }
    
    /**
     * Process log lines and detect events
     */
    private void processLogLines(GameServer server, List<String> lines) {
        Set<String> joinedPlayers = new HashSet<>();
        Set<String> leftPlayers = new HashSet<>();
        
        for (String line : lines) {
            // Extract timestamp if present
            String timestamp = "";
            Matcher timestampMatcher = TIMESTAMP_PATTERN.matcher(line);
            if (timestampMatcher.find()) {
                timestamp = timestampMatcher.group(1);
            }
            
            // Player join events
            Matcher joinMatcher = PLAYER_JOIN_PATTERN.matcher(line);
            if (joinMatcher.find()) {
                String playerName = joinMatcher.group(1).trim();
                joinedPlayers.add(playerName);
                // Process individually for immediate notification
                sendPlayerJoinNotification(server, playerName, timestamp);
                continue;
            }
            
            // Player leave events
            Matcher leaveMatcher = PLAYER_LEAVE_PATTERN.matcher(line);
            if (leaveMatcher.find()) {
                String playerName = leaveMatcher.group(1).trim();
                leftPlayers.add(playerName);
                // Process individually for immediate notification
                sendPlayerLeaveNotification(server, playerName, timestamp);
                continue;
            }
            
            // Player killed events
            Matcher killedMatcher = PLAYER_KILLED_PATTERN.matcher(line);
            if (killedMatcher.find()) {
                String killer = killedMatcher.group(1).trim();
                String victim = killedMatcher.group(2).trim();
                String weapon = killedMatcher.group(3).trim();
                String distance = killedMatcher.group(4).trim();
                
                // Send kill notification
                sendKillNotification(server, killer, victim, weapon, distance, timestamp);
                continue;
            }
            
            // Player died events
            Matcher diedMatcher = PLAYER_DIED_PATTERN.matcher(line);
            if (diedMatcher.find()) {
                String player = diedMatcher.group(1).trim();
                String cause = diedMatcher.group(2).trim();
                
                // Send death notification
                sendDeathNotification(server, player, cause, timestamp);
                continue;
            }
            
            // Airdrop events
            Matcher airdropMatcher = AIRDROP_PATTERN.matcher(line);
            if (airdropMatcher.find()) {
                String status = airdropMatcher.group(1).trim();
                if (status.equalsIgnoreCase("Waiting")) {
                    // Airdrop is now available
                    sendEventNotification(server, "Airdrop Event", "An airdrop is inbound!", 
                            "Status: " + status, Color.BLUE, timestamp);
                } else if (status.equalsIgnoreCase("Dropped") || status.equalsIgnoreCase("Active")) {
                    // Airdrop has been deployed
                    sendEventNotification(server, "Airdrop Event", "An airdrop has been deployed!", 
                            "Status: " + status, Color.BLUE, timestamp);
                }
                continue;
            }
            
            // Helicopter crash events
            Matcher heliMatcher = HELI_CRASH_PATTERN.matcher(line);
            if (heliMatcher.find()) {
                String position = heliMatcher.group(1).trim();
                sendEventNotification(server, "Helicopter Crash", "A helicopter has crashed nearby!", 
                        "Location: " + position, new Color(150, 75, 0), timestamp); // Brown
                continue;
            }
            
            // Trader events
            Matcher traderMatcher = TRADER_EVENT_PATTERN.matcher(line);
            if (traderMatcher.find()) {
                String position = traderMatcher.group(1).trim();
                sendEventNotification(server, "Trader Event", "A special trader has appeared!", 
                        "Location: " + position, new Color(0, 128, 0), timestamp); // Green
                continue;
            }
            
            // Mission events
            Matcher missionMatcher = MISSION_PATTERN.matcher(line);
            if (missionMatcher.find()) {
                String missionName = missionMatcher.group(1).trim();
                String status = missionMatcher.group(2).trim();
                
                if (status.equalsIgnoreCase("READY") || status.equalsIgnoreCase("ACTIVE")) {
                    sendEventNotification(server, "Mission Available", "A new mission is active!", 
                            "Mission: " + missionName + "\nStatus: " + status, 
                            new Color(148, 0, 211), timestamp); // Purple
                }
            }
        }
        
        // Send summary if needed for multiple players
        if (joinedPlayers.size() > 3) {
            sendPlayerSummary(server, joinedPlayers, true);
        }
        
        if (leftPlayers.size() > 3) {
            sendPlayerSummary(server, leftPlayers, false);
        }
    }
    
    /**
     * Send notification for player kill
     */
    private void sendKillNotification(GameServer server, String killer, String victim, 
                                      String weapon, String distance, String timestamp) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Player Kill")
                .setDescription(killer + " killed " + victim)
                .setColor(Color.RED)
                .addField("Weapon", weapon, true)
                .addField("Distance", distance + "m", true)
                .setTimestamp(new Date().toInstant());
        
        if (!timestamp.isEmpty()) {
            embed.setFooter(timestamp + " • " + server.getName(), null);
        } else {
            embed.setFooter(server.getName(), null);
        }
        
        sendToLogChannel(server, embed.build());
    }
    
    /**
     * Send notification for player death (not kill)
     */
    private void sendDeathNotification(GameServer server, String player, String cause, String timestamp) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Player Death")
                .setDescription(player + " died from " + cause)
                .setColor(Color.GRAY)
                .setTimestamp(new Date().toInstant());
        
        if (!timestamp.isEmpty()) {
            embed.setFooter(timestamp + " • " + server.getName(), null);
        } else {
            embed.setFooter(server.getName(), null);
        }
        
        sendToLogChannel(server, embed.build());
    }
    
    /**
     * Send notification for player joining
     */
    private void sendPlayerJoinNotification(GameServer server, String playerName, String timestamp) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Player Connected")
                .setDescription(playerName + " has joined the server")
                .setColor(Color.GREEN)
                .setTimestamp(new Date().toInstant());
        
        if (!timestamp.isEmpty()) {
            embed.setFooter(timestamp + " • " + server.getName(), null);
        } else {
            embed.setFooter(server.getName(), null);
        }
        
        sendToLogChannel(server, embed.build());
    }
    
    /**
     * Send notification for player leaving
     */
    private void sendPlayerLeaveNotification(GameServer server, String playerName, String timestamp) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Player Disconnected")
                .setDescription(playerName + " has left the server")
                .setColor(Color.RED)
                .setTimestamp(new Date().toInstant());
        
        if (!timestamp.isEmpty()) {
            embed.setFooter(timestamp + " • " + server.getName(), null);
        } else {
            embed.setFooter(server.getName(), null);
        }
        
        sendToLogChannel(server, embed.build());
    }
    
    /**
     * Send summary for multiple player joins/leaves
     */
    private void sendPlayerSummary(GameServer server, Set<String> players, boolean joining) {
        String title = joining ? "Multiple Players Connected" : "Multiple Players Disconnected";
        Color color = joining ? Color.GREEN : Color.RED;
        
        StringBuilder desc = new StringBuilder();
        int count = 0;
        for (String player : players) {
            if (count < 10) { // Limit to 10 names to avoid too long messages
                desc.append("• ").append(player).append("\n");
                count++;
            } else {
                desc.append("• And ").append(players.size() - 10).append(" more players...");
                break;
            }
        }
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setDescription(desc.toString())
                .setColor(color)
                .setTimestamp(new Date().toInstant())
                .setFooter(server.getName(), null);
        
        sendToLogChannel(server, embed.build());
    }
    
    /**
     * Send notification for server events
     */
    private void sendEventNotification(GameServer server, String title, String description, 
                                       String details, Color color, String timestamp) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .setTimestamp(new Date().toInstant());
        
        if (details != null && !details.isEmpty()) {
            embed.addField("Details", details, false);
        }
        
        if (!timestamp.isEmpty()) {
            embed.setFooter(timestamp + " • " + server.getName(), null);
        } else {
            embed.setFooter(server.getName(), null);
        }
        
        sendToLogChannel(server, embed.build());
    }
    
    /**
     * Send embed message to the server's log channel
     */
    private void sendToLogChannel(GameServer server, net.dv8tion.jda.api.entities.MessageEmbed embed) {
        Guild guild = jda.getGuildById(server.getGuildId());
        if (guild == null) {
            logger.warn("Guild not found for server {}: {}", server.getName(), server.getGuildId());
            return;
        }
        
        TextChannel logChannel = guild.getTextChannelById(server.getLogChannelId());
        if (logChannel == null) {
            logger.warn("Log channel not found for server {}: {}", server.getName(), server.getLogChannelId());
            return;
        }
        
        logChannel.sendMessageEmbeds(embed).queue(
                success -> logger.debug("Sent log notification to channel {}", logChannel.getId()),
                error -> logger.error("Failed to send log notification: {}", error.getMessage())
        );
    }
    
    /**
     * Get the path to the server log file
     */
    private String getServerLogPath(GameServer server) {
        // Base path for Deadside server logs
        String basePath = server.getGameServerId() + "/Deadside/Saved/Logs/";
        return basePath + "Deadside.log";
    }
}