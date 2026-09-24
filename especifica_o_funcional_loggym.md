# Especificação Funcional Consolidada: LogGym

Abaixo encontra-se a especificação funcional consolidada do aplicativo **LogGym**, estruturada para organizar as premissas, requisitos funcionais, regras arquiteturais e lacunas de produto. 

Para facilitar a leitura e o rastreio das decisões, foram utilizadas as seguintes marcações de origem:
* **[D]** = Definido explicitamente no contexto original.
* **[I]** = Preenchido por inferência ou senso comum para fechar o MVP.
* **[P]** = Requer uma decisão explícita de produto (pendente).

---

## 1. Premissas Consolidadas do MVP

O **LogGym** foi concebido como um aplicativo local, *single-user* e *offline-first*, sem autenticação, backend remoto ou sincronização entre dispositivos no escopo do MVP **[I]**.

### Modelo Conceitual Principal
```text
Plano → Treinos/Divisões → Exercícios configurados → Sessão → Séries realizadas
```

* **Exemplo de Estrutura:**
  * **Plano:** PPL (Push / Pull / Legs)
    * **Push**
      * Supino reto
        * Meta: 3 séries
        * Repetições: 6–10
        * Descanso: 180 s
    * **Pull**
    * **Legs**

> **Regra Fundamental:** O plano representa a **prescrição futura**, enquanto a sessão representa o que **efetivamente aconteceu**. Essa separação garante que alterações futuras em um plano não modifiquem o histórico de treinos passados.

---

## 2. Inicialização e Onboarding

| ID | Requisito | Origem |
| :--- | :--- | :---: |
| **RF-01** | O sistema deve identificar a primeira criação do banco local e realizar a carga inicial dos exercícios nativos. | `[D]` |
| **RF-02** | A carga inicial de exercícios nativos deve ocorrer apenas uma vez, sem criar duplicatas nas inicializações posteriores. | `[I]` |
| **RF-03** | No primeiro acesso, o sistema deve apresentar uma tela de onboarding. | `[D]` |
| **RF-04** | O onboarding deve permitir ao usuário selecionar um plano pré-definido. | `[D]` |
| **RF-05** | O onboarding deve permitir ao usuário iniciar a criação de um plano personalizado. | `[D]` |
| **RF-06** | O usuário poderá ignorar a escolha de plano durante o onboarding. | `[I]` |
| **RF-07** | Caso não exista plano ativo, o sistema deve impedir o início de um treino e direcionar o usuário para criação ou seleção de um plano. | `[I]` |
| **RF-08** | O onboarding não deve ser exibido novamente após sua conclusão, salvo se os dados locais do aplicativo forem apagados. | `[I]` |

> **Nota Técnica:** O efeito funcional do RF-01 pode ser implementado de forma limpa utilizando o `RoomDatabase.Callback`.

---

## 3. Biblioteca de Exercícios

| ID | Requisito | Origem |
| :--- | :--- | :---: |
| **RF-09** | O sistema deve disponibilizar uma biblioteca local de exercícios. | `[D]` |
| **RF-10** | A biblioteca deve conter exercícios nativos fornecidos pelo aplicativo. | `[D]` |
| **RF-11** | A biblioteca deve conter também exercícios personalizados criados pelo usuário. | `[D]` |
| **RF-12** | O sistema deve distinguir exercícios nativos de exercícios personalizados. | `[I]` |
| **RF-13** | O usuário deve poder criar um exercício personalizado. | `[D]` |
| **RF-14** | Todo exercício personalizado deve possuir pelo menos um nome. | `[I]` |
| **RF-15** | O usuário deve poder editar exercícios personalizados. | `[D]` |
| **RF-16** | O usuário deve poder excluir exercícios personalizados. | `[D]` |
| **RF-17** | A exclusão de um exercício personalizado deve exigir confirmação. | `[I]` |
| **RF-18** | Exercícios nativos não podem ser editados pelo usuário. | `[D]` |
| **RF-19** | Exercícios nativos não podem ser excluídos pelo usuário. | `[D]` |
| **RF-20** | Um exercício personalizado excluído não deve desaparecer de sessões históricas em que tenha sido utilizado. | `[I]` |
| **RF-21** | Caso um exercício personalizado excluído esteja presente em planos existentes, o sistema deve avisar o usuário e removê-lo apenas dessas configurações futuras após confirmação. | `[I]` |

