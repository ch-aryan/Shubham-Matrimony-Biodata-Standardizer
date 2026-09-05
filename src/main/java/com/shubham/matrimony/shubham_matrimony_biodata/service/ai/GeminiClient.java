package com.shubham.matrimony.shubham_matrimony_biodata.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Transport-level client for communicating with the Google Gemini REST API.
 * Contains zero domain dependencies.
 */
@Slf4j
@Component
public class GeminiClient {

    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final GeminiConfigProperties config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiClient(GeminiConfigProperties config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Package-private constructor for unit testing with custom HttpClient and ObjectMapper.
     */
    GeminiClient(GeminiConfigProperties config, HttpClient httpClient, ObjectMapper objectMapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Sends prompt to Gemini API configured for structured JSON output.
     *
     * @param systemInstruction instruction establishing role and output format
     * @param userPrompt data payload and extraction objectives
     * @return raw JSON response text, or empty if request failed or was rejected
     */
    public Optional<String> generateJson(String systemInstruction, String userPrompt) {
        String apiKey = config.getApi().getKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini API key is not configured. Skipping Gemini call.");
            return Optional.empty();
        }

        String model = config.getApi().getModel();
        int timeoutSeconds = config.getApi().getTimeoutSeconds();
        int maxRetries = config.getApi().getMaxRetries();

        String url = GEMINI_API_BASE + model + ":generateContent";

        Map<String, Object> requestPayload = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", userPrompt))
                        )
                ),
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemInstruction))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "responseMimeType", "application/json"
                )
        );

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(requestPayload);
        } catch (Exception e) {
            log.error("Failed to serialize Gemini request body", e);
            return Optional.empty();
        }

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .header("Content-Type", "application/json")
                        .header("x-goog-api-key", apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                int statusCode = response.statusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    return extractTextFromResponse(response.body());
                }

                log.warn("Gemini API attempt {} failed with status {}: {}", attempt + 1, statusCode, response.body());
                if ((statusCode == 429 || statusCode >= 500) && attempt < maxRetries) {
                    long backoffMs = (long) Math.pow(2, attempt) * 1000L;
                    Thread.sleep(backoffMs);
                    continue;
                }
                return Optional.empty();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Gemini API call interrupted", e);
                return Optional.empty();
            } catch (Exception e) {
                log.warn("Gemini API call error on attempt {}: {}", attempt + 1, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return Optional.empty();
                    }
                }
            }
        }

        return Optional.empty();
    }

    private Optional<String> extractTextFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    String rawText = parts.get(0).path("text").asText("");
                    return Optional.of(stripMarkdownFences(rawText));
                }
            }
            log.warn("No text content found in Gemini response candidate");
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to parse Gemini API response JSON", e);
            return Optional.empty();
        }
    }

    static String stripMarkdownFences(String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
