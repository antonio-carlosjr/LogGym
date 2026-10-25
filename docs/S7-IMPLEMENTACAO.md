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
