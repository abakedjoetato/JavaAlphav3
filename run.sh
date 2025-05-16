#!/bin/bash

# Make sure MongoDB is running
if [ ! -d "data/db" ]; then
  mkdir -p data/db
fi

# Start MongoDB in background if not already running
if ! pgrep -x "mongod" > /dev/null; then
  echo "Starting MongoDB..."
  mongod --dbpath=data/db --logpath=data/mongod.log --fork
fi

# Compile and run the Discord bot
echo "Compiling Deadside Discord Bot..."
mvn compile

echo "Starting Deadside Discord Bot..."
echo "Please ensure you've set the DISCORD_TOKEN environment variable."
mvn exec:java -Dexec.mainClass="com.deadside.bot.Main"