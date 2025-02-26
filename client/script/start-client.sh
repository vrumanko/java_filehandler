#!/bin/bash
CLASSPATH="../lib/filehandler-client.jar:../lib/commons-io-2.11.0.jar:../lib/log4j-1.2.17.jar"
echo "Classpath: $CLASSPATH"
echo "Starting FileHandlerClient with configuration: ../config/client.properties"

echo "Environment Information:"
echo "------------------------"
echo "Java Version: $(java -version 2>&1 | head -n 1)"
echo "Working Directory: $(pwd)"
echo "------------------------"

java -cp "$CLASSPATH" client.FileHandlerClient ../config/client.properties
