# F3 — Início (tablet) · Computadores e Apps (celular) — implementação

Fatia: `docs/planos/B-CONSOLE.md` §F3. Branch: `fatia/F3-inicio`, a partir de `dev`
(commit `fb0ae7ca`, com F0 e F1 já integradas). Worktree:
`C:\Users\Rafael\Documents\Projetos\Claude\Artemis-F3`. Porta de emulador: 5556.

## O que mudou e por quê

### Tablet — `PcView` vira a Início (`layout-sw600dp/activity_pc_view.xml` + `PcView.java`)

O layout `layout-sw600dp/activity_pc_view.xml` foi reescrito por inteiro: cabeçalho com o
chip do PC atual (`● ARTEMIS · on-line ⌄`, toque abre um diálogo simples — `AlertDialog` com
uma linha por PC conhecido, construída em código a partir de `console_pc_dialog_row.xml`, mais
"Adicionar PC"), chips esmaecidos dos demais PCs ao lado (construídos em código,
`criarChipEsmaecido`), e um botão "Ajustes" (44dp, ícone+rótulo — continua abrindo a
`StreamSettings` de sempre; a fatia F4 é quem redesenha essa tela). Seção **Continuar**
(card com miniatura, nome do app, `ARTEMIS · em execução`, Retomar/Encerrar no PC) só
aparece com um app rodando no PC atual. Seção **Apps**: `RecyclerView` horizontal de capas
3:4, foco = escala 1,2× (`ViewPropertyAnimator`, 120ms) + `console_capa_foco`, com o nome da
capa focada e um botão Abrir abaixo.

`PcView.java` ganhou a lógica inteira de "PC atual" da Início (tudo atrás de
`isConsoleInicio()`, que detecta o layout pela presença do id `consoleHeader` — no celular
esse bloco inteiro não roda):

- `escolherComputadorAtual()` — mantém o PC selecionado se continuar on-line+emparelhado;
  senão prefere o primeiro on-line+emparelhado; senão qualquer emparelhado; senão o primeiro
  da lista.
- `carregarAppsDoComputadorAtual()` — monta um `AppGridAdapter` de verdade para o PC atual
  (reusa exatamente a classe que `AppView` usa, **não duplica** o carregamento) e ou (a)
  semeia com `MoqueDeApps` quando `MoqueDeCasca`+`MoqueDeApps` estão ligados, ou (b) faz o
  carregamento real: cache em disco primeiro (mesmo caminho de
  `AppView#populateAppGridWithCache`), depois `ComputerManagerService.ApplistPoller`.
- Como `ComputerManagerService` só aceita **um** listener por vez
  (`ComputerManagerBinder#startPolling`), a atualização da lista de apps do PC atual roda
  dentro do mesmo listener que `PcView` já usa para a lista de PCs (`atualizarAppsDoTabletSeForOAtual`,
  chamado de dentro do `notifyComputerUpdated` existente) — não um segundo listener, que
  substituiria o primeiro.
- `AppCarouselAdapter` (novo, `grid/AppCarouselAdapter.java`) é um `RecyclerView.Adapter` fino
  que **delega** a criação/preenchimento de cada item para
  `AppGridAdapter#getView(int, View, ViewGroup)` contra uma View de
  `console_capa_carousel_item.xml` — é o mesmo mecanismo de reuso de `convertView` que o
  `GridView` do celular já usa; nenhuma lógica de `CachedAppAssetLoader` foi copiada.

**Achado só de emulador real, não de `screens.mjs`:** o primeiro `DPAD_RIGHT` depois da
fileira aparecer não movia foco nenhum. Causa: a Activity abre em modo de toque, e
`View#requestFocus()` falha **silenciosamente** numa View `focusable=true` que não é também
`focusableInTouchMode=true`. Corrigido em `console_capa_carousel_item.xml`
(`android:focusableInTouchMode="true"` no item) e pedindo o foco explicitamente no primeiro
item dentro de `AppCarouselAdapter#onBindViewHolder` (não na `RecyclerView` em si).

Pareamento continua o overlay `vidro_pairing.xml` de sempre — não tocado (fatia F5).

