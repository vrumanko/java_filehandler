#!/bin/bash
CLASSPATH="../lib/filehandler-server.jar:../lib/commons-io-2.11.0.jar:../lib/log4j-1.2.17.jar"
echo "Classpath: $CLASSPATH"
echo "Starting FileHandlerServer with configuration: ../config/server.properties"

echo "Environment Information:"
echo "------------------------"
echo "Java Version: $(java -version 2>&1 | head -n 1)"
echo "Working Directory: $(pwd)"
echo "------------------------"

java -cp "$CLASSPATH" server.FileHandlerServer ../config/server.properties
