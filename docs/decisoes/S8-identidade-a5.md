# S8 — identidade A5 / Índigo técnico

Plano B aprovado em 27/10/2026: 32 pontos, reserva 7; distribuição e aceite em [S8-IMPLEMENTACAO](../S8-IMPLEMENTACAO.md). Originais A5 em `docs/loggym identidade visual`, preservados em commit separado da regularização S7 e do código S8.

## Contrato visual

Todos os HEX aprovados seguem o README original A5. Cores centralizadas em recursos Android, consumidas pelo ColorScheme; sem cores dinâmicas, nova preferência ou alteração de backup. Sistema/Claro/Escuro existentes permanecem; marca e barras seguem tema efetivo, inclusive contrário ao sistema. Interface usa fonte padrão atual. Alvos ≥48 dp; espaços S7 4/8/12/16/24; cards equivalentes com raio 12 dp e elevação discreta.

| Extensão técnica | Claro | Escuro |
| --- | --- | --- |
| primaryContainer | #EAECF1 | #3C4156 |
| secondaryContainer | #F1F2F6 | #34384B |
| surfaceContainerLow | #FCFCFD | #242735 |
| surfaceContainer | #F8F9FA | #292D3C |
| surfaceContainerHigh | #F5F5F8 | #2F3344 |
| surfaceContainerHighest | #F1F2F6 | #34384B |
| outline funcional | #646577 | #B5B5C7 |

Complementos não substituem a paleta aprovada. Pares adicionais, estados, ajustes técnicos e sua justificativa serão registrados após inspeção das capturas/contraste. outlineVariant serve a divisores decorativos; controles usam outline funcional. Sucesso, seleção, conclusão e erro nunca dependem apenas da cor.

## Marca e ícone

Logotipo horizontal no onboarding (máximo 240 dp, proporção 538:120); símbolo 40×20 dp junto ao nome na Home. Sem marca em cards/controles. Vetores nativos, sem biblioteca SVG ou fonte Inter. Recursos claros/escuros escolhidos pelo tema efetivo de Compose, não apenas values-night. Descrições decorativas não duplicam o nome já anunciado.

Launcher adaptativo mantém paths, viewport, transformações, margem e escala originais; fundo #505F8C, foreground off-white, monochrome próprio. API 26+/33+, roundIcon e máscaras circular/squircle/arredondada. Previews não são foreground. Não há suporte anterior à API 26 ou aliases de Activity. Qualquer proposta de alterar escala exige evidência e aprovação antes da edição; demais tarefas independentes continuam.

## Limites e evidências

Sem novas animações: fade S7 120 ms, cor de conclusão 150 ms e Material nativo. Não alterar árvore/foco/IDs dos campos, eventos, tempos, mutexes, snapshots, navegação/gerações ou protocolos de dados. Room v3, DataStore e formato v2 intactos. Sem estatísticas, fotos, distância, kg/lb, modalidades ou regras novas.

Capturas antes/depois das seis áreas e onboarding, claro/escuro, com mesma fixture/configuração, em AVD isolado; detalhes em S8-IMPLEMENTACAO. Revisão de cor/contraste e estados real, não homologação automática. Build/JVM/lint/instrumentados e CI do HEAD obrigatórios. Responsável homologa e faz merge manualmente; sem release/auto-merge.
