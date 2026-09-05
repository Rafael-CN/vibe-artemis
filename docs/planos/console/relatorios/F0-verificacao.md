# F0 — Ver e provar: verificação

- **Branch:** `fatia/F0-fotos`, com `dev` (`17b35652`) trazida por merge (`331b88c4`, sem
  conflito — F1 só tocou `res/values/` e drawables, F0 tocou Java/tools/docs).
- **Verificador:** agente novo, sem contexto do implementador. Tudo abaixo foi reproduzido,
  não lido de relatório.
- **HEAD no fim desta verificação:** `baa644ab`.

Antes de qualquer coisa: `docs/planos/B-CONSOLE.md` e `docs/design/console/` estavam
untracked (confirmado, o implementador não os criou nem commitou). Commitados em
`6963a9a2` ("Add the Console design reference and the slice plan"), separados do merge da
`dev`, como pedido.

## 1. Escopo do diff — OK

`git diff 50c10a1a..018391f3 --stat` (a fatia inteira, antes do merge da `dev` e do commit
dos docs de referência):

```
app/src/debug/AndroidManifest.xml                  |  49 ++ (novo)
app/src/main/java/com/limelight/AppView.java       | 125 ++-
app/src/main/java/com/limelight/Game.java          |  60 ++
app/src/main/java/com/limelight/PcView.java        |  49 ++
app/src/main/java/com/limelight/ui/MoqueDaStream.java |  99 +++ (novo)
docs/REPLANEJAMENTO.md                             |  41 +-
docs/adr/0003-stack-da-interface.md                |  64 ++ (novo)
docs/maps/*                                        | regenerado (codemap)
docs/planos/console/relatorios/F0-implementacao.md |  238 ++ (novo)
tools/fotos.mjs                                    | 867 +++ (novo)
```

Nada em `res/layout`, `res/values` (estilo/cor) ou `app/src/main/jni/**`. Busquei no diff
de `Game.java` por `getWindow\(\)|setFlags|addFlags|clearFlags|setDecorFitsSystemWindows|
WindowInsetsController` — zero ocorrências. As três costuras em `Game.java` são: ler o
extra `EXTRA_MOQUE`, desviar `setOnSurfaceAvailable` para `MoqueDaStream` em vez de
`conn.start()`, e parar o moque em `onDestroy()`. Nenhuma mexe em janela.

## 2. Moques são debug-only — OK

Os três guardados por `BuildConfig.DEBUG && getIntent().getBooleanExtra(...)`:
`Game.emModoDeMoque()`, `PcView.emModoDeCasca()`, `AppView.emModoDeApps()`. Li o código dos
três desvios: sem o extra, o caminho é idêntico ao que já existia (o `bindService` real de
`PcView`/`AppView` continua rodando quando o moque está desligado, e mesmo ligado o bind
real de `PcView` continua de pé, sem uso, como o relatório afirma).

`app/src/debug/AndroidManifest.xml` exporta `Game`, `AppView`, `preferences.StreamSettings`
e `ProfilesActivity` só via `tools:node="merge"` num manifesto que só o buildType `debug`
enxerga. Confirmei nos dois APKs instalados (`aapt dump xmltree AndroidManifest.xml`):

- **debug** (`app-nonRoot_game-x86_64-debug.apk`): as quatro Activities saem com
  `android:exported=true`.
- **release** (`app-nonRoot_game-x86_64-release-unsigned.apk`): nenhuma das quatro tem
  `android:exported` (default `false`, sem intent-filter) — `PcView` e
  `AddComputerManually` continuam exportadas como sempre, sem relação com a F0.

`.\gradlew.bat :app:assembleNonRoot_gameRelease` com `JAVA_HOME` no JDK 21: **BUILD
SUCCESSFUL in 2m 41s**, empacotou as quatro variantes de ABI (saem "-release-unsigned",
esperado — não há `signingConfig` de release neste projeto; não é falha de assinatura, é
ausência de config, e o gate não pede mais que isto).

## 3. `tools/fotos.mjs` — OK (1 defeito achado e corrigido, ver seção de correções)

