# Finds JDK under Program Files\Java if JAVA_HOME is missing or invalid, then runs Gradle.
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GradleArgs
)

$ErrorActionPreference = 'Stop'

function Test-JavaHome([string] $javaHome) {
    return $javaHome -and (Test-Path (Join-Path $javaHome 'bin\java.exe'))
}

if (-not (Test-JavaHome $env:JAVA_HOME)) {
    $base = 'C:\Program Files\Java'
    if (Test-Path $base) {
        $jdk = Get-ChildItem $base -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match '^jdk-' } |
            Sort-Object Name -Descending |
            Select-Object -First 1
        if ($jdk -and (Test-JavaHome $jdk.FullName)) {
            $env:JAVA_HOME = $jdk.FullName
        }
    }
}

if (-not (Test-JavaHome $env:JAVA_HOME)) {
    Write-Host 'ERROR: Could not find JDK.' -ForegroundColor Red
    Write-Host 'Install JDK 11+ or set JAVA_HOME to the folder that contains bin\java.exe (e.g. C:\Program Files\Java\jdk-21.0.10).' -ForegroundColor Yellow
    Write-Host 'If you just installed Java, restart Cursor so the terminal inherits PATH.' -ForegroundColor Yellow
    exit 1
}

$env:Path = "$(Join-Path $env:JAVA_HOME 'bin');$env:Path"
Write-Host "Using JAVA_HOME=$($env:JAVA_HOME)" -ForegroundColor DarkGray

if (-not $GradleArgs -or $GradleArgs.Count -eq 0) {
    $GradleArgs = @('run')
}

& "$PSScriptRoot\gradlew.bat" @GradleArgs
