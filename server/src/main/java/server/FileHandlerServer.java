/**
* ## License
* This project is licensed under the GPLv3 License. 
*
* ## Support This Project  
* If this code helps you, consider sending a small crypto donation:  
* - **SOL**: `DL5sEEG6z666vyety2FdDZtTF1pMtMAnjKXSdZTYg34K` 
* - **BNB**: `0xC08f5CC86610e400bb3c12Fe8a085514F7e786E0` 
*/

package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.apache.log4j.Logger;

/**
 * FileHandlerServer class
 * 
 * This server receives encrypted and compressed files from clients, processes them
 * (decrypts, uncompresses), verifies file integrity via hash validation, and stores
 * them in designated client-specific storage locations.
 * 
 * Security features:
 * - Client-specific encryption keys
 * - File hash verification
 * - Secure key logging (masking)
 */
public class FileHandlerServer {
    // Logger for application logging
    private static final Logger logger = Logger.getLogger(FileHandlerServer.class);
    
    // Server configuration properties
    private Properties config;
    
    // Server network configuration
    private int port;
    private ServerSocket serverSocket;
    private boolean running;
    
    // Thread pool for handling multiple client connections
    private ExecutorService executor;
    
    // Client-specific configuration
    private Properties clientKeys;    // Maps client IDs to their encryption keys
    private Properties clientPaths;   // Maps client IDs to their storage directories

    /**
     * Constructor - Initializes the server with configuration from the specified path.
     * 
     * @param configPath Path to the server configuration file
     */
    public FileHandlerServer(String configPath) {
        try {
            // Load main configuration file
            config = new Properties();
            config.load(new FileInputStream(configPath));
            
            // Extract server port from configuration
            port = Integer.parseInt(config.getProperty("server.port"));
            
            // Load client encryption keys from separate file for better security
            clientKeys = new Properties();
            String clientKeysPath = config.getProperty("client.keys.path");
            if (clientKeysPath != null) {
                clientKeys.load(new FileInputStream(clientKeysPath));
            }
            
            // Load client storage path mappings from configuration file
            clientPaths = new Properties();
            String clientPathsConfig = config.getProperty("client.paths.config");
            if (clientPathsConfig != null) {
                clientPaths.load(new FileInputStream(clientPathsConfig));
            }
            
            // Log server startup and configuration details
            logger.info("=========================================");
            logger.info("=== Server Java_Filehandler Started =====");
            logger.info("=========================================");
            logger.info("Server configuration:");
            logger.info("-----------------------------------------");

            // Log storage paths for all configured clients
            for (String key : clientPaths.stringPropertyNames()) {
                String path = clientPaths.getProperty(key);
                logger.info("Client: " + key + ", Storage Path: " + path);
            }
            
            // Log masked encryption keys for security audit purposes
            for (String key : clientKeys.stringPropertyNames()) {
                String encryptionKey = clientKeys.getProperty(key);
                String maskedKey = maskEncryptionKey(encryptionKey);
                logger.info("Client: " + key + ", Encryption Key: " + maskedKey);
            }
            logger.info("-----------------------------------------");
           
        } catch (IOException e) {
            // Log error and exit if configuration cannot be loaded
            logger.error("Error loading configuration: " + e.getMessage(), e);
            System.exit(1);
        }
    }
    
    /**
     * Masks the encryption key for secure logging purposes.
     * Shows only first 4 and last 4 characters, with the middle replaced by "..."
     * 
     * @param key The encryption key to mask
     * @return A masked version of the key for secure logging
     */
    private String maskEncryptionKey(String key) {
        if (key == null || key.length() <= 8) {
            return "***masked***";
        }
        // Show only first 4 and last 4 characters for security
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }

