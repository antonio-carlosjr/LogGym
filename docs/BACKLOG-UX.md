# Backlog de experiência do usuário

## UX-NAV-01 — navegação principal inferior

**Registro:** 25/10/2026, solicitação do responsável pelo projeto. **Estado:** proposta para refinamento, sem implementação autorizada. É uma nova funcionalidade de UX; não é defeito da Sprint 4 nem parte de seus 32 pontos. A referência visual informada foi o padrão de navegação inferior de apps como o LiftLog.

**Objetivo:** avaliar substituir os botões de navegação principal na lista da Home por uma barra inferior, tornando os destinos principais acessíveis de forma consistente.

### Enquadramento e decisões antes da implementação

A candidatura passa à **Sprint 7**, fora do compromisso e da reserva da Sprint 6. O backup da S5 foi entregue após refinamento para 34 pontos, sem criptografia e com aviso claro. A ordem poderá ser revista pelo responsável após refinamento; este registro não aprova execução nem altera os demais itens do backlog.

- Destinos propostos para avaliação: Início, Planos, Exercícios e Histórico. Recomenda-se manter Configurações como ação na barra superior, evitando uma quinta aba inicialmente. Confirmar quantidade, nomes, ordem e acesso às configurações com o responsável.
- Recomenda-se mostrar a barra apenas nos destinos principais; onboarding, execução, editores, cadastro contextual e detalhes manteriam navegação focada com Voltar. Confirmar em quais telas a barra aparece antes de implementar.
- Definir se cada aba retoma seu contexto ou volta à raiz, como o Voltar do sistema percorre a navegação e o que acontece ao selecionar novamente a aba atual. Proposta inicial: restaurar busca/rolagem de cada destino principal, evitar entradas duplicadas e fazer Voltar de um fluxo interno retornar ao seu destino de origem.
- Definir comportamento com teclado aberto, paisagem, fonte ampliada e recriação. Uma adaptação para navigation rail em telas grandes exige refinamento próprio, sem inclusão automática.
- Aprovar esboço/contratos e critérios de navegação antes de mudar a Home. A referência a outro aplicativo não define sozinha essas decisões de produto.

### Base existente e impacto técnico previsto

- `HomeScreen` hoje reúne plano ativo, rascunho e botões para Planos, Exercícios, Histórico e Configurações dentro de uma lista. Preservar o conteúdo de treino e as ações Iniciar/Retomar/Descartar; reorganizar somente o acesso aos destinos aprovados.
- `LogGymNavHost` usa um único `NavHostController` e rotas tipadas em `Routes.kt`. Reutilizar esses contratos e o Material 3 já presente no projeto; avaliar `NavigationBar`/`NavigationBarItem` em uma estrutura comum aos destinos principais, sem criar controladores paralelos por padrão.
- Derivar a aba selecionada da rota efetiva. Planejar navegação sem duplicações e restauração de estado usando os mecanismos do controlador existente; não introduzir um índice de aba independente da pilha real.
- As telas possuem seus próprios `Scaffold`s. Coordenar barra compartilhada, barras superiores, padding e insets uma única vez para evitar espaços duplicados, sobreposição com o teclado ou com a navegação do sistema. Preservar tema e tratamento das barras em `MainActivity`/`LogGymApp`.
- Preservar o escopo dos ViewModels e os estados de busca/rolagem/formulário, inclusive `SavedStateHandle` do seletor/cadastro. A troca de aba não deve criar novos rascunhos, reinicializar preferências ou perder contexto do plano.
- A execução continua usando as saídas existentes de `SessionViewModel`, que coordenam buffers, debounce e gravações. Não oferecer troca direta de aba que contorne salvamento/erros da sessão. A faixa de descanso e o aviso final de cerca de 5 s mantêm seus timestamps.
- **Room:** nenhuma mudança de entidade, repositório de dados ou migração prevista. **DataStore:** nenhuma nova preferência necessária para a barra; a escolha de aba não vira configuração persistente automaticamente. Reavaliar esses impactos se as decisões de produto ampliarem o escopo.

### Decomposição e estimativa inicial (histórico)

Revisão no planejamento da S6: **13 pontos**, candidata à S7, sem implementação autorizada. O responsável escolheu cinco abas — Início, Planos, Exercícios, Histórico e Configurações — e Voltar das raízes para Início. Barra somente nas raízes; fluxos internos permanecem focados. A reestimativa considera estados de Configurações/backup e descarte de todas as pilhas salvas na troca de geração. A tabela abaixo preserva a candidatura inicial de 11 pontos; a divisão da S7 será refinada em planejamento próprio. A S6 aprovada contém apenas histórico e automação, 27 pontos e 12 de reserva.

