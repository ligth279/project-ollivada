package com.ullivada.zeropanic.applister.javafx.controllers;

import com.ullivada.zeropanic.applister.model.InstalledApp;
import com.ullivada.zeropanic.applister.service.AppService;
import com.ullivada.zeropanic.applister.service.ServiceFactory;
import com.ullivada.zeropanic.applister.service.ThreatService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Main application controller with alerts and apps panels.
 */
public class MainController {
    
    // Sidebar buttons
    @FXML private Button alertsButton;
    @FXML private Button appsButton;
    @FXML private Label usernameLabel;
    
    // Content panes
    @FXML private StackPane contentPane;
    @FXML private VBox alertsPanel;
    @FXML private VBox appsPanel;
    
    // Alerts panel components
    @FXML private Label totalThreatsLabel;
    @FXML private Label totalAppsLabel;
    @FXML private Label lastCheckLabel;
    @FXML private VBox threatsContainer;
    @FXML private Label noThreatsLabel;
    
    // Apps panel components
    @FXML private TableView<InstalledApp> appsTable;
    @FXML private TableColumn<InstalledApp, String> appNameColumn;
    @FXML private TableColumn<InstalledApp, String> appSourceColumn;
    @FXML private TableColumn<InstalledApp, String> appVersionColumn;
    @FXML private TableColumn<InstalledApp, String> appIdColumn;
    @FXML private TextField searchField;
    @FXML private Label appCountLabel;
    
    private ServiceFactory serviceFactory;
    private String currentUsername;
    private List<InstalledApp> allApps;
    
    @FXML
    public void initialize() {
        try {
            serviceFactory = ServiceFactory.getInstance();
            setupAppsTable();
        } catch (Exception e) {
            showErrorAlert("Initialization Error", "Failed to initialize: " + e.getMessage());
        }
    }
    
    public void setUsername(String username) {
        this.currentUsername = username;
        usernameLabel.setText("@" + username);
    }
    
    public void loadData() {
        loadApps();
        loadThreats();
    }
    