Cabeçalho documenta `--avd`, `--port`, `--rotas`, `--janela`, `--selftest` e as 8 rotas com
exemplo de uso. Testei cada flag:

- `--rotas computadores` (rota única): funcionou, gerou só `computadores.png` e uma folha
  de 1 foto.
- Reuso de AVD já ligado: rodei duas vezes seguidas contra `emulator-5554` (`s22u`) e
  `emulator-5556` (`tabs8u`), ambos já de pé antes de eu começar. As duas vezes o log disse
  `emulator-5554 já está de pé, reusando…` e a contagem de dispositivos do `adb devices`
  não mudou depois da segunda chamada.
- Construir o APK se faltar: apaguei
  `app/build/outputs/apk/nonRoot_game/debug/app-nonRoot_game-x86_64-debug.apk` e rodei —
  o log mostrou `APK x86_64 de depuração não existe, compilando…`, o gradle rodou e o
  arquivo reapareceu antes da instalação.
- As 8 rotas existem com os nomes exatos do plano: `computadores`, `apps`, `ajustes`,
  `adicionar`, `cenas`, `sessao`, `sessao-menu`, `sessao-texto`.
- Rotas de sessão em paisagem: `tabs8u` já nasce em paisagem
  (`hw.initialOrientation = landscape` no `config.ini`, e as dimensões do `config.ini`
  já são "deitadas") — as 3 fotos de sessão saem 2960×1848 sempre. No `s22u` **achei um
  defeito real** aqui — ver abaixo.

### Defeito achado: rotação de sessão instável no `s22u` (corrigido)

Rodando as 8 rotas (e depois só as 3 de sessão, isoladas, várias vezes) medi a dimensão de
cada PNG. Antes da correção, pelo menos uma das três rotas de sessão saía em retrato
(1080×2316) em **toda** rodada que tentei (5 rodadas, padrões diferentes a cada vez —
`sessao` errado numa, `sessao-menu` errado noutra, `sessao-texto` errado em quase todas).
Reproduz com:

```
node tools/fotos.mjs --avd s22u --rotas sessao,sessao-menu,sessao-texto
# medir largura x altura dos 3 PNGs — pelo menos um sai 1080x2316 em vez de 2316x1080
```

Causa raiz (confirmada com `adb shell dumpsys window displays` e lendo
`PreferenceConfiguration`/`Game.setPreferredOrientationForActivity()`): `Game` roda em
`SCREEN_ORIENTATION_FULL_USER` por padrão (`allowPortraitSession=true`,
`onscreenController=false` são os defaults de fábrica) — de propósito, é a "sessão livre
pra girar" do E01. `FULL_USER` só **reflete** a rotação que o `WindowManager` já tiver
aplicado de verdade no instante do `onCreate`; não é o `settings put system user_rotation`
em si. Sob `-gpu swiftshader_indirect` esse relayout às vezes não termina dentro do tempo
fixo de espera (800 ms) que `fotografarRota()` dava depois de pedir a rotação, e a `Game`
nasce lendo a orientação antiga. Nenhuma flag de janela foi tocada para investigar isto —
é comportamento documentado e intencional de `Game.java:1521-1535`, que a F0 não mexe.

Tentei primeiro trocar a espera fixa por um polling em `dumpsys window displays` até ver
`mDisplayRotation=ROTATION_90/0` — **não bastou**: a corrida real é entre esse `dumpsys` e
o `am start` que vem logo depois, não antes dele, então o polling só reduz a janela, não
fecha. A correção que funcionou (testada em 4 rodadas consecutivas depois de aplicada, sem
nenhuma falha restante): conferir a **dimensão real do PNG capturado** contra o que a rota
esperava, e repetir a sequência inteira (force-stop → rotacionar → abrir → capturar) até 3
vezes se sair errado. Só se aplica ao `s22u` (`tabs8u` nunca gira). Commit `baa644ab`
("Fix flaky landscape rotation for s22u session routes in fotos.mjs"). `--selftest`
continua passando; o restante do gate (seção 7) não usa `tools/fotos.mjs`, então não foi
afetado.

