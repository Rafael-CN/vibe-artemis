# G1 — Toque primeiro, e proporção no celular — verificação

Verificador independente, sem contexto do implementador (a sessão anterior morreu no
limite de uso sem deixar commit nem relatório — comecei do zero). Worktree
`C:\Users\Rafael\Documents\Projetos\Claude\Artemis-G1`, branch `fatia/G1-toque`, base
`dev` = `e91d3999` (4 commits à frente, árvore limpa no início). `G1-implementacao.md` foi
lido, mas tratado como alegação a conferir — toda alegação relevante foi reproduzida por
conta própria: toque real via `adb shell input tap`, medição de bounds via
`uiautomator dump`, gate por conta própria, fotos nos dois AVDs.

Emuladores: `s22ub` (5554) para o grosso do trabalho, `tabs8u` (5554, `s22ub` derrubado
antes) só para conferir que o tablet não regrediu. Um de cada vez, `adb devices`
conferido nas trocas. Havia um `emulator-5556` de pé o tempo todo (fatia G2, em
paralelo) — não toquei nele, nem o `s22u`. Submódulo `moonlight-common-c` já estava
inicializado neste worktree.

**Emulador não é o aparelho do dono.** Tudo abaixo foi confirmado no `s22ub`/`tabs8u`
(Android emulado, swiftshader), não no Galaxy S22 Ultra físico do Rafael. A causa raiz do
toque duplo (`View.onTouchEvent` + `requestFocus()`) é comportamento documentado do
framework, não específico de emulador, mas a prova de "primeiro toque funciona" abaixo é
só emulador.

## Veredito: **APROVADA**

Toquei dez controles ou mais em seis telas diferentes com um único `input tap` cada; todos
ativaram no primeiro toque, com efeito comprovado (não só anel de foco). Medi a capa 3:4
em pixel real nos dois AVDs — proporção exata. Margens de Ajustes conferidas e simétricas
(42px = 16dp × 2,625px/dp, batendo com `console_espaco_4`) nos dois tamanhos. Os dois itens
que o implementador registrou e não corrigiu foram julgados: a truncagem do Ajustes
avançado, aceito (comportamento padrão do androidx Preference, fora de escopo); os 38dp
dos segmentos, **não aceito como definitivo** — apliquei a correção pequena e óbvia que a
instrução previu (ampliar a área de toque sem alterar o desenho) e a testei tocando na
folga. Gate verde, sem regressão de teste, sem tocar nada da G2.

## 1. Toque único — prova por toque, controle a controle

Todos os toques abaixo foram `adb shell input tap <x> <y>` num único toque (nunca dois),
contra o APK **recompilado nesta sessão** a partir do HEAD da branch (`git show --stat`
confirmou que o commit mais recente antes da minha correção, `c1492b00`, só adiciona o
relatório .md — o binário instalado reflete o código de `bf8fda87`/`9338cec5`/`ef941c94`
de fato). Efeito de cada um, comprovado por `uiautomator dump` (valor/estado) ou
screenshot (tela nova, diálogo aberto):

| # | Tela | Controle | Efeito no 1º toque |
|---|------|----------|---------------------|
| 1 | Ajustes básico | Segmento "90 Hz" (Taxa) | Selecionado, preenchimento verde (`60 Hz`→`90 Hz`) |
| 2 | Ajustes básico | Segmento "Auto" (Codec) | Selecionado (`AV1`→`Auto`) |
| 3 | Ajustes básico | Segmento "Touchpad" (Modo do toque) | Selecionado (`Toque direto`→`Touchpad`) |
| 4 | Ajustes básico | Switch "Barra de texto" | `checked` true→false confirmado no dump |
| 5 | Ajustes básico | Switch "Controles na tela" | `checked` true→false confirmado no dump |
| 6 | Ajustes básico | Card "Avançado" | Abriu `StreamSettings` (`mCurrentFocus` mudou de Activity) |
| 7 | Ajustes avançado | Linha "Configurações de som Surround" | Abriu diálogo de escolha única |
| 8 | Ajustes avançado | Checkbox "Habilitar sistema de suporte à equalização" | `checked` true→false confirmado no dump |
| 9 | Computadores | Card "RAFAEL" (item de GridView) | Abriu diálogo de pareamento (PIN) |
| 10 | Computadores | Botão "Cancelar" (diálogo de pareamento) | Fechou o diálogo, voltou pra grade |
| 11 | Computadores | Botão "Adicionar PC" | Abriu `AddComputerManually` |
| 12 | Adicionar PC | Campo "Endereço do PC" | Focou e aceitou texto digitado (`input text`) |
| 13 | Adicionar PC | Botão "Conectar" | Iniciou diálogo "Conectando ao PC…" |
| 14 | Cenas/Perfis | Botão "Salvar a sessão atual como cena" | Abriu tela de edição de perfil novo |
| 15 | Cenas/Perfis (editor) | Seta de voltar | Retornou pra `ProfilesActivity` |
| 16 | Apps (moque `MoqueDeApps`) | Card "Steam" (item de GridView) | Abriu menu de opções do app |

