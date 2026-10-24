param(
    [Parameter(Mandatory = $true)][string]$StoreFile,
    [string]$Alias = 'loggym-release'
)
$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..')).TrimEnd('\')
$keyTarget = [IO.Path]::GetFullPath($StoreFile)
if (-not (Test-Path -LiteralPath $keyTarget -PathType Leaf)) { throw 'Keystore não encontrado.' }
if ($keyTarget.StartsWith($repoRoot + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'O keystore deve ficar fora do repositório.' }
$securePassword = Read-Host 'Senha da chave PKCS12 de release' -AsSecureString
$passwordPtr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
    $env:LOGGYM_RELEASE_STORE_FILE = $keyTarget
    $env:LOGGYM_RELEASE_KEY_ALIAS = $Alias
    $env:LOGGYM_RELEASE_STORE_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPtr)
    $env:LOGGYM_RELEASE_KEY_PASSWORD = $env:LOGGYM_RELEASE_STORE_PASSWORD
    Push-Location -LiteralPath $repoRoot
    try {
        & .\gradlew.bat assembleRelease --no-configuration-cache --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'Build de release falhou; consulte as mensagens anteriores.' }
    } finally { Pop-Location }
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPtr)
    $securePassword.Dispose()
    Remove-Item Env:LOGGYM_RELEASE_STORE_PASSWORD, Env:LOGGYM_RELEASE_KEY_PASSWORD -ErrorAction SilentlyContinue
}
