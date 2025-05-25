package com.example.speechtherapy;
public class ChatMessage {
    private String senderId;
    private String message;
    private String text;
    private String fileLink;
    private String originalFileName;
    public ChatMessage() {
        // Default constructor required for calls to DataSnapshot.getValue(ChatMessage.class)
    }
    public ChatMessage(String senderId, String message) {
        this.senderId = senderId;
        this.message = message;
    }
    public String getSenderId() {
        return senderId;
    }
    public String getMessage() {
        return message;
    }
    // Getters and setters for fileLink
    // Getters and setters for fileLink
    public String getFileLink() {
        return fileLink;
    }

    public void setFileLink(String fileLink) {
        this.fileLink = fileLink;
    }

    public String getFileName() {
        return fileLink != null ? fileLink.substring(fileLink.lastIndexOf('/') + 1) : null;
    }
    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

}

