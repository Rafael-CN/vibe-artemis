# F5 — Conectando · emparelhar · queda · adicionar PC — implementação

Fatia: `docs/planos/B-CONSOLE.md` §F5. Branch: `fatia/F5-telas`, a partir de `dev`
(F0, F1, F2, F3 e F4 já integradas). Worktree:
`C:\Users\Rafael\Documents\Projetos\Claude\Artemis-F5`. Porta de emulador: 5554.

## O que mudou e por quê

Troca de pele pura nas quatro telas da spec — nenhum fluxo novo, rede, pareamento
ou OpenSSL tocados:

- `app/src/main/res/layout/vidro_connecting.xml`
- `app/src/main/res/layout/vidro_pairing.xml`
- `app/src/main/res/layout/vidro_lost.xml`
- `app/src/main/res/layout/activity_add_computer_manually.xml`

### Regras da spec aplicadas

- **Fundo**: `@color/console_fundo` direto (antes `@color/vidro_void`/`@drawable/vidro_scrim`
  — os dois já apontavam/resolviam para o mesmo tom pelos aliases da F1, então não é
  mudança visual, é parar de depender do alias nas quatro telas que esta fatia possui).
- **Título**: `ConsoleText.Titulo` em `vidroConnectingApp` (nome do app), `vidroLostTitle`
  ("Perdemos o PC-X") e `manuallyAddPcText` ("PC address") — sem overrides de tamanho,
  literal como a spec pede.
- **Código de pareamento em mono grande**: os quatro dígitos agora usam
  `ConsoleText.Mono` com `textSize="32sp"` (a base do estilo é 13sp — "grande" pedia
  override explícito). O campo de IP em `activity_add_computer_manually.xml` também
  virou `ConsoleText.Mono` (TOKENS.md lista "endereço IP" como o próprio exemplo de uso
  do Mono).
- **Botões `ConsoleBotao`**: toda ação nas quatro telas trocou de `VidroButton.Accent`/
  `VidroButton.Ghost` para `ConsoleBotao.Primario`/`ConsoleBotao.Neutro` — Cancelar
  (conectando, pareamento), Tentar de novo + Sair (queda), Conectar (adicionar PC).
- **Ponto de estado no lugar do `VidroLightView`**:
  - *Conectando*: a luz radial saiu; entrou um `View` circular de 10dp
    (`vidroConnectingLight`) que começa `vidro_dot_off` (apagado) e vira `vidro_dot_on`
    (acento) no mesmo instante em que `VidroSession#onStageStarting` avança a barra
    para "Reservando a tela" — mesmo evento real de antes, só a representação visual
    mudou de intensidade de luz para cor de ponto.
  - *Pareamento*: a luz saiu **sem substituto novo** — `vidroPairWaitDot` já é
    exatamente "um ponto de estado simples (acento = ativo)" (usa `vidro_dot_on` desde
    antes desta fatia). Um segundo ponto atrás do código seria o mesmo sinal repetido.
  - *Adicionar PC*: a luz era puramente decorativa ali (intensidade fixa em 0,20, sem
    Java nenhum lendo o id) — sem um estado real para o ponto representar, a remoção
    pura pareceu mais honesta que inventar um ponto sempre aceso.
  - *Queda*: nunca teve `VidroLightView` — nada a trocar aqui.

### Foco por D-pad

Todo botão novo é `focusable="true"` + `android:foreground="@drawable/console_foco_botao"`
(drawable novo desta fatia — mesma composição de `console_foco.xml`, raio de controle,
mas **sem** o estado `state_pressed` com fundo sólido, para não apagar a cor do
`ConsoleBotao.Primario` ao tocar; a mesma ideia que `console_capa_foco.xml` já usa para
a capa focada).

**Decisão de implementação, não da spec**: a primeira versão usava o padrão do cartão
"Avançado" da F4 — um `FrameLayout` `wrap_content` com um `View` irmão
`match_parent`+`console_foco_botao` por cima. Isso **quebrou de verdade**: numa fileira
com dois desses lado a lado (Tentar de novo + Sair), o segundo botão simplesmente não
media largura nenhuma e desaparecia da tela — confirmado pixel a pixel
(`app/build/vidro-screens/09-perdemos-o-host.png`, nenhum pixel do `ConsoleBotao.Neutro`
em lugar nenhum da imagem antes da correção). O cartão da F4 usa esse padrão com
`FrameLayout` **`match_parent`**, não `wrap_content` — a diferença importa. Troquei para
`android:foreground` direto no próprio `TextView` do botão (funciona em qualquer
`View` a partir da API 23; o app tem `minSdk 21`, mas os aparelhos e emuladores desta
fatia são todos ≥ Android 6 — um aparelho anterior a isso perderia só o anel visual, não
a navegação por D-pad, que continua funcionando via `focusable`). Documentado também
como comentário em `console_foco_botao.xml`.

