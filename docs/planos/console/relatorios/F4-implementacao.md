# F4 — Ajustes básicos + Avançado — implementação

Fatia: `docs/planos/B-CONSOLE.md` §F4. Branch: `fatia/F4-ajustes`, a partir de `dev`
(commit `ea11e09a`, com F0, F1, F2 e F3 já integradas). Worktree:
`C:\Users\Rafael\Documents\Projetos\Claude\Artemis-F4`. Porta de emulador: 5554.

## O que mudou e por quê

### A nova preferência — `checkbox_compose_bar`

Seguindo `.claude/rules/prefs.md` (o `docs/howto/adicionar-preferencia.md` citado nas
instruções da tarefa **não existe neste worktree, nem no repositório principal, nem em
nenhum outro worktree `Artemis-F*` já verificado** — usei a regra de quatro passos do
arquivo de regras carregado automaticamente ao tocar `preferences.xml`, que cobre o
mesmo terreno):

1. **Chave e widget** em `res/xml/preferences.xml` — `CheckBoxPreference` nova, logo
   depois de `checkbox_enable_commit_text` (mesma vizinhança temática).
2. **Constante e default** em `PreferenceConfiguration.java` —
   `CHECKBOX_COMPOSE_BAR` (package-private, não `private`, porque `StreamSettingsBasico`
   precisa dela) + `DEFAULT_COMPOSE_BAR = true`.
3. **Leitura** em `readPreferences()` — novo campo público `composeBarEnabled`.
4. **Perfil** — não exige código: `SettingsProfile` não tem uma lista de chaves
   conhecidas, um perfil novo é `getAll()` das prefs globais (ver
   `docs/reference/preferencias.md`), então a chave nova já viaja com qualquer perfil
   criado depois que ela existir.

Strings novas (`title_compose_bar`/`summary_compose_bar`) em `values/strings.xml` e
`values-pt-rBR/strings.xml`.

**Fora do escopo desta fatia**: o bloco "Teclado" da guia da sessão (`GameMenu`, F2)
ainda não lê `composeBarEnabled` — a fatia só entrega a preferência (persistida,
com switch na tela básica); ligá-la ao comportamento real da guia é território de F2,
explicitamente fora do escopo desta tarefa.

### A tela nova — `StreamSettingsBasico`

Arquivos:
- `app/src/main/java/com/limelight/preferences/StreamSettingsBasico.java` (novo)
- `app/src/main/res/layout/console_ajustes_basico_conteudo.xml` (novo, conteúdo
  compartilhado)
- `app/src/main/res/layout/activity_stream_settings_basico.xml` (novo, moldura celular)
- `app/src/main/res/layout-sw600dp/activity_stream_settings_basico.xml` (novo, coluna
  centrada de 720dp no tablet)
- `app/src/main/res/drawable/console_segmento_bg.xml`,
  `app/src/main/res/color/console_segmento_texto.xml` (novos)
- `app/src/main/res/values/bools.xml`, `values-sw600dp/bools.xml` (novos — legenda
  curta/longa do card Avançado por tamanho de tela)
- `app/src/main/res/values/styles.xml` — `ConsoleDivisor`, `ConsoleSlider`
- `app/src/main/AndroidManifest.xml` — declaração da Activity

**Decisão estrutural**: `StreamSettings` (a Activity de sempre, com o
`PreferenceFragmentCompat` sobre `preferences.xml` inteiro) **não foi tocada**.
`StreamSettingsBasico` é uma Activity nova, irmã, que é quem `PcView` agora abre; o
card "Avançado" dela é que abre `StreamSettings` via `Intent`. Isso preserva
`EditProfileActivity` (que reusa `StreamSettings.SettingsFragment` diretamente para
editar perfis) sem qualquer mudança, e garante a regra do dono do fork — a árvore de
136 preferências continua inteira, alcançável e intocada, só que atrás de um toque a
mais.

**Persistência**: `StreamSettingsBasico` lê e grava direto em
`PreferenceManager.getDefaultSharedPreferences(this)` — o mesmo arquivo que
`StreamSettings.SettingsFragment#getPrefs()` já usa (não o overlay de perfil de
`ProfilesManager`). Isso é o comportamento que a árvore de hoje já tinha (a UI de
ajustes nunca respeitou o perfil ativo) e garante que a tela básica e a Avançado nunca
discordem sobre o valor atual de uma chave.

