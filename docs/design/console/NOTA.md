# Opção B — Console

## 1 · Ideia e tradeoff

Um lançador sóbrio: capas grandes, fundo quase preto, navegação por **foco** (o item ativo
cresce e ganha contorno) que funciona igual no toque, nas setas da capa e num gamepad. A
premissa é que o uso real é abrir um jogo ou a Área de trabalho e ficar lá — o resto (Ajustes,
seletor de PC) é deliberadamente discreto e neutro para não competir com a capa.

**Tradeoff:** os Ajustes ficam menos densos — uma lista de uma coluna, um item por linha, sem
grade — e isso custa espaço vertical (os controles segmentados por si só exigem ~80 px de
altura). E o foco como linguagem central só funciona se a implementação Android tratar
`View.OnFocusChangeListener`/D-pad com disciplina em toda superfície; meio-implementado, essa
opção lê pior que a A.

## 2 · Tokens

### Cor (hex — papel)

| Token | Hex | Papel |
|---|---|---|
| `bg-base` | `#0B0B0D` | fundo do app, vídeo de fora da sessão |
| `bg-surface` | `#16161A` | cards em repouso, listas, painéis, barra da sessão |
| `bg-surface-elevada` | `#1C1C20` | popover do seletor de PC, item com foco, capa focada |
| `bg-surface-selecionada` | `#22232A` | linha "atual" no seletor, capa focada (fundo) |
| `borda` | `#2A2A30` | divisórias, contorno de cards em repouso |
| `texto-primario` | `#F2F2F4` | títulos, rótulos, valores |
| `texto-secundario` | `#9A9AA5` | legendas, estado, "on-line", valores inativos |
| `texto-desabilitado` | `#5A5A62` / `#6A6A72` | rótulo e legenda de item desabilitado |
| `acento` (frio, menta-gelo) | `#7FE0C7` | aprox. `oklch(82% 0.10 175)` — contorno de foco, indicador "em execução"/"on-line", botão primário |
| `acento-texto-sobre` | `#0A1F1A` | texto/ícone sobre superfície de acento (contraste AA) |
| `destrutivo` | `#F2555B` | aprox. `oklch(62% 0.19 25)` — "Encerrar no PC" |
| `destrutivo-fundo-sutil` | `rgba(242,85,91,0.12)` | fundo do bloco "Encerrar no PC" na barra da sessão |

Uma cor só faz o trabalho: `#7FE0C7` nunca aparece em decoração, só em foco, estado
"ligado/em execução" e botão primário — nada de azul, nada de segunda cor de marca.

### Tipografia (Manrope; JetBrains Mono só para dado técnico)

| Papel | Tamanho / peso / line-height | Onde |
|---|---|---|
| Título de tela | 32 / ExtraBold(800) / 1.15 | "Ajustes", "Computadores", "Apps" |
| Título de card/capa focada | 18–20 / Bold(700) / 1.2–1.3 | nome do app focado, "Área de trabalho" |
| Rótulo | 16 (tablet) / 14 (celular) / SemiBold(600–700) / 1.3 | linhas de ajuste, nome de PC, blocos de menu |
| Rótulo secundário | 14 / Medium(500) / 1.4 | "· on-line", legendas de estado |
| Legenda/caption | 11–12 / Medium–Bold / 1.3 | uppercase de seção, ajuda de item desabilitado |
| Dado técnico (mono) | 12–14 / Regular–Medium / tabular-nums | endereço IP, "18 ms · 35 Mbps", teclas especiais (Esc/Tab/Win…) |

### Raios, espaçamento, alturas

- **Unidade de espaçamento:** 4 px, mas o ritmo real usa múltiplos de 8 (8/12/16/20/24/28/32).
- **Raios:** 8 px (botões, chips, controle segmentado, gatilho da sessão) · 10–12 px (cards,
  capas, painéis, folha da sessão).
- **Altura de controle:** linha de Ajustes 64 px (tablet) / 56–68 px conforme o conteúdo
  (celular); botão 44–48 px; controle segmentado 36–40 px; toggle 22–24 px de trilho; bloco da
  barra da sessão ≥ 96 px de largura, altura livre (~64–76 px) por icone+rótulo.
- **Capas de app:** 3:4, 160×213 em repouso no tablet (180×240 aprox. no celular, 2 colunas),
  foco = ~1,2× maior + contorno de acento de 3 px + anel suave (`box-shadow` 4 px, 15% de
  opacidade do acento — não é brilho, é a mesma cor do contorno esmaecida).

## 3 · Convocação do menu da sessão

O gatilho é uma **aba pequena e discreta**, ancorada ao centro da borda superior do vídeo,
meio para dentro/meio para fora da moldura: 64×20 px (56×16 no celular), fundo neutro
(`#1C1C20`), sem cor de acento, com um chevron para baixo. Ela **descansa ali sempre**, em
qualquer estado da sessão (aparece desenhada em repouso em `Tablet_Sessao_Texto`). Um toque
nela desliza para baixo a barra horizontal de blocos — que é a janela de diálogo própria
exigida pela restrição de `SurfaceView`, visualmente distinta do vídeo por fundo sólido,
borda inferior e sombra discreta. Com o menu aberto, a mesma aba permanece visível — agora
presa à borda inferior da barra, com o chevron invertido — servindo de alça para recolher
(ver `Tablet_Sessao_Menu` e, mais evidente por causa da tela curta, `Celular_Sessao_Menu`).
Não é gesto de borda: é toque num alvo fixo.

## 4 · Dúvidas que decidi sozinho

- **Onde mostrar o NOTEBOOK fora do ar no tablet.** Não há artboard dedicado a "seletor de
  PC" no tablet — só o chip no cabeçalho. Decidi mostrar o chip já **aberto** como um popover
  ancorado abaixo dele (com um véu escuro por trás, sem blur) para caber o estado on-line +
  fora do ar + "Adicionar PC" dentro de `Tablet_Inicio`, em vez de inventar uma nona tela.
- **"Abrir/retomar e encerrar no PC" na Início.** Não fica em todo card — só o card
  "Continuar" (Área de trabalho, rodando) ganha "Retomar" + "Encerrar no PC"; o card com foco
  de teclado (Fortaleza, parado) ganha só "Abrir". Encerrar um app que não está rodando não
  faz sentido; duplicar os dois botões em toda capa poluiria a grade.
- **Estilo do "Encerrar no PC" fora da sessão.** Na Início uso texto vermelho sobre botão
  neutro (não o bloco cheio vermelho da folha da sessão) porque só a ação da sessão
  (restrição 6) pede tratamento destrutivo com confirmação; encerrar um app pela Início é
  reversível na prática (abre de novo) e não merece o mesmo peso visual.
- **Rótulos dos blocos da sessão no celular em paisagem.** Em 883×412 os 8 blocos não cabem
  inteiros ao lado dos dois fixos à direita — decidi um trilho que rola (recorte visível na
  borda) para o grupo da esquerda e manter Desconectar/Encerrar no PC sempre fixos à direita,
  como o brief pede literalmente ("no extremo direito").
- **Onde usar JetBrains Mono.** Só nos dados que mudam ou são código: endereço IP, o par
  latência+taxa de bits no rodapé da sessão, e os nomes das teclas especiais (Esc, Tab, Win…)
  — tratei essas teclas como "código", não como rótulo de interface comum.
- **Contexto de host em Ajustes.** O brief não pede, mas coloquei "ARTEMIS · 192.168.0.10" sob
  o título — sem isso a tela não deixa claro que HDR/Codec são por host (achado real do
  catálogo: Ajustes abre sem host óbvio quando vem da Início).
