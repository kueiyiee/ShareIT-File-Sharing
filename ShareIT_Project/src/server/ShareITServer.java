package server;

import common.*;
import security.EncryptionUtil;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ShareITServer {
    private static final int PORT = 8080;
    private static final String UPLOAD_DIR = "shared_files/";
    private static final String USER_DIR = "users/";
    private static final String LOG_DIR = "logs/";
    
    private ServerSocket serverSocket;
    private boolean running;
    
    // Thread-safe collections
    private static Map<String, User> users = new ConcurrentHashMap<>();
    private static Map<String, FileTransfer> fileTransfers = new ConcurrentHashMap<>();
    private static Map<String, Set<FileTransfer>> userFiles = new ConcurrentHashMap<>();
    private static Map<String, Socket> onlineUsers = new ConcurrentHashMap<>();
    
    public ShareITServer() {
        initializeDirectories();
        loadUsers();
    }
    
    private void initializeDirectories() {
        new File(UPLOAD_DIR).mkdirs();
        new File(USER_DIR).mkdirs();
        new File(LOG_DIR).mkdirs();
    }
    
    private void loadUsers() {
        // Load users from file (in real app, use database)
        try {
            File userFile = new File(USER_DIR + "users.dat");
            if (userFile.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(userFile))) {
                    users.putAll((Map<String, User>) ois.readObject());
                }
            }
        } catch (Exception e) {
            log("Error loading users: " + e.getMessage());
        }
    }
    
    private void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USER_DIR + "users.dat"))) {
            oos.writeObject(users);
        } catch (Exception e) {
            log("Error saving users: " + e.getMessage());
        }
    }
    
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            log("🚀 ShareIT Premium Server started on port " + PORT);
            log("📁 Upload directory: " + new File(UPLOAD_DIR).getAbsolutePath());
            
            while (running) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            log("Server error: " + e.getMessage());
        }
    }
    
    private static void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String logMessage = "[" + timestamp + "] " + message;
        System.out.println(logMessage);
        
        // Write to log file
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_DIR + "server.log", true))) {
            out.println(logMessage);
        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }
    
    class ClientHandler extends Thread {
        private Socket socket;
        private DataInputStream dis;
        private DataOutputStream dos;
        private User currentUser;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());
                
                while (true) {
                    String command = dis.readUTF();
                    
                    switch (command) {
                        case "REGISTER":
                            handleRegister();
                            break;
                        case "LOGIN":
                            handleLogin();
                            break;
                        case "UPLOAD":
                            handleUpload();
                            break;
                        case "DOWNLOAD":
                            handleDownload();
                            break;
                        case "LIST_FILES":
                            handleListFiles();
                            break;
                        case "LIST_USERS":
                            handleListUsers();
                            break;
                        case "SEND_FILE":
                            handleSendFile();
                            break;
                        case "GET_STATS":
                            handleGetStats();
                            break;
                        case "LOGOUT":
                            handleLogout();
                            return;
                        case "DELETE_FILE":
                            handleDeleteFile();
                            break;
                        default:
                            dos.writeUTF("ERROR: Unknown command");
                    }
                }
            } catch (IOException e) {
                log("Client disconnected: " + (currentUser != null ? currentUser.getUsername() : "Unknown"));
            } finally {
                if (currentUser != null) {
                    currentUser.setOnline(false);
                    onlineUsers.remove(currentUser.getUsername());
                }
            }
        }
        
        private void handleRegister() throws IOException {
            try {
                String username = dis.readUTF();
                String password = dis.readUTF();
                String email = dis.readUTF();
                
                if (users.containsKey(username)) {
                    dos.writeUTF("ERROR: Username already exists");
                    return;
                }
                
                String passwordHash = EncryptionUtil.hashPassword(password);
                User newUser = new User(username, passwordHash, email);
                users.put(username, newUser);
                saveUsers();
                
                dos.writeUTF("SUCCESS: Registration successful");
                log("New user registered: " + username);
            } catch (Exception e) {
                dos.writeUTF("ERROR: Registration failed");
            }
        }
        
        private void handleLogin() throws IOException {
            try {
                String username = dis.readUTF();
                String password = dis.readUTF();
                
                User user = users.get(username);
                if (user == null || !user.getPasswordHash().equals(EncryptionUtil.hashPassword(password))) {
                    dos.writeUTF("ERROR: Invalid credentials");
                    return;
                }
                
                currentUser = user;
                currentUser.setOnline(true);
                onlineUsers.put(username, socket);
                
                dos.writeUTF("SUCCESS: Login successful");
                log("User logged in: " + username);
            } catch (Exception e) {
                dos.writeUTF("ERROR: Login failed");
            }
        }
        
        private void handleUpload() throws IOException {
            if (currentUser == null) {
                dos.writeUTF("ERROR: Not authenticated");
                return;
            }
            
            try {
                String fileName = dis.readUTF();
                long fileSize = dis.readLong();
                String receiver = dis.readUTF(); // Can be "public" or specific username
                
                if (!currentUser.canUpload(fileSize)) {
                    dos.writeUTF("ERROR: Storage limit exceeded");
                    return;
                }
                
                String fileId = UUID.randomUUID().toString();
                String filePath = UPLOAD_DIR + fileId + "_" + fileName;
                
                FileTransfer transfer = new FileTransfer(fileId, fileName, fileSize, currentUser.getUsername(), receiver);
                fileTransfers.put(fileId, transfer);
                
                // Update user's file list
                userFiles.computeIfAbsent(currentUser.getUsername(), k -> new HashSet<>()).add(transfer);
                
                // Save file
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    byte[] buffer = new byte[8192];
                    long totalRead = 0;
                    int bytesRead;
                    
                    while (totalRead < fileSize && (bytesRead = dis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        totalRead += bytesRead;
                    }
                }
                
                // Update user storage
                currentUser.setStorageUsed(currentUser.getStorageUsed() + fileSize);
                
                transfer.setStatus("COMPLETED");
                dos.writeUTF("SUCCESS");
                dos.writeUTF(fileId);
                
                log("File uploaded: " + fileName + " by " + currentUser.getUsername() + " to " + receiver);
                
            } catch (Exception e) {
                dos.writeUTF("ERROR: Upload failed");
            }
        }
        
        private void handleDownload() throws IOException {
            if (currentUser == null) {
                dos.writeUTF("ERROR: Not authenticated");
                return;
            }
            
            String fileId = dis.readUTF();
            FileTransfer transfer = fileTransfers.get(fileId);
            
            if (transfer == null) {
                dos.writeUTF("ERROR: File not found");
                return;
            }
            
            // Check permissions
            if (!transfer.getReceiver().equals("public") && 
                !transfer.getReceiver().equals(currentUser.getUsername()) &&
                !transfer.getSender().equals(currentUser.getUsername())) {
                dos.writeUTF("ERROR: Access denied");
                return;
            }
            
            String filePath = UPLOAD_DIR + fileId + "_" + transfer.getFileName();
            File file = new File(filePath);
            
            if (!file.exists()) {
                dos.writeUTF("ERROR: File not found on server");
                return;
            }
            
            dos.writeUTF("SUCCESS");
            dos.writeUTF(transfer.getFileName());
            dos.writeLong(file.length());
            
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
            }
            
            log("File downloaded: " + transfer.getFileName() + " by " + currentUser.getUsername());
        }
        
        private void handleListFiles() throws IOException {
            if (currentUser == null) {
                dos.writeUTF("ERROR: Not authenticated");
                return;
            }
            
            // Get files accessible to current user
            List<FileTransfer> accessibleFiles = new ArrayList<>();
            for (FileTransfer transfer : fileTransfers.values()) {
                if (transfer.getReceiver().equals("public") ||
                    transfer.getReceiver().equals(currentUser.getUsername()) ||
                    transfer.getSender().equals(currentUser.getUsername())) {
                    accessibleFiles.add(transfer);
                }
            }
            
            dos.writeInt(accessibleFiles.size());
            for (FileTransfer transfer : accessibleFiles) {
                dos.writeUTF(transfer.getFileId());
                dos.writeUTF(transfer.getFileName());
                dos.writeUTF(transfer.getSender());
                dos.writeUTF(transfer.getReceiver());
                dos.writeLong(transfer.getFileSize());
                dos.writeUTF(transfer.getFileType());
                dos.writeUTF(transfer.getTimestamp().toString());
            }
        }
        
        private void handleListUsers() throws IOException {
            if (currentUser == null) {
                dos.writeUTF("ERROR: Not authenticated");
                return;
            }
            
            dos.writeInt(users.size());
            for (User user : users.values()) {
                dos.writeUTF(user.getUsername());
                dos.writeUTF(user.getEmail());
                dos.writeBoolean(user.isOnline());
                dos.writeLong(user.getStorageUsed());
                dos.writeLong(user.getStorageLimit());
            }
        }
        
        private void handleSendFile() throws IOException {
            // Similar to upload but with specific receiver
            handleUpload();
        }
        
        private void handleGetStats() throws IOException {
            if (currentUser == null) {
                dos.writeUTF("ERROR: Not authenticated");
                return;
            }
            
            dos.writeUTF(currentUser.getUsername());
            dos.writeUTF(currentUser.getEmail());
            dos.writeLong(currentUser.getStorageUsed());
            dos.writeLong(currentUser.getStorageLimit());
            dos.writeInt(userFiles.getOrDefault(currentUser.getUsername(), new HashSet<>()).size());
            dos.writeInt(onlineUsers.size());
        }
        
        private void handleDeleteFile() throws IOException {
            if (currentUser == null) {
                dos.writeUTF("ERROR: Not authenticated");
                return;
            }
            
            String fileId = dis.readUTF();
            FileTransfer transfer = fileTransfers.get(fileId);
            
            if (transfer == null || !transfer.getSender().equals(currentUser.getUsername())) {
                dos.writeUTF("ERROR: File not found or access denied");
                return;
            }
            
            // Delete physical file
            String filePath = UPLOAD_DIR + fileId + "_" + transfer.getFileName();
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
            
            // Update storage
            currentUser.setStorageUsed(currentUser.getStorageUsed() - transfer.getFileSize());
            
            // Remove from collections
            fileTransfers.remove(fileId);
            userFiles.get(currentUser.getUsername()).remove(transfer);
            
            dos.writeUTF("SUCCESS: File deleted");
            log("File deleted: " + transfer.getFileName() + " by " + currentUser.getUsername());
        }
        
        private void handleLogout() throws IOException {
            if (currentUser != null) {
                currentUser.setOnline(false);
                onlineUsers.remove(currentUser.getUsername());
                log("User logged out: " + currentUser.getUsername());
            }
            dos.writeUTF("SUCCESS: Logged out");
        }
    }
    
    public static void main(String[] args) {
        new ShareITServer().start();
    }
}