#!/bin/bash

# AI Code Editor Installation Script for Linux
# Comprehensive installation for systems with no prerequisites

set -e

echo "=============================================="
echo "  AI Code Editor Installation Script"
echo "  Installing ALL prerequisites from scratch"
echo "=============================================="

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    echo "❌ Please run this script as root (sudo)"
    echo "   Example: sudo ./install-linux.sh"
    exit 1
fi

# Detect Linux distribution
if [ -f /etc/os-release ]; then
    . /etc/os-release
    DISTRO=$ID
    VERSION=$VERSION_ID
else
    echo "❌ Cannot detect Linux distribution"
    exit 1
fi

echo "📋 Detected system: $PRETTY_NAME"
echo ""

# Update package list
echo "🔄 Updating package list..."
case "$DISTRO" in
    ubuntu|debian)
        apt-get update -y
        ;;
    centos|rhel|fedora)
        if command -v dnf &> /dev/null; then
            dnf update -y
        else
            yum update -y
        fi
        ;;
    arch|manjaro)
        pacman -Sy --noconfirm
        ;;
    *)
        echo "⚠️  Unsupported distribution: $DISTRO"
        echo "   Please install dependencies manually:"
        echo "   - Java 21 (OpenJDK)"
        echo "   - Docker"
        echo "   - Maven"
        echo "   - curl, wget"
        exit 1
        ;;
esac

# Install essential tools first
echo "🛠️  Installing essential tools..."
case "$DISTRO" in
    ubuntu|debian)
        apt-get install -y curl wget software-properties-common apt-transport-https ca-certificates gnupg lsb-release
        ;;
    centos|rhel|fedora)
        if command -v dnf &> /dev/null; then
            dnf install -y curl wget which
        else
            yum install -y curl wget which
        fi
        ;;
    arch|manjaro)
        pacman -S --noconfirm curl wget which
        ;;
esac

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
    case "$DISTRO" in
        ubuntu|debian)
            # Add OpenJDK repository for latest Java versions
            apt-get install -y openjdk-21-jdk
            if [ $? -ne 0 ]; then
                # Fallback: install from Eclipse Temurin
                echo "🔄 Trying Eclipse Temurin repository..."
                wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | apt-key add -
                echo "deb https://packages.adoptium.net/artifactory/deb $(lsb_release -cs) main" | tee /etc/apt/sources.list.d/adoptium.list
                apt-get update
                apt-get install -y temurin-21-jdk
            fi
            ;;
        centos|rhel|fedora)
            if command -v dnf &> /dev/null; then
                dnf install -y java-21-openjdk-devel
            else
                yum install -y java-21-openjdk-devel
            fi
            ;;
        arch|manjaro)
            pacman -S --noconfirm jdk21-openjdk
            ;;
    esac
    
    # Set JAVA_HOME
    export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
    echo "export JAVA_HOME=$JAVA_HOME" >> /etc/environment
fi

# Install JavaFX (required for Java 11+)
echo "🎨 Checking JavaFX installation..."
if ! find /usr -name "*javafx*" 2>/dev/null | grep -q javafx; then
    echo "📦 Installing JavaFX..."
    case "$DISTRO" in
        ubuntu|debian)
            apt-get install -y openjfx libopenjfx-java
            ;;
        centos|rhel|fedora)
            if command -v dnf &> /dev/null; then
                dnf install -y java-21-openjfx java-21-openjfx-devel
            else
                yum install -y java-21-openjfx java-21-openjfx-devel
            fi
            ;;
        arch|manjaro)
            pacman -S --noconfirm java-openjfx
            ;;
    esac
    
    # Set JavaFX module path
    JAVAFX_PATH=$(find /usr -name "javafx*.jar" 2>/dev/null | head -1 | xargs dirname 2>/dev/null || echo "/usr/share/openjfx/lib")
    if [ -d "$JAVAFX_PATH" ]; then
        echo "export JAVAFX_PATH=$JAVAFX_PATH" >> /etc/environment
        echo "✅ JavaFX installed at: $JAVAFX_PATH"
    fi
