# G1 — Toque primeiro, e proporção no celular — implementação

Branch `fatia/G1-toque`, worktree `Artemis-G1`, base `dev` em `e91d3999`. Emulador
`s22ub` (porta 5554) para o grosso do trabalho; `tabs8u` (porta 5554, `s22ub` derrubado
antes) só para conferir que o tablet não regrediu. Um AVD de cada vez, cada um derrubado
(`emu kill`) antes de subir o próximo — confirmado por `adb devices` entre as trocas.
Submódulo `moonlight-common-c` inicializado (`git submodule update --init --recursive`,
estava vazio neste worktree).

## 1. Toque duplo — causa raiz

Reproduzido primeiro, antes de qualquer edição: instalei o APK do estado atual da `dev`
no `s22ub` e toquei "90 Hz" em Ajustes básico uma única vez —
`adb shell input tap 745 794`. Resultado: só um anel de foco aparecia ao redor do botão;
"60 Hz" continuava selecionado. Um segundo toque na mesma posição é que trocava a
seleção para "90 Hz". Fotos: `app/build/vidro-screens`/capturas manuais desta sessão
confirmaram o padrão antes da correção (uma delas, `before_seg.png`/`after_seg1.png`/
`after_seg2.png`, ficou só no scratchpad da sessão, não no repo).

**Causa**: `View.onTouchEvent()` — em `ACTION_UP`, se a view é `focusable` **e**
`focusableInTouchMode` **e ainda não estava focada**, o toque chama `requestFocus()`
primeiro; quando essa chamada pega o foco (`focusTaken = true`), o `performClick()`
daquele mesmo toque é **descartado** — só roda no toque seguinte, com a view já focada.
É comportamento documentado do framework (não um bug do Android), e o gatilho é
exatamente a combinação `clickable + focusable + focusableInTouchMode` numa `View`
comum despachada por `OnClickListener`.

**Não é universal**: uma `GridView`/`AdapterView` despacha o clique de item pelo próprio
`AbsListView.onTouchEvent()` (`performItemClick`), que nunca passa pelo
`requestFocus()` de `View.onTouchEvent()`. Testado: toquei o card "RAFAEL" em
Computadores (a `GridView` `pc_grid_view.xml` tem `focusableInTouchMode` no
**container**) e o clique (abriu o pareamento) ativou no primeiro toque. Ou seja, o
`focusableInTouchMode` do container do `GridView` não é a causa do toque duplo — é o
mesmo atributo num item **comum** (`LinearLayout`/`TextView`/`FrameLayout` clicável,
fora de um `AdapterView`) que quebra.

### O que mudou

Removido `android:focusableInTouchMode="true"` (mantido `focusable="true"`, suficiente
para D-pad — o modo toque é abandonado no primeiro evento de tecla, e a busca de foco
deixa de depender do atributo) de:

- `console_ajustes_basico_conteudo.xml` — os 11 segmentos (Resolução, Taxa, Codec, Modo
  do toque).
- `console_capa_carousel_item.xml` — o item do carrossel "APPS" da Início no tablet
  (`AppCarouselAdapter`/`RecyclerView`, mesmo padrão de bug que os segmentos: item
  comum com `OnClickListener`, não item de `GridView`).

Mantido, com comentário explicando o motivo (o container de D-pad, não o item, e por
que não é a causa do bug): `app_grid_view.xml`, `layout-sw600dp/app_grid_view.xml`,
`pc_grid_view.xml`. `focusableInTouchMode` no container do `GridView` deixa a lista
pronta para receber foco por D-pad assim que a tela abre, sem exigir um toque antes
para sair do modo toque — e não afeta o clique do item, pelo motivo acima.

**Não toquei** `console_guia.xml` (10 ocorrências), `console_teclas_fileira.xml` (5) nem
`activity_game.xml` (1) — são território da G2. Ver §5.

### Prova (toque único, cinco controles, duas telas)

Todos via `adb shell input tap` num único toque, com captura de tela antes/depois
mostrando a mudança de estado (preenchimento verde do segmento, não só o anel de foco):

1. Ajustes básico → "60 Hz" (estava "90 Hz"): ativou, virou verde, no 1º toque.
2. Ajustes básico → "Auto" (Codec, estava "HEVC"): ativou no 1º toque.
3. Ajustes básico → "Toque direto" (Modo do toque, estava "Touchpad"): ativou no 1º
   toque.
