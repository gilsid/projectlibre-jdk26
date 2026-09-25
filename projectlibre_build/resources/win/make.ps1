$AppVersion = "@version@"
$OutputDir = "app"

$JdkHome = $env:JAVA_HOME
if ([string]::IsNullOrWhiteSpace($JdkHome)) {
    $JdkHome = "C:\Program Files\Java\jdk-26"
}

$JavaPath = Join-Path $JdkHome "bin\java.exe"
$JpackagePath = Join-Path $JdkHome "bin\jpackage.exe"

if (-not (Test-Path $JavaPath -PathType Leaf)) {
    Write-Error "java not found. Set JAVA_HOME to a valid JDK 26 installation."
    exit 1
}

if (-not (Test-Path $JpackagePath -PathType Leaf)) {
    Write-Error "jpackage not found. Make sure JAVA_HOME is set to a valid JDK 26 installation."
    exit 1
}

$JavaVersionOutput = (& $JavaPath -version 2>&1 | Out-String)
$JavaVersionMatch = [regex]::Match($JavaVersionOutput, 'version "(?:1\.)?(\d+)')
if (-not $JavaVersionMatch.Success -or [int]$JavaVersionMatch.Groups[1].Value -lt 26) {
    Write-Error "ProjectLibre requires Java 26. JAVA_HOME points to an unsupported runtime."
    exit 1
}

# --- Create Output Directory ---
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

& $JpackagePath `
    --type msi `
    --name ProjectLibre `
    --app-version $AppVersion `
    --input source `
    --main-jar projectlibre-$AppVersion.jar `
    --icon source/projectlibre.ico `
    --license-file source/license/license.txt `
    --file-associations "pod.properties" `
    --file-associations "mpp.properties" `
    --file-associations "xml.properties" `
    --dest $OutputDir `
    --win-menu `
    --win-shortcut `
    --win-dir-chooser `
    --verbose

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host "MSI installer created in '$OutputDir'" -ForegroundColor Green