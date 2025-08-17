#!/bin/bash

# AI Code Editor Installation Script for Linux

set -e

echo "Installing AI Code Editor..."

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    echo "Please run this script as root (sudo)"
    exit 1
fi

# Update package list
echo "Updating package list..."
apt-get update

# Install Java 11 if not present
if ! command -v java &> /dev/null; then
    echo "Installing Java 11..."
    apt-get install -y openjdk-11-jdk
fi

# Install Docker if not present
if ! command -v docker &> /dev/null; then
    echo "Installing Docker..."
    apt-get install -y docker.io
    systemctl start docker
    systemctl enable docker
fi

# Install Maven if not present
if ! command -v mvn &> /dev/null; then
    echo "Installing Maven..."
    apt-get install -y maven
fi

# Install Ollama if not present
if ! command -v ollama &> /dev/null; then
    echo "Installing Ollama..."
    curl -fsSL https://ollama.ai/install.sh | sh
    
    # Start Ollama service
    systemctl start ollama
    systemctl enable ollama
    
    echo "Waiting for Ollama to start..."
    sleep 10
    
    # Pull default model
    echo "Pulling Llama 3.2 7B model..."
    ollama pull llama3.2:7b
fi

# Build the application
echo "Building AI Code Editor..."
if [ -f "pom.xml" ]; then
    mvn clean package -DskipTests
else
    echo "pom.xml not found. Please run this script from the project root directory."
    exit 1
fi

# Create application directory
APP_DIR="/opt/ai-code-editor"
mkdir -p "$APP_DIR"

# Copy JAR file
cp target/ai-code-editor-*.jar "$APP_DIR/ai-code-editor.jar"

# Create launch script
cat > "$APP_DIR/launch.sh" << 'EOF'
#!/bin/bash
cd /opt/ai-code-editor
java -jar ai-code-editor.jar "$@"
EOF

chmod +x "$APP_DIR/launch.sh"

# Create desktop entry
cat > /usr/share/applications/ai-code-editor.desktop << 'EOF'
[Desktop Entry]
Version=1.0
Type=Application
Name=AI Code Editor
Comment=AI-powered code editor with autonomous development capabilities
Exec=sudo /opt/ai-code-editor/launch.sh
Icon=applications-development
Terminal=true
StartupNotify=true
Categories=Development;IDE;
EOF

# Create command-line launcher
cat > /usr/local/bin/ai-code-editor << 'EOF'
#!/bin/bash
sudo /opt/ai-code-editor/launch.sh "$@"
EOF

chmod +x /usr/local/bin/ai-code-editor

echo ""
echo "✅ AI Code Editor installation completed!"
echo ""
echo "Usage:"
echo "  1. From command line: ai-code-editor"
echo "  2. From applications menu: AI Code Editor"
echo ""
echo "Prerequisites check:"
echo "  - Java: $(java -version 2>&1 | head -n 1)"
echo "  - Docker: $(docker --version 2>&1 || echo 'Not found')"
echo "  - Ollama: $(ollama --version 2>&1 || echo 'Not found')"
echo "  - Maven: $(mvn --version 2>&1 | head -n 1)"
echo ""
echo "Note: The application requires sudo privileges to run."
echo ""
