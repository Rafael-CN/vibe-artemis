# G2 — segunda verificação (última rodada possível antes de subir ao dono)

Verificador independente, não implementou nada desta fatia nem da rodada anterior.
Worktree `Artemis-G2`, branch `fatia/G2-sessao2` (11 commits à frente de `dev`, árvore
limpa antes de começar). `git submodule update --init --recursive` não foi necessário
(submódulo já populado). Emuladores usados um de cada vez, `s22u` primeiro (porta 5556,
`assembleNonRoot_gameDebug` instalado como `com.limelight.noirdebug`), depois `tabs8u` na
mesma porta — `s22ub` e a porta 5554 nunca tocados. Ambos derrubados (`emu kill`) ao
final de cada uso.

**Veredito: APROVADA.**

Os dois defeitos que devolveram a fatia estão corrigidos e eu os reproduzi de forma
independente, com medições próprias (não apenas relidas do relatório da implementação):
o alvo real de toque da aba agora é 120×56dp no celular e 128×60dp no tablet — testei 6
pontos dentro do retângulo em cada aparelho, mais um controle negativo fora dele, com
resultado idêntico em ambos; e o alternador da fileira de teclas especiais agora fica
inteiramente acima do IME real (Gboard), testado em dois ciclos completos de
expandir/recolher em retrato **e** em paisagem, sem nenhum toque vazando para o teclado
do sistema. Nenhum defeito novo bloqueante. Nenhuma correção foi necessária nesta rodada
— não há commit novo de código, só este relatório e duas fotos.

## 1. Área de toque da aba — CONFIRMADO nos dois aparelhos

**Celular (`s22u`/5556, paisagem, `MoqueDeStream=true`):** `uiautomator dump` mediu
`consoleAba` (o alvo real) em `bounds="[1064,0][1379,147]"` → 315×147px; densidade
2,625px/dp (315/2,625=120, 147/2,625=56) → **120×56dp exatos**, batendo com
`console_aba_touch_w/h`. `consoleAbaVisual` (o desenho) em `[1148,0][1295,42]` →
147×42px → **56×16dp exatos**, idêntico ao tamanho de antes desta fatia — o desenho não
engordou.

Toquei (`adb shell input tap`) em 6 pontos, fechando com `BACK` e conferindo o
fechamento antes do próximo toque:

| ponto | coordenada | abriu num toque? | fechou com BACK? |
|---|---|---|---|
| centro | (1221,73) | sim | sim |
| lateral esquerda | (1068,73) | sim | sim |
| lateral direita | (1375,73) | sim | sim |
| topo | (1221,4) | sim | sim |
| base | (1221,144) | sim | sim |
| canto inferior direito | (1375,144) | sim | sim |

Controle negativo — (1221,200), 53px abaixo do limite do retângulo (147) — **não** abriu
a guia, confirmando que a área nova não vazou para o vídeo além do previsto.

**Tablet (`tabs8u`/5556, paisagem, mesmo APK, mesmo `MoqueDeStream`):** `consoleAba` em
`[1384,128][1576,218]` → 192×90px; densidade 1,5px/dp (192/1,5=128, 90/1,5=60) →
**128×60dp exatos**. `consoleAbaVisual` em `[1432,128][1528,158]` → 96×30px → **64×20dp
exatos**, o tamanho de sempre no tablet — sem regressão. Repeti os mesmos 6 pontos +
controle negativo, com coordenadas recalculadas para o retângulo do tablet: todos os 6
abriram num toque, o controle negativo (1480,260) não abriu, estado final fechado.

**Foto:** comparei visualmente o desenho fechado (capturado nesta sessão, celular,
paisagem — pill discreta com chevron, sem crescer) contra
`docs/design/console/fotos/B_Celular_Sessao_Menu.png` — mesmo estilo de pill escura e
discreta, chevron centralizado, nada de engordar visualmente.

**Não verificável aqui:** a textura do toque sobre `SurfaceView` decodificando vídeo de
verdade — moque não é vídeo vivo. A lógica testada (retângulo real de `dispatchTouchEvent`
vs. a antiga tentativa de `TouchDelegate`) não depende de decodificação real, então o
risco remanescente é baixo, mas só o aparelho do dono, com a stream ao vivo, prova a
sensação final do toque.

## 2. Alternador das teclas especiais sob o teclado — CONFIRMADO, retrato e paisagem

