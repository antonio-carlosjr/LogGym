# Prompt para o Codex — aplicar identidade visual LogGym A5

Você vai trabalhar no repositório **LogGym**, um aplicativo Android em **Kotlin + Jetpack Compose + Material Design 3**, que já possui temas claro/escuro, navegação e funcionalidades de treinamento. Quero integrar a **identidade visual A5** aprovada, sem redesenhar o aplicativo inteiro e sem alterar regras de negócio.

## Materiais e fonte da verdade

Estou anexando o arquivo `loggym-a5-indigo-tecnico.zip`. Leia primeiro o `README.md` e examine os SVGs e XMLs antes de editar o projeto. O símbolo aprovado é o **A5**: halter horizontal, anilhas com cantos suavemente arredondados, barra central remetendo a um lápis e ponta geométrica sutil. **Não substitua o desenho por ícones genéricos, não regenere o logotipo e não altere as proporções do SVG.** A identidade segue o **minimalismo funcional**: interface organizada, cores contidas, sem gradientes, sombras decorativas ou excesso de cards.

### Arquivos fornecidos

- `svg/symbol/symbol-indigo.svg` e `symbol-indigo-light.svg`: símbolo isolado para fundos claros/escuros.
- `svg/symbol/symbol-black.svg` e `symbol-white.svg`: símbolo em uma cor.
- `svg/logo/logo-horizontal-light.svg` e `logo-horizontal-dark.svg`: assinaturas com o nome LogGym, convertido em curvas (não precisam de fonte instalada).
- `svg/launcher/launcher-indigo.svg`, `launcher-light.svg` e `launcher-dark.svg`: prévias visuais, **não** foreground adaptativo.
- `svg/launcher/adaptive-foreground.svg` e `adaptive-monochrome.svg`: desenho com transparência e área de segurança do launcher.
- `android/drawable/` e `android/mipmap-anydpi-v26/`: recursos Android VectorDrawable e Adaptive Icon já preparados; use-os como base, compatibilizando com a estrutura de recursos do projeto, se houver conflitos de nomes.
- `android/values/colors_loggym.xml`: cores base nomeadas.

## Paleta escolhida — C, Índigo técnico

**Tema claro:**
- `background`: `#F8F7F5`
- `surface`: `#FFFFFF`
- `onBackground` / `onSurface`: `#292B35`
- `onSurfaceVariant` / texto secundário: `#646577`
- `primary`: `#505F8C`
- `outlineVariant` / divisores discretos: `#DEDFE5`
- `onPrimary`: `#FFFFFF`

**Tema escuro:**
- `background`: `#191B25`
- `surface`: `#242735`
- `onBackground` / `onSurface`: `#EEEDF6`
- `onSurfaceVariant` / texto secundário: `#B5B5C7`
- `primary`: `#AAB7EB`
- `outlineVariant` / divisores discretos: `#3C4056`
- `onPrimary`: `#191B25`

Crie ou ajuste os `ColorScheme` claro e escuro. Preencha os demais papéis do Material 3 (por exemplo `primaryContainer`, `onPrimaryContainer`, `secondary`, `surfaceVariant`, `outline`, `error`) com tons consistentes, documentando eventuais extensões. **Não altere os seis valores HEX aprovados de cada tema.** Diferencie `outline` funcional de `outlineVariant` decorativo. Se existir uso de cor dinâmica do sistema que substitua a identidade da marca, garanta que a paleta da marca seja o padrão, sem remover silenciosamente uma preferência já oferecida ao usuário.

## Implementação solicitada

1. Inspecione a estrutura atual de `Theme`, `ColorScheme`, recursos do launcher, `AndroidManifest.xml`, configuração de tema escuro e pontos onde a marca é exibida. Apresente um resumo do que existe antes de propor mudanças relevantes.
2. Integre a paleta no tema Material 3 de forma centralizada: evite cores HEX espalhadas por composables, preserve a alternância claro/escuro e verifique contrastes e estados de interação. Não mude a arquitetura ou o fluxo das telas sem necessidade.
3. Integre o ícone do launcher usando `ic_launcher_foreground.xml` + background índigo sólido e `ic_launcher_monochrome.xml` para **themed icons** compatíveis (Android 13+). Verifique `mipmap-anydpi-v26`, `android:icon` e `android:roundIcon` no manifest. Preserve a área de segurança da máscara. Se o app suporta launchers/versões anteriores à API 26, providencie ícones legacy por densidade e teste-os. Não use os SVGs com fundo arredondado como foreground adaptativo; a máscara é aplicada pelo Android.
4. Reaproveite a versão isolada do símbolo e a assinatura horizontal somente onde fizer sentido (por exemplo, apresentação institucional, cabeçalho ou tela Sobre). Não coloque o logotipo completo em botões, abas de navegação ou controles operacionais. SVG não é automaticamente um recurso Drawable do Android; use VectorDrawable equivalente, conversão apropriada ou recursos já fornecidos, sem introduzir dependência desnecessária.
5. Revise usos atuais de cores na navegação inferior, botões primários, seleção de séries, campos de texto, diálogos, histórico e cronômetro para respeitar o novo `ColorScheme`. Faça apenas ajustes visuais necessários para consistência; **não redesenhe as cinco abas nem altere comportamento de treino, histórico, planos ou backup**.
6. Garanta que concluído, não concluído, descanso ativo/encerrado, erro e desabilitado não dependam somente de cor; preserve rótulos, ícones e semântica de acessibilidade. Respeite escala de fonte, TalkBack, alvos de toque e contraste equivalente a WCAG AA quando aplicável.
7. Preserve builds, testes e compatibilidade já estabelecidos. Execute build, testes e lint disponíveis; se houver testes de interface ou screenshots, atualize/valide os afetados. Confira ícones em máscaras redonda e quadrada/arredondada e temas claro/escuro. Registre limitações quando não puder executar algum teste.

## Entrega e controle de mudanças

- Apresente lista de arquivos alterados e justificativa breve, além de capturas de tela antes/depois se a infraestrutura permitir.
- Diferencie claramente a **paleta aprovada** dos tons complementares inferidos para o Material 3.
- Evite mudanças não relacionadas, refatorações amplas e dependências novas sem motivo.
- Abra um **Pull Request** com a implementação, resumo de escopo, evidências e resultados dos testes. **Não faça merge sem minha autorização explícita.**

A meta é integrar uma identidade consistente, discreta e utilizável, **não** realizar um redesign completo da experiência nesta tarefa.
