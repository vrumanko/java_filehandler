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
│   │   ├── client.properties - Client configuration
│   │   └── log4j.properties - Logging configuration
│   ├── lib/            - JAR dependencies
│   ├── logs/           - Log files
│   ├── script/         - Compilation and startup scripts
│   └── src/            - Source code
├── server/
│   ├── config/         - Configuration files
│   │   ├── server.properties - Server configuration
│   │   ├── client_keys.properties - Client encryption keys
│   │   ├── client_paths.properties - Client storage paths
│   │   └── log4j.properties - Logging configuration
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

### Installation Steps

1. Clone repository:
   ```
   git clone git@github.com:vrumanko/java_filehandler.git
   ```

2. Copy the downloaded JAR files to both client and server lib directories:
   ```
   cd java_filehandler
   ./test_setup.sh
   ```
It will download required libraries, create directory structure and create demo configuration for one machine.


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

## Client Configuration Details

The client application is configured through the `client.properties` file with the following parameters:

- `client.label`: A unique identifier for the client (required by the server)
- `source.directories`: Comma-separated list of directories to monitor for files
- `server.host`: IP address or hostname of the server
- `server.port`: Port number the server is listening on
- `encryption.key`: Secret key used for AES encryption
- `polling.interval.seconds`: Time between directory scans (defaults to 10 seconds if not specified)

Example client.properties:
```
client.label=client1
source.directories=/path/to/watch1,/path/to/watch2
server.host=192.168.1.100
server.port=9000
encryption.key=your-secret-key-here
polling.interval.seconds=30
```

## Server Configuration Details

The server uses multiple configuration files:

1. `server.properties`:
   - `server.port`: Port to listen on for client connections
   - `client.keys.file`: Path to the client keys configuration file
   - `client.paths.file`: Path to the client paths configuration file

2. `client_keys.properties`:
   - Maps client labels to their encryption keys
   - Format: `client_label=encryption_key`

3. `client_paths.properties`:
   - Maps client labels to their storage directories
   - Format: `client_label=/path/to/storage/directory`

## File Processing Workflow

1. The client scans configured directories at regular intervals
2. When a new file is found:
   - A SHA-256 hash is calculated for integrity verification
   - The file is compressed using GZIP
   - The compressed file is encrypted using AES
   - The encrypted file is sent to the server along with metadata
   - Upon successful transfer, the original file is deleted

3. The server:
   - Receives the encrypted file and metadata
   - Identifies the client and retrieves its encryption key
   - Decrypts and decompresses the file
   - Verifies the file integrity using the provided hash
   - Stores the file in the client's designated directory
   - If same file already exists in target directory, server raises error for that file and do not overwrite it
   - Sends a success confirmation to the client

## Logging Configuration

Both client and server use log4j for logging. The default configuration:
- Logs INFO level and above messages
- Outputs logs to both console and log files
- Server logs are written to `server/logs/server_filehandler.log`
- Client logs are written to `client/logs/client_filehandler.log`
- Log files are rotated when they reach 10MB with a maximum of 10 backup files

You can customize logging behavior by editing the respective log4j.properties files.

## Testing

1. Start both the server and client.
2. Create the source directories defined in the client configuration.
3. Place a test file in one of the monitored directories.
4. Check the logs and terminal output to see the progress.
5. Verify that the file is transferred to the server's storage location.

## Troubleshooting

### Log4j Configuration Issues
If you see log4j warnings like "No appenders could be found for logger", make sure:
- The log4j.properties file exists in the config directory
- The logs directory exists
- The startup script correctly references the log4j configuration file

### File Path Issues
The server application expects to find configuration files like client_keys.properties and client_paths.properties in the config directory relative to the working directory. If you encounter FileNotFoundException errors:
- Ensure all configuration files exist in the correct locations
- Make sure the startup scripts set the correct working directory
- Use absolute paths in properties files for external directories

### ClassNotFoundException
If you get "ClassNotFoundException" errors:
- Check that the JAR files exist in the lib directory
- Verify the classpath in the startup scripts is correct
- Make sure the compilation process completed successfully

### Connection Issues
If the client cannot connect to the server:
- Verify the server is running
- Check that the server IP and port in client.properties are correct
- Ensure there are no firewall rules blocking the connection
- Verify network connectivity between client and server machines

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

## License

This project is licensed under the GPLv3 License. 

## Support This Project  
If this code helps you, consider sending a small crypto donation:  
- **SOL**: `DL5sEEG6z666vyety2FdDZtTF1pMtMAnjKXSdZTYg34K` 
- **BNB**: `0xC08f5CC86610e400bb3c12Fe8a085514F7e786E0`