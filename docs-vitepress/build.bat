@echo off
rem ============================================================
rem  UncivCN docs site: one-click build + local preview
rem  Pipeline: Kotlin docs -> npm deps -> VitePress build -> preview
rem  Usage: double-click this file, or run build.bat in a terminal
rem  If a preview server is already running, you can choose to
rem  open it, rebuild+restart it, or exit.
rem ============================================================
setlocal
chcp 65001 >nul
cd /d "%~dp0"

rem ---- Detect whether a preview server is already running ----
set SERVER_RUNNING=0
curl -s -o nul --max-time 2 http://127.0.0.1:4173/Unciv/ 2>nul
if not errorlevel 1 set SERVER_RUNNING=1

if "%SERVER_RUNNING%"=="1" (
    echo.
    echo ============================================
    echo   预览服务器正在运行: http://localhost:4173/Unciv/
    echo ============================================
    echo.
    echo   [1] 打开现有预览（不重新构建）
    echo   [2] 重新构建并重启预览
    echo   [3] 退出
    echo.
    choice /c 123 /n /m "请选择 (1/2/3): "
    if errorlevel 3 (
        echo 已退出。
        endlocal
        exit /b 0
    )
    if errorlevel 2 goto :rebuild
    rem ---- Option 1: just open the browser ----
    start "" "http://localhost:4173/Unciv/"
    endlocal
    exit /b 0
)

echo 未检测到运行中的预览服务器，开始完整构建。
echo.

:rebuild
rem ---- If rebuilding while a server is running, stop it first ----
if "%SERVER_RUNNING%"=="1" (
    echo 正在停止现有预览服务器...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":4173" ^| findstr "LISTENING"') do (
        taskkill /F /PID %%a >nul 2>&1
    )
    ping -n 2 127.0.0.1 >nul
    echo 已停止。
    echo.
)

echo ============================================
echo   [1/4] Generating Kotlin docs (uniques, Lua API reference etc.)
echo ============================================
cd /d "%~dp0\.."
call gradlew.bat desktop:generateDocs --no-daemon -q
if errorlevel 1 (
    echo [FAILED] Kotlin doc generation failed. See output above.
    pause
    exit /b 1
)
echo [OK] Kotlin docs updated

echo.
echo ============================================
echo   [2/4] Checking npm dependencies
echo ============================================
cd /d "%~dp0"
if not exist "node_modules" (
    echo First run, installing dependencies...
    call npm install --no-fund --no-audit
    if errorlevel 1 (
        echo [FAILED] npm install failed. See output above.
        pause
        exit /b 1
    )
) else (
    echo Dependencies already present, skipping install
)

echo.
echo ============================================
echo   [3/4] Building site (VitePress)
echo ============================================
call npm run docs:build
if errorlevel 1 (
    echo [FAILED] Build failed. See output above.
    pause
    exit /b 1
)
echo [OK] Build succeeded, output in .vitepress/dist

echo.
echo ============================================
echo   [4/4] Starting local preview server
echo ============================================
echo Preview URL: http://localhost:4173/Unciv/
echo Browser will open automatically.
echo The server exits automatically after 5 minutes without visits,
echo or you can close its window / press Ctrl+C anytime.
echo.
start "UncivCN Docs Preview (auto-exits when idle)" /min cmd /c "cd /d %~dp0 && node scripts\preview-server.mjs"
ping -n 4 127.0.0.1 >nul
start "" "http://localhost:4173/Unciv/"
endlocal
