package com.deadside.bot.commands.stats;

import com.deadside.bot.commands.ICommand;
import com.deadside.bot.db.models.Player;
import com.deadside.bot.db.repositories.PlayerRepository;
import com.deadside.bot.premium.PremiumManager;
import com.deadside.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Command for checking a player's rank in various statistics
 */
public class RankCommand implements ICommand {
    private static final Logger logger = LoggerFactory.getLogger(RankCommand.class);
    private final PlayerRepository playerRepository = new PlayerRepository();
    private final PremiumManager premiumManager = new PremiumManager();
    private final DecimalFormat df = new DecimalFormat("#.##");
    
    @Override
    public String getName() {
        return "rank";
    }
    
    @Override
    public CommandData getCommandData() {
        return Commands.slash(getName(), "View a player's ranking in various statistics")
                .addOption(OptionType.STRING, "player", "In-game player name to check rank for", false)
                .addOption(OptionType.USER, "user", "Discord user to check rank for", false);
    }
    
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("This command can only be used in a server.").setEphemeral(true).queue();
            return;
        }
        
        // Check for premium if feature is restricted
        long guildId = event.getGuild().getIdLong();
        if (!premiumManager.hasPremium(guildId)) {
            event.reply("This command is only available with premium. The killfeed is available for free.").setEphemeral(true).queue();
            return;
        }
        
        String playerName = event.getOption("player", OptionMapping::getAsString);
        User targetUser = event.getOption("user", OptionMapping::getAsUser);
        
        // If neither option is provided, default to the command user
        if (playerName == null && targetUser == null) {
            targetUser = event.getUser();
        }
        
        event.deferReply().queue();
        
        try {
            Player player = null;
            
            if (playerName != null) {
                // Find player by in-game name
                List<Player> matchingPlayers = playerRepository.findByNameLike(playerName);
                if (!matchingPlayers.isEmpty()) {
                    // Try to find exact match first
                    for (Player p : matchingPlayers) {
                        if (p.getName().equalsIgnoreCase(playerName)) {
                            player = p;
                            break;
                        }
                    }
                    
                    // If no exact match, use first result
                    if (player == null) {
                        player = matchingPlayers.get(0);
                    }
                }
            } else if (targetUser != null) {
                // Find linked player by Discord user ID
                com.deadside.bot.db.repositories.LinkedPlayerRepository linkedPlayerRepo = 
                    new com.deadside.bot.db.repositories.LinkedPlayerRepository();
                com.deadside.bot.db.models.LinkedPlayer linkedPlayer = linkedPlayerRepo.findByDiscordId(targetUser.getIdLong());
                
                if (linkedPlayer != null) {
                    player = playerRepository.findByPlayerId(linkedPlayer.getMainPlayerId());
                }
            }
            
            if (player == null) {
                String errorMessage = playerName != null 
                    ? "No player found with name: " + playerName
                    : targetUser != null 
                        ? targetUser.getName() + " hasn't linked a Deadside account yet."
                        : "No player information found.";
                
                event.getHook().sendMessage(errorMessage).queue();
                return;
            }
            
            // Get all ranked players to compute ranks
            List<Player> allPlayers = playerRepository.getTopPlayersByKills(1000); // Get a lot of players
            
            // Calculate ranks and send embed
            event.getHook().sendMessageEmbeds(buildRankEmbed(player, allPlayers)).queue();
            
        } catch (Exception e) {
            logger.error("Error retrieving player rank", e);
            event.getHook().sendMessage("An error occurred while retrieving player rank.").queue();
        }
    }
    
    /**
     * Build the player rank embed with various stat rankings
     */
    private net.dv8tion.jda.api.entities.MessageEmbed buildRankEmbed(Player player, List<Player> allPlayers) {
        // Minimum threshold to be included in ranking
        final int MIN_KILLS = 5;
        
        // Filter players with minimum kills for ranking
        List<Player> rankablePlayers = allPlayers.stream()
                .filter(p -> p.getKills() >= MIN_KILLS)
                .collect(Collectors.toList());
        
        // Total number of ranked players
        int totalPlayers = rankablePlayers.size();
        
        // If player doesn't meet minimum threshold, still show stats but indicate not ranked
        boolean isRanked = player.getKills() >= MIN_KILLS;
        
        // Calculate kills rank
        int killsRank = isRanked ? calculateRank(player, rankablePlayers, 
                Comparator.comparingInt(Player::getKills).reversed()) : -1;
        
        // Calculate K/D rank (only for players with kills)
        int kdRank = isRanked ? calculateRank(player, rankablePlayers,
                Comparator.comparingDouble(Player::getKdRatio).reversed()) : -1;
        
        // Calculate score rank
        int scoreRank = isRanked ? calculateRank(player, rankablePlayers,
                Comparator.comparingInt(Player::getScore).reversed()) : -1;
                
        // Build embed description
        StringBuilder description = new StringBuilder();
        description.append("# ").append(player.getName()).append("'s Rankings\n\n");
        
        // Player stats summary
        description.append("## Player Stats\n");
        description.append("Kills: **").append(player.getKills()).append("**\n");
        description.append("Deaths: **").append(player.getDeaths()).append("**\n");
        description.append("K/D Ratio: **").append(df.format(player.getKdRatio())).append("**\n");
        description.append("Score: **").append(player.getScore()).append("**\n\n");
        
        // Rankings section
        description.append("## Rankings ");
        if (!isRanked) {
            description.append("(Needs ").append(MIN_KILLS).append("+ kills to be ranked)\n");
        } else {
            description.append("(Out of ").append(totalPlayers).append(" players)\n");
        }
        
        // Show ranking for each category
        if (isRanked) {
            description.append("Kills Rank: **#").append(killsRank).append("** (Top ")
                     .append(calculatePercentile(killsRank, totalPlayers)).append("%)\n");
                     
            description.append("K/D Ratio Rank: **#").append(kdRank).append("** (Top ")
                     .append(calculatePercentile(kdRank, totalPlayers)).append("%)\n");
                     
            description.append("Score Rank: **#").append(scoreRank).append("** (Top ")
                     .append(calculatePercentile(scoreRank, totalPlayers)).append("%)\n");
        } else {
            description.append("Not enough kills to be ranked yet. Get ").append(MIN_KILLS - player.getKills())
                     .append(" more kills to be included in rankings.\n");
        }
        
        // Weapon information
        if (player.getMostUsedWeapon() != null && !player.getMostUsedWeapon().isEmpty()) {
            description.append("\n## Weapon Stats\n");
            description.append("Favorite Weapon: **").append(player.getMostUsedWeapon()).append("** (")
                     .append(player.getMostUsedWeaponKills()).append(" kills)\n");
        }
        
        // Last updated timestamp
        description.append("\nLast updated: <t:").append(player.getLastUpdated() / 1000).append(":R>");
        
        return EmbedUtils.infoEmbed("Player Ranking", description.toString());
    }
    
    /**
     * Calculate the rank of a player among all players using the provided comparator
     */
    private int calculateRank(Player player, List<Player> allPlayers, Comparator<Player> comparator) {
        // Sort players by the given comparator
        List<Player> sortedPlayers = allPlayers.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
        
        // Find player's position (0-based index)
        for (int i = 0; i < sortedPlayers.size(); i++) {
            if (sortedPlayers.get(i).getPlayerId().equals(player.getPlayerId())) {
                return i + 1; // Convert to 1-based index for display
            }
        }
        
        return sortedPlayers.size() + 1; // If not found, place at bottom
    }
    
    /**
     * Calculate percentile (lower is better) based on rank and total count
     */
    private int calculatePercentile(int rank, int totalPlayers) {
        if (totalPlayers <= 0) return 100;
        return Math.max(1, Math.min(100, (int)((double)rank / totalPlayers * 100)));
    }
}