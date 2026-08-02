@echo off
setlocal

set PROJECT_ROOT=%~dp0..
set JAR_FILE=%PROJECT_ROOT%\target\tiny-light-0.0.1-SNAPSHOT.jar
set CF_EXE=%PROJECT_ROOT%\deploy\cloudflared.exe

echo ========================================
echo  tiny-light start
echo ========================================
echo.

REM --- Check JAR ---
if not exist "%JAR_FILE%" (
    echo [ERROR] JAR not found, please run deploy\build.bat first
    pause
    exit /b 1
)

REM --- Check cloudflared ---
if not exist "%CF_EXE%" (
    echo [ERROR] cloudflared.exe not found
    echo Download: https://github.com/cloudflare/cloudflared/releases/latest
    echo Get cloudflared-windows-amd64.exe, rename to cloudflared.exe
    echo Put it in deploy\ folder
    pause
    exit /b 1
)

REM --- Check MySQL ---
echo [1/3] Check MySQL...
sc query MySQL >nul 2>&1
if errorlevel 1 (
    sc query MySQL80 >nul 2>&1
    if errorlevel 1 (
        echo [ERROR] MySQL service not found
        pause
        exit /b 1
    )
)
echo     MySQL OK
echo.

REM --- Start JAR (background) ---
echo [2/3] Start Spring Boot...
cd /d "%PROJECT_ROOT%"
start "tiny-light-server" /min java -jar "%JAR_FILE%"
echo     JAR started (port 8080)
echo.

REM --- Wait for Spring Boot ---
echo     Waiting for service...
:WAIT_LOOP
timeout /t 2 /nobreak >nul
curl -s http://localhost:8080/api/health >nul 2>&1
if errorlevel 1 (
    goto WAIT_LOOP
)
echo     Service ready
echo.

REM --- Start cloudflared tunnel (foreground) ---
echo [3/3] Start Cloudflare Tunnel...
echo.
echo ========================================
echo  Service started!
echo.
echo  Local:  http://localhost:8080
echo  Public: see URL below (trycloudflare.com)
echo.
echo  Close this window to stop all services
echo ========================================
echo.

"%CF_EXE%" tunnel --url http://localhost:8080

REM --- After cloudflared exits, kill JAR ---
echo.
echo Stopping Spring Boot...
taskkill /FI "WINDOWTITLE eq tiny-light-server*" /F >nul 2>&1
echo Stopped.

endlocal
