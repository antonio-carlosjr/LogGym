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
- **Pull requests:** Codex prepara a entrega; responsável homologa e realiza o merge manualmente no GitHub, conforme política vigente abaixo. Revisão independente é opcional internamente; verificações técnicas e proteções efetivas continuam obrigatórias.
- **Arquivos compartilhados:** mudanças em `domain/` ou em interfaces de repositório são avisadas à equipe antes, porque afetam as três áreas.
- **Rastreabilidade:** a descrição do PR cita os RFs atendidos.

### Aprovação e integração de Pull Requests

1. O Codex implementa na branch correspondente, sem incluir alterações fora do escopo autorizado.
2. Executa os testes automatizados e demais verificações pertinentes, tratando falhas antes da integração.
3. Cria ou atualiza o PR destinado à `main`, com rastreabilidade e pendências documentadas.
4. Realiza revisão técnica do diff e apresenta ao responsável um resumo das alterações, resultados dos testes e limitações.
5. **O responsável realiza a homologação funcional e o merge manualmente no GitHub.** O Codex entrega o PR e o relatório; não executa merge nem habilita auto-merge. Política vigente por decisão expressa do responsável em 25/10/2026.
6. A integração deve usar merge commit, com CI verde, HEAD conferido e proteções satisfeitas. Não usar force push, desativar proteções ou simular revisões.
7. Aprovação de planejamento autoriza somente o escopo delegado, não publicação de release. Homologação individual, aprovação coletiva, integração e entrega são registros distintos.
8. Preservar o resultado efetivo e hash de merge no PR. Sincronização local após integração depende do trabalho solicitado, preservando alterações locais alheias à entrega.

Revisão independente continua opcional internamente. Os registros abaixo descrevem autorizações históricas; não autorizam o Codex a integrar novos PRs pela política atual.

**Aplicação ao PR #12:** em 24/10/2026, o responsável aprovou expressamente o PR e seu merge, incluindo a atualização desta política na mesma branch. Essa autorização dispensa nova confirmação para esta integração, após a CI do commit atualizado e demais condições técnicas aprovadas. O resultado e o hash de merge são registrados no próprio PR.

---

## Sprint 1: Fundação e prescrição ✅ concluída

> Entregue nos PRs #1 a #10. O critério de pronto foi validado pela interface no emulador (`Sprint1DefinitionOfDoneTest`) e na JVM (`Sprint1FlowTest`).

| ID | Tarefa | RFs | Responsável | Pts | Depende de |
| :--- | :--- | :--- | :---: | :---: | :--- |
| S1-01 | ✅ Abrir no Android Studio, corrigir erros de compilação, versionar `gradlew` e `gradle-wrapper.jar`, atualizar as versões das libs | — | Dev 1 | 3 | — |
| S1-02 | ✅ CI no GitHub Actions: `testDebugUnitTest` + `assembleDebug` em cada PR | — | Dev 1 | 2 | S1-01 |
| S1-03 | ✅ Testes de persistência com Room em memória via Robolectric (rodam na JVM e na CI, sem emulador): seed sem duplicatas, plano ativo único, cascade de divisões e prescrições | RF-01, RF-02, RF-21, RF-28, RF-29 | Dev 1 | 5 | S1-01 |
| S1-04a | ✅ Reunião de decisão e registro em [`decisoes/S1-04.md`](decisoes/S1-04.md); seção 17 da especificação marcada como [D] | seção 17 | Dev 1 (conduz), todos | 1 | — |
| S1-04b | ✅ Código: `enum MuscleGroup`, `NativeExercises` (60) e `PlanTemplates` (4) finais, regras de entrada em `domain/`, nome único, observação e descrição. Adota as recomendações P2, P3, P4, P6, P8, P9 e P10 | RF-130 a RF-148 | Dev 1 | 2 | S1-04a |
| S1-04c | ✅ Testes do seed (grupos válidos, nomes únicos, templates dentro das regras) e das regras de entrada | RF-130 a RF-148 | Dev 1 | 1 | S1-04b |
| S1-05 | ✅ `ExerciseLibraryViewModel` + lista com busca e indicação de nativo/personalizado | RF-09 a RF-12 | Dev 2 | 3 | S1-01, S1-04b |
| S1-06 | ✅ Diálogo de criar/editar exercício personalizado, com nome obrigatório e grupo muscular opcional; nativos bloqueados | RF-13 a RF-15, RF-18 | Dev 2 | 3 | S1-05 |
| S1-07 | ✅ Excluir personalizado com confirmação e aviso quando usado em planos (`countPlanUsages`) | RF-16, RF-17, RF-19 a RF-21 | Dev 2 | 2 | S1-05 |
| S1-08 | ✅ No editor: seletor de exercício da biblioteca e formulário de séries, faixa de repetições e descanso (validação via `ExerciseTarget`) | RF-46, RF-49 a RF-57 | Dev 2 | 5 | S1-04b, S1-05, S1-11 |
| S1-09 | ✅ `PlansViewModel` + lista de planos com o ativo destacado; ativar e trocar o plano ativo | RF-22, RF-28 a RF-31 | Dev 3 | 3 | S1-01 |
| S1-10 | ✅ Criar plano personalizado (nome), criar a partir de template, excluir com confirmação | RF-23, RF-24, RF-26, RF-27, RF-33, RF-35 a RF-38 | Dev 3 | 3 | S1-09 |
| S1-11 | ✅ `PlanEditorViewModel` + editor: renomear plano; adicionar, renomear e excluir divisões | RF-25, RF-39 a RF-43 | Dev 3 | 5 | S1-09 |
| S1-12 | ✅ Reordenar divisões e exercícios da divisão; remover exercício da divisão | RF-44, RF-45, RF-47, RF-48 | Dev 3 | 3 | S1-11 |

