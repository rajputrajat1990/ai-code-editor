#!/bin/bash

# AI Code Editor Bootstrap Script
# Detects OS, clones repository, installs dependencies, and sets up the application
# Compatible with Ubuntu, RHEL/CentOS/Fedora, macOS, and Windows (via Git Bash/WSL)

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to detect OS
detect_os() {
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        if [ -f /etc/debian_version ]; then
            echo "ubuntu"
        elif [ -f /etc/redhat-release ]; then
            echo "rhel"
        elif [ -f /etc/fedora-release ]; then
            echo "fedora"
        elif [ -f /etc/arch-release ]; then
            echo "arch"
        else
            echo "linux"
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        echo "macos"
    elif [[ "$OSTYPE" == "cygwin" ]] || [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "win32" ]]; then
        echo "windows"
    else
        echo "unknown"
    fi
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check if running as root/admin
check_admin_privileges() {
    if [[ $EUID -eq 0 ]]; then
        return 0
    else
        return 1
    fi
}

# Function to request admin privileges
request_admin() {
    log_warning "This script requires administrative privileges to install system packages."
    if [[ "$OS" == "macos" ]]; then
        log_info "Please enter your password when prompted by sudo."
    elif [[ "$OS" == "windows" ]]; then
        log_error "Please run this script as Administrator in PowerShell or Command Prompt."
        exit 1
    else
        log_info "Please enter your password when prompted by sudo."
    fi
}

# Function to install Git
install_git() {
    log_info "Installing Git..."
    case "$OS" in
        "ubuntu")
            sudo apt update && sudo apt install -y git
            ;;
        "rhel"|"fedora")
            if command_exists dnf; then
                sudo dnf install -y git
            else
                sudo yum install -y git
            fi
            ;;
        "arch")
            sudo pacman -S --noconfirm git
            ;;
        "macos")
            if command_exists brew; then
                brew install git
            else
                log_error "Please install Homebrew first: /bin/bash -c \"\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\""
                exit 1
            fi
            ;;
        *)
            log_error "Unsupported OS for automatic Git installation. Please install Git manually."
            exit 1
            ;;
    esac
    log_success "Git installed successfully."
}

# Function to install Java
install_java() {
    log_info "Installing Java 21..."
    case "$OS" in
        "ubuntu")
            sudo apt update && sudo apt install -y openjdk-21-jdk
            ;;
        "rhel"|"fedora")
            if command_exists dnf; then
                sudo dnf install -y java-21-openjdk java-21-openjdk-devel
            else
                sudo yum install -y java-21-openjdk java-21-openjdk-devel
            fi
            ;;
        "arch")
            sudo pacman -S --noconfirm jdk21-openjdk
            ;;
        "macos")
            if command_exists brew; then
                brew install openjdk@21
                # Add to PATH
                echo 'export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"' >> ~/.zshrc
                export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"
            else
                log_error "Please install Homebrew first."
                exit 1
            fi
            ;;
        *)
            log_error "Unsupported OS for automatic Java installation. Please install Java 21 manually."
            exit 1
            ;;
    esac
    log_success "Java installed successfully."
}

# Function to install Maven
install_maven() {
    log_info "Installing Maven..."
    case "$OS" in
        "ubuntu")
            sudo apt update && sudo apt install -y maven
            ;;
        "rhel"|"fedora")
            if command_exists dnf; then
                sudo dnf install -y maven
            else
                sudo yum install -y maven
            fi
            ;;
        "arch")
            sudo pacman -S --noconfirm maven
            ;;
        "macos")
            if command_exists brew; then
                brew install maven
            else
                log_error "Please install Homebrew first."
                exit 1
            fi
            ;;
        *)
            log_error "Unsupported OS for automatic Maven installation. Please install Maven manually."
            exit 1
            ;;
    esac
    log_success "Maven installed successfully."
}

# Function to install Docker
install_docker() {
    log_info "Installing Docker..."
    case "$OS" in
        "ubuntu")
            # Remove old versions
            sudo apt-get remove -y docker docker-engine docker.io containerd runc || true
            
            # Install dependencies
            sudo apt update
            sudo apt install -y apt-transport-https ca-certificates curl gnupg lsb-release
            
            # Add Docker's official GPG key
            curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
            
            # Set up stable repository
            echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
            
            # Install Docker Engine
            sudo apt update
            sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
            
            # Add current user to docker group
            sudo usermod -aG docker $USER
            
            # Start Docker service
            sudo systemctl enable docker
            sudo systemctl start docker
            ;;
        "rhel"|"fedora")
            if command_exists dnf; then
                sudo dnf remove -y docker docker-client docker-client-latest docker-common docker-latest docker-latest-logrotate docker-logrotate docker-selinux docker-engine-selinux docker-engine || true
                sudo dnf install -y dnf-plugins-core
                sudo dnf config-manager --add-repo https://download.docker.com/linux/fedora/docker-ce.repo
                sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
            else
                sudo yum remove -y docker docker-client docker-client-latest docker-common docker-latest docker-latest-logrotate docker-logrotate docker-engine || true
                sudo yum install -y yum-utils
                sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
                sudo yum install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
            fi
            
            sudo usermod -aG docker $USER
            sudo systemctl enable docker
            sudo systemctl start docker
            ;;
        "arch")
            sudo pacman -S --noconfirm docker docker-compose
            sudo usermod -aG docker $USER
            sudo systemctl enable docker
            sudo systemctl start docker
            ;;
        "macos")
            if command_exists brew; then
                log_info "Installing Docker Desktop for macOS..."
                brew install --cask docker
                log_warning "Please start Docker Desktop manually after installation."
            else
                log_error "Please install Homebrew first."
                exit 1
            fi
            ;;
        *)
            log_warning "Docker installation not automated for your OS. Please install Docker manually."
            ;;
    esac
    log_success "Docker installation completed."
}