---

## 4. Planos de Treino

### 4.1 Gerenciamento de Planos
| ID | Requisito | Origem |
| :--- | :--- | :---: |
| **RF-22** | O sistema deve permitir a existência de múltiplos planos de treino armazenados. | `[I]` |
| **RF-23** | O usuário deve poder criar um plano personalizado. | `[D]` |
| **RF-24** | Todo plano deve possuir um nome. | `[I]` |
| **RF-25** | O usuário deve poder editar um plano personalizado existente. | `[I]` |
| **RF-26** | O usuário deve poder excluir um plano. | `[I]` |
| **RF-27** | A exclusão de um plano deve exigir confirmação. | `[I]` |
| **RF-28** | Apenas um plano pode estar ativo por vez. | `[D]` |
| **RF-29** | Ao ativar um plano, qualquer outro plano anteriormente ativo deve ser automaticamente desativado. | `[I]` |
| **RF-30** | O sistema deve indicar visualmente qual plano está atualmente ativo. | `[I]` |
| **RF-31** | O usuário deve poder trocar o plano ativo. | `[I]` |
| **RF-32** | A troca do plano ativo deve afetar apenas sessões iniciadas posteriormente. | `[D]` |
| **RF-33** | A exclusão do plano ativo pode deixar o sistema temporariamente sem plano ativo. | `[I]` |
| **RF-34** | Enquanto não houver plano ativo, nenhuma nova sessão de treino poderá ser iniciada. | `[I]` |

### 4.2 Planos Pré-definidos
| ID | Requisito | Origem |
| :--- | :--- | :---: |
| **RF-35** | O sistema deve oferecer planos pré-definidos durante o onboarding e posteriormente na área de planos. | `[D]` |
| **RF-36** | O sistema deve oferecer suporte a estruturas como **Bro Split**, **Push/Pull/Legs** e **Upper/Lower**. | `[D]` |
| **RF-37** | Ao selecionar um plano pré-definido, o sistema deve criar uma instância local do plano para o usuário. | `[I]` |
| **RF-38** | A instância criada a partir de um plano pré-definido deve poder ser posteriormente personalizada sem alterar o template original do aplicativo. | `[I]` |

---

## 5. Divisões / Treinos dentro de um Plano

Um plano precisa possuir unidades executáveis (ex.: divisões como *Push, Pull, Legs* ou *Peito, Costas, Pernas*).

| ID | Requisito | Origem |
| :--- | :--- | :---: |
| **RF-39** | Um plano deve conter um ou mais treinos/divisões. | `[I]` |
| **RF-40** | Cada treino/divisão deve possuir um nome. | `[I]` |
| **RF-41** | O usuário deve poder adicionar divisões a um plano personalizado. | `[I]` |
| **RF-42** | O usuário deve poder renomear divisões. | `[I]` |
| **RF-43** | O usuário deve poder excluir divisões. | `[I]` |
| **RF-44** | O usuário deve poder alterar a ordem das divisões dentro do plano. | `[I]` |
| **RF-45** | Cada divisão deve conter uma lista ordenada de exercícios. | `[I]` |
| **RF-46** | O usuário deve poder adicionar exercícios da biblioteca a uma divisão. | `[I]` |
| **RF-47** | O usuário deve poder remover exercícios de uma divisão. | `[I]` |
| **RF-48** | O usuário deve poder alterar a ordem dos exercícios de uma divisão. | `[I]` |

> **Nota de Escopo:** Não há presunção de agenda semanal fixa (ex.: *Push* não precisa obrigatoriamente acontecer na segunda-feira). O usuário escolhe manualmente qual treino do plano ativo deseja executar.

---

## 6. Configuração de um Exercício dentro do Plano

As metas de séries, repetições e descanso pertencem à **relação do exercício com aquele treino específico**, e não ao exercício globalmente.

