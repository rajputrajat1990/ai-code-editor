package com.aicodeeditor.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages application configuration and settings
 */
public class ConfigurationManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
    private static ConfigurationManager instance;
    
    private final ObjectMapper objectMapper;
    private final Path configDir;
    private final Path configFile;
    private Map<String, Object> config;
    
    private ConfigurationManager() {
        this.objectMapper = new ObjectMapper();
        this.configDir = getConfigDirectory();
        this.configFile = configDir.resolve("config.json");
        this.config = new HashMap<>();
    }
    
    public static synchronized ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }
        return instance;
    }
    
    /**
     * Initialize configuration manager
     */
    public void initialize() {
        try {
            // Create config directory if it doesn't exist
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            
            // Load existing configuration or create default
            if (Files.exists(configFile)) {
                loadConfiguration();
            } else {
                createDefaultConfiguration();
                saveConfiguration();
            }
            
            logger.info("Configuration manager initialized");
            
        } catch (IOException e) {
            logger.error("Failed to initialize configuration", e);
        }
    }
    
    /**
     * Get configuration directory based on OS
     */
    private Path getConfigDirectory() {
        String osName = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        
        if (osName.contains("win")) {
            String appData = System.getenv("APPDATA");
            return Paths.get(appData != null ? appData : userHome, "AICodeEditor");
        } else if (osName.contains("mac")) {
            return Paths.get(userHome, "Library", "Application Support", "AICodeEditor");
        } else {
            return Paths.get(userHome, ".ai-code-editor");
        }
    }
    
    /**
     * Load configuration from file
     */
    private void loadConfiguration() throws IOException {
        JsonNode configNode = objectMapper.readTree(configFile.toFile());
        config = objectMapper.convertValue(configNode, Map.class);
        logger.info("Configuration loaded from {}", configFile);
    }
    
    /**
     * Create default configuration
     */
    private void createDefaultConfiguration() {
        config.put("ollama.host", "http://localhost:11434");
        config.put("ollama.model", "llama3.2:7b");
        config.put("ollama.timeout", 30000);
        config.put("editor.theme", "dark");
        config.put("editor.fontSize", 14);
        config.put("editor.tabSize", 4);
        config.put("sandbox.enabled", true);
        config.put("sandbox.containerImage", "ubuntu:22.04");
        config.put("sandbox.timeout", 300000);
        config.put("web.enabled", true);
        config.put("web.userAgent", "AI-Code-Editor/1.0");
        config.put("dependencies.autoInstall", true);
        config.put("dependencies.timeout", 600000);
        
        logger.info("Default configuration created");
    }
    
    /**
     * Save configuration to file
     */
    public void saveConfiguration() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(configFile.toFile(), config);
            logger.info("Configuration saved to {}", configFile);
        } catch (IOException e) {
            logger.error("Failed to save configuration", e);
        }
    }
    
    /**
     * Get configuration value
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return (T) value;
        } catch (ClassCastException e) {
            logger.warn("Configuration value type mismatch for key: {}", key);
            return defaultValue;
        }
    }
    
    /**
     * Set configuration value
     */
    public void set(String key, Object value) {
        config.put(key, value);
    }
    
    /**
     * Get string configuration value
     */
    public String getString(String key, String defaultValue) {
        return get(key, defaultValue);
    }
    
    /**
     * Get integer configuration value
     */
    public int getInt(String key, int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    /**
     * Get boolean configuration value
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return get(key, defaultValue);
    }
    
    /**
     * Shutdown configuration manager
     */
    public void shutdown() {
        saveConfiguration();
        logger.info("Configuration manager shutdown");
    }
}