### `MoqueEstado` no moque de stream (debug-only)

A spec pergunta se o moque de stream deveria ganhar `--es MoqueEstado conectando|queda`
para fotografar as duas telas de sessão — implementei exatamente isso, é o caminho menos
invasivo disponível:

- `Game.EXTRA_MOQUE_ESTADO` ("MoqueEstado"), lido só dentro do bloco já existente
  `if (emModoDeMoque())` (que já é `BuildConfig.DEBUG` + extra booleano — zero efeito em
  release, nada novo aqui).
- `MoqueEstado=conectando`: o moque começa a decodificar a superfície falsa mas **não**
  chama `connectionStarted()` — a tela de conectar (`showVidroConnecting()`, já visível
  desde `onCreate`) fica de pé para sempre, em vez de sumir no instante em que a
  superfície aparece.
- `MoqueEstado=queda`: o moque chama `connectionStarted()` normalmente (mesmo caminho de
  sempre) e, na sequência, mostra `vidroLost` por cima. **Não é a queda de rede real**
  (fora do escopo — "não mexa em rede"): é o par exato do que uma queda de verdade
  mostraria depois que o vídeo já estava de pé, fotografado sob demanda.
- Sem o extra (ou com qualquer outro valor), o moque se comporta exatamente como antes
  desta fatia — nenhuma rota existente de `sessao*` mudou de comportamento.

`tools/fotos.mjs` ganhou as rotas `sessao-conectando` e `sessao-queda` (mesmo padrão das
rotas `sessao*` já existentes, paisagem, `MoqueDeStream=true`).

### Pareamento (`vidro_pairing.xml`) não ganhou rota de `fotos.mjs`

A spec só pede o extra `MoqueEstado` para "as duas telas da sessão" (conectando e
queda — as duas vivem em `Game`/`activity_game.xml`). `vidro_pairing.xml` está incluído
em `activity_pc_view.xml` e só aparece via `PcView#showVidroPairing`, que depende de um
pareamento de rede real em andamento (`ComputerManagerService`) — não existia um moque
equivalente para isso antes desta fatia, e criar um seria inventar mecanismo de rede
fora do escopo ("não mexa em... emparelhamento"). A tela é fotografada e verificada por
`node tools/screens.mjs` (`07-pareamento`, `s22-07-pareamento` — inflação direta do
layout via Robolectric, sem precisar de Activity nem de host), que já cobria as quatro
telas desta fatia antes de eu tocar em qualquer coisa
(`app/src/test/java/com/limelight/VidroScreenshotTest.java`, linhas 84/93/107/115 —
arquivo não alterado nesta fatia).

### Drawables locais retemados (exclusivos das 4 telas — não afetam outras fatias)

Confirmei com `grep -rl` que `vidro_code_box.xml`, `vidro_field.xml` e
`vidro_progress_track.xml` só são referenciados dentro das quatro telas desta fatia —
seguros para reteamar direto (superfície/borda em vez de vidro translúcido, preenchimento
em `console_acento`) sem risco de regressão em F3/F4. `vidro_button_accent.xml` e
`vidro_button_ghost_bg.xml` (e o `_pressed`/`_ghost` que ele referencia) ficaram sem
nenhuma referência depois da troca para `ConsoleBotao.*` — **decisão deliberada de não
apagar**: `docs/planos/B-CONSOLE.md` §F6 é explicitamente "limpeza dos aliases `vidro_*`",
e apagar arquivos de estilo mortos é trabalho dessa fatia, não desta.

### Recurso órfão verificado antes de tocar