4. Computadores → botão "Ajustes" (rodapé, `TextView` comum sem `focusableInTouchMode`
   desde antes desta fatia): navegou para a tela no 1º toque.
5. Computadores → card "RAFAEL" (item de `GridView`, não clicável comum): abriu o fluxo
   de pareamento no 1º toque.

Depois, com `tools/fotos.mjs --avd s22ub`: rota `apps-foco` mostra o anel de foco por
D-pad ainda funcionando no card de app (prova de que tirar `focusableInTouchMode` não
quebrou o D-pad).

## 2. Proporção do card de app no celular

`app/src/main/res/layout/app_grid_item.xml` tinha a capa com `layout_width="match_parent"`
e altura **fixa** de 132dp. Numa grade de 2 colunas (`stretchMode="columnWidth"`), a
coluna acompanha a largura da tela — num celular de ~412dp a capa saía com ~190x132,
deitada, quando o desenho pede 3:4 em pé (a mesma proporção de `console_capa_w`/`console_capa_h`
= 160x213dp, citada pela fatia como referência).

Criada `com.limelight.grid.CapaAspectFrameLayout` (novo `FrameLayout`, mede a própria
altura a partir da largura medida: `height = width * 4/3`, `EXACTLY`). `app_grid_item.xml`
troca o `FrameLayout` de altura fixa por essa view, com `layout_height="wrap_content"`
— a proporção se mantém em qualquer largura de coluna, sem esticar nem distorcer a
imagem (`grid_image` continua `centerCrop`).

`app_grid_item.xml` é compartilhado por celular e tablet (`AppGridAdapter` não tem
variante por tela) — a correção vale para os dois. Conferido que não piora o tablet:
`app/build/fotos/tabs8u/apps.png` (4 colunas, `MoqueDeApps`) mostra cards 3:4 legíveis,
sem overflow nem corte.

Ajuste colateral necessário: `VidroScreenshotTest.java` capturava
`peca-card-de-app` com canvas fixo de 230dp de altura (dimensionado para a capa antiga
de 132dp); com a capa agora mais alta, esse canvas cortaria o rótulo abaixo dela.
Troquei para `heightDp=0` ("a view decide"), o mesmo padrão já usado para
`peca-linha-de-host`/`peca-card-de-cena`.

**Prova**: `app/build/vidro-screens/peca-card-de-app.png` (Robolectric, formato 3:4
visível) e `app/build/fotos/s22ub/apps.png` / `app/build/fotos/tabs8u/apps.png`
(emulador real, `MoqueDeApps` com 6 apps) — capas em pé nos dois AVDs, batendo com
`docs/design/console/fotos/B_Celular_Apps.png` (2 colunas, cards em pé).

## 3. Ajustes sem margens laterais no celular — causa raiz

`activity_stream_settings_basico.xml` (celular) tinha `android:fitsSystemWindows="true"`
**e** `android:paddingLeft/Top/Right="@dimen/console_espaco_4"` **na mesma view** (o
`ScrollView` raiz). `ViewGroup.fitSystemWindows()` — o caminho que roda quando não há
`OnApplyWindowInsetsListener` customizado — chama
`internalSetPadding(insets.left, insets.top, insets.right, insets.bottom)`, que
**substitui** o padding declarado pelo inset do sistema, não soma. Num celular sem
notch lateral esse inset é 0 à esquerda/direita, então os 16dp declarados viravam 0 —
confirmado visualmente (`crop_left.png`, recorte do scratchpad: o cartão de ajustes
tocava a borda esquerda da tela, x=0). O topo "parecia" certo por coincidência: o inset
da barra de status é maior que os 16dp pedidos, por isso ninguém reportou o topo.

Comparado com `activity_pc_view.xml`, `activity_app_view.xml`,
`activity_add_computer_manually.xml`, `activity_profiles.xml`,
`activity_stream_settings.xml` — todos têm `fitsSystemWindows="true"` na raiz, mas o
padding lateral vive num **filho** (título, cabeçalho, rodapé), imune à substituição.
Só a Ajustes básica (celular e tablet) tinha os dois atributos na mesma view.

### O que mudou

`activity_stream_settings_basico.xml` (celular): padding lateral tirado do
`ScrollView` e virou margem no `<include>` (filho, não sofre a substituição de
`fitSystemWindows()`). `paddingTop` do `ScrollView` ficou como estava, de propósito —
já era o inset do sistema na prática, e mexer nele empurraria o conteúdo mais para
baixo do que já estava (sem reclamação).

