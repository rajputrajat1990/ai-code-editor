package com.aicodeeditor.ai;

import com.aicodeeditor.sandbox.SandboxManager;
import com.aicodeeditor.dependencies.DependencyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Main AI Agent that coordinates all AI-powered operations
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
                
                // Note: sandboxManager and dependencyManager initialize automatically in their constructors
                
                logger.info("AI Agent initialized successfully");
                return true;
                
            } catch (Exception e) {
                logger.error("Failed to initialize AI Agent", e);
                return false;
            }
        });
    }
    
    /**
     * Process a development request from the user
     */
    public CompletableFuture<DevelopmentResult> developSoftware(String userRequirement, String preferredLanguage) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Processing development request: {}", userRequirement);
                
                DevelopmentResult result = new DevelopmentResult();
                result.setRequirement(userRequirement);
                result.setLanguage(preferredLanguage);
                
                // Step 1: Research and fact-check
                String researchPrompt = String.format(
                    "Research the following software development requirement and provide technical details:\n" +
                    "Requirement: %s\n" +
                    "Preferred Language: %s\n\n" +
                    "Please provide:\n" +
                    "1. Technical approach\n" +
                    "2. Required libraries/frameworks\n" +
                    "3. Project structure\n" +
                    "4. Key considerations",
                    userRequirement, preferredLanguage
                );
                
                // Get web research
                String webResearch = webQueryService.searchProgrammingInfo(
                    preferredLanguage + " " + userRequirement + " best practices examples"
                ).join();
                
                // Get AI analysis with web context
                String aiAnalysis = ollamaClient.generate(
                    researchPrompt + "\n\nWeb Research Context:\n" + webResearch,
                    SYSTEM_PROMPT
                ).join();
                
                result.setAnalysis(aiAnalysis);
                logger.info("Completed analysis phase");
                
                // Step 2: Set up development environment
                String setupResult = setupDevelopmentEnvironment(preferredLanguage, aiAnalysis);
                result.setEnvironmentSetup(setupResult);
                
                // Step 3: Generate code
                String codePrompt = String.format(
                    "Based on the analysis, create a complete implementation for:\n" +
                    "Requirement: %s\n" +
                    "Language: %s\n" +
                    "Analysis: %s\n\n" +
                    "Provide complete, runnable code with proper structure and documentation.",
                    userRequirement, preferredLanguage, aiAnalysis
                );
                
                String generatedCode = ollamaClient.generate(codePrompt, SYSTEM_PROMPT).join();
                result.setGeneratedCode(generatedCode);
                logger.info("Generated code");
                
                // Step 4: Execute and test in sandbox
                String executionResult = executeInSandbox(generatedCode, preferredLanguage);
                result.setExecutionResult(executionResult);
                
                result.setSuccess(true);
                logger.info("Development request completed successfully");
                
                return result;
                
            } catch (Exception e) {
                logger.error("Development request failed", e);
                DevelopmentResult errorResult = new DevelopmentResult();
                errorResult.setSuccess(false);
                errorResult.setErrorMessage("Development failed: " + e.getMessage());
                return errorResult;
            }
        });
    }
    
    /**
     * Execute code in sandbox environment
     */
    public CompletableFuture<String> executeCode(String code, String language) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sandboxManager.executeCode(code, language).join();
            } catch (Exception e) {
                logger.error("Code execution failed", e);
                return "Execution failed: " + e.getMessage();
            }
        });
    }
    
    /**
     * Install dependencies for a project
     */
    public CompletableFuture<String> installDependencies(String language, String[] dependencies) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dependencyManager.installDependencies(language, dependencies).join();
            } catch (Exception e) {
                logger.error("Dependency installation failed", e);
                return "Installation failed: " + e.getMessage();
            }
        });
    }
    
    /**
     * Setup development environment for a specific language
     */
    private String setupDevelopmentEnvironment(String language, String analysis) {
        try {
            // Extract dependencies from AI analysis
            String[] dependencies = extractDependencies(analysis);
            
            if (dependencies.length > 0) {
                String installResult = dependencyManager.installDependencies(language, dependencies).join();
                return "Environment setup completed:\n" + installResult;
            }
            
            return "Environment setup completed - no additional dependencies required";
            
        } catch (Exception e) {
            logger.error("Environment setup failed", e);
            return "Environment setup failed: " + e.getMessage();
        }
    }
    
    /**
     * Execute code in sandbox
     */
    private String executeInSandbox(String code, String language) {
        try {
            String result = sandboxManager.executeCode(code, language).join();
            return "Sandbox execution result:\n" + result;
        } catch (Exception e) {
            logger.error("Sandbox execution failed", e);
            return "Sandbox execution failed: " + e.getMessage();
        }
    }
    
    /**
     * Extract dependencies from AI analysis text
     */
    private String[] extractDependencies(String analysis) {
        // Simple pattern matching to extract common dependency patterns
        String[] patterns = {
            "pip install ([\\w-]+)",
            "npm install ([\\w-]+)",
            "apt-get install ([\\w-]+)",
            "dependency: ([\\w-]+)",
            "library: ([\\w-]+)",
            "framework: ([\\w-]+)"
        };
        
        java.util.Set<String> dependencies = new java.util.HashSet<>();
        
        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(analysis);
            while (m.find()) {
                dependencies.add(m.group(1));
            }
        }
        
        return dependencies.toArray(new String[0]);
    }
    
    /**
     * Shutdown the AI agent
     */
    public void shutdown() {
        try {
            ollamaClient.close();
            webQueryService.close();
            sandboxManager.close();
            dependencyManager.close();
            logger.info("AI Agent shutdown completed");
        } catch (Exception e) {
            logger.error("Error during AI Agent shutdown", e);
        }
    }
    
    /**
     * Result class for development operations
     */
    public static class DevelopmentResult {
        private String requirement;
        private String language;
        private String analysis;
        private String environmentSetup;
        private String generatedCode;
        private String executionResult;
        private boolean success;
        private String errorMessage;
        
        // Getters and setters
        public String getRequirement() { return requirement; }
        public void setRequirement(String requirement) { this.requirement = requirement; }
        
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        
        public String getAnalysis() { return analysis; }
        public void setAnalysis(String analysis) { this.analysis = analysis; }
        
        public String getEnvironmentSetup() { return environmentSetup; }
        public void setEnvironmentSetup(String environmentSetup) { this.environmentSetup = environmentSetup; }
        
        public String getGeneratedCode() { return generatedCode; }
        public void setGeneratedCode(String generatedCode) { this.generatedCode = generatedCode; }
        
        public String getExecutionResult() { return executionResult; }
        public void setExecutionResult(String executionResult) { this.executionResult = executionResult; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Development Result:\n");
            sb.append("Requirement: ").append(requirement).append("\n");
            sb.append("Language: ").append(language).append("\n");
            sb.append("Success: ").append(success).append("\n");
            
            if (success) {
                sb.append("\nAnalysis:\n").append(analysis).append("\n");
                sb.append("\nEnvironment Setup:\n").append(environmentSetup).append("\n");
                sb.append("\nGenerated Code:\n").append(generatedCode).append("\n");
                sb.append("\nExecution Result:\n").append(executionResult).append("\n");
            } else {
                sb.append("Error: ").append(errorMessage).append("\n");
            }
            
            return sb.toString();
        }
    }
}
