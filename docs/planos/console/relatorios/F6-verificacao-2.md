# F6 — Polimento contra o mockup e limpeza — verificação da 2ª rodada

Verificador independente da segunda implementação (`F6-implementacao-2.md`), depois da
devolução registrada em `F6-verificacao.md`. Worktree `Artemis-F6`, branch
`fatia/F6-polimento`, ~16 commits à frente de `dev` (`57b697c6`). Porta 5554, um AVD por
vez (`s22u` depois `tabs8u`, cada um derrubado com `adb emu kill` + `adb devices` vazio
antes do próximo e ao final).

## Veredito: **APROVADA**

O motivo da devolução anterior — faixa "N apps publicados · Recarregar" e banner de
retomar sessão na tela Apps do celular, ausentes do mockup — foi corrigido corretamente:
as duas funções continuam alcançáveis (retomar pelo selo "Em execução" na própria capa,
recarregar pelo menu de toque longo), sem reintroduzir a faixa. Nenhum defeito bloqueante
encontrado. Um achado pré-existente, fora do escopo desta fatia, é registrado no §6.

## 1. Comandos executados por mim

```
node tools/validate.mjs                 → passou (codemap OK, jni OK, build OK,
                                           test OK — 4 falha(s) preexistente(s), 0 novas)
node tools/screens.mjs                  → 30 telas em app/build/vidro-screens/, sem erro
./gradlew assembleNonRoot_gameDebug     → BUILD SUCCESSFUL (usei o APK já atualizado,
                                           UP-TO-DATE)
adb install (x86_64 debug) em s22u (5554) e depois em tabs8u (5554), um de cada vez
adb shell input tap/swipe + screencap manuais em ambos os AVDs
```

`JAVA_HOME=C:\Program Files\Java\jdk-21.0.12`, `ANDROID_HOME` apontado para o SDK do
usuário. Submódulo `moonlight-common-c` já inicializado (`git submodule status` sem `-`
na frente do hash).

## 2. Retomar e recarregar — prova direta por `adb`, não só leitura de código

Instalei o APK de debug (`app-nonRoot_game-x86_64-debug.apk`) no `s22u`, abri
`AppView --ez MoqueDeApps true` e testei eu mesmo:

- **Toque longo na capa "Área de trabalho" (a marcada "Em execução")** abre o menu de
  contexto com **"Retornar ao Jogo"** no topo e **"Recarregar"** no fim — foto
  `docs/planos/console/fotos/F6-2v-menu-s22u.png`. Isto prova que as duas ações
  continuam alcançáveis a partir da capa, como a regra "esconder pode, apagar não" exige.
- **Toquei em "Recarregar"**: o menu fecha sem crash (`adb logcat -d *:E` limpo depois),
  a grade permanece intacta — foto `F6-2v-after-reload2-s22u.png`. Bate com o relatado:
  é um no-op silencioso porque `managerBinder` é `null` no moque (o mesmo comportamento
  que o botão antigo tinha ali).
- **Confirmei a fixture (`popularAppsDeMentira`, `AppView.java`, commit `506bf400`)**: o
  trecho novo (13 linhas) só roda dentro do método privado que constrói a grade de
  mentira, copiando `AppObject#isRunning` para `lastRunningAppId` — o mesmo campo que
  `onItemClick`/`onCreateContextMenu` já liam antes desta fatia. Conferi
  `git diff 57b697c6 HEAD -- AppView.java`: nenhuma linha de `onItemClick`,
  `onCreateContextMenu` ou `onContextItemSelected` (a lógica real de decisão) foi tocada
  em nenhum dos dois rounds de F6 — só a fixture de depuração passou a alimentar um campo
  que já existia. **Não é um caso de lógica real mexida.**

