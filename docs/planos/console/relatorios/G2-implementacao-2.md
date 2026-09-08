# G2 — segunda rodada: os dois defeitos da devolução

Branch `fatia/G2-sessao2`, worktree `Artemis-G2`, em cima dos 7 commits anteriores da
fatia (`dev` = `e91d3999` na base). Emulador `s22u` na porta 5556, único usado (`tabs8u`
não foi tocado nesta rodada — ver "não verificado"). Submódulo já estava populado nesta
cópia, `git submodule update --init --recursive` não foi necessário. Emulador derrubado
(`emu kill`) ao final.

A fatia tinha sido **DEVOLVIDA** (`G2-verificacao.md`) por dois defeitos: a área de toque
da aba continuava de fato em 16dp (a tentativa com `TouchDelegate` não funciona nunca), e
o alternador da fileira de teclas especiais ficava majoritariamente embaixo do teclado
real. Os dois estão corrigidos e provados abaixo — nenhum dos dois é "não consegui
provar" desta vez.

## Defeito 1 (o que devolveu a fatia) — a aba continua difícil de acertar

**Causa raiz, confirmada pela verificação e conferida de novo aqui:** `backgroundTouchView`
e `streamContainer` (`activity_game.xml`) são `View`s irmãs da aba dentro do mesmo
`merge`, cobrindo a tela inteira. `Game#onTouch()` faz `backgroundTouchView` consumir todo
toque que caia nela. O algoritmo de despacho do Android testa os filhos do mais recente ao
mais antigo procurando quem **contenha o ponto tocado dentro do próprio retângulo real**;
`TouchDelegate` só é consultado quando **nenhum filho consome o evento**, e isso nunca
acontece aqui porque a superfície de vídeo consome tudo fora do retângulo desenhado da
aba (56×16dp no celular). Alargar a área "tocável" sem alargar o retângulo real da própria
`consoleAba` não tinha como funcionar — não é peculiaridade do moque, é como
`ViewGroup.dispatchTouchEvent` funciona.

**Correção:** removida a tentativa com `TouchDelegate` (`Game.java`,
`expandConsoleAbaToqueArea()` apagado, junto do import não usado de
`android.view.TouchDelegate`). Em vez disso, `console_aba.xml` ganhou uma segunda camada:

- O `FrameLayout` raiz (`consoleAba`) — o mesmo id que `Game.java` usa para
  `setOnClickListener`/`setOnTouchListener` — passou a ter o **tamanho real do alvo de
  toque**: `console_aba_touch_w/h` (120×56dp no celular, 128×60dp no tablet — os mesmos
  números que o `TouchDelegate` morto usava: 32dp de folga lateral, 20dp em cima/embaixo).
  Sem fundo (`background` removido daqui).
- Dentro dele, um `FrameLayout` filho novo (`consoleAbaVisual`) do tamanho de sempre
  (`console_aba_w/h`, 56×16dp), com o `console_aba_bg` e o chevron — encostado no topo do
  pai maior, então o desenho visível não muda **nem 1dp**.

Como `dispatchTouchEvent` testa o retângulo real de cada filho, e `consoleAba` agora É
esse retângulo maior, qualquer toque dentro dele nunca chega a ser testado contra
`streamContainer`/`backgroundTouchView` — o problema de raiz (delegate nunca consultado)
deixa de existir, não é contornado.

A alça de recolher dentro do `Dialog` da guia (`console_guia.xml`, id `consoleGuiaAlca`)
**não muda**: o `<include>` de lá já sobrescreve `layout_width`/`layout_height` de volta
para `console_aba_w/h` (56×16dp), e dentro do `Dialog` não há irmão nenhum disputando o
toque — o alvo maior só existe onde o defeito realmente vive, na janela da `Activity`.

**Prova, ao vivo, `s22u`/5556, `MoqueDeStream=true`, paisagem, um toque por vez:**
`uiautomator dump` confirmou a `consoleAba` real com `bounds="[1064,0][1379,147]"` — 315×147px,
que na densidade do aparelho (2,625px/dp, mesma da verificação anterior) é **120×56dp
exatos**, batendo com os tokens novos. Toquei (`adb shell input tap`) em 6 pontos
distintos dentro desse retângulo, fechando a guia (`ESCAPE`) entre um toque e outro para
repetir do estado fechado:

| ponto | coordenada | guia abriu? |
|---|---|---|
| centro | (1221,73) | sim |
| lateral esquerda (perto da borda) | (1068,73) | sim |
| lateral direita (perto da borda) | (1375,73) | sim |
| topo (perto de y=0) | (1221,4) | sim |
| base (perto de y=147) | (1221,144) | sim |
| canto inferior direito | (1375,144) | sim |

