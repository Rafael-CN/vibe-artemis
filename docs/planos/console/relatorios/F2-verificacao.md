# F2 — A sessão: guia, aba e barra de texto — verificação

- **Verificador:** agente novo, sem o contexto do implementador; relatório do implementador
  tratado como alegação a conferir.
- **Worktree:** `C:\Users\Rafael\Documents\Projetos\Claude\Artemis-F2`, branch `fatia/F2-sessao`.
- **Porta de emulador usada:** 5554, sequencial (`s22u` depois `tabs8u`, cada um morto com
  `adb emu kill` antes de subir o próximo). Nunca toquei em `Artemis`, `Artemis-F1`.

## Veredito: **APROVADA**

Nenhum defeito bloqueante. Dois achados pequenos, corrigidos nesta branch (ver §5), e um
achado de tooling (falso positivo do `validate.mjs`) reproduzido e caracterizado, não
consertado, conforme pedido.

## 1. Merge de `dev` (F3 → F2)

A F3 tinha sido integrada em `dev` (`33cd0d8f`) enquanto a F2 seguia baseada em `fb0ae7ca`.
Rodei `git diff fb0ae7ca..HEAD` **antes** do merge para isolar o que é da F2 (43 arquivos,
ver §2) e só depois `git merge dev`.

Conflitos, todos resolvidos mantendo as duas metades (arquivos disjuntos, nenhuma colisão de
lógica):

- `app/src/main/res/values/strings.xml` e `values-pt-rBR/strings.xml`: F2 adicionou o bloco
  `console_abrir_menu`/`console_guia_*`/`console_tecla_*`/`console_barra_texto_titulo`; F3
  adicionou `console_add_pc`/`console_estado_*`/`console_secao_*`/etc. Mantive os dois blocos
  lado a lado.
- `docs/maps/{DEPENDENCY_GRAPH,FILE_INDEX,HOTSPOTS,SYMBOLS}.md` e `code-index.json`: gerados.
  Não fiz merge manual — peguei uma versão qualquer para destravar o merge e rodei
  `node tools/codemap/codemap.mjs` depois, que sobrescreveu tudo a partir da árvore já
  mesclada (223 arquivos, 66.733 linhas, 30 pacotes).
- `tools/fotos.mjs`: **auto-merge limpo**, sem conflito — as duas fatias adicionaram rotas em
  blocos de objeto diferentes. Conferi депois que as rotas de F2 (`sessao-guia`,
  `sessao-guia-teclas`, `sessao-guia-fecha`, `sessao-texto`, `sessao-menu`) e as de F3
  (`computadores`, `apps`, `apps-foco`, `inicio`, `inicio-foco`, `inicio-dialogo`) sobreviveram
  as duas — confirmado rodando o script nos dois AVDs (16/16 rotas em cada, ver §5).

Commit do merge: `20b830dd`. Gate rodado depois do merge e verde (§4). Nenhum arquivo de F3
(`PcView.java`, `AppView.java`, `PcGridAdapter.java`, layouts `activity_pc_view.xml`/
`activity_app_view.xml`) foi tocado por mim além do merge automático.

## 2. Diff item a item contra a spec (`B-CONSOLE.md` §F2)

Isolado com `git diff fb0ae7ca..HEAD` antes do merge.