# Function to install Ollama
install_ollama() {
    log_info "Installing Ollama..."
    if [[ "$OS" == "macos" ]] || [[ "$OS" == "linux" ]] || [[ "$OS" == "ubuntu" ]] || [[ "$OS" == "rhel" ]] || [[ "$OS" == "fedora" ]] || [[ "$OS" == "arch" ]]; then
        curl -fsSL https://ollama.ai/install.sh | sh
        log_success "Ollama installed successfully."
        
        log_info "Pulling Llama 3.2 7B model (this may take a while)..."
        ollama pull llama3.2:7b || log_warning "Failed to pull model. You can try 'ollama pull llama3.2:7b' manually later."
    else
        log_warning "Ollama installation not automated for your OS. Please install manually from https://ollama.ai"
    fi
}

# Function to clone repository
clone_repository() {
    local repo_url="https://github.com/rajputrajat1990/ai-code-editor.git"
    local target_dir="ai-code-editor"
    
    log_info "Cloning AI Code Editor repository..."
    
    # Remove existing directory if it exists
    if [ -d "$target_dir" ]; then
        log_warning "Directory $target_dir already exists. Removing..."
        rm -rf "$target_dir"
    fi
    
    git clone "$repo_url" "$target_dir"
    cd "$target_dir"
    log_success "Repository cloned successfully."
}

# Function to build project
build_project() {
    log_info "Building AI Code Editor..."
    mvn clean package -q
    log_success "Project built successfully."
}

# Function to run installation script
run_install_script() {
    log_info "Running platform-specific installation script..."
    
    case "$OS" in
        "ubuntu"|"rhel"|"fedora"|"arch"|"linux")
            if [ -f "scripts/install-linux.sh" ]; then
                chmod +x scripts/install-linux.sh
                ./scripts/install-linux.sh
            else
                log_error "Linux installation script not found."
                exit 1
            fi
            ;;
        "macos")
            if [ -f "scripts/install-macos.sh" ]; then
                chmod +x scripts/install-macos.sh
                ./scripts/install-macos.sh
            else
                log_error "macOS installation script not found."
                exit 1
            fi
            ;;
        "windows")
            if [ -f "scripts/install-windows.bat" ]; then
                cmd.exe /c scripts/install-windows.bat
            else
                log_error "Windows installation script not found."
                exit 1
            fi
            ;;
        *)
            log_error "No installation script available for your OS."
            exit 1
            ;;
    esac
}

# Function to display final instructions
show_final_instructions() {
    log_success "AI Code Editor installation completed!"
    echo ""
    echo "🎉 Next Steps:"
    echo "1. Start Ollama service: ollama serve"
    echo "2. Run the AI Code Editor with admin privileges:"
    case "$OS" in
        "ubuntu"|"rhel"|"fedora"|"arch"|"linux")
            echo "   sudo java --module-path /usr/share/openjfx/lib --add-modules javafx.controls,javafx.fxml -jar target/ai-code-editor-1.0.0.jar"
            ;;
        "macos")
            echo "   sudo java --module-path /opt/homebrew/lib --add-modules javafx.controls,javafx.fxml -jar target/ai-code-editor-1.0.0.jar"
            ;;
        "windows")
            echo "   java -jar target/ai-code-editor-1.0.0.jar"
            ;;
    esac
    echo ""
    echo "📖 Features available:"
    echo "   - AI-powered autonomous code generation"
    echo "   - Sandboxed code execution"
    echo "   - Multi-language support"
    echo "   - Automatic dependency management"
    echo "   - Web research for fact-checking"
    echo ""
    echo "⚠️  Note: Make sure Docker is running before using the application."
    echo "📁 Project location: $(pwd)"
}

# Main execution flow
main() {
    log_info "Starting AI Code Editor Bootstrap..."
    
    # Detect operating system
    OS=$(detect_os)
    log_info "Detected OS: $OS"
    
    # Check if we need admin privileges for system package installation
    if ! check_admin_privileges; then
        request_admin
    fi
    
    # Install Git if not available
    if ! command_exists git; then
        install_git
    else
        log_success "Git is already installed."
    fi
    
    # Install Java if not available
    if ! command_exists java; then
        install_java
    else
        log_success "Java is already installed."
    fi
    
    # Install Maven if not available
    if ! command_exists mvn; then
        install_maven
    else
        log_success "Maven is already installed."
    fi
    
    # Install Docker if not available
    if ! command_exists docker; then
        install_docker
    else
        log_success "Docker is already installed."
    fi
    
    # Install Ollama if not available
    if ! command_exists ollama; then
        install_ollama
    else
        log_success "Ollama is already installed."
    fi
    
    # Clone repository (if not already in the repo directory)
    if [ ! -f "pom.xml" ] || [ ! -d "src" ]; then
        clone_repository
    else
        log_info "Already in AI Code Editor repository directory."
    fi
    
    # Build the project
    build_project
    
    # Run platform-specific installation
    run_install_script
    
    # Show final instructions
    show_final_instructions
}

# Run main function
main "$@"
