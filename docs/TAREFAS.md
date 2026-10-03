# Divisão de tarefas

Este documento detalha o [planejamento em 3 sprints](SPRINTS.md) em tarefas para uma equipe de **3 pessoas**.

## Papéis

Cada pessoa é dona de uma área ao longo das sprints. Isso reduz conflitos de merge e cria especialistas.

| Pessoa | Área principal |
| :--- | :--- |
| **Dev 1** | Infraestrutura, dados e testes: build, CI, DAOs, repositórios, testes instrumentados, release |
| **Dev 2** | Exercícios e execução: biblioteca de exercícios, configuração de exercícios no plano, tela de sessão, histórico |
| **Dev 3** | Planos e experiência: planos, editor de divisões, cronômetro, finalização, polimento e acessibilidade |

> Substitua "Dev 1/2/3" pelos nomes dos integrantes.

**Estimativa:** pontos na escala de Fibonacci (1, 2, 3, 5, 8). Como referência, 1 ponto equivale a cerca de meio dia de trabalho. A meta é 13 a 16 pontos por pessoa em cada sprint.

## Combinados da equipe

- **Branch por tarefa:** `feature/<id>-descricao-curta`, por exemplo `feature/S1-05-lista-exercicios`.
- **Commits:** atômicos, no padrão Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`…), em português.
- **Pull requests:** todo PR para `main` precisa de revisão de outra pessoa e CI verde (a CI passa a existir depois da S1-02).
- **Arquivos compartilhados:** mudanças em `domain/` ou em interfaces de repositório são avisadas à equipe antes, porque afetam as três áreas.
- **Rastreabilidade:** a descrição do PR cita os RFs atendidos.

---

## Sprint 1: Fundação e prescrição

| ID | Tarefa | RFs | Responsável | Pts | Depende de |
| :--- | :--- | :--- | :---: | :---: | :--- |
| S1-01 | Abrir no Android Studio, corrigir erros de compilação, versionar `gradlew` e `gradle-wrapper.jar`, atualizar as versões das libs | — | Dev 1 | 3 | — |
| S1-02 | CI no GitHub Actions: `testDebugUnitTest` + `assembleDebug` em cada PR | — | Dev 1 | 2 | S1-01 |
| S1-03 | Testes de persistência com Room em memória via Robolectric (rodam na JVM e na CI, sem emulador): seed sem duplicatas, plano ativo único, cascade de divisões e prescrições | RF-01, RF-02, RF-21, RF-28, RF-29 | Dev 1 | 5 | S1-01 |
| S1-04a | ✅ Reunião de decisão e registro em [`decisoes/S1-04.md`](decisoes/S1-04.md); seção 17 da especificação marcada como [D] | seção 17 | Dev 1 (conduz), todos | 1 | — |
| S1-04b | Código: `enum MuscleGroup`, `NativeExercises` e `PlanTemplates` finais, regras de entrada em `domain/`. **Bloqueada** pelas pendências P1 a P10 | RF-130 a RF-148 | Dev 1 | 2 | S1-04a |
| S1-04c | Testes do seed (grupos válidos, nomes únicos, templates dentro das regras) e das regras de entrada | RF-130 a RF-148 | Dev 1 | 1 | S1-04b |
| S1-05 | `ExerciseLibraryViewModel` + lista com busca e indicação de nativo/personalizado | RF-09 a RF-12 | Dev 2 | 3 | S1-01, S1-04b |
| S1-06 | Diálogo de criar/editar exercício personalizado, com nome obrigatório e grupo muscular opcional; nativos bloqueados | RF-13 a RF-15, RF-18 | Dev 2 | 3 | S1-05 |
| S1-07 | Excluir personalizado com confirmação e aviso quando usado em planos (`countPlanUsages`) | RF-16, RF-17, RF-19 a RF-21 | Dev 2 | 2 | S1-05 |
| S1-08 | No editor: seletor de exercício da biblioteca e formulário de séries, faixa de repetições e descanso (validação via `ExerciseTarget`) | RF-46, RF-49 a RF-57 | Dev 2 | 5 | S1-04b, S1-05, S1-11 |
| S1-09 | `PlansViewModel` + lista de planos com o ativo destacado; ativar e trocar o plano ativo | RF-22, RF-28 a RF-31 | Dev 3 | 3 | S1-01 |
| S1-10 | Criar plano personalizado (nome), criar a partir de template, excluir com confirmação | RF-23, RF-24, RF-26, RF-27, RF-33, RF-35 a RF-38 | Dev 3 | 3 | S1-09 |
| S1-11 | `PlanEditorViewModel` + editor: renomear plano; adicionar, renomear e excluir divisões | RF-25, RF-39 a RF-43 | Dev 3 | 5 | S1-09 |
| S1-12 | Reordenar divisões e exercícios da divisão; remover exercício da divisão | RF-44, RF-45, RF-47, RF-48 | Dev 3 | 3 | S1-11 |

**Carga:** Dev 1 = 14 · Dev 2 = 13 · Dev 3 = 14

> ⚠️ As decisões da S1-04 ampliam o escopo do MVP: tempo/distância, fotos dos nativos e kg/lb. As estimativas acima são as aprovadas na reunião. A proposta de reestimativa e as tarefas novas (cerca de +18 pontos) estão em [`decisoes/S1-04.md`](decisoes/S1-04.md#planejamento) e aguardam validação da equipe.

> Se a sprint apertar, a S1-12 passa para o início da Sprint 2. Dev 1 tem folga para ajudar na S1-08 ou na S1-12.

---

## Sprint 2: Execução do treino

| ID | Tarefa | RFs | Responsável | Pts | Depende de |
| :--- | :--- | :--- | :---: | :---: | :--- |
| S2-01 | `SessionViewModel`: contrato do estado da tela (`SessionUiState`) e ações. Primeira tarefa da sprint, desbloqueia Dev 2 e Dev 3 | — | Dev 2 | 3 | — |
| S2-02 | Tela de sessão: cards por exercício com meta (séries × faixa), linhas de série com carga (decimal, aceita 0) e repetições | RF-65, RF-73 a RF-78 | Dev 2 | 5 | S2-01 |
| S2-03 | Marcar e desmarcar série; adicionar série extra; remover série não concluída | RF-79 a RF-85 | Dev 2 | 3 | S2-02 |
| S2-04 | Persistência contínua dos campos com debounce (~500 ms) e gravação imediata no check | RF-97, RF-98 | Dev 1 | 3 | S2-01 |
| S2-05 | Testes instrumentados do `SessionRepository` com `Clock` fixo: snapshot, pré-preenchimento a partir do histórico, rascunho único, finalização atômica, descarte | RF-59 a RF-72, RF-105, RF-111, RF-115 | Dev 1 | 5 | — |
| S2-06 | Roteiro de teste manual de morte de processo e execução dele ("Don't keep activities", `adb shell am kill`) | RF-99, RF-100, RF-106 | Dev 1 | 2 | S2-03 |
| S2-07 | Barra do cronômetro: tempo restante atualizado a cada 1 s via `RestTimer.remaining()`, botão de pular, estado de concluído | RF-87 a RF-95 | Dev 3 | 5 | S2-01 |
| S2-08 | Finalizar treino: confirmação com séries pendentes, bloqueio de sessão vazia, volta à home | RF-86, RF-107 a RF-114 | Dev 3 | 3 | S2-01 |
| S2-09 | Descartar dentro da sessão com confirmação; "Retomar" na home abre a sessão; voltar sem finalizar | RF-102 a RF-104, RF-108 | Dev 3 | 2 | S2-08 |

**Carga:** Dev 1 = 10 · Dev 2 = 11 · Dev 3 = 10

> A folga da sprint vai para revisão de PRs e pareamento na integração entre a tela e o cronômetro, que é o ponto mais arriscado.

---

## Sprint 3: Histórico e entrega

| ID | Tarefa | RFs | Responsável | Pts | Depende de |
| :--- | :--- | :--- | :---: | :---: | :--- |
| S3-01 | `HistoryViewModel` + lista do histórico (data, divisão, plano, duração) | RF-116 a RF-119 | Dev 2 | 3 | — |
| S3-02 | Detalhe da sessão somente leitura: exercícios e séries com carga e repetições, usando os snapshots | RF-120 a RF-123, RF-128 | Dev 2 | 3 | S3-01 |
| S3-03 | Testes de isolamento temporal: renomear ou excluir exercício e alterar ou excluir plano depois da sessão | RF-124 a RF-127, RF-129 | Dev 1 | 3 | — |
| S3-04 | Testes de UI em Compose do fluxo principal: onboarding → iniciar → séries → finalizar → histórico | seção 15 | Dev 1 | 5 | S3-02 |
| S3-05 | Build de release assinado: keystore fora do repositório, `signingConfig`, APK gerado | — | Dev 1 | 2 | — |
| S3-06 | Mover todos os textos para `strings.xml`; estados vazios e mensagens de erro em todas as telas | — | Dev 3 | 3 | — |
| S3-07 | Ícone adaptativo do app e revisão visual (tema e espaçamentos) | — | Dev 3 | 2 | — |
| S3-08 | Acessibilidade: `contentDescription`, alvos ≥ 48 dp, contraste, teste com TalkBack | — | Dev 3 | 3 | S3-06 |
| S3-09 | Matriz de rastreabilidade RF → código/teste e atualização do README | todos | Dev 2 | 3 | S3-02 |
| S3-10 | Roteiro de demonstração e ensaio da apresentação | — | Dev 3 (conduz), todos | 2 | S3-04 |

**Carga:** Dev 1 = 10 · Dev 2 = 9 · Dev 3 = 10

> A Sprint 3 tem folga intencional para correção de bugs encontrados nos testes e na demonstração.

---

## Resumo por pessoa

| Pessoa | Sprint 1 | Sprint 2 | Sprint 3 | Total |
| :--- | :---: | :---: | :---: | :---: |
| Dev 1 | 14 | 10 | 10 | 34 |
| Dev 2 | 13 | 11 | 9 | 33 |
| Dev 3 | 14 | 10 | 10 | 34 |

## Caminho crítico

```text
S1-01 → S1-05 → S1-08         (biblioteca → configurar exercícios no plano)
S1-01 → S1-09 → S1-11 → S1-08 (planos → editor → configurar exercícios)
S2-01 → S2-02 → S2-03 → S2-06 (contrato da sessão → tela → interações → teste de morte de processo)
S3-01 → S3-02 → S3-04 → S3-10 (histórico → detalhe → teste E2E → demonstração)
```

A **S1-01** bloqueia todo o desenvolvimento de telas. Dev 1 deve terminá-la nos primeiros 1 ou 2 dias da Sprint 1. Enquanto isso, Dev 2 e Dev 3 podem prototipar as telas no Preview do Compose.
