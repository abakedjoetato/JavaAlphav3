package com.deadside.bot.commands.admin;

import com.deadside.bot.commands.ICommand;
import com.deadside.bot.config.Config;
import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.models.GuildConfig;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.db.repositories.GuildConfigRepository;
import com.deadside.bot.sftp.SftpManager;
import com.deadside.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command for managing game servers
 */
public class ServerCommand implements ICommand {
    private static final Logger logger = LoggerFactory.getLogger(ServerCommand.class);
    private final GameServerRepository serverRepository = new GameServerRepository();
    private final GuildConfigRepository guildConfigRepository = new GuildConfigRepository();
    private final SftpManager sftpManager = new SftpManager();
    private final com.deadside.bot.premium.PremiumManager premiumManager = new com.deadside.bot.premium.PremiumManager();
    
    @Override
    public String getName() {
        return "server";
    }
    
    @Override
    public CommandData getCommandData() {
        // Create option data for server name with autocomplete
        OptionData serverNameOption = new OptionData(OptionType.STRING, "name", "The name of the server", true)
                .setAutoComplete(true);
        
        return Commands.slash(getName(), "Manage Deadside game servers")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addSubcommands(
                        new SubcommandData("add", "Add a new game server")
                                .addOption(OptionType.STRING, "name", "The name of the server", true)
                                .addOption(OptionType.STRING, "host", "SFTP host address", true)
                                .addOption(OptionType.INTEGER, "port", "SFTP port", true)
                                .addOption(OptionType.STRING, "username", "SFTP username", true)
                                .addOption(OptionType.STRING, "password", "SFTP password", true)
                                .addOption(OptionType.INTEGER, "gameserver", "Game server ID", true),
                        new SubcommandData("remove", "Remove a game server")
                                .addOptions(serverNameOption),
                        new SubcommandData("list", "List all configured game servers"),
                        new SubcommandData("test", "Test SFTP connection to a server")
                                .addOptions(serverNameOption),
                        new SubcommandData("setkillfeed", "Set the killfeed channel for a server")
                                .addOptions(serverNameOption)
                                .addOption(OptionType.CHANNEL, "channel", "Channel for killfeed updates", true),
                        new SubcommandData("setlogs", "Set the server log channel for events")
                                .addOptions(serverNameOption)
                                .addOption(OptionType.CHANNEL, "channel", "Channel for server events and player join/leave logs", true)
                );
    }
    
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("This command can only be used in a server.").setEphemeral(true).queue();
            return;
        }
        
        Member member = event.getMember();
        if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("You need Administrator permission to use this command.").setEphemeral(true).queue();
            return;
        }
        
        String subCommand = event.getSubcommandName();
        if (subCommand == null) {
            event.reply("Invalid command usage.").setEphemeral(true).queue();
            return;
        }
        
        try {
            switch (subCommand) {
                case "add" -> addServer(event);
                case "remove" -> removeServer(event);
                case "list" -> listServers(event);
                case "test" -> testServerConnection(event);
                case "setkillfeed" -> setKillfeed(event);
                case "setlogs" -> setLogs(event);
                default -> event.reply("Unknown subcommand: " + subCommand).setEphemeral(true).queue();
            }
        } catch (Exception e) {
            logger.error("Error executing server command", e);
            event.reply("An error occurred: " + e.getMessage()).setEphemeral(true).queue();
        }
    }
    
    private void addServer(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        
        // Parse all options
        String name = event.getOption("name", OptionMapping::getAsString);
        String host = event.getOption("host", OptionMapping::getAsString);
        int port = event.getOption("port", OptionMapping::getAsInt); // Now required
        String username = event.getOption("username", OptionMapping::getAsString);
        String password = event.getOption("password", OptionMapping::getAsString);
        int gameServerId = event.getOption("gameserver", OptionMapping::getAsInt);
        
        // Acknowledge the command immediately
        event.deferReply(true).queue();
        
        // Check if guild config exists, create if not
        GuildConfig guildConfig = guildConfigRepository.findByGuildId(guild.getIdLong());
        if (guildConfig == null) {
            guildConfig = new GuildConfig(guild.getIdLong());
            guildConfigRepository.save(guildConfig);
        }
        
        // Check if server already exists
        if (serverRepository.findByGuildIdAndName(guild.getIdLong(), name) != null) {
            event.getHook().sendMessage("A server with this name already exists.").queue();
            return;
        }
        
        // Check if guild has a premium subscription that would cover all servers
        boolean guildHasPremium = premiumManager.hasGuildPremium(guild.getIdLong());
        
        // If guild doesn't have premium, check how many premium servers they already have
        if (!guildHasPremium) {
            // Count how many servers already have premium
            int premiumServerCount = premiumManager.countPremiumServers(guild.getIdLong());
            int existingServerCount = serverRepository.findAllByGuildId(guild.getIdLong()).size();
            
            // Allow one free server per guild (killfeed only)
            if (existingServerCount > 0 && premiumServerCount < existingServerCount) {
                event.getHook().sendMessageEmbeds(
                    EmbedUtils.errorEmbed("Premium Required", 
                            "You need to purchase premium for this server before adding it.\n\n" +
                            "Each additional server beyond the first requires its own premium subscription.\n" +
                            "Please use the `/premium purchase` command to get premium for this server.")
                ).queue();
                return;
            }
        }
        
        // Create new server
        GameServer gameServer = new GameServer(
                guild.getIdLong(),
                name,
                host,
                port,
                username,
                password,
                gameServerId
        );
        
        // Test the connection first
        try {
            boolean connectionResult = sftpManager.testConnection(gameServer);
            if (!connectionResult) {
                event.getHook().sendMessage("Failed to connect to the SFTP server. Please check your credentials and try again.").queue();
                return;
            }
            
            // If guild has premium, automatically mark this server as premium too
            if (guildHasPremium) {
                // Copy the premium duration from the guild to the server
                if (guildConfig.getPremiumUntil() > 0) {
                    gameServer.setPremium(true);
                    gameServer.setPremiumUntil(guildConfig.getPremiumUntil());
                } else {
                    // Guild has unlimited premium
                    gameServer.setPremium(true);
                    gameServer.setPremiumUntil(0);
                }
            } else if (serverRepository.findAllByGuildId(guild.getIdLong()).isEmpty()) {
                // This is the first server for this guild - it gets basic tier (killfeed only) for free
                gameServer.setPremium(false);
                gameServer.setPremiumUntil(0);
            }
            
            // Save the server if connection was successful
            serverRepository.save(gameServer);
            
            // Build success message
            StringBuilder successMessage = new StringBuilder();
            successMessage.append("Successfully added server **").append(name).append("**\n");
            successMessage.append("Host: ").append(host).append("\n");
            successMessage.append("Port: ").append(port).append("\n");
            successMessage.append("Game Server ID: ").append(gameServerId).append("\n\n");
            
            // Add premium status to the message
            if (gameServer.isPremium()) {
                successMessage.append("🌟 **PREMIUM SERVER** 🌟\n");
                if (gameServer.getPremiumUntil() > 0) {
                    long daysRemaining = (gameServer.getPremiumUntil() - System.currentTimeMillis()) / (24L * 60L * 60L * 1000L);
                    successMessage.append("Premium expires in ").append(daysRemaining).append(" days\n\n");
                } else {
                    successMessage.append("Premium never expires\n\n");
                }
            } else {
                successMessage.append("⚠️ **BASIC SERVER - KILLFEED ONLY** ⚠️\n");
                successMessage.append("Upgrade to premium to unlock all features: `/premium purchase`\n\n");
            }
            
            successMessage.append("You can set a killfeed channel with `/server setkillfeed ").append(name).append(" #channel`\n");
            successMessage.append("The bot will look for logs in: ").append(gameServer.getLogDirectory()).append("\n");
            successMessage.append("And deathlogs in: ").append(gameServer.getDeathlogsDirectory());
            
            event.getHook().sendMessageEmbeds(
                    EmbedUtils.successEmbed("Server Added", successMessage.toString())
            ).queue();
            
            logger.info("Added new game server '{}' for guild {}", name, guild.getId());
        } catch (Exception e) {
            logger.error("Error adding server", e);
            event.getHook().sendMessage("Error adding server: " + e.getMessage()).queue();
        }
    }
    
    private void setKillfeed(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        
        String serverName = event.getOption("name", OptionMapping::getAsString);
        TextChannel channel = event.getOption("channel", OptionMapping::getAsChannel).asTextChannel();
        
        // Look up the server
        GameServer server = serverRepository.findByGuildIdAndName(guild.getIdLong(), serverName);
        if (server == null) {
            event.reply("No server found with name: " + serverName).setEphemeral(true).queue();
            return;
        }
        
        // Update the killfeed channel
        server.setKillfeedChannelId(channel.getIdLong());
        serverRepository.save(server);
        
        event.reply("Killfeed channel for server **" + serverName + "** has been set to <#" + channel.getId() + ">.").queue();
        logger.info("Updated killfeed channel for server '{}' to {}", serverName, channel.getId());
    }
    
    private void removeServer(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        
        String name = event.getOption("name", OptionMapping::getAsString);
        
        // Look up the server
        GameServer server = serverRepository.findByGuildIdAndName(guild.getIdLong(), name);
        if (server == null) {
            event.reply("No server found with name: " + name).setEphemeral(true).queue();
            return;
        }
        
        // Remove the server
        serverRepository.delete(server);
        
        event.reply("Server **" + name + "** has been removed.").queue();
        logger.info("Removed game server '{}' from guild {}", name, guild.getId());
    }
    
    private void listServers(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        
        // Get all servers for this guild
        List<GameServer> servers = serverRepository.findAllByGuildId(guild.getIdLong());
        
        if (servers.isEmpty()) {
            event.reply("No game servers have been configured for this Discord server.").queue();
            return;
        }
        
        // Build server list
        StringBuilder description = new StringBuilder();
        for (GameServer server : servers) {
            description.append("**").append(server.getName()).append("**\n");
            description.append("Host: ").append(server.getHost()).append("\n");
            description.append("Killfeed Channel: <#").append(server.getKillfeedChannelId()).append(">\n\n");
        }
        
        event.replyEmbeds(
                EmbedUtils.infoEmbed("Configured Game Servers", description.toString())
        ).queue();
    }
    
    private void testServerConnection(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        
        String name = event.getOption("name", OptionMapping::getAsString);
        
        // Acknowledge the command immediately
        event.deferReply(true).queue();
        
        // Look up the server
        GameServer server = serverRepository.findByGuildIdAndName(guild.getIdLong(), name);
        if (server == null) {
            event.getHook().sendMessage("No server found with name: " + name).queue();
            return;
        }
        
        // Test the connection
        try {
            boolean result = sftpManager.testConnection(server);
            
            if (result) {
                event.getHook().sendMessageEmbeds(
                        EmbedUtils.successEmbed("Connection Successful", 
                                "Successfully connected to SFTP server **" + server.getName() + "**")
                ).queue();
            } else {
                event.getHook().sendMessageEmbeds(
                        EmbedUtils.errorEmbed("Connection Failed", 
                                "Failed to connect to SFTP server **" + server.getName() + "**\n" +
                                "Please check your credentials and try again.")
                ).queue();
            }
        } catch (Exception e) {
            logger.error("Error testing connection to server", e);
            event.getHook().sendMessageEmbeds(
                    EmbedUtils.errorEmbed("Connection Error", 
                            "Error testing connection to server: " + e.getMessage())
            ).queue();
        }
    }
    
    private void setLogs(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        
        String serverName = event.getOption("name", OptionMapping::getAsString);
        TextChannel channel = event.getOption("channel", OptionMapping::getAsChannel).asTextChannel();
        
        if (channel == null) {
            event.reply("Invalid channel specified.").setEphemeral(true).queue();
            return;
        }
        
        // Look up the server
        GameServer server = serverRepository.findByGuildIdAndName(guild.getIdLong(), serverName);
        if (server == null) {
            event.reply("No server found with name: " + serverName).setEphemeral(true).queue();
            return;
        }
        
        // Update the log channel
        server.setLogChannelId(channel.getIdLong());
        serverRepository.save(server);
        
        event.reply("Server log channel for **" + serverName + "** has been set to <#" + channel.getId() + ">. " +
                "You will now receive notifications for player joins/leaves and server events.").queue();
        logger.info("Updated log channel for server '{}' to {}", serverName, channel.getId());
    }
    
    @Override
    public List<Choice> handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) return List.of();
        
        String focusedOption = event.getFocusedOption().getName();
        
        // We only have autocomplete for server names
        if ("name".equals(focusedOption)) {
            String currentInput = event.getFocusedOption().getValue().toLowerCase();
            List<GameServer> servers = serverRepository.findAllByGuildId(guild.getIdLong());
            
            // Filter for servers that match the current input
            return servers.stream()
                .filter(server -> server.getName().toLowerCase().contains(currentInput))
                .map(server -> new Choice(server.getName(), server.getName()))
                .limit(25) // Discord has a max of 25 choices
                .collect(Collectors.toList());
        }
        
        return List.of(); // Empty list for no suggestions
    }
}
