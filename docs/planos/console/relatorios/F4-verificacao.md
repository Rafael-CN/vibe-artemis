# F4 — Ajustes básicos + Avançado — verificação

Verificador independente, sem contexto do implementador. Worktree
`C:\Users\Rafael\Documents\Projetos\Claude\Artemis-F4`, branch `fatia/F4-ajustes`, base
`dev` = `ea11e09a`. Porta reservada: **5554**. Não toquei em `Artemis`, `Artemis-F1`,
`Artemis-F2`; nenhum outro emulador estava de pé quando comecei (`adb devices` vazio).
Um emulador por vez, como pedido: `s22u` subiu, foi usado, foi morto (`emu kill`) antes
de subir `tabs8u`; `tabs8u` foi morto ao final. `adb devices` vazio nas duas checagens.

`docs/planos/console/relatorios/F4-implementacao.md` foi lido **por último**, depois da
spec (`B-CONSOLE.md` §0, §1, §F4), de `TOKENS.md` e dos dois mockups, e tratado como
alegação a conferir — não como fonte de verdade. Toda alegação relevante foi conferida
por conta própria: diff, gate, fotos nos dois AVDs, `adb shell` (tap, D-pad, leitura
direta do `SharedPreferences`).

## O que executei

- `JAVA_HOME=C:\Program Files\Java\jdk-21.0.12` em todos os comandos Node/Gradle.
- `git diff ea11e09a..HEAD --stat` e leitura completa dos arquivos tocados.
- `node tools/validate.mjs` — três vezes: uma antes de mexer em qualquer coisa, uma depois
  de forçar a falha do codemap (esperada, corrigida regenerando), uma depois da minha
  correção do defeito de D-pad (ver §4).
- `node tools/codemap/codemap.mjs` (rodado por mim depois da minha correção, porque ela
  mudou `StreamSettingsBasico.java`).
- `node tools/screens.mjs` — duas vezes (antes e depois da correção); 31 telas geradas
  nas duas rodadas, sem erro, incluindo `10-ajustes-basico.png` e
  `tab-10-ajustes-basico.png`.
- `node tools/fotos.mjs --avd s22u --port 5554 --rotas ajustes-basico,ajustes-avancado,ajustes-persistencia`
  e o mesmo para `--avd tabs8u`, **por conta própria**, nos meus próprios processos de
  emulador (subi e derrubei os dois eu mesmo).
- `adb shell input tap/keyevent`, `adb shell am force-stop`/`am start`,
  `adb shell run-as … cat shared_prefs/…` e `adb shell uiautomator dump` manualmente,
  fora dos scripts, para provar persistência real e navegação por D-pad control por
  control.
- Edição direta do `SharedPreferences` em disco (via `run-as`, pull/edit/push) para forçar
  um valor de `list_fps` fora dos três segmentos, sem passar pela UI.

## Veredito: **DEVOLVIDA → corrigida nesta sessão → APROVADA**

Encontrei um defeito real (D-pad não persiste os três switches — §4), pequeno e óbvio na
causa, e o corrigi nesta branch (commit descrito em §7). Depois da correção, com o gate
verde e todas as verificações abaixo repetidas, o veredito final é **APROVADA**.

## 1. Auditoria de preferências órfãs (o item que reprova sozinho)

`res/xml/preferences.xml` tem **137** `android:key=` (136 preexistentes + `checkbox_compose_bar`,
a única nova desta fatia). Contagem:

```
grep -c 'android:key=' app/src/main/res/xml/preferences.xml   → 137
```

- **Nenhuma preferência foi removida do XML.** `git diff ea11e09a..HEAD -- app/src/main/res/xml/preferences.xml`
  mostra só uma inserção (o `CheckBoxPreference` novo, +6 linhas). Nada foi apagado.
- **`StreamSettings.java` continua intocado** como Activity e como dono de
  `addPreferencesFromResource(R.xml.preferences)` (linha 336, único uso no arquivo) —
  a árvore inteira, sem cortes.
