package com.smd.shared;

public class User {
    private String id;  // Firebase UID as the user ID
    private String email;

    // Empty constructor for Firebase
    public User() {}

    public User(String id, String email) {
        this.id = id;
        this.email = email;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }
}
