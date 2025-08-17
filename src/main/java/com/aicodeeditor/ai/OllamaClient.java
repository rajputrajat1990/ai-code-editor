package com.aicodeeditor.ai;

import com.aicodeeditor.core.ConfigurationManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Client for communicating with Ollama AI model
 */
public class OllamaClient {
    private static final Logger logger = LoggerFactory.getLogger(OllamaClient.class);
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ConfigurationManager config;
    private final String baseUrl;
    private final String model;
    
    public OllamaClient() {
        this.config = ConfigurationManager.getInstance();
        this.baseUrl = config.getString("ollama.host", "http://localhost:11434");
        this.model = config.getString("ollama.model", "llama3.2:7b");
        
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofMinutes(5))
            .writeTimeout(Duration.ofMinutes(5))
            .build();
            
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Check if Ollama is running and accessible
     */
    public CompletableFuture<Boolean> isAvailable() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                    .url(baseUrl + "/api/version")
                    .build();
                    
                try (Response response = httpClient.newCall(request).execute()) {
                    return response.isSuccessful();
                }
            } catch (Exception e) {
                logger.debug("Ollama availability check failed", e);
                return false;
            }
        });
    }
    
    /**
     * Generate text completion using Ollama
     */
    public CompletableFuture<String> generate(String prompt, String systemPrompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String requestBody = createGenerateRequest(prompt, systemPrompt);
                
                Request request = new Request.Builder()
                    .url(baseUrl + "/api/generate")
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Ollama request failed: " + response.code());
                    }
                    
                    return parseResponse(response.body().string());
                }
                
            } catch (Exception e) {
                logger.error("Failed to generate text with Ollama", e);
                throw new RuntimeException("AI generation failed", e);
            }
        });
    }
    
    /**
     * Create JSON request for Ollama generate API
     */
    private String createGenerateRequest(String prompt, String systemPrompt) throws IOException {
        var requestJson = objectMapper.createObjectNode();
        requestJson.put("model", model);
        requestJson.put("prompt", prompt);
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            requestJson.put("system", systemPrompt);
        }
        requestJson.put("stream", false);
        requestJson.put("options", objectMapper.createObjectNode()
            .put("temperature", 0.7)
            .put("top_p", 0.9)
            .put("num_predict", 2048));
            
        return objectMapper.writeValueAsString(requestJson);
    }
    
    /**
     * Parse response from Ollama
     */
    private String parseResponse(String responseBody) throws IOException {
        JsonNode responseJson = objectMapper.readTree(responseBody);
        
        if (responseJson.has("error")) {
            throw new RuntimeException("Ollama error: " + responseJson.get("error").asText());
        }
        
        return responseJson.get("response").asText();
    }
    
    /**
     * Check if specified model is available
     */
    public CompletableFuture<Boolean> isModelAvailable(String modelName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                    .url(baseUrl + "/api/tags")
                    .build();
                    
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        return false;
                    }
                    
                    JsonNode tagsJson = objectMapper.readTree(response.body().string());
                    JsonNode models = tagsJson.get("models");
                    
                    if (models != null && models.isArray()) {
                        for (JsonNode modelNode : models) {
                            String name = modelNode.get("name").asText();
                            if (name.equals(modelName)) {
                                return true;
                            }
                        }
                    }
                    
                    return false;
                }
            } catch (Exception e) {
                logger.debug("Model availability check failed for: " + modelName, e);
                return false;
            }
        });
    }
    
    /**
     * Pull/download a model if not available
     */
    public CompletableFuture<Boolean> pullModel(String modelName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String requestBody = objectMapper.createObjectNode()
                    .put("name", modelName)
                    .toString();
                
                Request request = new Request.Builder()
                    .url(baseUrl + "/api/pull")
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    return response.isSuccessful();
                }
                
            } catch (Exception e) {
                logger.error("Failed to pull model: " + modelName, e);
                return false;
            }
        });
    }
    
    /**
     * Close the HTTP client
     */
    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
}
