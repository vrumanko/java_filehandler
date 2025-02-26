#!/bin/bash
echo "Compiling FileHandlerClient..."
mkdir -p bin
javac -d bin -cp "../lib/*" ../src/main/java/client/FileHandlerClient.java
if [ $? -eq 0 ]; then
  echo "Creating JAR file..."
  jar cf ../lib/filehandler-client.jar -C bin .
  echo "FileHandlerClient compiled successfully."
else
  echo "Compilation failed."
  exit 1
fi
