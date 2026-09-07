# F5 — Conectando · emparelhar · queda · adicionar PC — verificação

Verificador independente, sem contexto do implementador. Worktree
`C:\Users\Rafael\Documents\Projetos\Claude\Artemis-F5`, branch `fatia/F5-telas`, base
`dev`, 5 commits à frente. Porta reservada: **5554**. Não toquei em `Artemis` (principal)
nem em outro `Artemis-*`. Um emulador por vez: `s22u` subiu, foi usado, foi morto
(`adb -s emulator-5554 emu kill`) e `adb devices` confirmado vazio antes de subir
`tabs8u`; `tabs8u` foi morto ao final, `adb devices` vazio na checagem final.

`docs/planos/console/relatorios/F5-implementacao.md` foi lido **por último**, depois de
`AGENTS.md`, `B-CONSOLE.md` (§0, §1, §F5 inteira) e `TOKENS.md`, e tratado como alegação
a conferir, não como fonte de verdade. Toda alegação relevante foi conferida por conta
própria: diff completo contra `dev`, gate, `screens.mjs`, fotos nos dois AVDs, e uma
passada de D-pad via `adb shell input keyevent` que o implementador tinha admitido não
ter feito.

O submódulo `moonlight-common-c` já estava inicializado neste worktree (herdado do
trabalho do implementador) — não precisei rodar `git submodule update --init --recursive`.

## O que executei

- `JAVA_HOME="C:\Program Files\Java\jdk-21.0.12"`, `ANDROID_HOME` apontando para o SDK do
  usuário, em todos os comandos.
- `git diff dev..HEAD --stat` e leitura completa de cada arquivo tocado (os quatro
  layouts, os três drawables reteamados, `console_foco_botao.xml`, `Game.java`,
  `PcView.java`, `VidroSession.java`, `tools/fotos.mjs`).
- `node tools/validate.mjs` — duas vezes: uma no estado do implementador, outra depois da
  minha correção (§4). As duas: `[OK] codemap`, `[OK] jni`, `[OK] build`,
  `[OK] test — 4 falha(s) preexistente(s), nenhuma nova` (mesmo baseline que o
  implementador relatou).
- `node tools/screens.mjs` — duas vezes (antes e depois da correção); 31 telas em
  `app/build/vidro-screens/` nas duas rodadas, sem erro, incluindo `03-conectando.png`,
  `07-pareamento.png`/`s22-07-pareamento.png`, `09-endereco.png`, `09-perdemos-o-host.png`.
- `node tools/fotos.mjs --avd s22u --port 5554 --rotas adicionar,sessao-conectando,sessao-queda`
  e o mesmo para `--avd tabs8u`, por conta própria, nos meus próprios processos de
  emulador (subi e derrubei os dois eu mesmo). Rodei de novo depois da correção para
  confirmar que nada regrediu.
- `adb shell input keyevent` (D-pad e Enter) e `adb shell dumpsys input_method` (para ler
  `mServedView`, a prova objetiva de qual view tem o foco) em `AddComputerManually`
  (própria Activity) e em `Game` com `MoqueEstado=conectando`/`queda`, nos dois AVDs.
- `./gradlew :app:assembleNonRoot_gameDebug` e `adb install -r` para instalar meu próprio
  build corrigido (não apenas o do implementador).

## Veredito: **DEVOLVIDA → corrigida nesta sessão → APROVADA**

Um defeito real, pequeno e óbvio (§4: anel de foco invisível nos botões
`ConsoleBotao.Primario`), corrigido e commitado nesta branch. Um achado importante e
**não é defeito desta fatia** (§5: D-pad não alcança os botões de `vidro_connecting`/
`vidro_lost` porque `Game.java` intercepta toda tecla para o stream — arquitetura
pré-existente, fora do escopo de "não mexa em rede/protocolo"). A `VidroLightView` não
está órfã (§6). Nada de Kotlin/Compose, nenhuma cor fora de `console_*`/tokens já
documentados, nenhuma string nova, nada de F2/F3/F4 tocado além do necessário.

## 1. Diff contra a spec

Conferido `git diff dev..HEAD` arquivo por arquivo:

- **`vidro_connecting.xml`**: fundo `console_fundo` ✅, título `ConsoleText.Titulo` ✅,
  `VidroLightView` removida e trocada por `View` de 10dp com `vidro_dot_off`/`vidro_dot_on`
  ✅, botão Cancelar em `ConsoleBotao.Neutro` ✅. `VidroSession.onStageStarting` continua
  disparando o mesmo evento real (só trocou `setIntensity` por `setBackgroundResource`) —
  mecânica preservada.
