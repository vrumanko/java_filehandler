#!/bin/bash

# Test script for Java File Handler

echo "Testing Java File Handler setup..."

# Create test directories
echo "Creating test directories..."
mkdir -p /tmp/filehandler/source1
mkdir -p /tmp/filehandler/source2
mkdir -p /tmp/filehandler/received/client1
mkdir -p /tmp/filehandler/received/client2

# Create test file
echo "Creating test file..."
echo "This is a test file for Java File Handler" > /tmp/filehandler/source1/test.txt
echo "File creation timestamp: $(date)" >> /tmp/filehandler/source1/test.txt

# Check Java version
echo "Checking Java version..."
java -version

# Check required JAR files
echo "Checking for required JAR files..."
if [ ! -d "client/lib" ] || [ ! -d "server/lib" ]; then
  echo "Error: lib directories not found!"
  echo "Please create client/lib and server/lib directories and add required JAR files:"
  echo "- log4j-1.2.17.jar"
  echo "- commons-io-2.11.0.jar"
  exit 1
fi

# All checks passed
echo "Setup looks good! You can now:"
echo "1. Start the server: cd server/script && ./start-server.sh"
echo "2. Start the client: cd client/script && ./start-client.sh"
echo "3. Monitor the file transfer in logs and terminal output" 