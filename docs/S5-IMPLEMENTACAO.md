# Sprint 5 — backup e restauração

Implementação autorizada pelo responsável em 25/10/2026. Base: `cd9d8ea624c3446a5d4d7c9d43aeadea020fc0f5` (PR #13, Sprint 4 integrada). Branch: `feature/S5-01-backup-e-restauracao`. Homologação funcional individual aprovada pelo responsável nos dez grupos relatados em [HOMOLOGACAO-S5](HOMOLOGACAO-S5.md), sem bloqueantes. O responsável confirmou o refinamento visual final e realizou o merge do PR #14: 35370e2720d364e612dcaa7b7a392a0d5d08be02, com CI pós-merge aprovada. Não foi atribuído aceite coletivo nem cumprimento de pendências humanas anteriores.

## Compromisso

Alternativa A, 34 pontos e 5 de reserva. Dev 1/2/3 são áreas técnicas, não três pessoas necessariamente simultâneas. Sem histórico individual, marcação automática, navegação inferior ou criptografia. O planejamento detalhado é preservado em [TAREFAS](TAREFAS.md); os contratos/protocolo estão na [decisão S5](decisoes/S5-backup.md).

| ID | Entrega | Pontos | Área técnica |
| --- | --- | ---: | --- |
| S5-01 | Formato lógico, modalidades, limites e integridade | 3 | Dev 1: 1; Dev 2: 2 |
| S5-02 | Coordenação global, gerações e flush aguardável antes da captura | 8 | Dev 1: 5; Dev 2: 3 |
| S5-03 | Staging privado, validação integral e resumo | 5 | Dev 2 |
| S5-04 | Esquema 3/journal, aplicação, reversão e inicialização segura | 13 | Dev 1: 6; Dev 2: 2; Dev 3: 5 |
| S5-05 | Configurações/SAF, acessibilidade, regressão e instruções/documentação | 5 | Dev 3 |
| Total | Reserva adicional: 5 | **34** | **Dev 1: 12; Dev 2: 12; Dev 3: 10** |

## Componentes e comportamento

- `domain/backup`: contratos da operação e coordenação. Mutadores de planos, exercícios, sessões e preferências respeitam a exclusão global e a geração.
- `SessionViewModel`: participante de preparação; drena debounce e operações locais sem navegar ou marcar séries. Novas edições ficam impedidas durante a captura. Erros preservam buffers para correção/repetição.
- `data/backup/BackupFormat`, `BackupColumns`, `BackupData`: JSONL versionado, whitelists, SHA-256, limites, validações de domínio/relações/catálogo/alocadores.
- `BackupEngine`: captura privada consistente, validação em Room temporário, undo durável, substituição e recuperação entre Room/DataStore.
- `BackupRepositoryImpl`: jobs em primeiro plano com duração da aplicação, estados e tokens; transferência por URI, cancelamento, cópia limitada e limpeza segura.
- `RestoreJournalEntity`, `MIGRATION_2_3`, esquema exportado 3: única ampliação do banco; sete tabelas de negócio preservadas.
- `AppViewModel`/`LogGymApp`/`LogGymNavHost`: recuperação anterior às telas normais, bloqueio/retry em falha, descarte dos stores antigos e Home → Configurações após substituição.
- `SettingsViewModel`, `SettingsScreen`, `BackupSection`, recursos: modalidades, aviso sem criptografia, resumo e confirmação integral; estados e feedback acessíveis nos temas existentes.
- Manifest/regras XML: desabilitam cópia/transferência automática. `AGENTS.md` referencia fontes de verdade; política vigente: Codex prepara PR, responsável homologa e faz merge manual no GitHub.

## Verificações

Em 25/10/2026, build debug, APK de testes e **169 testes JVM** aprovados, sem falhas/erros/ignorados. Lint: **0 erros e 5 avisos de versões de dependências**, preservadas as bibliotecas atuais. Referência anterior: 132 JVM e 24 instrumentados. A CI do commit apresentado deve ser conferida no PR; seu resultado efetivo fica registrado ali, sem presumir aprovação antecipada.

**52 testes instrumentados aprovados**, sem falhas, no AVD local API 37 (`Medium_Phone_API_37.0`, instância de teste read-only em `emulator-5560`, sem apagar o AVD original). Execução final: `adb -s emulator-5560 shell am instrument -w com.loggym.test/com.loggym.HiltTestRunner`, resultado `OK (52 tests)`. Inclui migrações 1→2→3/2→3, engine/codec/validação, contratos SAF e fluxo de exportar/restaurar/retomar com rotação. API 35 será verificada pela CI; não presumir validação humana nem equivalência a todos os aparelhos/providers.

Uma rodada apresentou falha interna FIR/Kotlin do Lint durante análise paralela, inclusive em arquivo de teste anterior à S5. A execução completa com `--max-workers=1` passou sem suprimir detectores ou ignorar testes. Os comandos padrão seguem na CI; qualquer falha remota deverá ser diagnosticada antes de considerar a entrega pronta.

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --continue --max-workers=1 --no-configuration-cache --console=plain
# Emulador de teste selecionado explicitamente; CI usa API 35.
.\gradlew.bat connectedDebugAndroidTest --no-configuration-cache --console=plain
```

Sem credenciais, publicação de release, merge ou implementação de itens posteriores. Arquivo local `gradle/gradle-daemon-jvm.properties` não integra a entrega.

Os testes incluem flush antes de 500 ms sem espera arbitrária, revisão nova durante gravação antiga, entrada inválida/falha/cancelamento, borracha aceita, ausência de deadlock, liberação de leases e rejeição de geração antiga. Codec/validação exercitam campos/tipos/checksum, limites e catálogo/relações. Room/DataStore reais são reabertos após interrupção em undo/commit Room/preferências/commit final; reversão interrompida e undo corrompido também são cobertos. Os contratos SAF são testados com registry controlado, complementados por fluxo real de repositório/navegação; isso não comprova comportamento de todo provider externo.

## Pendências humanas

O refinamento final e a integração foram confirmados pelo responsável. Os itens do roteiro [HOMOLOGACAO-S5](HOMOLOGACAO-S5.md) permanecem como evidência histórica; não há reteste/merge da S5 pendente. O responsável confirmou os dez grupos funcionais, incluindo acessibilidade, sem especificar TalkBack abrangente ou todos os providers/subcenários do roteiro. Assinatura definitiva/custódia e ensaio anteriores permanecem não confirmados. Não foi atribuído aceite coletivo.

## Refinamento após homologação individual

Em 25/10/2026, `BackupSection` passou a apresentar as explicações em diálogo acessível pelo ícone junto ao título, retirando o bloco fixo. `backup_help` separa as modalidades com linha em branco; informação e modal de exportação reutilizam o conteúdo, incluindo o aviso de ausência de criptografia. `BackupSettingsTest` verifica abertura/fechamento sem iniciar operação e conteúdo do modal. Persistência, formato e protocolo de recuperação permanecem intactos.

Rodada local do refinamento: build debug/APK de teste e 169 JVM aprovados; lint sem erros (cinco avisos de versões); 4/4 testes instrumentados de `BackupSettingsTest` aprovados no API 37, em instância isolada read-only (`emulator-5560`), preservando o emulador do usuário. A nova CI será registrada no PR #14; os 52 instrumentados completos acima são a evidência anterior ao refinamento, sem confundir com esta rodada direcionada. Não presumir aprovação humana do novo layout.
