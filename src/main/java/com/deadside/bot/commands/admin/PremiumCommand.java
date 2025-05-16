package com.deadside.bot.commands.admin;

import com.deadside.bot.commands.ICommand;
import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.premium.PremiumManager;
import com.deadside.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;

/**
 * Command for managing premium features and subscriptions
 */
public class PremiumCommand implements ICommand {
    private static final Logger logger = LoggerFactory.getLogger(PremiumCommand.class);
    private final PremiumManager premiumManager;
    private final GameServerRepository serverRepository;
    private static final Color PREMIUM_COLOR = new Color(26, 188, 156); // Emerald green color for premium
    
    public PremiumCommand() {
        this.premiumManager = new PremiumManager();
        this.serverRepository = new GameServerRepository();
    }
    
    @Override
    public String getName() {
        return "premium";
    }
    
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        Guild guild = event.getGuild();
        
        if (guild == null || member == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Error", "This command can only be used in a server."))
                .setEphemeral(true)
                .queue();
            return;
        }
        
        // Check if user has admin permissions
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Permission Denied", 
                    "You need Administrator permission to manage premium features."))
                .setEphemeral(true)
                .queue();
            return;
        }
        
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Error", "Invalid subcommand."))
                .setEphemeral(true)
                .queue();
            return;
        }
        
        switch (subcommand) {
            case "status":
                handleStatusSubcommand(event, guild);
                break;
                
            case "enable":
                handleEnableSubcommand(event, guild);
                break;
                
            case "disable":
                handleDisableSubcommand(event, guild);
                break;
                
            case "verify":
                handleVerifySubcommand(event, guild, member);
                break;
                
            case "assign":
                handleAssignSubcommand(event, guild);
                break;
                
            case "unassign":
                handleUnassignSubcommand(event, guild);
                break;
                
            case "list":
                handleListSubcommand(event, guild);
                break;
                
            default:
                event.replyEmbeds(EmbedUtils.errorEmbed("Error", "Unknown subcommand."))
                    .setEphemeral(true)
                    .queue();
                break;
        }
    }
    
    /**
     * Handle the /premium status subcommand
     */
    private void handleStatusSubcommand(SlashCommandInteractionEvent event, Guild guild) {
        String statusDetails = premiumManager.getPremiumStatusDetails(guild.getIdLong());
        boolean hasPremium = premiumManager.hasPremium(guild.getIdLong());
        
        if (hasPremium) {
            event.replyEmbeds(EmbedUtils.customEmbed(
                    "✨ Premium Status",
                    "This server has **PREMIUM** features enabled!\n\n" + statusDetails + 
                    "\n\nPremium features include:\n" +
                    "• Advanced statistics and leaderboards\n" +
                    "• Faction system\n" +
                    "• Economy features\n" +
                    "• Real-time event notifications\n" +
                    "• Server monitoring and detailed logs",
                    PREMIUM_COLOR
                ))
                .queue();
        } else {
            event.replyEmbeds(EmbedUtils.customEmbed(
                    "Premium Status",
                    "This server is using the **FREE** tier.\n\n" + statusDetails + 
                    "\n\nOnly the basic killfeed feature is available. Upgrade to premium to unlock:\n" +
                    "• Advanced statistics and leaderboards\n" +
                    "• Faction system\n" +
                    "• Economy features\n" +
                    "• Real-time event notifications\n" +
                    "• Server monitoring and detailed logs\n\n" +
                    "Use `/premium verify` to check payment status or visit our website to purchase premium.",
                    new Color(189, 195, 199) // Light gray for free tier
                ))
                .queue();
        }
    }
    
    /**
     * Handle the /premium enable subcommand
     */
    private void handleEnableSubcommand(SlashCommandInteractionEvent event, Guild guild) {
        // Only bot owner can manually enable premium
        if (!isOwner(event.getUser().getIdLong())) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Permission Denied", 
                    "Only the bot owner can manually enable premium. Please use `/premium verify` to activate your premium subscription."))
                .setEphemeral(true)
                .queue();
            return;
        }
        
        OptionMapping durationOption = event.getOption("days");
        int days = durationOption != null ? durationOption.getAsInt() : 0;
        
        premiumManager.enablePremium(guild.getIdLong(), days);
        
        String durationText = days > 0 ? "for " + days + " days" : "with no expiration";
        event.replyEmbeds(EmbedUtils.customEmbed(
                "✨ Premium Enabled",
                "Premium features have been enabled for this server " + durationText + "!\n\n" +
                "All premium features are now available.",
                PREMIUM_COLOR
            ))
            .queue();
            
        logger.info("Premium manually enabled for guild ID: {} by user ID: {} for {} days", 
                guild.getId(), event.getUser().getId(), days);
    }
    
    /**
     * Handle the /premium disable subcommand
     */
    private void handleDisableSubcommand(SlashCommandInteractionEvent event, Guild guild) {
        // Only bot owner can manually disable premium
        if (!isOwner(event.getUser().getIdLong())) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Permission Denied", 
                    "Only the bot owner can manually disable premium."))
                .setEphemeral(true)
                .queue();
            return;
        }
        
        premiumManager.disablePremium(guild.getIdLong());
        
        event.replyEmbeds(EmbedUtils.customEmbed(
                "Premium Disabled",
                "Premium features have been disabled for this server.\n\n" +
                "Only the basic killfeed feature is now available.",
                new Color(189, 195, 199) // Light gray
            ))
            .queue();
            
        logger.info("Premium manually disabled for guild ID: {} by user ID: {}", 
                guild.getId(), event.getUser().getId());
    }
    
    /**
     * Handle the /premium verify subcommand
     */
    private void handleVerifySubcommand(SlashCommandInteractionEvent event, Guild guild, Member member) {
        event.deferReply().queue(); // This might take a while, so defer the reply
        
        boolean verified = premiumManager.verifyTip4servPayment(guild.getIdLong(), member.getIdLong());
        
        if (verified) {
            event.getHook().sendMessageEmbeds(EmbedUtils.customEmbed(
                    "✨ Payment Verified",
                    "Your payment has been verified and premium features are now enabled for this server!\n\n" +
                    "All premium features are now available.",
                    PREMIUM_COLOR
                ))
                .queue();
                
            logger.info("Premium payment verified for guild ID: {} by user ID: {}", 
                    guild.getId(), member.getId());
        } else {
            event.getHook().sendMessageEmbeds(EmbedUtils.customEmbed(
                    "Payment Verification",
                    "No active payment was found for this server.\n\n" +
                    "If you recently purchased premium, it may take a few minutes to process. " +
                    "If the problem persists, please check you used the correct Discord account during checkout " +
                    "or contact support with your purchase confirmation.",
                    new Color(189, 195, 199) // Light gray
                ))
                .queue();
                
            logger.info("Premium payment verification failed for guild ID: {} by user ID: {}", 
                    guild.getId(), member.getId());
        }
    }
    
    /**
     * Handle the /premium assign subcommand
     * Allows assigning premium to a specific server
     */
    private void handleAssignSubcommand(SlashCommandInteractionEvent event, Guild guild) {
        String serverName = event.getOption("server", OptionMapping::getAsString);
        if (serverName == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Error", "You must specify a server name."))
                .setEphemeral(true)
                .queue();
            return;
        }
        
        // Check if the server exists
        GameServer server = serverRepository.findByGuildIdAndName(guild.getIdLong(), serverName);
        if (server == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Error", 
                    "Server '" + serverName + "' does not exist. Use `/server add` to create it first."))
                .setEphemeral(true)
                .queue();
            return;
        }
        
        // Check if the guild has premium slots available
        int premiumSlots = premiumManager.getAvailablePremiumSlots(guild.getIdLong());
        int usedSlots = premiumManager.countPremiumServers(guild.getIdLong());
        
        if (premiumSlots <= usedSlots && !premiumManager.hasGuildPremium(guild.getIdLong())) {
            event.replyEmbeds(EmbedUtils.errorEmbed("No Premium Slots Available", 
                    "You don't have any available premium slots. Purchase more premium slots or free up a slot by unassigning premium from another server."))
                .setEphemeral(true)
                .queue();
            return;
        }
        
        // The server already has premium
        if (server.isPremium()) {
            event.replyEmbeds(EmbedUtils.customEmbed("Already Premium", 
                    "The server '" + serverName + "' already has premium features enabled.",
                    PREMIUM_COLOR))
                .queue();
            return;
        }
        
        // Assign premium to this server
        boolean success = premiumManager.enableServerPremium(guild.getIdLong(), serverName, 30); // Default to 30 days
        
        if (success) {
            event.replyEmbeds(EmbedUtils.successEmbed("Premium Assigned", 
                    "✨ Successfully assigned premium to server '" + serverName + "'.\n\n" +
                    "This server now has access to all premium features!"))
                .queue();
            
            logger.info("Premium assigned to server '{}' in guild {}", serverName, guild.getId());
        } else {
            event.replyEmbeds(EmbedUtils.errorEmbed("Error", 
                    "Failed to assign premium to server '" + serverName + "'. Please try again later."))
                .setEphemeral(true)
                .queue();
        }
    }
    
    /**
     * Handle the /premium unassign subcommand
     * Allows removing premium from a specific server
     */
    private void handleUnassignSubcommand(SlashCommandInteractionEvent event, Guild guild) {
        String serverName = event.getOption("server", OptionMapping::getAsString);
        if (serverName == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Error", "You must specify a server name."))
                .setEphemeral(true)
                .queue();
            return;
        }
        
        // Check if the server exists
        GameServer server = serverRepository.findByGuildIdAndName(guild.getIdLong(), serverName);
        if (server == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Error", 
                    "Server '" + serverName + "' does not exist."))
                .setEphemeral(true)
                .queue();
            return;
        }
        
        // The server doesn't have premium
        if (!server.isPremium()) {
            event.replyEmbeds(EmbedUtils.customEmbed("Not Premium", 
                    "The server '" + serverName + "' doesn't have premium features enabled.",
                    new Color(189, 195, 199)))
                .queue();
            return;
        }
        
        // Check if it's a guild-wide premium
        if (premiumManager.hasGuildPremium(guild.getIdLong())) {
            event.replyEmbeds(EmbedUtils.customEmbed("Guild Premium", 
                    "This guild has guild-wide premium, which affects all servers. " +
                    "You need to disable guild premium before managing individual servers.",
                    PREMIUM_COLOR))
                .queue();
            return;
        }
        
        // Remove premium from this server
        boolean success = premiumManager.disableServerPremium(guild.getIdLong(), serverName);
        
        if (success) {
            event.replyEmbeds(EmbedUtils.successEmbed("Premium Unassigned", 
                    "Premium has been removed from server '" + serverName + "'.\n\n" +
                    "This server now has only basic (killfeed) features."))
                .queue();
            
            logger.info("Premium unassigned from server '{}' in guild {}", serverName, guild.getId());
        } else {
            event.replyEmbeds(EmbedUtils.errorEmbed("Error", 
                    "Failed to unassign premium from server '" + serverName + "'. Please try again later."))
                .setEphemeral(true)
                .queue();
        }
    }
    
    /**
     * Handle the /premium list subcommand
     * Lists all servers and their premium status
     */
    private void handleListSubcommand(SlashCommandInteractionEvent event, Guild guild) {
        List<GameServer> servers = serverRepository.findAllByGuildId(guild.getIdLong());
        
        if (servers.isEmpty()) {
            event.replyEmbeds(EmbedUtils.customEmbed("No Servers", 
                    "You don't have any game servers configured yet. Use `/server add` to add your first server.",
                    new Color(189, 195, 199)))
                .queue();
            return;
        }
        
        // Build the server list
        StringBuilder message = new StringBuilder();
        
        boolean guildHasPremium = premiumManager.hasGuildPremium(guild.getIdLong());
        int availableSlots = premiumManager.getAvailablePremiumSlots(guild.getIdLong());
        int usedSlots = 0;
        
        // Count premium servers
        for (GameServer server : servers) {
            if (server.isPremium()) {
                usedSlots++;
            }
        }
        
        if (guildHasPremium) {
            message.append("**✨ This guild has GUILD-WIDE PREMIUM ✨**\n");
            message.append("All servers automatically have premium features.\n\n");
        } else {
            message.append("**Premium Slots**: ").append(usedSlots).append("/").append(availableSlots).append("\n\n");
        }
        
        message.append("**Your Game Servers:**\n");
        
        for (GameServer server : servers) {
            String status;
            if (server.isPremium()) {
                status = "✨ **PREMIUM**";
                if (server.getPremiumUntil() > 0) {
                    long daysRemaining = (server.getPremiumUntil() - System.currentTimeMillis()) / (24L * 60L * 60L * 1000L);
                    status += " (expires in " + daysRemaining + " days)";
                }
            } else {
                status = "⚠️ **BASIC** (killfeed only)";
            }
            
            message.append("• **").append(server.getName()).append("** - ").append(status).append("\n");
        }
        
        if (!guildHasPremium && availableSlots > usedSlots) {
            message.append("\nYou have **").append(availableSlots - usedSlots)
                   .append("** unused premium slot(s). Use `/premium assign` to assign premium to a server.");
        } else if (!guildHasPremium && availableSlots <= usedSlots) {
            message.append("\nYou have used all your premium slots. To add more, purchase additional premium slots.");
        }
        
        event.replyEmbeds(EmbedUtils.customEmbed("Server Premium Status", message.toString(), 
                PREMIUM_COLOR))
            .queue();
    }
    
    @Override
    public CommandData getCommandData() {
        return Commands.slash("premium", "Manage premium features and subscription")
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
            .addSubcommands(
                new SubcommandData("status", "Check the premium status of this server"),
                new SubcommandData("list", "List all servers and their premium status"),
                new SubcommandData("assign", "Assign premium to a specific game server")
                    .addOption(OptionType.STRING, "server", "The name of the game server to assign premium to", true),
                new SubcommandData("unassign", "Remove premium from a specific game server")
                    .addOption(OptionType.STRING, "server", "The name of the game server to remove premium from", true),
                new SubcommandData("verify", "Verify a premium payment for this server"),
                new SubcommandData("enable", "Enable premium features for this server (Bot Owner Only)")
                    .addOption(OptionType.INTEGER, "days", "Duration in days (0 for unlimited)", false),
                new SubcommandData("disable", "Disable premium features for this server (Bot Owner Only)")
            );
    }
    
    /**
     * Check if a user is the bot owner
     */
    private boolean isOwner(long userId) {
        String ownerIdStr = System.getenv("BOT_OWNER_ID");
        if (ownerIdStr == null || ownerIdStr.isEmpty()) {
            return false;
        }
        
        try {
            long ownerId = Long.parseLong(ownerIdStr);
            return userId == ownerId;
        } catch (NumberFormatException e) {
            logger.error("Invalid bot owner ID format", e);
            return false;
        }
    }
}