# Plano B — "Console" · fatias, times e critério de pronto

Decidido em 4 de setembro de 2026: o VibeArtemis adota a **opção B ("Console")** do redesenho
(`docs/design/console/` — `NOTA.md` com os tokens, `artboards/*.dc.html` com as telas,
`fotos/B_*.png` com a referência de pixel; `BRIEF.md` com as restrições que geraram tudo).
Este arquivo é a fonte de verdade de *como* ela entra no código: em fatias pequenas, cada uma
feita por um time de Sonnet 5, verificada nos dois emuladores e revisada antes de entrar em `dev`.

## 0. Decisões fixas (não se rediscutem por fatia)

| decisão | valor | por quê |
|---|---|---|
| Stack | **Java + XML**. Sem Kotlin, sem Compose. | `AGENTS.md` exige perguntar antes de introduzir Kotlin; `tools/screens.mjs` já rasteriza layouts na JVM; foco por D-pad é território nativo de View. ADR 0003 (fatia F0). |
| Referência visual | `docs/design/console/fotos/B_*.png` e os `.dc.html` | O mockup aprovado **é** a referência. Divergiu, é defeito. |
| Aparelhos | celular **Galaxy S22 Ultra** · tablet **Galaxy Tab S8 Ultra** (capa-teclado) | AVDs `s22u` (1080×2316 @420 dpi = **412×883 dp**, retrato, teclado virtual) e `tabs8u` (2960×1848 @240 dpi = **1973×1232 dp**, paisagem, teclado físico). A densidade 240 do Tab é suposição a confirmar com `adb shell wm density` quando ele estiver no cabo. |
| Larguras | telefone `< sw600dp` · tablet `>= sw600dp` | O mockup do tablet foi desenhado a 1280×800 dp; o Tab S8 Ultra tem mais largura. **Layouts fluidos**: a fileira de capas mostra mais capas, a coluna de Ajustes fica centrada com 720 dp, margens laterais de 32 dp. Nada é esticado. |
| Menu da sessão | **janela de diálogo** (`android.app.Dialog`, como o `GameMenu` de hoje) com o conteúdo "guia" da B, gravidade TOP, largura total | É a única classe de janela que comprovadamente aparece sobre a `SurfaceView` no S22 (o `GameMenu` original do Artemis aparece). Sem view "por cima" do vídeo para o menu, sem PopupWindow, sem gaveta. |
| Gatilho do menu | uma **aba** de 64×20 dp (56×16 no celular) no centro da borda superior do vídeo, sempre visível, tocável | Não é gesto de borda (conflita com os gestos do sistema; já falhou). Toque abre; a aba continua presa à borda inferior da guia quando aberta, com chevron invertido, e fecha. Botão Voltar continua abrindo o menu se `enableBackMenu`. |
| Barra de texto | a de hoje (E01: `openComposeBar`, `sendTextBlock`, modo `RESIZE` por padrão), com a cara da B | É a razão de existir do fork. Muda a pele, não a mecânica. Flags de janela de `Game.java` **não** se tocam (regra de plan mode). |
| Ajustes | tela básica da B na frente; **"Avançado" abre a árvore inteira de hoje** (136 preferências) | "Esconder pode, apagar não." |
| Strings | novas em `values/strings.xml` **e** `values-pt-rBR/strings.xml` (exceção acordada) | O app é usado em pt-BR. Outras traduções não se editam. |
| Verificação | `node tools/validate.mjs` verde **e** fotos dos dois AVDs **e** revisão do diff | "Verificado" sem foto de emulador não existe; sem foto de aparelho é provisório e se diz assim. |

## 1. Como cada fatia roda

Cada fatia é um **time de três** agentes Sonnet 5, em sequência:

1. **Implementador** — trabalha num worktree próprio, na branch `fatia/<id>-<nome>`. Lê `AGENTS.md`,
   este plano (só a sua fatia) e a referência visual. Implementa, roda `node tools/validate.mjs`,
   roda `node tools/screens.mjs` e a ferramenta de fotos (F0) no AVD que a fatia indica, commita
   (mensagens em inglês, corpo com o porquê), e relata: arquivos, comandos executados com saída
   resumida, caminho das fotos, o que **não** conseguiu verificar e por quê.
