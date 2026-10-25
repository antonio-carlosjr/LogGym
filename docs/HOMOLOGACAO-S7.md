# Homologação da Sprint 7

**Pendente:** aprovação do plano não é homologação funcional. Registrar responsável, data real, commit/APK, dispositivo/API, configuração e evidência por cenário. Testes automatizados e inspeção técnica não são aceite humano. Merge manual pelo responsável, sem auto-merge/release.

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
