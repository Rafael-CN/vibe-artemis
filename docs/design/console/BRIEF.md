# Brief — redesenho do VibeArtemis (o básico, super bem feito)

## O produto, em quatro linhas

VibeArtemis é um cliente Android de game streaming (fork do Artemis, que é fork do Moonlight).
Ele conecta num PC com Vibeshine (fork do Sunshine) e mostra a tela do PC — a Área de trabalho
ou um jogo — no aparelho. Usuário único: Rafael. Ele usa o app para **trabalhar e jogar no PC a
partir de um tablet com capa-teclado** e, no celular, para jogos e acesso rápido.

Aparelhos reais (as medidas dos artboards são estas, não outras):

| aparelho | artboard | observação |
|---|---|---|
| Galaxy S22 Ultra | 412 × 883 (retrato) e 883 × 412 (paisagem, para a sessão) | navegação por gestos do Android ligada |
| Tablet Android com capa-teclado | 1280 × 800 (paisagem) | teclado FÍSICO: nunca há teclado na tela nos artboards do tablet |

Escuro por padrão: é um app usado à noite, com tela OLED, ao lado de um jogo. Não desenhe tema claro.

## O que está sendo pedido — e o que NÃO está

Rafael reprovou o redesenho anterior ("Vidro/Âmbar": gaveta com física de mola, transições
compartilhadas, luz com temperatura). O veredito dele: *"te guiei em fazer o máximo de efeitos
e animações, mas no fim nem o básico a gente tinha feito bem feito."* O pedido agora é:

> **o básico, mas super bem feito, super bem polido. Nada mirabolante.**

Polimento aqui significa, concretamente:

- hierarquia tipográfica exata (uma escala declarada e obedecida: ex. 12 / 14 / 16 / 20 / 28 com line-heights);
- alinhamento e espaçamento consistentes (uma unidade de 4 ou 8 px, obedecida);
- **estados completos** desenhados: normal, pressionado, selecionado, desabilitado, foco (teclado);
- contraste AA no mínimo; alvos de toque ≥ 48 px; textos legíveis a 60 cm no tablet;
- conteúdo real em **pt-BR** (vocabulário do produto abaixo), sem lorem ipsum, sem "dados enfeite";
- **zero decoração**: sem gradientes decorativos, sem blur/vidro, sem sombras coloridas, sem brilho, sem emoji.
  Sombras só se forem a linguagem da direção (e discretas). Ícones = SVG inline, traço 1.5–2 px, grade 20/24.

Animações NÃO são assunto deste brief. A implementação usará as transições padrão da plataforma.

## Restrições duras (vêm da implementação; não são negociáveis)

1. **A sessão é um vídeo numa `SurfaceView`.** Qualquer coisa desenhada "por cima" do vídeo na
   mesma janela some no celular do Rafael (compositor da Samsung). Portanto: **o menu da sessão é
   uma janela de diálogo do Android** — pode ter qualquer forma (folha inferior, folha lateral,
   barra, caixa central), desde que seja visivelmente uma superfície própria, distinta do vídeo.
2. **O gatilho do menu NÃO é gesto de borda** (conflita com os gestos do sistema; já falhou).
   É um controle pequeno, sempre visível, tocável, arrastável — desenhe-o em repouso e explique
   onde ele descansa.
3. **Barra de texto** (a razão de existir do fork): um campo onde se digita (com o teclado da
   capa ou o teclado virtual) e se envia o texto inteiro de uma vez ao PC, com uma linha de teclas
   especiais. No celular, quando o teclado virtual sobe, **o vídeo encolhe para continuar visível**
   (não é coberto). Nos artboards do tablet o teclado é físico: só a barra aparece.
4. Fontes: só as que o app embarca — **Manrope** (interface) e **JetBrains Mono** (dados técnicos:
   IP, latência, bitrate, códigos). Carregue as duas do Google Fonts no `<helmet>` com fallback
   (`Manrope, "Segoe UI", system-ui, sans-serif` · `"JetBrains Mono", Consolas, monospace`).
5. "Ajustes avançados ficam": há ~126 preferências em 13 telas. **Esconder pode, apagar não.**
   Mostre o essencial na frente e um agrupamento "Avançado" recolhido.
6. O menu da sessão tem exatamente estas ações (pode agrupar, não pode inventar):
   Teclado · Barra de texto · Modo do toque (touchpad / toque direto) · Teclas especiais
   (Esc, Tab, Win, Alt+Tab, Ctrl+Alt+Del) · Girar a tela · Estatísticas · Picture-in-picture ·
   Controles na tela · **Desconectar** (o app continua rodando no PC) · **Encerrar no PC**
   (destrutivo: pede confirmação — desenhe-o como destrutivo).

## Vocabulário e conteúdo real

Leia, só leitura, para vocabulário e fatos:

- `C:\Users\Rafael\Documents\Projetos\Claude\Artemis\docs\design\DESIGN-SYSTEM.md` — seções 1, 2, 3, 4 e 8
  (produto, restrições, fundamentos, conteúdo real, o que o app não sabe). **Ignore as "quatro teses"**
  (seção 5): é o que estamos deixando para trás.
