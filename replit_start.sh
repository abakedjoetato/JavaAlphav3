#!/bin/bash

# This script is designed to start the bot in the Replit environment
echo "Starting Deadside Discord Bot in Replit environment..."

# Look for Java in various locations
echo "Searching for Java..."
JAVA_CMD=""

# Try standard location first
if command -v java &> /dev/null; then
  JAVA_CMD="java"
  echo "Found Java in standard PATH"
elif [ -f "/nix/store/9jzr9ddxk4j391lk5mik5cz5mc48800h-graalvm-ce-21.3.0/bin/java" ]; then
  JAVA_CMD="/nix/store/9jzr9ddxk4j391lk5mik5cz5mc48800h-graalvm-ce-21.3.0/bin/java"
  echo "Found Java at $JAVA_CMD"
elif [ -d "/nix/store" ]; then
  echo "Searching in /nix/store directory..."
  JAVA_PATH=$(find /nix/store -name "java" -type f -executable -path "*/bin/java" 2>/dev/null | head -n 1)
  if [ ! -z "$JAVA_PATH" ]; then
    JAVA_CMD="$JAVA_PATH"
    echo "Found Java at $JAVA_CMD"
  fi
fi

# If we found Java, run the jar directly
if [ ! -z "$JAVA_CMD" ]; then
  echo "Using Java at: $JAVA_CMD"
  # Ensure the jar file exists
  if [ -f "target/deadside-discord-bot-1.0-SNAPSHOT.jar" ]; then
    echo "Starting bot using Java..."
    $JAVA_CMD -jar target/deadside-discord-bot-1.0-SNAPSHOT.jar
  else
    echo "Building bot with Maven first..."
    mvn package -DskipTests
    if [ -f "target/deadside-discord-bot-1.0-SNAPSHOT.jar" ]; then
      echo "Starting bot using Java..."
      $JAVA_CMD -jar target/deadside-discord-bot-1.0-SNAPSHOT.jar
    else
      echo "Failed to build JAR file. Falling back to Maven exec."
      mvn exec:java -Dexec.mainClass="com.deadside.bot.Main"
    fi
  fi
else
  echo "Java executable not found. Using Maven to execute..."
  mvn exec:java -Dexec.mainClass="com.deadside.bot.Main"
fi