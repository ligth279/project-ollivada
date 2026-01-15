package com.ullivada.zeropanic.applister.javafx.controllers;

import com.ullivada.zeropanic.applister.service.AuthService;
import com.ullivada.zeropanic.applister.service.ServiceFactory;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for the login/registration screen.
 */
public class LoginController {
    
    @FXML private VBox loginForm;
    @FXML private VBox registerForm;
    
    // Login fields
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    
    // Register fields
    @FXML private TextField registerUsernameField;
    @FXML private PasswordField registerPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label registerErrorLabel;
    
    private ServiceFactory serviceFactory;
    
    @FXML
    public void initialize() {
        try {
            serviceFactory = ServiceFactory.getInstance();
        } catch (Exception e) {
            showError("Failed to initialize application: " + e.getMessage());
        }
        
        // Enter key on password field triggers login
        passwordField.setOnAction(event -> handleLogin());
        confirmPasswordField.setOnAction(event -> handleRegister());
    }
    
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }
        
        try {
            AuthService authService = serviceFactory.getAuthService();
            AuthService.LoginResult result = authService.login(username, password);
            
            if (result.isSuccess()) {
                // Login successful - navigate to main app
                navigateToMainApp(result.getUsername());
            } else {
                showError(result.getMessage());
            }
        } catch (Exception e) {
            showError("Login failed: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRegister() {
        String username = registerUsernameField.getText().trim();
        String password = registerPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showRegisterError("Please fill in all fields");
            return;
        }
        
        try {
            AuthService authService = serviceFactory.getAuthService();
            AuthService.RegistrationResult result = authService.register(username, password, confirmPassword);
            
            if (result.isSuccess()) {
                // Registration successful - auto-login
                navigateToMainApp(result.getUsername());
            } else {
                showRegisterError(result.getMessage());
            }
        } catch (Exception e) {
            showRegisterError("Registration failed: " + e.getMessage());
        }
    }
    
    @FXML
    private void showRegisterForm() {
        loginForm.setVisible(false);
        loginForm.setManaged(false);
        registerForm.setVisible(true);
        registerForm.setManaged(true);
        errorLabel.setVisible(false);
        registerErrorLabel.setVisible(false);
        
        // Clear fields
        registerUsernameField.clear();
        registerPasswordField.clear();
        confirmPasswordField.clear();
    }
    
    @FXML
    private void showLoginForm() {
        registerForm.setVisible(false);
        registerForm.setManaged(false);
        loginForm.setVisible(true);
        loginForm.setManaged(true);
        errorLabel.setVisible(false);
        registerErrorLabel.setVisible(false);
        
        // Clear fields
        usernameField.clear();
        passwordField.clear();
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
    
    private void showRegisterError(String message) {
        registerErrorLabel.setText(message);
        registerErrorLabel.setVisible(true);
    }
    
    private void navigateToMainApp(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            
            // Pass username to main controller
            MainController mainController = loader.getController();
            mainController.setUsername(username);
            mainController.loadData();
            
            // Switch scene
            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("ZeroPanic - Dashboard");
        } catch (Exception e) {
            showError("Failed to load main application: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
