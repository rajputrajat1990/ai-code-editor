#!/bin/bash

# AI Code Editor Launcher with JavaFX Support
# This script handles JavaFX module path configuration automatically

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_PATH="$SCRIPT_DIR/target/ai-code-editor-1.0.0.jar"

echo "🚀 AI Code Editor - Enhanced with Permanent Memory"
echo "================================================="

# Check if JAR exists
if [ ! -f "$JAR_PATH" ]; then
    echo "❌ AI Code Editor JAR not found at: $JAR_PATH"
    echo "   Please build the project first: mvn clean package"
    exit 1
fi

# Detect JavaFX installation
JAVAFX_PATHS=(
    "/usr/share/openjfx/lib"
    "/usr/lib/jvm/java-21-openjdk/lib"
    "/opt/homebrew/lib"
    "$(find /usr -name "javafx*.jar" 2>/dev/null | head -1 | xargs dirname 2>/dev/null)"
    "$JAVAFX_PATH"
)

JAVAFX_MODULE_PATH=""
for path in "${JAVAFX_PATHS[@]}"; do
    if [ -n "$path" ] && [ -d "$path" ] && ls "$path"/*javafx*.jar >/dev/null 2>&1; then
        JAVAFX_MODULE_PATH="$path"
        echo "✅ JavaFX found at: $JAVAFX_MODULE_PATH"
        break
    fi
done

if [ -z "$JAVAFX_MODULE_PATH" ]; then
    echo "⚠️  JavaFX not found in standard locations"
    echo "   Trying to run without explicit module path..."
    JAVA_ARGS=""
else
    JAVA_ARGS="--module-path $JAVAFX_MODULE_PATH --add-modules javafx.controls,javafx.fxml,javafx.web"
fi

# Check if running as root (recommended for system operations)
if [ "$EUID" -ne 0 ]; then
    echo "⚠️  Running as regular user. Some features may require sudo."
    echo "   For full functionality, run: sudo ./launch.sh"
fi

# Launch the application
echo "🎯 Starting AI Code Editor with Enhanced Intelligence..."
echo ""

java $JAVA_ARGS -jar "$JAR_PATH" "$@"
