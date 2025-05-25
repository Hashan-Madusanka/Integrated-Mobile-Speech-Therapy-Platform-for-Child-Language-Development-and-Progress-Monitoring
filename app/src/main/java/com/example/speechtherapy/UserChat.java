package com.example.speechtherapy;
public class UserChat {
    private String userId;
    private String username;
    private String email;
    public UserChat() {}
    public UserChat(String userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
    }
    public String getUserId() {
        return userId;
    }
    public String getUsername() {
        return username;
    }
    public String getEmail() {
        return email;
    }
}
