# Sprint 7 — navegação e usabilidade

Implementação do plano B aprovado em 25/10/2026: 33 pontos e 6 de reserva. Base `902a809`, PR #15 integrado; branch `feature/S7-01-navegacao-e-usabilidade`. [Contratos e esboços](decisoes/S7-usabilidade.md). Responsável homologa e faz merge manual; sem release.

| Tarefa | Pontos | Responsabilidade | Entrega |
| --- | ---: | --- | --- |
| S7-01 | 3 | Dev 3 | Contratos, tokens, esboços e referência anterior |
| S7-02 | 5 | Dev 2 | Barra com cinco raízes, seleção/Voltar/insets |
| S7-03 | 5 | Dev 1 | Estado restaurável, bloqueios e descarte de pilhas/gerações |
| S7-04 | 5 | Dev 2 | Linhas de séries com identidade e comandos preservados |
| S7-05 | 3 | Dev 3 | Layout adaptativo, semântica e regressão de foco |
| S7-06 | 5 | Dev 3 | Padronização pontual e estados compartilhados |
| S7-07 | 2 | Dev 3 | Fade 120 ms e cor 150 ms, sem bloquear operações |
| S7-08 | 5 | Dev 1 | Regressão, rastreabilidade e documentação |
| Total | **33** | Dev 1: 10; Dev 2: 10; Dev 3: 13 | Reserva: 6 |

## Componentes e garantias

- `TopLevelDestination`, `RootNavigationBar` e `LogGymNavHost`: Início, Planos, Exercícios, Histórico e Ajustes. Título Configurações preservado. Seleção pela rota, reseleção sem efeito, raízes salvas/restauradas. Voltar secundário retorna ao Início. Barra oculta no IME e nos fluxos internos.
- `RootNavigationLock`: bloqueio por entrada, sem vazamento entre destinos; inclui formulários/modais, comandos e SAF. `BackupSection` propaga também seletor de importação local. Nenhum cancelamento implícito.
- `LogGymApp` / `NavController.clearRootStacks`: elimina pilhas salvas e ativa na substituição de geração, antes de permitir ações sobre os dados substituídos. Sucesso/rollback abrem Início → Configurações. Tema e rotação não invalidam a geração.
- `ExerciseLibraryViewModel`: somente query e chave do filtro em SavedStateHandle; entidades não são serializadas. Listas usam os savers de rolagem do Compose por entrada.
- `SessionSetRow`: mesmos filhos/campos em um Layout adaptativo; mede texto, preserva largura confortável e move ações/empilha campos quando necessário. Séries continuam lazy por ID. Checkbox/remoção ≥48 dp. Dica de carga única, erro/incompletude explícitos, descanso e rodapé fixos.
- `LogGymSpacing`, tema e estados comuns: escala 4/8/12/16/24 dp, cards de 12 dp, carregamento/vazio/erro/heading. Paleta, cores dinâmicas e formatos históricos preservados.
- Fade somente entre raízes e cor baseada no estado persistido, sem efeitos de celebração. Escala do sistema respeitada. Nenhuma animação de altura/foco/remoção/cronômetro.

Room permanece v3; backup público v2 e recuperação privada S5 inalterados. Nenhum contrato de repositório de negócio, esquema, migração, preferência ou seed alterado. Persistência/check automático continuam pelos comandos S6.

## Ambiente e verificação

Ambiente local preparado em diretórios ignorados `build/toolchain` e `build/gradle-home`. Gradle 9.8.0 com JDK 25 instalado (wrapper confirmou suporte). Tentativa de obter JDK 21 pela API Adoptium recebeu HTTP 403. CI mantém JDK 21. SDK Platform 37.0 r2 e Build Tools 36 instalados pelo AGP; emulador descartável API 35 `LogGymS7`, resolução de referência 360×800 dp, sem dados do AVD do usuário. Nenhuma chave/credencial versionada.

A nova ferramenta Android usa nomes como `platforms/android-37.0`; a tentativa com o identificador anterior não instalou a platform. O AGP resolveu o pacote correto. Esses ajustes não mudam a configuração versionada do aplicativo.

Primeira regressão: build debug compilado; 193 JVM executados, um robô legado da S1 falhou ao tocar Iniciar durante transição. Diagnóstico da árvore mostrou Home e ação disponíveis; o robô passou a concluir a transição antes do toque e aguardar a navegação observável. Nenhuma assertiva de rascunho foi retirada.

Resultados finais, medição e CI serão registrados após execução, sem presumir aprovação antecipada. [Homologação humana S7](HOMOLOGACAO-S7.md) permanece distinta das verificações técnicas.

## Fora desta entrega

Estatísticas, gráficos/recordes, preferências por exercício, distância, fotos, kg/lb, navigation rail e animações de inserção/remoção. Candidatura S8: estatísticas dentro de Histórico, sujeita a planejamento próprio; nenhum código antecipado. Pendências anteriores de TalkBack abrangente, chave definitiva/custódia e ensaio não são concluídas por esta implementação.


## Medição de densidade e inspeção técnica

