# F0 — Ver e provar: implementação

- **Branch:** `fatia/F0-fotos`, a partir de `dev` (HEAD `50c10a1a`, sem worktree).
- **Fatia:** F0 de `docs/planos/B-CONSOLE.md` §2.

## O que mudou e por quê

### Moque da sessão — `Game.java`, `ui/MoqueDaStream.java`

`Game.java` ganhou o extra `EXTRA_MOQUE` ("MoqueDeStream"), lido só quando
`BuildConfig.DEBUG`. Três pontos de costura, todos guardados pelo extra:

1. Logo depois de ler os extras do Intent (host/appId/uniqueId), se o moque está
   ligado e algum desses valores está ausente, preenche um placeholder (`host =
   "127.0.0.1"`, `uniqueId = "moque"`, `appName = "Moque"`, `appId = 1`). Isto foi
   necessário porque `appId` default é `StreamConfiguration.INVALID_APP_ID`, que vale
   **0** — o primeiro código que escrevi (`appId = 0` como "válido") caiu exatamente
   no valor que o `if (appId == INVALID_APP_ID) finish();` logo abaixo rejeita, e a
   Activity fechava sozinha em ~1 s sem crash nenhum no logcat. Só descobri isso
   fotografando de verdade (ver seção de comandos).
2. Dentro de `streamContainer.setOnSurfaceAvailable(...)`, se o moque está ligado,
   cria um `MoqueDaStream`, manda pintar a superfície e chama `connectionStarted()`
   em vez de `conn.start(...)` — é o mesmo caminho que uma conexão real percorre ao
   subir (dispensa o spinner, liga `connected=true`, abre a ilha), só que sem tocar
   `MoonBridge`.
3. `onDestroy()` para o `MoqueDaStream` antes do `super.onDestroy()`.

`MoqueDaStream` é uma classe nova e pequena (`app/src/main/java/com/limelight/ui/`):
pinta a `Surface` de preto a cada 500 ms (cobre os 2-3 buffers de uma `SurfaceView`
sem apostar em quantos existem). Reimplementada do zero olhando o que a branch
`arquivo/redesenho-vidro-2026-08` fazia (`MoqueDaStream.java` de lá desenhava um
gradiente animado com grade e mira) — o pedido da F0 é literal ("superfície preta"),
então a versão daqui é mais simples de propósito: nada de gradiente, grade ou
contador de quadro. Nenhum outro código daquela branch foi trazido.

### Moque da casca — `PcView.java`, `AppView.java`

`PcView` lê `MoqueDeCasca` (também só em debug). Em `completeOnCreate()`, antes de
`initializeViews()`, se o extra está ligado, `popularComputadoresDeMentira()` cria
dois `ComputerDetails` (ARTEMIS on-line/emparelhado/192.168.0.10; NOTEBOOK fora do ar,
com MAC pra `updateVidroHeader()` dizer "dormindo" em vez de "nenhum PC") e os injeta
direto em `pcGridAdapter.addComputer(...)` — sem passar pelo `ComputerManagerService`
real (que continua ligado pelo bind existente, sem uso). `startComputerUpdates()`
ganhou uma guarda: com o moque ligado, não inicia o polling de verdade.

`AppView` lê `MoqueDeApps`. Em vez do `bindService` real, `popularAppsDeMentira()`
constrói um `AppGridAdapter` de verdade e seis `NvApp` (Área de trabalho, Steam,
Corrida Noturna, Fortaleza, Vale Silencioso, Estrada Sem Fim — a primeira marcada
`isRunning`). A parte que exigiu mais cuidado foi a capa: em vez de duplicar
`CachedAppAssetLoader`/`AppGridAdapter`, cada capa gerada (bloco 3:4, cor pelo hash do
nome, nome desenhado em cima) é semeada direto no cache de memória do loader
(`new MemoryAssetLoader().populateCache(new CachedAppAssetLoader.LoaderTuple(computer,
app), scaledBitmap)`) **antes** de `addApp()` — como o cache é estático,
`populateImageView` encontra a capa na hora e nunca tenta a rede. Zero mudança em
`grid/assets/*`.

### Manifesto de depuração — `app/src/debug/AndroidManifest.xml` (novo)

