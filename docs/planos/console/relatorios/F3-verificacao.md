# F3 — Início (tablet) · Computadores e Apps (celular) — verificação

Verificador independente, sem contexto do implementador. Worktree
`C:\Users\Rafael\Documents\Projetos\Claude\Artemis-F3`, branch `fatia/F3-inicio`,
base `dev` = `fb0ae7ca`. Porta reservada: **5556**. Não toquei em `Artemis`, `Artemis-F1`,
`Artemis-F2`, nem no emulador `emulator-5554` (que tinha `tabs8u` de outra fatia
rodando — ver seção "O que não pude verificar").

O relatório do implementador (`docs/planos/console/relatorios/F3-implementacao.md`) foi
lido **por último**, depois da spec e dos mockups, e tratado como alegação a conferir —
não como fonte de verdade. Todas as alegações relevantes foram conferidas por conta
própria (diff, gate, fotos, `adb shell input`).

## O que executei

- `git diff fb0ae7ca..HEAD --stat` e leitura completa dos arquivos tocados.
- `node tools/validate.mjs` (duas vezes — antes e depois das minhas duas correções).
  `JAVA_HOME=C:\Program Files\Java\jdk-21.0.12`.
- `./gradlew :app:testNonRoot_gameDebugUnitTest` isolado, para inspecionar as 4 falhas
  que o gate resume numa linha só — não confiei no "4 falha(s) preexistente(s)" sem abrir
  os stack traces (o aviso do prompt sobre falso positivo do `validate.mjs` é real: a
  saída resumida esconde detalhe).
- `node tools/codemap/jni-check.mjs` isolado.
- `node tools/screens.mjs` (Robolectric, 29 telas, sem erro).
- `node tools/fotos.mjs --avd s22u --port 5556 --rotas computadores,apps,apps-foco`,
  **por conta própria**, no meu próprio processo de emulador (a instância anterior do
  implementador já não existia; subi a minha, fotografei, e ao final matei só a minha
  instância em `emulator-5556` antes de tentar o tablet).
- `adb -s emulator-5556 shell input keyevent 20` manualmente, repetidas vezes, para
  confirmar o comportamento do foco na grade de Apps do celular fora do script.
- Inspeção pixel a pixel das fotos gerada, comparando com
  `docs/design/console/fotos/B_Celular_Computadores.png` e `B_Celular_Apps.png`.
- **Não consegui** rodar `tools/fotos.mjs --avd tabs8u` eu mesmo nesta sessão — ver seção
  dedicada abaixo. Inspecionei as fotos do tablet que já existiam em
  `app/build/fotos/tabs8u/*.png` (geradas pelo implementador, timestamps 00:03–00:09,
  antes desta verificação começar) como evidência complementar, deixando claro que não
  foram capturadas por mim.

## 1. Diff contra a spec

Fora do escopo: **nada encontrado.** `Game.java`, `GameMenu.java`, `activity_game.xml` não
aparecem no diff. Nenhum arquivo de F4 (Ajustes completo), F5 (conectando/pareamento/queda)
ou F2 (sessão) foi tocado. `git diff --stat` mostra só `PcView.java`, `AppView.java`,
`grid/AppCarouselAdapter.java` (novo), `grid/PcGridAdapter.java`, `ui/MoqueDeApps.java`
(novo, extraído de `AppView`), os layouts de `PcView`/`AppView` nos dois buckets, os
drawables novos, `strings.xml`/`values-pt-rBR/strings.xml`, `tools/fotos.mjs` e
`docs/maps/*` (regenerado). Nenhum arquivo Kotlin, nenhum Compose.

**Reuso de `AppGridAdapter`/`CachedAppAssetLoader`: confirmado, não duplicado.**
`AppCarouselAdapter` (fileira do tablet) delega bind para
`AppGridAdapter#getView(int, View, ViewGroup)` passando a própria View como
`convertView` — o mesmo mecanismo de reciclagem que o `GridView` do celular já usa. Não
há cópia da lógica de cache/rede. `MoqueDeApps` foi extraído de dentro de `AppView`
(o método antigo `popularAppsDeMentira`/`criarCapaDeMentira` virou uma classe
compartilhada) exatamente para a Início do tablet reusar a mesma fixture sem duplicar —
conferi o diff de `AppView.java` linha a linha: o método antigo foi removido por
inteiro, substituído por uma chamada ao helper novo.