**Linhas segmentadas** (Resolução ↔ `list_resolution`, Taxa ↔ `list_fps`, Codec ↔
`video_format`, Modo do toque ↔ `mouse_mode_list`): os `TextView` de cada opção são
**estáticos, com id fixo no XML**, não inflados dinamicamente. Uma primeira
implementação inflava um item por opção em runtime dentro de um `LinearLayout`
`wrap_content` aninhado em outro `wrap_content` com peso — sob o Robolectric em modo
de gráficos nativo (`tools/screens.mjs`), depois de um `removeAllViews()` seguido de
`addView()` reintroduzindo os itens, **só o primeiro item media largura diferente de
zero**, mesmo com `getChildCount()` correto (3 filhos de verdade, só um visível). Trocar
para Views fixas no layout eliminou o problema por completo e é mais simples de
raciocinar. Registrei o achado no comentário do método `configurarSegmentado()`.

Quando o valor salvo não bate com nenhum dos segmentos oferecidos, nenhum fica marcado
e o container inteiro vira `GONE` em favor de um `TextView` mono com o valor cru — a
regra "não force o valor pro segmento mais próximo" (B-CONSOLE.md §F4), que na prática
**é o estado de qualquer instalação nova**: o default de fábrica é 720p/60Hz, e 720p
não é um dos três segmentos de Resolução (1080p/1440p/Nativa) — então a primeira vez
que alguém abre a tela básica, a linha Resolução já nasce em modo mono. Taxa nasce
com "60 Hz" marcado (60 é um dos três). Estendi a mesma regra defensiva para Codec e
Modo do toque, mesmo a spec só citando Resolução/Taxa explicitamente — um valor
"Forçar H264" (`neverh265`, existe na árvore de hoje) ou um modo de mouse fora dos dois
oferecidos (`Modo do toque` só cobre `mouse_mode_list` 0 e 2, de 6 valores possíveis)
merecia o mesmo tratamento, não um "parece que travou".

**Resolução "Nativa"**: calculada em runtime a partir de `Display.getMode()` (API 23+,
com fallback a `getRealMetrics` abaixo disso) — o mesmo padrão que
`StreamSettings#reloadSettings()` já usa para achar o display ativo — e comparada/gravada
como string `WxH`, igual ao formato de `list_resolution` de sempre.

**HDR**: o switch fica desabilitado com a legenda "Exige jogo com HDR e GPU com HEVC
Main 10" quando o aparelho não suporta — o gate reusa exatamente a checagem que
`StreamSettings` já fazia para esconder a preferência da árvore de hoje (HDR10 via
`Display.HdrCapabilities`, firmware quebrado do Shield) e adiciona a existência de um
decoder HEVC (`MediaCodecHelper.findProbableSafeDecoder`) como proxy de "GPU com HEVC
Main10" — o codebase não expõe checagem de perfil Main10 mais fina que essa.

**Áudio**: toque abre um `AlertDialog` de escolha única (`R.style.ConsoleDialog`) com
os mesmos `audio_config_names`/`audio_config_values` de sempre — não é o mesmo widget
`ListPreference`, porque esta tela não é mais um `PreferenceScreen`, mas é a mesma
fonte de dados e o mesmo efeito.

**Taxa de bits**: `SeekBar` simples (não `SeekBarPreference`), 500–300000 kbps em
passos de 500 (mesmo range de `seekbar_bitrate_kbps` em `preferences.xml`), gravando
só no `onStopTrackingTouch`. **Decisão**: mudar Resolução/Taxa não recalcula o bitrate
default automaticamente (como o fluxo de hoje em `StreamSettings` faz via
`resetBitrateToDefault`) — cada controle grava só a própria chave. A spec não pede o
recálculo cruzado, e replicá-lo silenciosamente mudaria o bitrate do usuário sem ele
tocar no controle de bitrate; achei mais previsível deixar de fora. **Achado colateral**:
como `PreferenceConfiguration.getDefaultBitrate` já cai nesse cálculo quando a chave de
bitrate ainda não tem valor gravado, mudar a Taxa muda o número mostrado no slider até a
pessoa gravar um valor próprio — comportamento herdado do código de hoje, não introduzido
por mim, e documentado aqui para quem for comparar as fotos de perto.

