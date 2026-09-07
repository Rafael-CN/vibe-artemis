# Vidro — o que entrou no app

> **Histórico.** A identidade "Vidro" documentada aqui foi substituída pela tese
> **"Console"** (decidida em 4 de setembro de 2026, `docs/planos/B-CONSOLE.md`) — fonte de
> verdade viva de tokens e telas em `docs/design/console/` (`NOTA.md`, `artboards/`,
> `fotos/`). A fatia F6 apagou os aliases de cor `vidro_*` que ligavam este documento ao
> `console_*` (cada nome abaixo hoje é direto `console_*` em `res/values/colors.xml`, sem
> a camada de indireção); os achados de arquitetura (SurfaceView/blur, a regra de fosco em
> sessão) continuam válidos e foram herdados pela Console. Dimensões e drawables `vidro_*`
> que sobrevivem em `res/` são a identidade Vidro original, ainda usada por telas legadas
> não redesenhadas (perfis, ajustes de stream) — não aliases.

Registro do que veio do bundle do Claude Design (projeto *VibeArtemis: Sinal*, arquivos
`Vidro Estados e Prova`, `Vidro Prototipo v2`) e como cada peça foi traduzida para Android.

O bundle é HTML/CSS/React e o app é Android nativo, então **o código não se aproveita**.
O que atravessa é o sistema: cor, escala, forma, movimento e comportamento dos estados.

## O achado que mandou no resto

O painel `8c` do handoff traz uma descoberta técnica que muda a identidade inteira:

> O vídeo desenha num `StreamView extends SurfaceView` — uma camada de hardware separada
> da árvore de views. Um painel translúcido por cima **não consegue amostrar o vídeo**:
> `RenderEffect.createBlurEffect` borraria apenas o que está na mesma camada, ou seja,
> nada.

Trocar para `TextureView` resolveria o desenho e custaria o que a tese não pode pagar: uma
cópia de textura por quadro e um quadro a mais de atraso, num app cujo ponto é latência.

**A regra que saiu disso, e que está implementada:**

| Onde | Tratamento | Porquê |
|---|---|---|
| Em sessão | **Fosco** — `@color/vidro_glass_fill_session` (alpha 0,82) + borda de 1px | Blur ali borraria o nada, e o decodificador precisa da GPU inteira |
| Fora da sessão | Blur permitido, alpha 0,55 | O fundo é composto pela própria hierarquia; ali sai barato |
| Nunca | Blur animado, blur em tela cheia, blur abaixo da API 31 | Recomposição por quadro |

Por isso `vidro_glass_session.xml` e `vidro_glass_pill.xml` são cor sólida translúcida e
não `RenderEffect`. Não é limitação de esforço: é a leitura correta da plataforma.

## Cor

Os treze tokens vieram em OKLCH e foram medidos por APCA no navegador contra o pior fundo
possível (uma cena de vídeo clara), não estimados. A conversão para sRGB está em
`res/values/colors.xml` com o valor OKLCH de origem em comentário ao lado de cada um.

**O acento tem três papéis e eles não são intercambiáveis.** O tom a L 64% reprova em APCA
tanto como texto quanto como preenchimento — ele serve só para acender:

- `vidro_accent_glow` (L 64%) — halo, brilho, nunca texto
- `vidro_accent_text` (L 80%) — texto e ícone
- `vidro_accent_fill` (L 78%) — preenchimento de botão, com `vidro_on_accent` por cima

O tema aponta `colorAccent` para o papel de **texto**. Era o erro fácil aqui.

## A luz como estado

`pc_grid_item.xml` ganhou `grid_halo`, e `PcGridAdapter` define a alpha dela pelo estado do
host: aceso 0,72 · procurando 0,34 · dormindo 0,20. São os valores do handoff.

O que muda é **intensidade, nunca matiz** — então quem não distingue o azul continua lendo
a hierarquia. O rótulo de texto segue existindo como reforço, jamais como fonte única.

## Movimento

As curvas do handoff estão em `res/interpolator/`:

| Transição | ms | Curva |
|---|---|---|
| capa → vídeo | 420 | `vidro_enter` (.22,.72,0,1) |
| vídeo → card | 340 | `vidro_exit` (.32,.72,0,1) |
| ilha recolhe | 260 | `vidro_standard` (.4,0,.2,1) |
| folha sobe | 240 | `vidro_sheet` (.2,.8,.2,1) |

O handoff é explícito sobre o que **não** anima: blur, sombra e largura, os três forçam
recomposição por quadro. O recuo usa só transformação.

## O que ficou de fora, e por quê

**A face tipográfica.** O bundle roda em Manrope + JetBrains Mono. O app está com a face do
sistema. Embutir uma face de verdade é o maior ganho de personalidade que resta, e é uma
decisão do dono: ou baixar os TTFs e empacotar (~200 KB, licença SIL nos dois casos), ou
usar Downloadable Fonts via Play Services, que não pesa no APK mas depende de rede na
primeira carga. A escala, os pesos e o tracking já estão implementados, então trocar a face
mexe só nos estilos `VidroText.*`.

**A antecipação** (a feature exclusiva da tese: sugerir a ação provável pelo horário e
hábito) não foi implementada. É produto, não pintura, e vale como EPIC próprio.

**A sala escura** (depois das 22h a luz cai para 0,34 e o texto para L 90%): os tokens
existem — `vidro_text_night`, `vidro_halo_night` — mas nada os aciona ainda.

**As telas de pareamento e ajustes** herdaram o tema e ficaram coerentes, mas não foram
redesenhadas tela a tela.

## Onde está cada coisa

```
res/values/colors.xml        13 tokens + composições de vidro + bordas por peso
res/values/dimens.xml        raios, espaçamento, alcance do polegar
res/values/styles.xml        tema + escala tipográfica VidroText.* + componentes
res/interpolator/            as quatro curvas do handoff
res/drawable/vidro_*         vidro fosco, halo, cartão, botões, moldura de capa
res/layout/pc_grid_item      o host com a luz atrás
res/layout/app_grid_item     a capa com véu e moldura
res/layout/activity_game     overlays em sessão, fosco e com números em tabular
```