- **A árvore é alcançável.** `PcView` agora abre `StreamSettingsBasico`
  (a tela nova); o card "Avançado" dela abre `StreamSettings` via `Intent` simples, sem
  filtro nem argumento que module o que aparece. Confirmei **no aparelho** (s22u,
  emulator-5554): toquei "Avançado" e vi as categorias Video/Audio/Gamepad/Input/Host/
  General/UI/On-screen/Performance/Advanced/MISC, na ordem do XML, com o tema novo
  (títulos de categoria em `console_acento`).
- **`checkbox_compose_bar` não é órfã.** Está em `category_input_settings` ("Input
  Settings"), a mesma categoria de `checkbox_enable_commit_text` (logo depois dela, como
  o relatório diz) — categoria que `StreamSettings.java` **nunca remove por inteiro**
  (só remove `checkbox_absolute_mouse_mode` dessa categoria sob uma condição; a própria
  categoria e a preferência nova sobrevivem em qualquer cenário). Confirmei visualmente
  no tablet (`app/build/fotos/tabs8u/ajustes-avancado.png`): a seção "Input Settings"
  aparece por inteiro, com um resumo "Advanced" que lista "Open the text bar from the
  session menu" — a preferência nova, dentro do grupo de conveniência que agrupa os
  itens menos comuns da categoria (mesmo padrão usado em todas as outras categorias,
  não um esconderijo criado por esta fatia).
- **Nenhuma preferência ficou "sem entrada na básica e fora da árvore".** A tela básica
  cobre 8 chaves (`list_resolution`, `list_fps`, `seekbar_bitrate_kbps`, `video_format`,
  `checkbox_enable_hdr`, `list_audio_config`, `mouse_mode_list`/`checkbox_compose_bar`/
  `checkbox_show_onscreen_controls`); as outras 129 continuam só na árvore completa,
  que está inteira e alcançável — exatamente "esconder pode, apagar não".

**Conclusão do item 1: nenhuma preferência órfã. A árvore está inteira e alcançável.**

## 2. Persistência real, nas mesmas chaves de hoje

Testei eu mesmo, fora do `fotos.mjs`, em `emulator-5554` (s22u), depois de
`pm clear com.limelight.noirdebug` para partir de instalação limpa:

1. Estado de fábrica (`am start` direto): Resolução em mono `1280x720` (720p não é um
   dos três segmentos — comportamento correto, não bug), Taxa `60 Hz` marcada, Bitrate
   `10 Mbps`, Codec `Auto`, HDR desabilitado com a legenda, Áudio `Stereo`, Modo do toque
   `Direct touch`, Barra de texto **ligada** (default novo = `true`), Controles na tela
   desligado (default de sempre = `false`).
2. Mudei Taxa→90Hz, Codec→HEVC, Modo do toque→Touchpad, Barra de texto→off, Controles na
   tela→on, por toque.
3. `adb shell am force-stop com.limelight.noirdebug` + `am start -n …/.StreamSettingsBasico`
   (Activity nova do zero, não `onResume` de uma instância viva).
4. Fotografei de novo: **90 Hz, HEVC, Touchpad, Barra de texto off, Controles na tela on**
   — exatamente o que foi gravado, não o que a tela "lembrava".
5. Li o arquivo real em disco:
   `adb shell run-as com.limelight.noirdebug cat shared_prefs/com.limelight.noirdebug_preferences.xml`
   — chaves confirmadas: `list_resolution=1280x720`, `list_fps=90`,
   `video_format=forceh265`, `mouse_mode_list=2`, `checkbox_compose_bar=false`,
   `checkbox_show_onscreen_controls=true`. **É o arquivo padrão do
   `PreferenceManager.getDefaultSharedPreferences`**, o mesmo que a árvore de sempre usa
   — não existe um armazenamento novo paralelo. `seekbar_bitrate_kbps` não apareceu no
   arquivo porque eu não toquei no slider nessa rodada (o valor mostrado vem do cálculo
   de default, como o próprio relatório do implementador avisa) — não é bug, é a mesma
   regra herdada de `StreamSettings`.

