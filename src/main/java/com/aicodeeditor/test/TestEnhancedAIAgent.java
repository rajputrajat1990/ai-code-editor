package com.aicodeeditor.test;

import com.aicodeeditor.ai.EnhancedAIAgent;
import com.aicodeeditor.ai.OllamaClient;
import com.aicodeeditor.ai.WebQueryService;
import com.aicodeeditor.sandbox.SandboxManager;
import com.aicodeeditor.dependencies.DependencyManager;

/**
 * Test the Enhanced AI Agent capabilities
 */
public class TestEnhancedAIAgent {
    public static void main(String[] args) {
        try {
            System.out.println("🤖 Testing Enhanced AI Agent...");
            
            // Create services
            OllamaClient ollamaClient = new OllamaClient();
            WebQueryService webQueryService = new WebQueryService();
            SandboxManager sandboxManager = new SandboxManager();
            DependencyManager dependencyManager = new DependencyManager();
            
            // Create enhanced AI agent
            EnhancedAIAgent agent = new EnhancedAIAgent(
                ollamaClient, webQueryService, sandboxManager, dependencyManager,
                System.getProperty("user.dir")  // Use current directory for testing
            );
            
            // Initialize agent
            System.out.println("🔧 Initializing agent...");
            agent.initialize().get();
            System.out.println("✅ Agent initialized successfully!");
            
            // Test agent capabilities
            String testRequest = "Create a simple Python calculator that can add, subtract, multiply, and divide two numbers";
            
            System.out.println("\n📝 Test Request: " + testRequest);
            System.out.println("\n🚀 Agent is processing...");
            
            String result = agent.developSoftware(testRequest).get();
            
            System.out.println("\n=== AI AGENT RESULT ===");
            System.out.println(result);
            
            System.out.println("\n✨ Enhanced AI Agent test completed!");
            
        } catch (Exception e) {
            System.err.println("❌ Error testing Enhanced AI Agent: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