**Carga:** Dev 1 = 14 · Dev 2 = 13 · Dev 3 = 14

> ⚠️ As decisões da S1-04 ampliam o escopo do MVP: tempo/distância, fotos dos nativos e kg/lb. As estimativas acima são as aprovadas na reunião. A proposta de reestimativa e as tarefas novas (cerca de +18 pontos) estão em [`decisoes/S1-04.md`](decisoes/S1-04.md#planejamento) e aguardam validação da equipe.

> Se a sprint apertar, a S1-12 passa para o início da Sprint 2. Dev 1 tem folga para ajudar na S1-08 ou na S1-12.

---

## Sprint 2: Execução do treino

Implementação detalhada e rastreabilidade: [S2-IMPLEMENTACAO.md](S2-IMPLEMENTACAO.md). S2-10 foi reduzida a tempo manual; distância segue pendente. Código S2-01 a S2-09 e S2-10 parcial implementado; build e 103 testes JVM/14 instrumentados aprovados localmente em 24/10/2026, com CI remota verde para o mesmo commit. O usuário informou aprovação dos testes manuais restantes, incluindo recuperação de treino misto, finalização e pré-preenchimento; a S2-06 tem as evidências locais anteriores no [roteiro de recuperação](reunioes/S2-06-recuperacao.md). **Aceite individual formalizado pelo usuário; somente aprovação coletiva pendente, sem bloquear o desenvolvimento local da Sprint 3.**

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

| S2-10 (parcial) | Tipo fixo na criação, metas e execução por tempo manual em segundos, banco v2/migração e pré-preenchimento | RF-136 parcial | Dev 1 + Dev 2 | Reestimar | S2-01 (contrato integrado) |

**Carga:** Dev 1 = 10 · Dev 2 = 11 · Dev 3 = 10

A carga acima é a estimativa original, sem a ampliação de tempo. Não representa a capacidade reestimada.

> A folga da sprint vai para revisão de PRs e pareamento na integração entre a tela e o cronômetro, que é o ponto mais arriscado.

---

## Sprint 3: Histórico e entrega

Implementação e situação de cada entrega em [S3-IMPLEMENTACAO.md](S3-IMPLEMENTACAO.md). Tarefas originais preservadas; melhorias leves incorporadas ao polimento e à S3-UX-01. Aceite funcional individual das correções finais informado pelo usuário em 24/10/2026, sem bloqueantes. O responsável aprovou expressamente o PR #12 e seu merge conforme a nova política, condicionados à CI do commit atualizado e às condições técnicas; consultar o registro de integração no PR. TalkBack, assinatura definitiva sob custódia da equipe e ensaio humano permanecem pendentes. Não foi atribuído aceite coletivo nem testes não relatados ao usuário; aprovação coletiva não é requisito interno para integrar o código pela política vigente.

| ID | Tarefa | RFs | Responsável | Pts | Depende de |
| :--- | :--- | :--- | :---: | :---: | :--- |
| S3-01 | `HistoryViewModel` + lista do histórico (data, divisão, plano, duração) | RF-116 a RF-119 | Dev 2 | 3 | — |
| S3-02 | Detalhe da sessão somente leitura: exercícios e séries com carga e repetições, usando os snapshots | RF-120 a RF-123, RF-128 | Dev 2 | 3 | S3-01 |
| S3-03 | Testes de isolamento temporal: renomear ou excluir exercício e alterar ou excluir plano depois da sessão | RF-124 a RF-127, RF-129 | Dev 1 | 3 | — |
| S3-04 | Testes de UI em Compose do fluxo principal: onboarding → iniciar → séries → finalizar → histórico | seção 15 | Dev 1 | 5 | S3-02 |
| S3-05 | Build de release assinado: keystore fora do repositório, `signingConfig`, APK gerado | — | Dev 1 | 2 | — |
| S3-06 | Mover todos os textos para `strings.xml`; estados vazios e mensagens de erro em todas as telas | — | Dev 3 | 3 | — |
| S3-07 | Ícone adaptativo/monocromático, revisão visual, superfície do seletor e estado textual/visual das séries | — | Dev 3 | 3 | — |
| S3-08 | Acessibilidade: `contentDescription`, alvos ≥ 48 dp, contraste, teste com TalkBack | — | Dev 3 | 3 | S3-06 |
| S3-09 | Matriz de rastreabilidade RF → código/teste e atualização do README | todos | Dev 2 | 3 | S3-02 |
| S3-10 | Roteiro de demonstração e ensaio da apresentação | — | Dev 3 (conduz), todos | 2 | S3-04 |
| S3-UX-01 | Descanso fixo e destacado; aviso final oculto cerca de 5 s após o timestamp final; testes de retomada | RF-87 a RF-95 | Dev 2 | 2 | Estado da sessão; S3-07 |

**Carga:** Dev 1 = 10 · Dev 2 = 11 · Dev 3 = 11 · Total = 32

**Reserva:** 7 pontos sobre a capacidade conservadora de 39 (13 por integrante), para integração, correções e homologação.

> A Sprint 3 tem folga intencional para correção de bugs encontrados nos testes e na demonstração.

A melhoria do aviso “Descanso concluído” foi aprovada e incorporada à S3-UX-01; não representou defeito bloqueante da Sprint 2.

---

## Resumo por pessoa

| Pessoa | Sprint 1 | Sprint 2 | Sprint 3 | Total |
| :--- | :---: | :---: | :---: | :---: |
| Dev 1 | 14 | 10 | 10 | 34 |
| Dev 2 | 13 | 11 | 11 | 35 |
| Dev 3 | 14 | 10 | 11 | 35 |

## Caminho crítico

```text
S1-01 → S1-05 → S1-08         (biblioteca → configurar exercícios no plano)
S1-01 → S1-09 → S1-11 → S1-08 (planos → editor → configurar exercícios)
S2-01 → S2-02 → S2-03 → S2-06 (contrato da sessão → tela → interações → teste de morte de processo)
S3-01 → S3-02 → S3-04 → S3-10 (histórico → detalhe → teste E2E → demonstração)
```

A **S1-01** bloqueia todo o desenvolvimento de telas. Dev 1 deve terminá-la nos primeiros 1 ou 2 dias da Sprint 1. Enquanto isso, Dev 2 e Dev 3 podem prototipar as telas no Preview do Compose.

## Sprint 4 — prescrição, cadastro, limpeza e configurações

Planejamento aprovado pelo responsável em 24/10/2026. Implementação autônoma, com commits separados por entrega e um PR consolidado na branch `feature/S4-01-prescricao-e-configuracoes`; merge final somente após aprovação explícita do responsável. Esta organização foi autorizada para a Sprint 4 e preserva a política de integração acima.

| ID | Entrega | Responsabilidade técnica | Pontos |
| --- | --- | --- | ---: |
| S4-02 | Descanso manual inteiro de 0 a 600 s, mantendo ±15 sem arredondar | Dev 3 | 3 |
| S4-01 | Prescrição fixa/faixa; mínimo vazio → 0; fixo gravado como extremos iguais | Dev 3 | 5 |
| S4-03 | Cadastro em tela própria a partir da seleção, com formulário/regras compartilhados | Dev 2 | 5 |
| S4-05 | Borracha transacional por ocorrência do exercício; cancelamento de debounce e reconciliação | Dev 1: 5; Dev 2: 3 | 8 |
| S4-07 | Configurações, tema automático/claro/escuro e persistência | Dev 1: 2; Dev 3: 3 | 5 |
| S4-08 | Preferência global de pré-preenchimento, somente na criação de novos rascunhos | Dev 1 | 3 |
| S4-09 | Regressão integrada, documentação e homologação | Dev 2 | 3 |
| **Total** | | **Dev 1: 10; Dev 2: 11; Dev 3: 11** | **32** |

Os papéis são áreas de responsabilidade, sem pressupor três profissionais simultâneos. Os 39 pontos são referência provisória, com 7 reservados para integração, testes e correções. Duas semanas são uma janela organizacional; esforço estimado, execução e agenda da homologação são distintos. A entrega técnica de 32 pontos da S3 é uma referência de calibração, não comprovação de velocidade constante. Não reduzir qualidade para cumprir prazo.

Contratos precedem telas; S4-02/S4-01 alteram o formulário compartilhado em sequência. Configurações reutilizam DataStore; pré-preenchimento é lido antes da transação de criação. Cadastro compartilha campos e validações existentes. Borracha usa o mutex da sessão e transação Room, sem flush dos valores a apagar; observação antiga é cancelada/aguardada e retomada após o commit. Banco permanece v2, sem migração.

Situação, testes e limites: [S4-IMPLEMENTACAO.md](S4-IMPLEMENTACAO.md). Aceite manual: [HOMOLOGACAO-S4.md](HOMOLOGACAO-S4.md). A implementação e a CI não equivalem à homologação individual ou à autorização de merge.

Em 25/10/2026, o responsável aprovou individualmente os sete grupos funcionais da S4, sem defeitos bloqueantes, e posteriormente autorizou expressamente o merge commit do PR #13, a sincronização local e o registro da CI pós-merge. Implementação e homologação funcional individual concluídas; o [registro de integração do PR #13](https://github.com/antonio-carlosjr/LogGym/pull/13) consolida execução efetiva, HEAD integrado, hash do merge, ancestralidade e CI. Não registrar aprovação coletiva ou cumprimento de critérios humanos/release não confirmados. A autorização cobre este encerramento documental; não autoriza novos PRs, publicação de release ou implementação da S5.

## Sprint 5 — backup e restauração (autorizada em 25/10/2026)

Compromisso refinado de **34 pontos e 5 de reserva**; substitui a candidatura de 26 após detalhar coordenação/flush e recuperação Room/DataStore. Sem senha/criptografia. Os papéis são áreas técnicas, sem pressupor três pessoas simultâneas. Execução autônoma em branch, commits lógicos e PR consolidado; responsável homologa e faz merge manualmente no GitHub.

| ID | Entrega | Pontos | Responsabilidade técnica |
| --- | --- | ---: | --- |
| S5-01 | JSONL versionado, modalidades, integridade e compatibilidade | 3 | Dev 1: 1; Dev 2: 2 |
| S5-02 | Coordenador, gerações, participantes, flush aguardável e captura consistente | 8 | Dev 1: 5; Dev 2: 3 |
| S5-03 | Staging privado, validação integral e resumo | 5 | Dev 2 |
| S5-04 | Migração/journal, aplicação, reversão e inicialização segura | 13 | Dev 1: 6; Dev 2: 2; Dev 3: 5 |
| S5-05 | Configurações/SAF, acessibilidade, regressão, AGENTS e documentação | 5 | Dev 3 |
| Total | Reserva adicional: 5 | **34** | **Dev 1: 12; Dev 2: 12; Dev 3: 10** |

Completo inclui histórico e rascunho; sem histórico não inclui sessões e remove as atuais ao restaurar, com aviso explícito. Ambas as modalidades aguardam edições válidas inclusive debounce; não confirmam formulários com Salvar explícito. Drenar participantes antes da exclusão global. IDs/snapshots/zeros/nulos preservados. Migração Room 2→3 acrescenta journal; undo durável permite reverter dados e preferências. Não presumir atomicidade conjunta Room/DataStore nem liberar telas com recuperação pendente. Checksum não autentica nem cifra. Contratos em [decisão S5](decisoes/S5-backup.md), resultados em [implementação](S5-IMPLEMENTACAO.md) e relato de aceite individual/refinamento final em [homologação](HOMOLOGACAO-S5.md).

## Backlog posterior preservado

- **S4-04:** candidatura original de 5 pontos incorporada à S6-01/02/03, reestimada em 8 pontos, com painel e paginação.
- **S4-06:** candidatura original de 8 pontos incorporada à S6-04 a S6-08, reestimada em 16 pontos com preferência, backup vigente e regressão; conferir regras atuais na decisão S6.
- Compactação dos cards (3 pontos), sem reduzir legibilidade ou alvos de toque.
- Pré-preenchimento por exercício (5–8 pontos a refinar), sem colunas/maps de override antecipados na S4.
- Fotos, distância e kg/lb continuam pendentes; configurações não os incluem automaticamente.
- **UX-NAV-01:** avaliar navegação principal inferior, com destinos, estado, Voltar e acessibilidade refinados antes da execução. Nova proposta de UX, fora da S4 e do backup comprometido para S5. Candidata à Sprint 6; referência preliminar de 11 pontos, sem aprovação de execução. Ver [BACKLOG-UX.md](BACKLOG-UX.md).

Essas expansões não ocupam automaticamente a reserva da S4 nem o pacote de backup da S5. Pendências humanas/release da S3 permanecem separadas.

## Sprint 6 — alternativa A autorizada

Base `35370e2`, integração do PR #14 e aprovação final da S5 confirmadas pelo responsável. Execução autônoma em nova branch com commits lógicos; homologação e merge manual no GitHub pelo responsável, sem publicação de release pelo Codex.

| ID | Entrega | Pontos | Área técnica |
| --- | --- | ---: | --- |
| S6-01 | Consulta histórica por identidade/tipo, 25 sessões por cursor | 3 | Dev 1 |
| S6-02 | Painel modal sobre a execução, preservando buffers/posição/descanso | 3 | Dev 2 |
| S6-03 | Testes de histórico, snapshots, ocorrências e paginação | 2 | Dev 2 |
| S6-04 | Preferência desativada por padrão, backup v2 e undo privado S5 | 3 | Dev 1 |
| S6-05 | Elegibilidade independente do debounce e conclusão idempotente | 5 | Dev 2 |
| S6-06 | Concluído ou foco em outra série; proteger histórico/blur | 3 | Dev 3 |
| S6-07 | Coordenação com borracha, flush e gerações | 3 | Dev 1 |
| S6-08 | Regressão da marcação e configurações | 2 | Dev 3 |
| S6-09 | Regressão combinada, documentação e homologação | 3 | Dev 3 |
| Total | Reserva adicional: 12 sobre referência de 39 | **27** | **Dev 1: 9; Dev 2: 10; Dev 3: 8** |

Responsabilidades técnicas, sem pressupor três profissionais simultâneos ou velocidade fixa em duas semanas. A preferência/formato passou de 5 para 3 pontos ao retirar importação pública v1, mantendo integralmente recuperação e testes do formato vigente. Contratos/critério de aceite em [decisão S6](decisoes/S6-execucao.md), resultados em [implementação](S6-IMPLEMENTACAO.md), [homologação](HOMOLOGACAO-S6.md) individual aprovada, com reteste visual final pendente.

UX-NAV-01 é candidata à S7 (13 pontos; referência anterior 11), com cinco abas e Voltar das raízes para Início. Não implementá-la nem usar a reserva automaticamente. Compactação, preferências individuais, fotos, distância e kg/lb permanecem separados.


## Sprint 7 — navegação e usabilidade autorizadas

Plano B aprovado em 25/10/2026: 33 pontos e 6 de reserva (referência 39). S7-01 contratos 3; S7-02 navegação 5; S7-03 estado/gerações 5; S7-04 linhas 5; S7-05 adaptação/acessibilidade 3; S7-06 padronização 5; S7-07 microinterações 2; S7-08 regressão/documentação 5. Dev 1: 10; Dev 2: 10; Dev 3: 13 — responsabilidades técnicas, não disponibilidade simultânea.

Cinco raízes com rótulo Ajustes/título Configurações, séries em linhas e remoção direta. Sem alterações de dados/formato. [Contratos](decisoes/S7-usabilidade.md), [resultados](S7-IMPLEMENTACAO.md) e [homologação pendente](HOMOLOGACAO-S7.md). A candidatura UX-NAV-01 anterior foi incorporada a este compromisso. S6 integrada/homologada individualmente, inclusive refinamento final, conforme contexto do responsável; registros históricos preservados.

Reserva para correções; não incorporar estatísticas, kg/lb, distância, fotos ou preferências por exercício. Codex implementa/testa/revisa e entrega PR; responsável homologa e faz merge manualmente.
