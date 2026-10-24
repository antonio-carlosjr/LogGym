# Demonstração do MVP — Sprint 3

## Preparação

Usar o APK de release com assinatura definitiva verificada, em dispositivo de demonstração. Criar plano com uma divisão, um exercício de carga/repetições e um por tempo, três séries e descanso de 15 s. Manter o app offline. Preparar acesso ao `adb` e confirmar o serial do dispositivo. Ensaiar antes da apresentação; a existência deste roteiro não comprova ensaio ou aceite humano.

## Sequência sugerida — 6 a 8 minutos

1. Mostrar o onboarding e o aviso de saúde. Explicar biblioteca nativa/personalizada e plano ativo único.
2. Abrir o editor e mostrar as prescrições distintas de carga/repetições e tempo.
3. Iniciar um treino. Registrar `22,5 kg/8 reps` e `45 s`, marcar séries e mostrar o estado de conclusão.
4. Rolar a tela e mostrar o cronômetro fixo; aguardar o aviso final desaparecer ou usar Pular em uma nova contagem.
5. Esperar “Alterações salvas”, enviar ao fundo, executar `adb -s <serial> shell am kill com.loggym`, confirmar ausência de PID e reabrir. Retomar o mesmo treino misto.
6. Marcar uma série vazia, deixar outra pendente e abrir a confirmação de finalização. Cancelar uma vez, depois confirmar.
7. Abrir o histórico e o detalhe somente leitura. Mostrar valores ausentes distintos de zero e duração manual.
8. Iniciar novamente a divisão e mostrar o pré-preenchimento com checks desmarcados. Descartar esse novo rascunho com confirmação.

## Encerramento

Explicar que planos, prescrições e registros históricos são separados. Identificar as pendências conhecidas: aceite coletivo, homologação da Sprint 3 e entrega da chave definitiva, enquanto não confirmados. Recursos de Sprint 4, distância, fotos e kg/lb não devem ser apresentados como entregues.

Registrar responsável, data do ensaio, APK/certificado utilizado e dificuldades encontradas. O registro técnico dos testes está em [S3-IMPLEMENTACAO.md](S3-IMPLEMENTACAO.md).
