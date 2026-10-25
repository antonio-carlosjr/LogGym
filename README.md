# LogGym

[![Android CI](https://github.com/antonio-carlosjr/LogGym/actions/workflows/android.yml/badge.svg)](https://github.com/antonio-carlosjr/LogGym/actions/workflows/android.yml)

Aplicativo Android para registrar treinos de musculação. Funciona **localmente**, com **um único usuário** e **offline-first**: não tem login, backend nem sincronização no MVP.

A especificação funcional completa (RF-01 a RF-148) está em [`especifica_o_funcional_loggym.md`](especifica_o_funcional_loggym.md). Os comentários do código citam os RFs que cada trecho implementa.

---

## Stack

| Camada | Tecnologia |
| :--- | :--- |
| Linguagem | Kotlin 2.4 (integrado ao AGP 9) |
| Build | Gradle 9.8 (wrapper) + Android Gradle Plugin 9.4 |
| UI | Jetpack Compose + Material 3 |
| Navegação | Navigation Compose com rotas tipadas (`kotlinx.serialization`) |
| Persistência | Room 2.8 (SQLite), com o esquema exportado em `app/schemas/` |
| Preferências | DataStore Preferences (flag de onboarding) |
| Injeção de dependência | Hilt (KSP) |
| Assincronismo | Coroutines + Flow |
| Testes | JUnit 4, Robolectric, Compose UI Test e testes instrumentados |

`minSdk 26` (Android 8.0), para usar `java.time` sem desugaring. `compileSdk`/`targetSdk 37`.

---

## Como abrir e executar

**Pré-requisitos:** JDK 17 ou superior (testado com o 21) e Android SDK com a platform 37. O Android Studio já traz os dois.

1. Abra a pasta do projeto no **Android Studio**.
2. Aguarde o *Gradle Sync*. O wrapper baixa o Gradle 9.8.0 e confere o checksum.
3. Execute a configuração `app` em um emulador ou dispositivo.

Pela linha de comando, crie um `local.properties` (não versionado) com o caminho do SDK, por exemplo `sdk.dir=C:/Users/<você>/AppData/Local/Android/Sdk`. Depois rode:

```bash
./gradlew assembleDebug testDebugUnitTest
```

Os testes de ponta a ponta no dispositivo precisam de um emulador ou aparelho conectado:

```bash
./gradlew connectedDebugAndroidTest
```

O build usa *configuration cache* e *build cache* (`gradle.properties`), então as execuções seguintes levam poucos segundos.

---

## Estrutura de pastas

```text
app/src/main/java/com/loggym/
├── LogGymApplication.kt        # @HiltAndroidApp
├── MainActivity.kt             # Host Compose
│
├── domain/                     # Regras de negócio puras, sem Android
│   ├── model/                  # Exercise, Plan, Workout, WorkoutExercise, Session, ...
│   ├── logic/                  # SetPrefill (seção 8), RestTimer (seção 10)
│   └── repository/             # Interfaces (contratos) dos repositórios
│
├── data/                       # Implementação de persistência
│   ├── local/
│   │   ├── entity/             # Tabelas Room
│   │   ├── relation/           # POJOs @Relation (plano → divisões → exercícios, sessão → séries)
│   │   ├── dao/                # ExerciseDao, PlanDao, SessionDao
│   │   ├── LogGymDatabase.kt
│   │   ├── Converters.kt       # Instant ⇄ epoch millis
│   │   └── NativeExerciseSeedCallback.kt   # Carga inicial (RF-01/RF-02)
│   ├── seed/                   # Exercícios nativos e templates de plano (60 nativos e 4 templates)
│   ├── mapper/                 # Entity → modelo de domínio
│   ├── preferences/            # DataStore
│   └── repository/             # Implementações com as transações
│
├── di/                         # Módulos Hilt (banco, DAOs, Clock, repositórios)
│
└── ui/
    ├── LogGymApp.kt            # Decide o destino inicial (onboarding ou home)
    ├── navigation/             # Rotas tipadas + NavHost
    ├── theme/
    ├── components/             # Componentes compartilhados
    ├── onboarding/             # Tela + ViewModel (funcional)
    ├── home/                   # Tela + ViewModel (funcional)
    ├── exercises/  plans/  session/             # Biblioteca, planos e execução
    ├── history/                               # Lista/detalhe somente leitura com snapshots
    └── util/
```

---

## Arquitetura

O app usa camadas no estilo *Clean Architecture* simplificado, com MVVM na apresentação:

```text
UI (Compose)  →  ViewModel  →  Repository (interface, domain)  →  RepositoryImpl (data)  →  DAO  →  Room/SQLite
                                         ↑
                             domain/logic (regras puras e testáveis)
```

- **`domain`** não depende de Android nem de Room. Contém os modelos, as interfaces dos repositórios e as regras puras (pré-preenchimento, cronômetro, validação de metas).
- **`data`** implementa os repositórios. Toda operação que altera mais de uma tabela roda em `database.withTransaction { }`.
- **`ui`** observa `Flow`s expostos pelos ViewModels como `StateFlow` e nunca acessa DAOs diretamente.
- Um `java.time.Clock` é injetado nos repositórios, o que deixa os testes de horário e de descanso determinísticos.

### A regra mais importante: três conceitos separados (seção 19)

| Conceito | Entidade | Papel |
| :--- | :--- | :--- |
| `Exercise` | `exercises` | Definição reutilizável do movimento (nativo ou personalizado). |
| `WorkoutExercise` | `workout_exercises` | Prescrição do exercício **em uma divisão**: séries, faixa de repetições, descanso. |
| `SessionExercise` | `session_exercises` + `session_sets` | **Snapshot** da prescrição junto com a execução real. |

### Modelo de dados

```text
plans ─1:N─ workouts ─1:N─ workout_exercises ─N:1─ exercises
  (CASCADE)            (CASCADE)                 (CASCADE: RF-21)

sessions ─1:N─ session_exercises ─1:N─ session_sets
         (CASCADE)                (CASCADE)
   │                     │
   └ source_plan_id,     └ exercise_id  → referências fracas, SEM chave estrangeira
     source_workout_id
```

Decisões que decorrem da especificação:

- **Isolamento temporal (RF-61, RF-62, RF-123 a RF-129).** Quando a sessão começa, nome do plano, nome da divisão, nome do exercício, grupo muscular, séries, repetições e descanso são **copiados** para `sessions`/`session_exercises`. As sessões não têm chave estrangeira para planos ou exercícios, então renomear, alterar ou excluir esses registros não mexe no histórico.
- **Identidade para o pré-preenchimento (RF-68).** `session_exercises.exercise_id` guarda o id original sem FK. A consulta do "último treino" usa esse id em vez do nome e continua funcionando depois que o exercício é apagado.
- **Rascunho e sessão concluída na mesma tabela (RF-96, RF-115).** `sessions.status` vale `DRAFT` ou `COMPLETED`. Finalizar é uma transação que apaga as séries não marcadas (RF-81), remove exercícios sem séries e muda o status. Assim o treino não se perde nem aparece duplicado entre rascunho e histórico. Descartar apaga o rascunho (RF-105).
- **Um único rascunho (RF-63, RF-64).** `startSession` verifica, dentro da transação, se já existe um `DRAFT`.
- **Somente leitura depois de concluída (RF-128).** As queries que alteram séries filtram por sessões `DRAFT`.
- **Plano ativo único (RF-28, RF-29).** `setActivePlan` desativa todos e ativa um, na mesma transação.
- **Templates (RF-37, RF-38).** Os templates ficam em código (`data/seed/PlanTemplates.kt`) e referenciam os exercícios nativos por uma `native_key` estável. Selecionar um template cria cópias locais comuns, que o usuário pode editar livremente.
- **Carga inicial (RF-01, RF-02).** `RoomDatabase.Callback.onCreate` roda só quando o banco é criado. O `INSERT OR IGNORE` sobre o índice único de `native_key` impede duplicatas.
- **Onboarding (RF-03, RF-08).** Uma flag no DataStore é apagada junto com os dados do app.

### Cronômetro de descanso (seção 10)

O cronômetro não usa `ForegroundService`. Ao marcar uma série, o repositório grava `rest_started_at` e `rest_ends_at` na sessão, e a UI calcula `restante = rest_ends_at − agora` com `RestTimer.remaining()`. Desse jeito o tempo continua correndo com o app em segundo plano, resiste à recomposição e à morte do processo, e aparece como concluído se o horário final já passou (RF-93 a RF-95). Existe no máximo um cronômetro por sessão, e ele reinicia a cada série concluída (RF-90, RF-91).

### Pré-preenchimento das séries (seção 8)

`SessionDao.getLastCompletedSets` busca as séries executadas na sessão `COMPLETED` mais recente que contém o exercício. `SetPrefill.build` associa essas séries pela posição: as que faltam ficam vazias e as que sobram são ignoradas (RF-69 a RF-72).

---

## Estado atual

| Área | Situação |
| :--- | :--- |
| Build (AGP 9.4, Kotlin 2.4, Gradle 9.8) e CI (build, testes da JVM e emulador) | ✅ Sprint 1 |
| Banco, seed de 60 exercícios nativos e 4 planos prontos, regras de entrada (decisão S1-04) | ✅ Sprint 1 |
| Onboarding, Home, biblioteca de exercícios, planos e editor de plano | ✅ Sprint 1 |
| Testes: unidade, persistência e ViewModels (Robolectric), ponta a ponta na JVM e no emulador | ✅ Sprint 1 |
| Execução: carga/repetições e tempo manual, descanso, rascunho e finalização | Aceite individual formalizado em 24/10/2026; evidências de testes/CI registradas; aprovação coletiva pendente ([detalhes](docs/S2-IMPLEMENTACAO.md)) |
| Histórico, polimento, acessibilidade e descanso | Implementados na Sprint 3; 115 testes JVM e 18 instrumentados aprovados; testes e pendências em [S3-IMPLEMENTACAO.md](docs/S3-IMPLEMENTACAO.md) |
| Release assinado | Configuração externa pronta; chave definitiva/custódia e aceite humano pendentes ([procedimento](docs/RELEASE.md)) |

O planejamento das próximas entregas está em [`docs/SPRINTS.md`](docs/SPRINTS.md).

## Escopo posterior

Distância (RF-136 parcial), fotos dos nativos e kg/lb continuam pendentes. Biblioteca/templates e convenção de carga já foram decididos em [S1-04](docs/decisoes/S1-04.md). Alarme de descanso com o app fechado segue fora do MVP.

O banco está na versão **3**: migrações 1→2→3 preservam planos, rascunhos e histórico; a versão 3 acrescenta somente o journal da restauração. A sessão usa debounce de 500 ms: morte antes do commit pode perder a última digitação. A exportação salva buffers válidos antes da captura. Consulte o [roteiro de recuperação](docs/reunioes/S2-06-recuperacao.md) e o [protocolo de backup](docs/decisoes/S5-backup.md).

## Validação e entrega da Sprint 3

O histórico apresenta apenas sessões concluídas, com detalhe somente leitura e suporte a séries de carga/repetições e tempo manual. Valores ausentes são distintos de zero. O descanso aparece em faixa fixa; o aviso de conclusão some cerca de 5 s após o horário final, inclusive ao retomar. Banco Room v2 e regras da Sprint 2 preservados.

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug connectedDebugAndroidTest --no-configuration-cache --console=plain
```

Consulte a [matriz RF → código/teste](docs/RASTREABILIDADE.md), o [roteiro de homologação](docs/HOMOLOGACAO-S3.md), a [preparação de assinatura](docs/RELEASE.md) e a [demonstração](docs/DEMONSTRACAO.md). O usuário aprovou individualmente as correções finais no emulador em 24/10/2026, sem bloqueantes, e aprovou explicitamente o PR #12 e seu merge. A [política vigente](docs/TAREFAS.md#aprovação-e-integração-de-pull-requests) exige revisão técnica, CI verde e proteções satisfeitas; o responsável homologa e faz merge manualmente no GitHub. A aprovação na conversa descrita nesta seção corresponde à autorização histórica do PR #12. O [PR #12](https://github.com/antonio-carlosjr/LogGym/pull/12) registra a conclusão efetiva e o hash de merge. TalkBack, assinatura definitiva e ensaio humano permanecem pendentes; não foi atribuído aceite coletivo. Não há publicação automática nem autorização de merge para PRs futuros.

## Sprint 4 — homologação funcional individual aprovada; integrada pelo PR #13

Prescrição fixa/faixa, descanso manual inteiro de 0 a 600 s com ±15, cadastro contextual, borracha por exercício e configurações persistentes de tema/pré-preenchimento. Banco Room v2 preservado, sem migração; snapshots existentes não mudam. Check continua manual e o aviso de descanso mantém aproximadamente 5 s.

Consulte [implementação e testes](docs/S4-IMPLEMENTACAO.md), [homologação](docs/HOMOLOGACAO-S4.md) e [tarefas/roadmap](docs/TAREFAS.md). Entrega em branch com commits separados; CI e revisão técnica precedem sua aprovação explícita para merge final. Aprovação do plano não equivale a homologação nem merge autorizado.

Em 25/10/2026, o responsável aprovou os sete grupos funcionais da Sprint 4, sem defeitos bloqueantes, e autorizou expressamente o merge commit do PR #13. O [registro de integração do PR #13](https://github.com/antonio-carlosjr/LogGym/pull/13) consolida o resultado efetivo, hash do merge e CI pós-merge; a integração está concluída quando indicar `MERGED`. A autorização inclui sincronização e encerramento técnico documental, sem publicação de release ou início da implementação da Sprint 5. O relato não confirma TalkBack, avaliação abrangente de acessibilidade, assinatura definitiva, ensaio ou aprovação de outros integrantes.

Backup está reservado à Sprint 5: completo ou sem histórico, sem criptografia, com aviso claro sobre o conteúdo legível. Não foi implementado nesta sprint. Histórico individual, check automático, layout compacto, preferência por exercício, fotos, distância e kg/lb continuam no backlog.

A nova proposta de navegação inferior está em [UX-NAV-01](docs/BACKLOG-UX.md), fora do escopo da Sprint 4 e sem implementação autorizada.

## Sprint 5 — backup e restauração

Configurações → Backup e restauração permite exportar completo (incluindo histórico e rascunho) ou sem histórico (nenhuma sessão), e importar por substituição integral após validação/resumo/confirmação. Arquivos `.loggym` são legíveis, **sem criptografia**. Guarde-os em local apropriado; SHA-256 detecta corrupção, sem autenticar o produtor. Limites: 64 MiB/arquivo, 1 MiB/registro, 500 mil registros.

Exportar salva antes as edições válidas recebidas pelo autossalvamento; formulários com Salvar explícito continuam não confirmados. Restaurar sem histórico remove histórico e rascunho atuais. Não há mesclagem, cópia automática do Android, serviço de background ou retomada automática da transferência. Rotação preserva a operação; após morte do processo a transferência deve ser reiniciada. Recuperação de restauração interrompida ocorre antes das telas normais, revertendo dados e preferências anteriores quando necessário.

Veja [implementação/testes](docs/S5-IMPLEMENTACAO.md), [decisão técnica](docs/decisoes/S5-backup.md), [homologação individual](docs/HOMOLOGACAO-S5.md) e [AGENTS](AGENTS.md). Política atual: Codex implementa/testa/revisa, acompanha CI e abre PR; **o responsável homologa e faz merge manualmente no GitHub**. Os relatos e autorizações anteriores são históricos. O responsável aprovou individualmente os dez grupos funcionais da S5, sem bloqueantes. O responsável também aprovou o refinamento final e integrou o PR #14 (35370e2), com CI pós-merge verde. Aceite coletivo e cumprimento das pendências humanas anteriores não são presumidos.

## Sprint 6 — histórico individual e conclusão automática opcional

O relógio ao lado de cada exercício abre um histórico paginado por identidade/tipo, sobre o treino, com snapshots e ocorrências agrupadas por sessão. Abrir/fechar não conclui séries nem perde texto/posição ou reinicia descanso.

Configurações oferece conclusão automática, **desativada por padrão**. Após uma alteração efetiva em qualquer campo, Concluído no teclado ou entrar nos campos de outra série pode marcar se todos os valores necessários forem válidos. Próximo dentro da mesma linha, blur, histórico, pré-preenchimento e debounce não concluem. Check manual continua disponível; desmarcar exige nova edição para outra conclusão automática. Borracha e recuperação não reativam intenções antigas.

O backup público passa a **v2**, incluindo a preferência. Arquivos públicos v1 deixam de ser importáveis, com mensagem de incompatibilidade; gere um novo backup. Dados locais existentes continuam preservados, Room permanece v3. A recuperação privada de undo S5 pendente é mantida, assim como journal, rollback e consistência Room/DataStore. Não há criptografia.

Escopo autorizado: 27 pontos e 12 de reserva. [Implementação/resultados](docs/S6-IMPLEMENTACAO.md), [contratos](docs/decisoes/S6-execucao.md) e [homologação pendente](docs/HOMOLOGACAO-S6.md). Navegação inferior de cinco abas fica candidata à S7; não foi implementada.