| Item da spec | Resultado |
|---|---|
| Ilha e botão flutuante **removidos**, não escondidos | **OK** — `ui/VidroIsland.java`, `vidro_session_island.xml`, `vidro_island_tile.xml`, `vidro_session_sheet.xml` apagados; `<include>` da ilha e o `floatingMenuButton` fora de `activity_game.xml`; `setupVidroIsland()`/`initFloatingButton()` fora de `Game.java`. Confirmei com `grep -r VidroIsland` — zero ocorrência fora do histórico git. |
| Aba 64×20dp (56×16 celular), centro da borda superior, some com barra de texto/diálogo de conexão | **OK** — `console_aba.xml` usa `@dimen/console_aba_w/h` (os tokens da F1, com o override `values-sw600dp`), `layout_gravity="top\|center_horizontal"`. Confirmado nas fotos dos dois AVDs (aba maior e mais alta no `tabs8u`) e no fluxo manual (aba some quando abro a barra de texto, volta ao fechar). |
| Guia = `android.app.Dialog`, `Gravity.TOP`, sem dim, largura total | **OK** — `GameMenu.showMenu()` cria `new Dialog(themedContext, R.style.ConsoleDialog)`, `clearFlags(FLAG_DIM_BEHIND)`, `lp.gravity = Gravity.TOP`, `lp.width = MATCH_PARENT`. Tema renomeado `VidroDialogPlatform` → `ConsoleDialog` (mesmo parent `android:Theme.Material.Dialog.Alert`). |
| Ordem dos blocos | **OK** — Teclado · Barra de texto · Modo do toque · Teclas especiais · Girar a tela · Estatísticas · Controles na tela, Desconectar/Encerrar no PC fixos à direita. Confirmado no XML (`console_guia.xml`) e nas fotos dos dois AVDs. |
| "Teclas especiais" troca a fileira e volta | **OK** — testado ao vivo com `adb shell input tap` no `tabs8u`: tocar o bloco troca para Back·Esc·Tab·Win·Alt+Tab·Ctrl+Alt+Del sem fechar a guia; tocar "Back" volta à fileira padrão. Também confirmado por D-pad nas fotos (`sessao-guia-teclas.png`, os dois AVDs). |
| Desconectar/Encerrar no PC fixos à direita, destrutivo na cor certa, com confirmação | **OK** — `console_bloco_destrutivo_bg` usa `console_destrutivo_fundo` (12%), texto em `console_destrutivo`; `Game.quit()` mantém o `AlertDialog` de confirmação (Sim/Não) intacto — não foi tocado pelo diff. |
| "Modo do toque" mostra o estado atual no rótulo | **OK, testado ao vivo** — dump de UI antes/depois de tocar o bloco: rótulo vai de "Direct touch" para "Touchpad" e volta. Ver §6. |
| Barra de texto mantém `openComposeBar`/`sendTextBlock`/`closeComposeBar`/`RESIZE` | **OK** — `git diff` mostra zero linha tocada nessas quatro âncoras; o único código novo em `Game.java` para a barra é `refreshComposeBarMetrics()` (cosmético) e o fio das 5 teclas especiais (`wireComposeBarTecla`, que só chama `sendKeys`, já existente). |
| D-pad ←/→ move foco, Enter ativa, Esc/Voltar fecha, foco inicial no primeiro bloco | **OK** — confirmado nas fotos com `KEYCODE_DPAD_RIGHT`×N + `KEYCODE_ENTER` (implementador já documentou o bug de contagem cega corrigido com `navegarAteFoco()`; refiz o mesmo teste com `input tap` direto, que não depende de contar D-pad, e bate). |

## 3. Fronteiras que não podiam ser cruzadas

- **Flags de janela da `Game`, `SurfaceView`, decodificação, entrada:** `git diff fb0ae7ca..HEAD -- app/src/main/java/com/limelight/Game.java` não contém nenhuma linha tocando `getWindow().setFlags`, `getWindow().addFlags`, `SurfaceHolder`, `MoonBridge` ou os handlers de `onTouchEvent`/`onKeyDown` de jogo. As duas ocorrências de `getWindow()` no diff são a remoção do cálculo de bounds do botão flutuante (código deletado, não alterado). `jni-check` reportou fronteira íntegra (34 native/34 símbolos C/21 callbacks) sem mudança — F2 não tocou JNI mesmo.
- **Kotlin/Compose:** nenhum arquivo `.kt` no diff; todo o layout novo é XML.
- **Cores fora dos tokens `console_*`:** `git diff fb0ae7ca..HEAD -- app/src/main/res/values/colors.xml` está **vazio** — F2 não adicionou cor nenhuma, só consumiu os tokens da F1. As duas únicas cores hex literais nos drawables novos (`#267FE0C7` em `console_foco.xml`/`console_capa_foco.xml`) são arquivos **da F1**, não da F2 (o anel de foco a 15%, decisão já documentada em `TOKENS.md` §"Decisões desta fatia"). Achei mais uma cor hex literal (`#FFFFFF` em `console_ic_chevron.xml`), mas esse arquivo é da F3 (`git log --follow` confirma o commit `8fc24d30`, "Add Console drawables needed by the F3 screens") — fora do escopo desta verificação.
- **Strings novas nos dois arquivos certos e em nenhum outro `values-*`:** `git diff fb0ae7ca..HEAD --stat -- 'app/src/main/res/values-*'` mostra só `values-pt-rBR/strings.xml` tocado — nenhum outro idioma. As 16 strings novas de F2 (`console_abrir_menu` … `console_barra_texto_titulo`) existem em `values/strings.xml` e `values-pt-rBR/strings.xml`.

## 4. O bloco Picture-in-picture

