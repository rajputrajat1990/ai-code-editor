#!/bin/bash

# AI Code Editor System Requirements Verification Script
# Verifies all requirements are properly installed and configured

echo "🔍 AI Code Editor - System Requirements Verification"
echo "====================================================="
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print status
print_status() {
    local component=$1
    local status=$2
    local details=$3
    
    if [ "$status" = "OK" ]; then
        echo -e "${component}: ${GREEN}✅ ${details}${NC}"
    elif [ "$status" = "WARNING" ]; then
        echo -e "${component}: ${YELLOW}⚠️  ${details}${NC}"
    else
        echo -e "${component}: ${RED}❌ ${details}${NC}"
    fi
}

# Detect OS
OS=$(uname -s)
case "$OS" in
    Linux*)     PLATFORM="Linux";;
    Darwin*)    PLATFORM="macOS";;
    CYGWIN*|MINGW*|MSYS*) PLATFORM="Windows";;
    *)          PLATFORM="Unknown";;
esac

echo "🖥️  Platform: $PLATFORM"
echo ""

# Check Java
echo "☕ Checking Java Requirements..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    JAVA_MAJOR=$(echo $JAVA_VERSION | cut -d'.' -f1)
    
    if [ "$JAVA_MAJOR" -ge 21 ]; then
        print_status "Java Version" "OK" "$JAVA_VERSION (Required: 21+)"
    else
        print_status "Java Version" "ERROR" "$JAVA_VERSION (Required: 21+)"
        echo "   📥 Install Java 21: https://adoptium.net/"
    fi
    
    # Check JAVA_HOME
    if [ -n "$JAVA_HOME" ]; then
        print_status "JAVA_HOME" "OK" "$JAVA_HOME"
    else
        print_status "JAVA_HOME" "WARNING" "Not set (may cause issues)"
    fi
else
    print_status "Java" "ERROR" "Not installed"
    echo "   📥 Install Java 21: https://adoptium.net/"
fi

echo ""

# Check JavaFX
echo "🎨 Checking JavaFX Requirements..."
JAVAFX_FOUND=false
JAVAFX_PATHS=(
    "/usr/share/openjfx/lib"
    "/usr/lib/jvm/java-*-openjdk*/lib"
    "/opt/homebrew/lib"
    "$JAVAFX_PATH"
)