- **`vidro_pairing.xml`**: fundo `console_fundo` ✅, `ConsoleText.Legenda`/`.Mono`/
  `.Rotulo2` aplicados corretamente, os quatro dígitos em `ConsoleText.Mono` com
  `textSize="32sp"` (mono grande, como a spec pede) ✅, `VidroLightView` removida sem
  substituto — o `vidroPairWaitDot` que já existia (sempre `vidro_dot_on`) cumpre o papel
  do "ponto de estado simples (acento = ativo)" pedido pela spec; duplicar seria redundante,
  concordo com a leitura do implementador. Cancelar em `ConsoleBotao.Neutro` ✅.
  `PcView.java` só trocou a chamada de `VidroLightView.setIntensity` por um comentário —
  nenhuma linha de lógica de pareamento tocada.
- **`vidro_lost.xml`**: fundo `console_fundo` (era `vidro_scrim`) ✅, título
  `ConsoleText.Titulo` ✅, Tentar de novo em `ConsoleBotao.Primario`, Sair em
  `ConsoleBotao.Neutro` ✅. Nunca teve `VidroLightView` — nada a checar aí.
- **`activity_add_computer_manually.xml`**: fundo `console_fundo` ✅, título
  `ConsoleText.Titulo` (`manuallyAddPcText`) ✅, campo de IP em `ConsoleText.Mono` ✅
  (endereço IP é o próprio exemplo de uso do Mono na `TOKENS.md`), botão Conectar em
  `ConsoleBotao.Primario` ✅. `VidroLightView` removida sem substituto — concordo: ela era
  puramente decorativa ali (intensidade fixa, nenhum Java lendo o id), não havia estado
  real para um ponto representar.
- **Drawables reteamados** (`vidro_code_box.xml`, `vidro_field.xml`,
  `vidro_progress_track.xml`): confirmei com `grep -rl` que os três só são referenciados
  dentro das quatro telas desta fatia — sem risco de regressão cruzada. Cores agora vêm
  de `console_superficie`/`console_borda`/`console_acento`, sem hex novo.
- **`Game.java`/`VidroSession.java`/`PcView.java`**: só a costura de `MoqueEstado`
  (debug-only, ver §3) e a troca de `VidroLightView` por `View`+`setBackgroundResource`.
  Nenhuma linha de `nvstream/`, `crypto/`, protocolo de pareamento ou handshake de
  conexão foi tocada — confirmado lendo o diff inteiro, não só o resumo.

Estrutura e mecânica preservadas: é troca de pele, como a spec exige.

## 2. Passada de D-pad (a que o implementador não fez)

Fiz a passada com `adb shell input keyevent` nos dois AVDs, usando
`adb shell dumpsys input_method | grep mServedView` como prova objetiva de qual view tem
o foco (mais confiável que "parece focado" na screenshot).

**`activity_add_computer_manually.xml` (própria Activity, sem interceptação de tecla)**:
D-pad **funciona** mecanicamente — `KEYCODE_DPAD_DOWN` move o foco do campo de IP para o
botão Conectar (`mServedView` muda para `addPcButton`), e `KEYCODE_ENTER` ativa o clique
(toast "You must enter an IP address" apareceu, prova de que o `OnClickListener` disparou).
**Mas o anel de foco não aparecia na tela** — ver §4, corrigido.

**`vidro_connecting.xml` e `vidro_lost.xml` (dentro de `Game`, via `MoqueEstado`)**:
D-pad **não alcança nenhum dos botões**. Testei nos dois AVDs: com `MoqueEstado=queda`
(tela "Perdemos o PC"), `mServedView` fica travado em `com.limelight.ui.StreamContainer`
antes e depois de `KEYCODE_DPAD_RIGHT` — o foco nunca sai do container de vídeo para
`vidroLostRetry`/`vidroLostLeave`. Mesmo resultado em `vidro_connecting` com
`MoqueEstado=conectando`. Isto é esperado ao ler `Game.handleKeyDown` (linha ~2519):
toda tecla que não é de um dispositivo de controle "real" cai no ramo de teclado do
stream (`keyboardTranslator.translate(...)` → `conn.sendKeyboardInput(...)`) e o método
**sempre retorna `true`**, ou seja, a tecla nunca chega ao sistema de foco do Android.
Isso já era assim **antes** desta fatia (o `vidro_connecting.xml` de `dev` também não
tinha `focusable`/`clickable` no botão Cancelar, e o mecanismo de interceptação de
`Game.java` não foi tocado no diff) — não é regressão da F5, é limite arquitetural que a
spec proíbe esta fatia de mexer ("nada de rede... protocolo pode ter sido tocado"). Ver
§5 para o registro formal do achado.