`layout-sw600dp/activity_stream_settings_basico.xml` (tablet): mesmo defeito, mesma
correção (padding movido para o `FrameLayout` interno) — no Tab S8 Ultra não dava para
notar porque a tela é muito mais larga que 720+64dp e a coluna nunca chega perto da
borda, mas o padding lateral era enganoso (não fazia nada). Corrigido por consistência
e para a regra valer de verdade em qualquer `sw600dp+` mais estreito.

**Prova**: `app/build/fotos/s22ub/ajustes-basico.png` — cartão com respiro lateral
visível dos dois lados, e `app/build/vidro-screens/10-ajustes-basico.png`
(Robolectric, confirmação independente). Tablet sem regressão:
`app/build/fotos/tabs8u/ajustes-basico.png` e `tab-10-ajustes-basico.png` idênticos ao
que já era antes (coluna de 720dp centrada).

## 4. Revisão das demais telas (fora da sessão)

Percorridas no `s22ub` via `node tools/fotos.mjs --avd s22ub --port 5554` (21 rotas,
`MoqueDeCasca`/`MoqueDeApps`/`MoqueDeStream`, sem host real) e visualmente inspecionadas
todas as capturas em `app/build/fotos/s22ub/`: Computadores, Apps, Início (carrossel do
tablet, tocado só no `tabs8u`), Ajustes básico, Ajustes avançado, Adicionar PC, Cenas,
pareamento. Nenhum crash (`logcat` limpo por rota, conferido pelo próprio `fotos.mjs`).

O que achei, além dos três itens já descritos:

