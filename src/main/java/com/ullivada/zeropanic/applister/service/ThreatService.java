package com.ullivada.zeropanic.applister.service;

import com.ullivada.zeropanic.applister.database.DatabaseService;
import com.ullivada.zeropanic.applister.model.InstalledApp;
import com.ullivada.zeropanic.applister.supabase.SupabaseService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Threat detection service for JavaFX frontend.
 * Matches installed apps against Supabase threat database.
 */
public class ThreatService {
    private static final int SUPABASE_CHECK_INTERVAL_HOURS = 5;
    private final DatabaseService db;
    private final SupabaseService supabase;
    
    public ThreatService(DatabaseService db, SupabaseService supabase) {
        this.db = db;
        this.supabase = supabase;
    }
    
    /**
     * Get threats that match installed apps (filtered).
     * Uses cache if checked within last 5 hours.
     * @return List of matched threats with titles and URLs
     */
    public List<ThreatMatch> getThreats() throws SQLException {
        List<InstalledApp> apps = db.getAllApps();
        
        // Check if we need to update Supabase check timestamp
        if (!db.wasSupabaseCheckedWithinHours(SUPABASE_CHECK_INTERVAL_HOURS)) {
            db.updateSupabaseCheckTimestamp();
        }
        
        try {
            List<SupabaseService.ThreatMatch> supabaseThreats = supabase.getActiveThreats(apps);
            return convertToThreatMatches(supabaseThreats);
        } catch (IOException | InterruptedException e) {
            System.err.println("[Warn] Threat check failed: " + e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Force a fresh threat check (ignores cache).
     * @return List of matched threats
     */
    public List<ThreatMatch> forceCheckThreats() throws SQLException {
        List<InstalledApp> apps = db.getAllApps();
        db.updateSupabaseCheckTimestamp();
        
        try {
            List<SupabaseService.ThreatMatch> supabaseThreats = supabase.getActiveThreats(apps);
            return convertToThreatMatches(supabaseThreats);
        } catch (IOException | InterruptedException e) {
            System.err.println("[Warn] Threat check failed: " + e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Check if threat data was checked recently.
     * @return true if checked within last 5 hours
     */
    public boolean hasCachedThreatCheck() throws SQLException {
        return db.wasSupabaseCheckedWithinHours(SUPABASE_CHECK_INTERVAL_HOURS);
    }
    
    /**
     * Convert Supabase ThreatMatch to service ThreatMatch.
     */
    private List<ThreatMatch> convertToThreatMatches(List<SupabaseService.ThreatMatch> supabaseThreats) {
        List<ThreatMatch> result = new ArrayList<>();
        for (SupabaseService.ThreatMatch st : supabaseThreats) {
            result.add(new ThreatMatch(
                st.title() != null ? st.title() : "Unknown Threat",
                st.url() != null ? st.url() : "",
                st.targetApp() != null ? st.targetApp() : "Unknown",
                st.severity() != null ? st.severity() : "medium",
                st.description() != null ? st.description() : ""
            ));
        }
        return result;
    }
    
    /**
     * Threat match result object for JavaFX.
     * Contains title, URL, app name, severity, and description.
     */
    public static class ThreatMatch {
        private final String title;
        private final String url;
        private final String appName;
        private final String severity;
        private final String description;
        
        public ThreatMatch(String title, String url, String appName, String severity, String description) {
            this.title = title;
            this.url = url;
            this.appName = appName;
            this.severity = severity;
            this.description = description;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getUrl() {
            return url;
        }
        
        public String getAppName() {
            return appName;
        }
        
        public String getSeverity() {
            return severity;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getSeverityIcon() {
            return switch (severity.toLowerCase()) {
                case "critical" -> "🔴";
                case "high" -> "🟠";
                case "medium" -> "🟡";
                case "low" -> "🟢";
                default -> "⚠️";
            };
        }
    }
}