**Fileira de dois botões da tela de queda** (o bug que o implementador achou e corrigiu):
confirmei nos dois AVDs, via foto (`sessao-queda.png` de `s22u` e `tabs8u`), que "Try
again" e "Leave" aparecem **os dois visíveis, lado a lado, sem sumiço** — o bug do
`FrameLayout` `wrap_content` que ele descreve está de fato corrigido. "Alcançáveis por
D-pad" no sentido estrito não pude confirmar (ver parágrafo acima) — mas isso vale para
os dois botões igualmente, não é um problema específico de um dos dois.

## 3. `MoqueEstado` é debug-only de verdade?

Sim. `Game.EXTRA_MOQUE_ESTADO` só é lido dentro do bloco `if (emModoDeMoque())`
(linha 962), e `emModoDeMoque()` (linha 208) é
`return BuildConfig.DEBUG && getIntent().getBooleanExtra(EXTRA_MOQUE, false);` — mesma
trava que já protegia `EXTRA_MOQUE` antes desta fatia. Sem o extra `MoqueDeStream=true`,
`MoqueEstado` nunca é lido, então não muda nada em build de release nem em uma conexão
real sem o extra. Ponto de costura correto.

## 4. Defeito encontrado e corrigido: anel de foco invisível em `ConsoleBotao.Primario`

**Sintoma**: em `activity_add_computer_manually.xml`, mover o foco por D-pad até o botão
"Connect" (`ConsoleBotao.Primario`) e confirmar por `dumpsys input_method` que o foco
estava lá (`mServedView=...addPcButton`) — mas a tela não mostrava nenhum anel, nenhuma
mudança visual. `KEYCODE_ENTER` confirmava que o botão estava de fato ativo (disparou o
toast de validação), então o foco **funcionava**, só não era **visível** — viola
diretamente o item "confirme que o foco é visível" da tarefa.

**Causa**: `console_foco_botao.xml` (novo desta fatia) desenha o anel/borda de foco em
`console_acento` (o teal `#7FE0C7`) sobre um botão cujo **próprio fundo** já é
`console_acento` sólido (`ConsoleBotao.Primario`, via `console_botao_primario_bg.xml` da
F1). Anel e fundo ficam exatamente da mesma cor — o anel existe e é desenhado, mas é
opticamente indistinguível do preenchimento por baixo. Confirmado por captura de tela e
crop 2× antes/depois (nenhuma foto anexada aqui além da descrição — as capturas ficaram
em `app/build/fotos/` e no diretório de scratchpad, não versionadas).

Passo para reproduzir (antes da correção): abrir `AddComputerManually`, fechar o teclado
(`KEYCODE_BACK`), `adb shell input keyevent KEYCODE_DPAD_DOWN`, `adb shell dumpsys
input_method | grep mServedView` confirma foco no `addPcButton`, mas o screenshot não
mostra nenhum traço de anel.

**Correção aplicada** (commitada nesta branch): criei
`app/src/main/res/drawable/console_foco_botao_sobre_acento.xml`, mesma composição do
`console_foco_botao.xml` só trocando a cor do anel/borda para `console_acento_sobre` —
token que a própria `TOKENS.md` já define como "texto/ícone sobre superfície de acento",
ou seja, desenhado para ter contraste em cima do `console_acento`. Nenhuma cor nova: só
troquei qual token já existente o drawable usa, mesma conta de opacidade de 15% que
`console_foco_botao.xml`/`console_foco.xml` já usam (`console_acento_sobre` a 15% =
`#260A1F1A`). Apliquei esse novo drawable só nos dois `ConsoleBotao.Primario` desta
fatia: `vidroLostRetry` (`vidro_lost.xml`) e `addPcButton`
(`activity_add_computer_manually.xml`). Os `ConsoleBotao.Neutro` (Cancelar em
`vidro_connecting`/`vidro_pairing`, Sair em `vidro_lost`) continuam com
`console_foco_botao.xml` original — o fundo deles é `console_superficie` escuro, onde o
anel em `console_acento` já contrasta normalmente (não teria como reproduzir o mesmo
sintoma sem um moque de pareamento/queda real para testar ao vivo, mas a lógica de cor é
direta: teal sobre `#16161A` sempre contrasta).