Confirmado: **a justificativa do implementador é verdadeira.** Busquei todo uso de
`enterPictureInPictureMode`/`onPictureInPictureRequested`/`onUserLeaveHint` em
`Game.java` — os três caminhos de entrada em PiP são automáticos (o sistema chama ao sair do
app ou pedir PiP via recents), nenhum é uma ação que um botão pudesse simplesmente invocar
sem inventar comportamento novo. O `GameMenu` **antigo** (antes desta fatia, no diff) também
não tinha nenhuma opção de PiP manual. A omissão do bloco na guia está de acordo com a própria
regra da spec ("o que não existir como ação hoje, o bloco não aparece").

## 5. Gate (rodado por conta própria)

`JAVA_HOME=C:\Program Files\Java\jdk-21.0.12`.

```
node tools/validate.mjs
[ OK ] codemap
[ OK ] jni
[ OK ] build
[ OK ] test — 4 falha(s) preexistente(s), nenhuma nova
validate: passou.
```

Rodado três vezes nesta sessão: depois do merge de `dev`, depois de regenerar os mapas, e
depois do commit de correção do comentário (§7) — as três vezes verde, as 4 falhas batem
com `tools/test-baseline.json` (`ProfilesNavigationTest`×2, `SimpleStartupTest`, `StartupTest`).

`node tools/screens.mjs`: 29 telas em `app/build/vidro-screens/`, incluindo
`04-sessao-guia.png`/`05-sessao-barra-de-texto.png` (retrato) e `tab-04-sessao-guia.png`/
`tab-05-sessao-barra-de-texto.png` (paisagem). Único ponto de atenção, **não é defeito**: no
bloco "Modo do toque" o rótulo sai em branco nessas capturas — o `shoot()` do
`VidroScreenshotTest` só infla o layout, não roda `GameMenu.wireGuia()`, que é quem escreve o
texto em runtime (`refreshModoToqueLabel`). Confirmado que o rótulo aparece certo nas fotos
reais do app (`sessao-guia.png` dos dois AVDs, abaixo) — é a mesma limitação de
`shoot` vs. `shootActivity` já documentada em `AGENTS.md`.

### Fotos — `s22u` (16/16 rotas capturadas)

```
node tools/fotos.mjs --avd s22u --port 5554
```

- `sessao.png` — aba em repouso, chevron para baixo, centro da borda superior. **OK.**
- `sessao-guia.png` — guia aberta por `input tap` na aba: Keyboard·Text bar·Direct touch·
  Special keys·Rotate screen·Statisti(cortado, HorizontalScrollView)·[PiP ausente, ok]·
  Disconnect·End on the PC (destrutivo, fundo vermelho 12%). Bate em estrutura com
  `B_Tablet_Sessao_Menu.png`. **OK.**
- `sessao-guia-teclas.png` — D-pad ×3 + Enter em "Special keys": fileira trocou para
  Back(focado)·Esc·Tab·Win·Alt+Tab·Ctrl+Alt+Del em mono. **OK.**
- `sessao-guia-fecha.png` — `KEYCODE_ESCAPE` fecha a guia, aba volta ao repouso. **OK.**
- `sessao-texto.png` — barra de texto aberta com o teclado virtual de pé; o vídeo (superfície
  preta do moque) encolhe acima do teclado — `RESIZE` confirmado visualmente. **OK.**
- `sessao-menu.png` — `KEYCODE_BACK` com `enableBackMenu` abre a guia com foco em "Keyboard".
  **OK.**

### Fotos — `tabs8u` (16/16 rotas capturadas)

```
node tools/fotos.mjs --avd tabs8u --port 5554
```

- `sessao-guia.png` — os 7 blocos (sem PiP) cabem inteiros sem cortar, mais largo que o
  `s22u` — confirma a observação do implementador. **OK.**
- `sessao-guia-teclas.png`, `sessao-guia-fecha.png`, `sessao-menu.png` — mesmo comportamento
  do `s22u`. **OK.**
- `sessao-texto.png` — o teclado virtual **também** sobe no `tabs8u`, apesar de
  `hw.keyboard=yes` no AVD. Limitação conhecida do emulador (já registrada por F0/
  `REPLANEJAMENTO.md`): o Android não suprime o IME automaticamente só por declarar teclado
  físico no `config.ini`. **Não é defeito deste código** — a fileira de teclas especiais e o
  campo aparecem corretos por cima do teclado do emulador.

Emuladores derrubados com `adb -s emulator-5554 emu kill` entre um AVD e outro, e ao final.

## 6. Fluxos ao vivo (`adb shell input`, `tabs8u`)