**Cabeçalho — "PC atual, se houver"**: `PcView` (settingsButton) agora abre
`StreamSettingsBasico` em vez de `StreamSettings`; quando a Início do tablet (F3) já
tem um "PC atual" resolvido (`tabletCurrentComputerDetails`), nome e endereço vão como
extras (`EXTRA_PC_NAME`/`EXTRA_PC_ADDRESS`) e aparecem no cabeçalho via
`console_nome_e_estado` ("%1$s · %2$s", string já existente da F3). No celular
(Computadores) não existe essa noção de "atual" — o cabeçalho simplesmente omite o
trecho, como a spec permite. **Divergência conhecida do mockup**: o endereço é
mostrado no mesmo estilo do nome (`ConsoleText.Rotulo2`), não em mono como no
`B_Tablet_Ajustes.png` — decisão de simplicidade (evita `SpannableString` para um
detalhe tipográfico), deixada para o polimento de F6.

### `AndroidManifest.xml`

`StreamSettingsBasico` precisou de `android:exported="true"` — sem isso,
`adb shell am start -n .../.StreamSettingsBasico` falha com
`SecurityException: not exported from uid ...`, mesmo a Activity `StreamSettings`
(igualmente sem `android:exported` declarado) abrindo sem erro pelo mesmo comando. Não
investiguei a fundo a causa da diferença de comportamento entre as duas Activities;
segui o precedente já existente de `AddComputerManually`, que também declara
`exported="true"` neste mesmo manifesto.

### `tools/fotos.mjs`

Três rotas novas: `ajustes-basico` (a tela nova sozinha), `ajustes-avancado` (toca no
card "Avançado" — prova que a árvore inteira abre), `ajustes-persistencia` (mexe em
cada controle, `force-stop` + `am start` de novo, fotografa o que
`SharedPreferences` realmente guardou).

**Achado de plataforma, não deste código**: o primeiro toque já entregue a uma
`TextView` clicável comum não dispara `OnClickListener` nenhum — só o segundo toque no
mesmo lugar dispara. Reproduzi isso manualmente por `adb shell input tap`, sem
`uiautomator dump` no meio (então não é sobre o dump) e sem acontecer no
`SwitchCompat` (então não é "primeiro toque depois que a janela ganha foco" em geral).
A rota `ajustes-persistencia` agora toca cada segmento duas vezes; os switches, uma
só (um segundo toque num switch já alternado o desligaria de novo). Isto ficou
documentado em comentário no próprio `fotos.mjs`.

## Comandos executados e resultado

```
node tools/codemap/codemap.mjs
→ 224 arquivos, 280 tipos, 2649 métodos, 30 pacotes

node tools/validate.mjs
→ [ OK ] codemap
→ [ OK ] jni
→ [ OK ] build
→ [ OK ] test — 4 falha(s) preexistente(s) (tools/test-baseline.json), nenhuma nova
→ validate: passou.
```

```
./gradlew :app:compileNonRoot_gameDebugJavaWithJavac   → BUILD SUCCESSFUL
./gradlew :app:assembleNonRoot_gameDebug                → BUILD SUCCESSFUL
```
(rodados várias vezes durante o trabalho, inclusive depois de cada correção)

```
node tools/screens.mjs
→ 31 telas em app/build/vidro-screens/, sem erro — inclui as duas novas:
  10-ajustes-basico.png (celular, w393dp-h852dp) e
  tab-10-ajustes-basico.png (tablet, w1722dp-h1075dp), via shootActivity
  (Activity de verdade, não layout inflado puro — precisa de onCreate/onResume
  pra ter rótulo, segmento e switch reais).
```

```
node tools/fotos.mjs --avd s22u --port 5554 --rotas ajustes-basico,ajustes-avancado,ajustes-persistencia
→ 3/3 rota(s) capturada(s) em app/build/fotos/s22u

node tools/fotos.mjs --avd tabs8u --port 5554 --rotas ajustes-basico,ajustes-avancado,ajustes-persistencia
→ 3/3 rota(s) capturada(s) em app/build/fotos/tabs8u
```

