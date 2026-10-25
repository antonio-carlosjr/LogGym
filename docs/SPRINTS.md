# Planejamento do MVP em 3 sprints

O ponto de partida é a estrutura inicial: domínio, banco Room, repositórios, onboarding e home. A partir dela, o restante da [especificação funcional](../especifica_o_funcional_loggym.md) (RF-01 a RF-148, com requisitos parciais e pendentes identificados na matriz de rastreabilidade) fica dividido em três sprints de **2 semanas**. A ordem segue o fluxo da seção 15: **prescrever → executar → consultar**.

A divisão das tarefas entre os integrantes está em [`TAREFAS.md`](TAREFAS.md).

## Já concluído (estrutura inicial)

- Carga inicial dos exercícios nativos e onboarding com templates (RF-01 a RF-08).
- Início de sessão pela home, aviso de treino em andamento, retomar e descartar (RF-58, RF-63, RF-64, RF-96, RF-101 a RF-104).
- Regras de domínio com testes unitários: validação de metas, pré-preenchimento das séries e cronômetro por timestamp.
- Repositórios com as transações de snapshot, finalização atômica e plano ativo único.

---

## Sprint 1: Fundação validada, biblioteca e planos ✅ concluída

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

## Sprint 2: Execução do treino — aceite individual formalizado; aceite coletivo pendente

Escopo fechado: S2-01 a S2-09 e **tempo manual**, sem distância/fotos. Contratos, rastreabilidade e critérios estão em [S2-IMPLEMENTACAO.md](S2-IMPLEMENTACAO.md). Build, 103 testes JVM e 14 instrumentados foram aprovados localmente em 24/10/2026; a CI remota do mesmo commit também passou. O usuário informou aprovação dos cenários manuais restantes, inclusive recuperação de treino misto, finalização e pré-preenchimento. A [S2-06](reunioes/S2-06-recuperacao.md) contém as evidências locais anteriores e distingue a confirmação adicional informada pelo usuário. **O usuário formalizou seu aceite individual; somente a aprovação coletiva permanece pendente e não bloqueia o desenvolvimento local da Sprint 3.**

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

Os dois critérios foram confirmados na validação manual informada pelo usuário em 24/10/2026. Isso registra o aceite individual, sem declarar homologação coletiva.

---

## Sprint 3: Histórico, robustez e entrega — código concluído; aceite funcional individual; integração aprovada pelo responsável

**Objetivo:** consultar o passado com isolamento garantido e entregar um MVP polido. As dez tarefas originais e S3-UX-01 compõem 32 pontos (Dev 1: 10, Dev 2: 11, Dev 3: 11), com reserva de 7 pontos sobre a capacidade conservadora de 39. Implementação, evidências e pendências estão em [S3-IMPLEMENTACAO.md](S3-IMPLEMENTACAO.md).

Em 24/10/2026, o usuário aprovou as correções escuras da Home e da faixa, o aviso de 5 segundos e o funcionamento que testou, sem bloqueantes. Aprovou explicitamente o PR #12 e seu merge, incluindo a nova política de aprovação pelo responsável na conversa. CI final verde e condições técnicas continuam obrigatórias; revisão independente não é mais requisito interno. O resultado efetivo e o hash da integração são consolidados no [PR #12](https://github.com/antonio-carlosjr/LogGym/pull/12). TalkBack, APK com assinatura definitiva e ensaio humano continuam pendentes; não registrar esses critérios nem publicação como realizados. Não há aceite coletivo atribuído à equipe.

| Item | RFs | Onde |
| :--- | :--- | :--- |
| Lista do histórico mostrando data, divisão e plano | RF-116 a RF-119 | `ui/history/HistoryScreen.kt` |
| Detalhe somente leitura com exercícios e séries (carga e repetições) | RF-120 a RF-122, RF-128 | `ui/history/HistoryDetailScreen.kt` |
| Testes de isolamento temporal: renomear ou excluir exercício e alterar ou excluir plano depois da sessão | RF-123 a RF-127, RF-129 | `app/src/androidTest/` |
| Polimento: estados vazios, mensagens de erro, textos em `strings.xml`, ícone do app, acessibilidade (rótulos e alvos ≥ 48 dp) | — | `res/`, telas |
| Testes de UI do fluxo principal: onboarding → iniciar → finalizar → histórico | seção 15 | `app/src/androidTest/.../ui/` |
| APK de release assinado, README atualizado e roteiro da demonstração | — | `app/build.gradle.kts`, `docs/` |