2. **Verificador** — agente novo, sem o contexto do implementador. Confere o diff contra a
   especificação da fatia e contra os PNGs de referência; roda o gate e as fotos nos **dois** AVDs
   por conta própria; percorre os fluxos com `adb shell input` (toque, D-pad, teclado); escreve
   `docs/planos/console/relatorios/<id>-verificacao.md` com a lista de defeitos (cada um com foto
   e passo para reproduzir) e o veredito: **aprovada** ou **devolvida**.
3. **Integrador** — só quando aprovada: faz merge da branch em `dev` (sem push), roda
   `node tools/validate.mjs` e `node tools/codemap/codemap.mjs` em `dev`, commita os mapas se
   mudaram, remove o worktree, e copia a folha de contato final de cada AVD para
   `docs/planos/console/fotos/<id>-<avd>.png`.

Devolvida → um implementador novo recebe a lista de defeitos e a branch; no máximo duas
devoluções por fatia antes de o plano ser revisto. O revisor final de tudo é o Fable, que lê os
relatórios e as folhas de contato, não o código inteiro.

Regras que valem para todos: nada de `@Ignore`, assert enfraquecido, lint suprimido ou teste
apagado para ficar verde; nunca declarar build/teste/foto que não executou; portas de emulador
fixas por fatia (abaixo) para dois times não brigarem pela mesma instância.

## 2. As fatias

Ordem: **F0 → F1 → (F2 ‖ F3) → F4 → F5 → F6.** F2 e F3 tocam arquivos disjuntos e podem correr
em paralelo depois de F1.

### F0 — Ver e provar: fotos nos dois emuladores, moques de casca e de stream, ADR

Sem isto nenhuma fatia consegue provar nada. Tudo aqui é ferramenta e debug-only.

- **`tools/fotos.mjs`** (evolução de `tools/emu.mjs`, que hoje sobe só o AVD `vidro`):
  `node tools/fotos.mjs --avd s22u|tabs8u [--port 5554] [--rotas <lista>]`. Sobe o AVD sem
  janela (`-no-window`, ou com janela se `--janela`), espera o boot, instala o APK x86_64 debug
  (`app/build/outputs/apk/nonRoot_game/debug/app-nonRoot_game-x86_64-debug.apk` — construir antes
  se não existir), percorre as **rotas** e salva `app/build/fotos/<avd>/<rota>.png` mais uma
  folha de contato `app/build/fotos/<avd>/folha.png` (miniaturas com rótulo). Rotas iniciais:
  `computadores` (PcView com o moque de casca), `apps` (AppView com o moque), `ajustes`
  (StreamSettings), `adicionar` (AddComputerManually), `cenas` (ProfilesActivity),
  `sessao` (Game com o moque de stream), `sessao-menu` (idem, depois de tocar na aba/botão do
  menu), `sessao-texto` (idem, depois de abrir a barra de texto). No `tabs8u`, também `sessao`
  em paisagem é o padrão; no `s22u`, `sessao*` giram para paisagem (`settings put system
  user_rotation 1`) e as demais ficam em retrato. Cada rota espera a tela assentar (uiautomator
  `dump` estável ou 1,5 s) antes do `screencap`. A ferramenta deve funcionar com o AVD já
  ligado (reutiliza) e com `hw.ramSize` de 2048 MB nos dois AVDs (ajustar os `config.ini`).
- **Moque de casca** (debug-only, `BuildConfig.DEBUG`): `am start -n com.limelight.noirdebug/com.limelight.PcView --ez MoqueDeCasca true`
  faz a `PcView` mostrar dois PCs sem rede: **ARTEMIS** (on-line, emparelhado, `192.168.0.10`)
  e **NOTEBOOK** (fora do ar); e `AppView` com `--ez MoqueDeApps true` lista **Área de trabalho
  (em execução)**, **Steam**, **Corrida Noturna**, **Fortaleza**, **Vale Silencioso**,
  **Estrada Sem Fim**, com capas geradas (bloco sólido com o nome, 3:4). Ponto de costura menos
  invasivo possível — o time investiga `PcView`/`ComputerManagerService`/`AppGridAdapter` e
  escolhe; o critério é: zero efeito em build de release e nenhuma mudança de comportamento
  sem o extra.
