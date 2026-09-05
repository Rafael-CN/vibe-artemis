# F1 — Tokens da Console — implementação

Fatia: `docs/planos/B-CONSOLE.md` §F1 (lido no repositório principal, onde está commitado —
neste worktree o arquivo não existe porque ainda não foi versionado lá). Branch:
`fatia/F1-tokens`. Worktree: `C:\Users\Rafael\Documents\Projetos\Claude\Artemis-F1`.

## O que mudou e por quê

Esta fatia instala a linguagem visual da Console (Plano B) sem redesenhar nenhuma tela —
todos os layouts existentes continuam de pé, só os *tokens* por trás deles mudam. O efeito
esperado (e o critério de pronto da F1) é que as telas atuais, sem uma linha de layout
tocada, passem a mostrar cor e tipografia novas.

- **`app/src/main/res/values/colors.xml`** — os 13 tokens `console_*` de `NOTA.md` §2, com
  os hex exatos da nota. Os 25 `vidro_*` que já existiam (contei 25, não 24 como o resumo da
  tarefa dizia — reconferi linha a linha) deixaram de guardar hex próprio e viraram
  `@color/console_*`, cada um para o papel mais próximo (fundo→fundo, texto→texto, os três
  acentos→acento único, os pesos de borda→borda única, o halo→acento quando aceso e
  texto_2 quando não). Isso é o que faz as telas antigas mudarem de cor sem eu tocar em
  layout algum. Tabela completa em `docs/design/console/TOKENS.md`.
- **`app/src/main/res/values/dimens.xml`** (base = celular) e
  **`app/src/main/res/values-sw600dp/dimens.xml`** (override = tablet) — os dimens da F1:
  `console_espaco_1..6`, `console_raio_controle`, `console_raio_card`,
  `console_linha_ajuste` (56dp celular / 64dp tablet), `console_botao`, `console_capa_w/h`,
  `console_foco_borda`, `console_foco_anel`, `console_aba_w/h` (56×16 celular / 64×20
  tablet). `values-sw600dp/dimens.xml` já existia no repo, vazio — passou a ter conteúdo.
- **`app/src/main/res/values/styles.xml`** — `ConsoleText.Titulo/.Card/.Rotulo/.Rotulo2/
  .Legenda/.Mono`, `ConsoleBotao`/`.Primario`/`.Neutro`, `ConsoleSegmento`. `AppTheme` passou
  a apontar `colorPrimary`/`colorAccent`/`android:colorAccent`/`colorControlActivated` para
  `console_acento`, `android:windowBackground` para `console_fundo`, e ganhou `colorError` →
  `console_destrutivo` (não existia antes). Os estilos `VidroText.*`/`VidroButton.*`/
  `VidroChip`/`VidroDialog*` continuam exatamente como estavam — decisão registrada abaixo.
- **`app/src/main/res/values-sw600dp/styles.xml`** (novo) — override de tablet de
  `ConsoleText.Rotulo` (16sp; o celular, em `values/`, é 14sp).
- **`app/src/main/res/drawable/console_foco.xml`** e **`console_capa_foco.xml`** (novos) —
  o selector de foco pedido pela F1: contorno de 3dp em `console_acento` + anel de 4dp a 15%
  de opacidade do acento; `state_pressed` vira `console_superficie_selecionada` na variante
  genérica. Nenhum dos dois foi aplicado a um layout ainda — a F1 pede o recurso pronto, não
  a aplicação ("sem redesenhar telas ainda").
- **`app/src/main/res/drawable/console_botao_primario_bg.xml`** e
  **`console_botao_neutro_bg.xml`** (novos, não pedidos nominalmente pela F1, mas
  necessários) — fundo real para `ConsoleBotao.Primario`/`.Neutro` existirem; sem eles os
  dois estilos declarados pela F1 não teriam `android:background`. Nenhuma cor nova: só
  reaproveitam `console_acento`/`console_superficie`/`console_borda` e o dimen
  `vidro_stroke_width` já existente.
- **`docs/design/console/TOKENS.md`** (novo, pasta criada no worktree) — a tabela
  token → recurso → valor → onde usar, a tabela de alias `vidro_*` → `console_*`, e a seção
  "decisões desta fatia" com o raciocínio de cada escolha que não estava explícita na F1 nem
  na NOTA.

Nenhuma cor nova além das 13 da NOTA (a única exceção é um valor de opacidade *derivado*
inline nos dois drawables de foco — `#267FE0C7`, `console_acento` a 15% — não é token novo,
é a mesma cor do contorno esmaecida, calculada igual ao `console_destrutivo_fundo` que já
está na NOTA a 12%; ver comentário no próprio arquivo).

## Comandos executados, com saída resumida