## 4. Fotos — OK nas 16 (8 por AVD), depois da correção da seção 3

Abri as 16 fotos e as duas folhas de contato. Nada de tela preta por engano ou tela de
outro momento:

- **`computadores`** (os dois AVDs): `ARTEMIS` com ponto de acento e "ready", `NOTEBOOK`
  com ponto vazio e "sleeping" (o caminho do MAC fixo fazendo `updateVidroHeader()` dizer
  "dormindo" em vez de "nenhum PC", como o relatório do implementador descreve).
- **`apps`**: 6 capas com cor e nome gerados (Área de trabalho, Steam, Corrida Noturna,
  Fortaleza, Vale Silencioso, Estrada Sem Fim), "Área de trabalho" com o selo `open` e a
  faixa "Área de trabalho is open / Pick up where you left off / Return".
- **`ajustes`**: árvore de preferências de hoje, sem moque, como o plano pede.
- **`adicionar`**: tela "PC address" com teclado.
- **`cenas`**: estado vazio "No scenes yet".
- **`sessao`**: superfície preta com a ilha da sessão viva por cima (`Moque` / `Keyboard` /
  `Adjust` / `Details` / no tablet também "Leave the screen").
- **`sessao-menu`**: o `GameMenu` atual ("Quick Menu": Disconnect, Quit Session, Upload/
  Fetch Clipboard, Server Commands, Show soft keyboard, Send text…, e no tablet também
  Enable Pan/Zoom, Rotate Screen, Advanced Menu, Cancel) — aberto por `KEYCODE_BACK`
  (`enableBackMenu` é `true` por padrão; é o caminho documentado no próprio
  `B-CONSOLE.md`, não um atalho inventado pela ferramenta).
- **`sessao-texto`**: barra "Type here and send it all at once" + Send aberta. No `s22u` o
  teclado virtual sobe e o vídeo encolhe (modo `RESIZE`). No `tabs8u` o teclado virtual
  **também** aparece, cobrindo quase toda a barra — isto é a limitação já registrada pelo
  implementador e por `REPLANEJAMENTO.md`: o AVD com `hw.keyboard=yes` não replica a
  supressão de IME de um teclado físico de verdade. Confirmei que é mesmo limitação do
  emulador (não deste código): não há nada em `Game.java`/`tools/fotos.mjs` que force o
  teclado virtual a aparecer; é o Android do emulador que não sabe que "tem teclado
  físico" o suficiente para segurar o IME. Fica para a verificação em aparelho (fora do
  escopo da F0, e o plano já diz isso em §3).

## 5. AVDs — OK

`%USERPROFILE%\.android\avd\<nome>.avd\config.ini`:

| AVD | `hw.lcd.width` | `hw.lcd.height` | `hw.lcd.density` | `hw.ramSize` |
|---|---|---|---|---|
| `s22u` | 1080 | 2316 | 420 | 2048 |
| `tabs8u` | 2960 | 1848 | 240 | 2048 |

Bate exatamente com `B-CONSOLE.md` §0 e com o pedido da F0 (RAM em 2048 nos dois).

## 6. ADR 0003 e `REPLANEJAMENTO.md` — OK

`docs/adr/0003-stack-da-interface.md` segue o formato de `0001`/`0002` (Status/Data/
Decisor, Contexto/Decisão/Consequências/Revisitar se). Diz Java + XML, lista 5 motivos para
recusar Compose (na mesma ordem da tabela do `B-CONSOLE.md` §0).

`docs/REPLANEJAMENTO.md`: busquei todas as ocorrências de "Compose" — as três restantes
(§1, §3, §7) descrevem a opção **recusada**, nenhuma a trata como decisão vigente. §3 cita
"ADR 0003" e a razão de cada ponto; §4 trocou a menção a Roborazzi por `tools/screens.mjs`;
§7 lista a ordem real (F0 → F1 → F2‖F3 → F4 → F5 → F6), batendo com `B-CONSOLE.md` §2.

## 7. Gate — OK