- **Moque de stream** (debug-only): `am start -n com.limelight.noirdebug/com.limelight.Game --ez MoqueDeStream true`
  abre a `Game` sem host: superfície preta, sem tentar conectar, com todas as camadas de
  interface vivas (aba/menu, barra de texto, conectando/queda escondidas). A branch
  `arquivo/redesenho-vidro-2026-08` tem uma versão disso (`app/src/debug/AndroidManifest.xml`
  exportando a `Game` e um desvio no `onCreate`); ler com `git show arquivo/...:<caminho>` e
  **reimplementar** o mínimo no `Game.java` atual — não fazer cherry-pick. Não tocar em flags
  de janela.
- **ADR 0003 — stack da interface**: `docs/adr/0003-stack-da-interface.md`, curto: Java + XML;
  Compose avaliado e recusado (motivos da tabela acima); revisitável se a B exigir algo que View
  não dá.
- **Corrigir `docs/REPLANEJAMENTO.md` §3** para refletir o ADR (hoje diz Compose) e a ordem do §7.
- **Pronto quando**: `node tools/fotos.mjs --avd s22u` e `--avd tabs8u` produzem as 8 rotas e a
  folha de contato em cada AVD, sem intervenção manual; gate verde; relatório com as duas folhas.

Porta padrão desta fatia: 5554.

### F1 — Tokens da B

Instala a linguagem visual sem redesenhar telas ainda.

- Em `res/values/colors.xml`: os tokens da `NOTA.md` §2, com prefixo `console_`:
  `console_fundo #0B0B0D` · `console_superficie #16161A` · `console_superficie_elevada #1C1C20` ·
  `console_superficie_selecionada #22232A` · `console_borda #2A2A30` · `console_texto #F2F2F4` ·
  `console_texto_2 #9A9AA5` · `console_texto_desabilitado #5A5A62` · `console_texto_desabilitado_2 #6A6A72` ·
  `console_acento #7FE0C7` · `console_acento_sobre #0A1F1A` · `console_destrutivo #F2555B` ·
  `console_destrutivo_fundo #1FF2555B` (12 %). **Os tokens `vidro_*` viram aliases** dos
  `console_*` mais próximos (fundo→fundo, texto→texto, os três acentos→acento, destrutivo→destrutivo)
  para nada quebrar durante a transição; F6 apaga os aliases.
- Em `res/values/dimens.xml`: `console_espaco_1..6` (4/8/12/16/24/32 dp), `console_raio_controle`
  8 dp, `console_raio_card` 12 dp, `console_linha_ajuste` 64 dp (tablet) / 56 dp (celular, via
  `values`/`values-sw600dp`), `console_botao` 48 dp, `console_capa_w` 160 dp / `console_capa_h`
  213 dp, `console_foco_borda` 3 dp, `console_foco_anel` 4 dp, `console_aba_w` 64 dp / `console_aba_h`
  20 dp (celular 56×16 via `values-sw600dp` invertido: o default é o celular).
- Em `res/values/styles.xml`: escala `ConsoleText.Titulo` (32 sp / ExtraBold / 1.15),
  `.Card` (20 sp / Bold), `.Rotulo` (16 sp tablet · 14 sp celular / SemiBold),
  `.Rotulo2` (14 sp / Medium / cor texto_2), `.Legenda` (12 sp / Medium / caixa alta, tracking
  0,08 em), `.Mono` (13 sp / JetBrains Mono Medium / `tabular-nums`); `ConsoleBotao.Primario`
  (fundo acento, texto acento_sobre, 48 dp, raio 8) e `.Neutro` (superfície + borda);
  `ConsoleSegmento` (controle segmentado 36–40 dp). O tema `AppTheme` passa a apontar
  `colorPrimary`/`colorAccent`/`colorControlActivated` para `console_acento`, `android:windowBackground`
  para `console_fundo`, `colorError` para `console_destrutivo`.
- **Foco** é o coração da B: `res/drawable/console_foco.xml` (selector: `state_focused` → borda 3 dp
  `console_acento` + anel 4 dp a 15 %; `state_pressed` → superfície selecionada) como
  `android:foreground`/`background` de todo item focável. E `res/drawable/console_capa_foco.xml`
  para a capa (escala 1,2× fica por `animate`/`scaleX/Y` no F3; aqui só o contorno).
- **Pronto quando**: `node tools/screens.mjs` e as fotos dos dois AVDs mostram o app com o tema
  novo (cores e fontes trocadas, mesmo que os layouts ainda sejam os antigos); gate verde;
  `docs/design/console/TOKENS.md` escrito com a tabela token → recurso.