16 controles, 6 telas (Computadores, Ajustes básico, Ajustes avançado, Adicionar PC,
Cenas/Perfis, Apps) — acima do mínimo de dez pedido. Nenhum exigiu um segundo toque.

Sobre os três containers de `GridView` que o implementador manteve com
`focusableInTouchMode` (`app_grid_view.xml` ×2, `pc_grid_view.xml`): os controles #9 e #16
são exatamente itens dessas grades (Computadores e Apps) e ativaram no primeiro toque —
confirma a alegação de que o atributo no container do `AbsListView` não é a causa do bug
(o `AbsListView` despacha o clique por `performItemClick`, sem passar pelo
`requestFocus()` de `View.onTouchEvent()` que afeta uma `View` comum clicável).

**Achado ao ler `tools/fotos.mjs` (não tocado por esta fatia):** a rota
`ajustes-persistencia` ainda tem um `tocarSegmentoPorId` que toca cada segmento **duas
vezes de propósito**, com um comentário do autor original documentando o próprio bug do
toque duplo ("o PRIMEIRO toque numa TextView de segmento… não dispara OnClickListener
nenhum — só o SEGUNDO toque… dispara"). Isso é módulo de fora do escopo da G1 (script de
fotografia, não app), e o comentário descreve o estado **anterior** à correção — a
correção em si eu testei diretamente por toque único (itens 1–3 da tabela), não através
desse script. O script ficou desatualizado (continua funcionando, só faz um toque
redundante a mais por segmento) — não é um defeito da fatia, é uma leve dívida de
tooling; registro aqui para quem for tocar `fotos.mjs` depois, não bloqueia esta
aprovação.

## 2. Capa de app 3:4 no celular — medição real

`uiautomator dump` no `s22ub` (grade de 2 colunas, `MoqueDeApps`), bounds do
`grid_image` do primeiro card:

```
[58,603][529,1231] → largura 471px, altura 628px → altura/largura = 1,3333… = 4/3 exato
```

Sem distorção: `grid_image` é `centerCrop` (conferido no XML), cor sólida preenche o
retângulo inteiro sem faixa nem corte na foto (`app/build/fotos... apps.png` no
scratchpad da sessão, e visualmente na captura abaixo).

**Tablet (`tabs8u`, 4 colunas)** — mesma medição, mesmo APK:

```
[72,459][767,1386] → largura 695px, altura 927px → altura/largura = 1,3338 ≈ 4/3
```

Sem overflow, sem corte, cards legíveis nas duas fotos (`tab_apps2.png` do scratchpad).
Sem regressão.

## 3. Margens em Ajustes — celular e tablet

**Celular** (`s22ub`, `uiautomator dump` de `Ajustes básico`): `consoleAvancadoCard`
bounds `[42,1995][1038,2190]`, tela 1080px de largura → margem esquerda 42px, margem
direita 1080−1038=42px. Simétrico. `console_espaco_4` = 16dp; a 420dpi (2,625px/dp),
16dp = 42px — bate exatamente com o padding aplicado. Nada colado na borda, nada com
margem dupla (o card não tem padding extra somado ao do `ScrollView`).

