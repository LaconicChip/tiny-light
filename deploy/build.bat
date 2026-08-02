@echo off
setlocal

echo ========================================
echo  tiny-light build
echo ========================================
echo.

set PROJECT_ROOT=%~dp0..
set FRONTEND_DIR=%PROJECT_ROOT%\tiny-light-frontend
set STATIC_DIR=%PROJECT_ROOT%\src\main\resources\static

echo [1/4] Clean old static files...
if exist "%STATIC_DIR%" rmdir /s /q "%STATIC_DIR%"
mkdir "%STATIC_DIR%"
echo     done
echo.

echo [2/4] Build frontend...
cd /d "%FRONTEND_DIR%"
call npm run build
if errorlevel 1 (
    echo [ERROR] Frontend build failed
    pause
    exit /b 1
)
echo.

echo [3/4] Copy frontend dist to Spring Boot static...
xcopy /E /I /Y "%FRONTEND_DIR%\dist\*" "%STATIC_DIR%\"
echo     done
echo.

echo [4/4] Package Spring Boot JAR...
cd /d "%PROJECT_ROOT%"
call mvnw clean package -DskipTests
if errorlevel 1 (
    echo [ERROR] Maven package failed
    pause
    exit /b 1
)
echo.
echo ========================================
echo  Build complete!
echo  JAR: target\tiny-light-0.0.1-SNAPSHOT.jar
echo ========================================

endlocal
