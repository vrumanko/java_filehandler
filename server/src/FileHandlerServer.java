package server.src;

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

public class FileHandlerServer {
    private static final Logger logger = Logger.getLogger(FileHandlerServer.class);
    private Properties config;
    private int port;
    private ServerSocket serverSocket;
    private boolean running;
    private ExecutorService executor;
    private Properties clientKeys;
    private Properties clientPaths;

    public FileHandlerServer(String configPath) {
        try {
            // Load configuration
            config = new Properties();
            config.load(new FileInputStream(configPath));
            
            // Parse configuration
            port = Integer.parseInt(config.getProperty("server.port"));
            
            // Load client encryption keys and storage paths
            clientKeys = new Properties();
            String clientKeysPath = config.getProperty("client.keys.path");
            if (clientKeysPath != null) {
                clientKeys.load(new FileInputStream(clientKeysPath));
            }
            
            clientPaths = new Properties();
            String clientPathsConfig = config.getProperty("client.paths.config");
            if (clientPathsConfig != null) {
                clientPaths.load(new FileInputStream(clientPathsConfig));
            }
            
            logger.info("FileHandlerServer initialized on port: " + port);
            printEnvironmentInfo();
        } catch (IOException e) {
            logger.error("Error loading configuration: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    public void start() {
        logger.info("Starting file handler server on port: " + port);
        System.out.println("File Handler Server starting on port: " + port);
        
        running = true;
        executor = Executors.newFixedThreadPool(10);
        
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server is listening on port " + port);
            logger.info("Server started and listening on port " + port);
            
            while (running) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            if (running) {
                logger.error("Error starting server: " + e.getMessage(), e);
                System.out.println("Error starting server: " + e.getMessage());
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try {
            logger.info("Client connected: " + clientSocket.getInetAddress().getHostAddress());
            System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
            
            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
            
            // Read client label
            String clientLabel = dis.readUTF();
            logger.info("Client label: " + clientLabel);
            
            // Read original file name
            String originalFileName = dis.readUTF();
            logger.info("Original file name: " + originalFileName);
            
            // Read file hash
            String fileHash = dis.readUTF();
            logger.info("File hash: " + fileHash);
            
            // Read file size
            long fileSize = dis.readLong();
            logger.info("File size: " + fileSize + " bytes");
            
            System.out.println("Receiving file: " + originalFileName + " from client: " + clientLabel);
            
            // Process the file
            long startTime = System.currentTimeMillis();
            
            // Create temp file for encrypted data
            File encryptedFile = File.createTempFile("encrypted_", ".enc");
            
            // Read file data
            try (FileOutputStream fos = new FileOutputStream(encryptedFile)) {
                byte[] buffer = new byte[4096];
                long remaining = fileSize;
                int bytesRead;
                
                while (remaining > 0 && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
            }
            
            // Decrypt file
            String encryptionKey = clientKeys.getProperty(clientLabel);
            if (encryptionKey == null) {
                logger.error("No encryption key found for client: " + clientLabel);
                dos.writeUTF("ERROR: No encryption key found");
                return;
            }
            
            File decryptedFile = decryptFile(encryptedFile, encryptionKey);
            
            // Uncompress file
            File uncompressedFile = uncompressFile(decryptedFile);
            
            // Calculate hash
            String calculatedHash = calculateFileHash(uncompressedFile.toPath());
            
            // Verify hash
            if (!calculatedHash.equals(fileHash)) {
                logger.error("Hash verification failed for file: " + originalFileName);
                dos.writeUTF("ERROR: Hash verification failed");
                return;
            }
            
            // Store file
            String clientStoragePath = clientPaths.getProperty(clientLabel);
            if (clientStoragePath == null) {
                clientStoragePath = "incoming"; // Default storage directory
            }
            
            File storageDir = new File(clientStoragePath);
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }
            
            File destinationFile = new File(storageDir, originalFileName);
            Files.copy(uncompressedFile.toPath(), destinationFile.toPath());
            
            // Calculate duration
            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;
            
            // Log completion
            String logMessage = String.format(
                "File transfer completed: %s, Client: %s, Size: %d bytes, Hash: %s, Duration: %.2f seconds",
                originalFileName, clientLabel, fileSize, fileHash, duration
            );
            logger.info(logMessage);
            System.out.println(logMessage);
            
            // Send success response
            dos.writeUTF("SUCCESS");
            
            // Clean up temp files
            encryptedFile.delete();
            decryptedFile.delete();
            uncompressedFile.delete();
            
        } catch (Exception e) {
            logger.error("Error handling client: " + e.getMessage(), e);
            System.out.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                logger.error("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private File decryptFile(File encryptedFile, String encryptionKey) throws Exception {
        File decryptedFile = File.createTempFile("decrypted_", ".gz");
        
        SecretKeySpec secretKey = new SecretKeySpec(encryptionKey.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        
        try (FileInputStream fis = new FileInputStream(encryptedFile);
             FileOutputStream fos = new FileOutputStream(decryptedFile)) {
            
            byte[] inputBytes = new byte[(int) encryptedFile.length()];
            fis.read(inputBytes);
            
            byte[] outputBytes = cipher.doFinal(inputBytes);
            fos.write(outputBytes);
        }
        
        logger.debug("File decrypted successfully");
        return decryptedFile;
    }

    private File uncompressFile(File compressedFile) throws IOException {
        File uncompressedFile = File.createTempFile("uncompressed_", ".bin");
        
        try (FileInputStream fis = new FileInputStream(compressedFile);
             GZIPInputStream gzis = new GZIPInputStream(fis);
             FileOutputStream fos = new FileOutputStream(uncompressedFile)) {
            
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        }
        
        logger.debug("File uncompressed successfully");
        return uncompressedFile;
    }

    private String calculateFileHash(Path filePath) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(filePath);
        byte[] hashBytes = digest.digest(fileBytes);
        
        StringBuilder hexString = new StringBuilder();
        for (byte hashByte : hashBytes) {
            String hex = Integer.toHexString(0xff & hashByte);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        
        return hexString.toString();
    }

    private void printEnvironmentInfo() {
        logger.info("Environment Information:");
        logger.info("Java Version: " + System.getProperty("java.version"));
        logger.info("Java Home: " + System.getProperty("java.home"));
        logger.info("Working Directory: " + System.getProperty("user.dir"));
        logger.info("Server Port: " + port);
        
        System.out.println("\n--- Environment Information ---");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Java Home: " + System.getProperty("java.home"));
        System.out.println("Working Directory: " + System.getProperty("user.dir"));
        System.out.println("Server Port: " + port);
        System.out.println("-----------------------------\n");
    }

    public void stop() {
        running = false;
        
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.error("Error closing server socket: " + e.getMessage());
            }
        }
        
        if (executor != null) {
            executor.shutdown();
        }
        
        logger.info("FileHandlerServer stopped");
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java FileHandlerServer <config-file-path>");
            System.exit(1);
        }
        
        String configPath = args[0];
        FileHandlerServer server = new FileHandlerServer(configPath);
        server.start();
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
} 