**Tablet** (`tabs8u`, mesma tela): coluna centrada, sem tocar a borda da tela em nenhum
dos dois lados (captura em `tab_ajustes.png` do scratchpad) — comportamento esperado
(coluna de 720dp centrada, aprovado desde F4), sem regressão da correção do
`fitsSystemWindows`.

## 4. Os dois itens não corrigidos pelo implementador — julgamento

**(a) Truncagem de linha única no Ajustes avançado.** Confirmado visualmente
(`"Habilitar sistema de suporte à equalizaçã.."`, `"Video bitrate on metered networks,
Ritmo de q.."`) — comportamento padrão do `androidx.preference.PreferenceScreen`
(resumo/título de uma linha por padrão). **Aceito sem correção**: é comportamento do
framework em uma tela que o próprio código já comenta como "intocada" por design (a
árvore de 136 preferências, "esconder pode, apagar não"); mudar exigiria customizar
`PreferenceViewHolder` ou trocar `singleLineTitle`/formatação de resumo em toda a árvore
— redesenho, não correção pontual, e fora do escopo desta fatia (que é sobre toque e
proporção, não sobre a árvore avançada).

**(b) Segmentos com 38dp de altura.** Confirmado (`ConsoleSegmento`, `minHeight="38dp"`,
mas a altura real do `TextView` é fixada em `android:layout_height="38dp"` no XML,
abaixo do mínimo de 48dp). A reclamação do dono do fork foi especificamente **sobre
toque**, então **não aceitei manter como estava**: apliquei a correção pequena e óbvia
prevista pela própria tarefa — ampliar a área que responde ao toque sem alterar o que é
desenhado — e a testei.

### A correção aplicada (commit `600a068a`)

- `app/src/main/res/layout/console_ajustes_basico_conteudo.xml`: os quatro containers de
  segmento (`consoleResolucaoSegmentos`, `consoleTaxaSegmentos`, `consoleCodecSegmentos`,
  `consoleModoToqueSegmentos`) passam de `layout_height="wrap_content"` para
  `match_parent` + `gravity="center_vertical"` — passam a ocupar a altura cheia da linha
  (56dp, `@dimen/console_linha_ajuste`) em vez de só a altura da pílula (38dp). A pílula
  em si não muda: mesma altura de 38dp, mesmo desenho, mesma posição visual (centralizada
  pelo `gravity` novo).
- `StreamSettingsBasico.java`: `configurarSegmentado()` agora chama um novo método,
  `ampliarAreaDeToque(container, segmentos)`, que registra um `OnTouchListener` no
  container. Esse listener só é invocado quando o toque cai **fora** dos limites reais de
  qualquer pílula (do contrário a própria pílula, que continua `clickable`/`focusable`
  como sempre, consome o toque primeiro) — resolve manualmente qual pílula está mais
  próxima na horizontal e, se o toque também estiver dentro da folga vertical de 5dp
  (38dp + 5+5 = 48dp), chama `performClick()` nela. Não usei `android.view.TouchDelegate`
  porque ele só aceita um retângulo por View pai — não serve quando duas ou três pílulas
  dividem o mesmo pai lado a lado.

### Prova por toque (não só leitura de código)

- Tela "Taxa": toquei em `(703, 735)` — 9px acima do topo visível da pílula "90 Hz"
  (`consoleSeg90` bounds `[616,744][790,844]`, container `[421,721][996,868]`), dentro da
  folga de 13px (5dp × 2,625px/dp). Resultado: **"90 Hz" selecionado no mesmo toque**
  (captura `folga_test.png`).
- Sanidade contra falso positivo: toquei em `(900, 725)`, 19px acima do topo de "120 Hz" —
  fora da folga de 13px. Resultado: **nada mudou** ("90 Hz" continuou selecionado,
  captura `nofolga_test.png`) — a expansão não virou "o container inteiro é clicável".
- Tela "Modo do toque" (grupo de só 2 segmentos, caso de borda do laço): toquei em
  `(590, 1520)`, 9px acima do topo de "Toque direto" — "Toque direto" selecionado no
  mesmo toque (`toque_folga.png`).
- Toque normal dentro da pílula continua funcionando: `(742, 1129)` no centro de "HEVC"
  ativou normalmente (`hevc_test.png`).
