package com.aicodeeditor.ai;

import com.aicodeeditor.core.ConfigurationManager;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Service for querying web content for fact-checking and research
 */
public class WebQueryService {
    private static final Logger logger = LoggerFactory.getLogger(WebQueryService.class);
    
    private final OkHttpClient httpClient;
    private final ConfigurationManager config;
    private final String userAgent;
    
    // Common programming documentation sites
    private static final String[] PROGRAMMING_SITES = {
        "https://stackoverflow.com/search?q=",
        "https://docs.oracle.com/search/?q=",
        "https://developer.mozilla.org/en-US/search?q=",
        "https://github.com/search?q=",
        "https://www.geeksforgeeks.org/?s="
    };
    
    public WebQueryService() {
        this.config = ConfigurationManager.getInstance();
        this.userAgent = config.getString("web.userAgent", "AI-Code-Editor/1.0");
        
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofSeconds(60))
            .followRedirects(true)
            .build();
    }
    
    /**
     * Search for programming-related information
     */
    public CompletableFuture<String> searchProgrammingInfo(String query) {
        if (!config.getBoolean("web.enabled", true)) {
            return CompletableFuture.completedFuture("Web searching is disabled");
        }
        
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder results = new StringBuilder();
            String cleanQuery = cleanQuery(query);
            
            for (String site : PROGRAMMING_SITES) {
                try {
                    String content = searchSite(site + cleanQuery);
                    if (content != null && !content.isEmpty()) {
                        results.append("=== ").append(extractDomain(site)).append(" ===\n");
                        results.append(content).append("\n\n");
                    }
                } catch (Exception e) {
                    logger.debug("Failed to search site: " + site, e);
                }
                
                // Limit total results length
                if (results.length() > 5000) {
                    break;
                }
            }
            
            return results.length() > 0 ? results.toString() : "No relevant information found";
        });
    }
    
    /**
     * Search Stack Overflow for specific programming questions
     */
    public CompletableFuture<String> searchStackOverflow(String query) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = "https://api.stackexchange.com/2.3/search/advanced?order=desc&sort=relevance&q="
                    + java.net.URLEncoder.encode(query, "UTF-8") 
                    + "&site=stackoverflow";
                
                Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgent)
                    .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        return parseStackOverflowResponse(response.body().string());
                    }
                }
            } catch (Exception e) {
                logger.debug("Stack Overflow search failed", e);
            }
            
            return "Stack Overflow search failed";
        });
    }
    
    /**
     * Verify code syntax and best practices from documentation
     */
    public CompletableFuture<String> verifyCodeSyntax(String language, String code) {
        return CompletableFuture.supplyAsync(() -> {
            String query = language + " syntax " + extractKeywords(code);
            return searchProgrammingInfo(query).join();
        });
    }
    
    /**
     * Search for library/framework documentation
     */
    public CompletableFuture<String> searchDocumentation(String library, String topic) {
        return CompletableFuture.supplyAsync(() -> {
            String query = library + " " + topic + " documentation examples";
            return searchProgrammingInfo(query).join();
        });
    }
    
    /**
     * Search a specific website
     */
    private String searchSite(String url) throws IOException {
        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }
            
            String html = response.body().string();
            return extractRelevantContent(html);
        }
    }
    
    /**
     * Extract relevant content from HTML using Jsoup
     */
    private String extractRelevantContent(String html) {
        try {
            Document doc = Jsoup.parse(html);
            
            // Remove scripts, styles, and other non-content elements
            doc.select("script, style, nav, header, footer, aside").remove();
            
            // Extract main content
            StringBuilder content = new StringBuilder();
            
            // Look for code blocks first
            doc.select("code, pre, .highlight").forEach(element -> {
                String code = element.text().trim();
                if (code.length() > 10) {
                    content.append("CODE: ").append(code).append("\n");
                }
            });
            
            // Look for relevant text content
            doc.select("p, li, td, div.answer, div.post-text").forEach(element -> {
                String text = element.text().trim();
                if (text.length() > 50 && isRelevantContent(text)) {
                    content.append(text).append("\n");
                }
            });
            
            // Limit content length
            String result = content.toString();
            return result.length() > 2000 ? result.substring(0, 2000) + "..." : result;
            
        } catch (Exception e) {
            logger.debug("Failed to extract content from HTML", e);
            return "";
        }
    }
    
    /**
     * Check if text content is relevant for programming
     */
    private boolean isRelevantContent(String text) {
        String lower = text.toLowerCase();
        return lower.contains("function") || lower.contains("method") || 
               lower.contains("class") || lower.contains("variable") ||
               lower.contains("syntax") || lower.contains("example") ||
               lower.contains("code") || lower.contains("programming") ||
               lower.contains("error") || lower.contains("exception") ||
               lower.contains("import") || lower.contains("library");
    }
    
    /**
     * Extract keywords from code for searching
     */
    private String extractKeywords(String code) {
        // Simple keyword extraction - look for function names, imports, etc.
        Pattern functionPattern = Pattern.compile("(function|def|class|import|from|require)\\s+(\\w+)");
        java.util.regex.Matcher matcher = functionPattern.matcher(code);
        
        StringBuilder keywords = new StringBuilder();
        while (matcher.find()) {
            keywords.append(matcher.group(2)).append(" ");
        }
        
        return keywords.toString().trim();
    }
    
    /**
     * Clean query for URL encoding
     */
    private String cleanQuery(String query) {
        try {
            return java.net.URLEncoder.encode(query.replaceAll("[^\\w\\s]", " ").trim(), "UTF-8");
        } catch (Exception e) {
            return query.replaceAll("[^\\w\\s]", "").trim().replace(" ", "+");
        }
    }
    
    /**
     * Extract domain from URL
     */
    private String extractDomain(String url) {
        try {
            return new java.net.URL(url).getHost();
        } catch (Exception e) {
            return url;
        }
    }
    
    /**
     * Parse Stack Overflow API response
     */
    private String parseStackOverflowResponse(String jsonResponse) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(jsonResponse);
            com.fasterxml.jackson.databind.JsonNode items = root.get("items");
            
            if (items != null && items.isArray()) {
                StringBuilder result = new StringBuilder();
                int count = 0;
                
                for (com.fasterxml.jackson.databind.JsonNode item : items) {
                    if (count >= 3) break; // Limit to top 3 results
                    
                    String title = item.get("title").asText();
                    String excerpt = item.has("excerpt") ? item.get("excerpt").asText() : "";
                    
                    result.append("Q: ").append(title).append("\n");
                    if (!excerpt.isEmpty()) {
                        result.append("A: ").append(excerpt).append("\n");
                    }
                    result.append("\n");
                    count++;
                }
                
                return result.toString();
            }
        } catch (Exception e) {
            logger.debug("Failed to parse Stack Overflow response", e);
        }
        
        return "Failed to parse Stack Overflow results";
    }
    
    /**
     * Close the HTTP client
     */
    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
}
