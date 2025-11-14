package client;

import common.FileTransfer;
import common.User;
import security.EncryptionUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ShareITClient extends JFrame {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    
    private User currentUser;
    private JPanel mainPanel;
    private CardLayout cardLayout;
    
    // UI Components
    private JTextField loginUsername, loginPassword, registerUsername, registerPassword, registerEmail;
    private JButton loginBtn, registerBtn, logoutBtn, uploadBtn, downloadBtn, deleteBtn, refreshBtn;
    private JTable filesTable, usersTable;
    private JLabel welcomeLabel, statsLabel;
    private JProgressBar progressBar;
    
    public ShareITClient() {
        initializeUI();
        connectToServer();
    }
    
    private void initializeUI() {
        setTitle("ShareIT Premium - Secure File Sharing");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        // Set modern look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Main panel with card layout
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        createLoginPanel();
        createRegisterPanel();
        createDashboardPanel();
        
        add(mainPanel);
        showLoginPanel();
    }
    
    private void createLoginPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(new EmptyBorder(50, 100, 50, 100));
        
        // Title
        JLabel titleLabel = new JLabel("ShareIT Premium", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(new Color(0, 102, 204));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Form panel
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setBackground(new Color(240, 240, 240));
        formPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
        
        loginUsername = new JTextField();
        loginPassword = new JPasswordField();
        
        formPanel.add(new JLabel("Username:"));
        formPanel.add(loginUsername);
        formPanel.add(new JLabel("Password:"));
        formPanel.add(loginPassword);
        
        loginBtn = new JButton("Login");
        loginBtn.setBackground(new Color(0, 102, 204));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFont(new Font("Arial", Font.BOLD, 14));
        
        JButton gotoRegisterBtn = new JButton("Create Account");
        gotoRegisterBtn.setBackground(new Color(76, 175, 80));
        gotoRegisterBtn.setForeground(Color.WHITE);
        
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        buttonPanel.add(loginBtn);
        buttonPanel.add(gotoRegisterBtn);
        formPanel.add(buttonPanel);
        
        panel.add(formPanel, BorderLayout.CENTER);
        
        // Action listeners
        loginBtn.addActionListener(e -> login());
        gotoRegisterBtn.addActionListener(e -> showRegisterPanel());
        
        mainPanel.add(panel, "LOGIN");
    }
    
    private void createRegisterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(new EmptyBorder(50, 100, 50, 100));
        
        JLabel titleLabel = new JLabel("Create Account", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(new Color(0, 102, 204));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        formPanel.setBackground(new Color(240, 240, 240));
        formPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
        
        registerUsername = new JTextField();
        registerPassword = new JPasswordField();
        registerEmail = new JTextField();
        
        formPanel.add(new JLabel("Username:"));
        formPanel.add(registerUsername);
        formPanel.add(new JLabel("Password:"));
        formPanel.add(registerPassword);
        formPanel.add(new JLabel("Email:"));
        formPanel.add(registerEmail);
        
        registerBtn = new JButton("Register");
        registerBtn.setBackground(new Color(76, 175, 80));
        registerBtn.setForeground(Color.WHITE);
        
        JButton gotoLoginBtn = new JButton("Back to Login");
        gotoLoginBtn.setBackground(new Color(158, 158, 158));
        gotoLoginBtn.setForeground(Color.WHITE);
        
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        buttonPanel.add(registerBtn);
        buttonPanel.add(gotoLoginBtn);
        formPanel.add(buttonPanel);
        
        panel.add(formPanel, BorderLayout.CENTER);
        
        registerBtn.addActionListener(e -> register());
        gotoLoginBtn.addActionListener(e -> showLoginPanel());
        
        mainPanel.add(panel, "REGISTER");
    }
    
    private void createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(0, 102, 204));
        headerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        
        welcomeLabel = new JLabel("Welcome to ShareIT Premium");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 20));
        welcomeLabel.setForeground(Color.WHITE);
        
        logoutBtn = new JButton("Logout");
        logoutBtn.setBackground(new Color(244, 67, 54));
        logoutBtn.setForeground(Color.WHITE);
        
        headerPanel.add(welcomeLabel, BorderLayout.WEST);
        headerPanel.add(logoutBtn, BorderLayout.EAST);
        
        // Stats panel
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statsPanel.setBackground(new Color(224, 224, 224));
        statsPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        
        statsLabel = new JLabel();
        statsLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statsPanel.add(statsLabel);
        
        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setBorder(new EmptyBorder(10, 20, 10, 20));
        
        uploadBtn = new JButton("📤 Upload File");
        downloadBtn = new JButton("📥 Download");
        deleteBtn = new JButton("🗑️ Delete");
        refreshBtn = new JButton("🔄 Refresh");
        
        uploadBtn.setBackground(new Color(76, 175, 80));
        downloadBtn.setBackground(new Color(33, 150, 243));
        deleteBtn.setBackground(new Color(244, 67, 54));
        refreshBtn.setBackground(new Color(158, 158, 158));
        
        uploadBtn.setForeground(Color.WHITE);
        downloadBtn.setForeground(Color.WHITE);
        deleteBtn.setForeground(Color.WHITE);
        refreshBtn.setForeground(Color.WHITE);
        
        toolbar.add(uploadBtn);
        toolbar.add(downloadBtn);
        toolbar.add(deleteBtn);
        toolbar.add(refreshBtn);
        
        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        
        // Tabbed pane for files and users
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Files tab
        String[] fileColumns = {"ID", "File Name", "Sender", "Receiver", "Size", "Type", "Date"};
        DefaultTableModel filesModel = new DefaultTableModel(fileColumns, 0);
        filesTable = new JTable(filesModel);
        filesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane filesScroll = new JScrollPane(filesTable);
        
        // Users tab
        String[] userColumns = {"Username", "Email", "Status", "Storage Used"};
        DefaultTableModel usersModel = new DefaultTableModel(userColumns, 0);
        usersTable = new JTable(usersModel);
        JScrollPane usersScroll = new JScrollPane(usersTable);
        
        tabbedPane.addTab("📁 Files", filesScroll);
        tabbedPane.addTab("👥 Users", usersScroll);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(statsPanel, BorderLayout.CENTER);
        panel.add(toolbar, BorderLayout.SOUTH);
        panel.add(tabbedPane, BorderLayout.CENTER);
        panel.add(progressBar, BorderLayout.SOUTH);
        
        // Action listeners
        logoutBtn.addActionListener(e -> logout());
        uploadBtn.addActionListener(e -> uploadFile());
        downloadBtn.addActionListener(e -> downloadFile());
        deleteBtn.addActionListener(e -> deleteFile());
        refreshBtn.addActionListener(e -> refreshData());
        
        mainPanel.add(panel, "DASHBOARD");
    }
    
    private void connectToServer() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            System.out.println("Connected to ShareIT server");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Cannot connect to server: " + e.getMessage(), 
                "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
    
    private void login() {
        try {
            String username = loginUsername.getText();
            String password = loginPassword.getText();
            
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter username and password");
                return;
            }
            
            dos.writeUTF("LOGIN");
            dos.writeUTF(username);
            dos.writeUTF(password);
            
            String response = dis.readUTF();
            if (response.startsWith("SUCCESS")) {
                showDashboard();
                loadUserData();
            } else {
                JOptionPane.showMessageDialog(this, response.substring(6));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Login failed: " + e.getMessage());
        }
    }
    
    private void register() {
        try {
            String username = registerUsername.getText();
            String password = registerPassword.getText();
            String email = registerEmail.getText();
            
            if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields");
                return;
            }
            
            dos.writeUTF("REGISTER");
            dos.writeUTF(username);
            dos.writeUTF(password);
            dos.writeUTF(email);
            
            String response = dis.readUTF();
            if (response.startsWith("SUCCESS")) {
                JOptionPane.showMessageDialog(this, "Registration successful! Please login.");
                showLoginPanel();
            } else {
                JOptionPane.showMessageDialog(this, response.substring(6));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Registration failed: " + e.getMessage());
        }
    }
    
    private void showDashboard() {
        cardLayout.show(mainPanel, "DASHBOARD");
    }
    
    private void showLoginPanel() {
        cardLayout.show(mainPanel, "LOGIN");
    }
    
    private void showRegisterPanel() {
        cardLayout.show(mainPanel, "REGISTER");
    }
    
    private void loadUserData() {
        refreshFiles();
        refreshUsers();
        updateStats();
    }
    
    private void refreshFiles() {
        try {
            dos.writeUTF("LIST_FILES");
            int fileCount = dis.readInt();
            
            DefaultTableModel model = (DefaultTableModel) filesTable.getModel();
            model.setRowCount(0);
            
            DecimalFormat sizeFormat = new DecimalFormat("#,##0.00");
            
            for (int i = 0; i < fileCount; i++) {
                String fileId = dis.readUTF();
                String fileName = dis.readUTF();
                String sender = dis.readUTF();
                String receiver = dis.readUTF();
                long fileSize = dis.readLong();
                String fileType = dis.readUTF();
                String timestamp = dis.readUTF();
                
                String sizeStr = fileSize < 1024 ? fileSize + " B" : 
                               fileSize < 1024 * 1024 ? sizeFormat.format(fileSize / 1024.0) + " KB" :
                               sizeFormat.format(fileSize / (1024.0 * 1024.0)) + " MB";
                
                model.addRow(new Object[]{fileId, fileName, sender, receiver, sizeStr, fileType, timestamp});
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading files: " + e.getMessage());
        }
    }
    
    private void refreshUsers() {
        try {
            dos.writeUTF("LIST_USERS");
            int userCount = dis.readInt();
            
            DefaultTableModel model = (DefaultTableModel) usersTable.getModel();
            model.setRowCount(0);
            
            for (int i = 0; i < userCount; i++) {
                String username = dis.readUTF();
                String email = dis.readUTF();
                boolean isOnline = dis.readBoolean();
                long storageUsed = dis.readLong();
                long storageLimit = dis.readLong();
                
                String status = isOnline ? "🟢 Online" : "🔴 Offline";
                String storageStr = formatStorage(storageUsed) + " / " + formatStorage(storageLimit);
                
                model.addRow(new Object[]{username, email, status, storageStr});
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading users: " + e.getMessage());
        }
    }
    
    private void updateStats() {
        try {
            dos.writeUTF("GET_STATS");
            String username = dis.readUTF();
            String email = dis.readUTF();
            long storageUsed = dis.readLong();
            long storageLimit = dis.readLong();
            int fileCount = dis.readInt();
            int onlineUsers = dis.readInt();
            
            welcomeLabel.setText("Welcome, " + username + "! (" + email + ")");
            
            String stats = String.format("Storage: %s / %s | Files: %d | Online Users: %d",
                formatStorage(storageUsed), formatStorage(storageLimit), fileCount, onlineUsers);
            statsLabel.setText(stats);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading stats: " + e.getMessage());
        }
    }
    
    private String formatStorage(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    private void uploadFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            // Ask for receiver
            String receiver = (String) JOptionPane.showInputDialog(this,
                "Enter receiver username (or 'public' for everyone):",
                "Share File",
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                "public");
            
            if (receiver == null) return;
            
            new Thread(() -> {
                try {
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                    
                    dos.writeUTF("UPLOAD");
                    dos.writeUTF(file.getName());
                    dos.writeLong(file.length());
                    dos.writeUTF(receiver);
                    
                    // Send file content
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        long totalSent = 0;
                        
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            dos.write(buffer, 0, bytesRead);
                            totalSent += bytesRead;
                        }
                    }
                    
                    String response = dis.readUTF();
                    if (response.equals("SUCCESS")) {
                        String fileId = dis.readUTF();
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, "File uploaded successfully!\nFile ID: " + fileId);
                            refreshData();
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> 
                            JOptionPane.showMessageDialog(this, "Upload failed: " + response));
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(this, "Upload error: " + e.getMessage()));
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setVisible(false);
                        progressBar.setIndeterminate(false);
                    });
                }
            }).start();
        }
    }
    
    private void downloadFile() {
        int selectedRow = filesTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a file to download");
            return;
        }
        
        String fileId = (String) filesTable.getValueAt(selectedRow, 0);
        String fileName = (String) filesTable.getValueAt(selectedRow, 1);
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(fileName));
        int result = fileChooser.showSaveDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File outputFile = fileChooser.getSelectedFile();
            
            new Thread(() -> {
                try {
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                    
                    dos.writeUTF("DOWNLOAD");
                    dos.writeUTF(fileId);
                    
                    String response = dis.readUTF();
                    if (response.equals("SUCCESS")) {
                        String actualFileName = dis.readUTF();
                        long fileSize = dis.readLong();
                        
                        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            long totalRead = 0;
                            
                            while (totalRead < fileSize && (bytesRead = dis.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                                totalRead += bytesRead;
                            }
                        }
                        
                        SwingUtilities.invokeLater(() -> 
                            JOptionPane.showMessageDialog(this, "File downloaded successfully!"));
                    } else {
                        String error = dis.readUTF();
                        SwingUtilities.invokeLater(() -> 
                            JOptionPane.showMessageDialog(this, "Download failed: " + error));
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(this, "Download error: " + e.getMessage()));
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setVisible(false);
                        progressBar.setIndeterminate(false);
                    });
                }
            }).start();
        }
    }
    
    private void deleteFile() {
        int selectedRow = filesTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a file to delete");
            return;
        }
        
        String fileId = (String) filesTable.getValueAt(selectedRow, 0);
        String fileName = (String) filesTable.getValueAt(selectedRow, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete '" + fileName + "'?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                dos.writeUTF("DELETE_FILE");
                dos.writeUTF(fileId);
                
                String response = dis.readUTF();
                if (response.startsWith("SUCCESS")) {
                    JOptionPane.showMessageDialog(this, "File deleted successfully");
                    refreshData();
                } else {
                    JOptionPane.showMessageDialog(this, response.substring(6));
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Delete error: " + e.getMessage());
            }
        }
    }
    
    private void refreshData() {
        refreshFiles();
        refreshUsers();
        updateStats();
    }
    
    private void logout() {
        try {
            dos.writeUTF("LOGOUT");
            dis.readUTF(); // Read response
            
            showLoginPanel();
            loginUsername.setText("");
            loginPassword.setText("");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Logout error: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ShareITClient().setVisible(true);
        });
    }
}