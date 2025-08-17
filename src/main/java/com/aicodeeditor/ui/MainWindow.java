package com.aicodeeditor.ui;

import com.aicodeeditor.ai.AIAgent;
import com.aicodeeditor.core.ConfigurationManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.Optional;

/**
 * Main window of the AI Code Editor
 */
public class MainWindow {
    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);
    
    private final ConfigurationManager config;
    private final AIAgent aiAgent;
    private Stage primaryStage;
    
    // UI Components
    private CodeArea codeArea;
    private TextArea outputArea;
    private TextArea aiChatArea;
    private TextField userInputField;
    private ComboBox<String> languageSelector;
    private Button executeButton;
    private Button askAIButton;
    private ProgressIndicator progressIndicator;
    private Label statusLabel;
    
    // File management
    private File currentFile;
    
    public MainWindow() {
        this.config = ConfigurationManager.getInstance();
        this.aiAgent = new AIAgent();
    }
    
    /**
     * Show the main window
     */
    public void show(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        // Initialize AI Agent
        initializeAIAgent();
        
        // Create UI
        Scene scene = createMainScene();
        
        // Configure stage
        primaryStage.setTitle("AI Code Editor");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.setOnCloseRequest(e -> handleApplicationExit());
        
        // Apply theme
        applyTheme(scene);
        
        primaryStage.show();
        
        logger.info("Main window displayed");
    }
    
    /**
     * Create the main scene
     */
    private Scene createMainScene() {
        BorderPane root = new BorderPane();
        
        // Create menu bar
        MenuBar menuBar = createMenuBar();
        root.setTop(menuBar);
        
        // Create main content
        SplitPane mainSplit = new SplitPane();
        mainSplit.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        
        // Left side - Code editor
        VBox leftPanel = createCodeEditorPanel();
        
        // Right side - AI Chat and Output
        VBox rightPanel = createRightPanel();
        
        mainSplit.getItems().addAll(leftPanel, rightPanel);
        mainSplit.setDividerPositions(0.7);
        
        root.setCenter(mainSplit);
        
        // Create status bar
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);
        
        return new Scene(root, 1200, 800);
    }
    
    /**
     * Create menu bar
     */
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem newFile = new MenuItem("New");
        MenuItem openFile = new MenuItem("Open");
        MenuItem saveFile = new MenuItem("Save");
        MenuItem saveAsFile = new MenuItem("Save As");
        MenuItem exit = new MenuItem("Exit");
        
        newFile.setOnAction(e -> handleNewFile());
        openFile.setOnAction(e -> handleOpenFile());
        saveFile.setOnAction(e -> handleSaveFile());
        saveAsFile.setOnAction(e -> handleSaveAsFile());
        exit.setOnAction(e -> handleApplicationExit());
        
        fileMenu.getItems().addAll(newFile, openFile, new SeparatorMenuItem(), 
            saveFile, saveAsFile, new SeparatorMenuItem(), exit);
        
        // Edit menu
        Menu editMenu = new Menu("Edit");
        MenuItem undo = new MenuItem("Undo");
        MenuItem redo = new MenuItem("Redo");
        MenuItem cut = new MenuItem("Cut");
        MenuItem copy = new MenuItem("Copy");
        MenuItem paste = new MenuItem("Paste");
        
        undo.setOnAction(e -> codeArea.undo());
        redo.setOnAction(e -> codeArea.redo());
        cut.setOnAction(e -> codeArea.cut());
        copy.setOnAction(e -> codeArea.copy());
        paste.setOnAction(e -> codeArea.paste());
        
        editMenu.getItems().addAll(undo, redo, new SeparatorMenuItem(), cut, copy, paste);
        
        // Run menu
        Menu runMenu = new Menu("Run");
        MenuItem executeCode = new MenuItem("Execute Code");
        MenuItem installDependencies = new MenuItem("Install Dependencies");
        
        executeCode.setOnAction(e -> handleExecuteCode());
        installDependencies.setOnAction(e -> handleInstallDependencies());
        
        runMenu.getItems().addAll(executeCode, installDependencies);
        
        // AI menu
        Menu aiMenu = new Menu("AI");
        MenuItem developSoftware = new MenuItem("Develop Software");
        MenuItem improveCode = new MenuItem("Improve Code");
        MenuItem explainCode = new MenuItem("Explain Code");
        
        developSoftware.setOnAction(e -> handleDevelopSoftware());
        improveCode.setOnAction(e -> handleImproveCode());
        explainCode.setOnAction(e -> handleExplainCode());
        
        aiMenu.getItems().addAll(developSoftware, improveCode, explainCode);
        
        menuBar.getMenus().addAll(fileMenu, editMenu, runMenu, aiMenu);
        
        return menuBar;
    }
    
    /**
     * Create code editor panel
     */
    private VBox createCodeEditorPanel() {
        VBox panel = new VBox(5);
        panel.setPadding(new Insets(10));
        
        // Language selector and execute button
        HBox controls = new HBox(10);
        languageSelector = new ComboBox<>();
        languageSelector.getItems().addAll("Python", "Java", "JavaScript", "TypeScript", 
            "Go", "Rust", "C++", "C", "C#", "PHP", "Ruby");
        languageSelector.setValue("Python");
        
        executeButton = new Button("Execute");
        executeButton.setOnAction(e -> handleExecuteCode());
        
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(20, 20);
        
        controls.getChildren().addAll(new Label("Language:"), languageSelector, 
            executeButton, progressIndicator);
        
        // Code area with syntax highlighting
        codeArea = new CodeArea();
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 14px;");
        codeArea.getStylesheets().add(getClass().getResource("/styles/syntax-highlighting.css").toExternalForm());
        
        // Output area
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px;");
        outputArea.setPrefRowCount(10);
        
        SplitPane editorSplit = new SplitPane();
        editorSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        editorSplit.getItems().addAll(codeArea, new TitledPane("Output", outputArea));
        editorSplit.setDividerPositions(0.7);
        
        panel.getChildren().addAll(controls, editorSplit);
        VBox.setVgrow(editorSplit, Priority.ALWAYS);
        
        return panel;
    }
    
    /**
     * Create right panel with AI chat
     */
    private VBox createRightPanel() {
        VBox panel = new VBox(5);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(400);
        
        // AI Chat area
        aiChatArea = new TextArea();
        aiChatArea.setEditable(false);
        aiChatArea.setStyle("-fx-font-family: 'Arial'; -fx-font-size: 12px;");
        aiChatArea.setWrapText(true);
        
        // User input
        HBox inputBox = new HBox(5);
        userInputField = new TextField();
        userInputField.setPromptText("Ask AI to develop software or help with code...");
        userInputField.setOnAction(e -> handleAskAI());
        
        askAIButton = new Button("Ask AI");
        askAIButton.setOnAction(e -> handleAskAI());
        
        inputBox.getChildren().addAll(userInputField, askAIButton);
        HBox.setHgrow(userInputField, Priority.ALWAYS);
        
        TitledPane aiPane = new TitledPane("AI Assistant", new VBox(5, aiChatArea, inputBox));
        aiPane.setCollapsible(false);
        
        panel.getChildren().add(aiPane);
        VBox.setVgrow(aiPane, Priority.ALWAYS);
        
        return panel;
    }
    
    /**
     * Create status bar
     */
    private HBox createStatusBar() {
        HBox statusBar = new HBox();
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #d0d0d0; -fx-border-width: 1 0 0 0;");
        
        statusLabel = new Label("Ready");
        statusBar.getChildren().add(statusLabel);
        
        return statusBar;
    }
    
    /**
     * Initialize AI Agent
     */
    private void initializeAIAgent() {
        Task<Boolean> initTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return aiAgent.initialize().get();
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    if (getValue()) {
                        updateStatus("AI Agent initialized successfully");
                        addAIChatMessage("AI Assistant", "Hello! I'm ready to help you develop software. What would you like to create today?");
                    } else {
                        updateStatus("AI Agent initialization failed");
                        addAIChatMessage("System", "AI Agent failed to initialize. Please check that Ollama is running.");
                    }
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    updateStatus("AI Agent initialization failed");
                    addAIChatMessage("System", "AI Agent initialization failed: " + getException().getMessage());
                });
            }
        };
        
        new Thread(initTask).start();
    }
    
    /**
     * Handle file operations
     */
    private void handleNewFile() {
        codeArea.clear();
        currentFile = null;
        updateStatus("New file created");
    }
    
    private void handleOpenFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        File file = fileChooser.showOpenDialog(primaryStage);
        
        if (file != null) {
            try {
                String content = Files.readString(file.toPath());
                codeArea.replaceText(content);
                currentFile = file;
                updateStatus("File opened: " + file.getName());
            } catch (Exception e) {
                showError("Failed to open file", e.getMessage());
            }
        }
    }
    
    private void handleSaveFile() {
        if (currentFile != null) {
            saveToFile(currentFile);
        } else {
            handleSaveAsFile();
        }
    }
    
    private void handleSaveAsFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File");
        File file = fileChooser.showSaveDialog(primaryStage);
        
        if (file != null) {
            saveToFile(file);
            currentFile = file;
        }
    }
    
    private void saveToFile(File file) {
        try {
            Files.writeString(file.toPath(), codeArea.getText());
            updateStatus("File saved: " + file.getName());
        } catch (Exception e) {
            showError("Failed to save file", e.getMessage());
        }
    }
    
    /**
     * Handle code execution
     */
    private void handleExecuteCode() {
        String code = codeArea.getText();
        String language = languageSelector.getValue();
        
        if (code.trim().isEmpty()) {
            showWarning("No code to execute");
            return;
        }
        
        executeButton.setDisable(true);
        progressIndicator.setVisible(true);
        updateStatus("Executing code...");
        
        Task<String> executeTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return aiAgent.executeCode(code, language).get();
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    outputArea.setText(getValue());
                    executeButton.setDisable(false);
                    progressIndicator.setVisible(false);
                    updateStatus("Code execution completed");
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    outputArea.setText("Execution failed: " + getException().getMessage());
                    executeButton.setDisable(false);
                    progressIndicator.setVisible(false);
                    updateStatus("Code execution failed");
                });
            }
        };
        
        new Thread(executeTask).start();
    }
    
    /**
     * Handle AI interactions
     */
    private void handleAskAI() {
        String userInput = userInputField.getText().trim();
        if (userInput.isEmpty()) {
            return;
        }
        
        userInputField.clear();
        addAIChatMessage("You", userInput);
        
        askAIButton.setDisable(true);
        updateStatus("AI is processing your request...");
        
        Task<String> aiTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return aiAgent.developSoftware(userInput, languageSelector.getValue()).get().toString();
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    addAIChatMessage("AI Assistant", getValue());
                    askAIButton.setDisable(false);
                    updateStatus("AI response received");
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    addAIChatMessage("AI Assistant", "Sorry, I encountered an error: " + getException().getMessage());
                    askAIButton.setDisable(false);
                    updateStatus("AI request failed");
                });
            }
        };
        
        new Thread(aiTask).start();
    }
    
    private void handleDevelopSoftware() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Develop Software");
        dialog.setHeaderText("What software would you like me to develop?");
        dialog.setContentText("Describe your requirements:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(requirement -> {
            userInputField.setText(requirement);
            handleAskAI();
        });
    }
    
    private void handleImproveCode() {
        String code = codeArea.getSelectedText();
        if (code.isEmpty()) {
            code = codeArea.getText();
        }
        
        if (code.trim().isEmpty()) {
            showWarning("No code selected to improve");
            return;
        }
        
        userInputField.setText("Please improve this code and explain the improvements:\n" + code);
        handleAskAI();
    }
    
    private void handleExplainCode() {
        String code = codeArea.getSelectedText();
        if (code.isEmpty()) {
            code = codeArea.getText();
        }
        
        if (code.trim().isEmpty()) {
            showWarning("No code selected to explain");
            return;
        }
        
        userInputField.setText("Please explain this code:\n" + code);
        handleAskAI();
    }
    
    private void handleInstallDependencies() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Install Dependencies");
        dialog.setHeaderText("Enter dependencies to install (space-separated):");
        dialog.setContentText("Dependencies:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(deps -> {
            String[] dependencies = deps.trim().split("\\s+");
            String language = languageSelector.getValue();
            
            Task<String> installTask = new Task<String>() {
                @Override
                protected String call() throws Exception {
                    return aiAgent.installDependencies(language, dependencies).get();
                }
                
                @Override
                protected void succeeded() {
                    Platform.runLater(() -> {
                        addAIChatMessage("System", "Dependency installation result:\n" + getValue());
                        updateStatus("Dependencies installed");
                    });
                }
                
                @Override
                protected void failed() {
                    Platform.runLater(() -> {
                        addAIChatMessage("System", "Dependency installation failed: " + getException().getMessage());
                        updateStatus("Dependency installation failed");
                    });
                }
            };
            
            new Thread(installTask).start();
        });
    }
    
    /**
     * Helper methods
     */
    private void addAIChatMessage(String sender, String message) {
        Platform.runLater(() -> {
            aiChatArea.appendText(String.format("[%s]: %s\n\n", sender, message));
        });
    }
    
    private void updateStatus(String message) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            logger.info("Status: {}", message);
        });
    }
    
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    private void showWarning(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    private void applyTheme(Scene scene) {
        String theme = config.getString("editor.theme", "dark");
        if ("dark".equals(theme)) {
            scene.getStylesheets().add(getClass().getResource("/styles/dark-theme.css").toExternalForm());
        }
    }
    
    private void handleApplicationExit() {
        logger.info("Application exiting...");
        aiAgent.shutdown();
        Platform.exit();
        System.exit(0);
    }
}
