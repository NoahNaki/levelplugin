@echo off
setlocal

REM Move from scripts folder to repo root
cd /d "%~dp0.."

set BASE_BRANCH=main
set OUTPUT_ZIP=%CD%\target\pr-files.zip
set TEMP_PS1=%CD%\target\export-pr-files-temp.ps1

echo.
echo Repository Root:
echo %CD%
echo.
echo Exporting changed files compared to %BASE_BRANCH%...
echo.

if not exist "target" mkdir "target"

> "%TEMP_PS1%" echo $base = "%BASE_BRANCH%"
>> "%TEMP_PS1%" echo $zip = "%OUTPUT_ZIP%"
>> "%TEMP_PS1%" echo $files = git diff --name-only --diff-filter=AMR "$base...HEAD"
>> "%TEMP_PS1%" echo if (-not $files) { Write-Host "No changed files found."; exit 0 }
>> "%TEMP_PS1%" echo if (Test-Path $zip) { Remove-Item $zip -Force }
>> "%TEMP_PS1%" echo Add-Type -AssemblyName System.IO.Compression.FileSystem
>> "%TEMP_PS1%" echo $archive = [System.IO.Compression.ZipFile]::Open($zip, "Create")
>> "%TEMP_PS1%" echo try {
>> "%TEMP_PS1%" echo     foreach ($file in $files) {
>> "%TEMP_PS1%" echo         if (Test-Path $file -PathType Leaf) {
>> "%TEMP_PS1%" echo             Write-Host "Adding $file"
>> "%TEMP_PS1%" echo             $entryName = $file -replace "\\", "/"
>> "%TEMP_PS1%" echo             [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, (Resolve-Path $file), $entryName) ^| Out-Null
>> "%TEMP_PS1%" echo         }
>> "%TEMP_PS1%" echo     }
>> "%TEMP_PS1%" echo }
>> "%TEMP_PS1%" echo finally {
>> "%TEMP_PS1%" echo     $archive.Dispose()
>> "%TEMP_PS1%" echo }
>> "%TEMP_PS1%" echo Write-Host ""
>> "%TEMP_PS1%" echo Write-Host "Created ZIP:"
>> "%TEMP_PS1%" echo Write-Host $zip

powershell -NoProfile -ExecutionPolicy Bypass -File "%TEMP_PS1%"

echo.
if exist "%OUTPUT_ZIP%" (
    explorer /select,"%OUTPUT_ZIP%"
) else (
    echo ERROR: ZIP was not created.
)

pause