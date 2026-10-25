# S6 — histórico por identidade, conclusão intencional e backup vigente

Alternativa A aprovada pelo responsável em 25/10/2026: 27 pontos e 12 de reserva. Implementação não inclui navegação inferior. Papéis e tarefas: [S6-IMPLEMENTACAO](../S6-IMPLEMENTACAO.md).

## Histórico individual

Consulta somente leitura por `SessionExercise.exerciseId` original + `TrackingType`, sem join com nomes atuais da biblioteca. Referências fracas preservam acesso após exclusão; identidade ausente não recebe fallback por nome. Páginas de 25 sessões, cursor `(startedAt, sessionId)` decrescente, com todas as ocorrências correspondentes de uma sessão juntas. Consultas das ocorrências/séries são limitadas às sessões da página, em transação consistente, aproveitando os índices existentes.

Painel modal rolável sobre a execução, fechado por ação explícita, gesto ou Voltar. Um ViewModel de consulta por rota cancela/invalida respostas anteriores. Carregar mais não duplica requisições; erros conservam páginas carregadas. Rotação conserva alvo e rolagem; fechar reinicia a consulta na próxima abertura. A apresentação de séries é compartilhada com o detalhe geral, distinguindo nulo/zero/tempo e usando snapshots.

Abrir/fechar não chama flush, não exige corrigir entrada inválida, não muda a posição da execução nem escreve timestamps. A associação entre linhas focadas é interrompida antes de limpar foco/abrir o painel; fechamento não solicita foco programaticamente. O cronômetro continua derivado do Clock/timestamps existentes.

## Conclusão automática opcional

`auto_complete_enabled` no Preferences DataStore, padrão falso. Ausência de chave é falso; erro de leitura desabilita automação e apresenta retry sem impedir check manual. Mudar preferência não reescreve séries nem torna valores antigos elegíveis.

Edição efetiva é mudança real no texto de um campo aplicável. Uma alteração basta, mesmo que os demais valores venham do pré-preenchimento. Callback idêntico, seleção de texto, pré-preenchimento, cópia de extra, emissões de Room ou retomada não habilitam conclusão. Elegibilidade usa revisão própria, separada da revisão reconhecida pelo autossalvamento; persistir por debounce não a consome.

Eventos aceitos: Concluído no teclado de repetições/duração ou foco nos campos de outra série. Próximo entre carga e repetições não conclui. Perda de foco isolada, histórico, diálogos, navegação, rolagem, teclado recolhido e ciclo de vida não concluem. Carga/repetições preenchidas aceitam zero; duração preenchida deve ser positiva. Campos ausentes ou inválidos não geram check automático. Check manual incompleto continua permitido com os avisos existentes.

Operação sempre explícita `completed=true`, usando a transação idempotente existente. Série em conclusão é bloqueada individualmente; digitação no destino continua disponível. Eventos aceitos entram no registro do `PersistenceCoordinator`, aguardam mutex local e revalidam revisão/elegibilidade/preferência. Repetições não alternam check nem reiniciam descanso. Falha preserva buffers e permite nova edição/check manual, sem loop de repetição automática.

Desmarcação manual invalida a intenção anterior. Borracha invalida eventos pendentes da ocorrência imediatamente; aguarda gravações em andamento, cancela debounces e preserva seu tratamento de observação antiga. Uma gravação iniciada não é cancelada para simular rollback: a limpeza posterior prevalece sob o mutex. O flush salva valores sem gerar conclusão; comandos anteriormente aceitos são drenados antes da captura. Comandos de geração substituída são recusados.

## Backup público v2 e transição privada

Decisão expressa do responsável: sem retrocompatibilidade de arquivos públicos durante esta fase local de desenvolvimento. Formato público vigente é v2, com `autoCompleteEnabled` booleano obrigatório nas duas modalidades. Exportação não gera v1; importação rejeita v1 e versões desconhecidas com erro tipado/mensagem específica antes de substituir dados. Identificar versão precede exigir campos novos. Não há conversão nem herança silenciosa da configuração do destino.

Continuam: validação estrita de campos/tipos/duplicatas, catálogo, checksum/contagens, relações, IDs/pisos, limites de 64 MiB/arquivo, 1 MiB/registro e 500 mil registros; arquivo legível sem criptografia; confirmação de substituição integral; undo durável; atualização única DataStore; journal APPLYING/COMMITTED/ROLLED_BACK; recuperação idempotente anterior às telas normais e bloqueio quando impossível. A nova preferência participa da captura e do rollback.

Exceção restrita: undo privado v1 da S5 pode ser lido internamente para resolver journal pendente, após conferir UUID/caminho local e SHA registrado. Sua preferência automática ausente corresponde ao estado anterior desativado. Essa exceção não habilita importação pública v1. Novos undos usam v2. Journal mantém protocolo 1 e Room permanece esquema 3, sem novas entidades, índices ou migrações. Instalar a S6 não redefine os dados locais existentes.

## Evoluções fora do compromisso

UX-NAV-01 fica candidata à S7: cinco abas (Início, Planos, Exercícios, Histórico e Configurações); Voltar nas raízes retorna ao Início. Planejar invalidação de todas as pilhas salvas após restauração antes de executar. Compactação, preferências por exercício, fotos, distância e kg/lb permanecem no backlog.