**Strings**: todas as 13 novas (`console_add_pc`, `console_estado_online`, …) existem em
`values/strings.xml` **e** `values-pt-rBR/strings.xml`, com o mesmo nome nos dois; nenhuma
vazou para outro `values-*` (`git diff --name-only` só lista esses dois arquivos de
strings). Todas as 13 são referenciadas em pelo menos um layout ou classe Java — nenhuma
órfã. A única string pré-existente que mudou de texto (`vidro_pill_open`: "aberto"→"Em
execução" / "open"→"Running") é um recurso já compartilhado por `AppGridAdapter` nas duas
plataformas, documentado no relatório do implementador e confirmado no diff.

**Cores**: todo `@color/` novo nos layouts tocados é um dos 13 tokens `console_*` (ou
`vidro_dot_on`, que já era alias de `console_acento` desde a F1). Sem hex novo em XML —
com uma exceção que virou DEFEITO 1 abaixo.

## 2. Defeitos encontrados

### Defeito 1 (corrigido por mim) — chevron do chip da Início usava cor errada

`console_ic_chevron.xml` é um vetor com `strokeColor="#FFFFFF"` (branco puro, fora dos 13
tokens), com o comentário do próprio arquivo dizendo que "Tint sempre por quem usa
(`console_texto_2` ou `console_acento`, conforme o papel do elemento)". Conferi as três
instâncias:

- `pc_grid_item.xml` (chevron do card de PC on-line, celular): `app:tint="@color/console_acento"` — bate com `Celular_Computadores.dc.html` linha 37 (`stroke="#7FE0C7"`). Correto.
- `activity_app_view.xml` (chevron "‹" do cabeçalho Apps, celular): `app:tint="@color/console_texto_2"` — bate com `Celular_Apps.dc.html` linha 24 (`stroke="#9A9AA5"`). Correto.
- `layout-sw600dp/activity_pc_view.xml` (chevron do chip do PC atual, tablet): estava `app:tint="@color/console_texto_2"`. O mockup (`Tablet_Inicio.dc.html` linha 29) mostra esse mesmo chevron em `stroke="#7FE0C7"` — **`console_acento`**, não `console_texto_2`.

Corrigi trocando o `app:tint` dessa terceira instância para `@color/console_acento`
(`app/src/main/res/layout-sw600dp/activity_pc_view.xml`). Recompilei
(`assembleNonRoot_gameDebug`, sucesso) e refotografei o celular para garantir que nada
regrediu (o celular não usa esse layout).

### Defeito 2 (corrigido por mim) — comentário prometia uma chamada que não existia

`console_borda_tracejada.xml` (borda tracejada do card "Adicionar PC") tem um comentário
explícito: "quem usa este drawable chama `view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)`
no código (ConsoleUi helper)". Não existe nenhum `ConsoleUi` no projeto e
`grep -rn "LAYER_TYPE_SOFTWARE"` não achava nenhuma ocorrência em `app/src/main/java/`.
`dashPathEffect` em aceleração de hardware normalmente degrada para traço sólido — um
risco real em hardware de verdade, mesmo que o emulador (SwiftShader) tenha desenhado
certo na minha foto sem a chamada. Adicionei a chamada que faltava em
`PcView.java#initializeViews`, no bloco que já teria essa view (`addPlace`):
`addPlace.setLayerType(View.LAYER_TYPE_SOFTWARE, null)`. Recompilei e confirmei visualmente
(computadores.png) que a borda tracejada continua tracejada, agora com a garantia da
chamada explícita em vez de depender do renderer do momento.

Depois das duas correções: `node tools/codemap/codemap.mjs` (mapas ficaram desatualizados
pelas minhas edições — regenerei) e `node tools/validate.mjs` de novo, verde.

### Defeito 3 (não corrigido, cosmético, não bloqueante) — card do PC on-line sem contorno de acento no celular

`B_Celular_Computadores.png` e `Celular_Computadores.dc.html` mostram o card do PC
**on-line** com um contorno inteiro em `console_acento` (verde), diferente do card
"fora do ar" (sem contorno colorido). A implementação usa `console_card_bg.xml` (mesmo
drawable, borda neutra `console_borda`) para os dois estados — só o ponto, o rótulo e o
`alpha` mudam. Foto minha (`app/build/fotos/s22u/computadores.png`) confirma: nenhum dos
dois cards tem contorno de acento.

Não corrigi porque não é um "pequeno e óbvio": exige uma decisão de produto (o contorno
de acento deveria aparecer em qualquer PC on-line, ou só no "atual" quando houver mais de
um on-line? a fatia não define isso) e um novo drawable condicional em
`PcGridAdapter.populateView`. Fica como item para o próximo passe (F4 ou uma correção
pontual), não bloqueia esta fatia — a informação de estado (ponto + rótulo) já está
correta e a regra "uma cor só" da `NOTA.md` (que é sobre o ponto, não a borda do card) foi
respeitada.

### Observação (não é defeito da F3) — ícone de play cobre o nome do app "em execução"

Nas fotos de Apps (celular e tablet), o card com `RUNNING`/`Em execução` mostra um ícone
de play grande sobreposto ao nome do app ("Ár[...]de trabalho" fica parcialmente coberto).
O mockup (`B_Celular_Apps.png`) não tem esse ícone — o card em execução lá só tem o rótulo
"EM EXECUÇÃO" no topo e o nome legível embaixo. Conferi: esse ícone de play e o
posicionamento do rótulo (canto superior direito, não superior esquerdo como no mockup)
vêm de `AppGridAdapter`/`vidro_pill_open`, um mecanismo que **já existia antes da F3** e que
o diff não toca (`AppGridAdapter.java` não aparece em `git diff --stat`). O relatório do
implementador já reconhece isso: só o texto da pílula mudou, não a estrutura. Como F3
manda reusar `AppGridAdapter` sem duplicar, redesenhar esse elemento seria sair do escopo
desta fatia. Registro para quem tocar esse componente depois, não como defeito da F3.

### Observação (flaky, não reproduzido) — uma captura isolada de `apps-foco` saiu com foco errado

Na minha primeira rodada de `tools/fotos.mjs --avd s22u --rotas computadores,apps,apps-foco`,
a foto `apps-foco.png` saiu com o foco ainda no primeiro item (deveria estar no terceiro,
"Corrida Noturna", depois de um `DPAD_DOWN`). Refiz a rota isolada 4 vezes seguidas
(`node tools/fotos.mjs --avd s22u --port 5556 --rotas apps-foco`) e as 4 saíram corretas
(foco em "Corrida Noturna"). Também reproduzi manualmentecom `adb shell input keyevent 20`
puro, com o mesmo resultado correto. Não consegui reproduzir a falha de novo — parece um
problema pontual de tempo na primeira execução (a rota anterior, `apps`, pode ter deixado
o processo num estado transitório), não um defeito de foco na grade em si. Registro para
quem mexer em `tools/fotos.mjs` depois, não bloqueia esta fatia.

## 3. Gate (rodado por mim, de ponta a ponta, depois das correções)

```
node tools/codemap/codemap.mjs   → 224 arquivos, 279 tipos, 2661 métodos, 30 pacotes
node tools/codemap/jni-check.mjs → 34 native · 34 símbolos C · 21 callbacks · fronteira íntegra
./gradlew :app:assembleNonRoot_gameDebug        → BUILD SUCCESSFUL
./gradlew :app:testNonRoot_gameDebugUnitTest    → 78 testes, 4 falharam, 4 skipped (VidroScreenshotTest, que exige `screens.mjs`/flag própria — esperado)
node tools/validate.mjs                          → "validate: passou." (codemap/jni/build/test todos OK)
node tools/screens.mjs                           → 29 telas, sem erro
```

As 4 falhas de teste **não são novas** — abri os stack traces (não confiei na linha
resumida do `validate.mjs`, seguindo o aviso do prompt sobre o falso positivo já visto
noutra fatia):

- `ProfilesNavigationTest.clickingProfileButton_launchesProfilesActivity[FromAppView]`:
  `ClassCastException` (`MaterialTextView` → `ImageButton`). Conferi
  `git show fb0ae7ca:app/src/main/res/layout/activity_pc_view.xml` — `profilesButton` já
  era `TextView` **antes** da F3. O teste está desatualizado em relação a uma mudança de
  UI anterior a esta fatia, não algo que a F3 quebrou.
- `SimpleStartupTest.testApplicationOnCreate` e `StartupTest.testApplicationStartup`:
  `NullPointerException` em `ProfilesManager.load`/`ArtemisApplication.onCreate`
  (`Context.getFilesDir()` com `mBase` nulo) — nada relacionado a layout, `PcView` ou
  `AppView`; é um problema de setup do `Application` em teste, alheio à UI da Console.

Nenhuma das quatro toca código que a F3 mudou.

## 4. Fotos — celular (`s22u`), geradas por mim, porta 5556

`app/build/fotos/s22u/computadores.png`, `apps.png`, `apps-foco.png`, `folha.png` (as três
rotas exigidas pelo "Pronto quando": Computadores, Apps, foco na 3ª capa via D-pad).

- **Computadores**: título, card ARTEMIS on-line (ponto acento, IP mono, chevron acento),
  card NOTEBOOK fora do ar (esmaecido, "address unavailable", sem chevron), card tracejado
  "Add PC" com o `+` em acento. Estrutura e tipografia batem com o mockup; contorno de
  acento do card on-line ausente — Defeito 3 acima.
- **Apps**: cabeçalho "‹ ARTEMIS · online", título "Apps", grade 2 colunas, badge
  Running no primeiro card (posição/ícone pré-existentes, ver observação acima).
- **Apps com foco (D-pad)**: um `DPAD_DOWN` a partir do primeiro item pousa no terceiro
  ("Corrida Noturna"), com contorno de foco visível — confirmado em 5 execuções (1 anômala
  isolada, 4 corretas, ver observação acima) e num teste manual com `adb shell input`.

## 5. Fotos — tablet (`tabs8u`): o que NÃO pude verificar, e por quê

**Não consegui subir o `tabs8u` nesta sessão.** Depois de fotografar o celular, matei
minha própria instância em `emulator-5556` (a única que era minha) e tentei
`node tools/fotos.mjs --avd tabs8u --port 5556 --rotas inicio,inicio-foco,inicio-dialogo,vazio,computadores,apps`.
A ferramenta relatou "`emulator-5556 não apareceu em 4 min`". Investigando:
`C:/Users/Rafael/.android/avd/tabs8u.avd/{hardware-qemu.ini,multiinstance}.lock` estavam
(e continuaram, até o fim desta verificação) presos por outro processo — confirmei via
`adb -s emulator-5554 emu avd name` → `tabs8u`, e
`adb -s emulator-5554 shell dumpsys activity activities | grep topResumedActivity` →
`com.limelight.noirdebug/com.limelight.Game` **ativo**, ou seja, a fatia paralela (F2,
dona da porta 5554) estava genuinamente usando o AVD, não um processo esquecido. Um AVD
não sobe duas vezes ao mesmo tempo. Segui a instrução do plano — "espere e registre, não
force" — e não matei o processo alheio. Esperei em três janelas de checagem (~10 min,
depois mais ~15 min, depois mais ~15 min — no total quase 40 minutos monitorando o
arquivo de lock) e ele nunca liberou.

