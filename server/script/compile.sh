#!/bin/bash
echo "Compiling FileHandlerServer..."
mkdir -p bin
javac -d bin -cp "../lib/*" ../src/main/java/server/FileHandlerServer.java
if [ $? -eq 0 ]; then
  echo "Creating JAR file..."
  jar cf ../lib/filehandler-server.jar -C bin .
  echo "FileHandlerServer compiled successfully."
else
  echo "Compilation failed."
  exit 1
fi
