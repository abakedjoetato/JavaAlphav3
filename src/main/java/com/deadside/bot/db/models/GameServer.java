package com.deadside.bot.db.models;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

/**
 * Database model for a Deadside game server
 */
public class GameServer {
    @BsonId
    private ObjectId id;
    private long guildId;
    private String name;
    private String host;
    private int port;
    private String username;
    private String password;
    private int gameServerId;
    private long killfeedChannelId;
    private long logChannelId;
    private String lastProcessedKillfeedFile;
    private long lastProcessedKillfeedLine;
    private String lastProcessedLogFile;
    private long lastProcessedLogLine;
    private long lastProcessedTimestamp;
    
    public GameServer() {
        // Required for MongoDB POJO codec
    }
    
    public GameServer(String name, String host, int port, String username, String password, long guildId) {
        this.guildId = guildId;
        this.name = name;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.gameServerId = 1; // Default to 1
        this.killfeedChannelId = 0;
        this.logChannelId = 0;
        this.lastProcessedKillfeedFile = "";
        this.lastProcessedKillfeedLine = 0;
        this.lastProcessedLogFile = "";
        this.lastProcessedLogLine = 0;
        this.lastProcessedTimestamp = System.currentTimeMillis();
    }
    
    public GameServer(long guildId, String name, String host, int port, String username, String password, 
                      int gameServerId) {
        this.guildId = guildId;
        this.name = name;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.gameServerId = gameServerId;
        this.killfeedChannelId = 0; // Default to 0, set by separate command
        this.logChannelId = 0; // Default to 0, can be set later
        this.lastProcessedKillfeedFile = "";
        this.lastProcessedKillfeedLine = 0;
        this.lastProcessedLogFile = "";
        this.lastProcessedLogLine = 0;
        this.lastProcessedTimestamp = System.currentTimeMillis();
    }
    
    public ObjectId getId() {
        return id;
    }
    
    public void setId(ObjectId id) {
        this.id = id;
    }
    
    public long getGuildId() {
        return guildId;
    }
    
    public void setGuildId(long guildId) {
        this.guildId = guildId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getDirectory() {
        // Automatically construct directory path based on host and game server ID
        return "./" + host + "_" + gameServerId;
    }
    
    public String getLogDirectory() {
        return getDirectory() + "/Logs";
    }
    
    public String getDeathlogsDirectory() {
        return getDirectory() + "/actual1/deathlogs";
    }
    
    public int getGameServerId() {
        return gameServerId;
    }
    
    public void setGameServerId(int gameServerId) {
        this.gameServerId = gameServerId;
    }
    
    public long getKillfeedChannelId() {
        return killfeedChannelId;
    }
    
    public void setKillfeedChannelId(long killfeedChannelId) {
        this.killfeedChannelId = killfeedChannelId;
    }
    
    public long getLogChannelId() {
        return logChannelId;
    }
    
    public void setLogChannelId(long logChannelId) {
        this.logChannelId = logChannelId;
    }
    
    public String getLastProcessedKillfeedFile() {
        return lastProcessedKillfeedFile;
    }
    
    public void setLastProcessedKillfeedFile(String lastProcessedKillfeedFile) {
        this.lastProcessedKillfeedFile = lastProcessedKillfeedFile;
    }
    
    public long getLastProcessedKillfeedLine() {
        return lastProcessedKillfeedLine;
    }
    
    public void setLastProcessedKillfeedLine(long lastProcessedKillfeedLine) {
        this.lastProcessedKillfeedLine = lastProcessedKillfeedLine;
    }
    
    public String getLastProcessedLogFile() {
        return lastProcessedLogFile;
    }
    
    public void setLastProcessedLogFile(String lastProcessedLogFile) {
        this.lastProcessedLogFile = lastProcessedLogFile;
    }
    
    public long getLastProcessedLogLine() {
        return lastProcessedLogLine;
    }
    
    public void setLastProcessedLogLine(long lastProcessedLogLine) {
        this.lastProcessedLogLine = lastProcessedLogLine;
    }
    
    public long getLastProcessedTimestamp() {
        return lastProcessedTimestamp;
    }
    
    public void setLastProcessedTimestamp(long lastProcessedTimestamp) {
        this.lastProcessedTimestamp = lastProcessedTimestamp;
    }
    
    public void updateKillfeedProgress(String file, long line) {
        this.lastProcessedKillfeedFile = file;
        this.lastProcessedKillfeedLine = line;
        this.lastProcessedTimestamp = System.currentTimeMillis();
    }
    
    public void updateLogProgress(String file, long line) {
        this.lastProcessedLogFile = file;
        this.lastProcessedLogLine = line;
        this.lastProcessedTimestamp = System.currentTimeMillis();
    }
    
    // Status related methods needed by StringSelectMenuListener
    
    /**
     * Check if the server is currently online
     */
    public boolean isOnline() {
        // This should ideally be determined by server status checks
        // For now, we'll use a simple heuristic: if updated in the last 5 minutes
        return System.currentTimeMillis() - lastProcessedTimestamp < 5 * 60 * 1000;
    }
    
    /**
     * Get the current player count
     */
    public int getPlayerCount() {
        // This should be updated by server status checks
        return 0; // Placeholder
    }
    
    /**
     * Get the maximum number of players allowed
     */
    public int getMaxPlayers() {
        // This should be fetched from server config
        return 64; // Default value
    }
    
    /**
     * Get the server uptime in seconds
     */
    public long getUptime() {
        // This should be updated by server status checks
        return 0; // Placeholder
    }
    
    /**
     * Check if event notifications are enabled for this server
     */
    public boolean isEventNotificationsEnabled() {
        // This could be a configuration option
        return this.logChannelId > 0; // If log channel is set, assume notifications are enabled
    }
    
    /**
     * Get the last updated timestamp
     */
    public long getLastUpdated() {
        return lastProcessedTimestamp;
    }
    
    /**
     * Check if killfeed is enabled for this server
     */
    public boolean isKillfeedEnabled() {
        return this.killfeedChannelId > 0;
    }
    
    /**
     * Check if join/leave notifications are enabled for this server
     */
    public boolean isJoinLeaveNotificationsEnabled() {
        return this.logChannelId > 0;
    }
}