Harness local instrumentado compilado separadamente via init script, em `build/s7-reference/harness`, sem integrar o código do aplicativo ou a suíte versionada. Compara o fonte S6 do commit base com a S7 usando a mesma sessão de seis séries de carga, sem valores/erros, fonte 1×, cores dinâmicas desligadas, viewport 360×800 dp. Soma alturas não recortadas de cabeçalho, seis linhas e rodapé, incluindo os sete intervalos de 12 dp da lista S6. O cabeçalho de página/rodapé fixo não integram o bloco medido em nenhuma versão.

| Parte do bloco | S6 (dp) | S7 (dp) |
| --- | ---: | ---: |
| Cabeçalho do exercício | 104 | 88 |
| Seis séries | 1200 | 744 |
| Adicionar série | 40 | 48 |
| Espaços entre itens | 84 | 0 |
| **Total** | **1428** | **880** |

S7 ocupa **61,6%** da altura anterior: redução de **38,4%**, atendendo à meta ≤80%. Capturas locais `build/s7-reference/s7-density/legacy.png` e `current.png` inspecionadas tecnicamente. São evidências locais, não homologação humana; não extrapolar a meta para fonte ampliada ou outras combinações. Dois métodos de medição não contam como novos testes da suíte do produto.

## Correções encontradas na validação

- Matcher de campo vazio incluía o placeholder: teste passou a conferir `EditableText`, mantendo a exigência de limpar a busca após restauração.
- O robô S1 instrumentado usava Voltar como antigo botão superior mesmo após fechar modal com busca/IME. Segue agora o primeiro Voltar para teclado e a aba Início para o retorno explícito; assertivas de criação, proteção dos nativos e rascunho preservadas.
- A inspeção e o teste de alvo de toque separaram o desenho nativo do checkbox de um único container toggleable de 48 dp, sem alvo duplicado ou alteração do comando.
- Corrigidos escape do drive no `local.properties` ignorado, imports de operadores Kotlin e imports de novas assertivas. O construtor SavedStateHandle é fornecido pelo Hilt em produção, evitando avisos de API reservada a testes; ordinal usa recurso localizado.
- Uma análise FIR do lint falhou internamente em HomeViewModel; a execução posterior estável passou sem suprimir detectores ou ignorar casos.

Build padrão sem harness também foi executado com sucesso. Lint estável: **zero erros e cinco avisos preexistentes de versões**. Resultados finais e CI ficam consolidados abaixo e no PR, sem atribuir homologação S7.


## Resultado local final — 25/10/2026

| Verificação | Resultado |
| --- | --- |
| Build padrão `assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest` | BUILD SUCCESSFUL |
| Suíte JVM | **193 aprovados**, zero falhas/erros/ignorados |
| Lint | **0 erros, 5 avisos preexistentes de versões** |
| Suíte instrumentada padrão, API 35 | **71/71 aprovados** |
| Rodada com medição local | **73/73 aprovados**, sendo 71 do produto e 2 de comparação S6/S7 |
| Densidade do bloco de seis séries | **880 / 1428 dp = 61,6%**; redução 38,4% |