**Verificado depois da correção**: rebuild (`./gradlew :app:assembleNonRoot_gameDebug`),
reinstalei no emulador, repeti a mesma passada de D-pad em `AddComputerManually` nos dois
AVDs — o anel escuro (`console_acento_sobre`) agora aparece claramente contornando o
botão "Connect" quando focado, nos dois. `node tools/validate.mjs` e
`node tools/screens.mjs` rodados de novo depois da correção — gate ainda verde, nenhuma
tela quebrou.

## 5. Achado (não é defeito desta fatia): D-pad não alcança os botões dentro de `Game`

Registrado para o dono decidir se vira trabalho de outra fatia/época: `vidro_connecting`
e `vidro_lost` vivem dentro de `Game`, cujo `handleKeyDown` intercepta toda tecla que não
vem de um controle físico reconhecido e a envia como entrada de teclado para o stream —
o método sempre retorna `true`, então o foco do Android nunca chega às views da camada de
overlay. Isso é anterior a esta fatia (confirmado: o `vidro_connecting.xml` de `dev` já
não tinha `focusable`/`clickable` no botão Cancelar, e o diff desta fatia não toca em
`handleKeyDown`/`onKeyDown`/`dispatchKeyEvent`). A spec de F5 proíbe explicitamente mexer
em rede/protocolo, e a interceptação de teclado do `Game.java` está nessa fronteira — não
é razoável pedir ao implementador desta fatia que resolvesse isso. Também é hoje
parcialmente moot para `vidro_lost`: não existe caminho de produção que mostre essa tela
(a queda de rede real não está religada — `PcView`/`Game` não chamam nada que mostre
`vidroLost` fora do `MoqueEstado` de depuração desta fatia), então "alcançável por D-pad
numa queda real" não é verificável nem pelo lado da rede nem pelo lado do foco hoje.
`vidro_pairing.xml` (dentro de `PcView`, que não tem essa interceptação) deveria navegar
normalmente por D-pad — não consegui provar isso ao vivo porque não existe moque para
pareamento (só um host real gera essa tela), mas por leitura de código `PcView.java` não
sobrescreve `onKeyDown`/`dispatchKeyEvent`, então nada ali deveria bloquear o foco padrão
do Android como acontece em `Game`.

## 6. `VidroLightView` órfã?

**Não está órfã.** Ela saiu de três das quatro telas desta fatia (`vidro_connecting`,
`vidro_pairing`, `activity_add_computer_manually`), mas continua em uso real em
`app/src/main/res/layout/activity_profiles.xml` e `app/src/main/res/layout/
activity_stream_settings.xml` — fora do escopo de F5. A classe
`app/src/main/java/com/limelight/ui/VidroLightView.java` e o `declare-styleable
VidroLightView` em `values/styles.xml` continuam com consumidor. Nada a decidir aqui: a
classe permanece viva por outras telas, e a fatia F5 não tinha motivo para tocá-la.

