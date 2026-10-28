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

Resultados serão registrados após execução; referência S7: 193 JVM e 73 instrumentados, lint 0 erros/5 avisos existentes. Homologação S8 pendente. Codex abre PR e acompanha CI; responsável homologa e faz merge manualmente. Sem auto-merge/release.