Todos rodados dentro de `C:\Users\Rafael\Documents\Projetos\Claude\Artemis-F1`, PowerShell
(a ferramenta Bash deste ambiente está quebrada — todo comando bash falha na inicialização
do perfil com `exp: command not found`, antes mesmo de rodar; usei só PowerShell).

```
git worktree add "...\Artemis-F1" -b fatia/F1-tokens dev
git submodule update --init --recursive
```
Os dois rodaram sem erro; o submódulo `moonlight-common-c` (e o `enet` dentro dele) foram
clonados do zero, como esperado num worktree novo.

```
node tools/validate.mjs
```
Saída completa:
```
[....] codemap — os mapas em docs/maps/ precisam refletir o código atual
[ OK ] codemap
[....] jni — a fronteira JNI não tem checagem em tempo de compilação
[ OK ] jni
[....] build — compilar é o mínimo
[ OK ] build
[....] test — regressão de lógica Java
[ OK ] test — 4 falha(s) preexistente(s), nenhuma nova
validate: passou.
```
`assembleNonRoot_gameDebug` e `testNonRoot_gameDebugUnitTest` **de verdade rodaram e
passaram** (não é `--fast`; o worktree tinha `local.properties` copiado do repositório
principal, então o SDK foi encontrado). As 4 falhas de teste são as mesmas que
`tools/test-baseline.json` já registra como conhecidas — nenhuma nova, o próprio
`validate.mjs` confirma isso classificando por baseline. Não abri o baseline para conferir
quais são; o gate já faz essa checagem e ela é a fonte de verdade combinada pelo time.

```
node tools/screens.mjs
```
Saída resumida: 29 telas capturadas em `app/build/vidro-screens/`, todas com tamanho de
arquivo plausível (nenhuma em 0 KB, o que indicaria canvas vazio); nenhuma linha `!! FALHOU`
no relatório do runner — todas as `shootActivity`/`shoot` completaram.

## O que eu vi (fotos do `screens.mjs`) — "executei e vi", não "compilou"

Abri as imagens abaixo com a ferramenta de leitura de imagem, uma a uma, depois de gerá-las.
Isto é diferente de "o build passou": eu efetivamente olhei os PNGs listados aqui.

- `app/build/vidro-screens/01-home.png` — `PcView` viva: fundo `console_fundo` (quase
  preto), "Add" em `console_acento` (o verde-menta), botões "Scenes"/"Settings" em pílula
  escura com texto branco, Manrope visivelmente ExtraBold no título.
- `app/build/vidro-screens/02-catalogo-com-sessao.png` — `AppView` com o card de
  "Pick up where you left off": "Reload" e o ponto de estado em `console_acento`, botão
  "Return" em `console_acento` sólido com texto escuro (`console_acento_sobre`).
- `app/build/vidro-screens/04-sessao-ilha.png` — ilha da sessão: botão "Leave the screen"
  em `console_acento` sólido, ponto de estado ao lado de "controls" também no acento.
- `app/build/vidro-screens/05-folha-de-ajustes.png` — folha de ajustes em sessão: "Close"
  em `console_acento`, cabeçalhos em caixa alta ("BIT RATE", "FRAME PACING") no cinza
  `console_texto_2`, chips sem seleção em superfície escura com borda.
- `app/build/vidro-screens/07-pareamento.png` e `s22-07-pareamento.png` — "TYPE ON THE PC"
  em caixa alta, ponto "Waiting for the PC to confirm…" em `console_acento`.
- `app/build/vidro-screens/09-endereco.png` — `AddComputerManually`: botão "Connect" em
  `console_acento` sólido preenchendo a largura, "NEW PLACE" em caixa alta cinza, campo de
  IP com placeholder em fonte monoespaçada (JetBrains Mono, herdada do estilo antigo que
  também aponta pro alias).
- `app/build/vidro-screens/10-ajustes.png` — `StreamSettings`: título "Settings" em
  ExtraBold, fundo preto com o halo (ainda o `VidroLightView` antigo) agora tingido de verde
  em vez do azul original — confirma que `vidro_halo_awake` está resolvendo para
  `console_acento` de fato, não só no papel.
- `app/build/vidro-screens/tab-01-home.png` — a mesma `PcView`, só que no tamanho de tablet
  (1722×1075dp), mesma paleta.

Todas renderizadas por `shootActivity` (Activity de verdade, ver a lista em
`VidroScreenshotTest.telasEmRetrato`/`telasNoTablet`/`telasNoS22Ultra`) ou por `shoot`
(layout inflado, com `forceVisible` nas camadas de sobreposição) — anoto qual em cada
citação acima seria redundante linha a linha, mas nenhuma das evidências vem de uma tela
que eu não tenha efetivamente aberto.

## Achado por conferir, fora do escopo desta fatia