**Achado ao testar (não bloqueia F6, ver §6)**: toquei também em "Retornar ao Jogo" no
mesmo menu — crasha com `NullPointerException` em
`ComputerManagerBinder.getUniqueId()` dentro de `ServerHelper.createStartIntent`
(chamado por `ServerHelper.doStart`, chamado por `AppView.onContextItemSelected` /
`onItemClick`). Confirmei por `git diff 57b697c6 HEAD` que esse caminho (`doStart`
chamado com `managerBinder` sem checar null) é **idêntico ao que já existia antes da
F6** — a única remoção de `doStart` nesta fatia foi o `onClickListener` da faixa antiga,
que tinha exatamente o mesmo problema em potencial. É um efeito do moque nunca
vincular ao `ComputerManagerService` (documentado em `AGENTS.md`: "`PcView` sai quase
vazia porque espera o `ComputerManagerService`"), não uma regressão desta fatia.

## 3. Auditoria das strings apagadas

Repeti a varredura, independente da 1ª verificação e do relatório do implementador:

```
grep -rn "vidro_published_apps|vidro_published_app_one|vidro_session_open|
           vidro_session_return|vidro_return"  app/src/main/res app/src/main/java
→ zero ocorrências (nenhum consumidor em Java, XML)

grep -rn "getIdentifier" app/src/main/java
→ uma ocorrência, Game.java:1018, comentário dizendo que a técnica NÃO é usada ali

grep -rl (as 5 strings) app/src/main/res/values-*
→ nenhum arquivo values-* (incluindo values-pt-rBR) ainda contém qualquer uma
```

Confirmado: as 5 strings saíram tanto de `values/strings.xml` quanto de
`values-pt-rBR/strings.xml`, e não sobrou consumidor em nenhuma linguagem. `vidro_reload`
foi mantida de propósito (reaproveitada pelo item "Recarregar" do menu) — confirmei que
`R.string.vidro_reload` é referenciada só em `AppView.java:561` (o novo `menu.add`).

## 4. Diff da tela Apps contra `B_Celular_Apps.png`

`activity_app_view.xml` (diff do commit `bcae9ff9`): removidos o `RelativeLayout` de
contagem/"Recarregar" e o `RelativeLayout` `vidroResumeStrip` inteiro. O cabeçalho vai
direto do título "Apps" para a grade — confirmei ao vivo no `s22u`
(`F6-2v-apps-s22u.png`): estrutura idêntica ao mockup, mesma ausência de faixa e banner,
selo "Em execução" só na própria capa (igual ao mockup, que também mostra "EM EXECUÇÃO"
como rótulo na capa, não como faixa separada).

## 5. "Encerrar no PC" — foco pré-API-23, sem regressão da técnica da F5

`console_guia.xml` (diff do commit `b5bc7a6e`): raiz do bloco `consoleBlocoEncerrar`
virou `FrameLayout` com `foreground`/`clickable`/`focusable`, conteúdo (ícone + texto)
movido para uma `LinearLayout` filha com `layout_gravity="center"` — evita exatamente o
acidente que a F5 pegou (View de foco por cima escondendo o controle vizinho), porque
aqui não há View irmã, só o `foreground` da própria raiz.

Confirmei por foto em dois lugares independentes:
- `tools/screens.mjs` → `04-sessao-guia.png` e `tab-04-sessao-guia.png`: "Disconnect" e
  "End on the PC" (renderização em inglês do Robolectric) completos, lado a lado, sem
  sobreposição nem corte.
- `F6-2v-inicio-tabs8u.png` (AVD `tabs8u`, `PcView` com moque): card "Continuar" mostra
  "Retomar" e "Encerrar no PC" (pt-BR) completos e lado a lado.

## 6. Sem regressão do que a 1ª rodada já tinha corrigido

- **Contorno de repouso da fileira "APPS" na Início (tablet)**: `console_capa_borda.xml` +
  `console_capa_carousel_item.xml` (commit `f6a5ead1`, da 1ª verificação) intactos —
  confirmei via `grep` e por foto ao vivo (`F6-2v-inicio-tabs8u.png`): as seis capas da
  fileira "APPS" têm canto arredondado visível em repouso, nenhuma colada na vizinha.
- **`focusableInTouchMode="true"`**: presente nos 10 blocos de `console_guia.xml` e nas 5
  teclas de `console_teclas_fileira.xml` — `grep -n` confirma, nenhum dos dois arquivos
  perdeu o atributo na 2ª rodada.

## 7. Gate

```
node tools/validate.mjs
```

Rodei eu mesmo, do zero:

```
[ OK ] codemap
[ OK ] jni
[ OK ] build
[ OK ] test — 4 falha(s) preexistente(s), nenhuma nova
validate: passou.
```

`node tools/screens.mjs` → 30 telas renderizadas sem erro em `app/build/vidro-screens/`.

## 8. Fotos capturadas nesta rodada (nos dois AVDs, um de cada vez)

- `docs/planos/console/fotos/F6-2v-apps-s22u.png` — Apps (celular), sem faixa/banner.
- `docs/planos/console/fotos/F6-2v-menu-s22u.png` — menu de toque longo com "Retornar ao
  Jogo" e "Recarregar".
- `docs/planos/console/fotos/F6-2v-after-reload2-s22u.png` — depois de tocar
  "Recarregar", sem crash.
- `docs/planos/console/fotos/F6-2v-inicio-tabs8u.png` — Início (tablet): fileira "APPS"
  com contorno de repouso, card "Continuar" com "Retomar"/"Encerrar no PC" lado a lado.

(Não recriei as folhas de comparação lado a lado `F6-comparacao-*.png` da 1ª rodada —
usei comparação visual direta contra `B_Celular_Apps.png`, suficiente para o único item
que motivou a devolução.)

## 9. Não verificável a partir daqui — honestamente

- **Nada disto prova comportamento em aparelho físico**, nem contra um host real
  (Vibeshine). Só os dois AVDs (`s22u`, `tabs8u`), moques, como toda a fatia.
- **"Recarregar" de verdade** (com `managerBinder` não-nulo, revalidando a lista contra
  um host real) não foi exercido — só a alcançabilidade e a ausência de crash no moque.
- **O NPE ao tocar em "Retornar ao Jogo" no moque** (§2) não foi investigado a fundo além
  de confirmar, por `git diff`, que o caminho de código é anterior a toda a F6 e não foi
  tocado por nenhuma das duas rodadas — é um comportamento do fixture de depuração
  (`managerBinder` nunca vinculado em modo moque), não uma regressão desta fatia. Fica
  registrado para quem quiser investigar `ServerHelper.doStart`/`AppView` como tarefa à
  parte; não bloqueia esta verificação porque está fora do escopo declarado de F6 e o
  código não mudou.
- Não testei D-pad/teclado físico — só toque (`adb shell input tap`/`swipe`).
- As 4 falhas de teste preexistentes não foram investigadas (mesmo estado de sempre).
- Não medi tolerância de dp/cor pixel a pixel — comparação visual direta contra os
  mockups e as fotos anteriores já aprovadas.

## 10. Commits

Nenhum commit novo nesta verificação — não encontrei defeito que exigisse correção.
Sem merge em `dev`, sem push.
