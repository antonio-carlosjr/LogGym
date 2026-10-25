# Homologação da Sprint 6

**Homologação funcional individual aprovada pelo responsável em 25/10/2026**, conforme relato na conversa, sem defeitos bloqueantes. Referência anterior aos refinamentos finais: commit `7831c64`, PR #15. Dispositivo/API e evidências adicionais não foram informados neste relato.

Foram aprovados os 11 grupos: configuração da marcação automática; pré-preenchimento e conclusão automática; teclado e troca de séries; valores zero e duração; desmarcação manual; histórico individual; histórico sem interferência; borracha e persistência; regressão da execução; backup v2; aparência e acessibilidade nos cenários efetivamente testados. Esse aceite não presume todos os subcenários do roteiro abaixo, TalkBack abrangente, testes controlados de interrupção/reversão ou aprovação coletiva.

**Reteste visual pendente:** painel com data sem horário, plano/divisão na mesma linha e divisor somente entre sessões. Conferir temas claro/escuro, nomes longos/ausentes, várias sessões e continuidade de campos, posição e descanso ao fechar. Os refinamentos foram solicitados após o aceite funcional e ainda não foram homologados pelo responsável. Merge manual e release permanecem separados.

## Preparação

Na S5, manter dados locais e uma restauração/treino de referência. Atualizar sem desinstalar, verificando preservação de planos, biblioteca, histórico, temas e pré-preenchimento. Não é necessário preservar importação pública de arquivos v1. Usar ambiente de teste para substituição de dados.

| Cenário | Ação e resultado esperado |
| --- | --- |
| Padrão e persistência | Nova opção começa desativada; check manual mantém comportamento atual. Ativar/desativar, reabrir o app e conferir preferência. Nenhuma série antiga muda. |
| Pré-preenchimento | Sem editar, usar Concluído/mudar de linha: não marca. Alterar apenas um campo com os demais válidos e concluir: marca. |
| Teclado e outra série | Próximo entre carga/repetições não marca. Concluído em repetições/duração ou entrar nos campos de outra série pode marcar. A série de destino continua editável. |
| Valores válidos/incompletos | Zero em carga/repetições é válido; duração positiva é válida. Duração zero, texto inválido ou campo ausente não concluem automaticamente; check manual incompleto continua com aviso. |
| Desmarcação e eventos | Desmarcar e apenas mudar foco/usar Concluído não remarca. Nova edição efetiva habilita outra conclusão. Não duplicar início do descanso. |
| Histórico e buffers | Abrir/fechar painel com valores válidos e inválidos: texto e posição da execução permanecem, sem checks novos. Voltar fecha o painel primeiro. |
| Histórico por identidade | Conferir datas, snapshots, zeros/ausências/tempo, ocorrências repetidas, nomes iguais com IDs diferentes, renomeação e exclusão. Carregar mais mantém ordem e não duplica sessões. |
| Descanso | Abrir/fechar/rolar histórico e rotacionar com descanso ativo: timestamp não reinicia. Aviso final mantém aproximadamente 5 s. |
| Borracha | Limpar uma ocorrência durante edição/salvamento: valores/checks não reaparecem; preservar outros exercícios, séries, plano, histórico e descanso. Cancelar não limpa. |
| Recuperação do treino | Rotacionar e encerrar processo: persistidos são recuperados; valores carregados não marcam por si só. Não prometer recuperar texto perdido antes do commit. |
| Backup vigente | Exportar/restaurar v2 completo e sem histórico, em instalações limpas/ocupadas, com opção automática ligada/desligada. Configurações e dados correspondem ao arquivo. |
| Versões incompatíveis | Importar v1 e versão desconhecida: mensagem clara, sem alteração dos dados atuais. |
| Recuperação interna | Em ambiente controlado, testar falhas/interrupções Room/DataStore e reversão. Undo S5 pendente continua recuperável; erro impede telas normais e conserva artefatos. Não confundir testes automatizados de fronteira com um encerramento aleatório do app. |
| Acessibilidade/visual | Temas claro/escuro/sistema, fonte ampliada, rolagem, teclado e TalkBack: ícones descritos, alvos ≥48 dp, painel fechável e feedback legível. |
| Fluxo combinado | Plano misto → pré-preenchimento → alteração de um campo → conclusão automática → histórico → limpeza → recuperação → finalização parcial → histórico geral → exportação/restauração v2. |

Pendências humanas anteriores de assinatura definitiva/custódia e ensaio permanecem separadas. Não atribuir avaliação abrangente de TalkBack sem confirmação. O responsável fará homologação e merge manualmente no GitHub; sem merge/release pelo Codex.
