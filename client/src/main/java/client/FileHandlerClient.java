package client;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class FileHandlerClient {
    // Initialize logger
    private static final Logger logger = Logger.getLogger(FileHandlerClient.class);
    
    private Properties config;
    private List<String> sourceDirs;
    private String serverIp;
    private int serverPort;
    private String clientLabel;
    private String encryptionKey;
    private ScheduledExecutorService scheduler;
    private int pollingIntervalSeconds;

    public FileHandlerClient(String configPath) {
        try {
            // Load configuration
            config = new Properties();
            config.load(new FileInputStream(configPath));
            
            // Parse configuration
            sourceDirs = Arrays.asList(config.getProperty("source.directories").split(","));
            serverIp = config.getProperty("server.host");
            serverPort = Integer.parseInt(config.getProperty("server.port"));
            clientLabel = config.getProperty("client.label");
            encryptionKey = config.getProperty("encryption.key");
            
            // Get polling interval with default of 30 seconds if not specified
            String pollingIntervalStr = config.getProperty("polling.interval.seconds");
            pollingIntervalSeconds = (pollingIntervalStr != null) ? 
                                     Integer.parseInt(pollingIntervalStr) : 30;
            
            logger.info("FileHandlerClient initialized with label: " + clientLabel);
            System.out.println("FileHandlerClient initialized with label: " + clientLabel);
            printEnvironmentInfo();
        } catch (IOException e) {
            logger.error("Error loading configuration: " + e.getMessage(), e);
            System.err.println("Error loading configuration: " + e.getMessage());
            System.exit(1);
        }
    }

    public void start() {
        logger.info("Starting file handler client with label: " + clientLabel);
       
        // Schedule file polling using the configured interval
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::pollDirectories, 0, pollingIntervalSeconds, TimeUnit.SECONDS);
        
        logger.info("Directory polling scheduled every " + pollingIntervalSeconds + " seconds");
       
    }

    private void pollDirectories() {
        String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        logger.info(timeStamp + " - Scanning directories for files to send...");
       
        for (String dirPath : sourceDirs) {
            try {
                Path dir = Paths.get(dirPath);
                if (!Files.exists(dir)) {
                    logger.warn("Directory does not exist: " + dirPath);
                   
                    continue;
                }
                
                Files.list(dir).filter(Files::isRegularFile).forEach(this::processFile);
            } catch (IOException e) {
                logger.error("Error polling directory " + dirPath + ": " + e.getMessage(), e);
               
            }
        }
    }

    private void processFile(Path filePath) {
        String fileName = filePath.getFileName().toString();
        logger.info("Processing file: " + fileName);
       
        
        try {
            // Calculate file hash
            String fileHash = calculateFileHash(filePath);
            
            // Compress the file
            File compressedFile = compressFile(filePath.toFile());
            
            // Encrypt the file
            File encryptedFile = encryptFile(compressedFile);
            
            // Send the file to server
            boolean sent = sendFileToServer(encryptedFile, fileHash, fileName);
            
            // Clean up temporary files
            compressedFile.delete();
            encryptedFile.delete();
            
            if (sent) {
                // Delete original file after successful transfer
                Files.delete(filePath);
                logger.info("File processed and sent successfully: " + fileName);
              
            }
        } catch (Exception e) {
            logger.error("Error processing file " + fileName + ": " + e.getMessage(), e);
           
        }
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

    private File compressFile(File inputFile) throws IOException {
        File compressedFile = File.createTempFile("compressed_", ".gz");
        
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(compressedFile);
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
            
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                gzos.write(buffer, 0, len);
            }
        }
        
        return compressedFile;
    }

    private File encryptFile(File inputFile) throws Exception {
        File encryptedFile = File.createTempFile("encrypted_", ".tmp");
        
        // Generate a 32-byte (256-bit) key using SHA-256
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(encryptionKey.getBytes());
        
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(encryptedFile)) {
            
            byte[] inputBytes = new byte[(int) inputFile.length()];
            fis.read(inputBytes);
            
            byte[] outputBytes = cipher.doFinal(inputBytes);
            fos.write(outputBytes);
        }
        
        return encryptedFile;
    }

    private boolean sendFileToServer(File file, String fileHash, String originalFileName) {
        long startTime = System.currentTimeMillis();
        
        try (Socket socket = new Socket(serverIp, serverPort);
             OutputStream os = socket.getOutputStream();
             DataOutputStream dos = new DataOutputStream(os)) {
            
            // Send client label
            dos.writeUTF(clientLabel);
            
            // Send original file name
            dos.writeUTF(originalFileName);
            
            // Send file hash
            dos.writeUTF(fileHash);
            
            // Send file size
            dos.writeLong(file.length());
            
            // Send file data
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
            }
            
            // Check response
            try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
                String response = dis.readUTF();
                
                long endTime = System.currentTimeMillis();
                double duration = (endTime - startTime) / 1000.0;
                
                String message = "File sent: " + originalFileName + " (Duration: " + duration + " seconds)";
                logger.info(message);
               
                
                return "SUCCESS".equals(response);
            }
        } catch (IOException e) {
            logger.error("Error sending file to server: " + e.getMessage(), e);
           
            return false;
        }
    }

    private void printEnvironmentInfo() {
        logger.info("--- Environment Information ---");
        logger.info("Java Version: " + System.getProperty("java.version"));
        logger.info("Java Home: " + System.getProperty("java.home"));
        logger.info("Working Directory: " + System.getProperty("user.dir"));
        logger.info("Client Label: " + clientLabel);
        logger.info("Server IP: " + serverIp);
        logger.info("Server Port: " + serverPort);
        logger.info("Source Directories: " + String.join(", ", sourceDirs));
        logger.info("Polling Interval: " + pollingIntervalSeconds + " seconds");
        logger.info("-----------------------------");
        
       
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        logger.info("FileHandlerClient stopped");
       
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            logger.error("Missing configuration file path");
            System.out.println("Usage: java FileHandlerClient <config-file-path>");
            System.exit(1);
        }
        
        String configPath = args[0];
        FileHandlerClient client = new FileHandlerClient(configPath);
        String log4jConfPath = "../config/log4j.properties";
        PropertyConfigurator.configure(log4jConfPath);
        client.start();
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(client::stop));
    }
} 