Todos os 6 abriram a guia num único toque. Controle negativo: um toque claramente fora do
alvo (1221,200 — 53px abaixo do limite) **não** abriu a guia, confirmando que a área nova
não vazou para dentro do vídeo além do previsto. Foto:
`docs/planos/console/fotos/G2-implementacao-2-defeito1-aba-s22u.png` (guia aberta pelo
toque no centro).

**Não verificado:** a "textura" do toque sobre vídeo real decodificando — só o moque foi
testado, como sempre. A lógica de despacho testada aqui (retângulo real vs. área
`TouchDelegate`) não depende de vídeo real ser ou não decodificado, então o risco
remanescente é baixo, mas é honesto registrar que não é prova em live video.

## Defeito 2 (novo, achado na verificação) — o alternador fica embaixo do teclado

**Causa raiz:** `Game.java#updateComposeBar()` posicionava a barra inteira
(`vidroComposeBar.setTranslationY(-overlap)`) usando `overlap` — o inset do IME **já
descontado** da barra de navegação (`imeBottom - navBottom`), que `applyImeOffset` usa
para não encolher o vídeo duas vezes. Mas `vidroComposeBar` vive num container que vai até
o fundo real da janela (`setDecorFitsSystemWindows(false)`), então erguê-la só pelo
`overlap` deixava a base dela exatamente `navBottom` pixels **abaixo** do topo real do
teclado. Reproduzi a mesma medição da verificação: com o Gboard aberto em retrato,
`dumpsys window displays` mostrou `type=ime frame=[0,1433][1080,2316]` — igual ao que o
verificador mediu — e, antes da correção, a lógica antiga teria posicionado a base da
barra em `1433 + navBottom` (63px abaixo do topo do teclado, a mesma fresta de ~9dp que
sobrava para o alternador).

**Correção:** `updateComposeBar` passou a receber o inset **bruto** do IME
(`insets.getInsets(Type.ime()).bottom`, variável `imeBottom` que já existia em
`setupImeInsetsHandling`) em vez do `overlap` descontado — só na chamada
(`updateComposeBar(imeBottom)` em vez de `updateComposeBar(overlap)`) e no parâmetro
renomeado (`imeBottomRaw`) para deixar claro por quê. **Nada mudou** em
`applyImeOffset`/`setStreamGravity` (o vídeo continua usando `overlap`, ou seja
`ImeDisplayMode.RESIZE` está intocado) nem em `openComposeBar`/`closeComposeBar`/
`sendTextBlock`.

**Segunda causa, encontrada nesta rodada (não estava no relatório da verificação):** o
próprio alvo do alternador (`consoleBarraTextoTeclasToggle`) media só ~25dp de altura
(texto + 2×4dp de padding) — abaixo do mínimo de 48dp pedido, **mesmo depois** de
corrigida a posição. Adicionado `android:minHeight="@dimen/vidro_touch_target"` (48dp
celular / 52dp tablet, o mesmo token usado em `activity_app_view.xml`/`activity_pc_view.xml`
para outros alvos de toque do app) ao `LinearLayout` do alternador, em
`console_barra_texto.xml`. `gravity="center_vertical"` já existia e centra texto+chevron
dentro da altura maior.

