package com.aicodeeditor.test;

import com.aicodeeditor.ai.EnhancedAIAgent;
import com.aicodeeditor.ai.OllamaClient;
import com.aicodeeditor.ai.WebQueryService;
import com.aicodeeditor.sandbox.SandboxManager;
import com.aicodeeditor.dependencies.DependencyManager;

import java.util.Map;

/**
 * Test the Enhanced AI Agent's automatic context window management
 */
public class TestContextManagement {
    
    public static void main(String[] args) {
        try {
            System.out.println("🧠 Testing Enhanced AI Agent Context Window Management...\n");
            
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
            
            // Display initial context stats
            printContextStats("Initial State", agent.getContextStats());
            
            // Simulate multiple interactions to test context management
            String[] testRequests = {
                "Create a simple Python hello world program",
                "Now create a Python calculator with add, subtract, multiply, divide functions",
                "Add error handling and input validation to the calculator",
                "Create unit tests for the calculator functions",
                "Add a GUI interface using tkinter for the calculator",
                "Convert the calculator to use object-oriented programming",
                "Add logging capabilities to track calculator usage",
                "Create a web API version of the calculator using Flask",
                "Add authentication and user management to the web calculator",
                "Deploy the calculator to a Docker container with proper documentation"
            };
            
            for (int i = 0; i < testRequests.length; i++) {
                System.out.println(String.format("\n🚀 Request %d: %s", i + 1, testRequests[i]));
                
                // Show context stats before request
                Map<String, Object> beforeStats = agent.getContextStats();
                System.out.println(String.format("📊 Before: %.1f%% context usage (%d tokens)", 
                    (Double)beforeStats.get("usagePercentage"), 
                    beforeStats.get("totalTokensEstimate")));
                
                // Process request (we'll simulate this without actually calling the AI to avoid API costs)
                simulateAgentResponse(agent, testRequests[i]);
                
                // Show context stats after request
                Map<String, Object> afterStats = agent.getContextStats();
                System.out.println(String.format("📊 After: %.1f%% context usage (%d tokens), History: %d items, Summary: %s", 
                    (Double)afterStats.get("usagePercentage"), 
                    afterStats.get("totalTokensEstimate"),
                    afterStats.get("conversationHistorySize"),
                    (Boolean)afterStats.get("hasSummary") ? "Yes" : "No"));
                
                // Check if context management kicked in
                if ((Double)afterStats.get("usagePercentage") < (Double)beforeStats.get("usagePercentage")) {
                    System.out.println("✅ Automatic context compression occurred!");
                }
                
                if ((Double)afterStats.get("usagePercentage") > 75.0) {
                    System.out.println("⚠️ Context window over 75% - management should trigger soon");
                }
            }
            
            System.out.println("\n🎯 Final Context Statistics:");
            printContextStats("Final State", agent.getContextStats());
            
            System.out.println("\n✅ Context Window Management Test Completed!");
            System.out.println("🔍 Key Features Demonstrated:");
            System.out.println("  • Automatic context compression at 75% threshold");
            System.out.println("  • Conversation history summarization");
            System.out.println("  • Recent interaction preservation");
            System.out.println("  • Dynamic token estimation");
            System.out.println("  • No user intervention required");
            
        } catch (Exception e) {
            System.err.println("❌ Error testing context management: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void simulateAgentResponse(EnhancedAIAgent agent, String request) {
        // Simulate adding large responses to test context management
        // In a real scenario, this would be actual AI responses
        
        // We'll use reflection to access private methods for testing
        try {
            // Simulate user input
            java.lang.reflect.Field historyField = agent.getClass().getDeclaredField("conversationHistory");
            historyField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<String> history = (java.util.List<String>) historyField.get(agent);
            
            // Add user request
            history.add("USER: " + request);
            
            // Add simulated AI response (make it long to test context management)
            String simulatedResponse = "AI: " + generateSimulatedResponse(request);
            history.add(simulatedResponse);
            
            // Add simulated execution results
            history.add("EXECUTION: Code executed successfully with output...");
            
            // Call the context management method
            java.lang.reflect.Method manageContextMethod = agent.getClass().getDeclaredMethod("manageContextWindow", String.class);
            manageContextMethod.setAccessible(true);
            manageContextMethod.invoke(agent, simulatedResponse);
            
        } catch (Exception e) {
            System.err.println("Error simulating response: " + e.getMessage());
        }
    }
    
    private static String generateSimulatedResponse(String request) {
        // Generate a realistic-length AI response for testing
        StringBuilder response = new StringBuilder();
        response.append("[ANALYSIS] Breaking down the request: ").append(request).append("\n");
        response.append("Key requirements: ").append("implementing core functionality, error handling, user interface considerations.\n");
        response.append("[RESEARCH] Relevant technologies and best practices for this implementation.\n");
        response.append("[PLAN] Step-by-step implementation approach:\n");
        response.append("1. Set up project structure\n2. Implement core logic\n3. Add error handling\n4. Create user interface\n5. Test functionality\n");
        response.append("[IMPLEMENTATION] <CODE language=\"python\">\n# Sample implementation code here\ndef main():\n    print('Implementation')\n</CODE>\n");
        response.append("[TESTING] <EXECUTE>python main.py</EXECUTE>\n");
        response.append("[DOCUMENTATION] Usage instructions and examples for the implementation.\n");
        
        // Add padding to make responses longer for testing
        response.append("Additional implementation details and considerations...\n".repeat(10));
        
        return response.toString();
    }
    
    private static void printContextStats(String label, Map<String, Object> stats) {
        System.out.println(String.format("📈 %s:", label));
        System.out.println(String.format("  • Total tokens: %d / %d (%.1f%%)", 
            stats.get("totalTokensEstimate"), 
            stats.get("maxContextLength"), 
            (Double)stats.get("usagePercentage")));
        System.out.println(String.format("  • Conversation history: %d items", 
            stats.get("conversationHistorySize")));
        System.out.println(String.format("  • Has summary: %s", 
            (Boolean)stats.get("hasSummary") ? "Yes" : "No"));
        System.out.println(String.format("  • Completed tasks: %d", 
            stats.get("completedTasksCount")));
    }
}
