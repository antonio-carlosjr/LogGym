param(
    [Parameter(Mandatory = $true)][string]$StoreFile,
    [string]$Alias = 'loggym-release'
)
$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..')).TrimEnd('\')
$keyTarget = [IO.Path]::GetFullPath($StoreFile)
if ($keyTarget.Equals($repoRoot, [StringComparison]::OrdinalIgnoreCase) -or
    $keyTarget.StartsWith($repoRoot + '\', [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Escolha um caminho fora do repositório para o keystore.'
}
if (Test-Path -LiteralPath $keyTarget) { throw 'O arquivo já existe. Reutilize a chave existente; não a substitua.' }
. (Join-Path $PSScriptRoot 'ReleaseKeyTool.ps1')
$keytool = Get-ReleaseKeyTool
$securePassword = Read-Host 'Senha da primeira chave de release (mínimo 12 caracteres; guarde-a no gerenciador de senhas da equipe)' -AsSecureString
$confirmation = Read-Host 'Confirme a senha' -AsSecureString
$firstPtr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
$secondPtr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($confirmation)
try {
    $first = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($firstPtr)
    $second = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($secondPtr)
    if ($first.Length -lt 12 -or $first -cne $second) { throw 'Senhas diferentes ou menores que 12 caracteres.' }
    New-Item -ItemType Directory -Path ([IO.Path]::GetDirectoryName($keyTarget)) -Force | Out-Null
    $env:LOGGYM_RELEASE_STORE_FILE = $keyTarget
    $env:LOGGYM_RELEASE_KEY_ALIAS = $Alias
    $env:LOGGYM_RELEASE_STORE_PASSWORD = $first
    $env:LOGGYM_RELEASE_KEY_PASSWORD = $first
    & $keytool -genkeypair -storetype PKCS12 -keystore $keyTarget -alias $Alias -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=LogGym' -storepass:env LOGGYM_RELEASE_STORE_PASSWORD -keypass:env LOGGYM_RELEASE_KEY_PASSWORD
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível criar a chave. Verifique o resultado do keytool antes de repetir.' }
    Write-Host 'Chave criada. Faça backup protegido do keystore e registre a senha sob custódia da equipe.'
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($firstPtr)
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($secondPtr)
    $first = $null
    $second = $null
    $securePassword.Dispose()
    $confirmation.Dispose()
    Remove-Item Env:LOGGYM_RELEASE_STORE_PASSWORD, Env:LOGGYM_RELEASE_KEY_PASSWORD -ErrorAction SilentlyContinue
}
