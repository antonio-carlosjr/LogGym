# Homologação manual da Sprint 3

Registrar data real da execução, responsável, versão/commit, dispositivo/API, resultado por caso e captura ou log quando disponível. Os testes automatizados não representam aceite humano. Não registrar um cenário como aprovado sem executá-lo.

Use um plano ativo com um exercício de carga/repetições e outro de tempo manual, ambos com três séries e descanso de 15 s. Preserve os dados entre os casos. A assinatura final deve ser testada em ambiente separado da instalação de debug.

| Caso | Ação | Passa quando | Referência |
| --- | --- | --- | --- |
| Histórico vazio | Antes de finalizar o primeiro treino, abrir Histórico | Exibe estado vazio, com retorno à Home; nenhum rascunho listado | S3-01; RF-116 a RF-119 |
| Treino misto parcial | Iniciar; registrar carga `0`, reps `0`, duração `45`; marcar uma série por tipo e uma série vazia; deixar outras pendentes | Valores zero aceitos; incompletude avisada; check continua manual | S3-02/04; regressão S2 |
| Estado da série | Marcar, desmarcar e marcar novamente | Texto e check identificam o estado, além da cor; campos marcados bloqueados; aviso de campos vazios permanece | S3-07/08 |
| Descanso fixo | Após check, rolar até o fim da lista; testar teclado aberto | Faixa de descanso permanece visível abaixo da barra; Pular acessível, sem cobrir campos | S3-UX-01 |
| Pular | Tocar Pular com descanso ativo | Faixa desaparece e não retorna sem novo check | RF-92; S3-UX-01 |
| Fim do descanso | Fazer novo check e esperar o fim | Exibe “Descanso concluído” por aproximadamente 5 s e depois oculta | S3-UX-01 |
| Recuperação mista | Salvar valores, enviar ao fundo, encerrar processo e reabrir | Mesmo rascunho, valores e checks; tempo calculado pelo instante final; treino não finalizado | S2-06; regressão S3 |
| Retomada durante o aviso final | Voltar e retomar antes de completar 5 s do fim do descanso | Mostra somente o restante da janela original; não inicia mais 5 s a partir da retomada | S3-UX-01 |
| Retomada com descanso antigo | Reabrir mais de 5 s depois do término | Aviso antigo não reaparece; próximo check inicia nova contagem | S3-UX-01 |
| Finalização | Finalizar, cancelar, depois confirmar | Cancelamento preserva rascunho; confirmação informa pendentes/incompletas e salva apenas marcadas | S3-04; RF-107 a RF-115 |
| Lista e detalhe | Abrir Histórico e selecionar o treino | Data local, plano, divisão e duração; detalhe somente leitura com séries de ambos os tipos; zero e “não informado” distintos | S3-01/02 |
| Pré-preenchimento | Iniciar novamente a mesma divisão; conferir valores e descartar esse novo rascunho com confirmação | Valores salvos são sugeridos, séries começam desmarcadas; rascunho não entra no histórico | Regressão S2 |
| Seleção visual | Abrir seleção de exercícios no editor em tema claro e escuro | Linhas usam a superfície do diálogo, sem fundo branco destoante | S3-07 |
| Temas e fonte | Conferir claro, escuro, cores dinâmicas e fonte de sistema ampliada, especialmente o card “Treino em andamento” e a faixa de descanso | Textos e ações legíveis sobre superfícies neutras do tema; sem corte de ações ou sobreposição; listas rolam para todos os campos | S3-07/08 |
| TalkBack | Percorrer Home, plano, sessão, histórico e detalhe | Ações e estados anunciados; checkbox com exercício/série; ordem de foco compreensível; indicador não anuncia cada segundo automaticamente | S3-08 |
| Isolamento | Renomear/excluir os exercícios e alterar/excluir o plano; consultar o treino concluído | Nomes, metas e execução históricos permanecem iguais | S3-03; RF-123 a RF-129 |
| Release | Instalar APK assinado em AVD separado e repetir o fluxo offline | Assinatura verificada, ícone presente, app abre sem rede e mantém dados após reabertura | S3-05/10 |

“Don't keep activities” verifica recriação de Activity, não morte de processo. Para processo, enviar o app ao fundo e usar `adb -s <serial> shell am kill com.loggym`; confirmar que o PID desapareceu antes de reabrir. Para interrupção abrupta, `am force-stop` exige reabertura pelo launcher. Valores ainda não confirmados pelo debounce podem ser perdidos; valores confirmados e o rascunho devem sobreviver. O roteiro detalhado anterior continua em [S2-06](reunioes/S2-06-recuperacao.md).

Falhas de carregamento e de escrita também têm testes automatizados. Se aparecer erro durante o uso, confirmar que o formulário permanece preenchido e que repetir a ação funciona; não provocar corrupção do banco para simular essa condição manualmente.

A reabertura pode restaurar diretamente a rota da sessão. Nesse caso, conferir os dados, usar Voltar e salvar para acessar a Home e validar Retomar; não exigir que a navegação seja reiniciada para considerar a recuperação aprovada.

## Validação individual informada — 24/10/2026

O usuário confirmou a aprovação da validação manual das últimas correções no emulador:

| Comportamento explicitamente confirmado | Resultado relatado |
| --- | --- |
| Card “Treino em andamento” no tema escuro | Aparência aprovada |
| Faixa fixa de descanso no tema escuro | Aparência aprovada |
| Aviso “Descanso concluído” | Desaparece corretamente após aproximadamente 5 segundos |
| Funcionamento geral efetivamente testado | Correto; nenhum defeito bloqueante identificado |

Responsável pelo relato e validação: usuário. Data de recebimento/registro: 24/10/2026. Versão descrita: revisão local da Sprint 3 com superfícies neutras e aviso de 5 segundos; hash do APK, dispositivo/API, data/hora exata da execução e capturas não foram informados. A confirmação geral anterior da Sprint 3 também não discriminou todos os cenários executados; os casos do roteiro acima não recebem aprovação automática.

O usuário autorizou o encerramento técnico e a integração pelo PR #12 condicionados às regras do repositório. Em 24/10/2026, aprovou expressamente esse PR e substituiu a antiga exigência interna de revisão independente pela aprovação do responsável na conversa, mantendo revisão técnica do Codex, testes, CI e proteções efetivas. Essa autorização cobre a atualização da política na mesma branch; não autoriza merge de PRs futuros. O resultado e o hash integrado ficam registrados no [PR #12](https://github.com/antonio-carlosjr/LogGym/pull/12).

Aprovação coletiva, TalkBack, avaliação abrangente de acessibilidade, assinatura definitiva e ensaio humano permanecem separados deste aceite funcional individual e não foram confirmados por esse relato. A ausência de aceite coletivo não é requisito impeditivo de integração pela nova política. Consulte o [estado da implementação](S3-IMPLEMENTACAO.md#pendências-distintas-da-implementação).