Em `02-catalogo-com-sessao.png` e no card "Continuar" de `01-home.png`/`tab-01-home.png`
sobra um azul (`#1E8FEE` a baixa opacidade) no fundo e na borda do card de antecipação —
vem de `app/src/main/res/drawable/vidro_card_accent.xml`, que usa hex literal
(`#141E8FEE`/`#3D1E8FEE`), não uma referência a `@color/vidro_accent_glow`. Como não é um
dos 25 tokens `vidro_*` nomeados em `colors.xml`, o alias desta fatia não alcança esse
arquivo — e o card em si é conteúdo de `PcView`/`AppView`, que é escopo da F3, não da F1.
Deixo registrado para quem revisar ou for fazer a F3: é a única cor fora da paleta Console
que sobrou visível nas capturas.

## O que ficou por verificar

- **Nenhuma foto de emulador real (AVD) nem de aparelho físico.** A instrução desta fatia
  foi explícita: "Não use emulador nesta fatia" — a ferramenta de fotos é da F0 e o emulador
  está ocupado por ela. Toda a verificação visual acima veio de `tools/screens.mjs`
  (Robolectric, JVM, sem vídeo/animação/gesto). Isso cobre composição, cor e tipografia, mas
  não cobre nada que dependa de hardware gráfico real, toque, ou da `SurfaceView` — o
  próprio `AGENTS.md` já avisa que `screens.mjs` não pega isso.
- **`console_foco.xml`/`console_capa_foco.xml` não têm nenhuma tela usando-os ainda.** Não
  há como fotografar o efeito de foco nesta fatia porque nenhum item os referencia — isso é
  esperado (aplicar foco por D-pad é F2/F3), mas significa que o selector em si só foi
  conferido por leitura do XML, não por captura.
- Não abri `tools/test-baseline.json` para conferir manualmente que as 4 falhas
  pré-existentes são as mesmas de sempre — confiei na classificação que `validate.mjs` já
  faz (compara por nome de teste contra o baseline).

## Dúvidas decididas sozinho

Todas com o raciocínio completo em `docs/design/console/TOKENS.md`, seção "Decisões desta
fatia" — aqui só o resumo:

1. **Mapeamento dos 25 `vidro_*` para os 13 `console_*`.** A F1 dá o princípio
   (fundo→fundo, texto→texto, acentos→acento, destrutivo→destrutivo) mas não uma tabela
   nominal — os nomes não coincidem 1:1. Mapeei por papel semântico, usando o comentário
   original de cada `vidro_*` em `colors.xml` como bússola. Onde um token já não tinha uso
   nenhum em `res/` (conferi por grep antes de decidir: `vidro_light_core`,
   `vidro_halo_night`, `vidro_halo_none`, `vidro_text_night`, `vidro_stroke` puro), usei o
   alias mais neutro possível, já que não há tela para comparar contra o mockup.
2. **`console_botao_primario_bg.xml`/`console_botao_neutro_bg.xml` criados sem pedido
   nominal.** Consequência direta de `ConsoleBotao.Primario`/`.Neutro` precisarem de
   `android:background` de verdade — a F1 só nomeia os drawables de foco explicitamente.
3. **Anel de foco a 15% de opacidade não virou token novo em `colors.xml`.** É hex literal
   inline (`#267FE0C7`) nos dois drawables de foco, calculado a partir de `console_acento` —
   mesma conta que `console_destrutivo_fundo` já faz a 12%. Entendi que "nenhuma cor nova
   além da NOTA" fala de tokens/paleta, não proíbe uma variante de opacidade derivada de um
   token existente para um único uso pontual.
4. **`console_capa_w`/`h` sem variação por aparelho**, embora a NOTA fale em medidas
   diferentes para tablet e celular. Seguindo a F1 literalmente (ela é a fonte de verdade
   desta fatia, a NOTA é a visão mais ampla) até a F3 decidir com a grade de capas na frente.
5. **`VidroText.*`/`VidroButton.*`/`VidroChip`/`VidroDialog*` não foram reparentados para
   `Console*`**, apesar de a F1 permitir ("se fizer sentido"). Reparentar mudaria
   `letterSpacing`/`lineSpacingMultiplier`/tamanho das telas antigas — que a cor já muda
   sozinha via os aliases, sem esse risco.
6. **Contagem de tokens `vidro_*`: são 25, não 24.** O resumo que recebi dizia 24; contei
   linha a linha em `colors.xml` antes de escrever os aliases e cheguei a 25 (a diferença
   provavelmente é só um erro de contagem de quem escreveu o resumo — não indica um token
   faltando ou duplicado). Todos os 25 têm alias; a tabela em `TOKENS.md` lista os 25.

## Commits

Branch `fatia/F1-tokens`, sem merge em `dev`, sem push, worktree preservado em
`C:\Users\Rafael\Documents\Projetos\Claude\Artemis-F1` como pedido.