Porta: 5554.

### F2 — A sessão: guia, aba e barra de texto

O bug que prende o Rafael na stream é o que esta fatia resolve. Referência:
`B_Tablet_Sessao_Menu.png`, `B_Tablet_Sessao_Texto.png`, `B_Celular_Sessao_Menu.png`.

- **Aposentar** a ilha e o botão flutuante: `ui/VidroIsland.java`, `vidro_session_island.xml`,
  `vidro_island_tile.xml`, `vidro_session_sheet.xml`, o `<include>` da ilha em
  `activity_game.xml:118` e o `floatingMenuButton` (`activity_game.xml:131-142`, arraste em
  `Game.java:1085-1120`, `setupVidroIsland()` em `Game.java:1334`). Remover código morto, não
  esconder. A preferência `enableBackMenu` continua valendo.
- **A aba** (`res/layout/console_aba.xml`, incluída em `activity_game.xml` acima da
  `StreamContainer`): 64×20 dp, `console_superficie_elevada`, chevron para baixo (SVG → vector
  drawable), centro da borda superior, dentro dos insets do sistema, `contentDescription` =
  string nova `console_abrir_menu`. Toque → `showGameMenu(null)` (`Game.java:4820`). Sem
  arraste, sem gesto. Some enquanto a barra de texto ou um diálogo de conexão estiver aberto.
- **A guia** substitui a lista do `GameMenu` (`GameMenu.showMenuDialog`, `GameMenu.java:159-206`):
  mesmo `android.app.Dialog` (tema `VidroDialogPlatform` → renomear para `ConsoleDialog`),
  `Gravity.TOP`, largura `MATCH_PARENT`, sem dim (`FLAG_DIM_BEHIND` fora), `setCanceledOnTouchOutside(true)`,
  conteúdo `res/layout/console_guia.xml`: um `HorizontalScrollView` de blocos (ícone 24 dp +
  rótulo, largura ≥ 96 dp, altura 64–76 dp, focáveis, `console_foco`) na ordem **Teclado ·
  Barra de texto · Modo do toque · Teclas especiais · Girar a tela · Estatísticas ·
  Picture-in-picture · Controles na tela**, e à direita, fixos, **Desconectar** e **Encerrar no
  PC** (este em `console_destrutivo` sobre `console_destrutivo_fundo`; pede confirmação — o
  diálogo de confirmação existente, retemado). A aba fica presa à borda inferior da guia com
  chevron para cima e fecha ao toque. **Teclas especiais** troca os blocos pela fileira
  `Esc · Tab · Win · Alt+Tab · Ctrl+Alt+Del` (rótulos em `ConsoleText.Mono`) e um bloco "‹
  Voltar". Cada bloco chama a ação que o `GameMenu` já tem (teclado: `Game.java:1223`; texto:
  `openComposeBar`; teclas: família `game_menu_send_keys_*` em `GameMenu.java:212-252`; girar:
  `game_menu_rotate_screen`; estatísticas: `game_menu_toggle_hud`; PiP: a ação de PiP existente;
  controles na tela: alternância existente; desconectar / encerrar: as existentes). "Modo do
  toque" alterna `touchscreenTrackpad` em runtime, mostrando o estado atual no rótulo
  (`Touchpad` / `Toque direto`). O que não existir como ação hoje, o bloco **não aparece** —
  não se inventa ação.
- **D-pad / teclado físico**: ←/→ move o foco entre blocos, Enter ativa, Esc/Voltar fecha.
  Foco inicial no primeiro bloco. `DPAD_DOWN` na aba (quando a guia está fechada) não faz nada.
- **Barra de texto** (`vidro_compose_bar.xml` → `console_barra_texto.xml`): faixa no rodapé,
  cabeçalho `BARRA DE TEXTO` (`ConsoleText.Legenda`) com, à direita, latência e taxa de bits
  em `ConsoleText.Mono` **somente se** o app já tiver esses números (do overlay de desempenho);
  senão, nada — não inventar "18 ms". Campo (`ConsoleText.Rotulo`, fundo `console_superficie`,
  borda), botão **Enviar** (`ConsoleBotao.Primario`, ícone seta), fileira `Esc · Tab · Win ·
  Alt+Tab · Ctrl+Alt+Del` como blocos focáveis mono. Mecânica intacta: `openComposeBar`
  (`Game.java:1223`), `sendTextBlock` (`Game.java:2004`), `closeComposeBar` (`Game.java:1246`),
  `ImeDisplayMode.RESIZE` por padrão (`Game.java:2064`). No `s22u` (teclado virtual) o vídeo
  encolhe; no `tabs8u` (teclado físico) não há IME e a barra aparece sozinha.
