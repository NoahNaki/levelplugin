@echo off
setlocal

cd /d "%~dp0.."

call "scripts\deploy-config.bat"

echo Current folder:
cd

echo Building plugin...
call mvn -DskipTests package
if errorlevel 1 (
    echo.
    echo Build failed. Upload cancelled.
    pause
    exit /b 1
)

echo Uploading plugin...
"C:\Program Files (x86)\WinSCP\WinSCP.com" /command ^
  "open ftp://%FTP_USER%:%FTP_PASS%@%FTP_HOST%:%FTP_PORT%" ^
  "put ""%cd%\target\levelplugin.jar"" ""%REMOTE_PLUGIN_PATH%""" ^
  "exit"

if errorlevel 1 (
    echo.
    echo Upload failed. Reboot cancelled.
    pause
    exit /b 1
)

echo Rebooting server through DatHost...
node scripts\dathost-reboot.js
if errorlevel 1 (
    echo.
    echo DatHost reboot failed.
    pause
    exit /b 1
)

echo.
echo Deployment complete.
pause