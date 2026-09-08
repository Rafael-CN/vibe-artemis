# G2 — A sessão no aparelho — implementação

Branch `fatia/G2-sessao2`, worktree `Artemis-G2`, base `dev` (`e91d3999`, todas as fatias
Console anteriores já integradas). Emulador `s22u` na porta 5556; `tabs8u` também na 5556,
um de cada vez, cada um derrubado (`emu kill`) antes do próximo. `git submodule update
--init --recursive` foi necessário no início (submódulo vazio nesta cópia do worktree).

Esta fatia corrige defeitos que o dono do fork relatou depois de instalar o APK no S22
Ultra de verdade — as fatias anteriores desenharam para foco por D-pad e verificaram no
emulador; o toque ficou de segunda classe. Prioridade aqui foi o toque.

## Defeito 1 — "o click nos botões está estranho, o primeiro click é um focus e o segundo é que ativa"

**Causa raiz, confirmada no código-fonte da plataforma Android:** `View#onTouchEvent()`,
no `ACTION_UP`, roda isto (parafraseado):

```java
if (isFocusable() && isFocusableInTouchMode() && !isFocused()) {
    focusTaken = requestFocus();
}
if (!focusTaken) {
    performClick();
}
```

Todo bloco da guia e toda tecla da fileira especial tinha `android:focusableInTouchMode
="true"` **e** `android:clickable="true"` — exatamente a combinação que aciona esse
caminho. No primeiro toque, o bloco ainda não tinha foco; `requestFocus()` "pegava"
(`focusTaken = true`) e o clique daquele mesmo gesto era descartado. Só no segundo toque,
com o bloco já focado, `requestFocus()` retorna falso e `performClick()` roda.

`focusableInTouchMode` tinha sido acrescentado pela F6 para resolver um problema
diferente: `View#requestFocus()` **falha silenciosamente** numa view `focusable=true` sem
`focusableInTouchMode=true` enquanto o aparelho está em modo de toque (achado real da F3,
documentado em `F3-implementacao.md`) — e a F6 usou isso para o foco inicial
programático da guia (`GameMenu#wireGuia`, "foco no primeiro bloco") funcionar mesmo
quando a guia abre por toque. Só que essa mesma flag, colocada no bloco que também é
`clickable`, é o que aciona o desvio de clique acima.

**A saída não precisava de nenhuma das duas coisas ao mesmo tempo.** D-pad não depende de
`focusableInTouchMode`: uma tecla de seta já tira o aparelho do modo de toque (Android
chama isso de sair do "touch mode") **antes** de rodar o traversal de foco — e fora do
modo de toque, `requestFocus()` funciona em qualquer view `focusable=true` comum. Ou seja:
o foco inicial programático só "pega" quando a guia foi aberta por D-pad (o caso que
importa, capa-teclado do tablet); abrir por toque simplesmente não mostra anel de foco
nenhum, que é o comportamento certo para quem usa o dedo.

**Mudança:** removido `android:focusableInTouchMode="true"` das 10 ocorrências em
`console_guia.xml` e das 5 em `console_teclas_fileira.xml`. Nenhum código Java mudou —
`GameMenu.onBlock()`/`wireTecla()` já usavam `setOnClickListener`, que sempre funcionou
certo; o defeito inteiro morava na XML. **Não mexido:** a 1 ocorrência em
`activity_game.xml` (o `StreamContainer`) — é o mecanismo de foco para o teclado físico
receber eventos de tecla na superfície de vídeo, categoria diferente do defeito relatado
(não é `clickable`, não aciona `performClick`).

**Prova, não só leitura:** com o `s22u` na 5556, guia aberta por toque na aba
(`adb shell input tap` no centro dela), toquei uma vez em "Estatísticas"
(`adb shell input tap 1561 83`) e conferi por `uiautomator dump` **antes e depois de um
único toque**:

- antes: `consoleBlocoEstatisticas` presente na árvore (guia aberta).
- depois de UM toque: `consoleBlocoEstatisticas` já não existe (guia fechou) **e**
  `performanceOverlayBig` passou a existir com bounds não-vazios (o HUD ligou). As duas
  coisas exigem que `onClick` tenha rodado — não haveria como a guia fechar sozinha.

Documentado o porquê nos dois arquivos XML, para a próxima fatia não reintroduzir a
combinação por engano.

## Defeito 2 — "o menu na stream tá ruim de ativar"

A aba mede 56×16dp no celular (64×20dp no tablet, `console_aba_w`/`console_aba_h`) — 16dp
de altura é menos de um terço do alvo mínimo de toque recomendado (48dp,
`vidro_touch_target`). Não dava para simplesmente aumentar o desenho sem contrariar o
mockup (a aba é discreta de propósito, meio para dentro/meio para fora da moldura).

