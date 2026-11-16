param([ValidateSet('build','runClient','runServer','runGameTestServer')][string]$Task = 'build')
$ErrorActionPreference = 'Stop'
$ccdOldOptions = $env:JAVA_TOOL_OPTIONS
$ccdOldJava = $env:JAVA_HOME
try {
    if (-not $env:JAVA_HOME -and (Test-Path -LiteralPath 'C:/Program Files/Java/jdk-21')) {
        $env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
    }
    # Respect Windows' configured proxy, without modifying global Gradle or Java settings.
    $ccdUri = [uri]'https://maven.neoforged.net/releases/'
    $ccdProxy = [System.Net.WebRequest]::DefaultWebProxy.GetProxy($ccdUri)
    if ($ccdProxy -and $ccdProxy.Host -ne $ccdUri.Host) {
        $env:JAVA_TOOL_OPTIONS = "$ccdOldOptions -Dhttp.proxyHost=$($ccdProxy.Host) -Dhttp.proxyPort=$($ccdProxy.Port) -Dhttps.proxyHost=$($ccdProxy.Host) -Dhttps.proxyPort=$($ccdProxy.Port) -Dhttp.nonProxyHosts=localhost|127.*"
    }
    Push-Location $PSScriptRoot
    try {
        & .\gradlew.bat $Task --console=plain
        if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
    } finally { Pop-Location }
} finally {
    $env:JAVA_TOOL_OPTIONS = $ccdOldOptions
    $env:JAVA_HOME = $ccdOldJava
}