**Conclusão do item 2: persistência real confirmada, nas chaves de hoje, sem
armazenamento paralelo.**

## 3. Valor fora dos segmentos

Dois casos, os dois confirmados sem arredondar para o segmento mais próximo:

- **Resolução de fábrica (`1280x720`)**: já não bate com nenhum dos três segmentos
  (1080p/1440p/Nativa). Em toda foto de instalação limpa, nenhum segmento fica marcado
  e `1280x720` aparece à direita em `ConsoleText.Mono` (confirmei o estilo no XML:
  `consoleResolucaoValorMono` usa `style="@style/ConsoleText.Mono"`).
- **Taxa forçada para `75`** (fora de `60/90/120`): editei o `SharedPreferences` direto em
  disco (`run-as` + pull/edit/push, sem passar pela UI) para `list_fps=75` e reabri a
  tela — nenhum segmento de Taxa ficou marcado, e "75 Hz" apareceu à direita em mono
  (`consoleTaxaValorMono`, mesmo estilo). Foto:
  `scratchpad/s22u-fps75.png` (local, não versionada — comando reproduz).

**Conclusão do item 3: a regra "nenhum segmento marcado + valor exato em mono" vale para
Resolução e Taxa, como a spec exige.** (Codec e Modo do toque usam o mesmo mecanismo,
mas com rótulo humano em vez de mono — extensão do implementador, documentada e
razoável, já que a spec só menciona mono para Resolução/Taxa.)

## 4. Defeito encontrado e corrigido: switches não persistem via D-pad/Enter

**Sintoma**: usando só o teclado/D-pad (sem tocar a tela), naveguei até o switch "Barra
de texto" (foco visível em `console_acento`, confirmando o item de foco por D-pad) e
apertei `KEYCODE_DPAD_CENTER`. O switch trocou de visual (off→on) normalmente. Mas ao
ler o `SharedPreferences` em seguida, `checkbox_compose_bar` continuava `false` — a
mudança nunca foi gravada. Repeti para "Controles na tela": mesmo defeito. Confirmei com
`am force-stop` + `am start`: a tela voltava sempre ao valor gravado antigo, ignorando o
toggle visual do D-pad.

**Causa**: os três switches (`HDR`, `Barra de texto`, `Controles na tela`) gravavam a
mudança só quando `buttonView.isPressed()` era verdadeiro dentro do
`onCheckedChanged` — um jeito de distinguir clique do usuário de `setChecked()`
programático (chamado por `renderizarTudo()` a cada `onResume`). `View#isPressed()` não
fica `true` de forma confiável para um clique disparado por tecla
(`KEYCODE_DPAD_CENTER`/Enter) no momento em que o listener roda — o clique acontece, o
estado visual muda, mas o gate de gravação nunca abre. Por toque (o único caminho que o
`fotos.mjs` testa) o defeito não aparece, por isso passou pela verificação original.

**Correção** (`StreamSettingsBasico.java`, commitada nesta branch): troquei o gate por um
flag explícito `atualizandoProgramaticamente`, ligado só durante o trecho de
`renderizarTudo()` que chama `setChecked(...)` nos três switches, e desligado logo
depois (bloco `try/finally`). Os três listeners agora checam esse flag em vez de
`isPressed()` — funciona igual para toque e para tecla, porque não depende mais de um
sinal que o framework só garante para toque.

**Verificação da correção**: rebuild (`assembleNonRoot_gameDebug`, sucesso), reinstalado
no s22u, repeti o mesmo teste por D-pad — "Controles na tela" e "Barra de texto" agora
gravam no ato (`run-as cat shared_prefs/…` mostrou o valor novo imediatamente após o
`KEYCODE_DPAD_CENTER`) e sobrevivem a `force-stop`+`am start`. Reproduzi também
`node tools/fotos.mjs` (rota `ajustes-persistencia`, que usa toque) nos dois AVDs depois
da correção — continua batendo com o relatado pelo implementador, confirmando que a
correção não regrediu o caminho por toque.

