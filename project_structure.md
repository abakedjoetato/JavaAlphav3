# Deadside Discord Bot - Project Structure

## Overview
This is a Java-based multi-guild Discord bot for Deadside servers with SFTP integration, stats tracking, and premium features. The bot is built using JDA 5.x (Java Discord API).

## Main Components

### Core Components
- **Main**: Entry point for the application (`com.deadside.bot.Main`)
- **DeadsideBot**: Main bot implementation that handles initialization and connections (`com.deadside.bot.bot.DeadsideBot`)
- **Config**: Configuration manager that loads settings from config.properties and environment variables (`com.deadside.bot.config.Config`)

### Database
- **MongoDBConnection**: Singleton for MongoDB connection management (`com.deadside.bot.db.MongoDBConnection`)
- **Models**: Data models for various entities like GameServer, Player, etc. (`com.deadside.bot.db.models`)
- **Repositories**: Data access objects for CRUD operations (`com.deadside.bot.db.repositories`)

### SFTP Integration
- **SftpConnector**: Handles SFTP connections to game servers (`com.deadside.bot.sftp.SftpConnector`)
- **SftpManager**: Manages SFTP operations like file transfers (`com.deadside.bot.sftp.SftpManager`)

### Parsers
- **DeadsideCsvParser**: Parser for CSV death logs (`com.deadside.bot.parsers.DeadsideCsvParser`)
- **DeadsideLogParser**: Parser for server log files (`com.deadside.bot.parsers.DeadsideLogParser`)
- **KillfeedParser**: Specialized parser for killfeed data (`com.deadside.bot.parsers.KillfeedParser`)

### Command System
- **CommandManager**: Manages slash command registration and execution (`com.deadside.bot.commands.CommandManager`)
- **Command Categories**:
  - Admin: Server configuration and management commands (`com.deadside.bot.commands.admin`)
  - Economy: Currency, gambling, and reward commands (`com.deadside.bot.commands.economy`)
  - Faction: Player faction management commands (`com.deadside.bot.commands.faction`)
  - Player: Player profile and linking commands (`com.deadside.bot.commands.player`)
  - Stats: Statistics and leaderboard commands (`com.deadside.bot.commands.stats`)

### Event System
- **Listeners**: Event listeners for various Discord interactions:
  - `ButtonListener`: Handles button click events
  - `CommandListener`: Processes slash command interactions
  - `StringSelectMenuListener`: Handles dropdown menu selections

### Premium Features
- **PremiumManager**: Manages premium status and feature gating (`com.deadside.bot.premium.PremiumManager`)

### Schedulers
- **KillfeedScheduler**: Schedules regular killfeed updates (`com.deadside.bot.schedulers.KillfeedScheduler`)

### Utilities
- **EmbedUtils**: Helper methods for creating Discord embeds (`com.deadside.bot.utils.EmbedUtils`)

## Configuration
- **config.properties**: Main configuration file containing Discord token, MongoDB URI, etc.

## Dependencies (from pom.xml)
- JDA 5.0.0-beta.13 (Discord API)
- MongoDB Java Driver 4.10.2
- Apache Commons VFS2 2.9.0 (Virtual File System for SFTP)
- JSch 0.1.55 (SFTP implementation)
- OpenCSV 5.7.1 (CSV parsing)
- SLF4J and Logback (Logging)
- Apache Commons Lang3 (Utility functions)
- Gson (JSON handling)
- Apache Commons IO

## Database Models
- **GameServer**: Represents a Deadside game server with SFTP credentials, channels, etc.
- **Player**: Represents a player with stats like kills, deaths, etc.
- Other models for factions, economy, and player linking