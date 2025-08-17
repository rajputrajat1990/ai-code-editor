# AI Code Editor - One-Line Installation

## Quick Install

Run this single command to automatically detect your OS, install all dependencies, clone the repository, build the project, and set everything up:

### Linux/macOS/WSL:
```bash
curl -fsSL https://raw.githubusercontent.com/rajputrajat1990/ai-code-editor/master/bootstrap.sh | bash
```

### Alternative (if you prefer to download first):
```bash
# Download the bootstrap script
wget https://raw.githubusercontent.com/rajputrajat1990/ai-code-editor/master/bootstrap.sh

# Make it executable
chmod +x bootstrap.sh

# Run the bootstrap
./bootstrap.sh
```

## What the bootstrap script does:

1. **Detects your operating system** (Ubuntu, RHEL/CentOS, Fedora, Arch, macOS, Windows)
2. **Installs required dependencies**:
   - Git (if not installed)
   - Java 21 (OpenJDK)
   - Maven 3.x
   - Docker & Docker Compose
   - Ollama (AI model runtime)
3. **Clones the repository** from GitHub
4. **Downloads AI model** (Llama 3.2 7B)
5. **Builds the project** with Maven
6. **Runs the platform-specific installer**
7. **Provides final instructions**

## Supported Operating Systems:

- ✅ **Ubuntu** (18.04+)
- ✅ **RHEL/CentOS** (7+)
- ✅ **Fedora** (30+)
- ✅ **Arch Linux**
- ✅ **macOS** (10.15+) 
- ✅ **Windows** (via WSL2 or Git Bash)

## Manual Installation

If you prefer manual installation or the bootstrap fails, you can:

1. **Install dependencies manually**:
   - Java 21: `sudo apt install openjdk-21-jdk` (Ubuntu)
   - Maven: `sudo apt install maven`
   - Docker: Follow [Docker installation guide](https://docs.docker.com/engine/install/)
   - Ollama: `curl -fsSL https://ollama.ai/install.sh | sh`

2. **Clone and build**:
   ```bash
   git clone https://github.com/rajputrajat1990/ai-code-editor.git
   cd ai-code-editor
   mvn clean package
   ```

3. **Run the app**:
   ```bash
   sudo java --module-path /usr/share/openjfx/lib --add-modules javafx.controls,javafx.fxml -jar target/ai-code-editor-1.0.0.jar
   ```

## Requirements:

- **Admin/sudo privileges** (required for system package installation)
- **Internet connection** (for downloading dependencies and AI models)
- **4GB+ RAM** (recommended for AI models)
- **Docker support** (for sandboxed code execution)

## After Installation:

1. Start Ollama: `ollama serve`
2. Launch AI Code Editor (with admin privileges)
3. Start coding with AI assistance!

The application will automatically:
- Generate code based on your requirements
- Execute code in secure Docker containers
- Install dependencies as needed
- Research solutions online for better code quality
