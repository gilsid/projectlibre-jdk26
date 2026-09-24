@echo off
setlocal
set "PROJECTLIBRE_JAR=%~dp0projectlibre.jar"
if not exist "%PROJECTLIBRE_JAR%" (
    echo projectlibre.jar was not found next to this launcher.
    exit /b 1
)
if defined JAVA_HOME (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_EXE=java"
)
"%JAVA_EXE%" -Xms128m -Xmx768m -Dawt.useSystemAAFontSettings=on -Dswing.aatext=true -Dsun.java2d.xrender=true -Dsun.java2d.uiScale.enabled=true -jar "%PROJECTLIBRE_JAR%" %*
set "EXIT_CODE=%ERRORLEVEL%"
endlocal & exit /b %EXIT_CODE%
