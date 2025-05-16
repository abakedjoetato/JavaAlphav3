package com.deadside.bot.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;
import java.time.Instant;

/**
 * Utility class for creating themed embeds for the Deadside bot
 */
public class EmbedThemes {
    // Deadside themed color palette - emerald colors
    public static final Color PRIMARY_COLOR = new Color(0, 168, 107);      // Primary emerald green
    public static final Color SECONDARY_COLOR = new Color(14, 105, 72);    // Darker emerald
    public static final Color ACCENT_COLOR = new Color(108, 235, 184);     // Light emerald accent
    
    // Event colors
    public static final Color EVENT_COLOR = new Color(230, 126, 34);       // Orange for events
    public static final Color KILL_COLOR = new Color(231, 76, 60);         // Red for kills
    public static final Color DEATH_COLOR = new Color(149, 165, 166);      // Gray for deaths
    public static final Color JOIN_COLOR = new Color(46, 204, 113);        // Green for joins
    public static final Color LEAVE_COLOR = new Color(155, 89, 182);       // Purple for leaves
    
    // Status colors
    public static final Color SUCCESS_COLOR = new Color(39, 174, 96);      // Green for success
    public static final Color WARNING_COLOR = new Color(241, 196, 15);     // Yellow for warnings
    public static final Color ERROR_COLOR = new Color(192, 57, 43);        // Red for errors
    public static final Color INFO_COLOR = new Color(52, 152, 219);        // Blue for info
    
    // Deadside logo URL and icon URLs
    private static final String DEADSIDE_LOGO = "https://cdn.akamai.steamstatic.com/steam/apps/895400/header.jpg";
    private static final String SUCCESS_ICON = "https://cdn-icons-png.flaticon.com/512/5290/5290058.png";
    private static final String WARNING_ICON = "https://cdn-icons-png.flaticon.com/512/4201/4201973.png";
    private static final String ERROR_ICON = "https://cdn-icons-png.flaticon.com/512/1828/1828843.png";
    private static final String INFO_ICON = "https://cdn-icons-png.flaticon.com/512/471/471664.png";
    
    /**
     * Create a base themed embed with Deadside style
     */
    public static EmbedBuilder baseEmbed() {
        return new EmbedBuilder()
                .setColor(PRIMARY_COLOR)
                .setTimestamp(Instant.now())
                .setFooter("Deadside Bot", DEADSIDE_LOGO);
    }
    
    /**
     * Create an event embed (mission, airdrop, etc.)
     */
    public static MessageEmbed eventEmbed(String title, String description) {
        return baseEmbed()
                .setColor(EVENT_COLOR)
                .setTitle("🎮 " + title)
                .setDescription(description)
                .build();
    }
    
    /**
     * Create a kill event embed
     */
    public static MessageEmbed killEmbed(String killer, String victim, String weapon, int distance) {
        return baseEmbed()
                .setColor(KILL_COLOR)
                .setTitle("☠️ Kill Feed")
                .setDescription(String.format("**%s** killed **%s**\nWeapon: **%s**\nDistance: **%dm**", 
                        killer, victim, weapon, distance))
                .setThumbnail("https://cdn-icons-png.flaticon.com/512/4233/4233830.png")
                .build();
    }
    
    /**
     * Create a death event embed (suicide or environment death)
     */
    public static MessageEmbed deathEmbed(String victim, String cause) {
        return baseEmbed()
                .setColor(DEATH_COLOR)
                .setTitle("💀 Death Feed")
                .setDescription(String.format("**%s** died from **%s**", victim, cause))
                .setThumbnail("https://cdn-icons-png.flaticon.com/512/5509/5509405.png")
                .build();
    }
    
    /**
     * Create a player join embed
     */
    public static MessageEmbed joinEmbed(String playerName) {
        return baseEmbed()
                .setColor(JOIN_COLOR)
                .setTitle("🚶 Player Joined")
                .setDescription(String.format("**%s** joined the server", playerName))
                .setThumbnail("https://cdn-icons-png.flaticon.com/512/1828/1828471.png")
                .build();
    }
    
    /**
     * Create a player leave embed
     */
    public static MessageEmbed leaveEmbed(String playerName) {
        return baseEmbed()
                .setColor(LEAVE_COLOR)
                .setTitle("🚶‍♂️ Player Left")
                .setDescription(String.format("**%s** left the server", playerName))
                .setThumbnail("https://cdn-icons-png.flaticon.com/512/1828/1828470.png")
                .build();
    }
    
    /**
     * Create a success embed
     */
    public static MessageEmbed successEmbed(String title, String description) {
        return baseEmbed()
                .setColor(SUCCESS_COLOR)
                .setTitle("✅ " + title)
                .setDescription(description)
                .setThumbnail(SUCCESS_ICON)
                .build();
    }
    
    /**
     * Create a warning embed
     */
    public static MessageEmbed warningEmbed(String title, String description) {
        return baseEmbed()
                .setColor(WARNING_COLOR)
                .setTitle("⚠️ " + title)
                .setDescription(description)
                .setThumbnail(WARNING_ICON)
                .build();
    }
    
    /**
     * Create an error embed
     */
    public static MessageEmbed errorEmbed(String title, String description) {
        return baseEmbed()
                .setColor(ERROR_COLOR)
                .setTitle("❌ " + title)
                .setDescription(description)
                .setThumbnail(ERROR_ICON)
                .build();
    }
    
    /**
     * Create an info embed
     */
    public static MessageEmbed infoEmbed(String title, String description) {
        return baseEmbed()
                .setColor(INFO_COLOR)
                .setTitle("ℹ️ " + title)
                .setDescription(description)
                .setThumbnail(INFO_ICON)
                .build();
    }
    
    /**
     * Create a faction embed
     */
    public static EmbedBuilder factionEmbed() {
        return baseEmbed()
                .setColor(SECONDARY_COLOR);
    }
    
    /**
     * Create a stats embed
     */
    public static EmbedBuilder statsEmbed() {
        return baseEmbed()
                .setColor(ACCENT_COLOR);
    }
    
    /**
     * Create an economy embed
     */
    public static EmbedBuilder economyEmbed() {
        return baseEmbed()
                .setColor(new Color(241, 196, 15)); // Gold color for economy
    }
    
    /**
     * Create a premium embed
     */
    public static EmbedBuilder premiumEmbed() {
        return baseEmbed()
                .setColor(new Color(156, 89, 182)); // Purple color for premium
    }
    
    /**
     * Create a server embed
     */
    public static EmbedBuilder serverEmbed() {
        return baseEmbed()
                .setColor(new Color(52, 152, 219)); // Blue color for server info
    }
}