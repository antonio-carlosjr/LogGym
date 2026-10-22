# Sprint 2 — execução do treino

## Escopo e decisões

S2-01 a S2-09 e S2-10 parcial: carga/repetições e tempo manual em segundos. Distância, fotos, kg/lb e telas de histórico ficam para entregas posteriores. RF-136 permanece **parcialmente implementado**.

- O personalizado escolhe `WEIGHT_REPS` ou `TIME` ao criar; o tipo é imutável. Nativos/templates existentes mantêm carga/repetições.
- Prescrição de tempo: séries e segundos positivos, descanso entre 0 e 600 s em passos de 15. Padrão opcional: 3 × 30 s, descanso 90 s.
- Carga é externa adicional, em kg, por halter/lado conforme o equipamento. Zero é válido para carga/repetições; duração preenchida precisa ser positiva.
- Check incompleto é permitido, avisa na série/snackbar e preserva nulos. Texto inválido impede check e saída/finalização até correção ou descarte.
- Finalização confirma séries pendentes e marcadas incompletas no mesmo diálogo. Somente séries marcadas entram no registro definitivo.

## Implementação e rastreabilidade

| Tarefa | Entrega | Evidência principal |
| --- | --- | --- |
| S2-01 | Estado, buffers, erros, diálogos e resultado de navegação | `SessionViewModel` |
| S2-02 | Campos por tipo, metas e dica de carga | `SessionScreen`, `SetEntry` |
| S2-03 | Check/desmarcar, extras e remoção só de desmarcadas | tela e repositório de sessão |
| S2-04 | Debounce independente de 500 ms, revisões, escrita serial e check atômico | ViewModel; `saveSetAndSetCompleted` |
| S2-05 | Snapshot, rascunho único, pré-preenchimento, rollback, descarte e reabertura | `SessionRepositoryTest` (JVM/dispositivo) |
| S2-06 | Roteiro de interrupções e registro de evidências | `reunioes/S2-06-recuperacao.md` |
| S2-07 | Timestamps, recálculo ao retomar, atualização de 1 s e pular | `RestTimer`, ViewModel e tela |
| S2-08 | Finalização parcial e atômica, bloqueio de sessão sem checks | repositório/ViewModel |
| S2-09 | Retomar pela home, voltar salvando, descartar com confirmação | home, navegação e sessão |
| S2-10 parcial | Tipo fixo, meta de tempo, duração e pré-preenchimento | biblioteca/editor, modelos, DAOs e mapeadores |

Banco v2 com migração 1→2, sem recriação destrutiva. Preserva planos, IDs, rascunhos, histórico, timestamps e séries. O teste `SessionMigrationTest` abre o esquema v1 real, migra com Room e verifica integridade referencial.

## Persistência e limitações

Room é a fonte persistente. A digitação usa buffers por ID e revisão para impedir que emissões antigas substituam o texto atual. Voltar/check/finalizar aguardam gravação; falhas preservam o buffer e permitem tentar novamente. Remover e descartar cancelam debounces pendentes. Operações são serializadas.

O app solicita flush ao ir para segundo plano. Uma morte abrupta **antes do debounce/commit** pode perder a última digitação não salva; os dados confirmados no banco devem sobreviver. Desligamento/reinício nunca finaliza o rascunho automaticamente.

Cronômetro de descanso não usa serviço, alarme nem contador em segundo plano. O instante final é persistido, e o restante é reconstruído pelo relógio. Desmarcar preserva o descanso; check repetido não reinicia; descanso zero limpa o anterior.

## Verificação

- `assembleDebug`: gera APK de debug.
- `testDebugUnitTest`: regressão S1, ViewModel com tempo virtual, domínio, repositórios, migração e reabertura de banco em arquivo.
- `connectedDebugAndroidTest`: regressão S1, repositórios/migração e interface da sessão com Room.
- Roteiro S2-06: complementa os testes automatizados com recriação, encerramento de processo e reinício do dispositivo.

O encerramento da sprint depende de testes aprovados, evidências manuais e CI verde. A implementação local não equivale, por si só, à homologação/merge.

### Resultado local — 22/10/2026

`assembleDebug testDebugUnitTest connectedDebugAndroidTest --offline --no-configuration-cache --console=plain`: **BUILD SUCCESSFUL**. Foram aprovados **103 testes JVM e 14 instrumentados** no AVD MyPhone, Android 17/API 37, incluindo regressão S1. Relatórios em `app/build/reports/tests/testDebugUnitTest/` e `app/build/reports/androidTests/connected/debug/`.

APK: `app/build/outputs/apk/debug/app-debug.apk`. A execução local não publica PR nem comprova o resultado futuro do GitHub Actions.

A verificação fora da instrumentação também foi executada: kill em background, recriação de Activity, reinício do emulador, descanso expirado, perda esperada da última digitação não salva, recuperação de séries completas/incompletas/extras/removidas, finalização parcial, pré-preenchimento e descarte. Resultados em [S2-06](reunioes/S2-06-recuperacao.md#execução-local--22102026). A homologação em equipe e o CI remoto ainda precisam ser confirmados.