    private void setupAppsTable() {
        appNameColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().name()));
        
        appSourceColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().source()));
        
        appVersionColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().version().orElse("N/A")));
        
        appIdColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().id().orElse("N/A")));
    }
    
    private void loadApps() {
        try {
            AppService appService = serviceFactory.getAppService();
            allApps = appService.getInstalledApps();
            
            appsTable.getItems().setAll(allApps);
            totalAppsLabel.setText(String.valueOf(allApps.size()));
            appCountLabel.setText(allApps.size() + " apps");
        } catch (Exception e) {
            showErrorAlert("Error", "Failed to load apps: " + e.getMessage());
        }
    }
    
    private void loadThreats() {
        try {
            ThreatService threatService = serviceFactory.getThreatService();
            List<ThreatService.ThreatMatch> threats = threatService.getThreats();
            
            threatsContainer.getChildren().clear();
            totalThreatsLabel.setText(String.valueOf(threats.size()));
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm");
            lastCheckLabel.setText("Last check: " + LocalDateTime.now().format(formatter));
            
            if (threats.isEmpty()) {
                noThreatsLabel.setVisible(true);
                threatsContainer.getChildren().add(noThreatsLabel);
            } else {
                noThreatsLabel.setVisible(false);
                for (ThreatService.ThreatMatch threat : threats) {
                    threatsContainer.getChildren().add(createThreatCard(threat));
                    // Show desktop notification for each threat
                    com.ullivada.zeropanic.applister.util.NotificationUtil.showThreatNotification(
                        threat.getAppName(), 
                        threat.getSeverity()
                    );
                }
            }
        } catch (Exception e) {
            showErrorAlert("Error", "Failed to load threats: " + e.getMessage());
        }
    }
    
    private VBox createThreatCard(ThreatService.ThreatMatch threat) {
        VBox card = new VBox(12);
        card.getStyleClass().add("threat-card");
        card.getStyleClass().add("threat-card-" + threat.getSeverity().toLowerCase());
        card.setPadding(new Insets(20));
        
        // Header with severity badge and title
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label severityBadge = new Label(threat.getSeverityIcon() + " " + 
            threat.getSeverity().toUpperCase());
        severityBadge.getStyleClass().add("severity-badge");
        severityBadge.getStyleClass().add("severity-" + threat.getSeverity().toLowerCase());
        
        Label titleLabel = new Label(threat.getTitle());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: #000000;");
        titleLabel.setWrapText(true);
        
        header.getChildren().addAll(severityBadge, titleLabel);
        
        // App name
        Label appLabel = new Label("Affected App: " + threat.getAppName());
        appLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #000000; -fx-font-weight: 500;");
        
        // Description
        if (threat.getDescription() != null && !threat.getDescription().isEmpty()) {
            Label descLabel = new Label(threat.getDescription());
            descLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
            descLabel.setWrapText(true);
            card.getChildren().add(descLabel);
        }
        
        // URL Hyperlink
        if (threat.getUrl() != null && !threat.getUrl().isEmpty()) {
            Hyperlink urlLink = new Hyperlink("🔗 More Information");
            urlLink.setStyle("-fx-text-fill: #000000; -fx-font-size: 14px;");
            urlLink.setOnAction(e -> {
                getHostServices().showDocument(threat.getUrl());
            });
            card.getChildren().add(urlLink);
        }
        
        card.getChildren().addAll(0, List.of(header, appLabel));
        
        return card;
    }
    
    @FXML
    private void showAlertsPanel() {
        alertsPanel.setVisible(true);
        alertsPanel.setManaged(true);
        appsPanel.setVisible(false);
        appsPanel.setManaged(false);
        
        alertsButton.getStyleClass().add("active");
        appsButton.getStyleClass().remove("active");
    }
    
    @FXML
    private void showAppsPanel() {
        appsPanel.setVisible(true);
        appsPanel.setManaged(true);
        alertsPanel.setVisible(false);
        alertsPanel.setManaged(false);
        
        appsButton.getStyleClass().add("active");
        alertsButton.getStyleClass().remove("active");
    }
    
    @FXML
    private void handleForceScan() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Force Scan");
        confirmAlert.setHeaderText("Scan System Now?");
        confirmAlert.setContentText("This will scan your system for all installed applications. This may take a few moments.");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            performForceScan();
        }
    }
    
    private void performForceScan() {
        try {
            // Show loading
            Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
            progressAlert.setTitle("Scanning");
            progressAlert.setHeaderText("Scanning system...");
            progressAlert.setContentText("Please wait while we scan for installed applications.");
            progressAlert.show();
            
            // Scan in background
            new Thread(() -> {
                try {
                    AppService appService = serviceFactory.getAppService();
                    List<InstalledApp> apps = appService.forceScan();
                    
                    Platform.runLater(() -> {
                        progressAlert.close();
                        allApps = apps;
                        appsTable.getItems().setAll(apps);
                        totalAppsLabel.setText(String.valueOf(apps.size()));
                        appCountLabel.setText(apps.size() + " apps");
                        
                        // Reload threats
                        loadThreats();
                        
                        showInfoAlert("Scan Complete", 
                            "Found " + apps.size() + " installed applications.");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        progressAlert.close();
                        showErrorAlert("Scan Failed", e.getMessage());
                    });
                }
            }).start();
        } catch (Exception e) {
            showErrorAlert("Error", "Failed to start scan: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        
        if (query.isEmpty()) {
            appsTable.getItems().setAll(allApps);
        } else {
            List<InstalledApp> filtered = allApps.stream()
                .filter(app -> app.name().toLowerCase().contains(query))
                .toList();
            appsTable.getItems().setAll(filtered);
        }
        
        appCountLabel.setText(appsTable.getItems().size() + " apps");
    }
    
    @FXML
    private void handleLogout() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Logout");
        confirmAlert.setHeaderText("Logout?");
        confirmAlert.setContentText("Are you sure you want to logout?");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            navigateToLogin();
        }
    }
    
    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) alertsButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("ZeroPanic - Login");
        } catch (Exception e) {
            showErrorAlert("Error", "Failed to logout: " + e.getMessage());
        }
    }
    
    private javafx.application.HostServices getHostServices() {
        // Access HostServices through Application
        return (javafx.application.HostServices) 
            alertsButton.getScene().getWindow().getProperties().get("hostServices");
    }
    
    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
