# Sprint 8 — identidade A5 e consistência visual

Plano B aprovado e implementação autorizada em 27/10/2026: 32 pontos, reserva 7. Base `01268ba2cf18c851fed698e58f0a3774b2ee850f`, S7 integrada/homologada individualmente. Branch `feature/S8-01-identidade-a5-e-consistencia`. Dev 1/2/3 são áreas técnicas, não três profissionais simultâneos.

| Tarefa | Entrega | Pontos | Área |
| --- | --- | ---: | --- |
| S8-01 | Contrato, regularizações e evidências comparativas | 2 | Dev 3 |
| S8-02 | Tema A5, fundo raiz, barras e estados | 5 | Dev 1: 3; Dev 3: 2 |
| S8-03 | Vetores, launcher e monochrome | 5 | Dev 1 |
| S8-04 | Marca no onboarding/Home | 2 | Dev 2 |
| S8-05 | Home, planos, exercícios | 5 | Dev 2: 3; Dev 3: 2 |
| S8-06 | Execução e históricos | 5 | Dev 2: 3; Dev 3: 2 |
| S8-07 | Configurações, diálogos e feedback | 3 | Dev 3 |
| S8-08 | Regressão, acessibilidade, evidências e documentação | 5 | Dev 1: 3; Dev 2: 2 |

## Garantias

A5 original preservado em commit próprio, após regularização documental S7 separada. Escala/margens/proporções do launcher não serão modificadas sem nova aprovação. Sem cores dinâmicas na interface; preferências Sistema/Claro/Escuro mantidas. Room v3, DataStore, backup v2, domínio, foco/debounce e animações S7 preservados. Sem estatísticas, novas regras, dependências SVG/fontes ou publicação.

## Evidências e validação

Capturas antes/depois em AVD isolado, mesma fixture mista, temas claro/escuro e configuração registrada. O teste Sprint8VisualEvidenceTest aceita argumento opcional `evidencePhase=before|after` e grava PNGs no armazenamento externo privado do app de teste; artefatos serão copiados para `build/s8-visual`, ignorado. Dados históricos têm timestamps fixos; o descanso ativo é capturado próximo de 37 segundos, sem alterar o Clock de produção. Capturas não são golden tests nem homologação humana.

Resultados consolidados abaixo; referência S7: 193 JVM e 73 instrumentados, lint 0 erros/5 avisos existentes. Homologação S8 pendente. Codex abre PR e acompanha CI; responsável homologa e faz merge manualmente. Sem auto-merge/release.

## Resultado local e revisão técnica — 27/10/2026

S8-01 a S8-08 implementadas. Homologação funcional humana **pendente**, conforme [roteiro](HOMOLOGACAO-S8.md). CI do HEAD final será conferida e registrada no PR; não confundir resultados locais com CI, aceite humano ou release.

| Verificação | Resultado |
| --- | --- |
| Build debug, JVM, lint e APK instrumentado | BUILD SUCCESSFUL |
| JVM | **198 aprovados**, zero falhas/erros/ignorados; referência S7 193 |
| Lint | **0 erros, 5 avisos existentes de dependências**; nenhum detector suprimido |
| Instrumentados completos | **77/77 aprovados**, 131,29 s, AVD isolado API 37, 360×800 px/dp, densidade 160, fonte 1× |
| Evidências antes/depois | **30 pares (60 capturas)**: 15 cenários × claro/escuro; referência S7 sem mudança visual, mesma fixture/configuração |
| Recursos e launcher | Inflação JVM API 26/33; renderização de máscaras/tamanhos e mono; inspeção nativa na Home, drawer e informações do app; estilo temático Minimal do Android verificado |

Novos JVM: HEX exatos, contraste de texto/controles em superfícies, geometria congelada e resolução de recursos/monochrome por versão (incluindo logotipos e símbolo). Novos instrumentados: dois fluxos comparáveis claro/escuro e dois testes de marca/ações ampliadas/máscaras, sem retirar cobertura anterior ou contar repetições como novos testes.