- D-pad: `DPAD_LEFT` + `ENTER` depois dos testes moveu o foco visualmente (anel de foco em
  "Início") sem crash — a mudança não tocou `focusable`/`clickable` de nenhuma pílula, só
  a altura do container e um listener que só reage fora dos filhos.

Rebuild + reinstall confirmados antes de cada rodada de toque (`gradlew
assembleNonRoot_gameDebug` limpo, sem erro).

## 5. Gate

```
node tools/codemap/codemap.mjs
  → 225 arquivos | 67.372 linhas | 281 tipos | 2653 métodos | 30 pacotes | 36 TODOs

node tools/validate.mjs
[ OK ] codemap
[ OK ] jni
[ OK ] build
[ OK ] test — 4 falha(s) preexistente(s), nenhuma nova
validate: passou.

node tools/screens.mjs
  → 30 tela(s) em app/build/vidro-screens, sem erro
```

`codemap` falhou **antes** de eu regenerá-lo (esperado — editei XML/Java sem regenerar
ainda); rodei `codemap.mjs` e o gate passou limpo na segunda tentativa. As 4 falhas de
teste são as mesmas preexistentes descritas pelo implementador (não investiguei a causa —
não é regressão desta sessão, nem da correção que apliquei). `screens.mjs` gerou as
mesmas 30 telas do relatório do implementador, sem erro, incluindo `10-ajustes-basico.png`
e `peca-card-de-app.png` (conferidos visualmente — proporção 3:4 e margens presentes
também no Robolectric, confirmação independente do emulador).

## 6. Fronteiras

```
git diff e91d3999..HEAD --stat -- Game.java GameMenu.java activity_game.xml \
  console_guia.xml console_aba.xml console_barra_texto.xml console_teclas_fileira.xml
  → (vazio — nenhum tocado)

git diff e91d3999..HEAD -- '*.xml' '*.java' | grep '^\+' | grep -iE '#[0-9a-fA-F]{3,6}' \
  | grep -v console_
  → (vazio — nenhuma cor hex fora dos tokens console_*)

git diff e91d3999..HEAD --stat -- 'app/src/main/res/values-*'
  → (vazio — nenhuma tradução tocada)

git diff e91d3999..HEAD --name-only | grep -iE '\.kt$|\.kts$'
  → (vazio — nenhum Kotlin)
```

Todas as quatro checagens de fronteira vieram vazias, como pedido.

## 7. Não verificado — honestamente

- **Aparelho físico do dono.** Só `s22ub`/`tabs8u` (emuladores). A prova de "primeiro
  toque funciona" e a medição da capa 3:4 não foram repetidas no Galaxy S22 Ultra real.
- **D-pad físico.** Testei só `input keyevent` via `adb`, não um D-pad USB/Bluetooth de
  verdade.
- **`console_guia.xml`/`console_teclas_fileira.xml`/`activity_game.xml` (G2).** Não
  toquei, como instruído — são território de outra fatia em paralelo (`emulator-5556`).
  Não conferi se a recomendação do implementador (§5 do `G1-implementacao.md`) está
  correta; só confirmei que esta fatia não mexeu neles.
- **Ajustes avançado com a árvore inteira navegada por toque.** Toquei duas linhas
  (Surround, equalização) como prova de conceito; não percorri as 136 preferências uma a
  uma.
- **Sessão com host real (Vibeshine).** Sem host disponível nesta verificação; não
  reproduzi conectando/queda nem confirmei o comportamento de toque dentro de uma sessão
  de stream ativa (fora do escopo desta fatia de qualquer forma — território F5/G2).
- **`tools/fotos.mjs` desatualizado.** Identifiquei o toque duplo residual no script
  (§1), mas não o corrigi — é tooling de fora do escopo dos arquivos que esta fatia edita,
  e não afeta o veredito (a prova real foi por toque direto, não pelo script).

## 8. Commits desta sessão

```
600a068a Widen the touch target of Ajustes basico segments past their 38dp paint
```

Em cima dos 4 commits do implementador, na mesma branch `fatia/G1-toque`. Sem merge em
`dev`, sem push. Emulador `s22ub` (5554) derrubado ao final desta verificação.
