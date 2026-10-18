param(
    [Parameter(Mandatory=$true)][string]$SigningEnvelope,
    [string[]]$GradleTasks = @(':app:assembleRelease'),
    [string]$GradleExecutable = ''
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Security
$taskKeyDirectory = Split-Path ([IO.Path]::GetFullPath($SigningEnvelope))
$taskP12 = Join-Path $taskKeyDirectory 'MaxMusic-release.p12'
if (-not (Test-Path -LiteralPath $taskP12)) { throw 'Place the encrypted MaxMusic-release.p12 beside the signing envelope.' }
$taskBytes = [Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($SigningEnvelope), $null, [Security.Cryptography.DataProtectionScope]::CurrentUser)
try {
    $taskEnvelope = [Text.Encoding]::UTF8.GetString($taskBytes) | ConvertFrom-Json
    if ($taskEnvelope.format -ne 'MaxMusic signing envelope v1' -or $taskEnvelope.alias -ne 'maxmusic') { throw 'Unexpected signing envelope format.' }
    if ($taskEnvelope.keystore -cne [Convert]::ToBase64String([IO.File]::ReadAllBytes($taskP12))) { throw 'The encrypted keystore does not match the signing envelope.' }
    $env:MAXMUSIC_KEYSTORE = $taskP12
    $env:MAXMUSIC_STORE_PASSWORD = $taskEnvelope.password
    Push-Location (Split-Path $PSScriptRoot)
    try {
        if (-not $GradleExecutable) { $GradleExecutable = '.\gradlew.bat' }
        & $GradleExecutable --no-daemon @GradleTasks
        if ($LASTEXITCODE -ne 0) { throw 'Gradle release build failed.' }
    } finally { Pop-Location }
} finally {
    [Environment]::SetEnvironmentVariable('MAXMUSIC_KEYSTORE', $null, 'Process')
    [Environment]::SetEnvironmentVariable('MAXMUSIC_STORE_PASSWORD', $null, 'Process')
    [Array]::Clear($taskBytes, 0, $taskBytes.Length)
    $taskEnvelope = $null
}
