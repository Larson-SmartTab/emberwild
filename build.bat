@echo off
rem Build emberwild.jar from source on Windows (needs JDK 21+).
setlocal
cd /d "%~dp0"
if exist build rmdir /s /q build
mkdir build\classes
rem List sources with quotes so folders with spaces in their names still work.
powershell -NoProfile -Command "Get-ChildItem -Recurse -Path src -Filter *.java | ForEach-Object { '\"' + ($_.FullName -replace '\\','/') + '\"' } | Set-Content -Encoding ASCII build\sources.txt" || goto :fail
javac --release 21 -encoding UTF-8 -Xlint:-unchecked -d build\classes @build\sources.txt || goto :fail
xcopy /e /i /q /y resources build\classes >nul
> build\manifest.txt echo Main-Class: emberwild.Main
jar --create --file emberwild.jar --manifest build\manifest.txt -C build\classes . || goto :fail
echo Built emberwild.jar - run it with: java -jar emberwild.jar
exit /b 0
:fail
echo Build failed. Is a JDK 21+ installed and on your PATH? Try: java -version
exit /b 1