else
    echo "✅ JavaFX already installed"
fi

# Install Docker if not present
echo "🐳 Checking Docker installation..."
if ! command -v docker &> /dev/null; then
    echo "📦 Installing Docker..."
    case "$DISTRO" in
        ubuntu|debian)
            # Add Docker's official GPG key
            curl -fsSL https://download.docker.com/linux/$DISTRO/gpg | apt-key add -
            # Add Docker repository
            add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/$DISTRO $(lsb_release -cs) stable"
            apt-get update
            apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
            ;;
        centos|rhel)
            if command -v dnf &> /dev/null; then
                dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
                dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
            else
                yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
                yum install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
            fi
            ;;
        fedora)
            dnf config-manager --add-repo https://download.docker.com/linux/fedora/docker-ce.repo
            dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
            ;;
        arch|manjaro)
            pacman -S --noconfirm docker docker-compose
            ;;
    esac
    
    # Start and enable Docker service
    systemctl start docker
    systemctl enable docker
    
    # Add current user to docker group (for non-root access)
    if [ -n "$SUDO_USER" ]; then
        usermod -aG docker $SUDO_USER
        echo "ℹ️  User $SUDO_USER added to docker group"
    fi
else
    echo "✅ Docker is already installed"
fi

# Install Maven if not present
echo "🏗️  Checking Maven installation..."
if ! command -v mvn &> /dev/null; then
    echo "📦 Installing Maven..."
    case "$DISTRO" in
        ubuntu|debian)
            apt-get install -y maven
            ;;
        centos|rhel|fedora)
            if command -v dnf &> /dev/null; then
                dnf install -y maven
            else
                yum install -y maven
            fi
            ;;
        arch|manjaro)
            pacman -S --noconfirm maven
            ;;
    esac
else
    echo "✅ Maven is already installed"
fi

# Install Ollama if not present
echo "🤖 Checking Ollama installation..."
if ! command -v ollama &> /dev/null; then
    echo "📦 Installing Ollama..."
    curl -fsSL https://ollama.ai/install.sh | sh
    
    # Create systemd service for Ollama
    cat > /etc/systemd/system/ollama.service << 'EOF'
[Unit]
Description=Ollama Service
After=network-online.target

[Service]
ExecStart=/usr/local/bin/ollama serve
User=ollama
Group=ollama
Restart=always
RestartSec=3
Environment="OLLAMA_HOST=0.0.0.0"

[Install]
WantedBy=default.target
EOF

    # Create ollama user if it doesn't exist
    if ! id "ollama" &>/dev/null; then
        useradd -r -s /bin/false -d /usr/share/ollama ollama
    fi
    
    # Start Ollama service
    systemctl daemon-reload
    systemctl start ollama
    systemctl enable ollama
    
    echo "⏳ Waiting for Ollama to start..."
    sleep 15
    
    # Test if Ollama is responding
    if ! curl -s http://localhost:11434/api/tags > /dev/null; then
        echo "⚠️  Ollama service may not be running properly"
        echo "   Trying to start manually..."
        ollama serve &
        sleep 10
    fi
    
    # Pull recommended models
    echo "📥 Pulling recommended AI models..."
    echo "   This may take several minutes depending on your internet connection..."
    
    # Pull lightweight models first
    ollama pull llama3.2:3b
    echo "✅ Pulled Llama 3.2 3B (lightweight, fast)"
    
    ollama pull mistral:7b  
    echo "✅ Pulled Mistral 7B (balanced performance)"
    
    # Ask user if they want larger models
    echo ""
    read -p "🤔 Would you like to install larger, more capable models? (y/N): " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        ollama pull llama3.2:7b
        echo "✅ Pulled Llama 3.2 7B (high performance)"
        
        ollama pull codellama:7b
        echo "✅ Pulled CodeLlama 7B (specialized for coding)"
    fi
else
    echo "✅ Ollama is already installed"
    echo "📋 Available models:"
    ollama list
fi

