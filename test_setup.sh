#!/bin/bash

# Test script for Java File Handler

echo "Testing Java File Handler setup..."

# Create test directories
echo "Creating test directories..."
mkdir -p /tmp/filehandler/source1
mkdir -p /tmp/filehandler/source2
mkdir -p /tmp/filehandler/received
mkdir -p client/logs
mkdir -p server/logs

# Create test file
echo "Creating test file..."
echo "This is a test file for Java File Handler" > /tmp/filehandler/source1/test.txt
echo "File creation timestamp: $(date)" >> /tmp/filehandler/source1/test.txt

# Check Java version
echo "Checking Java version..."
java -version

# Create lib directories
mkdir -p client/lib server/lib

# Download required JAR files if missing
download_jar() {
  local dir="$1"
  local jar_name="$2"
  local jar_url="$3"
  
  if [ ! -f "$dir/lib/$jar_name" ]; then
    echo "Downloading $jar_name to $dir/lib/..."
    if command -v curl > /dev/null; then
      curl -L -o "$dir/lib/$jar_name" "$jar_url"
    elif command -v wget > /dev/null; then
      wget -O "$dir/lib/$jar_name" "$jar_url"
    else
      echo "Error: Please install curl or wget to download the JAR files."
      exit 1
    fi
    
    if [ $? -eq 0 ]; then
      echo "Successfully downloaded $jar_name to $dir/lib/"
    else
      echo "Failed to download $jar_name"
      exit 1
    fi
  else
    echo "$jar_name already exists in $dir/lib/"
  fi
}

LOG4J_URL="https://archive.apache.org/dist/logging/log4j/1.2.17/log4j-1.2.17.jar"
COMMONS_IO_URL="https://repo1.maven.org/maven2/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar"

# Download JARs for client
download_jar "client" "log4j-1.2.17.jar" "$LOG4J_URL"
download_jar "client" "commons-io-2.11.0.jar" "$COMMONS_IO_URL"

# Download JARs for server
download_jar "server" "log4j-1.2.17.jar" "$LOG4J_URL"
download_jar "server" "commons-io-2.11.0.jar" "$COMMONS_IO_URL"

# Verify JAR files
echo "Verifying JAR files..."
for dir in "client" "server"; do
  for jar in "log4j-1.2.17.jar" "commons-io-2.11.0.jar"; do
    if [ ! -f "$dir/lib/$jar" ]; then
      echo "Error: $jar not found in $dir/lib/"
      exit 1
    fi
  done
done

echo "All required JAR files are present."

# Verify encryption key length in config files
echo "Checking encryption key length..."

check_key_length() {
  local file="$1"
  local property="$2"
  
  if [ -f "$file" ]; then
    key=$(grep "$property" "$file" | cut -d'=' -f2)
    length=${#key}
    if [ "$length" -ne 16 ] && [ "$length" -ne 24 ] && [ "$length" -ne 32 ]; then
      echo "WARNING: The encryption key in $file is $length characters long."
      echo "For AES encryption, key length should be 16, 24, or 32 characters."
      echo "The system will still work because we use SHA-256 hashing, but it's recommended to use standard lengths."
    else
      echo "Key length in $file is good: $length characters (valid for AES)."
    fi
  fi
}

check_key_length "client/config/client.properties" "encryption.key"
check_key_length "server/config/client_keys.properties" "client1"

# All checks passed
echo ""
echo "Setup is complete! Follow these steps:"
echo "----------------------------------------"
echo "1. Compile the server: cd server/script && ./compile.sh"
echo "2. Compile the client: cd client/script && ./compile.sh"
echo "3. Start the server: cd server/script && ./start-server.sh"
echo "4. Start the client: cd client/script && ./start-client.sh"
echo "5. Monitor the file transfer in logs and terminal output"
echo ""
echo "Test scenario:"
echo "- The setup has created a test file at /tmp/filehandler/source1/test.txt"
echo "- When both client and server are running, the file will be transferred"
echo "- After successful transfer, it will appear in /tmp/filehandler/received/"
echo "----------------------------------------" 