`vidroLostRetry`/`vidroLostLeave`/`vidroLostRetryCount` não tinham nenhuma referência em
`app/src/main/java/` antes desta fatia (`grep -rn "vidroLost" app/src/main/java/` vazio).
Rodei `git log --all --oneline -S"R.id.vidroLost"` antes de mexer: o único commit que
manipulava esses ids (`62c6e89b`, "Archive the Vidro redesign as it stood on 4
September") **arquivou** essa mesma tela num redesenho anterior que o dono do fork
julgou uma regressão — não é um "esquecimento", é uma decisão explícita de recomeçar.
Não reativei a lógica de queda de rede real (fora do escopo desta fatia); só apliquei a
pele nos elementos que já existiam no XML, e wireei o suficiente para fotografar via
`MoqueEstado=queda` (debug-only), sem tocar em `nvstream/`, `crypto/` nem no protocolo.

## Comandos executados e resultado

```
git submodule update --init --recursive
→ necessário: moonlight-common-c/enet vieram vazios neste worktree, e o build
  falhava com "No rule to make target .../AudioStream.c" antes disso. Não é uma
  mudança desta fatia — é setup de ambiente que faltava.

JAVA_HOME="C:\Program Files\Java\jdk-21.0.12"
ANDROID_HOME="C:\Users\Rafael\AppData\Local\Android\Sdk"

node tools/codemap/codemap.mjs
→ 224 arquivos | 67.264 linhas | 280 tipos | 2648 métodos | 30 pacotes | 36 TODOs
  (o salto de 190→224 arquivos é o submódulo antes ausente entrando na contagem,
  não código desta fatia)

node tools/validate.mjs
→ [ OK ] codemap
→ [ OK ] jni
→ [ OK ] build
→ [ OK ] test — 4 falha(s) preexistente(s) (tools/test-baseline.json), nenhuma nova
→ validate: passou.
```

```
node tools/screens.mjs
→ 31 telas em app/build/vidro-screens/, sem erro — as quatro desta fatia:
  03-conectando.png, 07-pareamento.png (+ s22-07-pareamento.png), 09-endereco.png
  (+ 00-endereco-viva.png), 09-perdemos-o-host.png. Todas via `shoot()` (inflação
  direta do layout, forceVisible=true onde a camada nasce GONE) — não via
  shootActivity, então texto que só existe em runtime (nome do app, dígitos do
  código, host perdido) aparece em branco nessas capturas; a estrutura, cor e
  tipografia são o que se está verificando aqui.
```

```
./gradlew :app:assembleNonRoot_gameDebug → BUILD SUCCESSFUL (já up-to-date do validate.mjs)

node tools/fotos.mjs --avd s22u --port 5554 --rotas adicionar,sessao-conectando,sessao-queda
→ 3/3 rota(s) capturada(s) em app/build/fotos/s22u

node tools/fotos.mjs --avd tabs8u --port 5554 --rotas adicionar,sessao-conectando,sessao-queda
→ 3/3 rota(s) capturada(s) em app/build/fotos/tabs8u
```

Um emulador de cada vez: `adb devices` vazio antes de subir `s22u`; matei
`emulator-5554` (`adb -s emulator-5554 emu kill`) e confirmei `adb devices` vazio antes
de subir `tabs8u`; matei de novo ao final e confirmei `adb devices` vazio. Nenhum
emulador ficou de pé.

## Fotos

Todas em `app/build/fotos/<avd>/` (gerado — não versionado; comandos acima
reproduzem) e `app/build/vidro-screens/` (idem):

- `app/build/fotos/s22u/adicionar.png` — "PC address" no S22 Ultra, teclado do sistema
  aberto (o hostText pede foco via `<requestFocus />`, como antes desta fatia).
- `app/build/fotos/s22u/sessao-conectando.png` — `Game` com `MoqueDeStream` +
  `MoqueEstado=conectando`: a tela de conectar (fundo `console_fundo`, ponto apagado,
  barra em `console_acento`, botão "Cancel" `ConsoleBotao.Neutro`) fica de pé sobre o
  controle virtual, sem sumir.
- `app/build/fotos/s22u/sessao-queda.png` — idem com `MoqueEstado=queda`: `vidro_lost`
  visível por cima, "Try again" (`ConsoleBotao.Primario`, acento) e "Leave"
  (`ConsoleBotao.Neutro`) lado a lado.
- `app/build/fotos/tabs8u/{adicionar,sessao-conectando,sessao-queda}.png` — mesmas três
  rotas no Tab S8 Ultra (paisagem, mais larga; sem coluna centrada — esta fatia não
  criou `layout-sw600dp` para `activity_add_computer_manually.xml`, e a spec não pediu
  um tratamento de tablet específico para as quatro telas, diferente da F4).
- `app/build/fotos/{s22u,tabs8u}/folha.png` — folhas de contato por aparelho.
- `app/build/vidro-screens/03-conectando.png`, `07-pareamento.png`,
  `s22-07-pareamento.png`, `09-endereco.png`, `00-endereco-viva.png`,
  `09-perdemos-o-host.png` — as quatro telas via Robolectric, incluindo o pareamento
  (sem rota de `fotos.mjs`, ver seção acima).

Todas conferidas visualmente contra a linguagem das telas já feitas (Início, Apps,
sessão, Ajustes): fundo `console_fundo`, Manrope ExtraBold no título, JetBrains Mono no
código/endereço, `console_acento` só em foco/ativo/botão primário, nada de azul.

## Decisões tomadas onde a spec era ambígua

1. **Ponto de estado por tela** — a spec dá uma regra geral ("luz substituída por um
   ponto de estado simples"); apliquei caso a caso (novo ponto dinâmico em conectando,
   reaproveitar o ponto já existente em pareamento, remoção pura em adicionar-PC) em vez
   de forçar o mesmo componente nas quatro, porque só duas telas tinham um estado real
   para o ponto representar.
2. **Título literal `ConsoleText.Titulo`, sem overrides de tamanho** — descartei a
   tentação de manter os 24sp que as telas tinham antes (nome do app, "Perdemos o
   PC-X") porque a regra da spec é explícita e as fotos mostraram que o tamanho maior
   não quebra layout em nenhum dos dois AVDs.
3. **Foco por `android:foreground` direto, não wrapper `FrameLayout`** — ver seção
   "Foco por D-pad" acima; troquei de abordagem no meio do trabalho depois de achar o
   bug real (botão desaparecendo), não é uma preferência estética.
4. **Sem rota de `fotos.mjs` para pareamento** — ver seção dedicada acima; coberto por
   `tools/screens.mjs` em vez de inventar um segundo mecanismo de moque para uma tela
   que a spec não pediu explicitamente.
5. **`vidro_button_accent.xml`/`vidro_button_ghost_bg.xml` (e drawables que eles
   referenciam) viraram órfãos e não foram apagados** — trabalho de F6 por definição do
   plano (`B-CONSOLE.md` §F6, "limpeza dos aliases `vidro_*`").

## O que NÃO foi verificado, e por quê

- **Aparelho físico.** Nenhuma foto veio de um Galaxy S22 Ultra ou Tab S8 Ultra reais —
  fora do escopo desta fatia (`docs/planos/B-CONSOLE.md` §3, "depois das fatias").
- **`vidro_pairing.xml` num pareamento de rede real.** Só verificado via inflação de
  layout (Robolectric) — nunca visto atrás de `PcView#showVidroPairing` com um host de
  verdade respondendo. A mecânica de pareamento em si não foi tocada nesta fatia.
- **`vidro_lost.xml` numa queda de rede real.** O único caminho que a mostra hoje é o
  `MoqueEstado=queda` desta fatia — a reativação da detecção de queda de verdade
  (`onConnectionTerminated`/equivalente) está fora do escopo ("não mexa em rede") e não
  foi investigada a fundo além de confirmar, pelo git log, que foi removida de propósito
  em 5 de setembro.
- **Texto em runtime nas capturas de `screens.mjs`.** Nome do app, dígitos do código e
  nome do host perdido aparecem em branco nessas imagens porque `shoot()` infla o layout
  puro (não roda `onCreate`); confirmado que isso já era o comportamento antes desta
  fatia (mesmas telas, mesmo método) — não é regressão, é limitação documentada da
  ferramenta (`AGENTS.md`, seção "Ver o desenho sem gerar APK").
- **Anel de foco visual em `android:foreground` num aparelho com API < 23.** Nenhum
  aparelho ou emulador desta verificação tem API < 23; a navegação por D-pad
  (`focusable="true"`) continua funcionando nesse cenário hipotético, só o contorno
  visual de foco que não apareceria — documentado como comentário em
  `console_foco_botao.xml`.
- **Foco por D-pad percorrido manualmente com `adb shell input keyevent` nas quatro
  telas.** Confirmei `focusable="true"` + `console_foco_botao` no XML de cada botão e o
  próprio bug do wrapper (que teria sido invisível sem inspecionar pixel a pixel), mas
  não fiz uma passada de D-pad físico via `adb` em cada tela como o verificador da F4
  fez para Ajustes — ficou para a verificação desta fatia.

## Arquivos alterados

- **Novo:** `app/src/main/res/drawable/console_foco_botao.xml`,
  `docs/planos/console/relatorios/F5-implementacao.md`.
- **Editado:** `app/src/main/java/com/limelight/Game.java`,
  `app/src/main/java/com/limelight/PcView.java`,
  `app/src/main/java/com/limelight/ui/VidroSession.java`,
  `app/src/main/res/drawable/vidro_code_box.xml`,
  `app/src/main/res/drawable/vidro_field.xml`,
  `app/src/main/res/drawable/vidro_progress_track.xml`,
  `app/src/main/res/layout/activity_add_computer_manually.xml`,
  `app/src/main/res/layout/vidro_connecting.xml`,
  `app/src/main/res/layout/vidro_lost.xml`,
  `app/src/main/res/layout/vidro_pairing.xml`, `tools/fotos.mjs`.
- **Gerado:** `docs/maps/*` (via `node tools/codemap/codemap.mjs`, depois do
  `git submodule update`).

Nenhuma string nova foi necessária — as quatro telas reusam `strings.xml`/
`values-pt-rBR/strings.xml` já existentes.

Sem merge em `dev`, sem push — como instruído.