**Mudança, `Game.java#setupConsoleAba`:**

- `TouchDelegate` no pai da aba (`android.R.id.content`), recalculado a cada
  `onLayoutChange` da própria aba (ela muda de posição com rotação): a área tocável
  cresce 20dp em cima/embaixo e 32dp nas laterais (mais folga na horizontal — um alvo
  fino e largo erra mais para os lados), delegando de volta para a aba. O retângulo
  desenhado não muda nem 1dp.
- Deslize para baixo a partir da aba como atalho **adicional** para abrir a guia —
  rastreamento manual de `ACTION_DOWN/MOVE/UP` (mesmo padrão já usado no arrastar do
  `overlayToggleZoomButton`, `zoomButtonDX/DY` etc.), só reage a arrasto predominantemente
  vertical (não briga com a rolagem lateral da guia já aberta), e nunca substitui o toque
  simples — o `OnClickListener` de sempre continua ativo e é quem processa um toque que
  não vira arrasto. Não é gesto de borda do sistema: a aba já fica alguns dp abaixo do
  topo físico da tela, fora da faixa que o One UI reserva para o próprio gesto de puxar a
  cortina de notificações (não há sobreposição de área, e o gesto só é reconhecido depois
  que o toque já pousou dentro do `TouchDelegate` da aba).

**Não verificado no vídeo vivo:** o quanto a área expandida "sente-se" melhor ao toque só
o aparelho do dono prova — o moque não tem o mesmo peso de resposta tátil que um toque
real sobre `SurfaceView` decodificando quadros. Testei a extensão da área via
`TouchDelegate` lendo o código e a lógica de `getHitRect`/margens, não por captura de
toque fora do retângulo desenhado (não há uma forma simples de provar "toquei fora do
desenho e funcionou" por `adb shell input tap` sem antes saber o retângulo exato do
delegate em tempo de execução, e extrair isso exigiria instrumentação adicional fora do
escopo do que já existe em `tools/fotos.mjs`).

## Defeito 3 — "no celular tudo quadrado" (blocos da guia)

Comparei `docs/design/console/fotos/B_Celular_Sessao_Menu.png` com uma captura do estado
anterior à mudança (`node tools/screens.mjs`, `04-sessao-guia.png`), normalizando as duas
imagens para a mesma largura: o bloco "Teclado" ocupava ~15,5% da largura da barra no
app contra ~9,6% no mockup — cerca de 1,6× mais largo que deveria, proporcionalmente.

**Causa raiz:** os blocos usavam `android:minWidth="96dp"` (Desconectar) / `88dp`
(Voltar) / `104dp` (Encerrar) **copiados literalmente do tablet** — a mesma F1 que definiu
esses números documentou só dois tokens como diferentes por aparelho
(`console_linha_ajuste`, `console_aba_w/h`); o `minWidth` dos blocos nunca ganhou um
bucket próprio de celular. Num ícone de 24dp com rótulo curto ("Teclado"), 96dp de largura
mínima sobra tanto que o bloco fica mais largo que alto — quadrado/desproporcional, não a
barra fina e compacta do mockup.

**Mudança:** três tokens novos, com valores menores em `values/dimens.xml` (celular) na
mesma proporção (×0,667 arredondado ao grid de 4dp) e os valores originais preservados em
`values-sw600dp/dimens.xml` (tablet, sem mudança nenhuma):

| token | celular (novo) | tablet (igual a antes) |
|---|---|---|
| `console_bloco_min_w` | 64dp | 96dp |
| `console_bloco_voltar_min_w` | 56dp | 88dp |
| `console_bloco_encerrar_min_w` | 72dp | 104dp |

`console_guia.xml` passou a referenciar esses tokens em vez dos números fixos.

**Prova:** `node tools/screens.mjs` antes/depois (`04-sessao-guia.png`), normalizado à
mesma largura — depois da mudança, os mesmos 900px normalizados mostram "Keyboard", "Text
bar", o bloco sem rótulo do Modo do toque (rótulo só existe em runtime, ver
`AGENTS.md`/limitação do `shoot`), "Special keys", "Rotate screen" e "Statistics" inteiros
— praticamente a mesma densidade do mockup, contra só 3 rótulos completos antes. Também
`node tools/fotos.mjs --avd s22u --port 5556 --rotas sessao-guia` (aparelho real/moque):
`docs/planos/console/fotos/G2-s22u.png`.

**Coerência do tablet:** `values-sw600dp/dimens.xml` não mudou nenhum valor, só nomeou os
três números que já existiam. Conferido com `node tools/fotos.mjs --avd tabs8u --port
5556 --rotas sessao-guia,sessao-texto` — `docs/planos/console/fotos/G2-tabs8u.png` mostra
os 7 blocos com rótulo completo, exatamente como antes desta fatia.