**Isto não é um defeito da implementação da F3** — é a mesma situação que o próprio
implementador relatou (na direção oposta: ele não conseguiu o `s22u` porque a F2 o
segurava na 5554; eu não consegui o `tabs8u` pelo mesmo motivo, na mesma porta, com a F2
ainda ativa horas depois).

**Evidência que EXISTE, mas que eu não gerei nesta sessão**: `app/build/fotos/tabs8u/*.png`
(timestamps 00:03–00:09, de antes desta verificação começar) — inspecionei os PNGs
diretamente, pixel a pixel, em vez de confiar na prosa do relatório:

- `inicio.png`: cabeçalho com chip "ARTEMIS · online" (contorno de acento — vou confirmar
  que meu Defeito 1 não regride isto quando o AVD liberar, já que troquei só o chevron,
  não a borda do chip) + chip "NOTEBOOK" esmaecido + "Settings"; seção Continuar com
  "Área de trabalho", "ARTEMIS · Running", "Resume"/"Quit on PC"; fileira de 6 capas com
  badge Running na primeira; nome + "Open" abaixo. Estrutura bate com
  `Tablet_Inicio.dc.html`.
- `inicio-foco.png`: depois de 2 `DPAD_RIGHT`, o contorno e a escala de foco aparecem em
  "Corrida Noturna" (3ª capa), e o nome/o botão abaixo da fileira acompanham — bate com
  "foco movido por D-pad ×2" do "Pronto quando".
