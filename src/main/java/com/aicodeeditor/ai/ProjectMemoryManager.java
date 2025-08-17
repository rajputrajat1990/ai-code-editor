package com.aicodeeditor.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Permanent Memory System for AI Agent
 * 
 * Maintains persistent project memory across sessions:
 * - Task completion history
 * - Project context and learnings
 * - Code patterns and solutions
 * - Dependencies and configurations
 * - User preferences and patterns
 */
public class ProjectMemoryManager {
    private static final Logger logger = LoggerFactory.getLogger(ProjectMemoryManager.class);
    
    private final ObjectMapper objectMapper;
    private final String projectPath;
    private final File memoryFile;
    private final File sessionLogFile;
    
    // In-memory caches
    private final Map<String, Object> projectContext = new ConcurrentHashMap<>();
    private final List<TaskRecord> completedTasks = new ArrayList<>();
    private final List<LearningRecord> learnings = new ArrayList<>();
    private final Set<String> installedDependencies = ConcurrentHashMap.newKeySet();
    private final Map<String, String> codePatterns = new ConcurrentHashMap<>();
    
    // Session tracking
    private final String sessionId;
    private final List<SessionEvent> currentSessionEvents = new ArrayList<>();
    
    public ProjectMemoryManager(String projectPath) {
        this.objectMapper = new ObjectMapper();
        this.projectPath = projectPath;
        this.sessionId = generateSessionId();
        
        // Create .ai-memory directory in project root
        File memoryDir = new File(projectPath, ".ai-memory");
        if (!memoryDir.exists()) {
            memoryDir.mkdirs();
        }
        
        this.memoryFile = new File(memoryDir, "project-memory.json");
        this.sessionLogFile = new File(memoryDir, "session-" + sessionId + ".log");
        
        // Load existing memory
        loadMemoryFromFile();
        
        // Start session logging
        logSessionEvent("SESSION_START", "AI Agent session started", null);
        
        logger.info("Project memory initialized for: {}", projectPath);
    }
    