- **Fora do escopo**: flags de janela da `Game`, o `SurfaceView`, decodificação, entrada.
- **Pronto quando**: no moque de stream dos dois AVDs, fotos de: aba em repouso; guia aberta
  (após `input tap` na aba) idêntica em estrutura ao mockup; guia navegada por `input keyevent
  KEYCODE_DPAD_RIGHT` ×3 + `KEYCODE_ENTER` em "Teclas especiais" (foto da fileira de teclas);
  `KEYCODE_ESCAPE` fecha; barra de texto aberta (no `s22u` com o teclado virtual de pé e o vídeo
  encolhido; no `tabs8u` sem IME); Voltar com `enableBackMenu` abre a guia. Gate verde. Relatório
  diz explicitamente que a aparência sobre vídeo real **não** foi verificada (é moque).

Porta: 5554.

### F3 — Início (tablet) · Computadores e Apps (celular)

Referência: `B_Tablet_Inicio.png`, `B_Celular_Computadores.png`, `B_Celular_Apps.png`.

- **Tablet (`layout-sw600dp`)**: `PcView` vira a tela **Início** única: cabeçalho com o **chip do
  PC atual** (`● ARTEMIS · on-line ⌄`; toque abre um diálogo simples listando os PCs com estado
  e "Adicionar PC"), chips esmaecidos dos PCs fora do ar ao lado, e o botão **Ajustes**
  (ícone + rótulo, 44 dp, à direita). Seção `CONTINUAR`: card com a miniatura, nome do app em
  execução no PC atual, `● ARTEMIS · em execução`, **Retomar** (primário) e **Encerrar no PC**
  (neutro com texto destrutivo) — só aparece se houver app em execução (`AppView` já sabe:
  `vidroResumeStrip`). Seção `APPS`: `RecyclerView` horizontal de capas 3:4 (`console_capa_w/h`)
  do PC atual, com **foco** = escala 1,2× (animação de 120 ms com `ViewPropertyAnimator`, sem
  mola) + `console_capa_foco`; abaixo, nome da capa focada (`ConsoleText.Card`) e **Abrir**
  (primário). ←/→ movem o foco; Enter abre; toque abre. A lista de apps vem do mesmo caminho
  que `AppView` usa hoje (`AppGridAdapter`, `CachedAppAssetLoader`) — reusar, não duplicar.
  Emparelhar continua como overlay (`vidro_pairing.xml`, retemado em F5).
- **Celular**: `PcView` = **Computadores**: título `ConsoleText.Titulo`, cards altos em coluna
  (`● ON-LINE` / nome / IP mono / chevron; fora do ar esmaecido com `endereço indisponível`),
  **Adicionar PC** como card tracejado, foco com `console_foco`. `AppView` = **Apps**: cabeçalho
  `‹ ● ARTEMIS · on-line`, título, grade de 2 colunas de capas 3:4 com `EM EXECUÇÃO` no canto
  da capa em execução (primeira da grade), foco com contorno; toque abre; toque longo mantém o
  menu de hoje (encerrar no PC etc.).
- Estados de host: on-line / fora do ar / precisa emparelhar (ponto de cor: acento / texto_2 /
  texto_2 com rótulo "Emparelhar"). Nunca uma segunda cor.
- **Fora do escopo**: Ajustes (F4), conectando/emparelhar/queda (F5), sessão (F2).
- **Pronto quando**: com `MoqueDeCasca`/`MoqueDeApps`, fotos no `tabs8u` (Início com Continuar
  e fileira, foco movido com D-pad ×2, diálogo do chip aberto) e no `s22u` (Computadores, Apps,
  foco na 3ª capa via D-pad) batendo com os mockups em estrutura, cores e tipografia; sem moque,
  a `PcView` vazia mostra o estado "nenhum PC" na mesma linguagem; gate verde.

Porta: **5556** (roda em paralelo com F2).