- `inicio-dialogo.png`: diálogo "Computers" com "ARTEMIS · online", "NOTEBOOK · offline",
  "+ Add PC" — bate com a spec do diálogo simples.
- `vazio.png`: "Looking around your network", cabeçalho de chip ausente — mesma
  linguagem do estado vazio do celular.

Estas quatro imagens são consistentes com uma implementação correta, mas **não foram
produzidas por mim** — não posso declarar que "verifiquei no tablet" no mesmo sentido que
fiz para o celular. Quem integrar esta fatia (ou eu, se o AVD liberar antes do merge)
deveria rodar
`node tools/fotos.mjs --avd tabs8u --port <porta livre> --rotas inicio,inicio-foco,inicio-dialogo,vazio`
mais uma vez, no mínimo para confirmar que a correção do Defeito 1 (chevron do chip agora
`console_acento`) renderiza como esperado — é uma mudança de uma linha, risco baixo, mas
não fotografada por ninguém ainda.

## 6. Limitações assumidas pelo implementador — julgamento

- **Fotos do celular ausentes**: eu resolvi (seção 4). Deixou de ser limitação.
- **Polling real (não-moque) de apps na Início do tablet não exercitado ponta a ponta**:
  aceitável. Não há host pareado disponível nesta rede para testar de verdade, e o código
  (`carregarAppsDoComputadorAtual`) espelha literalmente os métodos já testados de
  `AppView` (`populateAppGridWithCache`/`updateUiWithAppList`/`updateUiWithServerinfo`) —
  confirmei essa correspondência lendo os dois lados. Risco residual, mas do tamanho que
  a fatia consegue endereçar sem um Vibeshine de verdade.
