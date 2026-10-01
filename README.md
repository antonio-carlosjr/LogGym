# LogGym

Aplicativo Android para registrar treinos de musculação. Funciona **localmente**, com **um único usuário** e **offline-first**: não tem login, backend nem sincronização no MVP.

A especificação funcional completa (RF-01 a RF-129) está em [`especifica_o_funcional_loggym.md`](especifica_o_funcional_loggym.md). Os comentários do código citam os RFs que cada trecho implementa.

---

## Stack

| Camada | Tecnologia |
| :--- | :--- |
| Linguagem | Kotlin 2.1 |
| UI | Jetpack Compose + Material 3 |
| Navegação | Navigation Compose com rotas tipadas (`kotlinx.serialization`) |
| Persistência | Room 2.6 (SQLite) |
| Preferências | DataStore Preferences (flag de onboarding) |
| Injeção de dependência | Hilt (KSP) |
| Assincronismo | Coroutines + Flow |
| Testes | JUnit 4 (unitários de domínio e dados de seed) |

`minSdk 26` (Android 8.0), para usar `java.time` sem desugaring. `compileSdk`/`targetSdk 35`.

---

## Como abrir e executar

1. Abra a pasta do projeto no **Android Studio** (Ladybug ou mais recente).
2. Aguarde o *Gradle Sync*. O Android Studio usa o `gradle/wrapper/gradle-wrapper.properties` (Gradle 8.11.1) e o JDK embutido.
3. Execute a configuração `app` em um emulador ou dispositivo.

Testes unitários:

```bash
./gradlew testDebugUnitTest
```

> O repositório ainda não versiona os scripts `gradlew`/`gradlew.bat` nem o `gradle-wrapper.jar`. Para gerá-los, rode `gradle wrapper` com um Gradle local ou use o Android Studio, que funciona sem eles.

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
│   ├── seed/                   # Exercícios nativos e templates de plano (conteúdo provisório)
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
    ├── exercises/  plans/  session/  history/   # Telas provisórias com os RFs pendentes
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
| Build, DI, banco, entidades, DAOs, seed | ✅ Estruturado |
| Repositórios (exercícios, planos, sessões, preferências) | ✅ Implementados |
| Regras de domínio + testes unitários | ✅ Pré-preenchimento, cronômetro, validação de metas, seed |
| Onboarding e Home (plano ativo, divisões, aviso de rascunho) | ✅ Funcionais |
| Exercícios, Planos/Editor, Sessão, Histórico | 🚧 Telas provisórias que listam os RFs pendentes |

O planejamento das próximas entregas está em [`docs/SPRINTS.md`](docs/SPRINTS.md).

## Decisões de produto pendentes (seção 17)

1. Conteúdo definitivo da biblioteca nativa. O conteúdo atual é provisório, em `data/seed/NativeExercises.kt`.
2. Exercícios e metas de fábrica dos templates. Provisório, em `data/seed/PlanTemplates.kt`.
3. Unidade de carga. Adotado **kg** (`weight_kg`).
4. Alerta de fim de descanso com o app fechado. Fora do MVP, sem `AlarmManager`.