- `C:\Users\Rafael\Documents\Projetos\Claude\Artemis\docs\CATALOGO-PRODUTO.md` — o que o produto tem e o que falta.
- `C:\Users\Rafael\Documents\Projetos\Claude\Artemis\app\src\main\res\values-pt-rBR\strings.xml` — os termos que já existem.

Dados de amostra (use estes; não invente marcas):

- PC do Rafael: **ARTEMIS**, on-line, endereço `192.168.0.10`, emparelhado. Um segundo PC
  **NOTEBOOK**, fora do ar, para mostrar o estado.
- Apps do ARTEMIS (capas = blocos sólidos com o nome, sem imagens): **Área de trabalho** (em execução
  agora), **Steam**, e mais 4 a 6 jogos com nomes genéricos plausíveis (ex. "Corrida Noturna",
  "Fortaleza", "Vale Silencioso"). As capas do Moonlight são retrato **3:4**.
- Estados que precisam aparecer em algum artboard: host on-line / fora do ar / precisa emparelhar;
  app em execução; item selecionado; item desabilitado; foco de teclado (o tablet tem setas).

## Os 8 artboards (nomes de arquivo exatos)

Escreva cada um em `<sua pasta>\<Nome>.dc.html`. O elemento raiz tem tamanho fixo igual ao frame,
`position: relative; overflow: hidden` e fundo pintado.

| arquivo | frame | o que mostra |
|---|---|---|
| `Tablet_Inicio.dc.html` | 1280×800 | Computadores e apps do ARTEMIS na mesma tela, do jeito que a direção resolve (dois painéis, trilho, lista+grade…). Adicionar PC, ir aos Ajustes, e por app: abrir/retomar e encerrar no PC. |
| `Tablet_Ajustes.dc.html` | 1280×800 | Ajustes: Resolução (1080p / 1440p / nativa), Taxa (60 / 90 / 120 Hz), Bitrate (slider, Mbps), Codec (Auto / HEVC / AV1), HDR, Áudio, Modo do toque, Barra de texto, Controles na tela; e "Avançado" recolhido. |
| `Tablet_Sessao_Menu.dc.html` | 1280×800 | A sessão com o **menu aberto**. O vídeo é um retângulo escuro representando a Área de trabalho do PC (uma barra de tarefas e o contorno de uma janela, abstratos, sem marca). |
| `Tablet_Sessao_Texto.dc.html` | 1280×800 | A sessão com a **barra de texto aberta** (rascunho digitado, botão Enviar, linha de teclas especiais) e o gatilho do menu em repouso. Sem teclado na tela (é físico). |
| `Celular_Computadores.dc.html` | 412×883 | Lista de PCs (ARTEMIS on-line, NOTEBOOK fora do ar) e Adicionar PC. |
| `Celular_Apps.dc.html` | 412×883 | Apps do ARTEMIS, Área de trabalho em execução. |
| `Celular_Ajustes.dc.html` | 412×883 | Os mesmos Ajustes, na largura do celular. |
| `Celular_Sessao_Menu.dc.html` | 883×412 | Sessão em paisagem com o menu aberto e o gatilho visível. |

Sem barra de status falsa, sem teclado falso, sem moldura de aparelho: o Android real desenha isso.

## Formato dos arquivos (obrigatório)

Carregue a skill `design` (ferramenta Skill) e siga **só** as seções "Authoring the seed .dc.html"
e "Quick syntax card". Resumo do que importa aqui:

- esqueleto: `<!doctype html><html><head><meta charset="utf-8"><script src="./support.js"></script></head><body><x-dc><helmet><style>…</style></helmet> …conteúdo… </x-dc></body></html>` — a linha do `support.js` fica exatamente assim;
- artboards **estáticos**: sem `<script data-dc-script>`; texto literal no markup;
- estilos **inline** (`style="…"`) em tudo que alguém possa querer ajustar; no `<helmet><style>` só `body{margin:0}`, `a`/`a:hover` e o `<link>` das fontes;
- layout com `display:flex` / `display:grid` **com `gap`**, nunca irmãos inline separados por espaço; grid em `repeat(N, minmax(0, 1fr))`;
- HTML canônico: toda tag fechada, todo atributo entre aspas;
- ícones SVG inline; nada de emoji; nada de imagens externas.

**NÃO** rode o helper `seed-canvas.mjs`, **NÃO** publique artefato, **NÃO** modifique nada no repositório
do Artemis. Você só escreve os 8 arquivos e a `NOTA.md` na sua pasta.

## A `NOTA.md` (obrigatória, curta)

1. **Ideia** da direção em até 3 linhas, e o **tradeoff** principal em até 2.
2. **Tokens** para implementar sem adivinhar: cores em hex com nome e papel (fundo, superfície,
   superfície elevada, texto primário/secundário, acento, destrutivo, bordas), escala tipográfica
   (tamanho/peso/line-height por papel), raios, unidade de espaçamento, altura dos controles.
3. **Como o menu da sessão é convocado e onde o gatilho descansa.**
4. Dúvidas que você teve e decidiu sozinho.
