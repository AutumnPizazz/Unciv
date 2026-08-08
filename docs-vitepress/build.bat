@echo off
rem ============================================================
rem  UncivCN docs site: one-click build + local preview
rem  Pipeline: Kotlin docs -> npm deps -> VitePress build -> preview
rem  Usage: double-click this file, or run build.bat in a terminal
rem ============================================================
setlocal
chcp 65001 >nul
cd /d "%~dp0"

echo.
echo ============================================
echo   [1/4] Generating Kotlin docs (uniques etc.)
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
echo Browser will open automatically. Close the preview window to stop.
echo.
start "UncivDocsPreview" /min cmd /c "cd /d %~dp0 && call npm run docs:preview -- --port 4173 --strictPort"
ping -n 4 127.0.0.1 >nul
start "" "http://localhost:4173/Unciv/"
endlocal