- **`android:foreground` em `LinearLayout` não honrado abaixo da API 23**: aceitável, não
  bloqueante. Afeta três elementos secundários (`consolePcChipAtual`, `settingsButton`,
  linhas do diálogo) — perdem o anel de foco *visual* em API 21/22, continuam clicáveis e
  focáveis. O coração do foco da B — a navegação por D-pad na fileira de capas e nos
  cards de PC — usa a técnica robusta (`duplicateParentState` + View de overlay), que
  **não** tem essa limitação; confirmei isso lendo `console_capa_carousel_item.xml` e
  `pc_grid_item.xml`. American Fire TV/tablets modernos rodam API bem acima de 23; o risco
  real é baixo e documentado, não escondido.

## 7. Veredito

**APROVADA**, com ressalvas registradas:

1. Dois defeitos reais corrigidos nesta branch (chevron do chip da Início com cor errada;
   comentário prometendo uma chamada de `setLayerType` que não existia) — ambos com
   commit nesta branch, gate verde depois.
2. Um defeito cosmético não corrigido (card de PC on-line sem contorno de acento no
   celular) — não bloqueante, requer decisão de produto, registrado para o próximo passe.
3. Uma lacuna de verificação genuína e fora do meu controle: não consegui gerar as fotos
   do `tabs8u` eu mesmo nesta sessão porque o AVD ficou preso por uma instância ativa da
   fatia F2 na porta 5554 durante toda a janela de trabalho (quase 40 minutos de espera,
   sem forçar). A conclusão sobre o tablet se apoia em revisão de código linha a linha
   (que é sólida e bate com a spec) e na inspeção das fotos que já existiam em disco de
   uma captura anterior, não geradas por mim — deixo isso explícito porque
   "verificado sem foto de emulador não existe" é a regra do próprio plano.

Recomendação para o Integrador: antes (ou logo depois) do merge, rodar
`node tools/fotos.mjs --avd tabs8u --port <porta livre>` uma vez para fechar o item 3 —
é a única verificação fotográfica que falta, e o risco identificado nela (o fix do
chevron) é pequeno.