Este é o único defeito encontrado. Por ser pequeno, de causa óbvia (um gate de detecção
de origem do clique que não cobre teclado/D-pad) e localizado num arquivo desta própria
fatia, corrigi e commitei nesta branch em vez de devolver — a fatia entra aprovada com a
correção incluída.

## 5. `checkbox_compose_bar` — os quatro lugares e o howto ausente

Confirmei pessoalmente, antes de ler o relatório: `docs/howto/adicionar-preferencia.md`
**não existe** neste worktree (`find . -iname "adicionar-preferencia*"` vazio), nem no
repositório principal `Artemis`, nem em `Artemis-F1`/`Artemis-F2`. A alegação do
implementador procede.

`.claude/rules/prefs.md` (que ele seguiu) cobre o mesmo terreno de
`docs/howto/adicionar-preferencia.md` citado na spec — mesmos quatro passos:

1. **Chave e widget**: `CheckBoxPreference` novo em `preferences.xml`, logo depois de
   `checkbox_enable_commit_text`, dentro de `category_input_settings` (confirmado no
   diff, §1 acima).
2. **Constante e default**: `CHECKBOX_COMPOSE_BAR` (package-private, não `private`,
   porque `StreamSettingsBasico` a usa) + `DEFAULT_COMPOSE_BAR = true` em
   `PreferenceConfiguration.java` (confirmado no diff).
3. **Leitura**: `config.composeBarEnabled = prefs.getBoolean(CHECKBOX_COMPOSE_BAR, DEFAULT_COMPOSE_BAR);`
   em `readPreferences()` (confirmado no diff).
4. **Perfil**: verifiquei o mecanismo real em `ProfilesManager.OverlaySharedPreferences`
   — um perfil é um "patch" por cima do `SharedPreferences` base; uma chave ausente do
   patch (como `checkbox_compose_bar` seria, para qualquer perfil criado antes desta
   fatia) cai no `base.getBoolean(key, defValue)` normalmente, sem exceção e sem
   "esquecer" o valor global. Não existe lista fixa de chaves conhecidas por perfil para
   atualizar — a alegação do implementador ("não exige código") procede.

**Strings**: `title_compose_bar`/`summary_compose_bar` existem em `values/strings.xml`
**e** `values-pt-rBR/strings.xml` — conferi as duas, texto coerente nos dois idiomas,
nenhum outro `values-*/` tocado (`git diff --stat` só lista esses dois arquivos de
string).

**Conclusão do item 5**: o procedimento seguido (`.claude/rules/prefs.md`) é equivalente
ao howto ausente para os quatro passos que importam; nada ficou incompleto.

## 6. Diff contra a spec, D-pad, fronteiras

- **Linhas de Ajustes**: Resolução, Taxa, Taxa de bits, Codec, HDR, Áudio, Modo do
  toque, Barra de texto, Controles na tela, card Avançado — todas presentes, na ordem do
  mockup, nos dois AVDs (fotos em `app/build/fotos/{s22u,tabs8u}/ajustes-basico.png`).
  Estrutura, cor (`console_acento` nos segmentos/switches ativos, `console_borda` nos
  inativos) e tipografia (`ConsoleText.Titulo/Rotulo/Rotulo2/Mono/Legenda`) batem com
  `B_Tablet_Ajustes.png`/`B_Celular_Ajustes.png`. Única divergência conhecida (já
  documentada pelo implementador): o endereço do PC no cabeçalho não está em mono como
  no mockup do tablet — cosmético, não testável sem "PC atual" resolvido no meu ambiente
  (sem host pareado), deixado para F6 como o relatório já registra.
- **Foco por D-pad**: testei a sequência inteira com `adb shell input keyevent
  KEYCODE_DPAD_DOWN` a partir do botão "Home" — o foco percorre Home → (pula Resolução/
  Taxa quando estão em modo mono, corretamente, porque não há segmento para focar) →
  Bitrate (slider) → Codec → (pula HDR desabilitado) → Audio → Modo do toque → Barra de
  texto → Controles na tela → card Avançado, sempre com contorno `console_acento`
  visível. Nenhuma linha ficou inalcançável por teclado.