| ID | Requisito | Origem |
| :--- | :--- | :---: |
| **RF-49** | Para cada exercício adicionado a um treino, o usuário deve definir uma meta de séries. | `[D]` |
| **RF-50** | A meta de séries deve ser um inteiro positivo. | `[I]` |
| **RF-51** | Para cada exercício, deve ser definida uma faixa mínima e máxima de repetições. | `[D]` |
| **RF-52** | A repetição mínima não pode ser maior que a repetição máxima. | `[I]` |
| **RF-53** | Os valores de repetições devem ser inteiros positivos. | `[I]` |
| **RF-54** | Cada exercício do treino deve possuir seu próprio tempo de descanso. | `[D]` |
| **RF-55** | O descanso deve ser armazenado como duração, independentemente do restante dos exercícios. | `[I]` |
| **RF-56** | Um descanso igual a zero deve significar cronômetro desativado para aquele exercício. | `[I]` |
| **RF-57** | A configuração de um mesmo exercício pode ser diferente em treinos diferentes (ex.: *Supino no Upper A* pode ter parâmetros diferentes do *Upper B*). | `[I]` |

---

## 7. Início de uma Sessão

| ID | Requisito | Origem |
| :--- | :--- | :---: |
| **RF-58** | O usuário deve poder iniciar uma sessão a partir de uma divisão do plano ativo. | `[I]` |
| **RF-59** | Ao iniciar a sessão, o sistema deve registrar data e horário de início. | `[I]` |
| **RF-60** | O sistema deve criar imediatamente um rascunho persistente da sessão. | `[D]` |
| **RF-61** | A sessão deve receber uma cópia (*snapshot*) dos dados relevantes da configuração utilizada naquele momento. | `[I]` |
| **RF-62** | Alterações realizadas no plano após o início da sessão não devem alterar a sessão já iniciada. | `[I]` |
| **RF-63** | Apenas uma sessão de treino pode permanecer em andamento simultaneamente. | `[I]` |
| **RF-64** | Se já existir uma sessão em andamento, o sistema não deve iniciar outra até que a anterior seja retomada e finalizada ou descartada. | `[I]` |

---

## 8. Pré-preenchimento das Séries

| ID | Requisito | Origem |
| :--- | :--- | :---: |
| **RF-65** | Ao iniciar um exercício, suas séries devem ser inicialmente preenchidas com os valores registrados na sessão concluída mais recente daquele exercício. | `[D]` |
| **RF-66** | Apenas sessões efetivamente finalizadas devem ser utilizadas como fonte do pré-preenchimento. | `[I]` |
| **RF-67** | Rascunhos e sessões descartadas não devem ser considerados como treino anterior. | `[I]` |
| **RF-68** | A associação do histórico deve ocorrer pela identidade do exercício, e não apenas pelo seu nome. | `[I]` |
| **RF-69** | As séries anteriores devem ser associadas às séries atuais pela sua posição ordinal. | `[I]` |
| **RF-70** | Se houver menos séries históricas que séries previstas atualmente, as séries adicionais devem iniciar vazias. | `[I]` |
| **RF-71** | Se houver mais séries históricas que séries previstas atualmente, apenas as séries correspondentes existentes no treino atual devem ser pré-preenchidas. | `[I]` |
| **RF-72** | Se nunca houver sido concluída uma sessão contendo aquele exercício, carga e repetições devem iniciar vazias. | `[I]` |

---

## 9. Execução das Séries

| ID | Requisito | Origem |
| :--- | :--- | :---: |
| **RF-73** | Cada série deve permitir o registro da carga utilizada. | `[I]` |
| **RF-74** | Cada série deve permitir o registro da quantidade de repetições executada. | `[I]` |
| **RF-75** | A carga deve aceitar valores decimais e valor zero. | `[I]` |
| **RF-76** | O valor zero deve permitir registrar exercícios sem carga externa. | `[I]` |
| **RF-77** | A quantidade de repetições deve ser um número inteiro não negativo. | `[I]` |
| **RF-78** | O usuário deve poder alterar os valores pré-preenchidos antes de concluir a série. | `[I]` |
| **RF-79** | Cada série deve possuir um estado indicando se foi concluída. | `[D]` |
| **RF-80** | O usuário deve marcar explicitamente uma série como concluída por meio de um check. | `[D]` |
| **RF-81** | Uma série não marcada como concluída não deve ser considerada executada no registro final. | `[I]` |
| **RF-82** | O usuário deve poder desmarcar uma série marcada acidentalmente. | `[I]` |
| **RF-83** | O usuário deve poder adicionar uma série extra durante uma sessão. | `[I]` |
| **RF-84** | O usuário deve poder remover uma série ainda não concluída durante a sessão. | `[I]` |
| **RF-85** | Séries adicionadas ou removidas durante uma sessão devem alterar apenas aquela sessão, sem modificar automaticamente o plano. | `[I]` |
| **RF-86** | O usuário pode finalizar um treino mesmo sem realizar todas as séries previstas. | `[I]` |

