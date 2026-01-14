package com.example.newsapp.service;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    // SCORING WEIGHTS
    private static final Map<String, Integer> KEYWORD_WEIGHTS = new HashMap<>();
    static {
        // High confidence words (Definite Cybersec)
        KEYWORD_WEIGHTS.put("ransomware", 10);
        KEYWORD_WEIGHTS.put("malware", 10);
        KEYWORD_WEIGHTS.put("ddos", 10);
        KEYWORD_WEIGHTS.put("zero-day", 10);
        KEYWORD_WEIGHTS.put("cve-", 10);
        KEYWORD_WEIGHTS.put("phishing", 8);
        KEYWORD_WEIGHTS.put("infosec", 8);

        // Medium confidence words (Could be general tech or life hacks)
        KEYWORD_WEIGHTS.put("hack", 2);
        KEYWORD_WEIGHTS.put("breach", 3);
        KEYWORD_WEIGHTS.put("security", 2);
        KEYWORD_WEIGHTS.put("privacy", 2);
        KEYWORD_WEIGHTS.put("data", 1);
        KEYWORD_WEIGHTS.put("password", 2);

        // Negative weights (Usually irrelevant)
        KEYWORD_WEIGHTS.put("life hack", -20);
        KEYWORD_WEIGHTS.put("kitchen", -20);
        KEYWORD_WEIGHTS.put("beauty", -20);
    }

    public Object getNewsHeadlines() {
        String url = newsApiUrl + newsApiKey;
        JsonNode root = restTemplate.getForObject(url, JsonNode.class);
        JsonNode articlesNode = root.path("articles");

        List<JsonNode> finalArticles = new ArrayList<>();

        if (articlesNode.isArray()) {
            for (JsonNode article : articlesNode) {
                String title = article.path("title").asText("").toLowerCase();
                
                // 1. Calculate Local Score
                int score = calculateScore(title);

                // 2. filtering Logic
                if (score >= 5) {
                    // High Probability: Keep it automatically
                    finalArticles.add(article);
                } else if (score >= 2) {
                    // Medium Probability: Ask Gemini (Token Usage here only)
                    if (verifyWithGemini(title)) {
                        finalArticles.add(article);
                    }
                } 
                // If score < 2, we drop it (save tokens, reduce noise)
            }
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.put("total_filtered_results", finalArticles.size());
        response.set("articles", objectMapper.valueToTree(finalArticles));
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

    private boolean verifyWithGemini(String title) {
        try {
            // Construct the Gemini API Request
            // We use a very strict prompt to save tokens and ensure boolean-like answer
            String promptText = "Is this headline about Cybersecurity? Answer strictly with YES or NO. Headline: \"" + title + "\"";

            // JSON Structure for Gemini API
            String requestBody = "{"
                    + "\"contents\": [{"
                    + "\"parts\": [{\"text\": \"" + promptText + "\"}]"
                    + "}]"
                    + "}";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            String fullUrl = geminiApiUrl + geminiApiKey;
            JsonNode response = restTemplate.postForObject(fullUrl, entity, JsonNode.class);

            // Extract text from Gemini response
            String answer = response.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText().trim().toUpperCase();

            return answer.contains("YES");

        } catch (Exception e) {
            // If API fails, default to false to stay safe, or true if you prefer noise over silence
            System.err.println("Gemini check failed for: " + title);
            return false; 
        }
    }
}