- **Fronteiras**: `git diff ea11e09a..HEAD --stat` — nenhum arquivo Kotlin, nenhum
  Compose, nenhum arquivo de `Game.java`/`GameMenu.java`/sessão (F2) ou de
  Início/Computadores/Apps (F3) tocado. Cores: as únicas ocorrências de hex fora de
  `colors.xml` nos arquivos novos são `#267FE0C7` em `console_segmento_bg.xml` — o
  mesmo literal de "acento a 15% de alfa" que a F1 já documentou e usou para o anel de
  foco (`TOKENS.md` §"Decisões desta fatia"), reaproveitado aqui, não uma cor nova.
  Nenhuma outra cor fora de `@color/console_*`. Strings novas só em `values/` e
  `values-pt-rBR/` (confirmado no diff, §5).

## 7. Gate e fotos

```
node tools/validate.mjs        (antes)  → passou (codemap/jni/build OK; test: 4 falha(s)
                                           preexistente(s), nenhuma nova)
node tools/validate.mjs        (depois de eu forçar list_fps=75 manualmente — não afeta
                                 o gate, só o SharedPreferences do dispositivo)
node tools/validate.mjs        (depois da correção do §4, com codemap regenerado)
                                → [ OK ] codemap / jni / build / test — mesmo resultado
                                  de teste (4 preexistentes, nenhuma nova) → passou.
node tools/screens.mjs         → 31 telas, sem erro, nas duas rodadas (antes/depois da
                                  correção) — inclui 10-ajustes-basico.png e
                                  tab-10-ajustes-basico.png via shootActivity.
node tools/fotos.mjs --avd s22u   --port 5554 --rotas ajustes-basico,ajustes-avancado,ajustes-persistencia
                                → 3/3 (rodado duas vezes: antes, achando o defeito do
                                  §4 ao inspecionar o estado de partida; depois da
                                  correção, com pm clear antes, para partir limpo)
node tools/fotos.mjs --avd tabs8u --port 5554 --rotas ajustes-basico,ajustes-avancado,ajustes-persistencia
                                → 3/3, depois da correção do §4.
```

Emuladores: `s22u` e `tabs8u`, um de cada vez, ambos em `emulator-5554`; matei cada um
(`adb -s emulator-5554 emu kill`) antes de subir o próximo e ao final. `adb devices`
vazio depois do último kill.

## Commit desta verificação

Um commit nesta branch, corrigindo o defeito do §4:
`app/src/main/java/com/limelight/preferences/StreamSettingsBasico.java` (troca do gate
`isPressed()` por um flag `atualizandoProgramaticamente`) + `docs/maps/*` regenerado
(porque o arquivo mudou de tamanho/estrutura de métodos).

## O que não pude verificar

- **Aparelho físico.** Nada aqui prova comportamento em um Galaxy S22 Ultra ou Tab S8
  Ultra reais — só em emulador (`swiftshader_indirect`), como o próprio plano prevê para
  esta etapa (`B-CONSOLE.md` §3, "depois das fatias").
- **HDR ligado.** Como o implementador já registrou, o emulador não reporta HDR10 nem
  decoder HEVC confiável — o switch aparece sempre desabilitado nas minhas fotos
  também. O comportamento do switch HDR **habilitado** (e a persistência dele por D-pad,
  que a mesma correção do §4 também cobre em teoria) não foi observado num aparelho que
  realmente suporte HDR.
- **Áudio 5.1/7.1**, **`checkbox_compose_bar` na tela de edição de perfil**: mesmas
  lacunas que o relatório do implementador já assinala — não naveguei para dentro do
  diálogo de áudio além do estado inicial, nem abri `EditProfileActivity` para
  fotografar a chave nova aparecendo num perfil (a leitura de código do overlay, §5,
  cobre a garantia de que ela não quebra nem é ignorada).
- **`GameMenu`/F2 lendo `checkbox_compose_bar`**: fora do escopo desta fatia, como a
  spec e o relatório dizem — não há comportamento de sessão para testar ainda.
