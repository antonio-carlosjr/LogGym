# Sprint 3 — histórico, acabamento e entrega

## Escopo e estado

Implementação local das dez tarefas originais e da melhoria leve S3-UX-01. Sem alterações no esquema Room v2, nas regras de check manual, no pré-preenchimento ou nas prescrições. As propostas da Sprint 4 não foram implementadas. O aceite individual da Sprint 2 foi formalizado pelo usuário; a aprovação coletiva não impede o desenvolvimento local autorizado.

| Tarefa | Implementação/evidência | Situação |
| --- | --- | --- |
| S3-01 | `HistoryViewModel`, `HistoryScreen`, contratos existentes do repositório | Implementada |
| S3-02 | `HistoryDetailViewModel`, `HistoryDetailScreen`; somente COMPLETED, snapshots, zero/nulos e tempo manual | Implementada |
| S3-03 | `SessionRepositoryTest`: alterações/exclusões após conclusão, histórico filtrado e ordenado | JVM e dispositivo aprovados |
| S3-04 | `Sprint3FlowTest`: onboarding → plano → treino misto parcial → histórico → detalhe | Fluxo aprovado no emulador |
| S3-05 | Assinatura externa em `app/build.gradle.kts`; scripts de chave/build; [procedimento](RELEASE.md) | Pipeline e assinatura de teste verificados; chave definitiva depende do custodiante |
| S3-06 | `strings.xml` com plurais; erros de carga e escrita recuperáveis, formulários preservados; preferências de testes isoladas | Implementada |
| S3-07 | Ícone adaptativo/monocromático, superfície do seletor, estado textual e visual das séries | Implementada |
| S3-08 | Rótulos, estado de check, cabeçalhos, cores do tema; testes de temas/fonte e alvo de Pular | Implementada; TalkBack e aceite visual humano pendentes |
| S3-09 | [Matriz RF](RASTREABILIDADE.md), README, planejamento e registro desta entrega | Documentada |
| S3-10 | [Roteiro de demonstração](DEMONSTRACAO.md) | Roteiro e ensaio técnico automatizado prontos; ensaio humano pendente |
| S3-UX-01 | Faixa fixa superior; aviso derivado de `rest_ends_at + 3 s`; nenhuma escrita para ocultar | Implementada |

## Regressão e resultados

Execução local final em **24/10/2026**, Windows e AVD Android 17/API 37, instância `emulator-5556` iniciada com `-read-only -no-snapshot -no-window`:

| Verificação | Resultado |
| --- | --- |
| `assembleDebug` | BUILD SUCCESSFUL |
| `testDebugUnitTest` | 115 aprovados; 0 falhas, erros e ignorados |
| `connectedDebugAndroidTest` | 18 aprovados; 0 falhas, erros e ignorados |
| `lintDebug` | 0 erros; 5 avisos não bloqueantes de versões de dependências/ferramentas |
| Validação sem credenciais | Build rejeitado conforme esperado, sem afetar debug |
| `assembleRelease` com chave temporária externa | BUILD SUCCESSFUL; `apksigner verify` aprovado, esquema v2 |
| Instalação e smoke offline do APK assinado de teste | Aprovados: plano, valores 0 kg/8 reps, check, morte de processo confirmada pelo PID, recuperação, Home/Retomar, finalização parcial e detalhe somente leitura |

As capturas de interface foram revistas em tema claro e escuro e com fonte ampliada no histórico. Os testes da sessão também verificaram temas claro/escuro/dinâmico e fonte 2×, visibilidade do descanso e alvo de toque de Pular. TalkBack e aprovação visual final permanecem humanos. Não confundir os resultados locais com CI remota desta alteração.

O código de produção manteve Room v2 e os mesmos contratos de escrita. A primeira execução de lint apontou formatos de argumentos numéricos e leitura de recursos sem acompanhar configuração; corrigidos com formatos tipados e `LocalResources`. O teste novo tinha ambiguidade entre o texto pesquisado e o item da seleção; o seletor do teste foi corrigido. Na revisão visual, o contraste dos campos bloqueados foi aumentado sem habilitá-los. Preferências dos testes Hilt são isoladas por componente, sem apagar preferências reais; a persistência DataStore possui teste próprio.

No smoke por adb, a primeira tentativa de kill ocorreu durante a transição para segundo plano e não encerrou o PID. A verificação aguardou a transição e confirmou a ausência do processo. A navegação restaurou diretamente a sessão; o teste então verificou também Voltar/Home/Retomar. Não foi necessário alterar a recuperação de produção.

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest --no-configuration-cache --console=plain
```

Relatórios: `app/build/reports/tests/testDebugUnitTest/`, `app/build/reports/androidTests/connected/debug/` e `app/build/reports/lint-results-debug.html`. APK de debug: `app/build/outputs/apk/debug/app-debug.apk`. Capturas e resultado do smoke estão em `app/build/s3-device/release-smoke/`. Capturas feitas dentro da instrumentação podem ser limpas pelo runner após os testes. São evidências técnicas locais, não aprovação humana.

## Critérios e pendências de aceite

Executar o [roteiro de homologação](HOMOLOGACAO-S3.md). O encerramento exige CI verde no commit entregue; consultar o resultado do PR correspondente, pois a evidência remota da Sprint 2 se refere ao commit `cb58fbb`. Registrar o aceite da Sprint 3 com responsáveis, data e versão. TalkBack, ensaio da apresentação e custódia/backup da chave exigem participação humana. Não houve publicação de release.

## Artefato de assinatura de validação

`app/build/outputs/apk/signing-validation/loggym-signing-validation.apk` foi gerado com certificado **LogGym Signing Validation Only**, de curta duração. A chave privada aleatória foi criada fora do repositório e apagada no `finally`; senhas não foram registradas em arquivos, logs ou conversa. O APK foi instalado somente na instância descartável. **Não distribuir este APK.** A chave definitiva da equipe não foi criada automaticamente: siga [RELEASE.md](RELEASE.md), escolha a senha no terminal e faça backup protegido. Essa participação não bloqueou as demais entregas.