    /**
     * Starts the server and begins listening for client connections.
     * Uses a thread pool to handle multiple clients simultaneously.
     */
    public void start() {
        // Initialize server state
        running = true;
        
        // Create thread pool for handling multiple client connections
        executor = Executors.newFixedThreadPool(10);
        
        try {
            // Start server socket on configured port
            serverSocket = new ServerSocket(port);
            
            // Get and log the server's IP address for connection information
            String serverIP = java.net.InetAddress.getLocalHost().getHostAddress();
            logger.info("Server is running on IP: " + serverIP + " and port " + port);
            
            // Main server loop - accept connections and process in separate threads
            while (running) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            // Only log as error if the exception wasn't caused by manual server shutdown
            if (running) {
                logger.error("Error starting server: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Handles an individual client connection.
     * Processes file transfers including:
     * - Reading metadata (client ID, filename, file hash, file size)
     * - Receiving encrypted data
     * - Decrypting and uncompressing data
     * - Verifying file integrity via hash
     * - Storing the file in client-specific directory
     * 
     * @param clientSocket The socket for the client connection
     */
    private void handleClient(Socket clientSocket) {
        try {
            // Log client connection with IP address for audit purposes
            logger.info("Client connected: " + clientSocket.getInetAddress().getHostAddress());
            
            // Initialize data streams for communication with client
            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
            
            // Read client identification label (used to determine encryption key and storage path)
            String clientLabel = dis.readUTF();
            logger.info("Client label: " + clientLabel);
            
            // Read original file name (will be preserved when storing)
            String originalFileName = dis.readUTF();
            logger.info("Original file name: " + originalFileName);
            
            // Read file hash (for integrity verification)
            String fileHash = dis.readUTF();
            logger.info("File hash: " + fileHash);
            
            // Read file size (for progress tracking and verification)
            long fileSize = dis.readLong();
            logger.info("File size: " + fileSize + " bytes");
            
            logger.info("Receiving file: " + originalFileName + " from client: " + clientLabel);
            
            // Start timing the file processing for performance logging
            long startTime = System.currentTimeMillis();
            
            // Create temporary file to store the encrypted data
            File encryptedFile = File.createTempFile("encrypted_", ".enc");
            
            // Read encrypted file data from client
            try (FileOutputStream fos = new FileOutputStream(encryptedFile)) {
                byte[] buffer = new byte[4096];
                long remaining = fileSize;
                int bytesRead;
                
                // Read data in chunks until all bytes received
                while (remaining > 0 && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
            }
            
            // Look up encryption key for this client
            String encryptionKey = clientKeys.getProperty(clientLabel);
            if (encryptionKey == null) {
                // Security error - client not authorized or missing key
                logger.error("No encryption key found for client: " + clientLabel);
                dos.writeUTF("ERROR: No encryption key found");
                return;
            }
            
            // Decrypt the file using client-specific encryption key
            File decryptedFile = decryptFile(encryptedFile, encryptionKey);
            
            // Uncompress the file (clients send GZIP compressed data)
            File uncompressedFile = uncompressFile(decryptedFile);
            
            // Calculate hash of processed file for integrity verification
            String calculatedHash = calculateFileHash(uncompressedFile.toPath());
            
            // Verify file integrity by comparing hashes
            if (!calculatedHash.equals(fileHash)) {
                // Data integrity error - file corrupted during transfer
                logger.error("Hash verification failed for file: " + originalFileName);
                dos.writeUTF("ERROR: Hash verification failed");
                return;
            }
            
            // Determine storage location for this client
            String clientStoragePath = clientPaths.getProperty(clientLabel);
            if (clientStoragePath == null) {
                clientStoragePath = "incoming"; // Default storage directory
            }
            
            // Ensure storage directory exists
            File storageDir = new File(clientStoragePath);
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }
            
            // Save the processed file to its final destination
            File destinationFile = new File(storageDir, originalFileName);
            Files.copy(uncompressedFile.toPath(), destinationFile.toPath());
            
            // Calculate processing duration for performance logging
            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;
            
            // Log successful file transfer with performance metrics
            String logMessage = String.format(
                "File transfer completed: %s, Client: %s, Size: %d bytes, Hash: %s, Duration: %.2f seconds",
                originalFileName, clientLabel, fileSize, fileHash, duration
            );
            logger.info(logMessage);
            
            // Notify client of successful transfer
            dos.writeUTF("SUCCESS");
            
            // Clean up temporary files
            encryptedFile.delete();
            decryptedFile.delete();
            uncompressedFile.delete();
            
        } catch (Exception e) {
            // Log any errors during client handling
            logger.error("Error handling client: " + e.getMessage(), e);
        } finally {
            // Ensure client socket is closed even if an exception occurs
            try {
                clientSocket.close();
            } catch (IOException e) {
                logger.error("Error closing client socket: " + e.getMessage());
            }
        }
    }

    /**
     * Decrypts a file using AES encryption with the provided key.
     * 
     * @param encryptedFile The file containing encrypted data
     * @param encryptionKey The key to use for decryption
     * @return A temporary file containing the decrypted data
     * @throws Exception If decryption fails
     */
    private File decryptFile(File encryptedFile, String encryptionKey) throws Exception {
        // Create temporary file for decrypted data
        File decryptedFile = File.createTempFile("decrypted_", ".tmp");
        
        // Generate a 32-byte (256-bit) key using SHA-256 hash of the provided key
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(encryptionKey.getBytes());
        
        // Initialize AES cipher for decryption
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        
        // Perform decryption operation
        try (FileInputStream fis = new FileInputStream(encryptedFile);
             FileOutputStream fos = new FileOutputStream(decryptedFile)) {
            
            // Read all encrypted bytes
            byte[] inputBytes = new byte[(int) encryptedFile.length()];
            fis.read(inputBytes);
            
            // Decrypt and write to output file
            byte[] outputBytes = cipher.doFinal(inputBytes);
            fos.write(outputBytes);
        }
        
        logger.info("File decrypted successfully");
        return decryptedFile;
    }

    /**
     * Uncompresses a GZIP compressed file.
     * 
     * @param compressedFile The file containing GZIP compressed data
     * @return A temporary file containing the uncompressed data
     * @throws IOException If decompression fails
     */
    private File uncompressFile(File compressedFile) throws IOException {
        // Create temporary file for uncompressed data
        File uncompressedFile = File.createTempFile("uncompressed_", ".bin");
        
        // Set up GZIP input stream for decompression
        try (FileInputStream fis = new FileInputStream(compressedFile);
             GZIPInputStream gzis = new GZIPInputStream(fis);
             FileOutputStream fos = new FileOutputStream(uncompressedFile)) {
            
            // Decompress data in chunks
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        }
        
        logger.info("File uncompressed successfully");
        return uncompressedFile;
    }

    /**
     * Calculates SHA-256 hash of a file for integrity verification.
     * 
     * @param filePath Path to the file to hash
     * @return Hex string representation of the SHA-256 hash
     * @throws Exception If hash calculation fails
     */
    private String calculateFileHash(Path filePath) throws Exception {
        // Create SHA-256 digest for hash calculation
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        
        // Read file contents
        byte[] fileBytes = Files.readAllBytes(filePath);
        
        // Calculate hash
        byte[] hashBytes = digest.digest(fileBytes);
        
        // Convert hash bytes to hexadecimal string
        StringBuilder hexString = new StringBuilder();
        for (byte hashByte : hashBytes) {
            String hex = Integer.toHexString(0xff & hashByte);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        
        return hexString.toString();
    }

    /**
     * Stops the server gracefully, closing connections and resources.
     */
    public void stop() {
        // Set running flag to false to stop the main server loop
        running = false;
        
        // Close server socket to stop accepting new connections
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.error("Error closing server socket: " + e.getMessage());
            }
        }
        
        // Shutdown thread pool gracefully
        if (executor != null) {
            executor.shutdown();
        }
        
        logger.info("FileHandlerServer stopped");
    }

    /**
     * Main method to start the server from command line.
     * 
     * @param args Command-line arguments, expects config file path
     */
    public static void main(String[] args) {
        // Validate command-line arguments
        if (args.length < 1) {
            System.out.println("Error: Missing configuration directory path!");
            System.out.println("Usage: java FileHandlerServer <config-directory-path>");
            System.exit(1);
        }

        // Initialize server with config file
        String configPath = args[0];
        FileHandlerServer server = new FileHandlerServer(configPath);
       
        // Start the server
        server.start();
        
        // Add shutdown hook to gracefully stop server when JVM exits
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
} 