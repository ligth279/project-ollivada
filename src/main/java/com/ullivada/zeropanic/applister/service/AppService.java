package com.ullivada.zeropanic.applister.service;

import com.ullivada.zeropanic.applister.collectors.*;
import com.ullivada.zeropanic.applister.database.DatabaseService;
import com.ullivada.zeropanic.applister.model.InstalledApp;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Application scanning service for JavaFX frontend.
 * Handles installed app detection and caching.
 */
public class AppService {
    private static final int SCAN_INTERVAL_HOURS = 20;
    private final DatabaseService db;
    
    public AppService(DatabaseService db) {
        this.db = db;
    }
    
    /**
     * Get installed applications (uses cache if available).
     * @return List of installed apps
     */
    public List<InstalledApp> getInstalledApps() throws SQLException {
        if (db.wasScannedWithinHours(SCAN_INTERVAL_HOURS)) {
            return db.getAllApps();
        } else {
            return forceScan();
        }
    }
    
    /**
     * Force a fresh system scan and update cache.
     * @return List of installed apps
     */
    public List<InstalledApp> forceScan() throws SQLException {
        List<InstalledApp> apps = scanSystem();
        db.updateApps(apps);
        return apps;
    }
    
    /**
     * Check if cached data is available.
     * @return true if scan was performed within the last 20 hours
     */
    public boolean hasCachedData() throws SQLException {
        return db.wasScannedWithinHours(SCAN_INTERVAL_HOURS);
    }
    
    private List<InstalledApp> scanSystem() {
        List<InstalledAppCollector> collectors = List.of(
            new SnapCollector(),
            new FlatpakCollector(),
            new AptCollector(),
            new DesktopEntryCollector(),
            new WindowsRegistryCollector()
        );
        
        List<InstalledApp> allApps = new ArrayList<>();
        for (InstalledAppCollector collector : collectors) {
            try {
                allApps.addAll(collector.collect());
            } catch (Exception e) {
                System.err.println("[Warn] Collector " + collector.sourceName() + " failed: " + e.getMessage());
            }
        }
        
        return allApps;
    }
}
