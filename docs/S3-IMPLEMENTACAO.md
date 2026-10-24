# Sprint 3 — histórico, acabamento e entrega

## Escopo e estado

Código da Sprint 3 implementado, contemplando as tarefas originais e a melhoria leve S3-UX-01. Etapas humanas de acessibilidade, demonstração e release permanecem discriminadas abaixo. Sem alterações no esquema Room v2, nas regras de check manual, no pré-preenchimento ou nas prescrições. As propostas da Sprint 4 não foram implementadas. O aceite individual da Sprint 2 foi formalizado pelo usuário; a aprovação coletiva não impede o desenvolvimento local autorizado.

**Estado:** implementação concluída e homologação funcional individual relatada/aprovada. Em 24/10/2026, o responsável aprovou explicitamente o PR #12 e autorizou seu merge, incluindo a nova política de integração. A aprovação na conversa e a revisão técnica pelo Codex substituem a antiga exigência interna de revisão independente, conforme [TAREFAS.md](TAREFAS.md#aprovação-e-integração-de-pull-requests). A CI do commit atualizado e demais condições técnicas continuam obrigatórias. O [registro de integração do PR #12](https://github.com/antonio-carlosjr/LogGym/pull/12) consolida o resultado efetivo, CI e hash de merge; a integração está concluída quando esse registro indicar `MERGED`. Entrega definitiva e verificações humanas específicas permanecem separadas, sem aceite coletivo presumido.

| Tarefa | Implementação/evidência | Situação |
| --- | --- | --- |
| S3-01 | `HistoryViewModel`, `HistoryScreen`, contratos existentes do repositório | Implementada |
| S3-02 | `HistoryDetailViewModel`, `HistoryDetailScreen`; somente COMPLETED, snapshots, zero/nulos e tempo manual | Implementada |
| S3-03 | `SessionRepositoryTest`: alterações/exclusões após conclusão, histórico filtrado e ordenado | JVM e dispositivo aprovados |
| S3-04 | `Sprint3FlowTest`: onboarding → plano → treino misto parcial → histórico → detalhe | Fluxo aprovado no emulador |
| S3-05 | Assinatura externa em `app/build.gradle.kts`; scripts de chave/build; [procedimento](RELEASE.md) | Pipeline e assinatura de teste verificados; chave definitiva depende do custodiante |
| S3-06 | `strings.xml` com plurais; erros de carga e escrita recuperáveis, formulários preservados; preferências de testes isoladas | Implementada |
| S3-07 | Ícone adaptativo/monocromático, superfície do seletor, estado textual e visual das séries; superfícies neutras na Home e no descanso | Implementada; correções escuras aprovadas individualmente |
| S3-08 | Rótulos, estado de check, cabeçalhos, cores do tema; testes de temas/fonte e alvo de Pular | Parte técnica implementada; TalkBack e avaliação humana abrangente de acessibilidade pendentes |
| S3-09 | [Matriz RF](RASTREABILIDADE.md), README, planejamento e registro desta entrega | Documentada |
| S3-10 | [Roteiro de demonstração](DEMONSTRACAO.md) | Roteiro e ensaio técnico automatizado prontos; ensaio humano pendente |
| S3-UX-01 | Faixa fixa superior; aviso derivado de `rest_ends_at + 5 s`; nenhuma escrita para ocultar | Implementada |

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

As capturas de interface foram revistas em tema claro e escuro e com fonte ampliada no histórico. Os testes da sessão também verificaram temas claro/escuro/dinâmico e fonte 2×, visibilidade do descanso e alvo de toque de Pular. TalkBack e avaliação visual abrangente permanecem humanos; a aprovação individual das duas correções escuras está registrada abaixo. Não confundir os resultados locais com CI remota desta alteração.

O código de produção manteve Room v2 e os mesmos contratos de escrita. A primeira execução de lint apontou formatos de argumentos numéricos e leitura de recursos sem acompanhar configuração; corrigidos com formatos tipados e `LocalResources`. O teste novo tinha ambiguidade entre o texto pesquisado e o item da seleção; o seletor do teste foi corrigido. Na revisão visual, o contraste dos campos bloqueados foi aumentado sem habilitá-los. Preferências dos testes Hilt são isoladas por componente, sem apagar preferências reais; a persistência DataStore possui teste próprio.

No smoke por adb, a primeira tentativa de kill ocorreu durante a transição para segundo plano e não encerrou o PID. A verificação aguardou a transição e confirmou a ausência do processo. A navegação restaurou diretamente a sessão; o teste então verificou também Voltar/Home/Retomar. Não foi necessário alterar a recuperação de produção.

