package com.example.newsapp.service;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class NewsService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${news.api.url}")
    private String newsApiUrl;

    @Value("${news.api.key}")
    private String newsApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.table}")
    private String supabaseTable;

    // We process only Top 5 to ensure we get rich data without timeouts
    private static final int EXTRACTION_BATCH_LIMIT = 5;

    // Keyword Weights for Algorithmic Filtering
    private static final Map<String, Integer> KEYWORD_WEIGHTS = new HashMap<>();
    static {
        KEYWORD_WEIGHTS.put("ransomware", 10);
        KEYWORD_WEIGHTS.put("vulnerability", 8);
        KEYWORD_WEIGHTS.put("exploit", 8);
        KEYWORD_WEIGHTS.put("patch", 5);
        KEYWORD_WEIGHTS.put("breach", 5);
        KEYWORD_WEIGHTS.put("bug", 3);
        KEYWORD_WEIGHTS.put("flaw", 3);
        KEYWORD_WEIGHTS.put("hack", 2);
    }

    public Object getNewsHeadlines() {
        // 1. Fetch News
        String url = newsApiUrl + newsApiKey;
        JsonNode root = restTemplate.getForObject(url, JsonNode.class);
        JsonNode articlesNode = root.path("articles");

        List<JsonNode> filteredArticles = new ArrayList<>();

        // 2. Filter Locally (Algorithm Only)
        if (articlesNode.isArray()) {
            for (JsonNode article : articlesNode) {
                String title = article.path("title").asText("").trim();
                int score = calculateScore(title.toLowerCase());
                
                // Keep only relevant news (Score > 3)
                if (score >= 3) {
                    filteredArticles.add(article);
                }
            }
        }

        System.out.println("Algorithm found " + filteredArticles.size() + " relevant articles.");

        // 3. Take Top X for Extraction
        List<JsonNode> batchToProcess = filteredArticles.stream()
                .limit(EXTRACTION_BATCH_LIMIT)
                .collect(Collectors.toList());

        // 4. Extract Data using Gemini & Store in Supabase
        if (!batchToProcess.isEmpty()) {
            extractAndStoreData(batchToProcess);
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", "Success");
        response.put("message", "Processed and stored " + batchToProcess.size() + " articles in Supabase.");
        return response;
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

    // --- GEMINI EXTRACTION & SUPABASE STORAGE ---
    private void extractAndStoreData(List<JsonNode> articles) {
        try {
            // A. Prepare Prompt
            StringBuilder prompt = new StringBuilder();
            prompt.append("Analyze the following news headlines and descriptions. ");
            prompt.append("Extract structured data for a security database. ");
            prompt.append("Return strictly a JSON Array of objects with these keys: ");
            prompt.append("'title', 'target_app' (the specific app/company affected, e.g. 'Zoom', 'Windows', or 'Unknown'), ");
            prompt.append("'description' (short summary), 'severity' (Low, Medium, High, Critical). ");
            prompt.append("Input Data:\n");

            for (int i = 0; i < articles.size(); i++) {
                JsonNode a = articles.get(i);
                String t = a.path("title").asText("").replace("\"", "'");
                String d = a.path("description").asText("").replace("\"", "'");
                // Pass the original URL so we can map it back later if needed, 
                // but Gemini output doesn't strictly need to echo it back if we map by index.
                // To keep it simple, we ask Gemini to return the index or we map by order.
                prompt.append("Item " + i + ": Title: " + t + " | Desc: " + d + "\n");
            }

            // B. Call Gemini
            ObjectNode contentNode = objectMapper.createObjectNode();
            ObjectNode partNode = objectMapper.createObjectNode();
            partNode.put("text", prompt.toString());
            ArrayNode partsArray = objectMapper.createArrayNode();
            partsArray.add(partNode);
            contentNode.set("parts", partsArray);
            ArrayNode contentsArray = objectMapper.createArrayNode();
            contentsArray.add(contentNode);
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.set("contents", contentsArray);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<JsonNode> entity = new HttpEntity<>(requestBody, headers);

            String geminiUrl = geminiApiUrl + geminiApiKey;
            JsonNode response = restTemplate.postForObject(geminiUrl, entity, JsonNode.class);

            // C. Parse Gemini Response
            if (response != null && response.has("candidates")) {
                String rawText = response.path("candidates").get(0)
                        .path("content").path("parts").get(0)
                        .path("text").asText();
                
                // Clean Markdown
                rawText = rawText.replace("```json", "").replace("```", "").trim();
                
                JsonNode extractedData = objectMapper.readTree(rawText);

                // D. Push to Supabase
                if (extractedData.isArray()) {
                    for (int i = 0; i < extractedData.size(); i++) {
                        JsonNode item = extractedData.get(i);
                        
                        // Merge the original URL back in (since Gemini doesn't generate URLs)
                        String originalUrl = "";
                        if (i < articles.size()) {
                            originalUrl = articles.get(i).path("url").asText();
                        }

                        saveToSupabase(item, originalUrl);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Extraction Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveToSupabase(JsonNode geminiData, String originalUrl) {
        try {
            // Build the JSON payload for Supabase
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("title", geminiData.path("title").asText());
            payload.put("target_app", geminiData.path("target_app").asText());
            payload.put("description", geminiData.path("description").asText());
            payload.put("severity", geminiData.path("severity").asText());
            payload.put("url", originalUrl);

            // Prepare Request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseKey);
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.set("Prefer", "return=minimal"); // Don't return the full object, just 201 Created

            HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);

            // Send POST
            String dbUrl = supabaseUrl + "/rest/v1/" + supabaseTable;
            restTemplate.postForEntity(dbUrl, entity, String.class);

            System.out.println("Saved to Supabase: " + geminiData.path("target_app").asText());

        } catch (Exception e) {
            System.err.println("Supabase Insert Failed: " + e.getMessage());
        }
    }
}