**Melhorias leves incluídas:** superfície coerente no seletor de exercícios; estado textual/visual de séries marcadas; faixa fixa do descanso; aviso de conclusão oculto após aproximadamente 5 s.

**Definição de pronto:**
- Todo RF está rastreado até o código ou um teste.
- O APK de release instala e funciona.
- O fluxo completo é demonstrado.

---

## Ampliações posteriores à Sprint 3

A Sprint 4 aprovada abaixo substitui a candidatura anterior de 29 pontos. Histórico individual e marcação automática permanecem adiados; os demais itens aprovados são rastreados nas tarefas, sem alterar o escopo histórico da Sprint 3.

## Fora do escopo

Seção 18 da especificação, mais a decisão pendente 4: alarme de descanso com o app fechado, gráficos e PRs, sincronização, supersets e RPE, edição do histórico.

## Riscos

- **O Sprint 1 é o mais pesado.** Se apertar, a reordenação de divisões e exercícios (RF-44, RF-48) passa para o início do Sprint 2.
- **Interrupções antes do commit:** a última digitação pode não sobreviver à janela de debounce. Validar recuperação conforme o roteiro S2-06.

## Sprint 4 — implementação e homologação funcional individual concluídas; integração autorizada

Pacote aprovado: descanso livre de 0 a 600 s mantendo ±15; repetições fixas/faixa; cadastro pela seleção; borracha por exercício; configurações de tema e pré-preenchimento. São 32 pontos e 7 de reserva sobre a referência provisória de 39; os papéis Dev 1/2/3 são áreas técnicas, sem presumir execução humana simultânea. Consulte [tarefas](TAREFAS.md#sprint-4--prescrição-cadastro-limpeza-e-configurações), [implementação](S4-IMPLEMENTACAO.md) e [homologação](HOMOLOGACAO-S4.md).

As tarefas S4-04/histórico individual e S4-06/marcação automática ficam no backlog; cards compactos, preferências por exercício, fotos, distância e kg/lb também continuam fora desta entrega. Não há migração Room: banco v2 e snapshots preservados. Merge final exige aprovação explícita após o resumo técnico e CI verde.

Em 25/10/2026, o responsável confirmou os sete grupos funcionais da Sprint 4, incluindo regressão completa, sem defeitos bloqueantes, e posteriormente autorizou expressamente o merge commit do PR #13, a sincronização e a CI pós-merge. Consulte os limites da evidência em [HOMOLOGACAO-S4.md](HOMOLOGACAO-S4.md). O [registro do PR #13](https://github.com/antonio-carlosjr/LogGym/pull/13) consolida o resultado da integração, o hash do merge e a CI pós-merge; a integração está concluída quando indicar `MERGED`. Não foi atribuído aceite coletivo. TalkBack, avaliação abrangente de acessibilidade, assinatura definitiva e ensaio humanos não foram confirmados por esse relato. Encerramento técnico é distinto dessas pendências e da entrega de release; a Sprint 5 não foi iniciada.

## Sprint 5 — proposta de backup sem criptografia

Backup completo (com histórico/rascunho) ou sem histórico (sem sessões), por substituição integral após validação e confirmação, com recuperação segura de falhas. Estimativa de 26 pontos. Por decisão do responsável, não incluir senha ou criptografia no escopo comprometido; apresentar aviso claro sobre os dados legíveis. Implementação reservada à S5, após refinamento próprio; não foi iniciada na S4.

## Proposta posterior — navegação principal inferior

[UX-NAV-01](BACKLOG-UX.md#ux-nav-01--navegação-principal-inferior) registra a nova melhoria de UX solicitada em 25/10/2026. Recomenda-se avaliar sua inclusão em uma Sprint 6, após o backup, sem alterar os compromissos de S4/S5. Estimativa preliminar de 11 pontos, sujeita a refinamento e aprovação; sem implementação ou alocação automática de capacidade. Definir destinos, comportamento de Voltar, preservação de estado e acessibilidade antes de executar.