# Build the application
echo "🏗️  Building AI Code Editor..."
if [ -f "pom.xml" ]; then
    echo "🔍 Verifying build environment..."
    
    # Check Java version for build
    JAVA_BUILD_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_BUILD_VERSION" -lt 21 ]; then
        echo "❌ Java 21+ is required for building. Found Java $JAVA_BUILD_VERSION"
        exit 1
    fi
    
    # Check Maven version
    MVN_VERSION=$(mvn --version 2>&1 | head -n 1)
    echo "✅ Build environment ready: $MVN_VERSION"
    
    echo "🔨 Compiling application..."
    mvn clean package -DskipTests -q
    
    if [ ! -f "target/ai-code-editor-1.0.0.jar" ]; then
        echo "❌ Build failed - JAR file not found"
        exit 1
    fi
    
    JAR_SIZE=$(du -h target/ai-code-editor-1.0.0.jar | cut -f1)
    echo "✅ Build successful - JAR size: $JAR_SIZE"
else
    echo "❌ pom.xml not found. Please run this script from the project root directory."
    exit 1
fi

# Create application directory
echo "📂 Setting up application directory..."
APP_DIR="/opt/ai-code-editor"
mkdir -p "$APP_DIR"

# Copy JAR file
cp target/ai-code-editor-1.0.0.jar "$APP_DIR/ai-code-editor.jar"

# Create comprehensive launch script with environment checks
cat > "$APP_DIR/launch.sh" << 'EOF'
#!/bin/bash

# AI Code Editor Launch Script with Environment Validation

set -e

APP_DIR="/opt/ai-code-editor"
cd "$APP_DIR"

echo "🚀 Starting AI Code Editor..."

# Validate Java
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "❌ Java 21+ required. Found Java $JAVA_VERSION"
    echo "   Please run the installation script again."
    exit 1
fi

# Validate Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker not found. Please run the installation script again."
    exit 1
fi

# Check Docker daemon
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker daemon not running. Starting Docker..."
    sudo systemctl start docker
    sleep 5
fi

# Validate Ollama
if ! command -v ollama &> /dev/null; then
    echo "❌ Ollama not found. Please run the installation script again."
    exit 1
fi

# Check Ollama service
if ! curl -s http://localhost:11434/api/tags > /dev/null; then
    echo "⚠️  Ollama service not responding. Attempting to start..."
    sudo systemctl start ollama
    sleep 10
    
    if ! curl -s http://localhost:11434/api/tags > /dev/null; then
        echo "❌ Ollama service failed to start. Please check the installation."
        exit 1
    fi
fi

# Check available models
MODELS=$(ollama list | tail -n +2 | wc -l)
if [ "$MODELS" -eq 0 ]; then
    echo "⚠️  No AI models found. Installing basic model..."
    ollama pull llama3.2:3b
fi

echo "✅ Environment validation complete"
echo "🎯 Launching AI Code Editor with Intelligence Amplification..."

# Launch with appropriate memory settings
java -Xmx4G -jar ai-code-editor.jar "$@"
EOF

chmod +x "$APP_DIR/launch.sh"

# Create enhanced desktop entry
cat > /usr/share/applications/ai-code-editor.desktop << 'EOF'
[Desktop Entry]
Version=1.0
Type=Application
Name=AI Code Editor
Comment=AI-powered code editor with autonomous development capabilities and intelligence amplification
Exec=sudo /opt/ai-code-editor/launch.sh
Icon=applications-development
Terminal=true
StartupNotify=true
Categories=Development;IDE;TextEditor;
Keywords=AI;Code;Editor;Development;Programming;Intelligence;
MimeType=text/plain;text/x-java;text/x-python;text/x-c;text/x-c++;text/javascript;application/json;
EOF

# Create command-line launcher with validation
cat > /usr/local/bin/ai-code-editor << 'EOF'
#!/bin/bash

# AI Code Editor Command-Line Launcher

# Check if running as root
if [ "$EUID" -eq 0 ]; then
    echo "⚠️  Running as root. This may cause permission issues."
    echo "   Consider running as a regular user with sudo when needed."
fi

# Launch the application
sudo /opt/ai-code-editor/launch.sh "$@"
EOF

