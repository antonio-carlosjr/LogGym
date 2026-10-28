# Homologação da Sprint 7

**Sprint 7 concluída — homologação funcional individual aprovada em 27/10/2026.** O responsável pelo projeto confirmou nesta conversa que realizou os testes manuais e que todos estão validados. O aceite abrange os grupos do roteiro abaixo e o reteste de consistência do cadastro. A evidência humana disponível é essa declaração; dispositivo/API, identificação exata do APK testado e capturas não foram informados e não são presumidos.

**Integração técnica concluída:** [PR #16](https://github.com/antonio-carlosjr/LogGym/pull/16), integrado manualmente pelo responsável em 26/10/2026 (America/Sao_Paulo), merge `01268ba2cf18c851fed698e58f0a3774b2ee850f`. [CI pós-merge aprovada](https://github.com/antonio-carlosjr/LogGym/actions/runs/40427237509), com build, testes JVM, lint e instrumentados. A versão integrada é a referência do encerramento, sem atribuir um hash de APK ao teste humano.

O aceite é individual, não coletivo. Assinatura definitiva/custódia, ensaio e publicação de release continuam separados, sem conclusão presumida. A validação dos cenários de acessibilidade da S7 não reescreve os registros históricos de verificações humanas anteriores.

| Grupo | Verificação esperada |
| --- | --- |
| Navegação | Cinco abas corretas e selecionadas; repetir seleção não duplica nem reposiciona; Voltar das raízes vai ao Início, no Início saída normal. Sem seta superior nas raízes. |
| Estado | Busca, filtro e rolagem permanecem entre abas e ao recriar Activity. Tema não reinicia navegação. Primeiro Voltar fecha teclado/modal. |
| Fluxos internos | Barra ausente no onboarding, treino, editores, cadastro e detalhe. Voltar retorna à origem correta; cadastro contextual conserva divisão/busca. |
| Bloqueios | Não trocar abas durante comandos, formulários modais, exportação, seletores SAF, validação, resumo e restauração. Cancelar explicitamente libera ações sem operação duplicada. |
| Reset de dados | Preparar as cinco abas; restaurar completo e sem histórico. Nenhuma busca/entidade antiga reaparece; Ajustes selecionada; dados e comandos funcionam. Cancelar resumo preserva contexto. Rollback controlado não reutiliza ViewModels da geração anterior. |
| Treino compacto | Carga/repetições e tempo legíveis; concluir/remover/adicionar/histórico/borracha acessíveis; zeros, ausências, erros e séries incompletas distintos. Não diminuir alvos para obter densidade. |
| Automação | Editar e Concluído/outra série conforme S6; Próximo, blur, histórico, rolagem, fonte e rotação não concluem. Desmarcação e borracha não reativam intenção antiga. |
| Persistência | Retomar após background/kill com commits preservados; nenhuma promessa de recuperar última tecla anterior ao debounce. Limpeza permanece, finalização salva somente marcadas. |
| Descanso | Faixa fixa com teclado/rolagem; abrir histórico não reinicia; aviso final mantém cerca de 5 s; Pular acessível. |
| Visual | Claro, escuro, automático/dinâmico, nomes longos, vazio, carregamento e erro; espaçamentos e hierarquia coerentes. Histórico individual mantém data sem horário, contexto na mesma linha e divisores entre sessões. |
| Adaptação | 320/360 dp, paisagem, fonte até 200%, teclado e navegação por gestos/três botões: campos/rodapé/FAB/rótulos sem cobertura. |
| Acessibilidade | TalkBack anuncia seleção, exercício/série, conclusão e ações; foco coerente, alvos ≥48 dp, estado não só por cor e sem anúncio a cada segundo. |
| Movimento | Fade curto entre raízes e cor discreta ao marcar; sem bloquear interação; animações desativadas no sistema continuam funcionais, sem repetição ao reabrir. |
| Desempenho | Rolar sessão com 50 séries em aparelho intermediário, preencher e trocar foco sem travamento perceptível nem perda de texto. |
| Densidade | Comparar referência S6/S7 em 360×800 dp, fonte padrão, seis séries de carga sem erros: bloco ≤80% da altura anterior. Fonte ampliada prioriza legibilidade. |

Recuperação/rollback em fronteiras controladas são complementados pelos testes de engine e navegação; não corromper banco real. Usar AVD/instalação isolada para importação, que substitui todos os dados. Pendências anteriores de assinatura definitiva/custódia e ensaio permanecem separadas.

## Reteste de consistência do cadastro — aprovado em 27/10/2026

- Plano novo e plano existente: adicionar exercício → cadastrar → salvar. Conferir retorno à mesma divisão/seletor, pesquisa pelo nome criado e ausência de inclusão automática; selecionar e prescrever normalmente.
- Aba Exercícios: Novo exercício abre a mesma tela. Salvar retorna à biblioteca; busca anterior não é trocada pelo nome criado. Com busca/filtro limpos, conferir o novo item e sair/retornar à aba.
- Em ambas as origens: Cancelar/Voltar não cadastra, preserva contexto e fecha primeiro o fluxo interno; repetir salvamento não duplica cadastro/retorno. Conferir recriação e ambos os tipos de exercício.
- Conferir temas claro/escuro, teclado, rótulos e ausência da barra inferior na tela de cadastro. Edição de exercício existente continua no diálogo atual.

Cenários aprovados pelo responsável conforme declaração de 27/10/2026. A homologação humana é independente da automação; aceites e pendências anteriores permanecem preservados.