Logs ignorados: `build/s8-final-checks.log`, `build/s8-instrumented-approved.log`; rodadas anteriores e diagnóstico em `build/s8-*`. Comandos:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest --max-workers=1 --no-configuration-cache --console=plain
adb -s emulator-5560 shell am instrument -w -r com.loggym.test/com.loggym.HiltTestRunner
```

### Capturas e avaliação dos complementos

Abrir **`build/s8-visual/index.html`** para comparação lado a lado e `metadata.json` para configuração, fixture, commits e hashes dos 27 originais A5. Baseline: app `01268ba`, harness `ed7a3d5`; código visual final `b0ddea1`, testes renderizados `fae00ff`. Os commits posteriores de testes/documentação não mudam o visual capturado.

Pastas `before/light`, `before/dark`, `after/light`, `after/dark`: onboarding, Home, planos/editor, biblioteca/cadastro, histórico/detalhe, configurações/backup/informação/modal, execução/descanso, histórico individual e entrada inválida. `after/launcher` contém prévias de máscaras circular/squircle/arredondada em 24/32/48/64 px e capturas do sistema normal/temático, drawer e app info. Prévias técnicas de máscaras não substituem testes de launchers OEM. Capturas e ferramentas são artefatos locais ignorados, não snapshots de aprovação automática.

Inspeção técnica: no claro, fundo off-white e cards brancos separam conteúdo sem blocos internos de cores diferentes; no escuro, fundo #191B25 e superfícies #242735 mantêm identidade e leitura. Os containers propostos foram mantidos sem recalibrar HEX: seleção/conclusão têm tom discreto com check/rótulo, descanso permanece legível, títulos de seção equivalentes e metadados secundários consistentes. Controles de entrada usam outline funcional, enquanto divisores e contornos decorativos podem permanecer sutis. Ações de planos ocupam linha própria quando necessário e mantêm 48 dp. Fonte padrão e densidade das séries não foram alteradas. Não houve proposta/alteração de escala do launcher.

Os screenshots não comprovam TalkBack, desempenho em aparelho intermediário, toda a matriz de orientação/fonte ou todos os launchers. Esses itens aguardam o responsável. Antes de carregar a preferência, o fundo nativo acompanha a configuração do sistema; após leitura, o tema efetivo e a marca seguem Sistema/Claro/Escuro sem duplicar preferência em outro armazenamento.

### Recursos e correções encontradas

- Foreground e monochrome são cópias byte a byte dos originais. SHA-256: `02cd38af0e1ce3e9f4d66310b84a452d41ad4f06def035458ff8fa781b340a4e` e `1256393b43957f2bf4531e6bb3cedd060d48530458c9d9fb80343eb63ab9f67e`. Paths, viewport 108, translação 19,5/36 e escala 0,3 preservados.
- Como minSdk é 26, os XMLs adaptativos equivalentes foram consolidados em mipmap-anydpi. O monochrome opcional é ignorado antes da API 33 e utilizado nas compatíveis; validado em API 26/33. Removidos recurso verde anterior e definição redundante, sem suprimir lint. Round icon aponta à mesma arte aprovada.
- Somente as dimensões intrínsecas dos vetores **internos** foram adequadas: símbolo 40×20 dp e logotipo 200×44,609665 dp, exibido em Compose com máximo 240 dp/proporção 538:120. Paths/proporções intactos; isso não muda a escala do foreground/monochrome do launcher. Originais no pacote permanecem íntegros.
- Primeiro teste de geometria comparava `108` com `108.0` do XML compilado: passou a comparar o número exato. Regenerados símbolos R locais sem cache após reorganização de arquivos; nenhuma mudança de biblioteca/configuração para contornar o problema.
- Leitura do tema em SideEffect de teste corrigida para capturar ColorScheme durante composição. Botões adaptativos receberam mínimo explícito de 48 dp, sem depender da medida visual nativa de 40 dp. Comparação raster tolera apenas 1/255 de alpha de antialias entre fills diferentes; o hash da geometria continua exato.
- Primeira suíte completa: 76/77, robô legado S1 tocava Pular fora do viewport após a marca aumentar o onboarding. O robô agora rola o controle visível/habilitado e aguarda a aba após fechar diálogo/IME. Todas as assertivas de criação, proteção de nativos e rascunho foram preservadas. Passou isoladamente (1/1) e na suíte completa final (77/77).

### Encaminhamento

Sem alterações em domínio, repositórios, Room/schema/migrações, DataStore, backup, SessionViewModel, eventos de foco/debounce ou navegação/gerações. Sem novas animações ou dependências. Regularização S7, originais A5, contrato S8, testes e implementação separados em commits. Responsável realiza homologação e merge manualmente no GitHub. Nenhum merge ou release pelo Codex; pendências anteriores de assinatura/custódia/ensaio não são declaradas cumpridas.