    private String generateSessionId() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }
    
    /**
     * Load existing memory from persistent storage
     */
    private void loadMemoryFromFile() {
        if (!memoryFile.exists()) {
            logger.info("No existing project memory found. Starting fresh.");
            return;
        }
        
        try {
            JsonNode root = objectMapper.readTree(memoryFile);
            
            // Load project context
            JsonNode contextNode = root.get("projectContext");
            if (contextNode != null && contextNode.isObject()) {
                contextNode.fields().forEachRemaining(entry -> {
                    projectContext.put(entry.getKey(), entry.getValue().asText());
                });
            }
            
            // Load completed tasks
            JsonNode tasksNode = root.get("completedTasks");
            if (tasksNode != null && tasksNode.isArray()) {
                for (JsonNode taskNode : tasksNode) {
                    TaskRecord task = objectMapper.treeToValue(taskNode, TaskRecord.class);
                    completedTasks.add(task);
                }
            }
            
            // Load learnings
            JsonNode learningsNode = root.get("learnings");
            if (learningsNode != null && learningsNode.isArray()) {
                for (JsonNode learningNode : learningsNode) {
                    LearningRecord learning = objectMapper.treeToValue(learningNode, LearningRecord.class);
                    learnings.add(learning);
                }
            }
            
            // Load installed dependencies
            JsonNode depsNode = root.get("installedDependencies");
            if (depsNode != null && depsNode.isArray()) {
                for (JsonNode depNode : depsNode) {
                    installedDependencies.add(depNode.asText());
                }
            }
            
            // Load code patterns
            JsonNode patternsNode = root.get("codePatterns");
            if (patternsNode != null && patternsNode.isObject()) {
                patternsNode.fields().forEachRemaining(entry -> {
                    codePatterns.put(entry.getKey(), entry.getValue().asText());
                });
            }
            
            logger.info("Loaded project memory: {} tasks, {} learnings, {} dependencies", 
                       completedTasks.size(), learnings.size(), installedDependencies.size());
            
        } catch (IOException e) {
            logger.error("Error loading project memory: {}", e.getMessage());
        }
    }
    
    /**
     * Save current memory state to persistent storage
     */
    public synchronized void saveMemoryToFile() {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            
            // Save project context
            ObjectNode contextNode = objectMapper.createObjectNode();
            projectContext.forEach((key, value) -> contextNode.put(key, value.toString()));
            root.set("projectContext", contextNode);
            
            // Save completed tasks
            ArrayNode tasksNode = objectMapper.createArrayNode();
            for (TaskRecord task : completedTasks) {
                tasksNode.add(objectMapper.valueToTree(task));
            }
            root.set("completedTasks", tasksNode);
            
            // Save learnings
            ArrayNode learningsNode = objectMapper.createArrayNode();
            for (LearningRecord learning : learnings) {
                learningsNode.add(objectMapper.valueToTree(learning));
            }
            root.set("learnings", learningsNode);
            
            // Save installed dependencies
            ArrayNode depsNode = objectMapper.createArrayNode();
            installedDependencies.forEach(depsNode::add);
            root.set("installedDependencies", depsNode);
            
            // Save code patterns
            ObjectNode patternsNode = objectMapper.createObjectNode();
            codePatterns.forEach(patternsNode::put);
            root.set("codePatterns", patternsNode);
            
            // Add metadata
            root.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            root.put("version", "1.0");
            
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(memoryFile, root);
            logger.debug("Project memory saved successfully");
            
        } catch (IOException e) {
            logger.error("Error saving project memory: {}", e.getMessage());
        }
    }
    
    /**
     * Record a completed task
     */
    public void recordCompletedTask(String taskId, String description, String result, Map<String, Object> metadata) {
        TaskRecord task = new TaskRecord(
            taskId,
            description,
            result,
            LocalDateTime.now(),
            metadata != null ? metadata : new HashMap<>()
        );
        
        completedTasks.add(task);
        logSessionEvent("TASK_COMPLETED", description, Map.of("taskId", taskId, "result", result));
        
        logger.info("Recorded completed task: {}", taskId);
    }
    
    /**
     * Record a learning or insight
     */
    public void recordLearning(String category, String insight, String context) {
        LearningRecord learning = new LearningRecord(
            category,
            insight,
            context,
            LocalDateTime.now()
        );
        
        learnings.add(learning);
        logSessionEvent("LEARNING_RECORDED", insight, Map.of("category", category));
        
        logger.info("Recorded learning in category '{}': {}", category, insight);
    }
    
    /**
     * Record an installed dependency
     */
    public void recordInstalledDependency(String dependency, String language, String version) {
        String depKey = language + ":" + dependency + ":" + version;
        installedDependencies.add(depKey);
        logSessionEvent("DEPENDENCY_INSTALLED", dependency, 
                       Map.of("language", language, "version", version));
        
        logger.info("Recorded installed dependency: {}", depKey);
    }
    
    /**
     * Store a useful code pattern
     */
    public void recordCodePattern(String patternName, String code, String description) {
        codePatterns.put(patternName, code);
        recordLearning("code_pattern", description, code);
        
        logger.info("Recorded code pattern: {}", patternName);
    }
    
    /**
     * Get project context
     */
    public Map<String, Object> getProjectContext() {
        return new HashMap<>(projectContext);
    }
    
    /**
     * Update project context
     */
    public void updateProjectContext(String key, Object value) {
        projectContext.put(key, value);
        logSessionEvent("CONTEXT_UPDATED", "Updated context: " + key, Map.of("key", key, "value", value.toString()));
    }
    
    /**
     * Get tasks completed in current session
     */
    public List<TaskRecord> getRecentTasks(int limit) {
        return completedTasks.stream()
                .sorted((a, b) -> b.timestamp.compareTo(a.timestamp))
                .limit(limit)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Get learnings by category
     */
    public List<LearningRecord> getLearningsByCategory(String category) {
        return learnings.stream()
                .filter(learning -> learning.category.equals(category))
                .sorted((a, b) -> b.timestamp.compareTo(a.timestamp))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Check if dependency is already installed
     */
    public boolean isDependencyInstalled(String dependency, String language) {
        return installedDependencies.stream()
                .anyMatch(dep -> dep.startsWith(language + ":" + dependency + ":"));
    }
    
    /**
     * Get installed dependencies for a language
     */
    public Set<String> getInstalledDependencies(String language) {
        return installedDependencies.stream()
                .filter(dep -> dep.startsWith(language + ":"))
                .map(dep -> dep.substring(dep.indexOf(":") + 1))
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }
    
    /**
     * Get code pattern by name
     */
    public String getCodePattern(String patternName) {
        return codePatterns.get(patternName);
    }
    
    /**
     * Search for similar tasks
     */
    public List<TaskRecord> findSimilarTasks(String description, int limit) {
        String[] searchTerms = description.toLowerCase().split("\\s+");
        
        return completedTasks.stream()
                .filter(task -> {
                    String taskDesc = task.description.toLowerCase();
                    return Arrays.stream(searchTerms)
                            .anyMatch(taskDesc::contains);
                })
                .sorted((a, b) -> b.timestamp.compareTo(a.timestamp))
                .limit(limit)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Generate memory summary for AI agent
     */
    public String generateMemorySummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("=== PROJECT MEMORY SUMMARY ===\n");
        
        // Project context
        if (!projectContext.isEmpty()) {
            summary.append("\nProject Context:\n");
            projectContext.forEach((key, value) -> 
                summary.append(String.format("- %s: %s\n", key, value)));
        }
        
        // Recent tasks
        List<TaskRecord> recentTasks = getRecentTasks(5);
        if (!recentTasks.isEmpty()) {
            summary.append("\nRecent Completed Tasks:\n");
            recentTasks.forEach(task -> 
                summary.append(String.format("- [%s] %s: %s\n", 
                    task.timestamp.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")),
                    task.taskId, task.description)));
        }
        
        // Key learnings
        Map<String, Long> learningCounts = learnings.stream()
                .collect(Collectors.groupingBy(l -> l.category, Collectors.counting()));
        if (!learningCounts.isEmpty()) {
            summary.append("\nLearning Categories:\n");
            learningCounts.forEach((category, count) -> 
                summary.append(String.format("- %s: %d insights\n", category, count)));
        }
        
        // Dependencies
        Map<String, Long> depCounts = installedDependencies.stream()
                .map(dep -> dep.split(":")[0])
                .collect(Collectors.groupingBy(lang -> lang, Collectors.counting()));
        if (!depCounts.isEmpty()) {
            summary.append("\nInstalled Dependencies by Language:\n");
            depCounts.forEach((lang, count) -> 
                summary.append(String.format("- %s: %d packages\n", lang, count)));
        }
        
        return summary.toString();
    }
    
    /**
     * Log session event
     */
    private void logSessionEvent(String eventType, String description, Map<String, Object> data) {
        SessionEvent event = new SessionEvent(
            LocalDateTime.now(),
            eventType,
            description,
            data != null ? data : new HashMap<>()
        );
        
        currentSessionEvents.add(event);
        
        // Also log to file
        try {
            String logLine = String.format("[%s] %s: %s%s\n",
                event.timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                event.eventType,
                event.description,
                event.data.isEmpty() ? "" : " | " + event.data);
            
            java.nio.file.Files.write(sessionLogFile.toPath(), 
                logLine.getBytes(), 
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
                
        } catch (IOException e) {
            logger.warn("Could not write to session log: {}", e.getMessage());
        }
    }
    
    /**
     * Close session and save final state
     */
    public void closeSession() {
        logSessionEvent("SESSION_END", "AI Agent session ended", 
                       Map.of("totalEvents", currentSessionEvents.size()));
        saveMemoryToFile();
        logger.info("Session {} closed with {} events", sessionId, currentSessionEvents.size());
    }
    
    // Data classes for memory records
    public static class TaskRecord {
        public String taskId;
        public String description;
        public String result;
        public LocalDateTime timestamp;
        public Map<String, Object> metadata;
        
        public TaskRecord() {}
        
        public TaskRecord(String taskId, String description, String result, 
                         LocalDateTime timestamp, Map<String, Object> metadata) {
            this.taskId = taskId;
            this.description = description;
            this.result = result;
            this.timestamp = timestamp;
            this.metadata = metadata;
        }
    }
    
    public static class LearningRecord {
        public String category;
        public String insight;
        public String context;
        public LocalDateTime timestamp;
        
        public LearningRecord() {}
        
        public LearningRecord(String category, String insight, String context, LocalDateTime timestamp) {
            this.category = category;
            this.insight = insight;
            this.context = context;
            this.timestamp = timestamp;
        }
    }
    
    public static class SessionEvent {
        public LocalDateTime timestamp;
        public String eventType;
        public String description;
        public Map<String, Object> data;
        
        public SessionEvent() {}
        
        public SessionEvent(LocalDateTime timestamp, String eventType, String description, Map<String, Object> data) {
            this.timestamp = timestamp;
            this.eventType = eventType;
            this.description = description;
            this.data = data;
        }
    }
}
