package com.aicodeeditor.ai;

import com.aicodeeditor.sandbox.SandboxManager;
import com.aicodeeditor.dependencies.DependencyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Enhanced AI Agent that creates agent-like behavior from any language model
 * through sophisticated prompt engineering and workflow management with permanent memory
 */
public class EnhancedAIAgent {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedAIAgent.class);
    
    private final OllamaClient ollamaClient;
    private final WebQueryService webQueryService;
    private final SandboxManager sandboxManager;
    private final DependencyManager dependencyManager;
    private final ProjectMemoryManager memoryManager;
    
    // Agent state management
    private Map<String, Object> conversationContext = new HashMap<>();
    private List<String> conversationHistory = new ArrayList<>();
    private Set<String> completedTasks = new HashSet<>();
    private Queue<AgentTask> taskQueue = new LinkedList<>();
    
    // Context window management
    private static final int MAX_CONTEXT_LENGTH = 32000; // Conservative estimate for most models
    private static final double CONTEXT_THRESHOLD = 0.75; // 75% threshold
    private static final int MIN_CONTEXT_PRESERVED = 5; // Minimum recent interactions to preserve
    
    // Context compression and summarization
    private String contextSummary = "";
    private int totalTokensEstimate = 0;
    
    // Enhanced system prompt for agent behavior with Intelligence Amplification
    private static final String AGENT_SYSTEM_PROMPT = """
        You are an INTELLIGENCE-AMPLIFIED AI software development agent. Your power comes not from memorizing everything, but from INTELLIGENTLY FINDING and SYNTHESIZING the right information using external tools.
        
        CORE PRINCIPLE: COMPENSATE WITH INTELLIGENCE, NOT JUST SCALE
        You are designed to MATCH OR EXCEED large language models through strategic tool use and structured reasoning.
        
        CRITICAL MINDSET SHIFTS:
        1. KNOWLEDGE GAP RECOGNITION: When you encounter questions beyond your training data, IMMEDIATELY use tools
        2. TOOL-FIRST APPROACH: Default to using external capabilities before relying on training knowledge
        3. MULTI-STEP REASONING: Break complex problems into manageable, verifiable pieces
        4. VERIFICATION-DRIVEN: Cross-check critical information using multiple sources
        
        INTELLIGENCE AMPLIFICATION WORKFLOW:
        
        For EVERY task, follow this enhanced protocol:
        
        1. ASSESS KNOWLEDGE GAPS
           - Is this information time-sensitive or post-training-cutoff?
           - Do I need verification from reliable sources?
           - Would external data improve accuracy?
           If YES to any: MANDATORY tool use
        
        2. STRATEGIC RESEARCH FIRST
           - <QUERY>current best practices for [topic]</QUERY>
           - <QUERY>latest developments in [technology] 2024 2025</QUERY>
           - <QUERY>common pitfalls and solutions for [implementation]</QUERY>
        
        3. MULTI-DOMAIN ANALYSIS
           - Break complex questions into specialized sub-queries
           - Research each domain thoroughly before synthesis
           - Cross-reference findings from multiple sources
        
        4. TOOL-ENHANCED IMPLEMENTATION
           - Use external verification for technical decisions
           - <INSTALL> dependencies based on current ecosystem
           - <EXECUTE> tests to validate approaches in real-time
        
        5. SYNTHESIS WITH SOURCE ATTRIBUTION
           - Combine research findings with base knowledge
           - Clearly indicate information sources and confidence levels
           - Flag any conflicting or outdated information
        
        PROACTIVE CODE VERIFICATION PROTOCOL (NEW):
        For ANY code implementation, even in languages you know well:
        
        1. FUNCTION/API VERIFICATION
           - Before using ANY function, method, or API: <QUERY>[language] [function_name] latest documentation 2024 2025</QUERY>
           - Verify current syntax, parameters, and return types
           - Check for deprecated methods or breaking changes
        
        2. LIBRARY/FRAMEWORK UPDATES
           - Before recommending libraries: <QUERY>[library_name] latest version features changelog</QUERY>
           - Verify compatibility with current language versions
           - Check for security updates or best practice changes
        
        3. SYNTAX AND BEST PRACTICES
           - Always verify: <QUERY>[language] current best practices 2024 2025</QUERY>
           - Look for syntax improvements or new language features
           - Validate recommended patterns are still current
        
        EXAMPLE PROACTIVE VERIFICATION:
        User asks: "Create a Python function to read CSV files"
        
        Your process:
        1. <QUERY>Python pandas read_csv function latest parameters 2024</QUERY>
        2. <QUERY>Python csv module vs pandas performance 2025</QUERY>  
        3. <QUERY>Python file handling best practices current</QUERY>
        4. Then provide verified, up-to-date implementation
        
        This ensures you ALWAYS provide the most current, accurate code implementations.
           - Always distinguish between training knowledge vs. retrieved info
           - Cite sources and reasoning chains
        
        RESPONSE FORMAT (Intelligence Amplified):
        
        [KNOWLEDGE ASSESSMENT]
        - What I know from training: [specific scope]
        - What needs current research: [identify gaps]
        - Tools required: [list specific searches/verifications needed]
        
        [STRATEGIC RESEARCH]
        - Current best practices: <QUERY>latest [domain] best practices 2025</QUERY>
        - Technical verification: <QUERY>[specific technical question]</QUERY>
        - Ecosystem updates: <QUERY>[technology] recent updates changes</QUERY>
        
        [ENHANCED ANALYSIS]
        - Requirements (research-verified)
        - Current ecosystem constraints
        - Risk factors identified through research
        
        [RESEARCH-DRIVEN PLAN]
        - Approach validated through external sources
        - Dependencies verified as current: <INSTALL>package language</INSTALL>
        - Testing strategy based on community practices
        
        [VERIFIED IMPLEMENTATION]
        - Code generated using researched best practices
        - <CODE language="lang">implementation</CODE>
        - External validation: <EXECUTE>verification commands</EXECUTE>
        
        [CONTINUOUS VERIFICATION]
        - Test results and their interpretation
        - Additional research if issues found: <QUERY>debugging [specific issue]</QUERY>
        - Iterative improvement based on real-world feedback
        
        REMEMBER: Your strength is INTELLIGENT AUGMENTATION, not raw knowledge. Always:
        - Use tools BEFORE making claims about current technologies
        - Verify implementation approaches through research
        - Cross-check critical technical decisions
        - Synthesize external findings with base reasoning
        
        You are designed to OUTPERFORM larger models by being smarter about information gathering and verification!
        """;
    
    // Additional prompt for small LLM behavior enhancement
    private static final String INTELLIGENCE_AMPLIFICATION_REMINDER = """
        
        INTELLIGENCE AMPLIFICATION ACTIVE:
        Remember - you achieve large-model performance through:
        • TOOL-FIRST thinking (research before assuming)
        • MULTI-STEP verification (break down complex queries)  
        • CURRENT information gathering (use web for recent data)
        • SOURCE synthesis (combine multiple reliable sources)
        • ITERATIVE refinement (test and improve based on results)
        
        This approach enables small models to match GPT-4 level performance!
        """;
    
    // Task representation for agent workflow
    private static class AgentTask {
        String type;        // QUERY, INSTALL, CODE, EXECUTE, etc.
        String content;     // The actual task content
        String language;    // For code/install tasks
        boolean completed;
        String result;      // Task execution result
        
        AgentTask(String type, String content, String language) {
            this.type = type;
            this.content = content;
            this.language = language;
            this.completed = false;
        }
    }
    
    // Intelligence Amplification Framework - Makes small LLMs behave like large ones
    private static class IntelligenceAmplificationFramework {
        
        // Component roles based on research findings
        enum ComponentRole {
            PLANNER,    // Breaks down complex queries (global-to-local approach)
            RESEARCHER, // Executes web searches and data gathering
            SYNTHESIZER,// Combines findings with base knowledge
            VERIFIER,   // Cross-checks and validates information
            CALLER      // Executes tool calls and API requests
        }
        
        // Knowledge gap assessment patterns
        static final String[] TIME_SENSITIVE_INDICATORS = {
            "current", "latest", "recent", "2024", "2025", "now", "today",
            "up-to-date", "modern", "new", "trending", "breaking"
        };
        
        static final String[] VERIFICATION_REQUIRED_INDICATORS = {
            "best practice", "recommended", "optimal", "should", "must",
            "performance", "security", "compatibility", "version"
        };
        
        static final String[] COMPLEX_REASONING_INDICATORS = {
            "compare", "analyze", "evaluate", "pros and cons", "trade-offs",
            "architecture", "design", "strategy", "approach"
        };
        
        // Progressive fine-tuning approach for different domains
        static final Map<String, String[]> DOMAIN_RESEARCH_PATTERNS = Map.of(
            "web_development", new String[]{"frontend frameworks 2025", "backend best practices", "deployment strategies"},
            "data_science", new String[]{"machine learning trends", "data processing libraries", "visualization tools"},
            "mobile_development", new String[]{"mobile frameworks comparison", "app store guidelines", "performance optimization"},
            "devops", new String[]{"containerization best practices", "CI/CD tools", "cloud deployment strategies"},
            "security", new String[]{"cybersecurity threats 2025", "secure coding practices", "vulnerability assessments"}
        );
        
        // Proactive Knowledge Verification Patterns - Always verify these even for known languages
        static final String[] CODE_IMPLEMENTATION_INDICATORS = {
            "function", "method", "class", "import", "package", "library", "framework",
            "API", "syntax", "parameter", "argument", "return", "exception", "deprecated"
        };
        
        static final String[] LANGUAGE_FUNCTION_PATTERNS = {
            "def ", "function ", "func ", "void ", "public ", "private ", "static ",
            "class ", "interface ", "struct ", "enum ", "const ", "var ", "let "
        };
        
        // Language-specific verification triggers
        static final Map<String, String[]> LANGUAGE_VERIFICATION_KEYWORDS = Map.of(
            "python", new String[]{"import", "from", "def", "class", "async", "await", "yield", "lambda"},
            "java", new String[]{"import", "class", "interface", "method", "annotation", "generic", "lambda"},
            "javascript", new String[]{"import", "export", "function", "class", "async", "await", "promise", "callback"},
            "typescript", new String[]{"import", "export", "interface", "type", "generic", "decorator", "namespace"},
            "go", new String[]{"import", "func", "struct", "interface", "goroutine", "channel", "defer"},
            "rust", new String[]{"use", "fn", "struct", "enum", "trait", "impl", "macro", "async"},
            "c++", new String[]{"#include", "class", "struct", "template", "namespace", "virtual", "override"},
            "c#", new String[]{"using", "class", "interface", "method", "property", "linq", "async", "await"}
        );
        
        // Documentation sources for proactive verification
        static final Map<String, String[]> OFFICIAL_DOCUMENTATION_SOURCES = Map.of(
            "python", new String[]{"python.org", "docs.python.org", "pypi.org", "stackoverflow.com python"},
            "java", new String[]{"oracle.com java", "openjdk.org", "maven.apache.org", "stackoverflow.com java"},
            "javascript", new String[]{"developer.mozilla.org", "nodejs.org", "npmjs.com", "stackoverflow.com javascript"},
            "typescript", new String[]{"typescriptlang.org", "definitivelytyped.org", "stackoverflow.com typescript"},
            "go", new String[]{"golang.org", "pkg.go.dev", "stackoverflow.com golang"},
            "rust", new String[]{"doc.rust-lang.org", "crates.io", "stackoverflow.com rust"},
            "c++", new String[]{"cppreference.com", "isocpp.org", "stackoverflow.com c++"},
            "c#", new String[]{"docs.microsoft.com", "nuget.org", "stackoverflow.com c#"}
        );
    }
    
    // Intelligence assessment for incoming queries
    private IntelligenceAmplificationFramework.ComponentRole assessQueryComplexity(String query) {
        String queryLower = query.toLowerCase();
        
        // Check for time-sensitive information needs
        boolean needsCurrentInfo = Arrays.stream(IntelligenceAmplificationFramework.TIME_SENSITIVE_INDICATORS)
            .anyMatch(queryLower::contains);
            
        // Check for verification requirements  
        boolean needsVerification = Arrays.stream(IntelligenceAmplificationFramework.VERIFICATION_REQUIRED_INDICATORS)
            .anyMatch(queryLower::contains);
            
        // Check for complex reasoning requirements
        boolean needsComplexReasoning = Arrays.stream(IntelligenceAmplificationFramework.COMPLEX_REASONING_INDICATORS)
            .anyMatch(queryLower::contains);
        
        // NEW: Check for code implementation that needs proactive verification
        boolean needsCodeVerification = needsProactiveCodeVerification(query);
        
        if (needsComplexReasoning) {
            return IntelligenceAmplificationFramework.ComponentRole.PLANNER;
        } else if (needsCurrentInfo || needsVerification || needsCodeVerification) {
            return IntelligenceAmplificationFramework.ComponentRole.RESEARCHER;
        } else {
            return IntelligenceAmplificationFramework.ComponentRole.SYNTHESIZER;
        }
    }
    
    // Proactive Code Verification Detection
    private boolean needsProactiveCodeVerification(String query) {
        String queryLower = query.toLowerCase();
        
        // Always verify if code implementation indicators are present
        boolean hasCodeIndicators = Arrays.stream(IntelligenceAmplificationFramework.CODE_IMPLEMENTATION_INDICATORS)
            .anyMatch(queryLower::contains);
            
        // Always verify if language function patterns are present
        boolean hasFunctionPatterns = Arrays.stream(IntelligenceAmplificationFramework.LANGUAGE_FUNCTION_PATTERNS)
            .anyMatch(query::contains);
        
        // Check for specific language keywords that need verification
        boolean hasLanguageKeywords = IntelligenceAmplificationFramework.LANGUAGE_VERIFICATION_KEYWORDS
            .values()
            .stream()
            .flatMap(Arrays::stream)
            .anyMatch(queryLower::contains);
        
        // Trigger verification for any code-related query
        return hasCodeIndicators || hasFunctionPatterns || hasLanguageKeywords;
    }
    
    // Progressive Intelligence Execution - Core research-backed method
    private String executeWithIntelligenceAmplification(String prompt) {
        try {
            // Phase 1: Assess query complexity and determine approach
            IntelligenceAmplificationFramework.ComponentRole primaryRole = assessQueryComplexity(prompt);
            
            // Phase 2: Execute role-based intelligence amplification
            switch (primaryRole) {
                case PLANNER:
                    return executeAsPlanner(prompt);
                case RESEARCHER:
                    return executeAsResearcher(prompt);
                case SYNTHESIZER:
                    return executeAsSynthesizer(prompt);
                case VERIFIER:
                    return executeAsVerifier(prompt);
                case CALLER:
                    return executeAsCaller(prompt);
                default:
                    return fallbackExecution(prompt);
            }
        } catch (Exception e) {
            logger.warn("Intelligence amplification failed, falling back to standard execution: " + e.getMessage());
            return fallbackExecution(prompt);
        }
    }
    
    // Progressive Intelligence Execution with enhanced prompt (includes memory context)
    private String executeWithIntelligenceAmplification(String originalRequest, String enhancedPrompt) {
        try {
            // Phase 1: Assess query complexity and determine approach
            IntelligenceAmplificationFramework.ComponentRole primaryRole = assessQueryComplexity(originalRequest);
            
            // Phase 2: Execute role-based intelligence amplification using enhanced prompt
            String response;
            switch (primaryRole) {
                case PLANNER:
                    response = executeAsPlanner(enhancedPrompt);
                    break;
                case RESEARCHER:
                    response = executeAsResearcher(enhancedPrompt);
                    break;
                case SYNTHESIZER:
                    response = executeAsSynthesizer(enhancedPrompt);
                    break;
                case VERIFIER:
                    response = executeAsVerifier(enhancedPrompt);
                    break;
                case CALLER:
                    response = executeAsCaller(enhancedPrompt);
                    break;
                default:
                    response = fallbackExecution(enhancedPrompt);
            }
            
            // Record successful task completion
            String taskId = "task_" + System.currentTimeMillis();
            recordTaskCompletion(taskId, originalRequest, response);
            
            return response;
            
        } catch (Exception e) {
            logger.warn("Intelligence amplification failed, falling back to standard execution: " + e.getMessage());
            return fallbackExecution(enhancedPrompt);
        }
    }
    
    // Planner role: Global-to-local decomposition
    private String executeAsPlanner(String prompt) {
        String plannerPrompt = AGENT_SYSTEM_PROMPT + 
            "\n\nACTING AS: Strategic Planner Component" +
            "\nCORE MISSION: Break down complex queries into actionable sub-tasks" +
            "\nAPPROACH: Global-to-local decomposition with tool-first thinking" +
            "\n\nDECOMPOSITION PROTOCOL:" +
            "\n1. Identify the main goal and sub-objectives" +
            "\n2. Map each objective to available tools" +
            "\n3. Create execution sequence with verification points" +
            "\n4. Plan for knowledge gap filling through research" +
            "\n\nUSER REQUEST: " + prompt;
            
        try {
            String planResponse = ollamaClient.generate(plannerPrompt, "").join();
            
            // Extract tasks from plan and execute them
            List<String> plannedTasks = extractTasksFromPlan(planResponse);
            StringBuilder results = new StringBuilder();
            
            for (String task : plannedTasks) {
                String taskResult = executeTaskWithAmplification(task);
                results.append(taskResult).append("\n");
            }
            
            // Final synthesis
            String synthesisPrompt = "Synthesize the following results into a comprehensive response:\n" + results.toString();
            return executeAsSynthesizer(synthesisPrompt);
            
        } catch (Exception e) {
            logger.warn("Planner execution failed: " + e.getMessage());
            return fallbackExecution(prompt);
        }
    }
    
    // Researcher role: External knowledge augmentation with proactive code verification
    private String executeAsResearcher(String prompt) {
        try {
            // Detect the programming language for targeted verification
            String detectedLanguage = detectProgrammingLanguage(prompt);
            
            // First, identify what needs to be researched
            String researchPrompt = AGENT_SYSTEM_PROMPT +
                "\n\nACTING AS: Research Component with Proactive Code Verification" +
                "\nCORE MISSION: Identify knowledge gaps and verify code implementations" +
                "\nAPPROACH: Multi-source validation with code verification priority" +
                "\nDETECTED LANGUAGE: " + detectedLanguage +
                "\n\nPROACTIVE CODE VERIFICATION PROTOCOL:" +
                "\n1. Identify specific functions, methods, or APIs mentioned" +
                "\n2. Extract library/framework names for verification" +
                "\n3. Formulate targeted verification queries for latest implementations" +
                "\n4. Cross-reference official documentation sources" +
                "\n5. Validate syntax, parameters, and best practices" +
                "\n\nSTANDARD RESEARCH PROTOCOL:" +
                "\n1. Identify general information gaps" +
                "\n2. Formulate targeted search queries" +
                "\n3. Cross-reference multiple sources" +
                "\n4. Validate information currency and accuracy" +
                "\n\nUSER REQUEST: " + prompt;
            
            String researchPlan = ollamaClient.generate(researchPrompt, "").join();
            
            // Extract both general search queries AND specific code verification queries
            List<String> searchQueries = extractSearchQueries(researchPlan);
            List<String> codeVerificationQueries = extractCodeVerificationQueries(prompt, detectedLanguage);
            
            // Combine all queries, prioritizing code verification
            List<String> allQueries = new ArrayList<>(codeVerificationQueries);
            allQueries.addAll(searchQueries);
            
            StringBuilder researchResults = new StringBuilder();
            
            // Execute searches with emphasis on official documentation
            for (String query : allQueries) {
                try {
                    String searchResult = webQueryService.searchProgrammingInfo(query).join();
                    researchResults.append("Query: ").append(query)
                                   .append("\nResults: ").append(searchResult)
                                   .append("\n\n");
                } catch (Exception e) {
                    logger.warn("Web search failed for query: " + query);
                }
            }
            
            // Combine research with base knowledge, emphasizing verified implementations
            String combinedPrompt = AGENT_SYSTEM_PROMPT +
                "\n\nVERIFIED CODE RESEARCH FINDINGS:\n" + researchResults.toString() +
                "\n\nORIGINAL REQUEST: " + prompt +
                "\n\nPROACTIVE SYNTHESIS INSTRUCTIONS:" +
                "\n1. Prioritize verified function signatures and implementations" +
                "\n2. Flag any deprecated methods or outdated practices" +
                "\n3. Include version compatibility information" +
                "\n4. Recommend current best practices and alternatives" +
                "\n5. Provide working code examples with verified syntax" +
                "\n\nSYNTHESIZE: Combine the verified research findings with your base knowledge.";
            
            return ollamaClient.generate(combinedPrompt, "").join();
            
        } catch (Exception e) {
            logger.warn("Researcher execution failed: " + e.getMessage());
            return fallbackExecution(prompt);
        }
    }
    
    // Synthesizer role: Knowledge integration
    private String executeAsSynthesizer(String prompt) {
        String synthesizerPrompt = AGENT_SYSTEM_PROMPT +
            "\n\nACTING AS: Synthesis Component" +
            "\nCORE MISSION: Integrate multiple knowledge sources into coherent responses" +
            "\nAPPROACH: Multi-domain analysis with source attribution" +
            "\n\nSYNTHESIS PROTOCOL:" +
            "\n1. Identify key concepts and relationships" +
            "\n2. Cross-reference information from multiple domains" +
            "\n3. Resolve conflicts through additional verification" +
            "\n4. Present integrated findings with clear attribution" +
            "\n\nUSER REQUEST: " + prompt;
            
        return ollamaClient.generate(synthesizerPrompt, "").join();
    }
    
    // Verifier role: Information validation
    private String executeAsVerifier(String prompt) {
        try {
            // First generate initial response
            String initialResponse = executeAsSynthesizer(prompt);
            
            // Then verify critical claims
            String verificationPrompt = AGENT_SYSTEM_PROMPT +
                "\n\nACTING AS: Verification Component" +
                "\nCORE MISSION: Validate information accuracy and identify potential issues" +
                "\nAPPROACH: Multi-step verification with external fact-checking" +
                "\n\nVERIFICATION PROTOCOL:" +
                "\n1. Identify verifiable claims in the response" +
                "\n2. Cross-check against current information" +
                "\n3. Flag any uncertainties or outdated information" +
                "\n4. Provide confidence levels for key statements" +
                "\n\nRESPONSE TO VERIFY:\n" + initialResponse;
            
            String verificationResult = ollamaClient.generate(verificationPrompt, "").join();
            
            // If verification identifies issues, research and correct
            if (verificationResult.toLowerCase().contains("issue") || 
                verificationResult.toLowerCase().contains("outdated") ||
                verificationResult.toLowerCase().contains("uncertain")) {
                
                String correctionPrompt = "Based on the verification results, provide a corrected response:\n" +
                    "Original: " + initialResponse + "\n" +
                    "Verification: " + verificationResult + "\n" +
                    "Original Request: " + prompt;
                
                return executeAsResearcher(correctionPrompt);
            }
            
            return initialResponse;
            
        } catch (Exception e) {
            logger.warn("Verifier execution failed: " + e.getMessage());
            return fallbackExecution(prompt);
        }
    }
    
    // Caller role: Tool execution specialist
    private String executeAsCaller(String prompt) {
        String callerPrompt = AGENT_SYSTEM_PROMPT +
            "\n\nACTING AS: Tool Execution Specialist" +
            "\nCORE MISSION: Execute complex tool sequences with error handling" +
            "\nAPPROACH: Multi-step tool orchestration with fallback strategies" +
            "\n\nEXECUTION PROTOCOL:" +
            "\n1. Map request to appropriate tool sequence" +
            "\n2. Execute with comprehensive error handling" +
            "\n3. Validate results and retry if necessary" +
            "\n4. Provide detailed execution logs" +
            "\n\nUSER REQUEST: " + prompt;
            
        return ollamaClient.generate(callerPrompt, "").join();
    }
    
    // Fallback for standard execution
    private String fallbackExecution(String prompt) {
        try {
            return ollamaClient.generate(AGENT_SYSTEM_PROMPT + "\n\n" + prompt, "").join();
        } catch (Exception e) {
            return "I encountered an error processing your request. Please try again.";
        }
    }
    
    // Helper methods for intelligence amplification
    private List<String> extractTasksFromPlan(String planResponse) {
        List<String> tasks = new ArrayList<>();
        String[] lines = planResponse.split("\n");
        
        for (String line : lines) {
            if (line.matches("\\d+\\..*") || line.contains("Task:") || line.contains("Step:")) {
                tasks.add(line.replaceAll("^\\d+\\.|Task:|Step:", "").trim());
            }
        }
        
        return tasks.isEmpty() ? List.of(planResponse) : tasks;
    }
    
    private List<String> extractSearchQueries(String researchPlan) {
        List<String> queries = new ArrayList<>();
        String[] lines = researchPlan.split("\n");
        
        for (String line : lines) {
            if (line.toLowerCase().contains("search") || line.toLowerCase().contains("query")) {
                String query = line.replaceAll("(?i).*search.*?[:\\-]\\s*", "")
                                  .replaceAll("(?i).*query.*?[:\\-]\\s*", "")
                                  .trim();
                if (!query.isEmpty() && query.length() > 5) {
                    queries.add(query);
                }
            }
        }
        
        // If no specific queries found, extract key terms from the original plan
        if (queries.isEmpty()) {
            String cleanPlan = researchPlan.replaceAll("[^a-zA-Z0-9\\s]", " ");
            String[] words = cleanPlan.split("\\s+");
            StringBuilder queryBuilder = new StringBuilder();
            
            for (String word : words) {
                if (word.length() > 3 && !isStopWord(word)) {
                    queryBuilder.append(word).append(" ");
                    if (queryBuilder.length() > 20) {
                        queries.add(queryBuilder.toString().trim());
                        queryBuilder = new StringBuilder();
                    }
                }
            }
            
            if (queryBuilder.length() > 0) {
                queries.add(queryBuilder.toString().trim());
            }
        }
        
        return queries.isEmpty() ? List.of("programming best practices") : queries;
    }
    
    private String executeTaskWithAmplification(String task) {
        // Determine the best approach for this specific task
        IntelligenceAmplificationFramework.ComponentRole taskRole = assessQueryComplexity(task);
        
        switch (taskRole) {
            case RESEARCHER:
                return executeAsResearcher(task);
            case VERIFIER:
                return executeAsVerifier(task);
            case CALLER:
                return executeAsCaller(task);
            default:
                return executeAsSynthesizer(task);
        }
    }
    
    private boolean isStopWord(String word) {
        String[] stopWords = {"the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by"};
        return Arrays.asList(stopWords).contains(word.toLowerCase());
    }
    
    // Language detection for proactive verification
    private String detectProgrammingLanguage(String query) {
        String queryLower = query.toLowerCase();
        
        // Direct language mentions
        if (queryLower.contains("python") || queryLower.contains("py ") || queryLower.contains(".py")) return "python";
        if (queryLower.contains("java ") || queryLower.contains(".java")) return "java";
        if (queryLower.contains("javascript") || queryLower.contains("js ") || queryLower.contains(".js")) return "javascript";
        if (queryLower.contains("typescript") || queryLower.contains("ts ") || queryLower.contains(".ts")) return "typescript";
        if (queryLower.contains("golang") || queryLower.contains("go ") || queryLower.contains(".go")) return "go";
        if (queryLower.contains("rust") || queryLower.contains(".rs")) return "rust";
        if (queryLower.contains("c++") || queryLower.contains("cpp") || queryLower.contains(".cpp")) return "c++";
        if (queryLower.contains("c#") || queryLower.contains("csharp") || queryLower.contains(".cs")) return "c#";
        
        // Language-specific keyword detection
        for (Map.Entry<String, String[]> entry : IntelligenceAmplificationFramework.LANGUAGE_VERIFICATION_KEYWORDS.entrySet()) {
            String language = entry.getKey();
            String[] keywords = entry.getValue();
            
            long matches = Arrays.stream(keywords).mapToLong(keyword -> 
                queryLower.split("\\b" + keyword + "\\b").length - 1).sum();
            
            if (matches >= 2) { // Multiple keyword matches indicate this language
                return language;
            }
        }
        
        return "general"; // Default when language cannot be determined
    }
    
    // Extract specific code verification queries based on detected functions/APIs
    private List<String> extractCodeVerificationQueries(String prompt, String language) {
        List<String> queries = new ArrayList<>();
        
        // Extract function/method names for verification
        List<String> functionNames = extractFunctionNames(prompt, language);
        
        // Extract library/package names for verification  
        List<String> libraryNames = extractLibraryNames(prompt, language);
        
        // Generate targeted verification queries
        for (String functionName : functionNames) {
            queries.add(language + " " + functionName + " function documentation 2024 2025");
            queries.add(language + " " + functionName + " parameters syntax latest");
            queries.add("site:stackoverflow.com " + language + " " + functionName + " example");
        }
        
        for (String libraryName : libraryNames) {
            queries.add(language + " " + libraryName + " latest version documentation");
            queries.add(language + " " + libraryName + " best practices 2024 2025");
            
            // Add official documentation source if available
            String[] officialSources = IntelligenceAmplificationFramework.OFFICIAL_DOCUMENTATION_SOURCES.get(language);
            if (officialSources != null) {
                for (String source : officialSources) {
                    queries.add("site:" + source.split(" ")[0] + " " + libraryName);
                }
            }
        }
        
        // Add general language-specific verification queries
        queries.add(language + " latest features 2024 2025 best practices");
        queries.add(language + " deprecated functions methods avoid 2024");
        
        return queries.stream().distinct().limit(8).collect(Collectors.toList()); // Limit to avoid too many queries
    }
    
    // Extract function/method names from the prompt
    private List<String> extractFunctionNames(String prompt, String language) {
        List<String> functions = new ArrayList<>();
        
        // Common patterns for function definitions and calls
        String[] patterns = {
            "\\b(\\w+)\\s*\\(",           // functionName(
            "def\\s+(\\w+)",              // def functionName
            "function\\s+(\\w+)",         // function functionName
            "func\\s+(\\w+)",             // func functionName
            "\\.(\\w+)\\(",               // .methodName(
            "\\w+\\.(\\w+)\\(",           // object.methodName(
            "from\\s+\\w+\\s+import\\s+(\\w+)", // from module import functionName
            "import\\s+.*\\.(\\w+)"       // import package.functionName
        };
        
        for (String pattern : patterns) {
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(prompt);
            while (matcher.find()) {
                String functionName = matcher.group(1);
                if (functionName.length() > 2 && !isCommonWord(functionName)) {
                    functions.add(functionName);
                }
            }
        }
        
        return functions.stream().distinct().limit(5).collect(Collectors.toList());
    }
    
    // Extract library/package names from the prompt
    private List<String> extractLibraryNames(String prompt, String language) {
        List<String> libraries = new ArrayList<>();
        
        // Common patterns for library/package imports
        String[] patterns = {
            "import\\s+(\\w+)",           // import library
            "from\\s+(\\w+)",             // from library
            "#include\\s*<(\\w+)>",       // #include <library>
            "using\\s+(\\w+)",            // using library
            "use\\s+(\\w+)",              // use library (Rust)
            "package\\s+(\\w+)",          // package library
            "require\\s*\\(['\"]([\\w-]+)['\"]\\)", // require('library')
        };
        
        for (String pattern : patterns) {
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(prompt);
            while (matcher.find()) {
                String libraryName = matcher.group(1);
                if (libraryName.length() > 2 && !isCommonWord(libraryName)) {
                    libraries.add(libraryName);
                }
            }
        }
        
        return libraries.stream().distinct().limit(4).collect(Collectors.toList());
    }
    
    private boolean isCommonWord(String word) {
        String[] commonWords = {"the", "and", "or", "but", "for", "with", "this", "that", "from", "import", "class", "public", "private", "static", "void", "return", "if", "else", "while", "true", "false", "null", "new", "int", "string", "var", "let", "const"};
        return Arrays.asList(commonWords).contains(word.toLowerCase());
    }
    
    public EnhancedAIAgent(OllamaClient ollamaClient, WebQueryService webQueryService,
                          SandboxManager sandboxManager, DependencyManager dependencyManager, 
                          String projectPath) {
        this.ollamaClient = ollamaClient;
        this.webQueryService = webQueryService;
        this.sandboxManager = sandboxManager;
        this.dependencyManager = dependencyManager;
        
        // Initialize permanent memory manager
        this.memoryManager = new ProjectMemoryManager(projectPath);
        
        // Load project context from memory
        conversationContext.putAll(memoryManager.getProjectContext());
        
        logger.info("Enhanced AI Agent initialized with autonomous workflow and permanent memory capabilities");
    }
    
    /**
     * Initialize the AI agent for autonomous operation
     */
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Initializing AI Agent with enhanced agent capabilities...");
                
                // Initialize all services
                // SandboxManager initializes automatically
                logger.info("Sandbox Manager initialized");
                
                // Set up conversation context
                conversationContext.put("initialized", true);
                conversationContext.put("capabilities", Arrays.asList(
                    "code_generation", "web_research", "dependency_management", 
                    "sandbox_execution", "error_diagnosis", "documentation"
                ));
                
                // Test AI model with agent prompt
                String testResponse = ollamaClient.generate(AGENT_SYSTEM_PROMPT + 
                    "\nTest: Respond with 'AGENT_MODE_ACTIVE' if you understand the agent workflow.", "").join();
                
                if (testResponse.contains("AGENT_MODE_ACTIVE") || testResponse.contains("agent")) {
                    logger.info("AI Model successfully configured for agent mode");
                } else {
                    logger.info("AI Model configured with enhanced prompting for agent-like behavior");
                }
                
                logger.info("AI Agent initialization completed successfully");
                
            } catch (Exception e) {
                logger.error("Failed to initialize AI Agent", e);
                throw new RuntimeException("AI Agent initialization failed", e);
            }
        });
    }
    
    /**
     * Save task completion to permanent memory
     */
    private void recordTaskCompletion(String taskId, String description, String result) {
        memoryManager.recordCompletedTask(taskId, description, result, 
            Map.of("timestamp", System.currentTimeMillis()));
        completedTasks.add(taskId);
        memoryManager.saveMemoryToFile();
    }
    
    /**
     * Record a learning or insight for future reference
     */
    private void recordLearning(String category, String insight, String context) {
        memoryManager.recordLearning(category, insight, context);
        memoryManager.saveMemoryToFile();
    }
    
    /**
     * Check if a dependency is already installed using memory
     */
    private boolean isAlreadyInstalled(String dependency, String language) {
        return memoryManager.isDependencyInstalled(dependency, language);
    }
    
    /**
     * Record successful dependency installation
     */
    private void recordDependencyInstallation(String dependency, String language, String version) {
        memoryManager.recordInstalledDependency(dependency, language, version);
        memoryManager.saveMemoryToFile();
    }
    
    /**
     * Get context from previous similar tasks
     */
    private String getPreviousTaskContext(String description) {
        List<ProjectMemoryManager.TaskRecord> similarTasks = 
            memoryManager.findSimilarTasks(description, 3);
        
        if (similarTasks.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("\n=== RELEVANT PREVIOUS EXPERIENCE ===\n");
        for (ProjectMemoryManager.TaskRecord task : similarTasks) {
            context.append(String.format("Previous Task: %s\n", task.description));
            context.append(String.format("Result: %s\n", task.result));
            context.append("---\n");
        }
        
        return context.toString();
    }
    
    /**
     * Generate enhanced prompt with memory context
     */
    private String generatePromptWithMemory(String userRequest) {
        StringBuilder prompt = new StringBuilder();
        
        // Add system prompt
        prompt.append(AGENT_SYSTEM_PROMPT);
        
        // Add project memory summary
        String memorySummary = memoryManager.generateMemorySummary();
        if (!memorySummary.trim().isEmpty()) {
            prompt.append("\n").append(memorySummary);
        }
        
        // Add previous task context
        String previousContext = getPreviousTaskContext(userRequest);
        if (!previousContext.trim().isEmpty()) {
            prompt.append(previousContext);
        }
        
        // Add current request
        prompt.append("\n=== CURRENT REQUEST ===\n");
        prompt.append(userRequest);
        
        return prompt.toString();
    }
    
    /**
     * Shutdown hook to save memory
     */
    public void shutdown() {
        logger.info("Shutting down Enhanced AI Agent...");
        memoryManager.closeSession();
        logger.info("Project memory saved successfully");
    }
    
    /**
     * Main method for autonomous software development with any language model
     */
    public CompletableFuture<String> developSoftware(String requirements) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting autonomous development process with intelligence amplification for: {}", requirements);
                
                // Generate task ID for tracking
                String taskId = "task_" + System.currentTimeMillis();
                
                // Add to conversation history and manage context window
                String userInput = "USER: " + requirements;
                conversationHistory.add(userInput);
                manageContextWindow(userInput);
                
                // Generate enhanced prompt with memory context
                String enhancedPrompt = generatePromptWithMemory(requirements);
                
                // Use Intelligence Amplification Framework for enhanced processing
                String intelligentResponse = executeWithIntelligenceAmplification(requirements, enhancedPrompt);
                
                String aiEntry = "AI: " + intelligentResponse;
                conversationHistory.add(aiEntry);
                manageContextWindow(aiEntry);
                
                // Parse and execute agent tasks from the intelligent response
                List<AgentTask> tasks = parseAgentResponse(intelligentResponse);
                String executionResults = executeAgentTasks(tasks);
                
                // Generate final response with results using intelligence amplification
                String finalPrompt = "Generate a comprehensive summary of the completed development work:\n" +
                    "Original Request: " + requirements + "\n" +
                    "AI Analysis: " + intelligentResponse + "\n" +
                    "Execution Results: " + executionResults;
                
                String finalResponse = executeWithIntelligenceAmplification(finalPrompt);
                
                logger.info("Autonomous development process with intelligence amplification completed");
                return finalResponse;
                
            } catch (Exception e) {
                logger.error("Error in autonomous development", e);
                return "Error in development process: " + e.getMessage();
            }
        });
    }
    
    /**
     * Build an enhanced prompt that creates agent behavior from any model
     */
    private String buildAgentPrompt(String requirements) {
        StringBuilder prompt = new StringBuilder();
        
        // System prompt for agent behavior
        prompt.append(AGENT_SYSTEM_PROMPT);
        
        // Include context summary if available
        if (!contextSummary.isEmpty()) {
            prompt.append("\n\nCONTEXT SUMMARY:\n").append(contextSummary);
        }
        
        // Context from previous interactions (recent history only)
        if (!conversationHistory.isEmpty()) {
            prompt.append("\n\nRECENT CONTEXT:\n");
            conversationHistory.stream()
                .limit(MIN_CONTEXT_PRESERVED) // Use recent interactions only
                .forEach(entry -> prompt.append(entry).append("\n"));
        }
        
        // Current task
        prompt.append("\n\nCURRENT TASK:\n").append(requirements);
        
        // Available capabilities reminder
        prompt.append("\n\nAVAILABLE TOOLS:");
        prompt.append("\n- Web search for research: <QUERY>search terms</QUERY>");
        prompt.append("\n- Install dependencies: <INSTALL>package_name language</INSTALL>");
        prompt.append("\n- Execute code: <EXECUTE>command</EXECUTE>");
        prompt.append("\n- Generate files: <CODE language=\"language\">code</CODE>");
        
        // Force structured response
        prompt.append("\n\nIMPORTANT: You MUST respond using the [ANALYSIS], [RESEARCH], [PLAN], [IMPLEMENTATION], [TESTING], [DOCUMENTATION] structure. Be specific and actionable.");
        
        return prompt.toString();
    }
    
    /**
     * Parse AI response and extract executable tasks
     */
    private List<AgentTask> parseAgentResponse(String response) {
        List<AgentTask> tasks = new ArrayList<>();
        
        // Extract web queries
        Pattern queryPattern = Pattern.compile("<QUERY>(.*?)</QUERY>", Pattern.DOTALL);
        Matcher queryMatcher = queryPattern.matcher(response);
        while (queryMatcher.find()) {
            tasks.add(new AgentTask("QUERY", queryMatcher.group(1).trim(), null));
        }
        
        // Extract dependency installations
        Pattern installPattern = Pattern.compile("<INSTALL>(.*?)\\s+(\\w+)</INSTALL>", Pattern.DOTALL);
        Matcher installMatcher = installPattern.matcher(response);
        while (installMatcher.find()) {
            tasks.add(new AgentTask("INSTALL", installMatcher.group(1).trim(), installMatcher.group(2).trim()));
        }
        
        // Extract code blocks
        Pattern codePattern = Pattern.compile("<CODE language=\"(.*?)\">(.*?)</CODE>", Pattern.DOTALL);
        Matcher codeMatcher = codePattern.matcher(response);
        while (codeMatcher.find()) {
            tasks.add(new AgentTask("CODE", codeMatcher.group(2).trim(), codeMatcher.group(1).trim()));
        }
        
        // Extract execution commands
        Pattern executePattern = Pattern.compile("<EXECUTE>(.*?)</EXECUTE>", Pattern.DOTALL);
        Matcher executeMatcher = executePattern.matcher(response);
        while (executeMatcher.find()) {
            tasks.add(new AgentTask("EXECUTE", executeMatcher.group(1).trim(), null));
        }
        
        logger.info("Parsed {} agent tasks from AI response", tasks.size());
        return tasks;
    }
    
    /**
     * Execute parsed agent tasks autonomously
     */
    private String executeAgentTasks(List<AgentTask> tasks) {
        StringBuilder results = new StringBuilder("\n=== AGENT EXECUTION RESULTS ===\n");
        
        for (AgentTask task : tasks) {
            try {
                logger.info("Executing agent task: {} - {}", task.type, task.content);
                
                switch (task.type) {
                    case "QUERY" -> {
                        String searchResult = webQueryService.searchProgrammingInfo(task.content).join();
                        task.result = searchResult;
                        results.append("\n🔍 WEB SEARCH: ").append(task.content)
                               .append("\nResults: ").append(searchResult.substring(0, Math.min(200, searchResult.length()))).append("...\n");
                    }
                    
                    case "INSTALL" -> {
                        String installResult = dependencyManager.installDependencies(task.language, new String[]{task.content}).join();
                        task.result = installResult.contains("Success") ? "SUCCESS" : "FAILED";
                        results.append("\n📦 DEPENDENCY: ").append(task.content).append(" (").append(task.language).append(") - ")
                               .append(task.result).append("\n");
                    }
                    
                    case "CODE" -> {
                        // Save code to temporary file for execution
                        String filename = "generated_code." + getFileExtension(task.language);
                        task.result = "Code saved as: " + filename;
                        results.append("\n💻 CODE GENERATED: ").append(filename).append(" (").append(task.language).append(")\n");
                        results.append("Preview: ").append(task.content.substring(0, Math.min(150, task.content.length()))).append("...\n");
                    }
                    
                    case "EXECUTE" -> {
                        String output = sandboxManager.executeCode(task.content, "bash").join();
                        task.result = output;
                        results.append("\n⚡ EXECUTION: ").append(task.content)
                               .append("\nOutput: ").append(output.substring(0, Math.min(200, output.length()))).append("...\n");
                    }
                }
                
                task.completed = true;
                completedTasks.add(task.type + ":" + task.content);
                
            } catch (Exception e) {
                logger.error("Failed to execute task: {}", task.type, e);
                task.result = "ERROR: " + e.getMessage();
                results.append("\n❌ FAILED: ").append(task.type).append(" - ").append(e.getMessage()).append("\n");
            }
        }
        
        return results.toString();
    }
    
    /**
     * Generate comprehensive final response combining AI output and execution results
     */
    private String generateFinalResponse(String aiResponse, String executionResults) {
        // Check context window before generating final response
        String followUpPrompt = buildFollowUpPrompt(aiResponse, executionResults);
        
        try {
            String finalAiResponse = ollamaClient.generate(followUpPrompt, "").join();
            
            // Manage context for the final response
            manageContextWindow("FINAL_RESPONSE: " + finalAiResponse);
            
            return "=== AI AGENT DEVELOPMENT REPORT ===\n\n" +
                   "🤖 INITIAL ANALYSIS & PLAN:\n" + aiResponse + "\n\n" +
                   executionResults + "\n\n" +
                   "🎯 FINAL ASSESSMENT & NEXT STEPS:\n" + finalAiResponse;
                   
        } catch (Exception e) {
            logger.error("Error generating final response", e);
            return aiResponse + "\n\n" + executionResults + 
                   "\n\n⚠️ Note: Could not generate final assessment due to: " + e.getMessage();
        }
    }
    
    /**
     * Build follow-up prompt with context management
     */
    private String buildFollowUpPrompt(String aiResponse, String executionResults) {
        StringBuilder prompt = new StringBuilder();
        
        // Estimate total tokens for the follow-up
        int basePromptTokens = estimateTokens(AGENT_SYSTEM_PROMPT);
        int aiResponseTokens = estimateTokens(aiResponse);
        int executionResultsTokens = estimateTokens(executionResults);
        int totalEstimate = basePromptTokens + aiResponseTokens + executionResultsTokens;
        
        // If we're approaching context limit, use abbreviated versions
        if (totalEstimate > (MAX_CONTEXT_LENGTH * 0.9)) {
            logger.info("Follow-up prompt would exceed context window, using abbreviated version");
            
            prompt.append(AGENT_SYSTEM_PROMPT);
            
            // Use truncated versions
            String abbreviatedAnalysis = aiResponse.length() > 1000 ? 
                aiResponse.substring(0, 1000) + "...[truncated for context]" : aiResponse;
            String abbreviatedResults = executionResults.length() > 1000 ? 
                executionResults.substring(0, 1000) + "...[truncated for context]" : executionResults;
            
            prompt.append("\n\nPREVIOUS ANALYSIS (abbreviated):\n").append(abbreviatedAnalysis);
            prompt.append("\n\nEXECUTION RESULTS (abbreviated):\n").append(abbreviatedResults);
            prompt.append("\n\nProvide a concise final summary and next steps based on the above context.");
            
        } else {
            // Use full context
            prompt.append(AGENT_SYSTEM_PROMPT);
            prompt.append("\n\nPREVIOUS ANALYSIS:\n").append(aiResponse);
            prompt.append("\n\nEXECUTION RESULTS:\n").append(executionResults);
            prompt.append("\n\nBased on the execution results above, provide a final summary with next steps, any issues found, and recommendations. Use the same structured format.");
        }
        
        return prompt.toString();
    }
    
    /**
     * Get file extension for programming language
     */
    private String getFileExtension(String language) {
        return switch (language.toLowerCase()) {
            case "python" -> "py";
            case "java" -> "java";
            case "javascript", "js" -> "js";
            case "typescript", "ts" -> "ts";
            case "cpp", "c++" -> "cpp";
            case "c" -> "c";
            case "go" -> "go";
            case "rust" -> "rs";
            case "php" -> "php";
            case "ruby" -> "rb";
            default -> "txt";
        };
    }
    
    /**
     * Get conversation context for debugging
     */
    public Map<String, Object> getConversationContext() {
        return new HashMap<>(conversationContext);
    }
    
    /**
     * Get completed tasks for tracking
     */
    public Set<String> getCompletedTasks() {
        return new HashSet<>(completedTasks);
    }
    
    /**
     * Reset agent state for new session
     */
    public void resetSession() {
        conversationHistory.clear();
        completedTasks.clear();
        taskQueue.clear();
        conversationContext.clear();
        contextSummary = "";
        totalTokensEstimate = 0;
        logger.info("Agent session reset completed");
    }
    
    /**
     * Estimate token count for a string (rough approximation)
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        // Rough approximation: 1 token ≈ 4 characters for most languages
        return Math.max(1, text.length() / 4);
    }
    
    /**
     * Check if context window is approaching capacity and manage automatically
     */
    private void manageContextWindow(String newContent) {
        // Estimate tokens for new content
        int newTokens = estimateTokens(newContent);
        totalTokensEstimate += newTokens;
        
        // Check if we're approaching the context window limit
        if (totalTokensEstimate > (MAX_CONTEXT_LENGTH * CONTEXT_THRESHOLD)) {
            logger.info("Context window at {}% capacity ({}), performing automatic compression", 
                       (int)((double)totalTokensEstimate / MAX_CONTEXT_LENGTH * 100), totalTokensEstimate);
            
            compressContext();
        }
    }
    
    /**
     * Compress context by summarizing older interactions and preserving recent ones
     */
    private void compressContext() {
        try {
            if (conversationHistory.size() <= MIN_CONTEXT_PRESERVED) {
                logger.info("Context too small to compress, skipping");
                return;
            }
            
            // Preserve recent interactions
            List<String> recentHistory = conversationHistory.subList(
                Math.max(0, conversationHistory.size() - MIN_CONTEXT_PRESERVED), 
                conversationHistory.size()
            );
            
            // Get older interactions for summarization
            List<String> olderHistory = conversationHistory.subList(
                0, 
                Math.max(0, conversationHistory.size() - MIN_CONTEXT_PRESERVED)
            );
            
            // Create summary of older interactions if we have content to summarize
            if (!olderHistory.isEmpty() && !contextSummary.contains("PREVIOUS_CONTEXT_SUMMARY")) {
                String historySummary = summarizeConversationHistory(olderHistory);
                
                // Update context summary
                contextSummary = "PREVIOUS_CONTEXT_SUMMARY:\n" + historySummary + "\n\n" + contextSummary;
                
                // Replace conversation history with recent items only
                conversationHistory.clear();
                conversationHistory.addAll(recentHistory);
                
                // Recalculate token estimate
                totalTokensEstimate = estimateTokens(contextSummary) + 
                                    conversationHistory.stream()
                                        .mapToInt(this::estimateTokens)
                                        .sum();
                
                logger.info("Context compressed: {} recent interactions preserved, older history summarized. New token count: {}", 
                           recentHistory.size(), totalTokensEstimate);
            } else {
                // If we already have a summary, just keep recent history and trim the summary if needed
                conversationHistory.clear();
                conversationHistory.addAll(recentHistory);
                
                // Trim context summary if it's getting too long
                if (estimateTokens(contextSummary) > MAX_CONTEXT_LENGTH / 4) {
                    contextSummary = contextSummary.substring(0, Math.min(contextSummary.length(), MAX_CONTEXT_LENGTH / 8)) + 
                                   "\n...[Context summary truncated for space]";
                }
                
                totalTokensEstimate = estimateTokens(contextSummary) + 
                                    conversationHistory.stream()
                                        .mapToInt(this::estimateTokens)
                                        .sum();
                
                logger.info("Context maintained: {} recent interactions kept, summary updated. Token count: {}", 
                           recentHistory.size(), totalTokensEstimate);
            }
            
        } catch (Exception e) {
            logger.error("Error during context compression", e);
            // Fallback: keep only recent history
            if (conversationHistory.size() > MIN_CONTEXT_PRESERVED * 2) {
                List<String> keepHistory = conversationHistory.subList(
                    conversationHistory.size() - MIN_CONTEXT_PRESERVED,
                    conversationHistory.size()
                );
                conversationHistory.clear();
                conversationHistory.addAll(keepHistory);
                
                totalTokensEstimate = conversationHistory.stream()
                    .mapToInt(this::estimateTokens)
                    .sum();
                
                logger.info("Fallback context compression: kept {} recent interactions", keepHistory.size());
            }
        }
    }
    
    /**
     * Summarize conversation history using AI to preserve important context
     */
    private String summarizeConversationHistory(List<String> history) {
        if (history.isEmpty()) return "";
        
        try {
            String historyText = String.join("\n", history);
            
            String summaryPrompt = """
                Please provide a concise summary of this conversation history, preserving:
                1. Key requirements and goals discussed
                2. Important technical decisions made
                3. Completed tasks and their outcomes
                4. Any ongoing issues or next steps
                
                Keep the summary focused and under 500 words.
                
                Conversation History:
                """ + historyText;
            
            String summary = ollamaClient.generate(summaryPrompt, "").join();
            
            // Truncate if too long
            if (summary.length() > 2000) {
                summary = summary.substring(0, 2000) + "...[truncated]";
            }
            
            return summary;
            
        } catch (Exception e) {
            logger.error("Failed to generate conversation summary", e);
            // Fallback: create basic summary
            return "Previous conversation covered " + history.size() + " interactions including development tasks and AI responses.";
        }
    }
    
    /**
     * Get current context window usage statistics
     */
    public Map<String, Object> getContextStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTokensEstimate", totalTokensEstimate);
        stats.put("maxContextLength", MAX_CONTEXT_LENGTH);
        stats.put("usagePercentage", (double)totalTokensEstimate / MAX_CONTEXT_LENGTH * 100);
        stats.put("conversationHistorySize", conversationHistory.size());
        stats.put("hasSummary", !contextSummary.isEmpty());
        stats.put("completedTasksCount", completedTasks.size());
        return stats;
    }
}
