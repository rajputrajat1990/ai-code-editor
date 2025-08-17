#!/bin/bash

# AI Code Editor Installation Script for macOS

set -e

echo "Installing AI Code Editor..."

# Check if running with sudo
if [ "$EUID" -ne 0 ]; then
    echo "Please run this script with sudo"
    exit 1
fi

# Install Homebrew if not present
if ! command -v brew &> /dev/null; then
    echo "Installing Homebrew..."
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
fi

# Install Java 11 if not present
if ! command -v java &> /dev/null; then
    echo "Installing Java 11..."
    brew install openjdk@11
    echo 'export PATH="/opt/homebrew/opt/openjdk@11/bin:$PATH"' >> ~/.zshrc
fi

# Install Docker if not present
if ! command -v docker &> /dev/null; then
    echo "Installing Docker..."
    brew install --cask docker
    echo "Please start Docker Desktop manually before proceeding."
    read -p "Press Enter after Docker Desktop is running..."
fi

# Install Maven if not present
if ! command -v mvn &> /dev/null; then
    echo "Installing Maven..."
    brew install maven
fi

# Install Ollama if not present
if ! command -v ollama &> /dev/null; then
    echo "Installing Ollama..."
    brew install ollama
    
    # Start Ollama service
    brew services start ollama
    
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
APP_DIR="/Applications/AI Code Editor.app/Contents"
mkdir -p "$APP_DIR/MacOS"
mkdir -p "$APP_DIR/Resources"

# Copy JAR file
cp target/ai-code-editor-*.jar "$APP_DIR/Resources/ai-code-editor.jar"

# Create Info.plist
cat > "$APP_DIR/Info.plist" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleExecutable</key>
    <string>ai-code-editor</string>
    <key>CFBundleIdentifier</key>
    <string>com.aicodeeditor.app</string>
    <key>CFBundleName</key>
    <string>AI Code Editor</string>
    <key>CFBundleVersion</key>
    <string>1.0.0</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0.0</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
</dict>
</plist>
EOF

# Create launch script
cat > "$APP_DIR/MacOS/ai-code-editor" << 'EOF'
#!/bin/bash
cd "$(dirname "$0")/../Resources"
sudo java -jar ai-code-editor.jar "$@"
EOF

chmod +x "$APP_DIR/MacOS/ai-code-editor"

# Create command-line launcher
cat > /usr/local/bin/ai-code-editor << 'EOF'
#!/bin/bash
sudo java -jar "/Applications/AI Code Editor.app/Contents/Resources/ai-code-editor.jar" "$@"
EOF

chmod +x /usr/local/bin/ai-code-editor

echo ""
echo "✅ AI Code Editor installation completed!"
echo ""
echo "Usage:"
echo "  1. From command line: ai-code-editor"
echo "  2. From Applications folder: AI Code Editor"
echo ""
echo "Prerequisites check:"
echo "  - Java: $(java -version 2>&1 | head -n 1)"
echo "  - Docker: $(docker --version 2>&1 || echo 'Not found')"
echo "  - Ollama: $(ollama --version 2>&1 || echo 'Not found')"
echo "  - Maven: $(mvn --version 2>&1 | head -n 1)"
echo ""
echo "Note: The application requires sudo privileges to run."
echo ""
