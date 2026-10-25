# Homologação manual da Sprint 4

Todos os casos abaixo estão **pendentes de confirmação humana**. Registrar data, responsável, commit/APK, dispositivo/API, resultado por caso e evidência disponível. Automação/CI não substituem esse registro.

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
