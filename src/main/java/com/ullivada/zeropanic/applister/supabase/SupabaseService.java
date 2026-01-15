package com.ullivada.zeropanic.applister.supabase;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for querying Supabase REST API (cyber_threats table).
 * Uses HTTPS REST API instead of direct PostgreSQL connection.
 */
public class SupabaseService {
    private final String supabaseUrl;
    private final String anonKey;
    private final HttpClient httpClient;

    public SupabaseService() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        this.supabaseUrl = dotenv.get("SUPABASE_URL");
        this.anonKey = dotenv.get("SUPABASE_ANON_KEY");
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    /**
     * Check if Supabase is configured (environment variables present).
     */
    public boolean isConfigured() {
        return supabaseUrl != null && !supabaseUrl.isBlank() 
            && anonKey != null && !anonKey.isBlank();
    }

    /**
     * Query cyber_threats table for active threats using REST API.
     * Returns list of ThreatMatch objects.
     */
    public List<ThreatMatch> getActiveThreats() throws IOException, InterruptedException {
        if (!isConfigured()) {
            return List.of();
        }

        // Supabase REST API endpoint with filters
        String endpoint = supabaseUrl + "/rest/v1/cyber_threats?select=target_app,title,description,severity,url&order=severity.desc,created_at.desc";
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("apikey", anonKey)
            .header("Authorization", "Bearer " + anonKey)
            .header("x-client-info", "applister-cli")
            .header("Accept", "application/json")
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                System.err.println("[DEBUG] Supabase API failed: HTTP " + response.statusCode());
                System.err.println("[DEBUG] Response: " + response.body());
                return List.of();
            }

            List<ThreatMatch> threats = parseJsonResponse(response.body());
            
            // Debug: Show if database is empty
            if (threats.isEmpty()) {
                System.out.println("[Debug] Supabase check successful - cyber_threats table is empty (no threats found)");
            } else {
                System.out.println("[Debug] Supabase check successful - found " + threats.size() + " threat(s) in database");
            }
            
            return threats;
        } catch (IOException | InterruptedException e) {
            System.err.println("[DEBUG] Supabase API error: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Simple JSON parser for cyber_threats response.
     * Using regex since we don't have a JSON library.
     */
    private List<ThreatMatch> parseJsonResponse(String jsonArray) {
        List<ThreatMatch> threats = new ArrayList<>();
        
        // Match each object in the JSON array
        Pattern objectPattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher objectMatcher = objectPattern.matcher(jsonArray);
        
        while (objectMatcher.find()) {
            String obj = objectMatcher.group(1);
            
            String targetApp = extractJsonField(obj, "target_app");
            String title = extractJsonField(obj, "title");
            String description = extractJsonField(obj, "description");
            String severity = extractJsonField(obj, "severity");
            String url = extractJsonField(obj, "url");
            
            if (targetApp != null && title != null) {
                threats.add(new ThreatMatch(targetApp, title, description, severity, url));
            }
        }
        
        return threats;
    }

    private String extractJsonField(String json, String fieldName) {
        // Match "field":"value" or "field":null
        Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*(?:\"([^\"]*)\"|null)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1); // Returns null if field is null
        }
        return null;
    }

    /**
     * Represents a cybersecurity threat from Supabase.
     */
    public record ThreatMatch(
        String targetApp,
        String title,
        String description,
        String severity,
        String url
    ) {
        public String getSeverityIcon() {
            if (severity == null) return "⚠️";
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
