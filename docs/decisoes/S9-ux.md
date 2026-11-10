# S9 — refinamento de UX e registro rápido no treino

Registro: 10/11/2026, solicitação do responsável. Escolhas confirmadas: tabela compacta para séries e entrega como sprint própria. Pontuação e distribuição ficam a cargo do responsável.

## Problema

Na execução, cada série usava dois campos com rótulo repetido ("Carga (kg)", "Repetições"), largura mínima de ~100 dp por campo, checkbox e lixeira. Em telas de 360 dp a linha não cabia: ações desciam ou campos empilhavam, e a altura variava entre séries. Textos de apoio redundantes ocupavam o topo e o rodapé.

## Escopo

- **Treino (prioridade):** linha `nº | kg | reps | ✓` com cabeçalho de colunas único por exercício e campos compactos sem rótulo (acessibilidade por descrição). Remoção da série pelo menu do número da série e por ação de acessibilidade. Limpar exercício e descartar treino vão para menus `⋮`. Teclado segue kg → reps → próxima série; o último campo do exercício usa Concluído. Foco seleciona o valor para sobrescrever. Prescrição curta (`3 × 8–12 · 1:30`). Topo sem aviso de halter/lado nem contador em texto (progresso no subtítulo e em barra). Rodapé só mostra estado quando salvando ou com falha.
- **Textos:** remover frases que repetem título ou explicam o óbvio; encurtar avisos mantendo consequências de perda de dados.
- **Demais telas:** ajustes pontuais de hierarquia e densidade em Home, Planos, Exercícios, Histórico e Configurações, reutilizando componentes existentes.

## Limites

Sem mudanças em Room v3, DataStore, backup v2, domínio, protocolo de persistência, debounce, conclusão automática ou navegação/gerações. Mantidos paleta A5, alvos ≥ 48 dp e testTags `set-{id}-weight/reps/duration`, `set-row-{id}`, `exercise-header/footer-{id}`. A mudança da árvore de campos da linha de série, congelada na S8, é autorizada aqui, preservando identidade e foco durante o reajuste de largura/fonte. Sem novas animações, estatísticas, kg/lb, distância ou fotos.

## Verificação

Build, JVM, lint e instrumentados conforme README/CI, com testes atualizados para localizar campos por tag/descrição. Homologação funcional e merge manuais pelo responsável; sem auto-merge ou release.