Exporta `Game`, `AppView`, `preferences.StreamSettings` e `ProfilesActivity` só no
buildType `debug` (`tools:node="merge"`). Nenhuma das quatro é exportada no manifesto
principal (`PcView` e `AddComputerManually` já eram, não precisaram de nada). Sem
isto, `adb shell am start -n .../AppView` (e as outras três) falhava com
`SecurityException` mesmo em build de depuração — confirmado testando (ver comandos).
Primeira versão do arquivo não compilava: o comentário XML tinha `--ez` dentro, e XML
não aceita hífen duplo em comentário (`SAXParseException`, achado pelo gate).

### `tools/fotos.mjs` (novo)

Evolução de `tools/emu.mjs` — sobe um AVD por nome/porta, reusa se já estiver de pé,
constrói o APK x86_64 de depuração só se faltar, instala, percorre as 8 rotas do
plano e monta uma folha de contato. Documentação de uso completa no cabeçalho do
próprio arquivo. Três bugs que só apareceram rodando de verdade (nenhum visível lendo
o código):

- `spawnSync(EMULATOR, ..., {detached:true})` **trava para sempre** — `spawnSync` é
  sempre síncrono e espera o filho terminar, `detached` não muda isso, e um emulador
  não termina sozinho. Troquei por `spawn()` assíncrono + `.unref()`.
  `tools/emu.mjs` tem o mesmo bug; sinalizei à parte (tarefa `task_9361968c`) em vez
  de consertar por conta própria, por estar fora do escopo desta fatia.
- `cmd /c timeout /t N /nobreak` sob `stdio:'ignore'` sai em ~90 ms em vez de esperar
  — o `timeout.exe` do Windows aborta na hora quando detecta que o stdin não é um
  console de verdade. Troquei todas as esperas por `powershell -Command
  "Start-Sleep -Milliseconds N"`, testado e confirmado que espera mesmo. Mesmo bug
  em `tools/emu.mjs`, também sinalizado à parte (`task_92366920`).
- Girar o `s22u` para paisagem só pega se a Activity da rota anterior já estiver
  morta: `Game` fixa a própria orientação (`setRequestedOrientation`) enquanto está
  em primeiro plano, e isso tem prioridade sobre `settings put system user_rotation`
  — descoberto porque a rota `computadores` (retrato) saía em paisagem mesmo pedindo
  retrato, sempre que a rota anterior era de sessão. `force-stop` agora roda sempre
  **antes** de mexer na rotação, nunca depois.
- Achar a ilha da sessão (`vidroTileKeyboard`) exigiu um plano B: ela recolhe sozinha
  em pílula 6,2 s depois de conectar (`VidroIsland.COLLAPSE_DELAY_MS`), e o tempo até
  a rota `sessao-texto` rodar de ponta a ponta às vezes passa disso. A rota agora
  procura o bloco; se não achar, procura a pílula, toca nela (reabre a ilha) e
  procura de novo.
- A primeira vez que `PcView` roda num AVD recém-instalado, sem `GLRenderer` em
  cache, o `onCreate` sobe uma `GLSurfaceView` e só chama `completeOnCreate()` no
  callback assíncrono dela — a tela fica vazia por um tempo que passava no teste de
  estabilidade do `uiautomator dump` como "assentada". Foi assim que a rota
  `computadores` no `tabs8u` saiu sem os dois PCs de mentira na primeira leva de
  fotos. `aguardarAssentar()` ganhou um piso de 900 ms antes da primeira leitura.
- O detector de crash original (`/FATAL|AndroidRuntime: |...|Caused by/`, copiado de
  `tools/emu.mjs`) deu falso positivo em quase toda rota: `force-stop` do processo
  anterior grava `AndroidRuntime: Shutting down VM` no logcat (linha benigna), e o
  logcat lido não é filtrado por processo, então `Caused by` de qualquer serviço do
  sistema também batia. Apertado para `/FATAL EXCEPTION|ClassCastException|
  InflateException|Fatal signal/`.

