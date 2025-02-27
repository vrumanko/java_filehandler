#!/bin/bash


#!/bin/bash

# Set variables
CLIENT_DIR="$(cd .. && pwd)"
LIB_DIR="$CLIENT_DIR/lib"
CONFIG_DIR="$CLIENT_DIR/config"

# Set classpath with absolute paths
CLASSPATH="$LIB_DIR/filehandler-client.jar:$LIB_DIR/commons-io-2.11.0.jar:$LIB_DIR/log4j-1.2.17.jar"
CONFIG_FILE="$CONFIG_DIR/client.properties"
LOG4J_CONFIG="$CONFIG_DIR/log4j.properties"

# Display startup information
echo "Classpath: $CLASSPATH"
echo "Starting FileHandlerClient with configuration: $CONFIG_FILE"
echo "Environment Information:"
echo "------------------------"
echo "Java Version: $(java -version 2>&1 | head -n 1)"
echo "Working Directory: $(pwd)"
echo "------------------------"

# Create log directory if it doesn't exist
mkdir -p "$CLIENT_DIR/logs"

# Change to the server directory to make relative paths work
cd "$CLIENT_DIR"

# Start the client
java -Dlog4j.configuration=file:"$LOG4J_CONFIG" -cp "$CLASSPATH" client.FileHandlerClient "$CONFIG_DIR/client.properties"
