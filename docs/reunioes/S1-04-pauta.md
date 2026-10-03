# Pauta: decisões de conteúdo e regras de entrada (S1-04)

| | |
| :--- | :--- |
| **Data** | ___/___/______ |
| **Duração prevista** | 60 min |
| **Participantes** | Dev 1 (condução), Dev 2, Dev 3 |
| **Objetivo** | Fechar as lacunas da seção 17 da [especificação](../../especifica_o_funcional_loggym.md) e as regras de entrada que ela não define |
| **Insumos** | [`NativeExercises.kt`](../../app/src/main/java/com/loggym/data/seed/NativeExercises.kt), [`PlanTemplates.kt`](../../app/src/main/java/com/loggym/data/seed/PlanTemplates.kt), [`ExerciseTarget.kt`](../../app/src/main/java/com/loggym/domain/model/ExerciseTarget.kt) |

> **Por que decidir agora:** o banco está na versão 1 e nunca foi publicado. Até a primeira release, dá para mudar o esquema e o seed sem migração. Depois disso, cada mudança exige uma `Migration` do Room.

**Status:** reunião realizada. As decisões abaixo foram registradas como a equipe as escreveu, e a consolidação, com as regras resultantes e as pendências, está em [`docs/decisoes/S1-04.md`](../decisoes/S1-04.md).

---

## 1. Biblioteca de exercícios nativos (seção 17, item 1) · 25 min

| Item | Lacuna | Como está hoje | Recomendação | Decisão |
| :---: | :--- | :--- | :--- | :--- |
| 1.1 | Lista definitiva de exercícios nativos | 25 exercícios; faltam movimentos comuns | Chegar a cerca de 40. Adicionar, por exemplo: supino declinado, crucifixo com halteres, remada unilateral, encolhimento, rosca Scott, tríceps testa, paralelas, flexão de braço, agachamento búlgaro, hack, cadeira abdutora/adutora, panturrilha sentado, abdominal crunch, elevação de pernas | Adicionar cerca de 60 exercícios |
| 1.2 | Exercícios por tempo ou distância | Não incluídos | Manter fora (prancha, esteira, corda), porque a seção 18 os exclui | Incluir exercícios por tempo ou distância |
| 1.3 | Formato do grupo muscular | Texto livre gravado na coluna (`"Peito"`) | Lista fixa (`enum MuscleGroup`), gravando uma chave estável (`chest`) com rótulo vindo do `strings.xml`. Permite o filtro da S1-05 e evita variações de escrita | Recomendação aprovada |
| 1.4 | Quantidade de grupos por exercício | Um | Manter **um grupo principal**. Secundários só servem para estatísticas musculares, que estão fora do MVP | Não limitar a 1 grupo por exercício. 1 exercício pode ter 1 ou mais grupos. |
| 1.5 | Grupo do exercício personalizado | Opcional, texto livre | Opcional, escolhido na mesma lista fixa | Recomendação aprovada |
| 1.6 | Equipamento | Não há campo; fica no nome | Não criar campo. Padrão de nome: "Movimento + variação + equipamento" | Recomendação parcialmente aprovada. Quero um placeholder aberto de poucos caracteres para o usuário escrever observação para aquele exercício |
| 1.7 | Instruções ou descrição | Não existem | Fora do MVP: fotos e vídeos estão excluídos, e texto sozinho agrega pouco | Incluir foto somente dos exercícios já pré-cadastrados. O usuário, ao cadastrar um exercício, não poderia inserir foto. Incluir descrição ou instrução em um campo. |
| 1.8 | Nomes duplicados | A especificação não diz nada; o código permite | Bloquear personalizado com nome igual (ignorando maiúsculas e espaços) a outro personalizado ou a um nativo | Bloquear personalizado com nome igual |
| 1.9 | Exercícios com peso corporal ou assistidos | Carga 0 permitida (RF-76) | "Carga" significa **carga externa adicional**: barra fixa sem colete = 0. Exercícios assistidos (carga negativa) não são suportados | Recomendação aprovada |
| 1.10 | Exercícios unilaterais e com halteres | Indefinido | Registrar o **peso de um halter/lado**, como está no equipamento, com essa dica no campo | O campo de carga é único. |
| 1.11 | Novos exercícios nativos em versões futuras | O seed roda apenas no `onCreate` do banco | Novos nativos e correções entram por `Migration` (`INSERT OR IGNORE` / `UPDATE` pela `native_key`). A `native_key` nunca muda | Recomendação aprovada |

