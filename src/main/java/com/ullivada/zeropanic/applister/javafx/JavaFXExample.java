package com.ullivada.zeropanic.applister.javafx;

import com.ullivada.zeropanic.applister.model.InstalledApp;
import com.ullivada.zeropanic.applister.service.AppService;
import com.ullivada.zeropanic.applister.service.AuthService;
import com.ullivada.zeropanic.applister.service.ServiceFactory;
import com.ullivada.zeropanic.applister.service.ThreatService;

import java.util.List;

/**
 * Example usage of services for JavaFX application.
 * This demonstrates how JavaFX controllers should interact with the backend.
 */
public class JavaFXExample {
    
    /**
     * Example: Login flow
     */
    public void loginExample(String username, String password) {
        try {
            ServiceFactory factory = ServiceFactory.getInstance();
            AuthService authService = factory.getAuthService();
            
            AuthService.LoginResult result = authService.login(username, password);
            
            if (result.isSuccess()) {
                System.out.println("Login successful! Username: " + result.getUsername());
                // Store username in JavaFX session/state
                // Navigate to main application screen
            } else {
                System.err.println("Login failed: " + result.getMessage());
                // Show error dialog in JavaFX
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example: Registration flow
     */
    public void registerExample(String username, String password, String confirmPassword) {
        try {
            ServiceFactory factory = ServiceFactory.getInstance();
            AuthService authService = factory.getAuthService();
            
            AuthService.RegistrationResult result = authService.register(username, password, confirmPassword);
            
            if (result.isSuccess()) {
                System.out.println("Registration successful! Username: " + result.getUsername());
                // Navigate to login screen or main app
            } else {
                System.err.println("Registration failed: " + result.getMessage());
                // Show error dialog in JavaFX
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example: Get installed apps (uses cache)
     */
    public void getAppsExample() {
        try {
            ServiceFactory factory = ServiceFactory.getInstance();
            AppService appService = factory.getAppService();
            
            List<InstalledApp> apps = appService.getInstalledApps();
            
            System.out.println("Found " + apps.size() + " installed apps:");
            for (InstalledApp app : apps) {
                System.out.println("  - " + app.name() + " (" + app.source() + ")");
            }
            
            // Populate JavaFX TableView/ListView with apps
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example: Force fresh scan
     */
    public void forceScanExample() {
        try {
            ServiceFactory factory = ServiceFactory.getInstance();
            AppService appService = factory.getAppService();
            
            System.out.println("Scanning system...");
            List<InstalledApp> apps = appService.forceScan();
            
            System.out.println("Scan complete! Found " + apps.size() + " apps");
            // Update JavaFX UI with new data
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example: Get threats (filtered - only matches installed apps)
     */
    public void getThreatsExample() {
        try {
            ServiceFactory factory = ServiceFactory.getInstance();
            ThreatService threatService = factory.getThreatService();
            
            List<ThreatService.ThreatMatch> threats = threatService.getThreats();
            
            if (threats.isEmpty()) {
                System.out.println("No threats found matching your installed apps!");
            } else {
                System.out.println("⚠️  Found " + threats.size() + " threat(s) matching your apps:");
                for (ThreatService.ThreatMatch threat : threats) {
                    System.out.println("\n" + threat.getSeverityIcon() + " " + threat.getTitle());
                    System.out.println("   App: " + threat.getAppName());
                    System.out.println("   Severity: " + threat.getSeverity());
                    System.out.println("   URL: " + threat.getUrl());
                    if (threat.getDescription() != null && !threat.getDescription().isEmpty()) {
                        System.out.println("   Description: " + threat.getDescription());
                    }
                }
            }
            
            // Display threats in JavaFX alert/notification
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Example: Cleanup on application exit
     */
    public void shutdownExample() {
        try {
            ServiceFactory factory = ServiceFactory.getInstance();
            factory.close();
            ServiceFactory.reset();
            System.out.println("Application shutdown complete");
        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
    }
    
    /**
     * Main method for testing
     */
    public static void main(String[] args) {
        JavaFXExample example = new JavaFXExample();
        
        // Test registration
        example.registerExample("testuser", "password123", "password123");
        
        // Test login
        example.loginExample("testuser", "password123");
        
        // Test getting apps
        example.getAppsExample();
        
        // Test threats
        example.getThreatsExample();
        
        // Cleanup
        example.shutdownExample();
    }
}