---

## 10. Cronômetro de Descanso

| ID | Requisito | Origem |
| :--- | :--- | :---: |
| **RF-87** | Ao marcar uma série como concluída, o sistema deve iniciar automaticamente o cronômetro correspondente ao descanso configurado para aquele exercício. | `[D]` |
| **RF-88** | O cronômetro deve exibir o tempo restante. | `[I]` |
| **RF-89** | Se o descanso configurado for zero, nenhum cronômetro deve ser iniciado. | `[I]` |
| **RF-90** | Deve existir no máximo um cronômetro de descanso ativo por sessão. | `[I]` |
| **RF-91** | Caso outra série seja concluída enquanto houver cronômetro ativo, o cronômetro deve ser reiniciado usando o descanso correspondente à nova série concluída. | `[I]` |
| **RF-92** | O usuário deve poder encerrar/pular manualmente o descanso. | `[I]` |
| **RF-93** | O tempo de descanso deve continuar logicamente transcorrendo caso o aplicativo vá para segundo plano. | `[D]` |
| **RF-94** | Ao retornar à tela, o sistema deve reconstruir o tempo restante a partir do instante final armazenado, em vez de depender de um contador continuamente executado. | `[I]` |
| **RF-95** | Caso o horário final já tenha passado, o sistema deve apresentar o descanso como concluído. | `[I]` |

> **Decisão Técnica (Timer):** Utiliza-se **timestamp** como fonte da verdade em vez de um `ForegroundService`. 
> * $\text{restStartedAt} = \text{agora}$
> * $\text{restEndsAt} = \text{agora} + 180\text{ s}$
> * Interface calcula: $\text{remaining} = \text{restEndsAt} - \text{agora}$
> 
> Isso torna o timer resistente à recomposição do Jetpack Compose, troca de tela e encerramentos de processo, eliminando a complexidade de serviços em background no MVP.

---

## 11. Rascunho da Sessão

| ID | Requisito | Origem |
| :--- | :--- | :---: |
| **RF-96** | Toda sessão iniciada deve existir inicialmente como rascunho. | `[D]` |
| **RF-97** | Mudanças relevantes da sessão devem ser persistidas durante sua execução de forma contínua. | `[I]` |
| **RF-98** | O rascunho deve preservar séries, cargas, repetições, checks e demais dados necessários para reconstruir a sessão. | `[D/I]` |
| **RF-99** | O fechamento inesperado do aplicativo não deve eliminar o rascunho existente. | `[D]` |
| **RF-100** | Reiniciar o dispositivo ou perder bateria não deve transformar o rascunho em sessão concluída. | `[I]` |
| **RF-101** | Ao abrir o aplicativo e encontrar um rascunho, o sistema deve informar que existe uma sessão em andamento. | `[D]` |
| **RF-102** | O usuário deve poder retomar a sessão. | `[D]` |
| **RF-103** | O usuário deve poder descartar a sessão em andamento. | `[I]` |
| **RF-104** | O descarte deve exigir confirmação explícita. | `[I]` |
| **RF-105** | Um rascunho descartado não deve aparecer no histórico. | `[I]` |
| **RF-106** | Alterações realizadas posteriormente no plano ou nos exercícios não devem modificar o rascunho já criado. | `[I]` |

---

## 12. Finalização da Sessão

