# Deadside Discord Bot

A robust Java-based Discord bot for Deadside game server management, offering comprehensive faction and player engagement tools with enhanced economic interactions.

## Features

- **Multi-Guild Support**: Support for multiple Discord servers (guilds)
- **Premium System**: Per-server premium features
- **SFTP Integration**: Pull server logs from user-provided game servers
- **Log & CSV Parsing**: Killfeed updates, historical data, and real-time log monitoring
- **Stat Engine**: Player and weapon statistics, leaderboards
- **Factions System**: Create and manage player factions
- **Economy**: In-bot currency system with gambling features

## Getting Started

### Prerequisites

- A Discord Bot Token from the [Discord Developer Portal](https://discord.com/developers/applications)

### Running the Bot

1. Click the "Run" button at the top of the Replit interface
2. The bot will attempt to start but will need your Discord token
3. Set your Discord token as an environment variable:
   - Go to the "Secrets" tab (lock icon) in the left sidebar
   - Add a new secret with the key `DISCORD_TOKEN` and your bot token as the value
4. Restart the bot by clicking the "Stop" and then "Run" button

## Bot Configuration

The bot is configured through environment variables and the `config.properties` file. 
The most important settings are:

- `DISCORD_TOKEN` (required): Your Discord bot token
- `BOT_OWNER_ID`: Your Discord user ID for admin access
- `BOT_HOME_GUILD_ID`: Main Discord server ID

## Troubleshooting

If you encounter any issues:
1. Check the console for error messages
2. Verify that your Discord token is correctly set
3. Make sure your bot has the necessary permissions in your Discord server

## Development

This bot is built with:
- Java 17
- JDA (Java Discord API) 5.0.0-beta.13
- MongoDB for data storage