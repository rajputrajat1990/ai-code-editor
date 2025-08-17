package com.aicodeeditor.dependencies;

import com.aicodeeditor.core.ConfigurationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages automatic installation of dependencies for different programming languages
 */
public class DependencyManager {
    private static final Logger logger = LoggerFactory.getLogger(DependencyManager.class);
    
    private final ConfigurationManager config;
    private final Map<String, DependencyInstaller> installers;
    
    public DependencyManager() {
        this.config = ConfigurationManager.getInstance();
        this.installers = new HashMap<>();
        initializeInstallers();
    }
    
    /**
     * Initialize dependency installers for different languages
     */
    private void initializeInstallers() {
        installers.put("python", new PythonDependencyInstaller());
        installers.put("javascript", new NodeJSDependencyInstaller());
        installers.put("typescript", new NodeJSDependencyInstaller());
        installers.put("java", new JavaDependencyInstaller());
        installers.put("go", new GoDependencyInstaller());
        installers.put("rust", new RustDependencyInstaller());
        installers.put("php", new PHPDependencyInstaller());
        installers.put("ruby", new RubyDependencyInstaller());
    }
    
    /**
     * Initialize the dependency manager
     */
    public void initialize() {
        logger.info("Dependency manager initialized");
    }
    
    /**
     * Install dependencies for a specific language
     */
    public CompletableFuture<String> installDependencies(String language, String[] dependencies) {
        if (!config.getBoolean("dependencies.autoInstall", true)) {
            return CompletableFuture.completedFuture("Automatic dependency installation is disabled");
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                DependencyInstaller installer = installers.get(language.toLowerCase());
                if (installer == null) {
                    return "No dependency installer available for language: " + language;
                }
                
                StringBuilder result = new StringBuilder();
                result.append("Installing dependencies for ").append(language).append(":\n");
                
                for (String dependency : dependencies) {
                    logger.info("Installing {} dependency: {}", language, dependency);
                    String installResult = installer.install(dependency);
                    result.append("- ").append(dependency).append(": ").append(installResult).append("\n");
                }
                
                return result.toString();
                
            } catch (Exception e) {
                logger.error("Dependency installation failed", e);
                return "Dependency installation failed: " + e.getMessage();
            }
        });
    }
    
    /**
     * Check if a dependency is already installed
     */
    public CompletableFuture<Boolean> isDependencyInstalled(String language, String dependency) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DependencyInstaller installer = installers.get(language.toLowerCase());
                if (installer == null) {
                    return false;
                }
                
                return installer.isInstalled(dependency);
                
            } catch (Exception e) {
                logger.debug("Dependency check failed for {}: {}", dependency, e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Close the dependency manager
     */
    public void close() {
        logger.info("Dependency manager closed");
    }
    
    /**
     * Base interface for dependency installers
     */
    public interface DependencyInstaller {
        String install(String dependency);
        boolean isInstalled(String dependency);
    }
    
    /**
     * Python dependency installer using pip
     */
    public static class PythonDependencyInstaller implements DependencyInstaller {
        @Override
        public String install(String dependency) {
            try {
                return executeCommand("pip", "install", dependency);
            } catch (Exception e) {
                return "Failed: " + e.getMessage();
            }
        }
        
        @Override
        public boolean isInstalled(String dependency) {
            try {
                Process process = new ProcessBuilder("pip", "show", dependency).start();
                return process.waitFor() == 0;
            } catch (Exception e) {
                return false;
            }
        }
    }
    
    /**
     * Node.js dependency installer using npm
     */
    public static class NodeJSDependencyInstaller implements DependencyInstaller {
        @Override
        public String install(String dependency) {
            try {
                return executeCommand("npm", "install", "-g", dependency);
            } catch (Exception e) {
                return "Failed: " + e.getMessage();
            }
        }
        
        @Override
        public boolean isInstalled(String dependency) {
            try {
                Process process = new ProcessBuilder("npm", "list", "-g", dependency).start();
                return process.waitFor() == 0;
            } catch (Exception e) {
                return false;
            }
        }
    }
    
    /**
     * Java dependency installer (basic Maven/Gradle check)
     */
    public static class JavaDependencyInstaller implements DependencyInstaller {
        @Override
        public String install(String dependency) {
            // For Java, we mainly check if Maven/Gradle is available
            // Actual dependency management would be through build files
            if (isMavenAvailable()) {
                return "Maven is available for dependency management";
            } else if (isGradleAvailable()) {
                return "Gradle is available for dependency management";
            } else {
                return "Please install Maven or Gradle for Java dependency management";
            }
        }
        
        @Override
        public boolean isInstalled(String dependency) {
            return isMavenAvailable() || isGradleAvailable();
        }
        
        private boolean isMavenAvailable() {
            try {
                Process process = new ProcessBuilder("mvn", "--version").start();
                return process.waitFor() == 0;
            } catch (Exception e) {
                return false;
            }
        }
        
        private boolean isGradleAvailable() {
            try {
                Process process = new ProcessBuilder("gradle", "--version").start();
                return process.waitFor() == 0;
            } catch (Exception e) {
                return false;
            }
        }
    }
    
    /**
     * Go dependency installer
     */
    public static class GoDependencyInstaller implements DependencyInstaller {
        @Override
        public String install(String dependency) {
            try {
                return executeCommand("go", "get", dependency);
            } catch (Exception e) {
                return "Failed: " + e.getMessage();
            }
        }
        
        @Override
        public boolean isInstalled(String dependency) {
            try {
                Process process = new ProcessBuilder("go", "list", dependency).start();
                return process.waitFor() == 0;
            } catch (Exception e) {
                return false;
            }
        }
    }
    
    /**
     * Rust dependency installer using cargo
     */
    public static class RustDependencyInstaller implements DependencyInstaller {
        @Override
        public String install(String dependency) {
            try {
                return executeCommand("cargo", "install", dependency);
            } catch (Exception e) {
                return "Failed: " + e.getMessage();
            }
        }
        
        @Override
        public boolean isInstalled(String dependency) {
            try {
                Process process = new ProcessBuilder("cargo", "search", dependency).start();
                return process.waitFor() == 0;
            } catch (Exception e) {
                return false;
            }
        }
    }
    
    /**
     * PHP dependency installer using composer
     */
    public static class PHPDependencyInstaller implements DependencyInstaller {
        @Override
        public String install(String dependency) {
            try {
                return executeCommand("composer", "global", "require", dependency);
            } catch (Exception e) {
                return "Failed: " + e.getMessage();
            }
        }
        
        @Override
        public boolean isInstalled(String dependency) {
            try {
                Process process = new ProcessBuilder("composer", "global", "show", dependency).start();
                return process.waitFor() == 0;
            } catch (Exception e) {
                return false;
            }
        }
    }
    
    /**
     * Ruby dependency installer using gem
     */
    public static class RubyDependencyInstaller implements DependencyInstaller {
        @Override
        public String install(String dependency) {
            try {
                return executeCommand("gem", "install", dependency);
            } catch (Exception e) {
                return "Failed: " + e.getMessage();
            }
        }
        
        @Override
        public boolean isInstalled(String dependency) {
            try {
                Process process = new ProcessBuilder("gem", "list", dependency).start();
                return process.waitFor() == 0;
            } catch (Exception e) {
                return false;
            }
        }
    }
    
    /**
     * Execute a command and return the output
     */
    private static String executeCommand(String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        boolean completed = process.waitFor(300, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new InterruptedException("Command timed out");
        }
        
        return output.toString();
    }
}