| ID | Requisito | Origem |
| :--- | :--- | :---: |
| **RF-107** | A sessão somente pode se tornar um registro definitivo quando o usuário selecionar explicitamente "Finalizar Treino". | `[D]` |
| **RF-108** | Fechar a tela, navegar para outra tela ou fechar o aplicativo não deve finalizar automaticamente a sessão. | `[D/I]` |
| **RF-109** | O sistema deve permitir finalizar uma sessão parcialmente executada. | `[I]` |
| **RF-110** | Caso existam séries previstas e não concluídas, o sistema deve solicitar confirmação antes da finalização. | `[I]` |
| **RF-111** | Uma sessão sem nenhuma série realizada não deve ser salva como treino concluído. | `[I]` |
| **RF-112** | Ao finalizar, o sistema deve registrar o horário de término da sessão. | `[I]` |
| **RF-113** | O sistema deve preservar data/hora de início e término da sessão. | `[I]` |
| **RF-114** | Após a finalização bem-sucedida, o rascunho correspondente deve deixar de existir. | `[I]` |
| **RF-115** | A conversão de rascunho para sessão concluída deve ser atomicamente consistente, evitando perda do treino ou duplicação entre histórico e rascunho. | `[I]` |

---

## 13. Histórico

| ID | Requisito | Origem |
| :--- | :--- | :---: |
| **RF-116** | O sistema deve manter um histórico das sessões concluídas. | `[D]` |
| **RF-117** | O histórico deve apresentar apenas sessões explicitamente finalizadas. | `[D]` |
| **RF-118** | O usuário deve poder visualizar a lista de sessões anteriores. | `[I]` |
| **RF-119** | As sessões devem ser identificáveis por data e treino realizado. | `[I]` |
| **RF-120** | O usuário deve poder abrir uma sessão anterior para consultar seus detalhes. | `[I]` |
| **RF-121** | Os detalhes devem apresentar os exercícios realizados. | `[I]` |
| **RF-122** | Os detalhes devem apresentar as séries efetivamente realizadas, incluindo carga e repetições. | `[I]` |
| **RF-123** | O histórico deve preservar os nomes e configurações relevantes existentes quando a sessão foi executada. | `[D]` |
| **RF-124** | Renomear posteriormente um exercício não deve modificar seu nome exibido em treinos antigos. | `[I]` |
| **RF-125** | Excluir posteriormente um exercício não deve remover seus registros históricos. | `[I]` |
| **RF-126** | Alterar número de séries, faixa de repetições ou descanso de um exercício não deve alterar sessões anteriores. | `[D]` |
| **RF-127** | Alterar ou excluir o plano de origem não deve alterar sessões anteriores. | `[D/I]` |
| **RF-128** | O histórico concluído será somente leitura no MVP. | `[I]` |

---

## 14. Regra Central de Isolamento Temporal

* **RF-129:** Uma sessão deve representar imutavelmente o estado relevante do treino no momento exato em que foi executada **[D/I]**.
  * *Explicação:* O relacionamento histórico não deve apontar diretamente para a entidade mutável `Exercise` atual. Caso um exercício mude de nome ou seja apagado, o histórico permanece perfeitamente preservado através de dados estáticos (*snapshots*).

---

## 15. Fluxo Funcional Completo

```text
Primeiro Acesso 
  → Carga de Exercícios Nativos 
  → Onboarding 
  → Selecionar/Criar Plano 
  → Plano Ativo 
  → Selecionar Divisão (Push/Pull/Legs/A/B) 
  → Iniciar Treino 
  → Criar Draft + Snapshot 
  → Buscar Último Histórico de cada Exercício 
  → Pré-preencher Séries 
  → Usuário Altera Carga/Reps 
  → Check da Série 
  → Salvar Draft 
  → Iniciar Descanso 
  → Finalizar Treino 
  → Persistir Sessão Concluída 
  → Remover Draft 
  → Histórico
```

* **Cenário de Interrupção (App Morre):**
  `Treino em execução` → `Draft já persistido` → `App encerra` → `App abre novamente` → `Alerta de sessão em andamento` → `Retomar / Descartar`.

---

## 16. Resumo das Lacunas Preenchidas por Inferência

