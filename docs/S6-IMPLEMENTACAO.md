# Sprint 6 — histórico individual e marcação automática

Implementação da alternativa A autorizada pelo responsável em 25/10/2026. Base: `35370e2720d364e612dcaa7b7a392a0d5d08be02`, merge do PR #14 realizado pelo responsável, com CI pós-merge aprovada. A aprovação funcional individual da S5 inclui o refinamento visual final. Não foi atribuído aceite coletivo ou cumprimento de TalkBack abrangente, assinatura definitiva/custódia e ensaio anteriores.

Branch: `feature/S6-01-historico-e-marcacao`. O responsável homologa e faz merge manualmente no GitHub. Nenhuma autorização de merge/release foi delegada.

## Compromisso aprovado

| ID | Entrega | Pontos | Responsabilidade técnica |
| --- | --- | ---: | --- |
| S6-01 | Consulta por identidade/tipo com cursor, 25 sessões por página | 3 | Dev 1 |
| S6-02 | Painel modal sobre a execução | 3 | Dev 2 |
| S6-03 | Testes de histórico, identidade e paginação | 2 | Dev 2 |
| S6-04 | Preferência opcional, backup público v2 e undo interno S5 | 3 | Dev 1 |
| S6-05 | Elegibilidade e conclusão idempotente | 5 | Dev 2 |
| S6-06 | Concluído no teclado e transferência entre séries | 3 | Dev 3 |
| S6-07 | Borracha, flush e gerações | 3 | Dev 1 |
| S6-08 | Regressão da marcação/configurações | 2 | Dev 3 |
| S6-09 | Regressão integrada, documentação e homologação | 3 | Dev 3 |
| Total | Reserva: 12 sobre referência provisória de 39 | **27** | **Dev 1: 9; Dev 2: 10; Dev 3: 8** |

Papéis são áreas técnicas, sem pressupor três profissionais simultâneos. O banco permanece v3; dados locais e snapshots devem ser preservados. Importação pública v1 deixa de ser suportada; recuperação privada de restauração S5 pendente continua obrigatória. Sem navegação inferior, compactação, preferências por exercício, fotos, distância ou kg/lb.

## Verificação e aceite

Em 25/10/2026, build debug, APK instrumentado e **192 testes JVM** aprovados: zero falhas, erros e ignorados. Lint aprovado sem erros, com cinco avisos existentes de versões de dependências. Referência da S5: 169 JVM. Nenhum teste foi ignorado nem detector suprimido para ocultar falha.

**62 testes instrumentados aprovados** no AVD descartável API 37 `LogGymS6Test` (`emulator-5560`). Um segundo processo do mesmo AVD do usuário foi recusado pelo emulador; foi criado ambiente independente em `build/s6-avd` com configuração copiada e dados novos, usando a imagem instalada. Nenhum dado do AVD original foi copiado/apagado ou usado pelos testes. A CI do PR executa novamente em API 35; seu resultado efetivo será registrado no PR.

Após o último refinamento de rótulo, build/lint foram repetidos sem erros (cinco avisos de dependências) e o fluxo S6 passou novamente, agora incluindo recriação da Activity com o painel de histórico aberto. Resultado direcionado: 1/1, sem somar repetições como novos testes da suíte. A proteção da série em conclusão inclui também o botão de remoção, sem bloquear a série de destino.

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --max-workers=1 --no-configuration-cache --console=plain
adb -s emulator-5560 shell am instrument -w com.loggym.test/com.loggym.HiltTestRunner
```

Resultados automatizados não representam homologação individual da S6; [roteiro manual](HOMOLOGACAO-S6.md) permanece pendente. Merge e release não foram realizados.

## Implementação e rastreabilidade

- S6-01/03: `ExerciseHistoryCursor/Page/Entry`, `SessionRepository.getExerciseHistoryPage`, consultas em `SessionDao` e mapeamento existente. Páginas de 25 sessões com todas as ocorrências, sem join com biblioteca atual. Testes reais de identidade/tipo, exclusão, posições, valores e desempate de datas; consulta verificada também após restauração v2.
- S6-02: `ExerciseHistoryViewModel`, `ExerciseHistorySheet`, relógio junto ao exercício e conteúdo compartilhado `HistorySetContent`. Estado do painel restaurável, consultas canceláveis, erro/retry/carregar mais; abrir/fechar não chama flush. Detalhe geral mantém seu layout/comportamento.
- S6-04: `UserSettings`, DataStore e Configurações com flag falsa por ausência. Codec público v2 estrito, erro tipado de versão incompatível e adaptador interno limitado ao undo v1. Dados locais não são redefinidos; teste real de preferências antigas sem a chave nova. Snapshot/rollback incluem a preferência.
- S6-05/06/07: `SessionViewModel` mantém revisão elegível independente de dirty, rastreia foco e comanda `completed=true`. Só a série em conclusão bloqueia campos/check/remoção; destino continua editável. Borracha e preferência invalidam eventos pendentes; geração antiga não admite comandos. IME Próximo mantém a linha; Concluído ou foco em outra série podem concluir. Histórico, blur, rolagem, ciclo de vida e flush não concluem.
- S6-08: gates/relógio virtual para escrita antiga, duplicação, destino editável, falha, desmarcação, borracha, preferência, flush e geração; DataStore/Room reais para backup, recuperação e preservação.
- S6-09: especificação RF-161/162, refinamento RF-80 e RF-154–160, matriz, roadmap e decisão [S6-execucao](decisoes/S6-execucao.md). S5 integrada/aprovada; S7 mantém UX-NAV-01 com cinco abas e Voltar das raízes para Início, sem execução nesta sprint.

O fluxo Compose combina preferência, pré-preenchimento sem marcação, edição de um campo, teclado, transferência entre séries, histórico com texto inválido e descanso, limpeza, recriação, finalização parcial e detalhe do histórico. A recuperação testa interrupções nas fronteiras Room/DataStore, reversão, undo corrompido, v1 público rejeitado e undo privado S5 válido.

## Revisão técnica e pendências

Banco e esquema exportado continuam v3, sem alteração de entidade/índice ou migração; biblioteca Room e limites de backup preservados. Mutação passa pelos contratos existentes e pelo coordenador; leitura histórica usa snapshots, consulta em transação e cursor estável. Nenhuma navegação inferior, criptografia, chave privada, credencial ou arquivo local do Gradle foi incluído.

Homologação funcional S6, TalkBack específico das novas telas e demais cenários manuais aguardam o responsável. Assinatura definitiva/custódia e ensaio anteriores continuam separados. O responsável homologa e integra o PR manualmente no GitHub.
