package com.deadside.bot.db.models;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

/**
 * Database model for a kill record from the killfeed
 */
public class KillRecord {
    @BsonId
    private ObjectId id;
    private long guildId;
    private String serverId;
    private String killer;
    private String victim;
    private String weapon;
    private long distance;
    private long timestamp;
    private String originalLine;
    
    public KillRecord() {
        // Required for MongoDB POJO codec
    }
    
    public KillRecord(long guildId, String serverId, String killer, String victim, String weapon, 
                     long distance, long timestamp, String originalLine) {
        this.guildId = guildId;
        this.serverId = serverId;
        this.killer = killer;
        this.victim = victim;
        this.weapon = weapon;
        this.distance = distance;
        this.timestamp = timestamp;
        this.originalLine = originalLine;
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
    
    public String getServerId() {
        return serverId;
    }
    
    public void setServerId(String serverId) {
        this.serverId = serverId;
    }
    
    public String getKiller() {
        return killer;
    }
    
    public void setKiller(String killer) {
        this.killer = killer;
    }
    
    public String getVictim() {
        return victim;
    }
    
    public void setVictim(String victim) {
        this.victim = victim;
    }
    
    public String getWeapon() {
        return weapon;
    }
    
    public void setWeapon(String weapon) {
        this.weapon = weapon;
    }
    
    public long getDistance() {
        return distance;
    }
    
    public void setDistance(long distance) {
        this.distance = distance;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getOriginalLine() {
        return originalLine;
    }
    
    public void setOriginalLine(String originalLine) {
        this.originalLine = originalLine;
    }
    
    @Override
    public String toString() {
        return "KillRecord{" +
                "killer='" + killer + '\'' +
                ", victim='" + victim + '\'' +
                ", weapon='" + weapon + '\'' +
                ", distance=" + distance +
                ", timestamp=" + timestamp +
                '}';
    }
}