# Find JavaFX installation
for pattern in "${JAVAFX_PATHS[@]}"; do
    for path in $pattern; do
        if [ -d "$path" ] && ls "$path"/*javafx*.jar >/dev/null 2>&1; then
            print_status "JavaFX" "OK" "Found at $path"
            JAVAFX_FOUND=true
            break 2
        fi
    done
done

if [ "$JAVAFX_FOUND" = false ]; then
    print_status "JavaFX" "ERROR" "Not found"
    echo "   📥 Install JavaFX:"
    echo "      Ubuntu/Debian: sudo apt install openjfx libopenjfx-java"  
    echo "      Fedora/CentOS: sudo dnf install java-*-openjfx"
    echo "      Arch Linux: sudo pacman -S java-openjfx"
    echo "      macOS: brew install openjfx"
fi

echo ""

# Check Docker
echo "🐳 Checking Docker Requirements..."
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version | cut -d' ' -f3 | cut -d',' -f1)
    print_status "Docker Version" "OK" "$DOCKER_VERSION"
    
    # Check if Docker daemon is running
    if docker info > /dev/null 2>&1; then
        print_status "Docker Daemon" "OK" "Running"
        
        # Check Docker permissions
        if docker ps > /dev/null 2>&1; then
            print_status "Docker Permissions" "OK" "User has access"
        else
            print_status "Docker Permissions" "WARNING" "May need to run as admin/sudo"
        fi
    else
        print_status "Docker Daemon" "ERROR" "Not running"
        echo "   🚀 Start Docker: systemctl start docker (Linux) or Docker Desktop"
    fi
else
    print_status "Docker" "ERROR" "Not installed"
    echo "   📥 Install Docker: https://docs.docker.com/get-docker/"
fi

echo ""

# Check Maven
echo "🏗️  Checking Maven Requirements..."
if command -v mvn &> /dev/null; then
    MAVEN_VERSION=$(mvn --version | head -n 1 | cut -d' ' -f3)
    print_status "Maven Version" "OK" "$MAVEN_VERSION"
else
    print_status "Maven" "ERROR" "Not installed"
    echo "   📥 Install Maven: https://maven.apache.org/install.html"
fi

echo ""

# Check Ollama
echo "🤖 Checking Ollama Requirements..."
if command -v ollama &> /dev/null; then
    OLLAMA_VERSION=$(ollama --version 2>/dev/null || echo "Unknown")
    print_status "Ollama Version" "OK" "$OLLAMA_VERSION"
    
    # Check if Ollama service is running
    if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
        print_status "Ollama Service" "OK" "Running on localhost:11434"
        
        # Check available models
        MODEL_COUNT=$(ollama list 2>/dev/null | tail -n +2 | wc -l | tr -d ' ')
        if [ "$MODEL_COUNT" -gt 0 ]; then
            print_status "AI Models" "OK" "$MODEL_COUNT models available"
            echo "   📋 Available models:"
            ollama list | tail -n +2 | while IFS= read -r line; do
                MODEL_NAME=$(echo "$line" | awk '{print $1}')
                MODEL_SIZE=$(echo "$line" | awk '{print $2}')
                echo "      🔹 $MODEL_NAME ($MODEL_SIZE)"
            done
        else
            print_status "AI Models" "WARNING" "No models installed"
            echo "   📥 Install basic model: ollama pull llama3.2:3b"
        fi
    else
        print_status "Ollama Service" "ERROR" "Not responding on localhost:11434"
        echo "   🚀 Start Ollama: ollama serve"
    fi
else
    print_status "Ollama" "ERROR" "Not installed"
    echo "   📥 Install Ollama: https://ollama.ai/"
fi

echo ""

# Check system resources
echo "💻 Checking System Resources..."

# Check available memory
if command -v free &> /dev/null; then
    # Linux
    TOTAL_MEM=$(free -g | awk 'NR==2{print $2}')
    AVAILABLE_MEM=$(free -g | awk 'NR==2{print $7}')
    
    if [ "$TOTAL_MEM" -ge 8 ]; then
        print_status "System Memory" "OK" "${TOTAL_MEM}GB total"
    elif [ "$TOTAL_MEM" -ge 4 ]; then
        print_status "System Memory" "WARNING" "${TOTAL_MEM}GB total (8GB+ recommended)"
    else
        print_status "System Memory" "ERROR" "${TOTAL_MEM}GB total (4GB minimum)"
    fi
elif command -v vm_stat &> /dev/null; then
    # macOS
    TOTAL_MEM_BYTES=$(sysctl -n hw.memsize)
    TOTAL_MEM_GB=$((TOTAL_MEM_BYTES / 1024 / 1024 / 1024))
    
    if [ "$TOTAL_MEM_GB" -ge 8 ]; then
        print_status "System Memory" "OK" "${TOTAL_MEM_GB}GB total"
    elif [ "$TOTAL_MEM_GB" -ge 4 ]; then
        print_status "System Memory" "WARNING" "${TOTAL_MEM_GB}GB total (8GB+ recommended)"
    else
        print_status "System Memory" "ERROR" "${TOTAL_MEM_GB}GB total (4GB minimum)"
    fi
fi

# Check available disk space
AVAILABLE_SPACE=$(df -h . | awk 'NR==2 {print $4}' | sed 's/[^0-9.]*//g')
if [ -n "$AVAILABLE_SPACE" ]; then
    if (( $(echo "$AVAILABLE_SPACE >= 5.0" | bc -l) )); then
        print_status "Disk Space" "OK" "${AVAILABLE_SPACE}GB available"
    elif (( $(echo "$AVAILABLE_SPACE >= 2.0" | bc -l) )); then
        print_status "Disk Space" "WARNING" "${AVAILABLE_SPACE}GB available (5GB+ recommended)"
    else
        print_status "Disk Space" "ERROR" "${AVAILABLE_SPACE}GB available (2GB minimum)"
    fi