chmod +x /usr/local/bin/ai-code-editor

# Create uninstaller script
cat > "$APP_DIR/uninstall.sh" << 'EOF'
#!/bin/bash

echo "🗑️  Uninstalling AI Code Editor..."

# Remove application files
sudo rm -rf /opt/ai-code-editor
sudo rm -f /usr/local/bin/ai-code-editor
sudo rm -f /usr/share/applications/ai-code-editor.desktop

echo "✅ AI Code Editor uninstalled"
echo "ℹ️  System dependencies (Java, Docker, Maven, Ollama) were not removed"
echo "   Remove them manually if no longer needed"
EOF

chmod +x "$APP_DIR/uninstall.sh"

# Comprehensive installation verification
echo ""
echo "🔍 Performing comprehensive installation verification..."
echo "=============================================="

# Verify Java
JAVA_FINAL_VERSION=$(java -version 2>&1 | head -n 1)
echo "☕ Java: $JAVA_FINAL_VERSION"

# Verify Docker
DOCKER_VERSION=$(docker --version 2>&1)
if docker info > /dev/null 2>&1; then
    echo "🐳 Docker: $DOCKER_VERSION ✅"
else
    echo "🐳 Docker: $DOCKER_VERSION ⚠️ (daemon not running)"
fi

# Verify Maven
MAVEN_VERSION=$(mvn --version 2>&1 | head -n 1)
echo "🏗️  Maven: $MAVEN_VERSION"

# Verify Ollama and models
OLLAMA_VERSION=$(ollama --version 2>&1)
echo "🤖 Ollama: $OLLAMA_VERSION"

if curl -s http://localhost:11434/api/tags > /dev/null; then
    echo "📋 Available AI Models:"
    ollama list | while IFS= read -r line; do
        if [[ ! "$line" =~ ^NAME.* ]]; then
            echo "   📦 $line"
        fi
    done
else
    echo "⚠️  Ollama service not responding"
fi

# Verify application
if [ -f "$APP_DIR/ai-code-editor.jar" ]; then
    JAR_SIZE=$(du -h "$APP_DIR/ai-code-editor.jar" | cut -f1)
    echo "🎯 AI Code Editor: Installed ($JAR_SIZE) ✅"
else
    echo "❌ AI Code Editor: Installation failed"
    exit 1
fi

echo ""
echo "🎉 AI Code Editor installation completed successfully!"
echo "=============================================="
echo ""
echo "🚀 Usage Options:"
echo "   📱 Command line: ai-code-editor"
echo "   🖥️  Applications menu: AI Code Editor"
echo "   🛠️  Direct launch: sudo /opt/ai-code-editor/launch.sh"
echo ""
echo "🔧 Management:"
echo "   🗑️  Uninstall: sudo /opt/ai-code-editor/uninstall.sh"
echo "   📊 Status check: systemctl status ollama docker"
echo ""
echo "⚡ Features Ready:"
echo "   ✅ Intelligence Amplification Framework"
echo "   ✅ Proactive Code Verification System" 
echo "   ✅ Cross-platform compatibility"
echo "   ✅ Autonomous development workflows"
echo "   ✅ Local AI processing with Ollama"
echo "   ✅ Docker sandbox environment"
echo ""
echo "📚 Documentation:"
echo "   📖 Intelligence Amplification: ./INTELLIGENCE_AMPLIFICATION.md"
echo "   🔍 Code Verification: ./PROACTIVE_CODE_VERIFICATION.md" 
echo "   📋 Complete Guide: ./COMPLETE_SYSTEM_SUMMARY.md"
echo ""
echo "⚠️  Note: The application requires sudo privileges for:"
echo "   • Docker container management"
echo "   • System dependency installation"
echo "   • Sandbox environment operations"
echo ""

# Final environment check for user
if [ -n "$SUDO_USER" ]; then
    echo "👤 Installation completed for user: $SUDO_USER"
    echo "🔄 Please log out and back in to apply Docker group membership"
    echo "   Or run: newgrp docker"
fi

echo "✨ Ready to revolutionize your development workflow!"
echo ""
