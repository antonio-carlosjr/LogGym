# Homologação manual da Sprint 4

## Homologação funcional individual aprovada — relato de 25/10/2026

O responsável pelo projeto informou nesta conversa que concluiu a homologação manual da Sprint 4, aprovou os comportamentos abaixo e não identificou defeitos bloqueantes.

| Grupo confirmado | Referência | Resultado informado |
| --- | --- | --- |
| Descanso manual | S4-02 | Aprovado individualmente |
| Repetições fixas e em faixa | S4-01 | Aprovado individualmente |
| Cadastro contextual | S4-03 | Aprovado individualmente |
| Borracha por exercício | S4-05 | Aprovado individualmente |
| Tema do aplicativo | S4-07 | Aprovado individualmente |
| Preferência de pré-preenchimento | S4-08 | Aprovado individualmente |
| Regressão do fluxo completo | S4-09 | Aprovado individualmente |

**Evidência:** declaração explícita do responsável nesta conversa, registrada em 25/10/2026. A última implementação disponibilizada era o commit `a0252b0adc02c0aac5eec9d8281a430e207ed9c8` do [PR #13](https://github.com/antonio-carlosjr/LogGym/pull/13). O relato não informa identificador exato do APK testado, dispositivo/API, capturas/logs ou data exata de execução; não atribuir esses dados ao usuário por inferência.

Esta aprovação abrange os sete grupos relatados. Não confirma individualmente cada subcaso do roteiro, nem TalkBack, avaliação abrangente de acessibilidade, assinatura definitiva, ensaio ou aceite de outros integrantes. As pendências humanas/release anteriores permanecem abertas. A política atual exige aprovação explícita para integrar: **o merge do PR #13 ainda não foi autorizado**.

## Roteiro detalhado de referência

O roteiro abaixo é preservado para rastreabilidade e verificações complementares. Seus subcasos não recebem aprovação individual por dedução do relato agregado. Em novas verificações, registrar data real, responsável, commit/APK, dispositivo/API, resultado por caso e evidência disponível. Automação/CI não substituem o registro humano.

Use uma divisão com exercícios de carga/repetições e tempo, três séries de cada, e histórico anterior com valores conhecidos. Preserve o mesmo plano para aproveitar os casos.

| Caso | Ação | Passa quando | Tarefa |
| --- | --- | --- | --- |
| Descanso livre | Digitar 37, usar +15 e −15; salvar/reabrir | Mostra 52 e 37; também 37−15=22; valores persistem | S4-02 |
| Limites e inválidos | Testar vazio, 0, 600, −1, 601 e 3,7 | Vazio/0 desativam; 600 aceito; inválidos não salvam nem viram outro inteiro; botões desabilitados no inválido | S4-02 |
| Fixo | Escolher Fixas e digitar 10 | Exibe 3×10, reabre fixo; vazio/zero inválidos | S4-01 |
| Faixa | Testar mínimo vazio/0 e máximo 12; depois máximo vazio/0 e mínimo maior | Vazio salvo como 0; inválidos rejeitados; 10–10 reabre fixo | S4-01 |
| Buffers de modo | Digitar nos dois modos e alternar sem salvar | Os dois rascunhos de campo são preservados; padrão só por escolha | S4-01 |
| Snapshot | Iniciar treino, depois alterar prescrição/descanso do plano | Rascunho mantém metas e descanso anteriores; próximos treinos usam novos valores | S4-01/02 |
| Cadastro cancelado | Buscar no seletor, rolar, cadastrar e cancelar | Retorna ao seletor da mesma divisão com busca/posição anteriores | S4-03 |
| Cadastro salvo | Criar personalizado por carga ou tempo, salvar | Volta buscando o novo nome; nada adicionado ao plano até selecionar/configurar | S4-03 |
| Cadastro inválido/recriado | Nome vazio/duplicado; recriar Activity com formulário preenchido | Erros recuperáveis; dados do formulário preservados; sem duplicação ao salvar | S4-03 |
| Tema | Escolher automático/claro/escuro; fechar e reabrir | Preferência persiste; automático acompanha sistema; forçados prevalecem e barras ficam legíveis | S4-07 |
| Navegação/tema | Mudar tema e retornar a treino existente; conferir Home e faixa | Mesma navegação/dados, contraste correto; descanso não reinicia | S4-07 |
| Pré-preenchimento | Desativar com rascunho existente, retomar e iniciar outro depois | Existente intacto; novo vazio/desmarcado; reativar afeta apenas próxima criação | S4-08 |
| Borracha cancelada | Preencher/marcar/extras e tocar borracha → Cancelar | Todos os dados mantidos | S4-05 |
| Borracha confirmada | Limpar um exercício misto com séries extras e descanso ativo | Valores nulos/checks desmarcados, mesmos IDs/ordem/quantidade; outros exercícios, descanso e plano intactos | S4-05 |
| Limpar entrada incompleta | Digitar dados incompletos/errados e limpar antes do debounce | A limpeza não exige corrigir/salvar; dados não reaparecem | S4-05 |
| Persistência da limpeza | Voltar/Home/Retomar, depois encerrar processo em segundo plano e reabrir | Dados apagados não voltam; descanso continua por timestamp; histórico anterior intacto | S4-05 |
| Fechamento integrado | Finalizar parcialmente, abrir histórico e iniciar novamente | Somente marcadas no histórico; campos nulos/zero distintos; pré-preenchimento respeita preferência | S4-09 |
| Acessibilidade visual | Fonte ampliada, teclado, claro/escuro e cores dinâmicas | Campos/erros/ações legíveis, botões acessíveis e sem sobreposição | S4-09 |

Para morte de processo, enviar ao fundo e usar `adb -s <serial> shell am kill com.loggym`, confirmando ausência de PID antes de reabrir. Recriar Activity não substitui esse teste. Digitação interrompida antes do commit mantém a limitação documentada do debounce; limpeza confirmada deve persistir.

Gravação em andamento, emissão antiga e falha de transação são validadas com gates/triggers nos testes automatizados; não corromper o banco real para tentar reproduzi-las manualmente. Aviso de descanso continua por cerca de 5 segundos após o término e não reinicia em retomadas.
