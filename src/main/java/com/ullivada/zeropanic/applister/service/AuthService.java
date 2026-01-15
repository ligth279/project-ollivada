package com.ullivada.zeropanic.applister.service;

import com.ullivada.zeropanic.applister.database.DatabaseService;

import java.sql.SQLException;

/**
 * Authentication service for JavaFX frontend.
 * Handles user login and registration.
 */
public class AuthService {
    private final DatabaseService db;
    
    public AuthService(DatabaseService db) {
        this.db = db;
    }
    
    /**
     * Login with username and password.
     * @return LoginResult with success status and username
     */
    public LoginResult login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return new LoginResult(false, null, "Username cannot be empty");
        }
        
        if (password == null || password.isEmpty()) {
            return new LoginResult(false, null, "Password cannot be empty");
        }
        
        try {
            boolean authenticated = db.authenticate(username, password);
            if (authenticated) {
                return new LoginResult(true, username, "Login successful");
            } else {
                // Check if user exists to provide better error message
                if (db.userExists(username)) {
                    return new LoginResult(false, null, "Incorrect password");
                } else {
                    return new LoginResult(false, null, "User not found");
                }
            }
        } catch (SQLException e) {
            return new LoginResult(false, null, "Database error: " + e.getMessage());
        }
    }
    
    /**
     * Register a new user.
     * @return RegistrationResult with success status and username
     */
    public RegistrationResult register(String username, String password, String confirmPassword) {
        if (username == null || username.trim().isEmpty()) {
            return new RegistrationResult(false, null, "Username cannot be empty");
        }
        
        if (password == null || password.isEmpty()) {
            return new RegistrationResult(false, null, "Password cannot be empty");
        }
        
        if (!password.equals(confirmPassword)) {
            return new RegistrationResult(false, null, "Passwords do not match");
        }
        
        if (password.length() < 6) {
            return new RegistrationResult(false, null, "Password must be at least 6 characters");
        }
        
        try {
            if (db.userExists(username)) {
                return new RegistrationResult(false, null, "Username already exists");
            }
            
            db.createUser(username, password);
            return new RegistrationResult(true, username, "Registration successful");
        } catch (SQLException e) {
            return new RegistrationResult(false, null, "Database error: " + e.getMessage());
        }
    }
    
    /**
     * Result object for login operations.
     */
    public static class LoginResult {
        private final boolean success;
        private final String username;
        private final String message;
        
        public LoginResult(boolean success, String username, String message) {
            this.success = success;
            this.username = username;
            this.message = message;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    /**
     * Result object for registration operations.
     */
    public static class RegistrationResult {
        private final boolean success;
        private final String username;
        private final String message;
        
        public RegistrationResult(boolean success, String username, String message) {
            this.success = success;
            this.username = username;
            this.message = message;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