O montador da folha de contato é um codificador de PNG mínimo escrito no próprio
arquivo (decode/encode 8-bit RGB/RGBA sem interlace, os 5 filtros de linha do PNG,
redimensionamento por vizinho mais próximo, uma fonte de bloco desenhada à mão) —
usa só `node:zlib` (inflate/deflate do IDAT), sem dependência nova (não há
`package.json` no repositório). `node tools/fotos.mjs --selftest` roda dois
autotestes sem emulador: ida-e-volta de uma imagem sintética pelos dois lados do
codificador, e os 5 filtros de linha reconstruídos à mão (fora do meu próprio
codificador) contra o valor original — sem isto o segundo bug (filtros nunca usados
pelo meu próprio encoder, que só escreve filtro None) teria passado batido.

### `docs/adr/0003-stack-da-interface.md` (novo) e `docs/REPLANEJAMENTO.md`

ADR curto: Java + XML, Compose avaliado e recusado, motivos na tabela do
`B-CONSOLE.md` §0. `REPLANEJAMENTO.md` §3 trocou o parágrafo "Por que Jetpack
Compose" (que citava uma "ADR 0004" nunca escrita) por "Por que Java + XML"; §4
trocou a menção a Roborazzi por `tools/screens.mjs`; §7 foi reescrito pra seguir a
ordem real do `B-CONSOLE.md` (F0 → F1 → F2‖F3 → F4 → F5 → F6) em vez do rascunho
pré-replanejamento.

### `config.ini` dos AVDs (fora do repositório)