| Candidato | Entrega verificável | Responsabilidade técnica proposta | Pontos |
| --- | --- | --- | ---: |
| UX-NAV-01A | Refinar destinos/Voltar/estado e aprovar esboços e contrato de visibilidade | Dev 3 | 3 |
| UX-NAV-01B | Integrar estrutura comum e rotas, preservando estados e fluxos existentes | Dev 2 | 5 |
| UX-NAV-01C | Regressão integrada, acessibilidade, documentação e homologação | Dev 1, com Dev 3 na revisão visual | 3 |
| **Total preliminar** | Estimativa inclui testes; depende das decisões acima | Papéis técnicos, sem pressupor disponibilidade simultânea | **11** |

Confirmar capacidade e reestimar após o refinamento. Não retirar testes nem deslocar entregas aprovadas de S4/S5 para acomodar esta proposta.

### Critérios de aceite propostos

1. Cada item abre o destino correto, indica seleção pela rota e mantém ícone e rótulo compreensíveis. Repetir a seleção não duplica telas.
2. Voltar, troca de abas, recriação e retorno de editores/cadastro respeitam o contrato aprovado, preservando busca, rolagem e divisão de origem. Onboarding continua concluído uma única vez.
3. Iniciar/Retomar/Descartar, limpeza, finalização parcial e histórico mantêm os comportamentos homologados. Nenhum caminho ignora falhas de salvamento ou recria o treino em andamento.
4. Barra e conteúdo respeitam teclado/insets e não cobrem campos, mensagens ou ações. Temas claro/escuro, cores dinâmicas, tela compacta, paisagem e fonte ampliada permanecem utilizáveis.
5. Alvos de toque de pelo menos 48 dp, rótulos textuais, seleção anunciada e ordem de foco coerente no TalkBack. Estado não depende só de cor; verificar contraste e ausência de foco duplicado.
6. Banco e preferências existentes permanecem intactos; eventual necessidade real de persistência adicional exige decisão e testes próprios antes da integração.

### Verificações previstas

- JVM: regras de seleção/visibilidade e tratamento de erros, caso sejam extraídas regras próprias; verificar retenção de estado dos ViewModels afetados sem criar testes que apenas repitam callbacks triviais.
- Compose/instrumentados: percorrer todos os destinos, repetir seleção, usar Voltar, restaurar busca/rolagem, recriar Activity e retornar do cadastro contextual à mesma divisão. Reexecutar os fluxos S3/S4 de treino misto, limpeza, finalização parcial e histórico.
- Persistência/recuperação: verificar treino existente antes/depois das transições e recuperação após morte do processo, incluindo valores zero/nulos/tempo, descanso por timestamp e preferência de pré-preenchimento aplicada somente a novas sessões.
- Manual: TalkBack, fonte ampliada, temas/cores dinâmicas, teclado, orientação, área de gestos e navegação do sistema. Registrar cada resultado humano separadamente; nenhum desses testes foi executado por este planejamento.
- Build, JVM, lint, instrumentação e CI do commit entregue, seguidos de homologação individual e aprovação explícita do responsável antes do merge.

Histórico individual, marcação automática, compactação dos cards, pré-preenchimento por exercício, fotos, distância e kg/lb continuam separados deste item. Navegação inferior não implementa essas funcionalidades implicitamente.


## Refinamento aprovado na S7 — 25/10/2026

UX-NAV-01 incorporada ao plano B autorizado: cinco raízes, rótulo Ajustes/título Configurações, Voltar secundário ao Início, estado preservado e invalidação de pilhas após restauração. As recomendações iniciais acima ficam como histórico. Compromisso total S7: 33 pontos e 6 de reserva, incluindo linhas compactas, padronização pontual, duas microinterações e regressão. [Contrato vigente](decisoes/S7-usabilidade.md).

Estatísticas candidatas à S8, internas à aba Histórico e sem sexta aba, dependem de planejamento próprio. Preferências por exercício, distância, kg/lb e fotos seguem separados.

## Encerramento da entrega S7 — 27/10/2026

UX-NAV-01 foi entregue na S7, integrada pelo PR #16 e homologada individualmente pelo responsável, incluindo o ajuste de cadastro compartilhado. [Evidências e limites](S7-IMPLEMENTACAO.md). As candidaturas e estimativas anteriores permanecem como histórico; demais itens do backlog não são automaticamente aprovados ou concluídos por este encerramento.
