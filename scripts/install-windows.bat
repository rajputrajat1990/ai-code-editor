@echo off
REM AI Code Editor Installation Script for Windows

echo Installing AI Code Editor...

REM Check if running as administrator
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo This script must be run as Administrator.
    echo Right-click on the script and select "Run as administrator"
    pause
    exit /b 1
)

REM Check if Java is installed
java -version >nul 2>&1
if %errorLevel% neq 0 (
    echo Java is not installed. Please install Java 11 or higher.
    echo Download from: https://adoptium.net/
    pause
    exit /b 1
)

REM Check if Docker is installed
docker --version >nul 2>&1
if %errorLevel% neq 0 (
    echo Docker is not installed. Please install Docker Desktop.
    echo Download from: https://www.docker.com/products/docker-desktop
    pause
    exit /b 1
)

REM Check if Maven is installed
mvn --version >nul 2>&1
if %errorLevel% neq 0 (
    echo Maven is not installed. Please install Maven.
    echo Download from: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

REM Check if Ollama is installed
ollama --version >nul 2>&1
if %errorLevel% neq 0 (
    echo Installing Ollama...
    powershell -Command "Invoke-WebRequest -Uri https://ollama.ai/download/windows -OutFile ollama-installer.exe"
    ollama-installer.exe
    del ollama-installer.exe
    echo Please restart this script after Ollama installation completes.
    pause
    exit /b 0
)

REM Build the application
echo Building AI Code Editor...
if not exist pom.xml (
    echo pom.xml not found. Please run this script from the project root directory.
    pause
    exit /b 1
)

mvn clean package -DskipTests

REM Create application directory
set APP_DIR=C:\Program Files\AI Code Editor
mkdir "%APP_DIR%" 2>nul

REM Copy JAR file
copy target\ai-code-editor-*.jar "%APP_DIR%\ai-code-editor.jar"

REM Create launch script
echo @echo off > "%APP_DIR%\launch.bat"
echo cd /d "%APP_DIR%" >> "%APP_DIR%\launch.bat"
echo java -jar ai-code-editor.jar %%* >> "%APP_DIR%\launch.bat"

REM Create desktop shortcut
set DESKTOP=%USERPROFILE%\Desktop
echo Set oWS = WScript.CreateObject("WScript.Shell") > "%DESKTOP%\AI Code Editor.vbs"
echo sLinkFile = "%DESKTOP%\AI Code Editor.lnk" >> "%DESKTOP%\AI Code Editor.vbs"
echo Set oLink = oWS.CreateShortcut(sLinkFile) >> "%DESKTOP%\AI Code Editor.vbs"
echo oLink.TargetPath = "%APP_DIR%\launch.bat" >> "%DESKTOP%\AI Code Editor.vbs"
echo oLink.WorkingDirectory = "%APP_DIR%" >> "%DESKTOP%\AI Code Editor.vbs"
echo oLink.Description = "AI Code Editor" >> "%DESKTOP%\AI Code Editor.vbs"
echo oLink.Save >> "%DESKTOP%\AI Code Editor.vbs"
cscript "%DESKTOP%\AI Code Editor.vbs"
del "%DESKTOP%\AI Code Editor.vbs"

REM Add to PATH
setx PATH "%PATH%;%APP_DIR%" /M

echo.
echo ✅ AI Code Editor installation completed!
echo.
echo Usage:
echo   1. From command line: "%APP_DIR%\launch.bat"
echo   2. From desktop shortcut: AI Code Editor
echo.
echo Prerequisites check:
java -version 2>&1 | findstr "version"
docker --version 2>nul || echo Docker: Not found
ollama --version 2>nul || echo Ollama: Not found
mvn --version 2>&1 | findstr "Apache Maven"
echo.
echo Note: The application requires administrator privileges to run.
echo.
pause