Objetivo: reproduzir o deadlock que o implementador achou (fechar a guia *depois* de rodar a
ação adiada por `runWithGameFocus`, que trava porque a janela da Activity só recupera foco
depois do Dialog fechar) e confirmar que a correção (`onBlock` fecha o diálogo *antes* de
chamar a ação) segura.

1. **Abrir/fechar por toque na aba/alça, 5 ciclos rápidos** (tap aba → 0,6s → Esc → 0,6s,
   repetido 5×): `mCurrentFocus` sempre volta para a janela da `Game` (nunca fica preso num
   Dialog fantasma), aba sempre volta ao repouso. **Sem deadlock.**
2. **"Modo do toque" (bloco com `withGameFocus=true`, o tipo que travava antes da correção):**
   toquei o bloco, a guia fechou imediatamente (dump de UI confirma `consoleGuiaBarra`
   ausente e `consoleAba` de volta), reabri a guia e o rótulo tinha ido de "Direct touch"
   para "Touchpad" — a ação rodou de verdade, não só a guia fechou. Toquei de novo para
   voltar ao estado original. **Sem deadlock, ação confirmada.**
3. **"Teclas especiais" → tocar um bloco de tecla:** guia fecha, teclas mono presentes tanto
   na guia quanto na barra de texto (`console_teclas_fileira.xml` incluído duas vezes, IDs
   não colidem por viverem em árvores de View diferentes — confirmado, sem crash nem
   exceção de ID duplicado).
4. **Barra de texto via bloco "Text bar":** guia fecha, `vidroComposeBar` aparece
   (`bounds="[0,1083][2960,1848]"` no dump), `KEYCODE_BACK` fecha a barra e devolve a aba —
   mecânica de fechar com Voltar preservada, não é código desta fatia mas não quebrou.

Nenhuma trava, nenhum ANR, nenhum estado inconsistente em ~15 minutos de uso manual
intercalando os fluxos acima.

## 7. Defeitos encontrados e correções aplicadas

Pequenos, corrigidos nesta branch (commit `7e216c7a`):

- **Comentário desatualizado em `styles.xml`:** o bloco `ConsoleDialog` ainda dizia "O
  GameMenu usa `android.app.AlertDialog`", uma frase que descrevia o menu **antigo** (antes
  desta fatia). Corrigido para descrever o `android.app.Dialog` de verdade que a guia usa
  hoje. Gate rodado de novo depois da correção, continua verde.

Achados que **não** exigem correção de código, mas registro porque afetam a confiança no
relatório do implementador ou no escopo da fatia:

- **Relatório impreciso sobre `GameMenu.PREF_NAME`/`KEY_NAME`.** O relatório da F2 diz "nada
  mais lê essa preferência agora" (parágrafo sobre "Teclas especiais" perder a importação de
  atalhos). Isso é **falso**: `KeyBoardController.java:466` e
  `KeyBoardControllerConfigurationLoader.java:484` leem a mesma
  `SharedPreferences("specialPrefs")`/`"special_key"` para montar teclas customizadas no
  controle virtual — um caminho completamente independente do menu antigo. A remoção em si
  não quebrou nada (os campos `PREF_NAME`/`KEY_NAME` continuam públicos em `GameMenu.java`,
  intocados), mas a alegação de "órfão" está errada — é uma imprecisão de relatório, não um
  bug de código.
- **Perda de descoberta não sinalizada: "alternar emulação de mouse" do gamepad.**
  `ControllerHandler.getGameMenuOptions()` ainda devolve essa opção, mas a guia nova não lê
  `GameInputDevice.getGameMenuOptions()` em lugar nenhum (só o comentário em `GameMenu.java`
  registra por que o tipo `MenuOption` foi mantido). Diferente do bitrate/ritmo de quadros —
  que o implementador **sinalizou explicitamente** como perda —, esta não foi mencionada.
  Verifiquei que a ação continua alcançável por fora do menu (segurar o botão Start/Menu do
  controle, `ControllerHandler.java:2513`), então não é perda total de funcionalidade, só de
  descoberta pela UI — severidade baixa, mas o Fable deveria saber que existe mais uma opção
  "curada para fora" além das já documentadas.

## 8. O que continua sem verificação possível

Repito e confirmo as ressalvas do próprio implementador, porque continuam válidas depois
desta verificação:

- **Aparência sobre vídeo real, em aparelho.** Tudo aqui — minhas fotos incluídas — foi
  contra o moque de stream (superfície preta) nos dois AVDs. A visibilidade do `Dialog` sobre
  `SurfaceView` com vídeo *vivo* decodificando de um host de verdade não foi, e não pode ser,
  provada por este processo. É o primeiro alvo declarado em `B-CONSOLE.md` §3.
