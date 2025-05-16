package com.deadside.bot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

/**
 * Interface for slash command implementations
 */
public interface Command {
    /**
     * Get the name of the command
     */
    String getName();
    
    /**
     * Get the description of the command
     */
    String getDescription();
    
    /**
     * Get the options for the command
     */
    List<OptionData> getOptions();
    
    /**
     * Execute the command
     */
    void execute(SlashCommandInteractionEvent event);
}