package com.aicodeeditor.sandbox;

import com.aicodeeditor.core.ConfigurationManager;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages Docker-based sandbox environments for safe code execution
 */
public class SandboxManager {
    private static final Logger logger = LoggerFactory.getLogger(SandboxManager.class);
    
    private final ConfigurationManager config;
    private DockerClient dockerClient;
    private final Path sandboxDir;
    private final Map<String, String> languageImages;
    
    public SandboxManager() {
        this.config = ConfigurationManager.getInstance();
        // Create a default sandbox directory in temp if not configured
        String sandboxPath = config.getString("sandbox.directory", System.getProperty("java.io.tmpdir") + "/ai-code-editor/sandbox");
        this.sandboxDir = Paths.get(sandboxPath);
        this.languageImages = new HashMap<>();
        initializeLanguageImages();
        
        try {
            // Create sandbox directory if it doesn't exist
            Files.createDirectories(sandboxDir);
            
            DefaultDockerClientConfig dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock")
                .build();
                
            ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(dockerConfig.getDockerHost())
                .build();
                
            this.dockerClient = DockerClientImpl.getInstance(dockerConfig, httpClient);
        } catch (Exception e) {
            logger.error("Failed to initialize Docker client", e);
            throw new RuntimeException("Docker initialization failed", e);
        }
    }
    
    /**
     * Execute code in a sandbox environment
     */
    public CompletableFuture<String> executeCode(String code, String language) {
        return CompletableFuture.supplyAsync(() -> {
            String containerId = null;
            try {
                logger.info("Executing {} code in sandbox", language);
                
                // Get appropriate Docker image for language
                String image = languageImages.get(language.toLowerCase());
                if (image == null) {
                    throw new IllegalArgumentException("Unsupported language: " + language);
                }
                
                // Create temporary directory for this execution
                Path executionDir = Files.createTempDirectory(sandboxDir, "exec_");
                
                // Write code to file
                Path codeFile = writeCodeToFile(code, language, executionDir);
                
                // Create and start container
                containerId = createContainer(image, executionDir);
                dockerClient.startContainerCmd(containerId).exec();
                
                // Execute code
                String result = executeCodeInContainer(containerId, codeFile, language);
                
                // Cleanup
                cleanupExecution(containerId, executionDir);
                
                return result;
                
            } catch (Exception e) {
                logger.error("Code execution failed", e);
                if (containerId != null) {
                    try {
                        dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                    } catch (Exception cleanup) {
                        logger.warn("Failed to cleanup container", cleanup);
                    }
                }
                return "Execution failed: " + e.getMessage();
            }
        });
    }
    
    /**
     * Create sandbox directory
     */
    private Path createSandboxDirectory() {
        try {
            String userHome = System.getProperty("user.home");
            Path dir = Paths.get(userHome, ".ai-code-editor", "sandbox");
            Files.createDirectories(dir);
            return dir;
        } catch (IOException e) {
            logger.error("Failed to create sandbox directory", e);
            throw new RuntimeException("Sandbox directory creation failed", e);
        }
    }
    
    /**
     * Initialize language-specific Docker images
     */
    private Map<String, String> initializeLanguageImages() {
        Map<String, String> images = new HashMap<>();
        images.put("python", "python:3.11-slim");
        images.put("java", "openjdk:11-jdk-slim");
        images.put("javascript", "node:18-alpine");
        images.put("typescript", "node:18-alpine");
        images.put("go", "golang:1.21-alpine");
        images.put("rust", "rust:1.75-slim");
        images.put("cpp", "gcc:latest");
        images.put("c", "gcc:latest");
        images.put("csharp", "mcr.microsoft.com/dotnet/sdk:8.0");
        images.put("php", "php:8.2-cli");
        images.put("ruby", "ruby:3.2-slim");
        return images;
    }
    
    /**
     * Pull required Docker images
     */
    private void pullRequiredImages() {
        logger.info("Pulling required Docker images...");
        
        for (Map.Entry<String, String> entry : languageImages.entrySet()) {
            try {
                String image = entry.getValue();
                logger.info("Pulling image: {}", image);
                
                dockerClient.pullImageCmd(image)
                    .start()
                    .awaitCompletion(300, TimeUnit.SECONDS);
                    
                logger.info("Successfully pulled: {}", image);
            } catch (Exception e) {
                logger.warn("Failed to pull image for {}: {}", entry.getKey(), e.getMessage());
            }
        }
    }
    
