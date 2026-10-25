# LogGym — instruções para agentes

## Fontes de verdade

- Leia [README](README.md), [sprints](docs/SPRINTS.md) e [tarefas/política de PRs](docs/TAREFAS.md).
- Consulte a [especificação](especifica_o_funcional_loggym.md), as [decisões](docs/decisoes/) e os registros de implementação/homologação da sprint pertinente antes de alterar regras.
- O [backlog de UX](docs/BACKLOG-UX.md) contém propostas, não autorização de implementação.

## Arquitetura e dados

- Preserve Compose → ViewModel → domínio/repositórios → dados; use Hilt e os contratos existentes. Não acesse Room/DataStore diretamente nos composables.
- Preserve snapshots, identidades históricas, rascunhos, tipos de exercício e a distinção entre nulo e zero. Reutilize validações e Clock injetado.
- Mudanças de esquema exigem migração e testes de preservação; não use fallback destrutivo. Room e DataStore não constituem uma transação conjunta.
- Respeite o protocolo de coordenação/recuperação documentado na sprint; não introduza gravações que o contornem.

## Execução e verificação

- Trabalhe autonomamente dentro do escopo autorizado, em branch de trabalho, com commits lógicos em português no padrão Conventional Commits.
- Preserve alterações locais alheias à tarefa. Não versione credenciais, chaves, arquivos locais ou artefatos de build.
- Execute build e testes pertinentes conforme README e [CI](.github/workflows/android.yml). Documente resultados, ambiente e impedimentos; não presuma sucesso nem ignore testes para ocultar falhas.
- Separe automação, homologação humana, integração e entrega de release. Não atribua testes ou aprovação a pessoas sem confirmação.

## Pull requests

- O Codex implementa, testa, revisa tecnicamente, acompanha a CI e cria/atualiza o PR com entregas e limitações.
- O responsável realiza a homologação funcional e o merge manualmente no GitHub. Não execute merge nem habilite auto-merge.
- Não use force push, desative proteções ou simule revisões. Publicar release exige autorização própria.
- Preserve os registros históricos; a política vigente detalhada fica em TAREFAS.md, sem duplicá-la aqui.
