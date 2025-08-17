#!/bin/bash

# AI Code Editor Installation Script for macOS
# Comprehensive installation for systems with no prerequisites

set -e

echo "=============================================="
echo "  AI Code Editor Installation Script"
echo "  Installing ALL prerequisites from scratch"
echo "=============================================="

# Check if running with sudo
if [ "$EUID" -ne 0 ]; then
    echo "❌ Please run this script with sudo"
    echo "   Example: sudo ./install-macos.sh"
    exit 1
fi

echo "📋 Detected system: macOS"
echo ""

# Get the original user (who called sudo)
ORIGINAL_USER=${SUDO_USER:-$(logname)}
ORIGINAL_HOME=$(eval echo ~$ORIGINAL_USER)

echo "👤 Installing for user: $ORIGINAL_USER"
echo ""

# Install Homebrew if not present (as the original user)
echo "🍺 Checking Homebrew installation..."
if ! sudo -u $ORIGINAL_USER command -v brew &> /dev/null; then
    echo "📦 Installing Homebrew..."
    sudo -u $ORIGINAL_USER /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    
    # Add Homebrew to PATH for the session
    if [[ $(uname -m) == "arm64" ]]; then
        echo 'export PATH="/opt/homebrew/bin:$PATH"' >> $ORIGINAL_HOME/.zshrc
        export PATH="/opt/homebrew/bin:$PATH"
    else
        echo 'export PATH="/usr/local/bin:$PATH"' >> $ORIGINAL_HOME/.zshrc
        export PATH="/usr/local/bin:$PATH"
    fi
    
    # Source the updated PATH
    sudo -u $ORIGINAL_USER source $ORIGINAL_HOME/.zshrc || true
else
    echo "✅ Homebrew is already installed"
fi

# Install Java 21 if not present or wrong version
echo "☕ Checking Java installation..."
JAVA_REQUIRED=21
JAVA_INSTALLED=false

if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge "$JAVA_REQUIRED" ]; then
        echo "✅ Java $JAVA_VERSION is already installed"
        JAVA_INSTALLED=true
    else
        echo "⚠️  Java $JAVA_VERSION found, but Java $JAVA_REQUIRED is required"
    fi
fi

if [ "$JAVA_INSTALLED" = false ]; then
    echo "📦 Installing Java 21..."
    sudo -u $ORIGINAL_USER brew install openjdk@21
    
    # Link Java for system use
    sudo ln -sfn $(brew --prefix)/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
    
    # Add Java to PATH
    echo 'export PATH="$(brew --prefix)/opt/openjdk@21/bin:$PATH"' >> $ORIGINAL_HOME/.zshrc
    export PATH="$(brew --prefix)/opt/openjdk@21/bin:$PATH"
    
    # Set JAVA_HOME
    echo 'export JAVA_HOME="$(brew --prefix)/opt/openjdk@21"' >> $ORIGINAL_HOME/.zshrc
    export JAVA_HOME="$(brew --prefix)/opt/openjdk@21"
fi

# Install JavaFX if not present
echo "🎨 Checking JavaFX installation..."
if ! brew list | grep -q openjfx; then
    echo "📦 Installing JavaFX..."
    sudo -u $ORIGINAL_USER brew install openjfx
    
    # Set JavaFX path
    JAVAFX_PATH="$(brew --prefix)/lib"
    echo "export JAVAFX_PATH=\"$JAVAFX_PATH\"" >> $ORIGINAL_HOME/.zshrc
    echo "✅ JavaFX installed at: $JAVAFX_PATH"
else
    echo "✅ JavaFX already installed"
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
