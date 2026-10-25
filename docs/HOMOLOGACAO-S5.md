# Homologação da Sprint 5

Estado em 25/10/2026: **pendente de execução pelo responsável**. Implementação/testes automatizados não equivalem a aceite funcional individual. Não reutilizar a aprovação da Sprint 4 como aceite desta entrega.

Registrar para cada cenário: versão/commit, Android/AVD/aparelho, tema, data, responsável, resultado e evidência. Use instalação/AVD de teste e arquivos próprios; importar substitui todos os dados atuais. Nenhuma senha ou criptografia é oferecida.

## Preparação

Criar um plano misto, exercícios personalizados de carga e tempo, prescrição fixa/faixa e descanso de 37 s; adicionar observação a nativo. Concluir parcialmente uma sessão com zero, decimal, tempo e série marcada incompleta. Iniciar outra, adicionar extra, marcar uma série e manter descanso. Escolher tema escuro e desligar pré-preenchimento. Guardar evidência dos IDs/números, nomes, metas, valores, checks e histórico visíveis.

| # | Fazer | Resultado que define aprovação | Rastreabilidade |
| --- | --- | --- | --- |
| 1 | Abrir Configurações, rolar até Backup e restauração; conferir temas claro/escuro/sistema | Seção após preferências, duas ações, explicação das modalidades e aviso de arquivo legível; sem botão na Home | S5-05, RF-153/158 |
| 2 | Exportar completo; escolher destino pelo Android | Seletor só após preparação; sucesso apenas ao terminar; arquivo `.loggym` disponível, com tamanho não nulo | S5-01/02/05 |
| 3 | Alterar valores da sessão e solicitar exportação o mais cedo possível; restaurar esse arquivo | Últimos valores válidos recebidos pelo editor reaparecem, inclusive zero/decimal/tempo; nenhum check/descanso iniciado pelo flush | S5-02, RF-155 |
| 4 | Deixar entrada inválida, solicitar exportação e corrigir/tentar novamente | Sem seletor nem backup publicado no erro; texto permanece corrigível; tentativa posterior funciona | S5-02 |
| 5 | Exportar completo e sem histórico; importar cada um em instalação limpa (dois cenários) | Completo repõe plano/nativos/notas/personalizados/preferências/histórico/rascunho. Sem histórico repõe configuração sem qualquer sessão | S5-01/03/04, RF-154 |
| 6 | Importar cada modalidade em instalação já ocupada (dois cenários) | Resumo exibe modalidade, data, contagens e presença de rascunho; confirmação substitui tudo. Sem histórico avisa e remove histórico/rascunho anteriores | S5-03/05, RF-156 |
| 7 | Cancelar exportação no seletor e importação no seletor/resumo | Sem mensagem falsa de sucesso e sem substituição; autossalvamentos já concluídos continuam salvos; novas ações disponíveis | S5-05 |
| 8 | Importar arquivo aleatório, truncado, alterado, versão desconhecida ou acima do limite | Erro claro antes de tocar nos dados atuais; plano, histórico e preferências continuam iguais | S5-01/03 |
| 9 | Restaurar completo, retomar treino misto, conferir extras/ausentes/zeros/checks; finalizar e consultar histórico | Dados/snapshots iguais; descanso calculado pelos timestamps, aviso antigo não reaparece; histórico preservado e novo treino continua funcional | S5-04, regressão S2–S4 |
| 10 | Editar, iniciar limpeza de exercício e exportar depois; restaurar | Apagados não reaparecem; outro exercício, descanso, plano e histórico preservados | S5-02, S4-05 |
| 11 | Rotacionar durante preparação/cópia/validação e no resumo | Uma operação, nenhum seletor/confirm duplicado; andamento ou resumo preservado; sem substituição prematura | S5-05 |
| 12 | Interromper processo durante transferência; abrir app novamente | Transferência deve ser reiniciada manualmente; arquivo parcial não aceito; dados atuais íntegros | S5-05 |
| 13 | Exercitar interrupção da restauração em ambiente controlado, conforme fronteiras cobertas pelos testes | Antes de COMMITTED: dados e preferências anteriores retornam; após COMMITTED: importados permanecem. Falha de recuperação bloqueia telas normais e permite tentar novamente | S5-04, RF-157 |
| 14 | Repetir nos providers disponíveis (local e, se usado, nuvem), inclusive cancelar/falhar escrita | Mensagens corretas; nenhum incompleto apresentado como válido. Remoção física de parcial depende do provider | S5-05 |
| 15 | Fonte ampliada, teclado, rolagem, TalkBack e toque | Títulos/ações compreensíveis, aviso/resumo legíveis e roláveis, progresso/resultado anunciado, alvos ≥48 dp, confirmação explícita | S5-05 |

Os quatro cenários modalidade × instalação limpa/ocupada são obrigatórios. Automação injeta falhas nas fronteiras do protocolo; encerrar o processo aleatoriamente não comprova qual fronteira foi atingida. Registrar separadamente teste humano e resultado automatizado, sem editar o banco de produção para simular falhas.

Pendências de S3/S4 (TalkBack abrangente, assinatura definitiva, custódia e ensaio) permanecem separadas. O responsável fará a homologação e o merge manualmente no GitHub; Codex não integra automaticamente.
