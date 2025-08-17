@echo off
REM AI Code Editor Installation Script for Windows
REM Comprehensive installation for systems with no prerequisites

echo ==============================================
echo   AI Code Editor Installation Script
echo   Installing ALL prerequisites from scratch
echo ==============================================
echo.

REM Check if running as administrator
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo ❌ This script must be run as Administrator.
    echo    Right-click on the script and select "Run as administrator"
    echo.
    pause
    exit /b 1
)

echo 📋 Detected system: Windows
echo.

REM Create temporary directory for downloads
set TEMP_DIR=%TEMP%\ai-code-editor-install
if not exist "%TEMP_DIR%" mkdir "%TEMP_DIR%"
cd /d "%TEMP_DIR%"

REM Check and install Java 21
echo ☕ Checking Java installation...
java -version >nul 2>&1
if %errorLevel% neq 0 (
    echo 📦 Java not found. Installing Eclipse Temurin JDK 21...
    echo    Downloading Java 21 installer...
    powershell -Command "Invoke-WebRequest -Uri 'https://github.com/adoptium/temurin21-binaries/releases/latest/download/OpenJDK21U-jdk_x64_windows_hotspot.msi' -OutFile 'temurin-21-jdk.msi'"
    echo    Installing Java 21...
    msiexec /i temurin-21-jdk.msi /quiet /norestart ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJarFileRunWith,FeatureJavaHome
    if %errorLevel% neq 0 (
        echo ❌ Java installation failed. Please install manually from https://adoptium.net/
        pause
        exit /b 1
    )
    echo ✅ Java 21 installed successfully
    REM Refresh environment variables
    call refreshenv
) else (
    REM Check Java version
    for /f tokens^=3 %%i in ('java -version 2^>^&1 ^| findstr /i version') do set JAVA_VER=%%i
    set JAVA_VER=%JAVA_VER:"=%
    for /f "delims=." tokens=1 %%i in ("%JAVA_VER%") do set JAVA_MAJOR=%%i
    if %JAVA_MAJOR% LSS 21 (
        echo ⚠️  Java %JAVA_VER% found, but Java 21+ is required
        echo 📦 Installing Java 21...
        powershell -Command "Invoke-WebRequest -Uri 'https://github.com/adoptium/temurin21-binaries/releases/latest/download/OpenJDK21U-jdk_x64_windows_hotspot.msi' -OutFile 'temurin-21-jdk.msi'"
        msiexec /i temurin-21-jdk.msi /quiet /norestart ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJarFileRunWith,FeatureJavaHome
        call refreshenv
    ) else (
        echo ✅ Java %JAVA_VER% is compatible
    )
)

REM Check and install JavaFX
echo 🎨 Checking JavaFX installation...
set JAVAFX_INSTALLED=0
if exist "%ProgramFiles%\Java\*javafx*" set JAVAFX_INSTALLED=1
if exist "%ProgramFiles(x86)%\Java\*javafx*" set JAVAFX_INSTALLED=1

if %JAVAFX_INSTALLED% equ 0 (
    echo 📦 JavaFX not found. Installing OpenJFX...
    echo    Downloading OpenJFX...
    set JAVAFX_VERSION=21.0.1
    powershell -Command "Invoke-WebRequest -Uri 'https://download2.gluonhq.com/openjfx/%JAVAFX_VERSION%/openjfx-%JAVAFX_VERSION%_windows-x64_bin-sdk.zip' -OutFile 'openjfx.zip'"
    
    echo    Extracting OpenJFX...
    powershell -Command "Expand-Archive -Path 'openjfx.zip' -DestinationPath 'C:\Program Files\Java\' -Force"
    
    REM Set JavaFX environment variable
    for /d %%d in ("C:\Program Files\Java\javafx-sdk-*") do (
        setx JAVAFX_PATH "%%d\lib" /M
        echo ✅ JavaFX installed at: %%d\lib
    )
    
    REM Cleanup
    del openjfx.zip
) else (
    echo ✅ JavaFX already installed
)

REM Check and install Docker
echo 🐳 Checking Docker installation...
docker --version >nul 2>&1
if %errorLevel% neq 0 (
    echo 📦 Docker not found. Installing Docker Desktop...
    echo    Downloading Docker Desktop installer...
    powershell -Command "Invoke-WebRequest -Uri 'https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe' -OutFile 'DockerDesktopInstaller.exe'"
    echo    Installing Docker Desktop...
    echo    ⚠️  This may take several minutes and require a system restart
    start /wait DockerDesktopInstaller.exe install --quiet --accept-license
    if %errorLevel% neq 0 (
        echo ❌ Docker installation may have failed
        echo    Please install manually from https://www.docker.com/products/docker-desktop
        echo    and restart this script
        pause
        exit /b 1
    )
    echo ✅ Docker Desktop installed
    echo ⚠️  Please start Docker Desktop manually and restart this script
    pause
    exit /b 0
) else (
    echo ✅ Docker is already installed
    REM Check if Docker daemon is running
    docker info >nul 2>&1
    if %errorLevel% neq 0 (
        echo ⚠️  Docker daemon not running. Please start Docker Desktop
        pause
    )
)

REM Check and install Maven
echo 🏗️  Checking Maven installation...
mvn --version >nul 2>&1
if %errorLevel% neq 0 (
    echo 📦 Maven not found. Installing Apache Maven...
    echo    Downloading Maven...
    set MAVEN_VERSION=3.9.5
    powershell -Command "Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile 'maven.zip'"
    echo    Extracting Maven...
    powershell -Command "Expand-Archive -Path 'maven.zip' -DestinationPath 'C:\Program Files\'"
    ren "C:\Program Files\apache-maven-%MAVEN_VERSION%" "Apache-Maven"
    
    REM Add Maven to PATH
    setx /M PATH "%PATH%;C:\Program Files\Apache-Maven\bin"
    set PATH=%PATH%;C:\Program Files\Apache-Maven\bin
    
    REM Set MAVEN_HOME
    setx /M MAVEN_HOME "C:\Program Files\Apache-Maven"
    
    echo ✅ Maven installed successfully
) else (
    echo ✅ Maven is already installed
)
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