Um emulador de cada vez, como pedido: `s22u` derrubado (`adb -s emulator-5554 emu
kill`) antes de subir `tabs8u` na mesma porta; `tabs8u` derrubado ao final. Nenhum
emulador ficou de pé.

## Fotos

Todas em `app/build/fotos/<avd>/` (gerado — não versionado; comandos acima
reproduzem):

- `app/build/fotos/s22u/ajustes-basico.png` — tela básica no S22 Ultra, estado de
  fábrica: Resolução em mono (720p não é um dos três segmentos), Taxa 60Hz marcado,
  Codec Auto, HDR desabilitado com legenda, Modo do toque "Toque direto", Barra de
  texto ligada (default), Controles na tela desligado (default), card Avançado com a
  legenda curta ("Touch only if you know why").
- `app/build/fotos/s22u/ajustes-avancado.png` — depois de tocar o card: a árvore
  inteira de `preferences.xml` (Video Settings/Video resolution/.../Advanced), tema
  novo, provando que nada foi apagado.
- `app/build/fotos/s22u/ajustes-persistencia.png` — depois de tocar Taxa=90Hz,
  Codec=HEVC, Modo do toque=Touchpad, Barra de texto e Controles na tela, e
  **reabrir a Activity do zero** (`force-stop` + `am start`): a foto mostra 90Hz,
  HEVC, Touchpad, Barra de texto desligada e Controles na tela ligados — exatamente o
  que ficou gravado, não o que a tela "lembrava" de antes.
- `app/build/fotos/tabs8u/ajustes-basico.png`, `ajustes-avancado.png`,
  `ajustes-persistencia.png` — mesmas três rotas no Tab S8 Ultra, coluna centrada de
  720dp, mesmos resultados.
- `app/build/fotos/s22u/folha.png`, `app/build/fotos/tabs8u/folha.png` — folhas de
  contato com as três rotas lado a lado, por aparelho.

Todas as seis fotos (mais as duas capturas offline do `screens.mjs`) foram conferidas
visualmente contra `docs/design/console/fotos/B_Tablet_Ajustes.png` e
`B_Celular_Ajustes.png`: estrutura, cores (`console_acento` nos segmentos/switches
ativos, `console_borda` nos inativos), tipografia (`ConsoleText.Titulo`/`Rotulo`/
`Rotulo2`/`Mono`/`Legenda`) e a coluna de 720dp no tablet batem com o mockup.

## Decisões tomadas onde a spec era ambígua

1. **`docs/howto/adicionar-preferencia.md` não existe** (nem neste worktree, nem no
   repositório principal `Artemis`, nem em `Artemis-F0`/`F1`/`F2`/`F3`) — segui
   `.claude/rules/prefs.md` em vez disso, que cobre o mesmo terreno (chave+widget,
   constante+default, leitura, perfil) e carrega automaticamente ao tocar
   `preferences.xml`. Reporto a ausência aqui em vez de fingir que segui um documento
   que não existe.
2. **"nenhum segmento marcado + valor em mono"** — a spec só descreve essa regra
   explicitamente para Resolução e Taxa. Estendi para Codec e Modo do toque (com
   rótulo humano em vez de mono para estes dois, já que o valor de fallback é um nome
   de opção — "Forçar H264", "Mouse normal" — não um dado técnico tipo IP/Hz).
3. **Bitrate não recalcula ao mudar Resolução/Taxa** — decisão deliberada de não
   replicar o efeito colateral de `StreamSettings` (que ajusta o bitrate default a
   cada mudança de resolução/fps); cada controle da tela básica grava só a própria
   chave.
4. **Legenda do card Avançado** — o mockup do celular mostra só "Mexa só sabendo por
   quê" (sem o prefixo "Codec, taxa de atualização e rede —"); o do tablet mostra a
   frase inteira. Criei duas strings (`console_ajuste_avancado_legenda` completa,
   `_curta` abreviada) e um `bool` por tamanho de tela (`values/bools.xml` = curta,
   `values-sw600dp/bools.xml` = completa) para bater com os dois mockups.
5. **Endereço do PC no cabeçalho não está em mono** — ver seção acima; deixado para
   F6.
