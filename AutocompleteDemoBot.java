import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AutocompleteDemoBot extends ListenerAdapter {
    
    private final Random random = new Random();
    
    public static void main(String[] args) {
        // Replace with your actual bot token from environment variable
        String token = System.getenv("DISCORD_TOKEN");
        
        if (token == null || token.isEmpty()) {
            System.out.println("Please set the DISCORD_TOKEN environment variable");
            System.exit(1);
        }
        
        // Create JDA instance
        JDA jda = JDABuilder.createDefault(token)
                .setActivity(Activity.playing("with autocomplete"))
                .addEventListeners(new AutocompleteDemoBot())
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .build();
        
        try {
            jda.awaitReady();
            
            // Register commands
            jda.updateCommands().addCommands(
                // Slot machine command with autocomplete
                Commands.slash("slot", "Play a slot machine game with bet amount")
                        .addOption(new OptionData(OptionType.INTEGER, "amount", "Amount to bet (10-1000)", true)
                                .setAutoComplete(true)),
                                
                // Blackjack command with autocomplete
                Commands.slash("blackjack", "Play a game of blackjack")
                        .addOption(new OptionData(OptionType.INTEGER, "bet", "Amount to bet (50-2000)", true)
                                .setAutoComplete(true)),
                                
                // Admin economy command with autocomplete
                Commands.slash("eco", "Admin economy management")
                        .addSubcommands(
                            Commands.subcommand("give", "Give coins to a player")
                                .addOption(OptionType.USER, "user", "The user to give coins to", true)
                                .addOption(new OptionData(OptionType.INTEGER, "amount", "Amount to give", true)
                                        .setAutoComplete(true)),
                            Commands.subcommand("take", "Take coins from a player")
                                .addOption(OptionType.USER, "user", "The user to take coins from", true)
                                .addOption(new OptionData(OptionType.INTEGER, "amount", "Amount to take", true)
                                        .setAutoComplete(true))
                        )
            ).queue();
            
            System.out.println("Bot is ready! Commands registered.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        
        switch (commandName) {
            case "slot":
                handleSlotCommand(event);
                break;
            case "blackjack":
                handleBlackjackCommand(event);
                break;
            case "eco":
                handleEcoCommand(event);
                break;
        }
    }
    
    private void handleSlotCommand(SlashCommandInteractionEvent event) {
        long amount = event.getOption("amount", 0L, OptionMapping::getAsLong);
        
        if (amount < 10 || amount > 1000) {
            event.reply("Invalid bet amount. Please bet between 10 and 1000 coins.").setEphemeral(true).queue();
            return;
        }
        
        // Simulate slot machine
        String[] symbols = {"🍒", "🍊", "🍋", "🍇", "🍉", "💎"};
        String result = symbols[random.nextInt(symbols.length)] + " " + 
                        symbols[random.nextInt(symbols.length)] + " " + 
                        symbols[random.nextInt(symbols.length)];
        
        // Random win/loss
        boolean win = random.nextInt(3) == 0;
        
        if (win) {
            event.reply("You bet " + amount + " coins and got: " + result + "\nYou won " + (amount * 2) + " coins!").queue();
        } else {
            event.reply("You bet " + amount + " coins and got: " + result + "\nYou lost " + amount + " coins.").queue();
        }
    }
    
    private void handleBlackjackCommand(SlashCommandInteractionEvent event) {
        long amount = event.getOption("bet", 0L, OptionMapping::getAsLong);
        
        if (amount < 50 || amount > 2000) {
            event.reply("Invalid bet amount. Please bet between 50 and 2000 coins.").setEphemeral(true).queue();
            return;
        }
        
        // Simulate blackjack game
        int playerHand = 10 + random.nextInt(11);
        int dealerHand = 10 + random.nextInt(11);
        
        // Random win/loss
        boolean win = playerHand > dealerHand;
        
        if (win) {
            event.reply("Blackjack! Your hand: " + playerHand + ", Dealer's hand: " + dealerHand + 
                       "\nYou won " + amount + " coins!").queue();
        } else {
            event.reply("Bust! Your hand: " + playerHand + ", Dealer's hand: " + dealerHand + 
                       "\nYou lost " + amount + " coins.").queue();
        }
    }
    
    private void handleEcoCommand(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        
        if (subcommand == null) {
            event.reply("No subcommand provided").setEphemeral(true).queue();
            return;
        }
        
        // Get user and amount from options
        OptionMapping userOption = event.getOption("user");
        long amount = event.getOption("amount", 0L, OptionMapping::getAsLong);
        
        if (userOption == null) {
            event.reply("No user specified").setEphemeral(true).queue();
            return;
        }
        
        if (amount <= 0) {
            event.reply("Amount must be greater than 0").setEphemeral(true).queue();
            return;
        }
        
        String targetUser = userOption.getAsUser().getAsMention();
        
        switch (subcommand) {
            case "give":
                event.reply("Successfully gave **" + amount + " coins** to " + targetUser).queue();
                break;
            case "take":
                event.reply("Successfully took **" + amount + " coins** from " + targetUser).queue();
                break;
            default:
                event.reply("Unknown subcommand: " + subcommand).setEphemeral(true).queue();
        }
    }
    
    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        String command = event.getName();
        String option = event.getFocusedOption().getName();
        
        if (option.equals("amount") || option.equals("bet")) {
            // Get the user's partial input
            String userInput = event.getFocusedOption().getValue();
            boolean hasUserInput = !userInput.isEmpty();
            long userValue = 0;
            
            if (hasUserInput) {
                try {
                    userValue = Long.parseLong(userInput);
                } catch (NumberFormatException e) {
                    hasUserInput = false;
                }
            }
            
            List<Choice> choices = new ArrayList<>();
            
            switch (command) {
                case "slot":
                    choices = getSlotSuggestions(hasUserInput, userValue);
                    break;
                case "blackjack":
                    choices = getBlackjackSuggestions(hasUserInput, userValue);
                    break;
                case "eco":
                    String subcommand = event.getSubcommandName();
                    if (subcommand != null) {
                        switch (subcommand) {
                            case "give":
                                choices = getEcoGiveSuggestions(hasUserInput, userValue);
                                break;
                            case "take":
                                choices = getEcoTakeSuggestions(hasUserInput, userValue);
                                break;
                        }
                    }
                    break;
            }
            
            event.replyChoices(choices).queue();
        }
    }
    
    private List<Choice> getSlotSuggestions(boolean hasUserInput, long userValue) {
        List<Choice> choices = new ArrayList<>();
        
        // If user entered a valid value, include it
        if (hasUserInput && userValue >= 10 && userValue <= 1000) {
            choices.add(new Choice(formatAmount(userValue) + " coins", userValue));
        }
        
        // Mock player's balance (in a real implementation, this would be fetched from the database)
        long playerBalance = 5000;
        long maxBet = Math.min(playerBalance, 1000);
        
        // Add max bet option
        if (maxBet >= 10) {
            if (maxBet == 1000) {
                choices.add(new Choice("Maximum bet (1,000)", 1000));
            } else {
                choices.add(new Choice("All in! (" + formatAmount(maxBet) + ")", maxBet));
            }
        }
        
        // Add common slot betting options
        long[] slotBets = {10, 20, 50, 100, 200, 500, 1000};
        for (long bet : slotBets) {
            if (bet <= maxBet && choices.size() < 25) {
                choices.add(new Choice(formatAmount(bet) + " coins", bet));
            }
        }
        
        return choices;
    }
    
    private List<Choice> getBlackjackSuggestions(boolean hasUserInput, long userValue) {
        List<Choice> choices = new ArrayList<>();
        
        // If user entered a valid value, include it
        if (hasUserInput && userValue >= 50 && userValue <= 2000) {
            choices.add(new Choice(formatAmount(userValue) + " coins", userValue));
        }
        
        // Mock player's balance
        long playerBalance = 10000;
        long maxBet = Math.min(playerBalance, 2000);
        
        // Add max bet option
        if (maxBet >= 50) {
            if (maxBet == 2000) {
                choices.add(new Choice("Maximum bet (2,000)", 2000));
            } else {
                choices.add(new Choice("All in! (" + formatAmount(maxBet) + ")", maxBet));
            }
        }
        
        // Add percentage-based options
        if (playerBalance >= 200) {
            choices.add(new Choice("Half balance (" + formatAmount(playerBalance / 2) + ")", 
                    Math.min(playerBalance / 2, 2000)));
            
            if (playerBalance >= 1000) {
                choices.add(new Choice("25% balance (" + formatAmount(playerBalance / 4) + ")", 
                        Math.min(playerBalance / 4, 2000)));
            }
        }
        
        // Add common blackjack betting options
        long[] blackjackBets = {50, 100, 200, 500, 1000, 1500, 2000};
        for (long bet : blackjackBets) {
            if (bet <= maxBet && bet >= 50 && choices.size() < 25) {
                choices.add(new Choice(formatAmount(bet) + " coins", bet));
            }
        }
        
        return choices;
    }
    
    private List<Choice> getEcoGiveSuggestions(boolean hasUserInput, long userValue) {
        List<Choice> choices = new ArrayList<>();
        
        // If user entered a valid value, include it
        if (hasUserInput && userValue > 0) {
            choices.add(new Choice(formatAmount(userValue) + " coins", userValue));
        }
        
        // Mock target player's balance
        long playerBalance = 2500;
        
        // Add balance-based suggestions
        choices.add(new Choice("Match balance: " + formatAmount(playerBalance), playerBalance));
        if (playerBalance >= 1000) {
            choices.add(new Choice("10% of balance: " + formatAmount(playerBalance / 10), playerBalance / 10));
            choices.add(new Choice("Double balance: " + formatAmount(playerBalance * 2), playerBalance * 2));
        }
        
        // Add standard admin amounts
        long[] commonAmounts = {100, 500, 1000, 5000, 10000, 25000, 50000};
        for (long amount : commonAmounts) {
            if (choices.size() < 25) {
                choices.add(new Choice(formatAmount(amount) + " coins", amount));
            }
        }
        
        return choices;
    }
    
    private List<Choice> getEcoTakeSuggestions(boolean hasUserInput, long userValue) {
        List<Choice> choices = new ArrayList<>();
        
        // If user entered a valid value, include it
        if (hasUserInput && userValue > 0) {
            choices.add(new Choice(formatAmount(userValue) + " coins", userValue));
        }
        
        // Mock target player's balance
        long playerBalance = 2500;
        
        // Add balance-based suggestions
        if (playerBalance > 0) {
            choices.add(new Choice("All coins: " + formatAmount(playerBalance), playerBalance));
            
            if (playerBalance >= 10) {
                choices.add(new Choice("Half: " + formatAmount(playerBalance / 2), playerBalance / 2));
            }
            if (playerBalance >= 100) {
                choices.add(new Choice("10%: " + formatAmount(playerBalance / 10), playerBalance / 10));
            }
        }
        
        // Add standard admin amounts
        long[] commonAmounts = {100, 500, 1000, 5000, 10000};
        for (long amount : commonAmounts) {
            if (choices.size() < 25) {
                choices.add(new Choice(formatAmount(amount) + " coins", amount));
            }
        }
        
        return choices;
    }
    
    private String formatAmount(long amount) {
        return String.format("%,d", amount);
    }
}