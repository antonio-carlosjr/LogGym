# S2-06 — roteiro de recuperação após interrupções

## Preparação

Instalar o APK debug em um emulador/dispositivo de teste. Criar um plano misto com carga/repetições e tempo manual; configurar descanso de 90 s. Preparar séries completas, marcadas incompletas, extras e uma removida. Não apagar dados nem reinstalar entre os passos de recuperação.

Os comandos abaixo usam `adb` no PATH; no Windows ele está em `<SDK>/platform-tools/adb.exe`. Havendo vários dispositivos, selecionar o alvo com `-s <serial>`.

## Casos

| Caso | Passos | Resultado esperado |
| --- | --- | --- |
| Salvar e retomar | Digitar carga `22,5`, reps `8`, duração `45`; esperar “Alterações salvas”; voltar e retomar | Mesmos valores e ordem; status rascunho |
| Check antes de 500 ms | Digitar e imediatamente marcar | Valor mais recente, check e descanso gravados juntos |
| Incompleto | Marcar uma série vazia | Aviso visível; valores vazios, sem conversão para zero |
| Recriação | Ativar temporariamente “Don't keep activities”; enviar app ao fundo e abrir novamente | Dados persistidos recuperados; nenhum treino finalizado |
| Kill em background | Esperar “Alterações salvas”; Home do Android; `adb shell am kill com.loggym`; abrir pelo launcher | Rascunho preservado; retomar funciona |
| Descanso ativo | Matar em background durante descanso e voltar antes do término | Restante calculado pelo instante final, sem reiniciar |
| Descanso expirado | Repetir o kill e aguardar além do instante final | “Descanso concluído”, sem valor negativo |
| Reinício | Salvar dados; `adb reboot`; esperar boot e abrir pelo launcher | Mesmo rascunho, checks e timestamps; nenhuma finalização automática |
| Janela de debounce | Digitar e encerrar abruptamente antes de 500 ms, usando `adb shell am force-stop com.loggym`; reabrir pelo launcher | Pode recuperar o último commit, sem prometer a última tecla; rascunho permanece |
| Isolamento | Voltar, renomear plano/exercício ou alterar meta e retomar | Nomes/metas do snapshot inicial preservados |
| Finalização parcial | Marcar ao menos uma série; deixar pendentes e incompletas; finalizar | Um diálogo explica ambos; cancelar preserva rascunho; confirmar salva apenas marcadas |
| Descarte | Abrir confirmação, cancelar, reabrir e confirmar | Cancelar mantém treino; confirmar remove rascunho e não cria histórico |

“Don't keep activities” testa recriação de Activity, não morte de processo. `am kill` deve ser usado com o aplicativo em segundo plano. `force-stop` é um teste de interrupção abrupta e exige reabertura explícita. Restaurar a opção de desenvolvedor após o teste.

## Evidências

Registrar data, dispositivo/API, comando/caso, valores antes/depois, resultado e captura ou log. Não registrar um caso como aprovado sem executá-lo.

| Verificação | Situação |
| --- | --- |
| Reabertura de banco em arquivo | Coberta por teste automatizado |
| Migração v1→v2 com rascunho/histórico | Coberta por teste automatizado |
| Recriação, kill e reinício do aparelho | Aprovados localmente em 22/10/2026; evidências abaixo |
| Morte durante digitação/debounce | Último valor não salvo foi perdido; rascunho e commits preservados, conforme esperado |

## Execução local — 22/10/2026

Ambiente: **MyPhone (AVD), Android 17/API 37**, APK debug gerado nesta implementação. O teste fora da instrumentação usou o plano Full Body 3x/Full Body A no banco real do app. Foi iniciado um emulador sem janela; a configuração `always_finish_activities` foi restaurada ao valor anterior após a verificação.

| Caso executado | Evidência observada | Resultado |
| --- | --- | --- |
| Gravação e morte do processo | Carga **22,5 kg**, **8 reps**, check e timestamps; `am kill` em background, ausência de PID confirmada, reabertura pelo launcher | Snapshot das três tabelas de sessão idêntico antes/depois |
| Descanso antes/depois do término | Restante reconstruído após kill; após o horário final, tela mostrou “Descanso concluído” | Aprovado |
| Reinício do emulador | `adb reboot`, boot completo, reabertura e retomada | Mesmos IDs, valores, checks e timestamps; status DRAFT |
| Recriação da Activity | `always_finish_activities=1`, Home do Android e reabertura | Snapshot preservado; configuração restaurada |
| Interrupção da digitação | Digitar `9` em uma série pendente e executar imediatamente `am force-stop` | Carga continuou nula; commit anterior e rascunho preservados. Não há garantia de salvar a última tecla |
| Tipos de série na recuperação | Completa, marcada incompleta, série original removida e nova extra; novo kill em background e reabertura | Snapshot idêntico; removida ausente e extra preservada |
| Finalização parcial/incompleta | Um diálogo explicou séries pendentes e campos vazios; cancelar preservou DRAFT; confirmar finalizou | Apenas as duas séries marcadas foram mantidas, com nulos preservados |
| Próximo treino | Iniciar novamente a mesma divisão após finalizar | Primeira série veio com 22,5 kg/8 reps e desmarcada |
| Descarte | Cancelar preservou o rascunho; nova confirmação o removeu | Nenhum histórico adicional; a sessão concluída anterior permaneceu |

O check antes de 500 ms e o isolamento de snapshots após alterações no plano também são cobertos pelos testes automatizados aprovados. Os testes instrumentados incluem criação/prescrição/execução de tempo e avisos de incompletude.

Artefatos locais da execução: `app/build/s2-recovery/`, com cópias estáveis do banco e `snapshot.json` por etapa (`before-kill`, `after-kill`, `after-reboot`, `after-activity-recreation`, `before-force-stop`, `after-force-stop`, `mixed-before-kill`, `mixed-after-kill`, `finished`, `prefilled-next`, `after-discard`). São dados de teste gerados localmente, fora do versionamento.

## Confirmação adicional informada pelo usuário — 24/10/2026

O usuário confirmou a recuperação de um **treino misto**, com exercícios de carga/repetições e por tempo, após encerramento do processo. Também informou que persistência de valores e marcações, finalização apenas das séries marcadas e pré-preenchimento no treino seguinte passaram na validação manual. Esta confirmação é um relato do usuário recebido em 24/10/2026; data exata da execução, dispositivo e capturas/logs adicionais não foram informados. Ela não substitui as evidências técnicas da execução local de 22/10/2026 nem representa aceite formal da equipe. Os demais cenários manuais informados estão consolidados em [S2-IMPLEMENTACAO.md](../S2-IMPLEMENTACAO.md#validação-manual-informada-pelo-usuário--relato-de-24102026).
