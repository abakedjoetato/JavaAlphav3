#!/bin/bash

echo "Starting Deadside Discord Bot with Maven..."

# Check if Discord token is set
if [ -z "$DISCORD_TOKEN" ]; then
  echo "ERROR: DISCORD_TOKEN environment variable is not set!"
  echo "Please set it in the Secrets tab (lock icon) in Replit."
  exit 1
fi

# Run the bot with Maven
mvn exec:java -Dexec.mainClass="com.deadside.bot.Main" || {
  echo "Failed to start the bot with main class. Exiting."
  exit 1
}