Rodada padrão executada após o último alinhamento de estados visuais, sem fontes do harness no APK de testes. Não somar rodadas repetidas como novos testes. Referência S6: 192 JVM/62 instrumentados; S7 acrescenta recuperação de filtro/query e nove cenários instrumentados (navegação/rolagem, bloqueios, gerações/rollback e foco/alvo de toque).

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --max-workers=1 --no-configuration-cache --console=plain
adb -s emulator-5560 shell am instrument -w -r com.loggym.test/com.loggym.HiltTestRunner
```

Logs locais ignorados: `build/s7-delivery-build.log`, `build/s7-product-final.log`, `build/s7-instrumented-final.log`; relatórios JVM/lint em `app/build/reports`. APK debug: `app/build/outputs/apk/debug/app-debug.apk`. SDK/cache/AVD preparados em `build` são temporários e serão removidos por limpeza do build; para uso permanente configurar SDK externo no `local.properties` privado.

Revisão técnica: seleção pela rota, limpeza de stores salvos/ativos, bloqueios de callbacks/SAF, árvore de campos estável, estado acessível único no check e ausência de alterações em dados, schemas e dependências conferidos. A CI do HEAD final será acompanhada e terá resultado/URL registrados no PR consolidado. Homologação funcional individual **S7 permanece pendente**, assim como testes humanos não relatados; nenhum merge ou release executado.


### Revisão de prioridade de Voltar

Handler das raízes composto após o NavHost para prevalecer sobre o pop interno e salvar estado no retorno ao Início. Teste da biblioteca ampliado: busca/filtro → outra aba → recriação → Voltar → Início → Biblioteca, exigindo o mesmo contexto. IME continua fechando primeiro; fluxos internos mantêm seus handlers. CI do novo HEAD obrigatória; resultados remotos anteriores não validam antecipadamente este ajuste.


### Primeira CI — diagnóstico de preparação

Em `1dc95c8`, build/JVM/lint aprovados com JDK 21. Instrumentados: 63/71 aprovados; oito falhas nos @Before dos dois novos robôs S7, aguardando Pular por enquanto fora da área composta em tela menor. Helpers passam a rolar a LazyColumn até o botão e confirmar visibilidade antes do toque, seguindo o padrão existente. Nenhuma assertiva funcional removida.

Uma repetição local encontrou também preparação do robô S3 concorrendo com a recuperação inicial: a escrita de fixture era corretamente recusada pelo coordenador. O robô agora aguarda onboarding visível antes de cadastrar os dados, sem alterar o protocolo de recuperação nem adicionar atraso arbitrário. Revalidar o HEAD corrigido localmente e na CI; resultados remotos anteriores são históricos.


Revalidação após esses ajustes: build/JVM/lint/APK instrumentado aprovados; **71/71 instrumentados em 320×640 dp, API 35**, incluindo o retorno ao Início/reabertura da biblioteca com contexto preservado. Rodada dirigida de navegação: 4/4, sem somar repetições à contagem da suíte. Log local `build/s7-compact-viewport-final.log`. CI do HEAD corrigido acompanhada no PR #16.

## Ajuste de consistência do cadastro — 26/10/2026

Solicitado pelo responsável antes da homologação/merge do PR #16. Partida `dca722b`, branch `feature/S7-01-navegacao-e-usabilidade`, limpa e sincronizada após `fetch`; commits e implementação anteriores preservados. `main` não foi modificada.

A ação Novo exercício na aba Exercícios navega para a mesma `ExerciseCreateRoute`/`ExerciseCreateScreen` já usada pelo plano, reutilizando formulário, ViewModel, validação e persistência. O diálogo de edição de exercícios existentes permanece. Somente o editor do plano recebe `createdExerciseId` para preencher a busca do seletor; a biblioteca conserva busca/filtro e observa os dados salvos sem filtrar automaticamente pelo nome criado. Cancelar/Voltar não grava, e a navegação/sucesso são protegidos contra entrada repetida ou já removida. Nenhuma mudança em Room, DataStore, backup ou contratos de domínio.

Cobertura: `Sprint4FlowTest` conserva plano novo/cadastro contextual; `Sprint7NavigationTest` acrescenta plano existente e biblioteca (retorno, busca, cancelamento, ausência de inclusão automática, persistência/recriação e temas claro/escuro); `TimedExerciseFlowTest` usa a mesma tela no harness, mantendo prescrição/execução por tempo. O teste existente `ExerciseCreateViewModelTest` mantém cobertura de falha, recriação e submissão única.

Build debug, **193 JVM (zero falhas/erros/ignorados)**, lint (**0 erros/5 avisos existentes**) e APK instrumentado aprovados. Corrigido import da extensão `hasRoute` conforme a biblioteca atual durante compilação. O teste novo explicitou foco no campo antes de Voltar para testar fechamento de IME, sem confundir com retorno à Home; mantém todas as assertivas.

Primeira suíte completa: 72/73 em AVD isolado API 37 `LogGymS6Test`; falha intermitente no teste existente `cancelPreviewPreservesSavedLibraryContextAndUnlocksTabs`, que observou a busca vazia ao reabrir. Repetição isolada passou (1/1). Nenhum teste ignorado nem regra de backup/navegação alterada para ocultar falha. Nova rodada completa e CI do HEAD serão informadas no PR. [Reteste humano](HOMOLOGACAO-S7.md) continua pendente; não há aceite coletivo, merge ou publicação de release.

Revalidação local final do ajuste: **73/73 instrumentados aprovados** em API 37, incluindo os dois cenários novos; 121,121 s. Não somar repetições à contagem. Logs ignorados: `build/s7-shared-create-build.log`, `build/s7-shared-create-full.log` (primeira rodada), `build/s7-shared-create-cancel-retest.log` e `build/s7-shared-create-full-final.log`. Suíte anterior preservada; aprovação humana deste ajuste e resultado da CI do novo HEAD ficam separados e serão acompanhados no PR #16.


Primeira CI do ajuste (`5293a13`): 192/193 JVM e 72/73 instrumentados; lint sem erros. JVM legado S1 enviava Voltar quando o comando de ativação ainda podia manter as abas bloqueadas; passou a aguardar aba habilitada antes do mesmo evento, mantendo todas as assertivas. Teste novo da biblioteca chegou ao item salvo, mas falhou numa abertura/fechamento adicional de detalhes; o critério passa a verificar diretamente item visível após saída/retorno/recriação e busca vazia, sem acoplar cadastro ao diálogo de edição/detalhes não alterado. Nenhuma mudança adicional de produção. Nova verificação do HEAD corrigido obrigatória, sem usar a CI anterior como aprovação.

Revalidação das correções de teste: build/JVM/lint/APK aprovados, **193/193 JVM**; em API 37 e viewport 320×640 dp, sete cenários dirigidos passaram, e o cenário novo de biblioteca passou isoladamente (1/1) após aguardar a ocultação da barra por IME antes de Voltar. Não se presume nova rodada completa local após a última correção de teste; a CI executará a suíte inteira no HEAD corrigido. Regras e arquivos de produção permanecem exatamente como em `5293a13`.