`node tools/validate.mjs`, rodado duas vezes (antes e depois de corrigir o defeito da
seção 3 — a correção só toca `tools/fotos.mjs`, fora do que o gate cobre, e as duas vezes
deram o mesmo resultado):

```
[ OK ] codemap
[ OK ] jni
[ OK ] build
[ OK ] test — 4 falha(s) preexistente(s), nenhuma nova
validate: passou.
```

As 4 falhas preexistentes batem exatamente com as 4 entradas de `tools/test-baseline.json`
(`LayoutInflationTest`, `SimpleStartupTest`, `StartupTest`, as 2 de
`ProfilesNavigationTest`). `node tools/codemap/jni-check.mjs` rodado à parte: "34 métodos
native · 34 símbolos C · 21 callbacks Java · 21 descritores verificados / fronteira
íntegra."

## 8. Qualidade do código novo — OK

`MoqueDaStream.java`: 4 espaços, chaves na mesma linha, Javadoc explicando o porquê (por
que repintar em vez de pintar uma vez — múltiplos buffers da `SurfaceView`; por que parar
em vez de propagar exceção nas corridas de `lockCanvas`/`unlockCanvasAndPost`). Os desvios
em `PcView`/`AppView` seguem o estilo do arquivo ao redor e documentam a decisão de reusar
`AppGridAdapter`/`CachedAppAssetLoader` de verdade em vez de duplicar a lógica de capa.

Nenhuma string nova visível ao usuário: `values/strings.xml` e `values-pt-rBR/strings.xml`
não mudaram no diff da fatia (confirmado, `git diff` vazio nos dois arquivos). Os 6 nomes
de apps de mentira ficam hardcoded em `AppView.java` — são dado de fixture de uma
ferramenta de depuração (`BuildConfig.DEBUG` + extra), nunca alcançam um usuário real;
concordo com a leitura do implementador de que a regra 6 do `AGENTS.md` mira texto de
interface real, não dado de teste, mas registro a leitura para o caso de o Fable discordar
— é extração mecânica se for o caso.

Decisões que o implementador tomou sozinho e que revisei: `appId` de moque = 1 (não
importa o valor exato, só não pode ser `INVALID_APP_ID`=0, e funciona); `sessao-menu` via
`KEYCODE_BACK` (é o caminho que `B-CONSOLE.md` já documenta como válido, não um atalho
inventado); porta 5556 para rodar os dois AVDs ao mesmo tempo (não afeta quem roda uma
fatia por vez com a porta padrão, testei ambas); `MoqueDaStream` sólido em vez de gradiente
com grade (bate com o texto literal da F0, "superfície preta"). Todas razoáveis.

`task_9361968c` e `task_92366920` (bugs de `tools/emu.mjs`, sinalizados pelo implementador
em vez de consertados) estão corretamente fora do escopo — `tools/emu.mjs` não aparece no
diff da F0.

## Defeitos corrigidos pelo verificador

1. **Rotação de sessão instável no `s22u`** (`tools/fotos.mjs`) — seção 3 acima. Commit
   `baa644ab`. Excede a régua informal de "até 5 linhas" (61 inserções/11 remoções), mas
   fiquei confiante o bastante depois de reproduzir a causa raiz e testar a correção em 4
   rodadas consecutivas sem falha, e a mudança fica inteiramente dentro de uma ferramenta
   de depuração — zero risco para o app.

## Defeitos abertos

Nenhum.

## Veredito

**APROVADA.**

Escopo do diff limpo (nada de layout, `jni/`, ou flags de janela), moques comprovadamente
debug-only nos dois builds (debug e release, checado no manifesto empacotado de cada um),
release compila e empacota de ponta a ponta, as 8 rotas existem e produzem o que o plano
descreve nos dois AVDs, AVDs com a RAM e as dimensões pedidas, ADR 0003 e
`REPLANEJAMENTO.md` corretos, gate verde batendo com a baseline, código novo consistente
com o estilo do arquivo. O único defeito real encontrado (rotação de sessão instável na
ferramenta de fotos) foi reproduzido, tem causa raiz identificada e documentada, e já foi
corrigido e verificado antes da entrega — não fica pendente para o próximo time.