### F4 — Ajustes básicos + Avançado

Referência: `B_Tablet_Ajustes.png`, `B_Celular_Ajustes.png`.

- `StreamSettings` ganha uma tela **básica** nova (`res/xml/preferences_basico.xml` +
  layouts de preferência customizados), coluna centrada de 720 dp no tablet, cabeçalho `‹ Início
  / Ajustes / ARTEMIS · 192.168.0.10` (o PC atual, se houver). Linhas de `console_linha_ajuste`:
  **Resolução** (segmentado `1080p · 1440p · Nativa` ↔ `list_resolution`; se o valor atual não
  for um dos três, nenhum segmento marcado e o valor exato em mono à direita), **Taxa**
  (`60 · 90 · 120 Hz` ↔ `list_fps`, mesma regra), **Taxa de bits** (slider ↔
  `seekbar_bitrate_kbps`, valor `NN Mbps` em mono), **Codec** (`Auto · HEVC · AV1` ↔
  `video_format`), **HDR** (switch ↔ `checkbox_enable_hdr`; desabilitado com a legenda
  "Exige jogo com HDR e GPU com HEVC Main 10" quando o decodificador não suportar), **Áudio**
  (`Estéreo ›` ↔ `list_audio_config`, abre a lista de hoje), **Modo do toque** (`Touchpad ·
  Toque direto` ↔ o campo `touchscreenTrackpad`, ver `PreferenceConfiguration.java:954-968`),
  **Barra de texto** (switch ↔ preferência **nova** `checkbox_compose_bar`, padrão ligado: com
  ela, o bloco Teclado da guia abre a barra; sem ela, alterna o teclado cru), **Controles na
  tela** (switch ↔ `checkbox_show_onscreen_controls`), e por fim o card **Avançado**
  ("Codec, taxa de atualização e rede — mexa só sabendo por quê") que abre a
  `preferences.xml` inteira, intocada, com o tema novo.
- Foco por D-pad em toda linha; o segmento selecionado em `console_acento`.
- **Pronto quando**: fotos nos dois AVDs batendo com os mockups; mudar cada controle e reabrir
  a tela mantém o valor (persistência real nas mesmas chaves de hoje); "Avançado" abre a árvore
  completa; gate verde; `docs/howto/adicionar-preferencia.md` seguido para a preferência nova.

Porta: 5554.

### F5 — Conectando · emparelhar · queda · adicionar PC

Sem mockup próprio (o brief não pediu): as quatro telas existentes (`vidro_connecting.xml`,
`vidro_pairing.xml`, `vidro_lost.xml`, `activity_add_computer_manually.xml`) trocam de pele para
os tokens e a tipografia da B, mantendo estrutura e mecânica. Regras: fundo `console_fundo`,
título `ConsoleText.Titulo`, código de pareamento em mono grande, botões `ConsoleBotao`, luz
`VidroLightView` substituída por um ponto de estado simples (acento = ativo). Pronto quando as
quatro aparecem nas fotos (o moque de stream ganha `--es MoqueEstado conectando|queda` para
mostrar as duas da sessão) e batem com a linguagem das outras telas.

Porta: 5554.

### F6 — Polimento contra o mockup e limpeza

- Folha de contato **lado a lado** (mockup × emulador) por tela e por AVD; corrigir cada delta
  de espaçamento, tamanho e cor até ficar na tolerância (celular: mesma grade de dp do mockup,
  então quase exato; tablet: mesma estrutura e medidas, mais capas).
- Apagar os aliases `vidro_*`, layouts e estilos mortos; regenerar mapas; atualizar
  `docs/design/` (o `DESIGN-SYSTEM.md` e `VIDRO-IMPLEMENTACAO.md` passam a apontar para
  `console/`); `values-pt-rBR` completo para toda string nova.
- Pronto quando o Fable aprova as folhas e o gate está verde. Depois disso: os aparelhos.

## 3. Depois das fatias: os aparelhos

Com a depuração por Wi-Fi ligada (`tools/parear-aparelho.bat`), `tools/fotos.mjs --serial <ip:porta>`
roda as mesmas rotas no S22 Ultra e no Tab S8 Ultra. Só então uma tela é "verificada" sem
adjetivo. O primeiro alvo no aparelho é a **guia da sessão sobre vídeo real** — o que nenhum
emulador prova.
