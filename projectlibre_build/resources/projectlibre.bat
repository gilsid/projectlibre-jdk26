@echo off
setlocal
set "PROJECTLIBRE_JAR=%~dp0projectlibre.jar"
if not exist "%PROJECTLIBRE_JAR%" (
    echo projectlibre.jar was not found next to this launcher.
    exit /b 1
)
rem Strip surrounding quotes so a quoted JAVA_HOME still forms a valid path.
set "JAVA_HOME=%JAVA_HOME:"=%"
if defined JAVA_HOME (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_EXE=java"
)
if not exist "%JAVA_EXE%" (
    if defined JAVA_HOME (
        echo java.exe was not found under "%JAVA_HOME%\bin". Set JAVA_HOME to a valid JDK 26 installation.
        exit /b 1
    )
)
set "JAVA_MAJOR="
for /f "tokens=3" %%v in ('"%JAVA_EXE%" -version 2^>^&1 ^| findstr /i "version"') do (
    if not defined JAVA_MAJOR set "JAVA_MAJOR=%%~v"
)
set "JAVA_MAJOR=%JAVA_MAJOR:"=%"
for /f "delims=.+-_ tokens=1,2" %%a in ("%JAVA_MAJOR%") do (
    if "%%a"=="1" (
        set "JAVA_MAJOR=%%b"
    ) else (
        set "JAVA_MAJOR=%%a"
    )
)
if not defined JAVA_MAJOR (
    echo Unable to determine the Java version for "%JAVA_EXE%".
    exit /b 1
)
if %JAVA_MAJOR% LSS 26 (
    echo ProjectLibre requires Java 26 or later, but found Java %JAVA_MAJOR%.
    exit /b 1
)
"%JAVA_EXE%" -Xms128m -Xmx768m -Dawt.useSystemAAFontSettings=on -Dswing.aatext=true -Dsun.java2d.xrender=true -Dsun.java2d.uiScale.enabled=true -jar "%PROJECTLIBRE_JAR%" %*
set "EXIT_CODE=%ERRORLEVEL%"
endlocal & exit /b %EXIT_CODE%
