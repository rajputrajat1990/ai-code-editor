package com.aicodeeditor.ai;

import com.aicodeeditor.sandbox.SandboxManager;
import com.aicodeeditor.dependencies.DependencyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Original AI Agent - kept for backward compatibility
 * Use EnhancedAIAgent for better autonomous behavior
 */
public class AIAgent {
    private static final Logger logger = LoggerFactory.getLogger(AIAgent.class);
    
    private final OllamaClient ollamaClient;
    private final WebQueryService webQueryService;
    private final SandboxManager sandboxManager;
    private final DependencyManager dependencyManager;
    
    private static final String SYSTEM_PROMPT = """
        You are an expert software development assistant with the following capabilities:
        1. Write high-quality, well-documented code
        2. Execute commands in a sandbox environment
        3. Install dependencies automatically
        4. Verify information using web searches
        5. Follow best practices and modern conventions
        
        When asked to develop software:
        1. First research the requirements and technologies
        2. Plan the project structure
        3. Set up the development environment with necessary dependencies
        4. Write clean, maintainable code
        5. Test the implementation
        6. Provide documentation
        
        Always prioritize security, performance, and maintainability.
        """;
    
    public AIAgent() {
        this.ollamaClient = new OllamaClient();
        this.webQueryService = new WebQueryService();
        this.sandboxManager = new SandboxManager();
        this.dependencyManager = new DependencyManager();
    }
    
    public AIAgent(OllamaClient ollamaClient, WebQueryService webQueryService,
                   SandboxManager sandboxManager, DependencyManager dependencyManager) {
        this.ollamaClient = ollamaClient;
        this.webQueryService = webQueryService;
        this.sandboxManager = sandboxManager;
        this.dependencyManager = dependencyManager;
    }
    
    /**
     * Initialize the AI agent
     */
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Initializing AI Agent...");
                
                // Check if Ollama is available
                if (!ollamaClient.isAvailable().join()) {
                    logger.error("Ollama is not available. Please ensure it's running on localhost:11434");
                    return false;
                }
                
                logger.info("AI Agent initialized successfully");
                return true;
                
            } catch (Exception e) {
                logger.error("Failed to initialize AI Agent", e);
                return false;
            }
        });
    }
    
    /**
     * Simple development method for backward compatibility
     */
    public CompletableFuture<String> developSoftware(String requirements) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Processing development request: {}", requirements);
                
                String prompt = SYSTEM_PROMPT + "\n\n" +
                               "User Request: " + requirements + "\n\n" +
                               "Please develop this software step by step.";
                
                String response = ollamaClient.generate(prompt, "").join();
                
                logger.info("Development response generated");
                return response;
                
            } catch (Exception e) {
                logger.error("Error in development process", e);
                return "Error in development process: " + e.getMessage();
            }
        });
    }
}
