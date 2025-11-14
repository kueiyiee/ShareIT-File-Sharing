package common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class User implements Serializable {
    private String username;
    private String passwordHash;
    private String email;
    private LocalDateTime registrationDate;
    private boolean isOnline;
    private long storageUsed;
    private long storageLimit;

    public User(String username, String passwordHash, String email) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.registrationDate = LocalDateTime.now();
        this.isOnline = false;
        this.storageUsed = 0;
        this.storageLimit = 100 * 1024 * 1024; // 100MB default
    }

    // Getters and setters
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getEmail() { return email; }
    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public boolean isOnline() { return isOnline; }
    public long getStorageUsed() { return storageUsed; }
    public long getStorageLimit() { return storageLimit; }
    
    public void setOnline(boolean online) { isOnline = online; }
    public void setStorageUsed(long storageUsed) { this.storageUsed = storageUsed; }
    
    public boolean canUpload(long fileSize) {
        return (storageUsed + fileSize) <= storageLimit;
    }
}