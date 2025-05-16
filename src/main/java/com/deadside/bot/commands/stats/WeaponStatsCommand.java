package com.deadside.bot.commands.stats;

import com.deadside.bot.commands.ICommand;
import com.deadside.bot.db.models.Player;
import com.deadside.bot.db.repositories.PlayerRepository;
import com.deadside.bot.premium.PremiumManager;
import com.deadside.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Command for viewing weapon-specific statistics
 */
public class WeaponStatsCommand implements ICommand {
    private static final Logger logger = LoggerFactory.getLogger(WeaponStatsCommand.class);
    private final PlayerRepository playerRepository = new PlayerRepository();
    private final PremiumManager premiumManager = new PremiumManager();
    
    @Override
    public String getName() {
        return "weapon";
    }
    
    @Override
    public CommandData getCommandData() {
        return Commands.slash(getName(), "View statistics for a specific weapon")
                .addOption(OptionType.STRING, "name", "The name of the weapon", true);
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
        
        String weaponName = event.getOption("name", OptionMapping::getAsString);
        if (weaponName == null || weaponName.isEmpty()) {
            event.reply("Please provide a weapon name.").setEphemeral(true).queue();
            return;
        }
        
        event.deferReply().queue();
        
        try {
            // Fetch top players for this weapon
            List<Player> players = playerRepository.getTopPlayersByWeapon(weaponName, 10);
            
            if (players.isEmpty()) {
                // If no exact match found, try to find weapons that contain the search string
                List<Player> allPlayers = playerRepository.getTopPlayersByKills(100); // Get a lot of players to search through
                Map<String, List<Player>> weaponGroups = new HashMap<>();
                
                // Group players by weapon
                for (Player player : allPlayers) {
                    String weapon = player.getMostUsedWeapon();
                    if (weapon != null && !weapon.isEmpty() && weapon.toLowerCase().contains(weaponName.toLowerCase())) {
                        weaponGroups.computeIfAbsent(weapon, k -> new ArrayList<>()).add(player);
                    }
                }
                
                if (weaponGroups.isEmpty()) {
                    event.getHook().sendMessage("No players found using weapon: " + weaponName).queue();
                    return;
                }
                
                // Find the closest matching weapon with the most players
                String closestWeapon = weaponGroups.entrySet().stream()
                        .max(Comparator.comparingInt(e -> e.getValue().size()))
                        .map(Map.Entry::getKey)
                        .orElse("");
                
                if (!closestWeapon.isEmpty()) {
                    // Sort the players for this weapon by kill count
                    players = weaponGroups.get(closestWeapon).stream()
                            .sorted(Comparator.comparingInt(Player::getMostUsedWeaponKills).reversed())
                            .limit(10)
                            .collect(Collectors.toList());
                    
                    weaponName = closestWeapon; // Update the weapon name for display
                }
            }
            
            if (players.isEmpty()) {
                event.getHook().sendMessage("No players found using weapon: " + weaponName).queue();
                return;
            }
            
            // Build the weapon stats embed
            StringBuilder description = new StringBuilder();
            description.append("**Top Players Using ").append(weaponName).append("**\n\n");
            
            for (int i = 0; i < players.size(); i++) {
                Player player = players.get(i);
                description.append("`").append(i + 1).append(".` **")
                        .append(player.getName()).append("** - ")
                        .append(player.getMostUsedWeaponKills()).append(" kills\n");
            }
            
            event.getHook().sendMessageEmbeds(
                    EmbedUtils.infoEmbed("Weapon Statistics: " + weaponName, description.toString())
            ).queue();
            
        } catch (Exception e) {
            logger.error("Error retrieving weapon stats", e);
            event.getHook().sendMessage("An error occurred while retrieving weapon statistics.").queue();
        }
    }
}