| Aspecto | Decisão Adotada para o MVP |
| :--- | :--- |
| **Login / Conta** | Inexistente. Um único usuário local por instalação. |
| **Backend** | Inexistente. Dados armazenados localmente via Room. |
| **Planos salvos** | Permitidos múltiplos planos, mas apenas um ativo por vez. |
| **Sem plano ativo** | Permitido no app, mas impede o início de novos treinos. |
| **Escolha do treino do dia** | Manual. O usuário escolhe qual divisão quer executar. |
| **Templates pré-definidos** | Servem como modelo e geram cópias locais editáveis. |
| **Exercício sem histórico** | Séries começam totalmente vazias. |
| **Diferença de séries (passado vs. atual)** | Correspondência por posição ordinal; faltantes ficam vazias. |
| **Rascunho como histórico anterior** | Não. Apenas sessões finalizadas contam como treino anterior. |
| **Séries não marcadas** | Não são consideradas executadas no registro final. |
| **Meta de séries** | O usuário pode fazer menos ou mais séries (via adição manual), sem alterar o plano original. |
| **Carga zero** | Permitida para exercícios sem carga externa. |
| **Alteração de plano/exercício/histórico** | Isoladas; não afetam sessões em andamento ou finalizadas. |
| **Sessões simultâneas** | Não permitidas. Apenas uma por vez. |
| **Sessão vazia** | Não pode ser finalizada. |
| **Cronômetro** | Baseado em timestamp persistido (sem Foreground Service). |

---

## 17. Lacunas Pendentes (Decisões de Produto)

Itens que não possuem resposta óbvia por senso comum e dependem de diretrizes de escopo:

1. **Conteúdo da biblioteca nativa [P]:** Quais exercícios nativos incluir, quantidade exata, se terão grupos musculares associados, equipamentos e instruções. (*Recomendação para o MVP: manter simples com ID, Nome, flag `isCustom` e opcionalmente `muscleGroup`*).
2. **Conteúdo exato dos planos pré-definidos [P]:** A estrutura PPL ou Upper/Lower genérica não define quais exercícios específicos compõem cada dia, quantidade de séries e faixas de repetições de fábrica.
3. **Unidade de carga padrão [P]:** Utilizar quilogramas (**kg**) para o contexto nacional (recomendado).
4. **Alertas de descanso fora do app [P]:** Decidir se o celular deve vibrar/tocar exatamente ao fim do descanso caso o usuário feche o aplicativo (o que exigiria `AlarmManager` ou permissões adicionais). *Recomendado manter fora do MVP*.

---

## 18. Escopo Explicitamente Fora do MVP

Para evitar o crescimento descontrolado do projeto, os seguintes itens estão descartados nesta fase:
* Autenticação e cadastro de usuários / nuvem / sincronização
* Compartilhamento social, amigos e perfis de personal trainers
* Gráficos complexos de progresso, cálculo de $1\text{RM}$ e rastreio de recordes pessoais (PRs)
* Volume semanal, estatísticas musculares e calendário/agenda semanal obrigatória
* Notificações push de "hora de treinar"
* Importação/exportação de dados, fotos e vídeos de exercícios
* Integração com smartwatches ou Google Fit / Health Connect
* Métodos avançados (Supersets, Drop sets, RPE/RIR, notas por série)
* Exercícios baseados em tempo ou distância
* Edição retroativa de histórico

---

## 19. A Regra Arquitetural Mais Importante do Projeto

O domínio do LogGym resume-se a três conceitos fundamentais que **jamais** devem ser mapeados na mesma entidade de banco de dados ou domínio:

1. `Exercise` $\rightarrow$ Definição reutilizável do movimento base.
2. `WorkoutExercise` $\rightarrow$ Prescrição daquele exercício dentro de uma rotina específica.
3. `PerformedExercise` / `SessionExercise` $\rightarrow$ Fotografia histórica (*snapshot*) da prescrição unida à execução real.

> **Por que isso importa?** Essa separação arquitetural resolve de forma nativa e elegante a imutabilidade do histórico, o pré-preenchimento inteligente, a exclusão/renomeação segura de exercícios e a integridade de rascunhos sem acoplamento indevido.