    /**
     * Write code to appropriate file based on language
     */
    private Path writeCodeToFile(String code, String language, Path executionDir) throws IOException {
        String filename;
        switch (language.toLowerCase()) {
            case "python":
                filename = "main.py";
                break;
            case "java":
                filename = "Main.java";
                break;
            case "javascript":
                filename = "main.js";
                break;
            case "typescript":
                filename = "main.ts";
                break;
            case "go":
                filename = "main.go";
                break;
            case "rust":
                filename = "main.rs";
                break;
            case "cpp":
                filename = "main.cpp";
                break;
            case "c":
                filename = "main.c";
                break;
            case "csharp":
                filename = "Program.cs";
                break;
            case "php":
                filename = "main.php";
                break;
            case "ruby":
                filename = "main.rb";
                break;
            default:
                filename = "main.txt";
        }
        
        Path codeFile = executionDir.resolve(filename);
        Files.write(codeFile, code.getBytes());
        return codeFile;
    }
    
    /**
     * Create Docker container for code execution
     */
    private String createContainer(String image, Path executionDir) {
        CreateContainerResponse container = dockerClient.createContainerCmd(image)
            .withWorkingDir("/app")
            .withBinds(new Bind(executionDir.toString(), new Volume("/app")))
            .withNetworkMode("none") // No network access for security
            .withMemory(512 * 1024 * 1024L) // 512MB memory limit
            .withCmd("sleep", "3600") // Keep container alive
            .exec();
            
        return container.getId();
    }
    
    /**
     * Execute code inside container
     */
    private String executeCodeInContainer(String containerId, Path codeFile, String language) throws Exception {
        String[] command = getExecutionCommand(language, codeFile.getFileName().toString());
        
        ExecCreateCmdResponse exec = dockerClient.execCreateCmd(containerId)
            .withAttachStdout(true)
            .withAttachStderr(true)
            .withCmd(command)
            .exec();
        
        // Capture output
        ExecutionResultCallback callback = new ExecutionResultCallback();
        dockerClient.execStartCmd(exec.getId()).exec(callback);
        
        // Wait for completion with timeout
        int timeout = config.getInt("sandbox.timeout", 300000);
        callback.awaitCompletion(timeout, TimeUnit.MILLISECONDS);
        
        return callback.getOutput();
    }
    
    /**
     * Get execution command for specific language
     */
    private String[] getExecutionCommand(String language, String filename) {
        switch (language.toLowerCase()) {
            case "python":
                return new String[]{"python", filename};
            case "java":
                return new String[]{"sh", "-c", "javac " + filename + " && java Main"};
            case "javascript":
                return new String[]{"node", filename};
            case "typescript":
                return new String[]{"sh", "-c", "npx tsc " + filename + " && node main.js"};
            case "go":
                return new String[]{"go", "run", filename};
            case "rust":
                return new String[]{"sh", "-c", "rustc " + filename + " && ./main"};
            case "cpp":
                return new String[]{"sh", "-c", "g++ -o main " + filename + " && ./main"};
            case "c":
                return new String[]{"sh", "-c", "gcc -o main " + filename + " && ./main"};
            case "csharp":
                return new String[]{"dotnet", "run"};
            case "php":
                return new String[]{"php", filename};
            case "ruby":
                return new String[]{"ruby", filename};
            default:
                return new String[]{"cat", filename};
        }
    }
    
    /**
     * Cleanup execution environment
     */
    private void cleanupExecution(String containerId, Path executionDir) {
        try {
            // Remove container
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
            
            // Remove temporary directory
            FileUtils.deleteDirectory(executionDir.toFile());
            
        } catch (Exception e) {
            logger.warn("Cleanup failed", e);
        }
    }
    
    /**
     * Check if Docker is available
     */
    public boolean isDockerAvailable() {
        try {
            if (dockerClient == null) return false;
            dockerClient.pingCmd().exec();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get list of running containers
     */
    public List<Container> getRunningContainers() {
        try {
            return dockerClient.listContainersCmd().exec();
        } catch (Exception e) {
            logger.error("Failed to list containers", e);
            return List.of();
        }
    }
    
    /**
     * Close the sandbox manager
     */
    public void close() {
        try {
            if (dockerClient != null) {
                // Cleanup any remaining containers
                List<Container> containers = dockerClient.listContainersCmd()
                    .withLabelFilter(Map.of("ai-code-editor", "sandbox"))
                    .exec();
                    
                for (Container container : containers) {
                    dockerClient.removeContainerCmd(container.getId()).withForce(true).exec();
                }
                
                dockerClient.close();
            }
            
            logger.info("Sandbox manager closed");
        } catch (Exception e) {
            logger.error("Error closing sandbox manager", e);
        }
    }
}
