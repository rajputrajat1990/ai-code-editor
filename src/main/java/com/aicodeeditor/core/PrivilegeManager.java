package com.aicodeeditor.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages privilege checks across different operating systems
 */
public class PrivilegeManager {
    private static final Logger logger = LoggerFactory.getLogger(PrivilegeManager.class);
    
    /**
     * Checks if the application is running with administrator privileges
     * @return true if running with admin privileges, false otherwise
     */
    public static boolean hasAdminPrivileges() {
        String osName = System.getProperty("os.name").toLowerCase();
        
        if (osName.contains("win")) {
            return hasWindowsAdminPrivileges();
        } else if (osName.contains("mac") || osName.contains("nix") || osName.contains("nux")) {
            return hasUnixAdminPrivileges();
        }
        
        logger.warn("Unknown operating system: {}", osName);
        return false;
    }
    
    /**
     * Checks for Windows administrator privileges
     */
    private static boolean hasWindowsAdminPrivileges() {
        try {
            // Try to create a file in Windows system directory
            String systemRoot = System.getenv("SystemRoot");
            if (systemRoot == null) {
                systemRoot = "C:\\Windows";
            }
            
            File testFile = new File(systemRoot + "\\temp_admin_test.txt");
            boolean canWrite = testFile.createNewFile();
            
            if (canWrite) {
                testFile.delete();
                return true;
            }
            
            // Alternative method: check if we can run elevated commands
            ProcessBuilder pb = new ProcessBuilder("net", "session");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            return exitCode == 0;
            
        } catch (Exception e) {
            logger.debug("Windows admin privilege check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks for Unix/Linux administrator privileges (root or sudo)
     */
    private static boolean hasUnixAdminPrivileges() {
        try {
            // Check if running as root
            String user = System.getProperty("user.name");
            if ("root".equals(user)) {
                return true;
            }
            
            // Check if user has sudo privileges
            ProcessBuilder pb = new ProcessBuilder("sudo", "-n", "true");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                return true;
            }
            
            // Try to write to a system directory
            Path testPath = Paths.get("/tmp/admin_test_" + System.currentTimeMillis());
            try {
                Files.createFile(testPath);
                Files.delete(testPath);
                
                // Check if we can access /etc (typical admin-only directory)
                File etcDir = new File("/etc");
                return etcDir.canWrite();
                
            } catch (IOException e) {
                return false;
            }
            
        } catch (Exception e) {
            logger.debug("Unix admin privilege check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Attempts to restart the application with administrator privileges
     */
    public static void restartWithPrivileges() {
        String osName = System.getProperty("os.name").toLowerCase();
        String javaPath = System.getProperty("java.home") + "/bin/java";
        String jarPath = getJarPath();
        
        try {
            ProcessBuilder pb;
            
            if (osName.contains("win")) {
                // Windows: Use PowerShell to restart with elevated privileges
                pb = new ProcessBuilder("powershell", "Start-Process", 
                    "\"" + javaPath + "\"", 
                    "-ArgumentList", "\"-jar\",\"" + jarPath + "\"", 
                    "-Verb", "RunAs");
            } else {
                // Unix/Linux: Use sudo to restart
                pb = new ProcessBuilder("sudo", javaPath, "-jar", jarPath);
            }
            
            pb.start();
            System.exit(0);
            
        } catch (Exception e) {
            logger.error("Failed to restart with elevated privileges", e);
        }
    }
    
    private static String getJarPath() {
        try {
            return PrivilegeManager.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI().getPath();
        } catch (Exception e) {
            logger.error("Could not determine JAR path", e);
            return "";
        }
    }
}
