@echo off
setlocal

set PROJECT_ROOT=%~dp0..

echo ========================================
echo  tiny-light update
echo ========================================
echo.

echo [1/4] Pull latest code...
cd /d "%PROJECT_ROOT%"
git pull origin main
if errorlevel 1 (
    echo [ERROR] git pull failed
    pause
    exit /b 1
)
echo.

echo [2/4] Update frontend dependencies...
cd /d "%PROJECT_ROOT%\tiny-light-frontend"
call npm install
if errorlevel 1 (
    echo [ERROR] npm install failed
    pause
    exit /b 1
)
echo.

echo [3/4] Rebuild...
call "%PROJECT_ROOT%\deploy\build.bat"
if errorlevel 1 (
    echo [ERROR] Build failed
    pause
    exit /b 1
)
echo.

echo [4/4] Done!
echo.
echo ========================================
echo  Update complete, run deploy\start.bat
echo ========================================

endlocal
