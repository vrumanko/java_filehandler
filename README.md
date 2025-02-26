# Java File Handler

A client-server application for securely transferring files. The client watches specified directories for new files, processes them (hashing, compressing, encrypting), and sends them to the server. The server decrypts, uncompresses, and stores the files in specified locations based on the client's identity.

## Features

- File monitoring on client side
- File compression (GZIP)
- File encryption (AES)
- Secure file transfer
- File integrity verification with SHA-256 hashing
- Configurable client and server settings
- Detailed logging of all operations
- Terminal status updates

## Project Structure

```
java_filehandler/
├── client/
│   ├── config/         - Configuration files
│   ├── lib/            - JAR dependencies
│   ├── logs/           - Log files
│   ├── script/         - Compilation and startup scripts
│   └── src/            - Source code
├── server/
│   ├── config/         - Configuration files
│   ├── lib/            - JAR dependencies
│   ├── logs/           - Log files
│   ├── script/         - Compilation and startup scripts
│   └── src/            - Source code
└── README.md           - This file
```

## Prerequisites

- Java JDK 8 or higher
- Required libraries:
  - log4j-1.2.17.jar
  - commons-io-2.11.0.jar

## Obtaining Required JAR Files

This project depends on specific JAR files that need to be downloaded and placed in the appropriate directories:

### Log4j 1.2.17

1. Download log4j-1.2.17.jar from the Apache Archive:
   - Direct link: https://archive.apache.org/dist/logging/log4j/1.2.17/log4j-1.2.17.jar
   - Or visit: https://archive.apache.org/dist/logging/log4j/1.2.17/

2. Verify the file integrity (optional):
   - The SHA1 checksum should be: `5af35056b4d257e4b64b9e8069c0746e8b08629f`

### Commons IO 2.11.0

1. Download commons-io-2.11.0.jar from Apache Commons:
   - Direct link: https://repo1.maven.org/maven2/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar
   - Or visit: https://commons.apache.org/proper/commons-io/download_io.cgi

2. Verify the file integrity (optional):
   - The SHA1 checksum should be: `a2503f302b11ebde7ebc3df41daebe0e4eea3689`

### Installation Steps

1. Create the lib directories if they don't exist:
   ```
   mkdir -p client/lib server/lib
   ```

2. Copy the downloaded JAR files to both client and server lib directories:
   ```
   cp /path/to/log4j-1.2.17.jar client/lib/
   cp /path/to/log4j-1.2.17.jar server/lib/
   cp /path/to/commons-io-2.11.0.jar client/lib/
   cp /path/to/commons-io-2.11.0.jar server/lib/
   ```

3. Verify the installation:
   ```
   ls -la client/lib/
   ls -la server/lib/
   ```
   
   You should see both JAR files in each directory.

## Setup

1. Download and place the required JAR files in the `client/lib/` and `server/lib/` directories.

2. Configure client:
   - Edit `client/config/client.properties` to set:
     - Client label
     - Source directories to monitor
     - Server IP and port
     - Encryption key

3. Configure server:
   - Edit `server/config/server.properties` to set:
     - Server port
     - Client key and path configuration files
   - Edit `server/config/client_keys.properties` to define encryption keys for each client
   - Edit `server/config/client_paths.properties` to define storage paths for each client

## Compilation

1. Client:
   ```
   cd client/script
   ./compile.sh
   ```

2. Server:
   ```
   cd server/script
   ./compile.sh
   ```

## Usage

1. Start the server:
   ```
   cd server/script
   ./start-server.sh
   ```

2. Start the client:
   ```
   cd client/script
   ./start-client.sh
   ```

3. Place files in the monitored directories. The client will automatically process and send them to the server.

## Testing

1. Start both the server and client.
2. Create the source directories defined in the client configuration.
3. Place a test file in one of the monitored directories.
4. Check the logs and terminal output to see the progress.
5. Verify that the file is transferred to the server's storage location.

## License

This project is licensed under the MIT License. 