- **Ajustes avançado** (`StreamSettings`, `PreferenceScreen` do androidx) tem descrições
  cortadas em uma linha ("Habilitar sistema de suporte à equalizaçã..", "Video bitrate
  on metered networks, Ritmo de q.."). É o comportamento padrão do
  `PreferenceScreen`/`ListPreference` do androidx renderizando resumo de uma linha, não
  algo desta fatia nem específico do celular — não mexi: a tela é intencionalmente a
  árvore de preferências "crua" (comentário do próprio arquivo, "intocada"), e mudar o
  comportamento do `Preference` é além do escopo de correção de defeito pontual.
  Registro para decisão de produto futura.
- **Segmentos de Ajustes básico têm 38dp de altura** (`consoleSeg*`), abaixo do mínimo
  de 48dp para alvo de toque. É dimensão deliberada, batendo com
  `B_Celular_Ajustes.png`, aprovada em fatia anterior (F4) — não mudei, porque alterar a
  altura reabriria decisão de design já fechada; registro em vez de inventar redesenho.
- Computadores, Apps, Adicionar PC, Cenas e Pareamento: sem proporção errada, sem
  elemento colado na borda, sem texto cortado além do caso acima, alvos de toque ≥48dp
  nos controles clicáveis novos/tocados. Nenhuma ação necessária.

Não alcancei conectando/queda com host real (sem Vibeshine disponível nesta sessão);
`sessao-conectando`/`sessao-queda` do `fotos.mjs` (moque) rodaram sem crash, mas não
comparei pixel a pixel contra mockup — essas telas são território F5/G2, só conferidas
aqui para descartar regressão.

## 5. O que a G2 precisa aplicar

`console_guia.xml`, `console_teclas_fileira.xml` e `activity_game.xml` têm exatamente o
mesmo padrão de bug (`clickable + focusable + focusableInTouchMode` numa `View`
comum), não tocados por serem território da G2:

- `console_guia.xml`: 10 blocos clicáveis comuns —
  `consoleBlocoTeclado`, `consoleBlocoBarraTexto`, `consoleBlocoModoToque` e mais 5
  blocos da fileira horizontal (linhas ~59–279), mais `consoleBlocoDesconectar` e
  `consoleBlocoEncerrar` (linhas ~326/365). Todos `LinearLayout`/`FrameLayout` com
  `OnClickListener` — mesmo padrão exato dos segmentos que corrigi em Ajustes básico.
  Recomendo remover `focusableInTouchMode`, manter `focusable="true"`.
- `console_teclas_fileira.xml`: 5 teclas (`consoleKeyEsc`, `consoleKeyTab`, e mais 3),
  `TextView` clicável comum — mesmo padrão. Mesma recomendação.
- `activity_game.xml`: **diferente** — `focusableInTouchMode` está no `streamContainer`
  (`StreamContainer`, `focusedByDefault="true"` + `<requestFocus />`), a superfície que
  recebe foco para capturar teclado/gamepad, sem `OnClickListener` nem gesto de toque
  associado a essa `View` especificamente. Não é o mesmo padrão de bug (não há um clique
  sendo suprimido); não precisa da mesma remoção, mas fica registrado para a G2 avaliar
  com contexto de `Game.java`, que esta fatia não deveria tocar.

Não testei o efeito real do bug nessas três telas (não posso editá-las nem instalar uma
build com esses elementos alterados para provar) — a identificação é por leitura de
código, comparando o padrão de atributos com os que reproduzi e corrigi em Ajustes
básico e no carrossel.

## 6. Gate

```
node tools/codemap/codemap.mjs   → 225 arquivos, 67.308 linhas (novo: CapaAspectFrameLayout.java)
node tools/validate.mjs
[ OK ] codemap
[ OK ] jni
[ OK ] build
[ OK ] test — 4 falha(s) preexistente(s), nenhuma nova
validate: passou.
```

As 4 falhas de teste já existiam antes desta fatia (`validate.mjs` as rotula como
preexistentes; não investiguei a causa — não é regressão introduzida aqui). Lembrete de
`.claude/rules/tests.md`: a suíte roda em Robolectric sem `System.loadLibrary` — não
prova nada sobre a fronteira JNI, protocolo ou decodificação; aqui, como não toquei
nenhum dos dois, é só a garantia de que nada de lógica Java quebrou.

`node tools/screens.mjs`: 30 telas renderizadas sem erro em `app/build/vidro-screens/`.

## 7. Fotos

- `app/build/vidro-screens/` (Robolectric, `tools/screens.mjs`): 30 arquivos, inclusive
  `peca-card-de-app.png` (capa 3:4 isolada) e `10-ajustes-basico.png`/
  `tab-10-ajustes-basico.png` (margem lateral, celular e tablet).
- `app/build/fotos/s22ub/` (emulador real, `tools/fotos.mjs`): 21 rotas + `folha.png`,
  0 crash.
- `app/build/fotos/tabs8u/` (idem, tablet): 21 rotas + `folha.png`, 0 crash — conferência
  de não-regressão do tablet depois de mexer em `app_grid_item.xml` (compartilhado) e
  em `layout-sw600dp/activity_stream_settings_basico.xml`.

## 8. Não verificado — honestamente

- **G2 (`console_guia.xml`, `console_teclas_fileira.xml`)**: identifiquei o mesmo padrão
  de bug por leitura, mas não reproduzi nem corrigi — são arquivos vetados para esta
  fatia. A recomendação do §5 não foi testada em build.
- **Telas de sessão com host real**: só moque (`MoqueDeStream`). Conectando/queda,
  guia em uso de verdade, tudo isso só foi visto sem crash, não comparado contra
  mockup nem contra comportamento real de rede.
- **Ajustes avançado**: descrições cortadas registradas, não investigado se o
  `PreferenceScreen` tem alguma configuração (ex. `singleLineTitle`/resumo custom) que
  resolveria sem redesenho — fica como observação, não como correção tentada.
- **Aparelho físico**: só os dois AVDs (`s22ub`, `tabs8u`), nenhum teste no Galaxy S22
  Ultra real do dono do fork.
- **D-pad físico**: a prova de que `focusable` sozinho basta veio da rota `apps-foco`
  do `fotos.mjs` (que navega via `input keyevent`/lógica interna do script) e de
  leitura da documentação do framework — não testei com um D-pad USB/Bluetooth de
  verdade plugado no aparelho.
- **`ajustes-persistencia`/`inicio-dialogo`**: rotas do `fotos.mjs` que rodaram sem
  crash mas não inspecionei o conteúdo de cada uma a fundo (o aviso
  "consolePcChipAtual não encontrado" na rota `inicio-dialogo` do `s22ub` é
  pré-existente ao meu trabalho — a tela de Início/diálogo de PCs no celular não expõe
  esse id, só o tablet; não investiguei se é uma falha do script ou uma diferença de
  tela esperada).

## 9. Commits

Em fatias lógicas na branch `fatia/G1-toque`, sem merge em `dev`, sem push (ver
`git log`).
