package com.aicodeeditor;

import com.aicodeeditor.ui.MainWindow;
import com.aicodeeditor.core.PrivilegeManager;
import com.aicodeeditor.core.ConfigurationManager;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application class for AI Code Editor
 * Handles application initialization and privilege checks
 */
public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) {
        // Check for admin privileges
        if (!PrivilegeManager.hasAdminPrivileges()) {
            logger.error("This application requires administrator privileges to run.");
            System.err.println("Please run this application as an administrator.");
            System.exit(1);
        }
        
        logger.info("Starting AI Code Editor...");
        launch(args);
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialize configuration
        ConfigurationManager.getInstance().initialize();
        
        // Create and show main window
        MainWindow mainWindow = new MainWindow();
        mainWindow.show(primaryStage);
        
        logger.info("AI Code Editor started successfully");
    }
    
    @Override
    public void stop() throws Exception {
        logger.info("Shutting down AI Code Editor...");
        // Cleanup resources
        ConfigurationManager.getInstance().shutdown();
        super.stop();
    }
}