## 2. Planos pré-definidos (seção 17, item 2) · 20 min

| Item | Lacuna | Como está hoje | Recomendação | Decisão |
| :---: | :--- | :--- | :--- | :--- |
| 2.1 | Quais estruturas oferecer | PPL, Upper/Lower e Bro Split | Manter as três (RF-36) e avaliar um **Full Body 3x** para iniciantes | Recomendação aprovada |
| 2.2 | Público-alvo dos templates | Valores pensados para nível intermediário | Iniciante a intermediário: 4 a 6 exercícios e 15 a 20 séries por treino | Recomendação parcialmente aprovada. Incluir de 12 a 20 séries. |
| 2.3 | Conteúdo de cada divisão | Exercícios, séries, repetições e descanso provisórios | Validar com alguém da área (professor ou educador físico). Compostos primeiro; descanso de 120 a 180 s nos compostos e de 60 a 90 s nos isoladores | Somos da área. Relaxa. 😎 |
| 2.4 | Mesmo template escolhido duas vezes | Cria dois planos com o mesmo nome | Permitir, com sufixo automático "(2)" | Recomendação parcialmente aprovada. O usuário pode editar os exercícios já previamente cadastrados no template. |
| 2.5 | Ativação automática do plano criado | Onboarding ativa; área de planos indefinida | Onboarding ativa direto; na área de planos, perguntar "Ativar agora?" | Recomendação aprovada |
| 2.6 | Aviso de saúde | Não existe | Uma linha no onboarding: o app não substitui orientação profissional | Recomendação aprovada |

## 3. Unidade e regras de entrada (seção 17, item 3, mais limites não definidos) · 10 min

| Item | Lacuna | Como está hoje | Recomendação | Decisão |
| :---: | :--- | :--- | :--- | :--- |
| 3.1 | Unidade de carga | kg (`weight_kg`) | Somente kg; sem alternância para lb no MVP | O usuário pode escolher entre kg e lb. Kg já pré-definido como padrão |
| 3.2 | Precisão da carga | `Double` sem regra | Até 2 casas decimais. Aceitar vírgula ou ponto; exibir em pt-BR sem zeros à direita (`22,5`) | Recomendação aprovada |
| 3.3 | Limites da carga e das repetições executadas | Apenas "≥ 0" | Carga de 0 a 999,99; repetições de 0 a 999 | E o Ronnie Coleman, fdp? Recomendação negada. |
| 3.4 | Limites da prescrição | Apenas "> 0" (RF-50, RF-53) | Séries de 1 a 20; repetições de 1 a 100 | Recomendação negada. |
| 3.5 | Faixa e granularidade do descanso | Segundos livres | 0 a 600 s, com seletor em passos de 15 s. 0 desativa o cronômetro (RF-56) | Recomendação aprovada |
| 3.6 | Valores padrão ao adicionar exercício a uma divisão (S1-08) | Não definidos | 3 séries, 8 a 12 repetições, 90 s de descanso | Recomendação parcialmente aprovada. Somente se o usuário quiser atribuir o valor padrão. Do contrário, deixar zerado. |

---

## 4. Encaminhamentos · 5 min

| Subtarefa | Entrega | Responsável | Pts |
| :--- | :--- | :---: | :---: |
| S1-04a | Registrar as decisões desta pauta e marcar a seção 17 da especificação como **[D]** | Dev 1 | 1 |
| S1-04b | Código: `enum MuscleGroup`, `NativeExercises` e `PlanTemplates` finais, constantes de limites e padrões em `domain/` | Dev 1 | 2 |
| S1-04c | Testes: ampliar o `SeedDataTest` (grupos válidos, nomes únicos, templates dentro dos limites) e testar os limites do `ExerciseTarget` | Dev 1 | 1 |

**Impactos:**
- A estimativa do S1-04 vai de 2 para 4 pontos.
- Dev 1 passa de 12 para 14 pontos na Sprint 1.
- A S1-05 (filtro por grupo) e a S1-08 (padrões e limites) passam a depender da S1-04b.

## Notas da reunião

- Decisões consolidadas, impactos e pontos que ainda precisam de esclarecimento: [`docs/decisoes/S1-04.md`](../decisoes/S1-04.md).
- Encaminhamentos da seção 4 aprovados.
