package com.example.speechtherapy;

public class Therapist {
    private String id;
    private String username;
    private String email;
    public Therapist() {}
    public Therapist(String id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }
    public String getId() {
        return id;
    }
    public String getUsername() {
        return username;
    }
    public String getEmail() {
        return email;
    }
}