**Retrato (`s22u`, Gboard real, não moque de teclado):** abri a guia → "Barra de texto" →
toquei no campo (`mInputShown=true` confirmado por `dumpsys input_method`). `dumpsys
window displays`: `type=ime frame=[0,1433][1080,2316]`. `vidroComposeBar`:
`[0,1073][1080,1433]` — base exatamente em y=1433, no topo do IME (antes da correção
ficaria em 1496, 63px dentro do teclado). `consoleBarraTextoTeclasToggle` (recolhido):
`[32,1286][404,1412]` — altura 126px = **48,0dp exatos** (bate com `vidro_touch_target`),
base em 1412, **21px (8dp) acima** do IME.

Dois ciclos completos de expandir/recolher, tocando sempre no centro do alternador (a
posição dele muda entre os dois estados porque a fileira acima cresce):

| ciclo | ação | resultado |
|---|---|---|
| 1 | toque (218,1349) expande | `consoleBarraTextoTeclasRow` aparece, 1 toque |
| 1 | scroll até Ctrl+Alt+Del completo | `[723,1286][1027,1412]`, 304px, tecla inteira visível |
| 1 | toque (218,1212) recolhe | fileira some, 1 toque |
| 2 | toque (218,1349) expande | aparece de novo, 1 toque |
| 2 | toque (218,1212) recolhe | some de novo, 1 toque |

Em nenhum dos 4 toques (2 ciclos) o toque caiu no Gboard — `mInputShown` seguiu `true`
o tempo todo, sem abrir figurinhas/GIF como acontecia na verificação anterior. Foto:
`docs/planos/console/fotos/G2-verificacao-2-defeito2-teclas-retrato-s22u.png` — a fileira
de 5 teclas e o rótulo "TECLAS ESPECIAIS" visivelmente acima do teclado real, com respiro
entre os dois.

**Paisagem (não testado pelo implementador, item cobrado nesta rodada):** mesmo roteiro.
`ime frame=[128,394][2316,1080]`. `vidroComposeBar`: `[128,34][2316,394]` — base em 394,
exatamente o topo do IME de novo. `consoleBarraTextoTeclasToggle`: `[160,247][532,373]`
— 126px = 48,0dp, base em 373, 21px acima do IME. Dois ciclos completos (expandir,
recolher, expandir, recolher), todos em 1 toque, `mInputShown` permaneceu `true` o tempo
todo — mesmo comportamento do retrato, sem regressão por orientação.

**Vídeo não mudou de tamanho por causa do fix:** conferido por diff (abaixo) que
`applyImeOffset`/`setStreamGravity` continuam recebendo `overlap` (o inset já descontado
da nav bar), não o `imeBottom` bruto que passou a ir só para `updateComposeBar`. Medição:
`streamContainer` manteve letterbox estável (altura 1427px em retrato, independente do
estado da barra) — nada indica redimensionamento novo do vídeo.

**Pequena imprecisão no relatório da implementação, não bloqueante:** ele descreve a
tecla Ctrl+Alt+Del rolada como "304px ≈ 112dp"; com a densidade medida (2,625px/dp) isso
é **115,8dp**, não 112 — só um arredondamento na prosa, o número em pixels (304, acima do
mínimo de 112dp declarado) e o comportamento estão corretos.

## 3. Tablet — sem regressão (item cobrado nesta rodada, implementador não usou `tabs8u`)

Além do hitRect da aba (seção 1, confirmado 128×60dp exatos), testei a barra de texto
com Gboard real no tablet: `ime frame=[0,1221][2960,1848]`. `vidroComposeBar`:
`[0,993][2960,1221]` — base exatamente no topo do IME, mesmo comportamento do celular
(a função é compartilhada, não há código específico de tablet em `updateComposeBar`).
`consoleBarraTextoTeclasRow` (sempre visível no tablet, sem alternador — confirmado
`consoleBarraTextoTeclasToggle` ausente do dump, 0 ocorrências): `[24,1131][2936,1203]`
— base 1203, 18px (6dp) acima do IME. Foto:
`docs/planos/console/fotos/G2-verificacao-2-tablet-barra-ime-tabs8u.png` — as 5 teclas
(Esc·Tab·Win·Alt+Tab·Ctrl+Alt+Del) inteiras, sem rolagem necessária no tablet (tela mais
larga), visivelmente acima do teclado real, sem sobreposição.

`values-sw600dp/dimens.xml` confirmado sem mudança nos tokens de bloco (96/88/104dp) —
defeito 3 da rodada anterior seguiu intacto no tablet.

