# APK assinado e custódia da chave

A versão inicial é `0.1.0` (`versionCode 1`). A chave definitiva ainda depende de criação e guarda pelo responsável da equipe. Não envie senhas ou o keystore pela conversa, e não os coloque dentro do repositório. Não há publicação automática.

## Primeira chave

Em um terminal PowerShell local, na raiz do projeto, execute:

```powershell
.\scripts\New-ReleaseKey.ps1 -StoreFile "$env:USERPROFILE\.loggym\release\loggym-release.p12"
```

O script solicita a senha com entrada protegida e confirmação. Usa PKCS12/RSA 3072, não substitui arquivo existente e localiza `keytool` pelo PATH ou pelo JDK do `java`. Registre a senha em um gerenciador de senhas da equipe e faça backup protegido do keystore fora do projeto. A equipe deve identificar o custodiante; perder a chave compromete atualizações futuras com a mesma assinatura.

## Gerar o APK

Depois de guardar a chave, execute:

```powershell
.\scripts\Build-Release.ps1 -StoreFile "$env:USERPROFILE\.loggym\release\loggym-release.p12"
```

O script pede novamente a senha protegida, configura as variáveis apenas na sessão do processo de build e limpa as senhas no bloco `finally`. Não use transcrição, `--debug`, `--info`, build scans ou comandos que imprimam o ambiente ao lidar com credenciais. A execução usa `--no-configuration-cache`.

Para ambientes de automação, forneça externamente `LOGGYM_RELEASE_STORE_FILE`, `LOGGYM_RELEASE_STORE_PASSWORD`, `LOGGYM_RELEASE_KEY_ALIAS` e `LOGGYM_RELEASE_KEY_PASSWORD`; então execute `./gradlew assembleRelease --no-configuration-cache`. PKCS12 usa a mesma senha para armazenamento e chave. Não há segredo padrão ou fallback para a chave de debug. Um keystore dentro do repositório ou credenciais ausentes impedem o build de release; o debug permanece independente.

Saída definitiva: `app/build/outputs/apk/release/app-release.apk`. Verifique antes de distribuir:

```powershell
& '<SDK>\build-tools\36.0.0\apksigner.bat' verify --verbose --print-certs app\build\outputs\apk\release\app-release.apk
```

Use a versão de build-tools instalada no seu SDK. Verifique o certificado com o custodiante da chave. Assinaturas de debug e release são diferentes: valide o APK em outro AVD ou instância com dados isolados, sem desinstalar a cópia usada na homologação. O aceite inclui instalação, abertura offline, criação, recuperação e finalização de um treino misto, e consulta do histórico.

## Evidência de assinatura de teste

Um APK eventualmente gerado com chave temporária para verificar o pipeline fica identificado em `app/build/outputs/apk/signing-validation/`. Essa chave não é a chave definitiva da equipe e esse artefato não deve ser distribuído. O resultado executado, incluindo limitações, está em [S3-IMPLEMENTACAO.md](S3-IMPLEMENTACAO.md).