`hw.ramSize` de `s22u.avd` e `tabs8u.avd` reduzido de 3072 para 2048 MB, conforme
pedido. Isto vive em `%USERPROFILE%\.android\avd\`, fora do controle de versão —
nenhum commit corresponde a esta mudança.

## Comandos executados

```
node tools/fotos.mjs --selftest
```
Saída: `autoteste dos 5 filtros de linha do PNG: OK` /
`autoteste do PNG: OK (37x29 ida e volta, folha 1120x314)`.

```
node tools/validate.mjs
```
Rodado várias vezes durante o trabalho (falhou 2x por motivos já corrigidos: mapas
desatualizados, depois `SAXParseException` no manifesto de depuração). **Última
execução, gate completo:** `codemap` OK, `jni` OK, `build` OK, `test` OK — "4
falha(s) preexistente(s), nenhuma nova" (mesma baseline de antes desta fatia,
`tools/test-baseline.json`). Saída final: `validate: passou.`

```
node tools/fotos.mjs --avd s22u
node tools/fotos.mjs --avd tabs8u --port 5556
```
Cada um rodado por último 5 vezes ao todo durante o trabalho, à medida que os bugs
acima apareciam e eram corrigidos. **As execuções finais** (depois de todos os
consertos, sem interferência manual no meio) terminaram `8/8 rota(s) capturada(s)`
para os dois AVDs, código de saída 0.

- Porta do `s22u`: 5554 (padrão da fatia). Porta do `tabs8u`: 5556 — precisei de uma
  porta diferente porque rodei as duas fotos na mesma máquina, ao mesmo tempo em que
  o `s22u` continuava de pé; o plano reserva 5554 para F0 e 5556 para quem rodar em
  paralelo (F3), e usei a mesma convenção só para não derrubar o `s22u` no meio da
  investigação. Isto não afeta quem rodar só uma fatia por vez com a porta padrão.

## Onde estão as folhas de contato

- `app/build/fotos/s22u/folha.png` (+ os 8 PNGs individuais na mesma pasta)
- `app/build/fotos/tabs8u/folha.png` (+ os 8 PNGs individuais na mesma pasta)

(`app/build/` é gerado — não entra no commit. As imagens ficam na máquina onde este
relatório foi escrito; rodar os dois comandos acima as reproduz.)

Conferi visualmente as 16 fotos (8 por AVD). Pontos que valem registrar:

- `apps` (as duas plataformas): a resposta "Área de trabalho is open / Pick up where
  you left off / Return" apareceu sozinha, sem eu ter pedido nada além de
  `isRunning=true` no primeiro app — é comportamento existente de
  `updateVidroCatalogHeader()`, prova de que reusar `AppGridAdapter` de verdade (em
  vez de simular) também exercita esse caminho.
- `sessao-texto` no `tabs8u`: o teclado virtual aparece mesmo com `hw.keyboard=yes`
  no `config.ini` do AVD (que deveria emular um teclado físico e suprimir o IME). O
  `REPLANEJAMENTO.md` já registra isto como limitação conhecida do emulador ("não tem
  ... o teclado da capa do tablet") — não é defeito deste código, é o emulador não
  replicando o comportamento de supressão de IME de um teclado físico de verdade.
  Fica para a verificação em aparelho (fora do escopo da F0).
- A primeira vez que `Game` abre em modo de moque num AVD, o Android mostra uma
  única vez o aviso de sistema "Viewing full screen / To exit, swipe down from the
  top" (tela cheia por causa das flags de imersão que `Game` já usa hoje, não é nada
  que esta fatia introduziu). Ele não reapareceu nas fotos automatizadas porque cada
  rota de sessão já tinha sido aberta manualmente por mim durante a investigação
  antes da leva final de fotos — mas numa máquina/AVD totalmente virgem ele
  provavelmente aparece na primeira foto de sessão. Não tentei suprimir: é
  comportamento do sistema, não do app, e a ilha/guia por baixo continuam corretas
  (conferido nas fotos manuais que tirei durante a depuração).

## O que ficou por verificar, e por quê

- **Aparelho de verdade.** Nada disto foi instalado em um S22 Ultra ou Tab S8 Ultra —
  fora do escopo da F0 (`docs/planos/B-CONSOLE.md` §3, "depois das fatias"). Em
  particular, a supressão de teclado virtual por teclado físico só se prova lá.
- **`--serial` para aparelho por Wi-Fi.** `tools/fotos.mjs` só implementa `--avd` +
  `--port`, como a F0 pede. Não implementei `--serial` (mencionado em
  `B-CONSOLE.md` §3 como trabalho futuro) — não fazia parte do critério de pronto
  desta fatia.
- **`tools/emu.mjs`.** Os dois bugs que encontrei nele (spawnSync bloqueante, sleep
  que não dorme) não foram corrigidos — sinalizados como tarefas separadas
  (`task_9361968c`, `task_92366920`) em vez de misturar no escopo desta fatia.
- **`docs/design/console/` e `docs/planos/B-CONSOLE.md` continuam sem commit.**
  Encontrei os dois como untracked no início do trabalho (não fui eu que os criei) e
  não os commitei nesta branch — não fazem parte do que a F0 pede entregar, e
  presumi que o Fable os versiona separadamente. Se isso estiver errado, é uma
  decisão fácil de reverter.

## Dúvidas que decidi sozinho

1. **`appId` de mentira = 1, não 0.** `StreamConfiguration.INVALID_APP_ID` é 0; o
   plano não especifica o valor, escolhi o menor inteiro positivo.
2. **Nomes dos 6 apps de mentira ficaram hardcoded em `AppView.java`**, não em
   `strings.xml`. São dados de fixture de uma ferramenta de depuração
   (`BuildConfig.DEBUG` + extra), nunca aparecem pra um usuário real; entendi que a
   regra 6 do `AGENTS.md` ("strings em strings.xml") mira texto de interface real,
   não dado de teste. Se o entendimento for outro, é uma extração mecânica.
3. **`sessao-menu` usa `KEYCODE_BACK`, não um toque no botão flutuante.** O botão
   flutuante (`floatingMenuButton`) fica invisível por padrão
   (`enableFloatingButton` default `false`); `enableBackMenu` é `true` por padrão e é
   o caminho que já existe hoje para abrir o menu sem depender de nenhum botão
   visível. `B-CONSOLE.md` já documenta esse caminho como válido ("Botão Voltar
   continua abrindo o menu se enableBackMenu").
4. **Porta 5556 para o `tabs8u`** ao rodar os dois AVDs ao mesmo tempo nesta máquina
   (ver seção de comandos) — a fatia pede porta padrão 5554, usei 5556 (a mesma que
   F3 usaria) só para não derrubar o `s22u` durante a investigação. Rodar `tabs8u`
   sozinho com a porta padrão funciona igual.
5. **`MoqueDaStream` bem mais simples que a versão da branch de arquivo** (preto liso
   em vez de gradiente com grade e mira). O texto da F0 pede literalmente "superfície
   preta"; entendi isso como a especificação, não como "faça o mínimo que ainda prove
   algo" — mas se o objetivo era ter as marcas de canto/grade para detectar
   corte/proporção errada, é fácil adicionar de volta.
