# S7 — navegação e usabilidade

Plano B aprovado pelo responsável em 25/10/2026: 33 pontos e 6 de reserva. Base `902a809d903e924ace6ba1f8ea773726b1c18072`, PR #15 integrado. Dev 1/2/3 são responsabilidades técnicas, sem pressupor execução simultânea. Branch `feature/S7-01-navegacao-e-usabilidade`.

## Contratos

- Um NavHost/Controller, rotas tipadas e cinco raízes: Início, Planos, Exercícios, Histórico e Ajustes (título Configurações). Seleção pela rota; reseleção sem efeito. Troca salva/restaura estado sem duplicar destinos. Voltar das raízes secundárias salva seu contexto e retorna ao Início; no Início, saída normal do Android.
- Barra somente nas raízes, oculta com IME aberto. Fluxos internos mantêm Voltar. Modais, comandos pendentes e todas as etapas protegidas de backup/SAF impedem troca de aba. Não confirmar formulários nem cancelar operações implicitamente.
- Busca/filtro em SavedStateHandle; rolagens salvas por entrada. Troca de tema não recria navegação. Geração substituída limpa pilhas salvas e ativa, liberando ViewModels/participantes antigos; sucesso e rollback retornam Início → Configurações. Cancelamento anterior à substituição preserva contexto.
- Insets da barra externa são aplicados/consumidos antes dos scaffolds das telas. FAB e listas reservam espaço próprio; teclado não cobre os campos finais.

## Séries adaptativas

Referência anterior preservada localmente em `build/s7-reference/SessionScreen.kt`, cópia do commit base (artefato ignorado). Medição/captura executada deve constar no registro de implementação; a cópia do fonte não comprova medição.

Esboços de organização (ordem de leitura: ordinal, campos, concluir, remover):

```text
Exercício                         histórico  borracha
prescrição · descanso
1   [Carga kg] [Repetições]        [check] [remover]
2   [Carga kg] [Repetições]        [check] [remover]
Adicionar série

Largura reduzida / fonte ampliada:
1   [Carga kg] [Repetições]
                         [check] [remover]
Campos empilhados quando não couberem com 100 dp cada.
```

Manter filhos lazy/chaves por ID e os mesmos nós de campos ao reorganizar medida/posição. Alvos ≥48 dp, campos Material legíveis, remoção direta somente de pendentes. Checkbox identifica estado sem depender da cor. Erros/incompletude explícitos. Dica de carga uma vez na sessão. Não alterar debounce, foco intencional, automação, mutexes ou comandos de dados. Meta de altura: ≤80% do bloco anterior em 360×800 dp, fonte padrão, seis séries sem erros; depende de medição real.

## Identidade e movimento

Espaços 4/8/12/16/24 dp, margem 16 dp, cards equivalentes com raio 12 dp. Preservar paleta, cores dinâmicas e temas. Componentes pequenos para seção, vazio, carregamento e erro recuperável. Não redesenhar formulários nem mudar formatos/agrupamentos dos históricos.

Fade de 120 ms somente entre raízes diferentes; cor da conclusão em 150 ms. Sem animar altura, foco, remoção, inserção ou descanso. Estado inicial/restaurado sem efeitos; animações seguem a escala do sistema e não condicionam escrita/habilitação.

## Dados e limites

Room v3 e backup público v2 preservados, sem migração, nova preferência ou contrato de repositório. Recuperação privada S5, journal/undo e isolamento temporal intactos. Estatísticas, kg/lb, distância, fotos, preferências por exercício e navigation rail fora da S7. Reserva para correções, não ampliação; microinterações são o primeiro candidato a adiamento, sem cortar testes/acessibilidade.

## Entrega

Commits lógicos em português, testes pertinentes, revisão técnica e CI do HEAD final antes do relatório. Responsável homologa e faz merge manualmente. Sem auto-merge/release. Homologação individual final S6 informada no contexto fornecido pelo responsável para este planejamento; atualizar registros sem presumir aprovação coletiva ou testes humanos não relatados.

## Consistência do cadastro — 26/10/2026

A criação pela raiz Exercícios usa a mesma `ExerciseCreateRoute`/`ExerciseCreateScreen` do seletor do plano, com os mesmos campos, validação e `ExerciseCreateViewModel`. O diálogo de edição de exercícios existentes permanece. Não há nova tela, contrato de domínio ou mudança de persistência.

O retorno usa a entrada anterior: somente `PlanEditorRoute` recebe `createdExerciseId`, mantendo a pesquisa pelo nome e a seleção/prescrição explícitas. A biblioteca observa o repositório e conserva sua busca/filtro sem receber esse resultado; um filtro já ativo pode ocultar o novo item até ser removido. Cancelar/Voltar não grava e retorna à origem. Admissão da navegação exige entrada atual retomada; callback de sucesso ignora entrada que já foi removida, evitando pop duplicado. Proteções de submissão existentes continuam no ViewModel.
