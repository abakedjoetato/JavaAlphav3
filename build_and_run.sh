#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# Color codes for better readability
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Building and Running Deadside Discord Bot ===${NC}"

# Step 1: Check for MongoDB
echo -e "${YELLOW}Checking MongoDB status...${NC}"
if pgrep -x "mongod" > /dev/null
then
    echo -e "${GREEN}MongoDB is already running${NC}"
else
    echo -e "${YELLOW}Starting MongoDB...${NC}"
    mkdir -p data/db
    mongod --dbpath=data/db --fork --logpath=data/mongod.log
    echo -e "${GREEN}MongoDB started successfully${NC}"
fi

# Step 2: Check environment variables
echo -e "${YELLOW}Checking environment variables...${NC}"
if [ -z "$DISCORD_TOKEN" ]; then
  echo -e "${RED}ERROR: DISCORD_TOKEN environment variable is not set!${NC}"
  echo -e "${YELLOW}Please set it in the Secrets tab (lock icon) in Replit.${NC}"
  exit 1
fi

# Step 3: Build the project
echo -e "${YELLOW}Building the project with Maven...${NC}"
# Use -T 1C for parallel builds with 1 thread per core
# Use -Dmaven.test.skip=true to skip tests
mvn clean package -T 1C -Dmaven.test.skip=true

# Step 4: Run the bot
echo -e "${GREEN}Starting the Deadside Discord Bot...${NC}"
java -jar target/deadside-discord-bot-1.0-SNAPSHOT.jar