# AI Code Editor

A lightweight, cross-platform code editor with AI agent capabilities that can develop software autonomously.

## Features

- **Cross-Platform**: Runs on Windows, macOS, and Linux (Ubuntu/RHEL)
- **AI Agent**: Powered by locally hosted Ollama AI models (7B parameters)
- **Sandbox Environment**: Isolated code execution using Docker containers
- **Web Querying**: Fact-checking and research capabilities via web search
- **Automatic Dependencies**: Installs required dependencies automatically
- **Admin Privileges**: Requires elevated privileges for system operations
- **Multi-Language Support**: Python, Java, JavaScript, TypeScript, Go, Rust, C/C++, C#, PHP, Ruby

## Prerequisites

1. **Java 11 or higher**
2. **Docker** - For sandbox environment
3. **Ollama** - For AI model hosting
   ```bash
   # Install Ollama
   curl -fsSL https://ollama.ai/install.sh | sh
   
   # Pull a 7B model (e.g., Llama 3.2)
   ollama pull llama3.2:7b
   ```
4. **Maven** - For building the project
5. **Admin/Root privileges** - Required for running the application

## Installation

### 1. Clone and Build

```bash
git clone <repository-url>
cd ai-code-editor
mvn clean package
```

### 2. Run with Admin Privileges

#### On Linux/macOS:
```bash
sudo java -jar target/ai-code-editor-1.0.0.jar
```

#### On Windows (PowerShell as Administrator):
```powershell
java -jar target/ai-code-editor-1.0.0.jar
```

## Quick Start

1. **Start Ollama**: Ensure Ollama is running with your preferred model
2. **Launch Editor**: Run with admin privileges
3. **Ask AI**: Use the AI Assistant panel to describe what you want to develop
4. **Automatic Development**: The AI will:
   - Research requirements
   - Set up the development environment
   - Install dependencies
   - Generate code
   - Test in sandbox
   - Provide results

## Example Usage

### Develop a Web Scraper
1. Type in AI chat: "Create a Python web scraper that extracts article titles from a news website"
2. The AI will:
   - Research web scraping best practices
   - Install required packages (requests, beautifulsoup4)
   - Generate complete code
   - Test it in a sandboxed environment
   - Show results

### Build a REST API
1. Request: "Build a Node.js REST API for a todo application with CRUD operations"
2. The AI will:
   - Set up Node.js environment
   - Install Express.js and dependencies
   - Generate API code with proper structure
   - Test the endpoints
   - Provide documentation

## Configuration

The editor creates a configuration file at:
- **Windows**: `%APPDATA%/AICodeEditor/config.json`
- **macOS**: `~/Library/Application Support/AICodeEditor/config.json`
- **Linux**: `~/.ai-code-editor/config.json`

### Key Configuration Options:

```json
{
  "ollama.host": "http://localhost:11434",
  "ollama.model": "llama3.2:7b",
  "sandbox.enabled": true,
  "web.enabled": true,
  "dependencies.autoInstall": true
}
```

## Architecture

### Core Components

1. **Main Application** (`Main.java`)
   - Entry point with privilege checks
   - JavaFX application lifecycle

2. **AI Agent** (`AIAgent.java`)
   - Coordinates all AI operations
   - Handles development workflow

3. **Ollama Client** (`OllamaClient.java`)
   - Communicates with local AI model
   - Handles text generation

4. **Web Query Service** (`WebQueryService.java`)
   - Searches programming resources
   - Fact-checks information

5. **Sandbox Manager** (`SandboxManager.java`)
   - Docker-based code execution
   - Isolated environment management

6. **Dependency Manager** (`DependencyManager.java`)
   - Automatic package installation
   - Multi-language support

7. **UI Components** (`ui/`)
   - JavaFX-based interface
   - Code editor with syntax highlighting

### Security Features

- **Sandboxed Execution**: All code runs in isolated Docker containers
- **Network Isolation**: Containers have no network access by default
- **Resource Limits**: Memory and CPU constraints on execution
- **Admin Privileges**: Required for system-level operations
- **Local AI**: No code sent to external services

## Supported Languages & Dependencies

| Language   | Package Manager | Auto-Install Support |
|------------|----------------|---------------------|
| Python     | pip            | ✅                  |
| JavaScript | npm            | ✅                  |
| TypeScript | npm            | ✅                  |
| Java       | Maven/Gradle   | ✅                  |
| Go         | go mod         | ✅                  |
| Rust       | cargo          | ✅                  |
| PHP        | composer       | ✅                  |
| Ruby       | gem            | ✅                  |
| C/C++      | system         | ⚠️ Manual          |
| C#         | dotnet         | ⚠️ Manual          |

## Troubleshooting

### Ollama Not Available
- Ensure Ollama is installed and running
- Check that the model is pulled: `ollama list`
- Verify the host URL in configuration

### Docker Issues
- Ensure Docker is installed and running
- Check Docker permissions for your user
- Verify Docker images are available

### Privilege Issues
- Run as administrator/root
- Check file permissions in installation directory
- Verify system PATH includes Java

### Dependency Installation Fails
- Check internet connection
- Verify package managers are installed
- Review error messages in AI chat

## Development

### Project Structure
```
src/main/java/com/aicodeeditor/
├── Main.java                    # Application entry point
├── core/                        # Core utilities
│   ├── ConfigurationManager.java
│   └── PrivilegeManager.java
├── ai/                          # AI components
│   ├── AIAgent.java
│   ├── OllamaClient.java
│   └── WebQueryService.java
├── sandbox/                     # Sandboxing
│   ├── SandboxManager.java
│   └── ExecutionResultCallback.java
├── dependencies/                # Dependency management
│   └── DependencyManager.java
└── ui/                          # User interface
    └── MainWindow.java
```

### Building from Source

1. **Prerequisites**: Java 11+, Maven, Docker, Ollama
2. **Build**: `mvn clean package`
3. **Run**: `sudo java -jar target/ai-code-editor-1.0.0.jar`

### Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes with tests
4. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- **Ollama** - Local AI model hosting
- **JavaFX** - Modern Java UI framework
- **RichTextFX** - Advanced text editing components
- **Docker Java** - Docker integration
- **OkHttp** - HTTP client library

## Roadmap

- [ ] Plugin system for custom AI prompts
- [ ] Version control integration (Git)
- [ ] Remote development server support
- [ ] Custom Docker images for specific languages
- [ ] Collaborative editing features
- [ ] Advanced debugging capabilities
- [ ] Code review AI assistant
- [ ] Performance optimization tools

---

**Note**: This is an autonomous AI development tool. Always review generated code before deploying to production environments.