A [primeira CI da Sprint 3](https://github.com/antonio-carlosjr/LogGym/actions/runs/40125824880), no commit `2abd171`, encontrou uma falha JVM de sincronização e duas falhas instrumentadas de rolagem/visibilidade no API 35 (114/115 e 16/18 aprovados). Os testes de planos passaram a aguardar o término da escrita antes da próxima ação. Os testes da sessão passaram a rolar pela `LazyColumn` até compor a série e a localizar o aviso persistente dentro da lista, aguardando o commit do check. As asserções funcionais foram preservadas. A suíte JVM completa voltou a passar localmente; consultar o resultado subsequente do PR para a confirmação remota das correções. Nenhum código de produção foi alterado nessa correção de testes.

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest --no-configuration-cache --console=plain
```

Relatórios: `app/build/reports/tests/testDebugUnitTest/`, `app/build/reports/androidTests/connected/debug/` e `app/build/reports/lint-results-debug.html`. APK de debug: `app/build/outputs/apk/debug/app-debug.apk`. Capturas e resultado do smoke estão em `app/build/s3-device/release-smoke/`. Capturas feitas dentro da instrumentação podem ser limpas pelo runner após os testes. São evidências técnicas locais, não aprovação humana.

## Critérios e pendências de aceite

Revisão solicitada em 24/10/2026: o card “Treino em andamento” e a faixa de descanso usam superfícies neutras do tema com cores de conteúdo explícitas, para melhorar a combinação e o contraste no modo escuro. A janela do aviso final passou de 3 para aproximadamente 5 segundos. Contagem, timestamps, Pular e recuperação mantêm os contratos anteriores; o aviso não reinicia ao retomar. Essa revisão não acrescenta funcionalidades da Sprint 4 nem representa aceite coletivo.

Verificação local dessa revisão: `assembleDebug`, 115 testes JVM e lint aprovados (0 erros; 5 avisos de atualização). Os 18 instrumentados passaram no AVD temporário LogGymS3Review/API 37, executados diretamente pelo runner via `adb -s emulator-5556 shell am instrument -w -r com.loggym.test/com.loggym.HiltTestRunner`, após `assembleDebugAndroidTest`. O teste de temas contempla também escuro com cores dinâmicas. Capturas da Home clara/escura e da faixa escura foram conferidas, e Retomar/Pular foram verificados fora da instrumentação. As evidências temporárias foram geradas em `app/build/s3-dark-review/`, incluindo `instrumentation.log`, e podem ser removidas por limpeza do build. A CI da revisão final deve ser conferida nos checks do commit mais recente do PR #12; a CI anterior de `3f9e6c0` não valida automaticamente esse novo commit.

### Validação manual e autorização informadas — 24/10/2026

O usuário, responsável pela validação manual, aprovou no emulador: aparência do card “Treino em andamento” no tema escuro; aparência da faixa de descanso no tema escuro; desaparecimento de “Descanso concluído” após aproximadamente 5 segundos; e funcionamento geral que efetivamente testou. Não identificou defeitos bloqueantes. Esse relato complementa a validação funcional geral da Sprint 3 anteriormente informada, sem comprovar cada cenário do roteiro. O usuário autorizou expressamente o encerramento técnico e o merge do PR #12, desde que satisfeitos os requisitos obrigatórios do repositório.

Data do registro: 24/10/2026. Responsável: usuário que realizou e relatou os testes. Versão descrita: revisão local da Sprint 3 com superfícies neutras e aviso de 5 segundos. Data/hora exata da execução, identificação do APK, dispositivo/API e capturas individuais não foram informados. Não atribuir ao usuário testes de TalkBack, assinatura definitiva, ensaio ou demais cenários não explicitamente confirmados. O [registro de homologação](HOMOLOGACAO-S3.md#validação-individual-informada--24102026) distingue os resultados relatados dos critérios ainda abertos.

### Pendências distintas da implementação

- **Integração técnica:** aprovação explícita do responsável já recebida para o PR #12, incluindo a atualização da política. Conferir CI do commit atualizado, conflitos e proteções antes do merge commit. Resultado efetivo e versão integrada são registrados no PR; revisão independente não é mais uma exigência interna.
- **Aprovação coletiva:** não foi atribuída à equipe para a Sprint 3; o registro coletivo da Sprint 2 permanece separado. Pela nova política, essa ausência não bloqueia a integração autorizada pelo responsável.
- **S3-08:** teste humano de TalkBack e avaliação de acessibilidade ainda não confirmados.
- **S3-05:** criação/custódia/backup da chave definitiva e homologação do APK com essa assinatura pendentes; a chave temporária não representa entrega definitiva.
- **S3-10:** ensaio/apresentação humana ainda não confirmados.

As verificações humanas e de release ainda não realizadas impedem registrar todos os critérios originais de entrega como concluídos. O encerramento técnico e a integração autorizados pelo responsável são distintos desses itens, do aceite coletivo e da entrega do APK definitivo. Não publicar release nesta etapa.

### Política e registro de integração — 24/10/2026

O responsável substituiu expressamente a obrigatoriedade interna de revisão por outra pessoa pelo fluxo de revisão técnica do Codex, apresentação dos resultados e aprovação explícita na conversa. A autorização desta integração foi recebida depois da implementação, testes, revisão técnica e relato das pendências, e inclui as mudanças documentais da política no PR #12. Não autoriza merge automático de PRs futuros.

A consulta remota anterior à integração retornou `main.protected=false`, sem regras aplicáveis de rulesets; merge commit está habilitado. Isso deve ser reconferido antes da operação. Nenhuma proteção será alterada ou contornada, e nenhuma revisão de terceiros será simulada. A conclusão da integração, o commit final da branch e o hash do merge na `main` ficam consolidados na descrição e no evento de merge do PR #12, evitando registrar antecipadamente uma operação ainda não executada.

## Artefato de assinatura de validação

`app/build/outputs/apk/signing-validation/loggym-signing-validation.apk` foi gerado com certificado **LogGym Signing Validation Only**, de curta duração. A chave privada aleatória foi criada fora do repositório e apagada no `finally`; senhas não foram registradas em arquivos, logs ou conversa. O APK foi instalado somente na instância descartável. **Não distribuir este APK.** A chave definitiva da equipe não foi criada automaticamente: siga [RELEASE.md](RELEASE.md), escolha a senha no terminal e faça backup protegido. Essa participação não bloqueou as demais entregas.