Também confirmei o achado do próprio implementador sobre `vidroLostRetry`/
`vidroLostLeave`/`vidroLostRetryCount`: `git log --all --oneline -S"R.id.vidroLost"`
aponta para o commit `62c6e89b` ("Archive the Vidro redesign as it stood on 4
September"), que arquivou essa tela deliberadamente — não é recurso esquecido, é decisão
anterior do dono do fork. A fatia não reativou a lógica de queda real, só a pele —
consistente com o escopo.

## 7. Fronteiras

- **Kotlin/Compose**: nenhum arquivo `.kt` no diff; todas as mudanças são Java/XML.
- **Cores**: `grep` em todos os arquivos tocados não encontrou hex fora dos já
  documentados pela `TOKENS.md`/F1 (`#267FE0C7` já existia como padrão em
  `console_foco.xml`; o `#260A1F1A` que introduzi segue a mesma fórmula de opacidade
  sobre um token já existente, não é cor nova).
- **Strings**: nenhuma string nova — confirmado que `values/strings.xml` e
  `values-pt-rBR/strings.xml` não aparecem no diff. As quatro telas reusam strings já
  existentes.
- **F2/F3/F4**: `git diff dev..HEAD --stat` mostra só os arquivos das quatro telas de F5,
  `Game.java`/`PcView.java`/`VidroSession.java` (na costura mínima), `tools/fotos.mjs`,
  mapas gerados e o relatório do implementador. Nada de `console_barra_texto.xml`
  (F2), listas/grade de apps (F3) ou `StreamSettingsBasico`/Ajustes (F4) foi tocado.

## 8. Gate

```
node tools/validate.mjs
[ OK ] codemap
[ OK ] jni
[ OK ] build
[ OK ] test — 4 falha(s) preexistente(s), nenhuma nova
validate: passou.
```

Rodado duas vezes (antes e depois da minha correção do §4), mesmo resultado. As 4 falhas
preexistentes batem com o baseline que o implementador já tinha relatado
(`tools/test-baseline.json`) — nenhuma nova, nada de `@Ignore`, assert enfraquecido ou
teste apagado.

```
node tools/screens.mjs
→ 31 tela(s) em app/build/vidro-screens/, sem erro, nas duas rodadas (antes/depois da
  correção) — incluindo as quatro desta fatia (03-conectando, 07-pareamento/
  s22-07-pareamento, 09-endereco/00-endereco-viva, 09-perdemos-o-host).
```

## 9. Fotos, nos dois AVDs, por conta própria

```
node tools/fotos.mjs --avd s22u --port 5554 --rotas adicionar,sessao-conectando,sessao-queda
→ 3/3 rota(s) capturada(s) em app/build/fotos/s22u (rodado duas vezes, antes/depois da
  correção — mesmo resultado, sem regressão visual)

node tools/fotos.mjs --avd tabs8u --port 5554 --rotas adicionar,sessao-conectando,sessao-queda
→ 3/3 rota(s) capturada(s) em app/build/fotos/tabs8u
```

Um emulador por vez: subi `s22u` (`adb devices` vazio antes), usei, matei
(`emu kill`), confirmei `adb devices` vazio, subi `tabs8u`, usei, matei ao final,
confirmei `adb devices` vazio de novo.

As quatro telas aparecem nas fotos e conversam visualmente com o resto do app: fundo
`console_fundo` preto uniforme, Manrope no título, JetBrains Mono no código/endereço,
`console_acento` só em ativo/foco/botão primário, nenhum azul, mesma linguagem que
Início/Apps/Ajustes já fotografados nas fatias anteriores. No S22 Ultra, `sessao-queda`
mostra "Try again" e "Leave" lado a lado, os dois legíveis e sem sumiço (o bug que o
implementador corrigiu). No Tab S8 Ultra (paisagem, sem overlay de controle virtual por
ser tablet), a mesma tela mostra os dois botões centralizados sobre fundo preto, também
sem sumiço. `adicionar.png` nos dois AVDs mostra o layout coerente com a Console, teclado
do sistema aberto (comportamento herdado, não desta fatia).

## O que NÃO fica provado aqui

- **Aparelho físico.** Nenhuma foto veio de um Galaxy S22 Ultra ou Tab S8 Ultra reais —
  só emuladores, como o plano prevê para esta etapa.
- **Rede real.** `MoqueEstado=conectando`/`queda` fotografam a pele sobre uma conexão
  simulada; nenhuma tela desta fatia foi vista atrás de uma conexão, pareamento ou queda
  de rede genuínos. `vidro_pairing.xml` especificamente não foi vista atrás de um
  pareamento de verdade nesta verificação nem na implementação — só via
  `tools/screens.mjs` (inflação de layout) e por leitura de código.
- **D-pad físico num aparelho com teclado (Tab S8 Ultra com capa-teclado).** Testei via
  `adb shell input keyevent`, que é o melhor proxy disponível num emulador, mas não é o
  mesmo canal de um teclado Bluetooth/USB real.
- **Alcançabilidade por D-pad de `vidro_connecting`/`vidro_lost` numa sessão real.** Ver
  §5 — arquiteturalmente bloqueado por `Game.handleKeyDown`, pré-existente, fora do
  escopo desta fatia para corrigir.
- **`vidro_pairing.xml` com D-pad ao vivo.** Sem moque de pareamento, não pude confirmar
  na prática que o foco se move entre os controles dessa tela — só a leitura de código
  de `PcView.java` (sem interceptação de tecla) sustenta a expectativa de que funcione.

## Arquivos alterados nesta verificação

- **Novo:** `app/src/main/res/drawable/console_foco_botao_sobre_acento.xml`,
  `docs/planos/console/relatorios/F5-verificacao.md`.
- **Editado:** `app/src/main/res/layout/vidro_lost.xml` (só o `android:foreground` de
  `vidroLostRetry`), `app/src/main/res/layout/activity_add_computer_manually.xml` (só o
  `android:foreground` de `addPcButton`).

Sem merge em `dev`, sem push.