6. **`android:exported="true"` em `StreamSettingsBasico`** — necessário para
   `am start` funcionar (ver seção AndroidManifest acima); não é uma mudança pedida
   pela spec, mas sem ela nem a verificação por emulador nem qualquer automação futura
   via `adb shell am start` funcionaria.

## O que NÃO foi verificado, e por quê

- **`GameMenu` (F2) não lê `checkbox_compose_bar`.** A preferência existe, persiste
  corretamente e tem UI nesta fatia, mas o comportamento real que ela descreve
  ("liga o bloco Teclado da guia à barra de texto") é código de sessão — F2,
  explicitamente fora do escopo desta tarefa. Não há nada para testar aqui além da
  própria persistência do valor, que foi verificada.
- **HDR nunca foi visto LIGADO nas fotos.** O emulador `swiftshader_indirect` não
  reporta HDR10 (`Display.HdrCapabilities`) nem um decoder HEVC confiável via
  `MediaCodecHelper` — então o switch aparece sempre desabilitado, com a legenda,
  nas duas rotas de todos os aparelhos testados. O comportamento do switch **habilitado**
  (aparelho com HDR de verdade) não foi observado — só o caminho desabilitado, que é
  justamente o que o emulador permite provar.
- **Áudio 5.1/7.1 não foi selecionado nas fotos** — o diálogo abre (a lógica reusa
  `audio_config_names`/`values` de sempre), mas nenhuma rota de `fotos.mjs` navega
  para dentro dele; só a existência do diálogo e a leitura do valor atual foram
  fotografadas indiretamente (o rótulo "Stereo"/"Estéreo" na linha).
- **Aparelho físico.** Nenhuma destas fotos veio de um Galaxy S22 Ultra ou Tab S8
  Ultra reais — fora do escopo desta fatia (`docs/planos/B-CONSOLE.md` §3, "depois
  das fatias").
- **A causa exata da diferença de `exported` entre `StreamSettings` e
  `StreamSettingsBasico`** não foi investigada a fundo (ver seção AndroidManifest) —
  contornada com o precedente já existente no próprio manifesto, não compreendida ao
  nível do framework.
- **`checkbox_compose_bar` num perfil (`EditProfileActivity`) não foi aberto na UI**
  — confirmei por leitura de código que perfis são snapshot de `getAll()`, então a
  chave nova entra automaticamente, mas não abri a tela de edição de perfil para
  fotografar a preferência nova aparecendo lá.
- **`AGENTS.md` diz "mapas regerados, se mudou estrutura"** — regenerados
  (`node tools/codemap/codemap.mjs`) e commitados; não abri o `SYMBOLS.md`/`
  DEPENDENCY_GRAPH.md` inteiros para conferir cada linha do diff, só confiei na saída
  do gerador (`validate.mjs` também confirma "codemap OK").

## Arquivos alterados

Veja `git log --oneline ea11e09a..HEAD` neste worktree (6 commits); resumo por
arquivo:

- **Novo:** `app/src/main/java/com/limelight/preferences/StreamSettingsBasico.java`,
  `app/src/main/res/layout/console_ajustes_basico_conteudo.xml`,
  `app/src/main/res/layout/activity_stream_settings_basico.xml`,
  `app/src/main/res/layout-sw600dp/activity_stream_settings_basico.xml`,
  `app/src/main/res/drawable/console_segmento_bg.xml`,
  `app/src/main/res/color/console_segmento_texto.xml`,
  `app/src/main/res/values/bools.xml`, `app/src/main/res/values-sw600dp/bools.xml`.
- **Editado:** `app/src/main/AndroidManifest.xml`,
  `app/src/main/java/com/limelight/PcView.java`,
  `app/src/main/java/com/limelight/preferences/PreferenceConfiguration.java`,
  `app/src/main/res/xml/preferences.xml`,
  `app/src/main/res/values/strings.xml`, `app/src/main/res/values-pt-rBR/strings.xml`,
  `app/src/main/res/values/styles.xml`,
  `app/src/test/java/com/limelight/VidroScreenshotTest.java`, `tools/fotos.mjs`.
- **Gerado:** `docs/maps/*` (via `node tools/codemap/codemap.mjs`).

Sem merge em `dev`, sem push — como instruído.
