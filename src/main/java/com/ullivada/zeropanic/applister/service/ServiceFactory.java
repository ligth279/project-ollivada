package com.ullivada.zeropanic.applister.service;

import com.ullivada.zeropanic.applister.database.DatabaseService;
import com.ullivada.zeropanic.applister.supabase.SupabaseService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

/**
 * Service factory for JavaFX frontend.
 * Creates and manages service instances.
 */
public class ServiceFactory {
    private static ServiceFactory instance;
    private DatabaseService databaseService;
    private AuthService authService;
    private AppService appService;
    private ThreatService threatService;
    
    private ServiceFactory() throws SQLException, IOException {
        ensureDatabaseDirectory();
        this.databaseService = new DatabaseService();
        this.authService = new AuthService(databaseService);
        this.appService = new AppService(databaseService);
        this.threatService = new ThreatService(databaseService, new SupabaseService());
    }
    
    /**
     * Get singleton instance of ServiceFactory.
     */
    public static ServiceFactory getInstance() throws SQLException, IOException {
        if (instance == null) {
            instance = new ServiceFactory();
        }
        return instance;
    }
    
    /**
     * Reset singleton (useful for testing).
     */
    public static void reset() throws SQLException {
        if (instance != null && instance.databaseService != null) {
            instance.databaseService.close();
        }
        instance = null;
    }
    
    public AuthService getAuthService() {
        return authService;
    }
    
    public AppService getAppService() {
        return appService;
    }
    
    public ThreatService getThreatService() {
        return threatService;
    }
    
    public DatabaseService getDatabaseService() {
        return databaseService;
    }
    
    /**
     * Close all resources.
     */
    public void close() throws SQLException {
        if (databaseService != null) {
            databaseService.close();
        }
    }
    
    private static void ensureDatabaseDirectory() throws IOException {
        String userHome = System.getProperty("user.home");
        Path dbDir = Paths.get(userHome, ".applister");
        if (!Files.exists(dbDir)) {
            Files.createDirectories(dbDir);
        }
    }
}
