# LogGym — Identidade Visual A5 · Índigo técnico

## 1. Decisão de design

- **Conceito aprovado:** direção **A5**: halter horizontal com anilhas de cantos arredondados e eixo estilizado como lápis. A barra e a ponta são desenhadas com geometria simples; a dupla leitura é intencional.
- **Paleta aprovada:** **C — Índigo técnico**.
- **Estética:** minimalismo funcional; sem gradientes, sombras decorativas ou efeitos 3D.
- **Status:** proposta vetorial pronta para integração e avaliação visual em dispositivos. Não há verificação de marca registrada.

## 2. Paleta (HEX exato)

| Papel | Tema claro | Tema escuro |
|---|---|---|
| Fundo | `#F8F7F5` | `#191B25` |
| Superfície | `#FFFFFF` | `#242735` |
| Texto principal | `#292B35` | `#EEEDF6` |
| Texto secundário | `#646577` | `#B5B5C7` |
| Destaque / Primary | `#505F8C` | `#AAB7EB` |
| Divisores / outlineVariant | `#DEDFE5` | `#3C4056` |

Combinações recomendadas: texto branco (`#FFFFFF`) sobre o primary claro `#505F8C`; texto `#191B25` sobre o primary escuro `#AAB7EB`. Contrastes aproximados desses pares: 6,26:1 e 8,72:1, respectivamente. Textos primários e secundários nas superfícies-base também passam AA em contraste de texto comum, mas os componentes reais precisam ser testados com seus próprios estados.

**Observação sobre Material 3:** a paleta acima são seis papéis visuais por tema, não todos os papéis obrigatórios de `ColorScheme`. O Codex pode derivar `primaryContainer`, `surfaceVariant`, `outline` e cores semânticas mantendo coerência, sem substituir os seis HEX aprovados. Evite usar `outlineVariant` como a única borda de um campo que precisa ser percebido.

## 3. Conteúdo dos arquivos SVG

### `svg/symbol/`
- `symbol-indigo.svg`: símbolo índigo `#505F8C` em fundo transparente, para fundos claros.
- `symbol-indigo-light.svg`: símbolo `#AAB7EB` em fundo transparente, para fundos escuros.
- `symbol-black.svg` e `symbol-white.svg`: variações de uma cor para usos monocromáticos.

### `svg/logo/`
- `logo-horizontal-light.svg`: símbolo índigo + `LogGym` em grafite sobre fundo transparente.
- `logo-horizontal-dark.svg`: símbolo azul claro + `LogGym` em texto claro sobre fundo transparente.
- `wordmark-light.svg` e `wordmark-dark.svg`: nome da marca isolado.
- O wordmark usa curvas vetoriais do **Inter Display Bold**, sem depender da fonte instalada. Os arquivos **não incluem nenhum arquivo de fonte**.

### `svg/launcher/`
- `launcher-indigo.svg`: visualização de ícone índigo com símbolo off-white.
- `launcher-light.svg` e `launcher-dark.svg`: alternativas visuais para documentação.
- `adaptive-foreground.svg`: foreground em `108 × 108` e fundo transparente para conversão em VectorDrawable.
- `adaptive-monochrome.svg`: foreground de uma única cor em `108 × 108`, fundo transparente, para **themed icons** do Android 13+.
- `adaptive-background.svg`: quadrado de cor uniforme `#505F8C`.

**Importante:** o fundo arredondado dos previews não deve ser embutido no foreground adaptativo. O Android aplica sua própria máscara. A área de segurança foi preservada; não aumente o foreground até encostar nas bordas.

## 4. Extras prontos para Android

`android/drawable/` contém vetores Android (XML) já convertidos, para reduzir risco de conversão divergente: `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`, `ic_launcher_background.xml` e `ic_loggym_symbol.xml`.

`android/mipmap-anydpi-v26/` contém os arquivos `ic_launcher.xml` e `ic_launcher_round.xml`. `android/values/colors_loggym.xml` contém a paleta nomeada.

Esses arquivos são **referências prontas para adaptação**. Copie-os para `app/src/main/res` observando os recursos existentes e a configuração atual do manifest. O ícone adaptativo requer API 26+, enquanto a camada monocromática é usada em versões compatíveis, geralmente Android 13+. Para versões antigas, gere também PNGs legacy de ícone (mipmap por densidade) se o projeto precisar deles. Não substitua recursos aleatoriamente sem inspecionar o projeto.

## 5. Diretrizes de uso

- Não distorcer, rotacionar, espelhar ou adicionar contorno ao símbolo A5.
- Preserve o espaçamento entre anilhas e eixo central e o espaço negativo junto à ponta.
- Não inserir texto dentro do ícone do launcher.
- Em fundos claros use o símbolo índigo; em fundos escuros use o índigo claro ou versão branca, conforme contraste.
- Para telas de treino, números (kg, repetições, duração) devem ter hierarquia visual; a cor de destaque fica restrita a ações e estados significativos.
- Não indique conclusão, erro ou descanso somente pela cor; use ícones, rótulos ou padrões complementares.
- Em componentes Material 3, preserve os tamanhos mínimos de toque e a adaptação a fontes aumentadas.

## 6. Fontes

Logotipo horizontal: **Inter Display Bold**, convertido em paths nos SVGs. Interface Android: avalie Inter ou mantenha Roboto existente, respeitando legibilidade e custo de adição de fontes. **Não é necessário importar a fonte do logotipo** para exibir os SVGs.

## 7. Validação recomendada

1. Conferir vetor e PNG rasterizado do símbolo em 24, 32, 48 e 64 px (o detalhe do lápis pode desaparecer nos menores tamanhos; a silhueta do halter deve continuar reconhecível).
2. Visualizar app icon com máscaras circular, squircle e arredondada, com margens preservadas.
3. Testar tema claro/escuro nos componentes reais e escalonamento de fonte; usar WCAG 2.2 AA como referência.
4. Validar ícone adaptativo em launcher Android e **themed icon** quando compatível.
5. Comparar com ícones anteriores da aplicação para evitar inconsistências e regressões.