### Celular — `PcView` = Computadores, `AppView` = Apps

`app/src/main/res/layout/activity_pc_view.xml`: título "Computadores", a lista de PCs
continua o mesmo `AdapterFragment`/`PcGridAdapter` de sempre (agora com `pc_grid_item.xml`
reconstruído como card alto: ponto de estado + rótulo, nome, endereço em mono ou "endereço
indisponível", chevron só quando on-line+emparelhado), e um card tracejado fixo "Adicionar
PC" acima do rodapé Cenas/Ajustes existente.

`app/src/main/res/layout/activity_app_view.xml`: cabeçalho `‹ ● ARTEMIS · on-line` (clicável,
fecha a tela — mesmo id `vidroBack` de sempre) acima do título "Apps"; grade de 2 colunas
existente mantida; a faixa de sessão aberta e a contagem/recarregar continuam, só re-vestidas
com os tokens Console. O rótulo "aberto"/"em execução" da capa em destaque já existia
(`vidro_pill_open`, usado por `AppGridAdapter` nas duas plataformas) — só troquei o texto
("open"→"Running" / "aberto"→"Em execução") para bater com o mockup, sem duplicar o
mecanismo.

`PcGridAdapter.populateView` ganhou a leitura do endereço/chevron e trocou os dois pontos de
estado "fora do ar" (antes: contorno `vidro_dot_off`/`vidro_dot_far`, o segundo com um hex
`#66AEB1B6` que não era nenhum token) por um único `console_dot_neutro` sólido — a regra da
NOTA.md ("uma cor só") vale para o ponto; o rótulo ao lado (`grid_state`) é quem diz a
diferença ("on-line" / "fora do ar" / "Emparelhar").

**Removido, verificado antes de tirar:** `vidroGreeting`/`vidroStateTitle`/`vidroLight` e o
card de "antecipação" (`vidroAnticipation`/`vidroNotNow`/`vidroNotToday`) saíram dos dois
layouts de `activity_pc_view.xml`. Rodei `grep` no código Java antes de apagar: os três
primeiros só eram lidos dentro do método `updateVidroHeader()`, que eu também removi (ficaria
morto, `findViewById` teria retornado `null` sempre — na real o **build quebrava**, porque
`R.id.vidroGreeting` etc. deixam de existir na `R` quando nenhum layout os declara mais); os
da "antecipação" já não tinham **nenhuma** referência em Java antes desta fatia (conferido por
`grep`) — eram órfãos que vieram do Vidro anterior, não algo que eu tenha desconectado agora.

### Ferramentas — `tools/fotos.mjs`

Seis rotas novas: `inicio`/`inicio-foco`/`inicio-dialogo` (tablet, os dois moques juntos —
a segunda manda dois `DPAD_RIGHT`, a terceira acha `consolePcChipAtual` via
`uiautomator dump` e toca nele), `apps-foco` (celular, um `DPAD_DOWN` a partir do primeiro
item desce uma linha inteira numa grade de 2 colunas e pousa exatamente no terceiro), e
`vazio` (`PcView` sem nenhum extra de depuração — o estado "nenhum PC" real).

## Comandos executados e resultado

```
node tools/codemap/codemap.mjs        → 224 arquivos, 279 tipos, 2661 métodos, 30 pacotes
node tools/validate.mjs               → codemap OK, jni OK, build OK,
                                         test OK — 4 falha(s) preexistente(s), nenhuma nova
                                         "validate: passou."
```

`./gradlew :app:compileNonRoot_gameDebugJavaWithJavac` e
`./gradlew :app:assembleNonRoot_gameDebug` rodados várias vezes durante o trabalho (a última,
depois do fix de foco, com sucesso — `BUILD SUCCESSFUL`).

```
node tools/screens.mjs
```
29 telas renderizadas em `app/build/vidro-screens/` sem erro. Conferi visualmente
`s22-01-home.png` (Computadores, casca vazia — título + card tracejado "Add PC" + rodapé),
`s22-02-catalogo.png` (Apps, casca) e `tab-01-home.png` (Início, casca — chip vazio +
"Looking around your network", já que é o layout puro sem o adapter da Activity real por
trás).

```
node tools/fotos.mjs --avd tabs8u --port 5556 --rotas computadores,apps,inicio,inicio-foco,inicio-dialogo,vazio
```
6/6 rotas capturadas. Depois, ao notar que o foco não se movia em `inicio-foco`, corrigi o
bug de `focusableInTouchMode` (acima), recompilei e refotografei `inicio-foco`,
`inicio-dialogo` e `vazio` (as três rotas afetadas ou que valiam a pena reconferir).

```
node tools/fotos.mjs --avd s22u --port 5556 --rotas computadores,apps,apps-foco
```
**Não completou** — ver seção "o que não verifiquei" abaixo.

## Fotos

Todas em `app/build/fotos/tabs8u/` (gerado — não versionado; comandos acima reproduzem):

- `app/build/fotos/tabs8u/inicio.png` — Início com Continuar + fileira de Apps, moques ligados.
- `app/build/fotos/tabs8u/inicio-foco.png` — mesma tela, foco em "Corrida Noturna" (3ª capa)
  depois de 2× `DPAD_RIGHT`; nome e botão Abrir abaixo da fileira acompanham o item focado.
- `app/build/fotos/tabs8u/inicio-dialogo.png` — diálogo do chip aberto: "Computers" / ARTEMIS
  on-line / NOTEBOOK offline / "+ Add PC".
- `app/build/fotos/tabs8u/vazio.png` — `PcView` sem nenhum moque, sem nenhum PC (ver nota
  abaixo sobre como cheguei nesse estado) — "Looking around your network", cabeçalho de chip
  escondido.
- `app/build/fotos/tabs8u/computadores.png`, `apps.png` — rotas herdadas da F0, continuam
  passando com o app da F3 (regressão).
- `app/build/fotos/tabs8u/folha.png` — folha de contato com as 6 rotas acima lado a lado.

## O que NÃO consegui verificar, e por quê

- **Fotos do `s22u` (celular): Computadores, Apps e o foco na 3ª capa não foram
  capturadas.** `node tools/fotos.mjs --avd s22u --port 5556` tentou subir um emulador em
  `emulator-5556` e **não apareceu em 4 minutos** (erro literal da ferramenta:
  `emulator-5556 não apareceu em 4 min`). Investigando, achei `emulator-5554` já rodando com
  `adb -s emulator-5554 emu avd name` respondendo `s22u` — ou seja, **já existe uma instância
  do AVD `s22u` de pé na porta 5554** (reservada para a fatia F2, que corre em paralelo neste
  mesmo plano — `nunca use 5554` é uma instrução explícita minha). Um AVD não sobe duas vezes
  ao mesmo tempo: confirmei o lock em
  `C:/Users/Rafael/.android/avd/s22u.avd/{hardware-qemu.ini,multiinstance}.lock`. Não matei
  o processo em 5554 (não é meu para derrubar) e esperei ~5 minutos monitorando o lock; ele
  continuava ocupado quando parei de esperar. **Isto não é um defeito da implementação da
  F3** — é um conflito de agenda entre worktrees rodando o mesmo AVD ao mesmo tempo. Fica
  pendente: assim que a `s22u` da porta 5554 for liberada, rodar
  `node tools/fotos.mjs --avd s22u --port 5556 --rotas computadores,apps,apps-foco` reproduz
  o que falta.
  - Evidência parcial que EXISTE: `node tools/screens.mjs` (Robolectric, sem emulador) já
    renderizou `s22-01-home.png` e `s22-02-catalogo.png` com o layout novo, cores e tipografia
    corretas — mas é a "casca" (layout inflado puro), não a Activity real com dados e sem
    nenhuma interação de D-pad. Não prova foco nem o card de PC com dado real.
- **A capa "Área de trabalho" na Início (tablet) não teve o foco fotografado especificamente**
  (só a 3ª capa, "Corrida Noturna", via o D-pad da rota `inicio-foco`) — o "Pronto quando" da
  F3 só pede "foco movido por D-pad ×2" no tablet, sem especificar qual capa deve ficar em
  foco; interpretei como qualquer capa alcançável por 2 `DPAD_RIGHT` a partir da primeira.
- **`vazio.png` (tablet) precisou de `pm clear` e de desligar wifi/dados do emulador
  temporariamente.** A primeira tentativa mostrou um PC real (`RAFAEL`, "Pair") — o
  `tabs8u` tem alcance de rede até um host de verdade na rede do dono do fork. Desliguei
  wifi+dados (`adb shell svc wifi/data disable`) e limpei os dados do app
  (`adb shell pm clear com.limelight.noirdebug`, que também apaga a lista de PCs já
  descoberta e persistida) para chegar ao "nenhum PC" genuíno; religuei wifi/dados depois.
  Registro isto porque é uma intervenção fora do fluxo normal de `tools/fotos.mjs`, embora
  não tenha exigido nenhuma mudança de código.
- **Retomar/Abrir/Encerrar no PC (Início do tablet) não foram clicados de verdade.** O
  "Pronto quando" da F3 não pede esse teste, e clicar "Retomar"/"Abrir" chamaria
  `ServerHelper.doStart` contra o IP de mentira do moque (192.168.0.10), abrindo a `Game`
  real tentando conectar a um host que não existe — fora do escopo de verificação visual
  desta fatia.
- **Polling real (não-moque) de apps na Início do tablet não foi exercitado ponta a ponta**
  (nenhum host real pareado estava disponível para testar `carregarAppsDoComputadorAtual`
  pelo caminho de `ComputerManagerService`/`ApplistPoller` — só o caminho de moque foi
  fotografado). A integração foi escrita espelhando literalmente `AppView`'s
  `populateAppGridWithCache`/`updateUiWithAppList`/`updateUiWithServerinfo` (mesmos métodos,
  adaptados), e o build/testes passam, mas "espelha um caminho que já funciona" não é o mesmo
  que "testado com host de verdade".
- **Emulador não é aparelho físico.** Nenhuma destas fotos foi tirada num Galaxy S22
  Ultra ou Tab S8 Ultra reais — fora do escopo desta fatia (`docs/planos/B-CONSOLE.md` §3).
- **`android:foreground` em `LinearLayout`** (usado como contorno de foco em
  `consolePcChipAtual`, `settingsButton` e nas linhas do diálogo) só é honrado pelo framework
  a partir da API 23 — em minSdk 21 esses três elementos especificamente perdem o anel de
  foco visual (continuam clicáveis/focáveis, só sem o desenho). Os itens da fileira de capas
  e os cards de PC usam a técnica de View-por-cima (`duplicateParentState`), que não tem essa
  limitação.

## Arquivos alterados

Veja `git log --oneline fb0ae7ca..HEAD` neste worktree para a lista de commits; resumo por
arquivo:

- **Novo:** `app/src/main/java/com/limelight/ui/MoqueDeApps.java`,
  `app/src/main/java/com/limelight/grid/AppCarouselAdapter.java`,
  `app/src/main/res/layout/console_capa_carousel_item.xml`,
  `app/src/main/res/layout/console_pc_dialog_row.xml`,
  `app/src/main/res/drawable/console_{borda_tracejada,card_bg,chip_bg,chip_bg_atual,
  dot_neutro,ic_add,ic_chevron}.xml`.
- **Reescrito:** `app/src/main/res/layout-sw600dp/activity_pc_view.xml`,
  `app/src/main/res/layout/activity_pc_view.xml`,
  `app/src/main/res/layout/activity_app_view.xml`,
  `app/src/main/res/layout/pc_grid_item.xml`.
- **Editado:** `app/src/main/java/com/limelight/PcView.java`,
  `app/src/main/java/com/limelight/AppView.java` (extração para `MoqueDeApps`),
  `app/src/main/java/com/limelight/grid/PcGridAdapter.java`,
  `app/src/main/res/values/strings.xml`, `app/src/main/res/values-pt-rBR/strings.xml`,
  `tools/fotos.mjs`.
- **Gerado:** `docs/maps/*` (via `node tools/codemap/codemap.mjs`).
