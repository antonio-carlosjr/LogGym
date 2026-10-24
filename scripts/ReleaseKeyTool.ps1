# Carregado pelos scripts de release; não lê ou exibe senhas.
function Get-ReleaseKeyTool {
    $available = Get-Command keytool -ErrorAction SilentlyContinue
    if ($available) { return $available.Source }
    $javaSettings = & java -XshowSettings:properties -version 2>&1
    $javaHomeLine = $javaSettings | ForEach-Object { "$_" } | Where-Object { $_ -match '^\s*java.home\s*=' } | Select-Object -First 1
    if ($javaHomeLine) {
        $javaDirectory = ($javaHomeLine -split '=', 2)[1].Trim()
        $candidate = Join-Path $javaDirectory 'bin\keytool.exe'
        if (Test-Path -LiteralPath $candidate) { return $candidate }
    }
    throw 'Instale um JDK e disponibilize java/keytool no PATH.'
}
