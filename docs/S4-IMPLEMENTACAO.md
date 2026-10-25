# Sprint 4 — implementação e evidências

## Escopo aprovado e estado

Planejamento e ajustes aprovados pelo responsável em 24/10/2026. Pacote de 32 pontos (Dev 1: 10, Dev 2: 11, Dev 3: 11) e reserva de 7 sobre referência provisória de 39. Papéis são áreas técnicas; não pressupor três pessoas simultâneas nem converter pontos em prazo garantido. A janela organizacional de duas semanas não reduz critérios de qualidade.

Código implementado e enviado na branch `feature/S4-01-prescricao-e-configuracoes`, com commits separados por entrega e [PR #13](https://github.com/antonio-carlosjr/LogGym/pull/13) consolidado. **Implementação e homologação funcional individual concluídas:** em 25/10/2026, o responsável confirmou aprovação de descanso manual, repetições fixas/faixa, cadastro contextual, borracha, tema, preferência de pré-preenchimento e regressão do fluxo completo, sem defeitos bloqueantes identificados. Evidência e limites em [HOMOLOGACAO-S4.md](HOMOLOGACAO-S4.md).

**Integração autorizada expressamente pelo responsável em 25/10/2026**, após homologação funcional individual e CI aprovada em `9fb6366`. A autorização abrange o merge commit do PR #13, a sincronização da `main`, a verificação da CI pós-merge e este registro documental de encerramento. Não autoriza publicação de release nem implementação da Sprint 5.

O [registro de integração do PR #13](https://github.com/antonio-carlosjr/LogGym/pull/13) consolida a execução efetiva, o HEAD integrado, o hash do merge, a conferência de ancestralidade e a CI pós-merge. A integração está concluída quando o PR indicar `MERGED`; seu encerramento técnico inclui a confirmação desses resultados no registro, sem antecipar uma operação ainda não executada. Aceite de outros integrantes e critérios humanos/release não confirmados permanecem separados, sem atribuição de aprovação coletiva. Nenhuma release publicada.

| Tarefa | Componentes e comportamento |
| --- | --- |
| S4-02 | `TargetForm`, `ExerciseTarget`, editor: descanso textual validado; 0–600 inteiros; ±15 sem arredondar; vazio → 0; inválido não salva |
| S4-01 | `RepetitionMode` somente na UI; fixo gravado como mínimo=máximo; faixa com mínimo vazio →0 e máximo positivo; resumo/reabertura compatíveis |
| S4-07 | `UserSettings`, `ThemeMode`, DataStore, `SettingsViewModel/Screen`, raiz e MainActivity: tema persistente e barras acompanhando tema efetivo, sem recriar NavHost |
| S4-08 | `SessionRepositoryImpl`: leitura única antes da transação; revalidação de rascunho; flag padrão true; nenhum efeito em retomadas/limpeza |
| S4-03 | `ExerciseEditorFields` compartilhado; `ExerciseCreateViewModel/Screen`; resultado por SavedStateHandle; contexto/posição preservados no cancelamento; salvar busca pelo novo nome, sem inserir prescrição |
| S4-05 | `SessionRepository.clearExerciseSets`, DAO transacional, ViewModel/mutex e ícone: limpa somente ocorrência DRAFT, preservando IDs/ordem/quantidade e descanso; ignora emissões antigas |
| S4-09 | Testes de regras, repositório, DataStore, ViewModels e fluxo Compose; documentos, roteiro e registro da homologação funcional individual dos sete grupos |

## Contratos e compatibilidade

Room permanece **v2**: nenhuma entidade, coluna ou migração adicionada. Somente nova operação de UPDATE sobre as séries. Sessões concluídas são protegidas no repositório; referências e snapshots históricos continuam independentes dos planos.

`UserPreferencesRepository` preserva onboarding e acrescenta configurações observáveis/setters de tema e pré-preenchimento. Ausência de chave usa sistema/true; erros não são tratados como ausência. A raiz conserva destino inicial e NavHost; mudanças de tema recompõem conteúdo e barras, preservando ViewModels. Cores dinâmicas existentes mantidas.

Criar rascunho captura a flag uma única vez fora da transação e a aplica a todos os exercícios. Falha de leitura não cria dados parciais; rascunho existente prevalece sem leitura/reaplicação. Extras ainda copiam a última série do treino atual.

Limpeza cancela debounces somente do exercício, aguarda o mutex/gravação em andamento, cancela e aguarda observação antiga, faz commit e atualiza buffers/snapshot local antes de reobservar. Não valida nem salva os valores a apagar. Erro mantém buffers e confirmação, permite retry e reinicia observação. Transação não escreve no descanso, plano ou histórico. Cancelar não limpa. Nenhuma operação de check automático introduzida.

Cadastro usa os mesmos campos, validações de repositório e mapeamento de erros da biblioteca. Campos e contexto simples ficam no SavedStateHandle. Resultado é consumido uma vez pela navegação; biblioteca observada resolve nome/ID e a prescrição só abre após seleção do usuário.

## Verificação

Verificação local em 24/10/2026: build debug e APK instrumentado aprovados; **132 testes JVM e 24 instrumentados**, sem falhas, erros ou testes ignorados. Instrumentação executada no AVD isolado/read-only `Medium_Phone_API_37.0`, API 37, preservando os dados do AVD habitual. Lint sem erros; cinco avisos preexistentes sobre versões de dependências/Gradle. A referência anterior era 115 JVM e 18 instrumentados.

A revisão técnica conferiu escopo, transações, buffers/debounce, estados de erro, consumo de resultado da navegação e ausência de mudança de schema. A [CI da implementação](https://github.com/antonio-carlosjr/LogGym/actions/runs/40150749551) foi aprovada no commit `a0252b0`, incluindo build/JVM/lint e instrumentação API 35. As alterações documentais de encerramento passam por nova CI; seu HEAD e resultado final são registrados no PR consolidado. Evidência técnica automatizada e homologação individual são registros distintos.

Smoke técnico no AVD isolado: tela de configurações inspecionada em tema escuro, com barras legíveis; tema escuro e pré-preenchimento desativado permaneceram após encerrar/reabrir o processo. Isso não declara homologação humana, TalkBack nem avaliação visual abrangente.

Falhas encontradas na verificação e corrigidas: import/callback ausentes na compilação dos testes; indentação do formulário compartilhado apontada pelo lint; teste integrado que concatenava ao valor padrão em vez de substituí-lo. O teste aguarda ações habilitadas e commits observáveis, sem atrasos arbitrários. Campo de descanso passou a ocupar a largura do diálogo, com ±15 abaixo para preservar espaço de leitura.

As duas primeiras execuções de instrumentação na CI passaram 23/24 cenários: o novo teste tocava o chip Tempo fora da área visível, com o teclado aberto. O diagnóstico de semântica e a reprodução local em 320×640 mostraram que a seleção não ocorria antes de recriar a Activity; não era perda de estado do formulário. O helper agora rola controles compostos até a área visível e confere a seleção antes/depois da recriação. Não foram removidas verificações nem alteradas regras do cadastro para contornar a falha. O resultado da CI final é registrado no PR.

Na execução seguinte, o fluxo S4 passou na API 35, mas o robô legado S3 falhou ao conferir três séries pendentes. Seu helper foi alinhado para tocar controles visíveis/habilitados e aguardar a persistência das três marcações antes de finalizar. As expectativas de finalização parcial, campos vazios e histórico foram preservadas integralmente.

Comandos: `assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest` e `connectedDebugAndroidTest` (ou runner direcionado explicitamente ao AVD isolado). Relatórios JVM/lint em `app/build/reports/`; logs de dispositivo em `app/build/s4-device/`, temporários e sujeitos à limpeza do build.

Testes novos cobrem 37±15, limites/entradas inválidas, fixo/faixa, persistência e falhas de preferências, navegação estável, criação/cancelamento/retorno, limpeza durante debounce/gravação/emissão antiga, rollback e snapshots. A limpeza de séries por tempo e extras também é verificada com duas ocorrências do mesmo exercício, preservando a outra ocorrência e o descanso. `Sprint4FlowTest` integra prescrição → cadastro → treino misto → limpeza → retomada → histórico → configurações → novo rascunho sem pré-preenchimento.

## Limites e pendências

- [Homologação funcional individual S4](HOMOLOGACAO-S4.md) aprovada nos sete grupos informados em 25/10/2026; não presumir execução de subcasos específicos não detalhados.
- Aprovação explícita para o merge do PR #13 recebida em 25/10/2026; reconferir o HEAD final, CI, conflitos e proteções antes do merge commit. O resultado efetivo fica no registro de integração do PR. Essa autorização não se estende a novos PRs.
- Backup fica na S5, **sem criptografia**, com aviso claro. Proteção por senha removida do escopo comprometido. Completo inclui histórico/rascunho; sem histórico não inclui sessões; importação substitui tudo após validação/confirmacão e recuperação segura.
- Histórico individual (S4-04), marcação automática (S4-06), compactação, pré-preenchimento por exercício, fotos, distância e kg/lb não implementados.
- TalkBack/avaliação abrangente, assinatura definitiva/custódia e ensaio humanos da S3 permanecem separados; não foram presumidos concluídos.
- Reorganização da navegação principal por barra inferior registrada como nova proposta [UX-NAV-01](BACKLOG-UX.md#ux-nav-01--navegação-principal-inferior). Não integra os 32 pontos da S4 e não foi implementada.
