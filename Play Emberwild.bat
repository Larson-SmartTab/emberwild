@echo off
rem Double-click to host a game on Windows.
cd /d "%~dp0"
where java >nul 2>nul || (echo Java 21+ is required: https://adoptium.net/  & pause & exit /b 1)
java -jar emberwild.jar %*
pause
