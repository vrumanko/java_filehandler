#!/bin/bash

# Startup script for FileHandlerServer

# Change to server directory
cd "$(dirname "$0")/.."

# Create log directory if it doesn't exist
mkdir -p logs

# Set Java options
JAVA_OPTS="-Dlog4j.configuration=file:config/log4j.properties"

# Get server configuration file
CONFIG_FILE="config/server.properties"
if [ "$1" != "" ]; then
  CONFIG_FILE="$1"
fi

echo "Starting FileHandlerServer with configuration: $CONFIG_FILE"
echo "Environment Information:"
echo "------------------------"
echo "Java Version: $(java -version 2>&1 | head -n 1)"
echo "Working Directory: $(pwd)"
echo "------------------------"

# Start the server
java $JAVA_OPTS -jar lib/filehandler-server.jar "$CONFIG_FILE" 