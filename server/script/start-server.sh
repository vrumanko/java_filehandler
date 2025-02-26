#!/bin/bash

# Set variables
SERVER_DIR="$(cd .. && pwd)"
LIB_DIR="$SERVER_DIR/lib"
CONFIG_DIR="$SERVER_DIR/config"

# Set classpath with absolute paths
CLASSPATH="$LIB_DIR/filehandler-server.jar:$LIB_DIR/commons-io-2.11.0.jar:$LIB_DIR/log4j-1.2.17.jar"
CONFIG_FILE="$CONFIG_DIR/server.properties"
LOG4J_CONFIG="$CONFIG_DIR/log4j.properties"

# Display startup information
echo "Classpath: $CLASSPATH"
echo "Starting FileHandlerServer with configuration: $CONFIG_FILE"
echo "Environment Information:"
echo "------------------------"
echo "Java Version: $(java -version 2>&1 | head -n 1)"
echo "Working Directory: $(pwd)"
echo "------------------------"

# Create log directory if it doesn't exist
mkdir -p "$SERVER_DIR/logs"

# Change to the server directory to make relative paths work
cd "$SERVER_DIR"

# Start the server
java -Dlog4j.configuration=file:"$LOG4J_CONFIG" -cp "$CLASSPATH" server.FileHandlerServer "$CONFIG_FILE"
