# Planejamento do MVP em 3 sprints

O ponto de partida é a estrutura inicial: domínio, banco Room, repositórios, onboarding e home. A partir dela, o restante da [especificação funcional](../especifica_o_funcional_loggym.md) (RF-01 a RF-129) fica dividido em três sprints de **2 semanas**. A ordem segue o fluxo da seção 15: **prescrever → executar → consultar**.

## Já concluído (estrutura inicial)

- Carga inicial dos exercícios nativos e onboarding com templates (RF-01 a RF-08).
- Início de sessão pela home, aviso de treino em andamento, retomar e descartar (RF-58, RF-63, RF-64, RF-96, RF-101 a RF-104).
- Regras de domínio com testes unitários: validação de metas, pré-preenchimento das séries e cronômetro por timestamp.
- Repositórios com as transações de snapshot, finalização atômica e plano ativo único.

---

## Sprint 1: Fundação validada, biblioteca e planos

**Objetivo:** o usuário monta e mantém seus planos de ponta a ponta.

| Item | RFs | Onde |
| :--- | :--- | :--- |
| Validar o build no Android Studio, versionar `gradlew` e `gradle-wrapper.jar`, revisar versões das libs | — | raiz, `gradle/` |
| CI no GitHub Actions: `testDebugUnitTest` + `assembleDebug` | — | `.github/workflows/android.yml` |
| Fechar as decisões pendentes 1 a 3 da seção 17: biblioteca nativa, templates e unidade kg | — | `data/seed/` |
| Testes instrumentados dos DAOs com banco em memória: seed único, plano ativo único, cascades | RF-01, RF-02, RF-21, RF-28, RF-29 | `app/src/androidTest/.../data/` |
| Tela de Exercícios: lista com distinção nativo/personalizado, criar, editar, excluir com confirmação e aviso de uso em planos | RF-09 a RF-21 | `ui/exercises/` |
| Tela de Planos: lista com o ativo destacado, criar, ativar, excluir com confirmação, criar a partir de template | RF-22 a RF-38 | `ui/plans/PlansScreen.kt` |
| Editor de plano: renomear; CRUD e reordenação de divisões; adicionar exercícios da biblioteca, configurar séries, faixa de repetições e descanso, remover e reordenar | RF-25, RF-39 a RF-57 | `ui/plans/PlanEditorScreen.kt` |

**Definição de pronto:**
- CI verde.
- Um plano personalizado é criado do zero e iniciado pela home.
- O app impede editar ou excluir exercícios nativos.

---

## Sprint 2: Execução do treino

**Objetivo:** registrar um treino completo que resista a interrupções.

| Item | RFs | Onde |
| :--- | :--- | :--- |
| Tela de sessão: séries pré-preenchidas, campos de carga (decimal, aceita 0) e repetições, marcar e desmarcar | RF-65 a RF-82 | `ui/session/` + `SessionViewModel` |
| Adicionar e remover séries apenas na sessão | RF-83 a RF-86 | `SessionRepository.addSet` / `removeSet` |
| Barra do cronômetro: atualização a cada 1 s calculada por `RestTimer.remaining()`, botão de pular | RF-87 a RF-95 | `domain/logic/RestTimer.kt` |
| Persistência contínua dos campos com debounce | RF-97, RF-98 | ViewModel |
| Finalizar com confirmação de séries pendentes, bloquear sessão vazia, descartar | RF-103 a RF-115 | `finishSession` / `discardSession` |
| Testes instrumentados do repositório de sessão: snapshot, pré-preenchimento, finalização atômica, rascunho único, com `Clock` fixo | RF-59 a RF-72, RF-115 | `app/src/androidTest/.../repository/` |
| Roteiro de teste manual de morte de processo ("Don't keep activities" e kill via adb) | RF-99, RF-100 | `docs/` |

**Definição de pronto:**
- Um treino é iniciado, o app é morto, e o treino é retomado e finalizado.
- O treino seguinte da mesma divisão vem pré-preenchido.

---

## Sprint 3: Histórico, robustez e entrega

**Objetivo:** consultar o passado com isolamento garantido e entregar um MVP polido.

| Item | RFs | Onde |
| :--- | :--- | :--- |
| Lista do histórico mostrando data, divisão e plano | RF-116 a RF-119 | `ui/history/HistoryScreen.kt` |
| Detalhe somente leitura com exercícios e séries (carga e repetições) | RF-120 a RF-122, RF-128 | `ui/history/HistoryDetailScreen.kt` |
| Testes de isolamento temporal: renomear ou excluir exercício e alterar ou excluir plano depois da sessão | RF-123 a RF-127, RF-129 | `app/src/androidTest/` |
| Polimento: estados vazios, mensagens de erro, textos em `strings.xml`, ícone do app, acessibilidade (rótulos e alvos ≥ 48 dp) | — | `res/`, telas |
| Testes de UI do fluxo principal: onboarding → iniciar → finalizar → histórico | seção 15 | `app/src/androidTest/.../ui/` |
| APK de release assinado, README atualizado e roteiro da demonstração | — | `app/build.gradle.kts`, `docs/` |

**Definição de pronto:**
- Todo RF está rastreado até o código ou um teste.
- O APK de release instala e funciona.
- O fluxo completo é demonstrado.

---

## Fora do escopo

Seção 18 da especificação, mais a decisão pendente 4: alarme de descanso com o app fechado, gráficos e PRs, sincronização, supersets e RPE, edição do histórico.

## Riscos

- **O Sprint 1 é o mais pesado.** Se apertar, a reordenação de divisões e exercícios (RF-44, RF-48) passa para o início do Sprint 2.
- **O build nunca foi compilado.** Por isso a validação é o primeiro item do Sprint 1, para revelar cedo incompatibilidades de versão ou de KSP.