- **Teclado físico de verdade / supressão de IME.** O `tabs8u` mostrou o teclado virtual
  mesmo com `hw.keyboard=yes` — comportamento do emulador, não do app; só um Tab S8 Ultra
  real com a capa-teclado prova a supressão.
- **Tela externa (`dialogScreenContext != game`).** "Girar a tela" e "Controles na tela"
  escondidos nesse caminho não foram exercidos — nem por mim, nem (pelo relatório) pelo
  implementador.
- **`ic_menu.xml`/`menu_button` órfãos.** Confirmei por `grep` que nada em `res/` referencia
  mais `ic_menu.xml` (removido) nem usa `menu_button` fora das traduções preservadas; não há
  como garantir por análise estática que nenhum `getIdentifier()` em runtime resolvia esse
  nome — mesma ressalva do implementador, não fechada por mim.

## Achado de tooling: o falso positivo do `validate.mjs`, reproduzido

O implementador relatou ter visto `validate.mjs` dizer "`[ OK ] test — 0 falha(s)
preexistente(s)`" num estado que na verdade não compilava. Reproduzi de propósito:

1. Adicionei uma linha que não compila em `VidroScreenshotTest.java`
   (`private static final int X = SIMBOLO_QUE_NAO_EXISTE;`).
2. Regenerei `docs/maps/` para não travar no estágio `codemap` antes de chegar em `test`.
3. Rodei `node tools/validate.mjs`. Saída: `[ OK ] build` (correto — o estágio `build`
   compila só `main`, não `test`) seguido de **`[ OK ] test — 4 falha(s) preexistente(s),
   nenhuma nova`** — o mesmo número de falhas conhecidas de sempre, como se a suíte tivesse
   rodado normalmente.
4. Rodei `./gradlew.bat :app:testNonRoot_gameDebugUnitTest --console=plain` **diretamente**:
   `FAILURE: Build failed` — `:app:compileNonRoot_gameDebugUnitTestJavaWithJavac FAILED`,
   erro de compilação real, nenhum teste chegou a rodar.
5. Reverti a linha quebrada e regenerei `docs/maps/` de volta ao estado correto.

**Causa raiz, lida em `tools/validate.mjs`:** o estágio `test` decide passar/falhar por
`r.status === 0` como qualquer outro estágio (linha `const ok = r.status === 0`), e nesse
caso `r.status` **era** não-zero (o Gradle falhou de verdade). Mas o tratamento especial do
estágio `test` (linhas ~182-196) — pensado para separar falha nova de falha antiga via os XML
em `app/build/test-results/testNonRoot_gameDebugUnitTest/` — entra em ação sempre que
`ok === false` e `s.name === 'test'`, **sem checar se o Gradle sequer chegou a escrever XML
novo nesta execução**. Como a compilação falhou antes de qualquer teste rodar, os arquivos
XML lidos por `classifyTestFailures()` são os **da execução anterior, bem-sucedida** — que
por acaso tinham exatamente as 4 falhas da baseline e nenhuma outra. O resultado: "0 falha(s)
NOVA(S)" é literalmente verdade sobre um diretório de resultados que não tem nada a ver
com a tentativa atual.

**Para reproduzir:** quebrar a compilação de qualquer arquivo em `app/src/test/java/`
(símbolo inexistente, import faltando) **sem** quebrar `app/src/main/`, garantir que
`app/build/test-results/testNonRoot_gameDebugUnitTest/` tenha XML de uma execução anterior
bem-sucedida, e rodar `node tools/validate.mjs`. O estágio `build` passa (não compila teste),
o estágio `test` "passa" com a contagem de falhas da execução anterior, apesar de a suíte
atual não compilar.

**Não consertei** — só caracterizei, como pedido. Uma correção honesta precisaria checar se
o timestamp dos XML é posterior ao início desta execução do Gradle (ou, mais simples, apagar
`app/build/test-results/` antes de rodar `test` e tratar "diretório ausente depois do
Gradle rodar" como falha do estágio, não como "zero falhas").

## Arquivos relevantes

- `docs/planos/console/relatorios/F2-verificacao.md` (este arquivo)
- `app/src/main/res/values/styles.xml` (comentário corrigido, commit `7e216c7a`)
- Merge: commit `20b830dd`
- Fotos desta verificação (geradas nesta sessão, não commitadas — `app/build/` é gerado):
  `app/build/fotos/s22u/*.png`, `app/build/fotos/tabs8u/*.png`
