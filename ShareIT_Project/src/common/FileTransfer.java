package common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class FileTransfer implements Serializable {
    private String fileId;
    private String fileName;
    private long fileSize;
    private String sender;
    private String receiver;
    private LocalDateTime timestamp;
    private String status;
    private String fileType;

    public FileTransfer(String fileId, String fileName, long fileSize, String sender, String receiver) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.sender = sender;
        this.receiver = receiver;
        this.timestamp = LocalDateTime.now();
        this.status = "PENDING";
        this.fileType = getFileExtension(fileName);
    }

    private String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        return lastIndex > 0 ? fileName.substring(lastIndex + 1).toUpperCase() : "UNKNOWN";
    }

    // Getters and setters
    public String getFileId() { return fileId; }
    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
    public String getFileType() { return fileType; }
    
    public void setStatus(String status) { this.status = status; }
}