## Defeito 4 — "o digitar com input tá mto grande e n dá pra ver a tela"

**Causa raiz:** a barra de texto reusava os tokens de espaçamento compartilhados de todo o
app Console (`console_espaco_4/3/2` — 16/12/8dp) para o próprio recuo e para os respiros
entre as três fileiras (cabeçalho · campo+enviar · teclas especiais), e a fileira de
teclas especiais **sempre** ficava visível, mesmo com o teclado virtual já ocupando
metade da tela. Com o teclado aberto, cada dp que a barra ocupa acima dele é um dp de
vídeo a menos — e a fileira de teclas especiais sozinha já valia ~48dp mais o respiro.

**Mudança 1 — recuos próprios, menores no celular:** três tokens novos
(`console_barra_texto_pad_h/pad_v/gap`) com valores reduzidos em `values/dimens.xml`
(12/8/4dp) e os valores antigos preservados em `values-sw600dp/dimens.xml` (16/12/8dp,
idênticos a `console_espaco_4/3/2` — o tablet não perde altura nenhuma).

**Mudança 2 — a fileira de teclas especiais recolhe por padrão no celular:** um
alternador novo (`consoleBarraTextoTeclasToggle`, reaproveitando a mesma string "Special
keys" e os mesmos drawables de chevron da aba) mostra/esconde
`consoleBarraTextoTeclasRow`. `Game.java#setupComposeBarTeclasToggle` decide o estado
inicial pelo bool `console_barra_teclas_recolhida_por_padrao` (`true` no celular, `false`
no tablet — lá o alternador nem aparece, a fileira sempre visível como sempre esteve).
Nenhuma mecânica de envio mudou: `wireComposeBarTecla` continua ligando as 5 teclas
exatamente como antes, só a visibilidade inicial da fileira que as contém é nova.

**Mudança 3 — a fileira virou rolagem horizontal:** era um `LinearLayout` simples; agora é
uma `HorizontalScrollView`. Ctrl+Alt+Del sozinho já passa de 112dp de largura mínima —
cinco teclas lado a lado não cabem na largura de um celular em retrato sem rolar, e sem
isso as últimas ficavam simplesmente fora da tela, inalcançáveis.

**O que ficou intacto, de propósito:** `openComposeBar`/`closeComposeBar`/`sendTextBlock`
e `ImeDisplayMode.RESIZE` por padrão — nenhuma dessas funções foi tocada, só o desenho ao
redor delas.

**Prova:** `docs/planos/console/fotos/G2-barra-texto-teclado-s22u.png` — barra de texto
aberta no `s22u` em retrato **com o teclado virtual Gboard de verdade aberto** (não moque
de teclado): cabeçalho enxuto, campo+Enviar numa linha só, fileira de teclas especiais
recolhida (não visível), e a maior parte da tela acima do teclado ainda é vídeo. Também
confirmei por `uiautomator dump`, antes de abrir o teclado, que `consoleBarraTextoTeclas
Toggle` existe e `consoleBarraTextoTeclasRow` **não** aparece na árvore (recolhida por
padrão) logo depois que `openComposeBar` roda.

**Não verificado:** o toque no próprio alternador para expandir a fileira de volta — duas
tentativas via `adb shell input tap` acabaram acertando a barra de ferramentas do próprio
Gboard (emoji/figurinhas) em vez do alternador do app, porque o teclado da tela mudou de
altura entre o dump e o toque e as coordenadas ficaram velhas. A lógica em si
(`toggle.setOnClickListener` trocando visibilidade e o ícone do chevron) é simples e foi
conferida por leitura; expandir e recolher de fato com o teclado aberto fica para quem
testar no aparelho.

## Defeito 5 — "no modo vertical a stream n se adapta e fica cortada"

Este foi o mais delicado do lote — dimensionamento de `SurfaceView` é a parte mais frágil
do app (`.claude/rules/input.md`, `AGENTS.md`). Investiguei antes de tocar em qualquer
coisa, com cuidado redobrado para não esbarrar em flag de janela, decodificação ou
entrada.

**O que já existe, e por quê:** `Game.java#setPreferredOrientationForActivity()` tem um
ramo — `prefConfig.allowPortraitSession && !prefConfig.onscreenController` — que põe a
`Activity` em `SCREEN_ORIENTATION_FULL_USER` ("sessão livre para girar"; comentário no
próprio código, aparentemente de uma fatia anterior a esta). `allowPortraitSession` é
`true` por padrão. Nesse modo, `StreamContainer.onMeasure()` (não tocado nesta fatia)
já faz o letterbox: descontando padding, mede o filho preservando `desiredAspectRatio`
(a proporção negociada com o host) sem cortar nem esticar, a não ser que o modo de escala
seja `FILL` (recorta por definição, comportamento esperado desse modo) ou `STRETCH`
(ignora esse ramo inteiro).

**O que eu verifiquei, ao vivo (não só lendo código):** limpei os dados do app
(`pm clear`, para garantir preferências realmente padrão — `autoInvertVideoResolution`
off, `onscreenController` off, `videoScaleMode` FIT, todos os defaults documentados em
`PreferenceConfiguration.java`), girei o `s22u` para retrato (`settings put system
user_rotation 0`) e subi `com.limelight.Game --ez MoqueDeStream true` manualmente.
`uiautomator dump` confirmou `streamContainer` com bounds `[0,918][1080,1525]` numa tela
de `1080×2316`: **1080×607px, largura cheia, altura reduzida para ~16:9** — o vídeo cabe
em letterbox, não corta. `docs/planos/console/fotos/G2-retrato-s22u.png` é a foto dessa
sessão (o moque desenha superfície preta sólida — a prova de verdade está no bounds do
`uiautomator`, não no pixel, porque **moque não é vídeo vivo**).

**Por que não mudei nada aqui:** não encontrei bug de medida para reproduzir. A única
forma de reproduzir corte que localizei foi mudando as próprias preferências —
`checkbox_show_onscreen_controls=true` desativa esse ramo inteiro (cai em "Lock to
current orientation", que trava a Activity na orientação de quando a sessão começou,
tipicamente paisagem, porque `autoOrientation` também é `false` por padrão) — ou
`videoScaleMode=FILL`, que recorta por *definição* do próprio modo, não por defeito de
medida. As duas são escolhas de configuração, não bugs de `StreamContainer`. Forçar
`FIT` em retrato independente da preferência do usuário mudaria comportamento fora do que
o defeito relatado pede, e o próprio dono do fork não indicou ter mexido nessas duas
opções.

**Recomendo ao dono do fork, antes de reabrir este defeito:** conferir, no aparelho real,
Ajustes → "Mostrar controle na tela" (deveria estar desligado, ele não joga com gamepad
virtual) e a "Escala de vídeo" (deveria estar em "Ajustar", não "Preencher"/"Esticar"). Se
as duas já estiverem como o padrão espera e a stream ainda cortar em retrato **com vídeo
de verdade**, é preciso reabrir com essa confirmação — o cenário mais provável, dado que
o moque não decodifica quadro nenhum, é uma diferença de comportamento entre a superfície
falsa do moque e o `MediaCodec` decodificando de verdade (fora do que consigo reproduzir
sem host).

## Gate e o que não foi verificado

- `node tools/validate.mjs`: **passou** (codemap, jni, build, test — 4 falhas
  pré-existentes, nenhuma nova).
- `node tools/screens.mjs`: rodado antes/depois da mudança do defeito 3 para comparação.
- `node tools/fotos.mjs --avd s22u --port 5556` (bateria completa, 22 rotas): 22/22
  capturadas, sem crash. `sessao-retrato` precisou de 3 tentativas do próprio harness para
  a rotação assentar (mesma flakiness de `esperarRotacaoAplicada` sob
  `swiftshader_indirect` já documentada no arquivo para as outras rotas de sessão) — não é
  regressão desta fatia.
- `node tools/fotos.mjs --avd tabs8u --port 5556 --rotas sessao-guia,sessao-texto`:
  2/2, usado só para a conferência de coerência do tablet (defeito 3).
- **Não verificado, honestamente:** a aparência de qualquer coisa desta fatia sobre
  **vídeo vivo** — moque não é vídeo vivo, e isso só o aparelho do dono prova. Isso inclui
  a "textura" real do toque na aba expandida (defeito 2), o recolher/expandir do
  alternador de teclas especiais com o teclado de verdade aberto (defeito 4, tentativa
  frustrada documentada acima), e principalmente o defeito 5 com decodificação real —
  ver a recomendação de configuração acima antes de reabrir.

## Commits desta fatia

1. `Fix double-tap on session guia blocks and slim them for phone` — defeitos 1 e 3.
2. `Widen the session tab's touch area and add a swipe-down shortcut` — defeito 2.
3. `Shrink the compose bar for phone and collapse its special keys row` — defeito 4.
4. `Add a portrait verification route to fotos.mjs` — ferramenta de verificação do
   defeito 5 (nenhuma mudança de comportamento do app).
5. `Regenerate code maps after the G2 session slice`.

Sem merge em `dev`, sem push — conforme instruído.
