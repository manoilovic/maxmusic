param(
    [string]$SigningEnvelope = (Join-Path $PSScriptRoot 'MaxMusic-signing-key.dpapi'),
    [Parameter(Mandatory=$true)][string]$Destination
)
$ErrorActionPreference = 'Stop'
if (Test-Path -LiteralPath $Destination) { throw 'Destination already exists; refusing to replace it.' }
Add-Type -AssemblyName System.Security
Add-Type -AssemblyName System.Security.Cryptography.Pkcs
$taskBytes = [Security.Cryptography.ProtectedData]::Unprotect([IO.File]::ReadAllBytes($SigningEnvelope), $null, [Security.Cryptography.DataProtectionScope]::CurrentUser)
$taskCertificate = $null
$taskPassword = $null
$taskConfirm = $null
$taskRsa = $null
try {
    $taskEnvelope = [Text.Encoding]::UTF8.GetString($taskBytes) | ConvertFrom-Json
    if ($taskEnvelope.format -ne 'MaxMusic signing envelope v1') { throw 'Unexpected signing envelope format.' }
    $taskPassword = Read-Host 'New backup password (store it separately in your password manager)' -AsSecureString
    $taskConfirm = Read-Host 'Repeat new backup password' -AsSecureString
    $taskPasswordText = [Net.NetworkCredential]::new('', $taskPassword).Password
    $taskConfirmText = [Net.NetworkCredential]::new('', $taskConfirm).Password
    if ($taskPasswordText.Length -lt 16 -or $taskPasswordText -cne $taskConfirmText) { throw 'Passwords must match and contain at least 16 characters.' }
    $taskFlags = [Security.Cryptography.X509Certificates.X509KeyStorageFlags]::EphemeralKeySet -bor [Security.Cryptography.X509Certificates.X509KeyStorageFlags]::Exportable
    $taskCertificate = [Security.Cryptography.X509Certificates.X509Certificate2]::new([Convert]::FromBase64String($taskEnvelope.keystore), [string]$taskEnvelope.password, $taskFlags)
    if (-not $taskCertificate.HasPrivateKey) { throw 'Private key is missing.' }
    # Standard .NET PKCS12 APIs preserve the Java alias and link the cert to the key.
    $taskPbe = [Security.Cryptography.PbeParameters]::new([Security.Cryptography.PbeEncryptionAlgorithm]::Aes256Cbc, [Security.Cryptography.HashAlgorithmName]::SHA256, 600000)
    $taskRsa = [Security.Cryptography.X509Certificates.RSACertificateExtensions]::GetRSAPrivateKey($taskCertificate)
    $taskKeyContents = [Security.Cryptography.Pkcs.Pkcs12SafeContents]::new()
    $taskCertContents = [Security.Cryptography.Pkcs.Pkcs12SafeContents]::new()
    $taskKeyBag = $taskKeyContents.AddShroudedKey($taskRsa, [string]$taskPasswordText, $taskPbe)
    $taskCertBag = $taskCertContents.AddCertificate($taskCertificate)
    $taskNameWriter = [System.Formats.Asn1.AsnWriter]::new([System.Formats.Asn1.AsnEncodingRules]::DER)
    $taskNameWriter.WriteCharacterString([System.Formats.Asn1.UniversalTagNumber]::BMPString, 'maxmusic', $null)
    $taskLocalId = [Security.Cryptography.SHA1]::HashData($taskCertificate.RawData)
    foreach ($taskBag in @($taskKeyBag, $taskCertBag)) {
        $taskBag.Attributes.Add([Security.Cryptography.AsnEncodedData]::new([Security.Cryptography.Oid]::new('1.2.840.113549.1.9.20'), $taskNameWriter.Encode())) | Out-Null
        $taskBag.Attributes.Add([Security.Cryptography.Pkcs.Pkcs9LocalKeyId]::new($taskLocalId)) | Out-Null
    }
    $taskBuilder = [Security.Cryptography.Pkcs.Pkcs12Builder]::new()
    $taskBuilder.AddSafeContentsUnencrypted($taskKeyContents)
    $taskBuilder.AddSafeContentsEncrypted($taskCertContents, [string]$taskPasswordText, $taskPbe)
    $taskBuilder.SealWithMac([string]$taskPasswordText, [Security.Cryptography.HashAlgorithmName]::SHA256, 600000)
    $taskExport = $taskBuilder.Encode()
    $taskVerify = [Security.Cryptography.X509Certificates.X509Certificate2]::new($taskExport, $taskPasswordText, $taskFlags)
    try {
        if (-not $taskVerify.HasPrivateKey -or $taskVerify.Thumbprint -ne $taskCertificate.Thumbprint) { throw 'Portable backup recovery verification failed.' }
    } finally { $taskVerify.Dispose() }
    [IO.File]::WriteAllBytes([IO.Path]::GetFullPath($Destination), $taskExport)
    Write-Output 'Portable encrypted backup created and recovery verified. Its password was not saved.'
} finally {
    if ($taskRsa) { $taskRsa.Dispose() }
    if ($taskCertificate) { $taskCertificate.Dispose() }
    if ($taskPassword) { $taskPassword.Dispose() }
    if ($taskConfirm) { $taskConfirm.Dispose() }
    [Array]::Clear($taskBytes, 0, $taskBytes.Length)
    $taskEnvelope = $null
    $taskPasswordText = $null
    $taskConfirmText = $null
}
