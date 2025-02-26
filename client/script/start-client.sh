#!/bin/bash

# Startup script for FileHandlerClient

# Change to client directory
cd "$(dirname "$0")/.."

# Create log directory if it doesn't exist
mkdir -p logs

# Set Java options
JAVA_OPTS="-Dlog4j.configuration=file:config/log4j.properties"

# Get client configuration file
CONFIG_FILE="config/client.properties"
if [ "$1" != "" ]; then
  CONFIG_FILE="$1"
fi

echo "Starting FileHandlerClient with configuration: $CONFIG_FILE"
echo "Environment Information:"
echo "------------------------"
echo "Java Version: $(java -version 2>&1 | head -n 1)"
echo "Working Directory: $(pwd)"
echo "------------------------"

# Start the client
java $JAVA_OPTS -jar lib/filehandler-client.jar "$CONFIG_FILE" 