#!/bin/bash

# Compilation script for FileHandlerServer

echo "Compiling FileHandlerServer..."

# Move to project root
cd "$(dirname "$0")/.."
SERVER_ROOT=$(pwd)

# Create bin directory if it doesn't exist
mkdir -p bin

# Find all required JAR files in the lib directory
CLASSPATH="."
for jar in lib/*.jar; do
  if [ -f "$jar" ]; then
    CLASSPATH="$CLASSPATH:$jar"
  fi
done

# Compile Java files
echo "Compiling with classpath: $CLASSPATH"
javac -cp "$CLASSPATH" -d bin src/server/src/FileHandlerServer.java

if [ $? -eq 0 ]; then
  echo "Compilation successful!"
else
  echo "Compilation failed!"
  exit 1
fi

# Create JAR file
echo "Creating JAR file..."
mkdir -p lib
jar cvfm lib/filehandler-server.jar script/manifest-server.txt -C bin .

echo "Done!" 