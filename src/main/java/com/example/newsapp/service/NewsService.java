package com.example.newsapp.service;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.annotation.PostConstruct;

@Service
public class NewsService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${news.api.url}")
    private String newsApiUrl;

    @Value("${news.api.key}")
    private String newsApiKey;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.model}")
    private String groqModel;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.table}")
    private String supabaseTable;

    // --- SETUP: Create SQLite Table ---
    @PostConstruct
    public void initSqlite() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS news_buffer (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT UNIQUE, " +
                "description TEXT, " +
                "url TEXT, " +
                "processed BOOLEAN DEFAULT 0)");
    }

    private static final Map<String, Integer> KEYWORD_WEIGHTS = new HashMap<>();
    static {
        KEYWORD_WEIGHTS.put("ransomware", 10);
        KEYWORD_WEIGHTS.put("vulnerability", 8);
        KEYWORD_WEIGHTS.put("exploit", 8);
        KEYWORD_WEIGHTS.put("patch", 5);
        KEYWORD_WEIGHTS.put("breach", 5);
        KEYWORD_WEIGHTS.put("security", 3);
        KEYWORD_WEIGHTS.put("cyber", 3);
    }

    // ==========================================
    // PHASE 1: COLLECT (Fetch -> Filter -> SQLite)
    // ==========================================
    public Object fetchAndBufferNews() {
        String url = newsApiUrl + newsApiKey;
        JsonNode root = restTemplate.getForObject(url, JsonNode.class);
        JsonNode articlesNode = root.path("articles");
        int savedCount = 0;

        if (articlesNode.isArray()) {
            for (JsonNode article : articlesNode) {
                String title = article.path("title").asText("").trim();
                
                if (calculateScore(title.toLowerCase()) >= 3) {
                    try {
                        String desc = article.path("description").asText("");
                        String articleUrl = article.path("url").asText("");
                        
                        int rows = jdbcTemplate.update(
                            "INSERT OR IGNORE INTO news_buffer (title, description, url, processed) VALUES (?, ?, ?, 0)",
                            title, desc, articleUrl
                        );
                        savedCount += rows;
                    } catch (Exception e) {
                        System.err.println("SQLite Error: " + e.getMessage());
                    }
                }
            }
        }
        
        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", "Buffered");
        response.put("new_items_saved", savedCount);
        return response;
    }

    // ==========================================
    // PHASE 2: EXTRACT (SQLite -> Groq -> Supabase)
    // ==========================================
    public Object processPendingNews() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM news_buffer WHERE processed = 0 LIMIT 20"
        );

        if (rows.isEmpty()) {
            return "No pending news to process.";
        }

        int successCount = extractAndUploadWithGroq(rows);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", "Processed");
        response.put("count", successCount);
        return response;
    }

private int extractAndUploadWithGroq(List<Map<String, Object>> rows) {
        int uploaded = 0;
        try {
            // 1. Prepare Prompt (Strict Single-Entity Extraction)
            StringBuilder prompt = new StringBuilder();
            prompt.append("Extract the specific software or app name from these headlines.\n");
            prompt.append("Format: Return a JSON Array of objects: [{\"target_app\": \"Name\", \"severity\": \"Level\"}].\n\n");
            
            prompt.append("RULES for 'target_app':\n");
            prompt.append("1. EXTRACT ONLY THE PRODUCT NAME (e.g., 'Chrome', 'Windows', 'Zoom').\n");
            prompt.append("2. MAX 2 WORDS. Do not include versions (like 'v2.0').\n");
            prompt.append("3. IF IT IS A COUNTRY (e.g., 'Iran', 'China') or HACKER GROUP -> RETURN 'Unknown'.\n");
            prompt.append("4. IF UNSURE -> RETURN 'Unknown'.\n\n");
            
            prompt.append("EXAMPLES:\n");
            prompt.append("Input: 'Critical flaw in Google Chrome allows remote code execution'\n");
            prompt.append("Output: \"Chrome\"\n");
            prompt.append("Input: 'Iran hackers target Israel water systems'\n");
            prompt.append("Output: \"Unknown\" (Country is not an App)\n");
            prompt.append("Input: 'Microsoft Outlook vulnerability exposed'\n");
            prompt.append("Output: \"Outlook\"\n");
            prompt.append("Input: 'New ransomware attacks US hospitals'\n");
            prompt.append("Output: \"Unknown\" (Sector is not an App)\n\n");
            
            prompt.append("TASK INPUT:\n");

            for (int i = 0; i < rows.size(); i++) {
                String title = (String) rows.get(i).get("title");
                // Remove extra noise characters to keep it clean
                title = title.replaceAll("[^a-zA-Z0-9 .:-]", ""); 
                prompt.append(i + ": " + title + "\n");
            }

            // 2. Build Groq Request (OpenAI Format)
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", groqModel);
            
            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode systemMsg = objectMapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", "You are a strict JSON extractor.");
            messages.add(systemMsg);
            
            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", prompt.toString());
            messages.add(userMsg);
            
            requestBody.set("messages", messages);

            // 3. Send Request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + groqApiKey);

            HttpEntity<JsonNode> entity = new HttpEntity<>(requestBody, headers);
            JsonNode response = restTemplate.postForObject(groqApiUrl, entity, JsonNode.class);

            // 4. Parse Groq Response
            if (response != null && response.has("choices")) {
                String content = response.path("choices").get(0)
                        .path("message").path("content").asText();
                
                // Clean up any potential markdown backticks
                content = content.replace("```json", "").replace("```", "").trim();
                
                JsonNode extractedArray = objectMapper.readTree(content);

                if (extractedArray.isArray()) {
                    for (int i = 0; i < extractedArray.size(); i++) {
                        JsonNode item = extractedArray.get(i);
                        Map<String, Object> originalRow = rows.get(i);
                        
                        boolean sent = saveToSupabase(
                            (String) originalRow.get("title"),
                            item.path("target_app").asText("Unknown"),
                            (String) originalRow.get("description"),
                            item.path("severity").asText("Unknown"),
                            (String) originalRow.get("url")
                        );

                        if (sent) {
                            jdbcTemplate.update("UPDATE news_buffer SET processed = 1 WHERE id = ?", originalRow.get("id"));
                            uploaded++;
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Groq Extraction Failed: " + e.getMessage());
        }
        return uploaded;
    }

    private int calculateScore(String title) {
        int score = 0;
        for (Map.Entry<String, Integer> entry : KEYWORD_WEIGHTS.entrySet()) {
            if (title.contains(entry.getKey())) {
                score += entry.getValue();
            }
        }
        return score;
    }

    private boolean saveToSupabase(String title, String app, String desc, String sev, String url) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("title", title);
            payload.put("target_app", app);
            payload.put("description", desc);
            payload.put("severity", sev);
            payload.put("url", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseKey);
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.set("Prefer", "return=minimal");

            HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);
            restTemplate.postForEntity(supabaseUrl + "/rest/v1/" + supabaseTable, entity, String.class);
            return true;
        } catch (Exception e) {
            System.err.println("DB Upload Failed: " + e.getMessage());
            return false;
        }
    }
}