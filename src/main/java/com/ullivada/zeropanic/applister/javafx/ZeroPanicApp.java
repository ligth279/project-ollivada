package com.ullivada.zeropanic.applister.javafx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX Application Entry Point for ZeroPanic.
 * Beautiful cybersecurity app with animated gradient background.
 */
public class ZeroPanicApp extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load login view
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root, 1280, 800);
        
        primaryStage.setTitle("ZeroPanic - Context-Aware Cyber Intelligence");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
