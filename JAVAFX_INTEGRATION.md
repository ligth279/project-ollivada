# JavaFX Integration Guide

## Service Layer Architecture

The application now has a clean service layer that JavaFX can call directly (no HTTP/REST needed).

### Services Available

1. **AuthService** - User authentication
2. **AppService** - Installed applications management
3. **ThreatService** - Security threat detection
4. **ServiceFactory** - Creates and manages all services

---

## Quick Start for JavaFX

### 1. Get Service Factory

```java
import com.ullivada.zeropanic.applister.service.ServiceFactory;

ServiceFactory factory = ServiceFactory.getInstance();
```

### 2. Authentication

#### Login
```java
AuthService authService = factory.getAuthService();
AuthService.LoginResult result = authService.login(username, password);

if (result.isSuccess()) {
    String loggedInUser = result.getUsername();
    // Store username, navigate to main screen
} else {
    String errorMessage = result.getMessage();
    // Show error alert
}
```

#### Register New User
```java
AuthService authService = factory.getAuthService();
AuthService.RegistrationResult result = authService.register(
    username, 
    password, 
    confirmPassword
);

if (result.isSuccess()) {
    String newUser = result.getUsername();
    // Auto-login or navigate to login
} else {
    String errorMessage = result.getMessage();
    // Show error (e.g., "Passwords do not match")
}
```

### 3. Get Installed Apps

#### With Cache (20-hour)
```java
AppService appService = factory.getAppService();
List<InstalledApp> apps = appService.getInstalledApps();

// Populate TableView
for (InstalledApp app : apps) {
    String name = app.name();
    String source = app.source(); // "snap", "flatpak", "windows-registry"
    Optional<String> version = app.version();
}
```

#### Force Fresh Scan
```java
AppService appService = factory.getAppService();
List<InstalledApp> apps = appService.forceScan(); // Ignore cache
```

#### Check if Cache Available
```java
AppService appService = factory.getAppService();
boolean hasCache = appService.hasCachedData();
```

### 4. Get Threats (Filtered)

```java
ThreatService threatService = factory.getThreatService();
List<ThreatService.ThreatMatch> threats = threatService.getThreats();

if (threats.isEmpty()) {
    // Show "No threats" message
} else {
    // Display threats in ListView/TableView
    for (ThreatService.ThreatMatch threat : threats) {
        String title = threat.getTitle();
        String url = threat.getUrl();
        String appName = threat.getAppName();
        String severity = threat.getSeverity(); // "critical", "high", "medium", "low"
        String description = threat.getDescription();
        String icon = threat.getSeverityIcon(); // "🔴", "🟠", "🟡", "🟢"
    }
}
```

### 5. Application Shutdown

```java
ServiceFactory factory = ServiceFactory.getInstance();
factory.close(); // Close database connections
ServiceFactory.reset(); // Reset singleton
```

---

## JavaFX Controller Example

```java
package com.ullivada.zeropanic.javafx.controllers;

import com.ullivada.zeropanic.applister.service.*;
import com.ullivada.zeropanic.applister.model.InstalledApp;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class MainController {
    
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TableView<InstalledApp> appsTable;
    @FXML private ListView<ThreatService.ThreatMatch> threatsList;
    
    private ServiceFactory factory;
    private String currentUser;
    
    @FXML
    public void initialize() {
        try {
            factory = ServiceFactory.getInstance();
        } catch (Exception e) {
            showError("Failed to initialize services", e.getMessage());
        }
    }
    
    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        try {
            AuthService auth = factory.getAuthService();
            AuthService.LoginResult result = auth.login(username, password);
            
            if (result.isSuccess()) {
                currentUser = result.getUsername();
                loadApps();
                loadThreats();
            } else {
                showError("Login Failed", result.getMessage());
            }
        } catch (Exception e) {
            showError("Error", e.getMessage());
        }
    }
    
    @FXML
    private void handleScan() {
        try {
            AppService appService = factory.getAppService();
            var apps = appService.forceScan();
            appsTable.getItems().setAll(apps);
            loadThreats(); // Refresh threats after scan
        } catch (Exception e) {
            showError("Scan Failed", e.getMessage());
        }
    }
    
    private void loadApps() {
        try {
            AppService appService = factory.getAppService();
            var apps = appService.getInstalledApps();
            appsTable.getItems().setAll(apps);
        } catch (Exception e) {
            showError("Error", e.getMessage());
        }
    }
    
    private void loadThreats() {
        try {
            ThreatService threatService = factory.getThreatService();
            var threats = threatService.getThreats();
            threatsList.getItems().setAll(threats);
            
            if (!threats.isEmpty()) {
                showWarning("Security Alert", 
                    threats.size() + " threat(s) found!");
            }
        } catch (Exception e) {
            showError("Error", e.getMessage());
        }
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
```

---

## Data Models

### InstalledApp
```java
public record InstalledApp(
    String name,           // App name
    String source,         // "snap", "flatpak", "apt", "desktop", "windows-registry"
    Optional<String> id,   // App ID
    Optional<String> version,
    Optional<String> origin
)
```

### ThreatMatch
```java
public static class ThreatMatch {
    String getTitle()          // Threat title
    String getUrl()            // Link to more info
    String getAppName()        // Affected app name
    String getSeverity()       // "critical", "high", "medium", "low"
    String getDescription()    // Details
    String getSeverityIcon()   // "🔴", "🟠", "🟡", "🟢"
}
```

---

## Features

✅ **Cross-platform** - Works on Windows 11 and Linux
✅ **Smart caching** - 20-hour app cache, 5-hour threat cache
✅ **Filtered threats** - Only shows threats matching installed apps
✅ **No HTTP** - Direct Java method calls (same JVM)
✅ **Type-safe** - Return POJOs, not JSON
✅ **Thread-safe** - Services can be called from JavaFX thread

---

## Example JavaFX Application Structure

```
src/main/java/com/yourcompany/
├── javafx/
│   ├── Main.java                    # JavaFX Application entry
│   ├── controllers/
│   │   ├── LoginController.java
│   │   ├── MainController.java
│   │   └── ThreatsController.java
│   └── views/
│       ├── login.fxml
│       ├── main.fxml
│       └── threats.fxml
└── (use existing applister services)
```

---

## Testing the Service Layer

Run the example:
```bash
java -cp target/app-lister-cli-0.1.0-SNAPSHOT.jar \
  com.ullivada.zeropanic.applister.javafx.JavaFXExample
```

This will test all service methods without JavaFX UI.