fi

echo ""

# Check network connectivity
echo "🌐 Checking Network Requirements..."
if curl -s --max-time 10 https://ollama.ai > /dev/null; then
    print_status "Internet Connection" "OK" "Can reach ollama.ai"
else
    print_status "Internet Connection" "WARNING" "Cannot reach ollama.ai (required for model downloads)"
fi

if curl -s --max-time 10 https://github.com > /dev/null; then
    print_status "GitHub Access" "OK" "Can reach github.com"
else
    print_status "GitHub Access" "WARNING" "Cannot reach github.com"
fi

echo ""

# Check if AI Code Editor is installed
echo "🎯 Checking AI Code Editor Installation..."

# Common installation paths
INSTALL_PATHS=(
    "/opt/ai-code-editor/ai-code-editor.jar"
    "C:/Program Files/AI-Code-Editor/ai-code-editor.jar"
    "/Applications/AI-Code-Editor.app"
    "./target/ai-code-editor-1.0.0.jar"
)

AI_EDITOR_FOUND=false
for path in "${INSTALL_PATHS[@]}"; do
    if [ -f "$path" ]; then
        AI_EDITOR_FOUND=true
        FILE_SIZE=$(du -h "$path" 2>/dev/null | cut -f1)
        print_status "AI Code Editor" "OK" "Found at $path ($FILE_SIZE)"
        break
    fi
done

if [ "$AI_EDITOR_FOUND" = false ]; then
    print_status "AI Code Editor" "WARNING" "Not found in common locations"
    echo "   🏗️  Build with: mvn clean package"
fi

echo ""

# Summary
echo "📋 INSTALLATION SUMMARY"
echo "======================="

# Count issues
CRITICAL_ISSUES=0
WARNINGS=0

# Check for critical issues
if ! command -v java &> /dev/null || [ "$JAVA_MAJOR" -lt 21 ]; then
    CRITICAL_ISSUES=$((CRITICAL_ISSUES + 1))
fi

if ! command -v docker &> /dev/null; then
    CRITICAL_ISSUES=$((CRITICAL_ISSUES + 1))
fi

if ! command -v mvn &> /dev/null; then
    CRITICAL_ISSUES=$((CRITICAL_ISSUES + 1))
fi

if ! command -v ollama &> /dev/null; then
    CRITICAL_ISSUES=$((CRITICAL_ISSUES + 1))
fi

# Print summary
if [ "$CRITICAL_ISSUES" -eq 0 ]; then
    echo -e "${GREEN}✅ System ready for AI Code Editor!${NC}"
    echo ""
    echo "🚀 Next steps:"
    echo "   1. Build the application: mvn clean package"
    echo "   2. Run the application: java -jar target/ai-code-editor-1.0.0.jar"
    echo "   3. Or use installation script for your platform"
else
    echo -e "${RED}❌ $CRITICAL_ISSUES critical issue(s) found${NC}"
    echo ""
    echo "🛠️  Required actions:"
    echo "   1. Install missing components listed above"
    echo "   2. Run this script again to verify"
    echo "   3. Use the platform-specific installation script"
fi

echo ""
echo "📚 Installation Scripts:"
echo "   🐧 Linux: sudo ./scripts/install-linux.sh"
echo "   🪟 Windows: Run scripts/install-windows.bat as Administrator"
echo "   🍎 macOS: sudo ./scripts/install-macos.sh"
echo ""
echo "📖 Documentation:"
echo "   📋 Complete Guide: ./COMPLETE_SYSTEM_SUMMARY.md"
echo "   🔍 Features: ./PROACTIVE_CODE_VERIFICATION.md"
echo "   🧠 Intelligence: ./INTELLIGENCE_AMPLIFICATION.md"
echo ""