**Prova, ao vivo, `s22u`/5556, retrato, `MoqueDeStream=true`, Gboard REAL aberto (não
moque de teclado)** — reproduzindo exatamente o cenário da verificação (guia → "Barra de
texto" → toque no campo → `mInputShown=true`):

- `dumpsys window displays`: `type=ime frame=[0,1433][1080,2316] visible=true` — mesmo
  inset que o verificador mediu.
- `vidroComposeBar` (`uiautomator dump`): `bounds="[0,1073][1080,1433]"` — base em
  **y=1433, exatamente o topo do IME**. Antes da correção teria ficado em 1496 (63px
  dentro do teclado, a medição da verificação).
- `consoleBarraTextoTeclasToggle` (recolhido, antes de tocar): `bounds="[32,1286][404,
  1412]"` — altura 126px = **48,0dp exatos** (bate com `vidro_touch_target`), base em
  1412, **21px (8dp) acima** do topo do IME (1433) — inteiramente acima do teclado, ao
  contrário dos 42px que ficavam embaixo dele antes.
- Toquei no alternador (218,1349, dentro do retângulo acima) — expandiu em UM toque:
  `consoleBarraTextoTeclasRow` apareceu com `bounds="[32,1286][1048,1412]"` (base 1412,
  ainda acima do IME) e as 5 teclas dentro dela (`consoleKeyCtrlAltDel` em
  `[851,1286][1048,1412]`, cortada pela borda do viewport). Um `swipe` da direita para a
  esquerda revelou a tecla inteira: `[723,1286][1027,1412]`, 304px ≈ **112dp**, o mínimo
  declarado, ainda acima do teclado.
- Toquei de novo no alternador (agora em `[32,1149][404,1275]`, subiu porque a fileira
  acima dele cresceu) — recolheu em UM toque, `consoleBarraTextoTeclasRow` sumiu do dump.
- Foto: `docs/planos/console/fotos/G2-implementacao-2-defeito2-teclas-expandidas-s22u.png`
  — as 5 teclas e o rótulo "TECLAS ESPECIAIS" visivelmente acima do Gboard real, com
  respiro visível entre os dois.

Todos os toques (abrir a barra, tocar o campo, expandir o alternador, rolar até
Ctrl+Alt+Del, recolher) funcionaram de primeira — nenhum caiu no próprio Gboard
(figurinhas/GIF), ao contrário do que a verificação reproduziu 3 vezes antes da correção.

**Não verificado:** landscape com o alternador expandido por vários ciclos seguidos (só
testei o expandir/recolher uma vez cada em retrato, o cenário que a verificação
reproduziu); `tabs8u` não foi usado — lá o alternador nem existe
(`console_barra_teclas_recolhida_por_padrao=false`), então o defeito não se aplica, mas
não confirmei ao vivo que a barra em si (sem alternador) continua se comportando bem com
IME real no tablet.

## O que NÃO foi mudado

Conferido no diff final: nenhuma flag de janela da `Game`, nenhuma linha de decodificação
ou entrada, `ImeDisplayMode.RESIZE`/`applyImeOffset`/`setStreamGravity` intactos,
`openComposeBar`/`closeComposeBar`/`sendTextBlock` intactos, guia continua
`android.app.Dialog` (nada tocado em `GameMenu.java`), sem Kotlin/Compose, sem string nova
(`values/strings.xml`/`values-pt-rBR/strings.xml` não mudaram — só `dimens.xml` dos dois
lados), sem cor fora dos tokens `console_*` (o novo `consoleAbaVisual` reusa
`console_aba_bg`, nada de cor nova).

## Gate

- `node tools/codemap/codemap.mjs`: mapas regenerados depois das mudanças em `Game.java`
  (remoção de método/import).
- `node tools/validate.mjs` (`JAVA_HOME` = JDK 21), rodado **duas vezes** (a segunda depois
  do `minHeight` do alternador): **passou as duas** — codemap, jni, build, test (4 falhas
  pré-existentes, nenhuma nova, mesmo número que a verificação já reportava).
- `node tools/screens.mjs`: 30 telas renderizadas sem erro; `04-sessao-guia.png` e
  `05-sessao-barra-de-texto.png` conferidas visualmente, sem regressão de layout (a
  captura de layout força visibilidade, então não reflete o alternador recolhido — só a
  composição/medida).
- `node tools/fotos.mjs --avd s22u --port 5556 --rotas sessao-guia,sessao-texto`: 2/2
  capturadas (`sessao-guia` precisou de 1 repetição do próprio harness por sair em
  retrato na primeira tentativa — a mesma flakiness de `esperarRotacaoAplicada` sob
  `swiftshader_indirect` já documentada em `fotos.mjs`, não é regressão desta rodada).
  Bateria completa (22 rotas) **não** foi rodada de novo — as duas mudanças são
  cirúrgicas (uma view nova sem irmãos afetados, uma troca de parâmetro numérico) e a
  prova ponto-a-ponto por `adb`/`uiautomator` dá evidência mais direta dos dois critérios
  de pronto do que uma nova rodada de fotos daria.

## Não verificado, honestamente

- **Vídeo vivo real em qualquer um dos dois defeitos** — moque não é vídeo vivo; a
  "sensação" do toque e a aparência da barra sobre `SurfaceView` decodificando quadros
  reais só o aparelho do dono prova.
- `tabs8u` não foi usado nesta rodada (nenhum dos dois defeitos se aplica lá do mesmo
  jeito: a aba do tablet já tinha alvo maior relativo, 64×20dp, e o alternador nem existe
  — mas não confirmei ao vivo).
- Landscape com o alternador (só testado em retrato, o cenário exato da devolução).
- Ciclos repetidos de expandir/recolher (testado uma vez cada sentido).

## Commits desta rodada

1. Correção do defeito 1 (alvo real de toque da aba — `console_aba.xml` +
   `dimens.xml`/`values-sw600dp/dimens.xml` + remoção do `TouchDelegate` morto em
   `Game.java`).
2. Correção do defeito 2 (inset bruto do IME para posicionar a barra +
   `minHeight` no alternador).
3. Mapas de código regenerados.
4. Este relatório e as fotos.

Sem merge em `dev`, sem push.