## 4. Fronteiras invioláveis — respeitadas

Revisei `git diff f481577c..HEAD` (o diff completo desta segunda rodada) e o diff
acumulado da fatia:

- `GameMenu.java`: **não tocado nesta rodada** (stat confirma). `guiaDialog = new
  Dialog(themedContext, R.style.ConsoleDialog)` continua a única forma de abrir a guia —
  `android.app.Dialog`, não overlay na mesma janela.
- Nenhuma flag de janela da `Game`, nenhuma linha de decodificação ou dispatch de entrada
  da Activity tocada — só `setOnClickListener`/`setOnTouchListener` em views já
  existentes, e a troca do parâmetro de `updateComposeBar`.
- `applyImeOffset(overlap)` e a chamada em `setupImeInsetsHandling` continuam recebendo
  `overlap` (inset já descontado da nav bar) — só `updateComposeBar` passou a receber
  `imeBottom` bruto. `setStreamGravity` não aparece no diff.
- `openComposeBar`/`closeComposeBar`/`sendTextBlock`: zero ocorrências no diff desta
  rodada.
- `ImeDisplayMode.RESIZE`: não tocado.
- Sem Kotlin/Compose em nenhum arquivo do diff.
- Cores: `consoleAbaVisual` reusa `console_aba_bg` (nenhuma cor nova fora de `console_*`).
- Strings: só `values/dimens.xml` e `values-sw600dp/dimens.xml` mudaram — nenhuma
  ocorrência de `strings.xml` ou `values-pt-rBR/` no diff.
- `console_guia.xml` (a alça dentro do Dialog) sobrescreve `layout_width/height` do
  `<include>` de volta para `console_aba_w/h` (56×16dp) — o alvo maior só existe fora do
  Dialog, onde o defeito realmente vivia; conferido lendo o XML.

## 5. Retrato (defeito 5 da rodada anterior) — sem mudança, por decisão

Conferido só por diff, como pedido: nenhuma linha em torno de `StreamContainer`,
`onMeasure`, `videoScaleMode` ou orientação (`SCREEN_ORIENTATION_*`) aparece no diff desta
rodada nem da rodada anterior a esta verificação. Ninguém mexeu.

## Gate

- `node tools/validate.mjs` (`JAVA_HOME=C:\Program Files\Java\jdk-21.0.12`): **passou** —
  codemap, jni, build, test (4 falhas pré-existentes, nenhuma nova, mesma contagem das
  duas rodadas anteriores).
- `node tools/screens.mjs`: 30 telas renderizadas sem erro, incluindo
  `tab-04-sessao-guia.png` e `tab-05-sessao-barra-de-texto.png` (tablet, via Robolectric).
- `./gradlew.bat assembleNonRoot_gameDebug`: `BUILD SUCCESSFUL`, instalado via `adb install
  -r` nos dois emuladores.
- Não rodei `tools/fotos.mjs` (bateria completa) em nenhum dos dois aparelhos — as
  mudanças desta rodada já tinham prova ponto-a-ponto via `adb`/`uiautomator` mais
  granular do que uma nova rodada de fotos daria, e o tempo de emulador era o recurso mais
  escasso (dois aparelhos, dois orientações, dois testes de IME real).

## O que não é verificável daqui

- **Vídeo vivo real em qualquer um dos dois defeitos** — moque não é vídeo vivo; a
  sensação do toque e a aparência da barra sobre `SurfaceView` decodificando quadros
  reais só o aparelho do dono, com o host Vibeshine, prova.
- Desconectar/Encerrar não foram acionados ao vivo (mesmo cuidado da rodada anterior,
  para não encerrar a sessão moque no meio da verificação) — confirmado por leitura de
  código que nenhum dos dois foi tocado nesta rodada (`onBlock()` genérico, inalterado).
- Defeito 5 (retrato/letterbox) com decodificação real e preferências não-padrão
  (`checkbox_show_onscreen_controls`, `videoScaleMode=FILL`) — já era assim na rodada
  anterior, segue de pé como recomendação para o aparelho do dono.

## Conclusão

Os dois defeitos que devolveram a fatia estão corrigidos, com prova própria e
independente (não apenas relida do relatório da implementação) em ambos os aparelhos e
em ambas as orientações relevantes. Nenhum defeito novo bloqueante apareceu na cobertura
adicional pedida (tablet, paisagem, ciclos repetidos). As fronteiras invioláveis seguem
intactas. **Aprovada — sem merge em `dev`, sem push**, conforme instrução.
