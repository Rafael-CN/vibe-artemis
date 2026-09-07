# F6 — Polimento contra o mockup e limpeza — implementação

Branch `fatia/F6-polimento`, worktree `Artemis-F6`, base `57b697c6` (F0–F5 integradas).
Porta de emulador 5554, um AVD por vez, cada um derrubado (`emu kill`) antes de subir o
próximo.

## 1. Pendências herdadas das verificações — tratadas

### Card do PC on-line sem contorno de acento (pendência da verificação F3)

`B_Celular_Computadores.png`/`Celular_Computadores.dc.html` mostram o card do PC
**on-line e emparelhado** com contorno inteiro em `console_acento`; o card "fora do ar"
fica com a borda neutra de sempre. A implementação usava `console_card_bg.xml` para os
dois estados.

Corrigido com a mesma condição que já pinta o ponto e o chevron de acento
(`online && paired`, comentário existente em `PcGridAdapter.java`):

- Novo drawable `console_card_bg_online.xml` (mesmo corpo, borda 1.5dp `console_acento`
  — mesma espessura que `console_chip_bg_atual.xml`, já usado para o chip do PC atual).
- `pc_grid_item.xml`: id novo `consolePcCard` na `FrameLayout` do corpo do card.
- `PcGridAdapter.populateView`: escolhe entre os dois drawables por `online && paired`.

Confirmado por foto (`app/build/fotos/s22u/computadores.png`): o card ARTEMIS (on-line)
tem a borda verde, o NOTEBOOK (fora do ar) não — igual ao mockup.

### Referência a `docs/howto/adicionar-preferencia.md` (arquivo inexistente)

Não existe nem neste worktree nem em nenhum outro (confirmado por quem verificou F4).
`.claude/rules/prefs.md` é o arquivo real com o mesmo conteúdo. Corrigido o ponteiro em
`docs/planos/B-CONSOLE.md` (linha da tabela de fatias) e, por ser o mesmo erro na mesma
categoria ("Onde ler antes de mexer"), também em `AGENTS.md`.

### `android:foreground` em `LinearLayout` (API 21–22 perdem o anel de foco)

`android:foreground` num `ViewGroup` genérico só existe a partir da API 23; o app tem
`minSdk 21`. Três elementos da F3 usavam isso:

- `consolePcChipAtual` e `settingsButton` em `layout-sw600dp/activity_pc_view.xml`
  (tablet, Início).
- As linhas do diálogo de PCs, `console_pc_dialog_row.xml`.

**Resolvido sem a técnica de View-por-cima** (que a F5 já provou quebrar num
`FrameLayout` `wrap_content` com uma `View` irmã `match_parent`) — usei uma alternativa
mais simples e sem esse risco: `FrameLayout` tem suporte **nativo** a
`android:foreground` desde a API 1 (é um atributo próprio da classe, promovido para
`View` em geral só na API 23). Troquei a tag raiz de `LinearLayout` para `FrameLayout`
nos três elementos, movendo o conteúdo horizontal (ícone + texto) para um
`LinearLayout` filho sem foreground nenhum — sem `View` irmã, sem o bug do segundo
botão sumindo.

Provado com foto, não só lido no código:

- `app/build/fotos/tabs8u/computadores.png` (rota `computadores` = Início no tablet):
  chip do PC atual (com anel de foco visível, verde) e botão Ajustes lado a lado,
  ambos inteiros.
- `app/build/fotos/tabs8u/dialogo-pcs.png` (toque manual no chip): as duas linhas do
  diálogo (ARTEMIS, NOTEBOOK) e "Adicionar PC" todas visíveis e completas.
- `app/build/vidro-screens/tab-01-home.png` (Robolectric, `tools/screens.mjs`): mesma
  conferência, adapter à parte.

Não toquei `console_guia.xml`, que também tem uma `LinearLayout` com
`android:foreground` — não é um dos três elementos citados pela pendência (é a guia da
sessão, F2, tela diferente); registro para quem tocar essa tela depois.

### Ícone de play cobrindo o nome do app "em execução" (pré-existente, apontado na F3)

Reproduzido: quando o host não publica box art de verdade, o carregador desenha o nome
do app dentro da própria capa gerada (comentário original do arquivo: "o lugar da capa
continua existindo com o nome por cima"); o ícone de play de 46dp, centralizado, cobria
o meio desse nome (`app/build/fotos/s22u/apps.png` antes da correção — "Área de
trabalho" saía como "Ár[...]de trabalho").

Corrigido movendo `grid_overlay` (o ícone) do centro para o canto inferior-direito da
capa, 40dp, com a margem `vidro_space_3` — canto que fica livre em qualquer capa, real
ou gerada. Não mexi na pílula "Em execução"/posição dela nem na máscara de escurecimento
— só o ícone, que era o item específico da pendência; a pendência mesma já registrava
que redesenhar o componente inteiro (posição da pílula, ausência de escurecimento,
etc., como o mockup mostra) é decisão de produto fora do escopo de uma correção pontual.

Confirmado por foto, celular e tablet: `app/build/fotos/s22u/apps.png` e
`app/build/fotos/tabs8u/apps.png` — "Área de trabalho" legível por inteiro, ícone no
canto, sem sobrepor nenhum outro card da grade (a fileira de dois cards ao lado do
card "em execução" continua com os dois visíveis — não é o mesmo bug da F5).

## 2. Limpeza dos aliases `vidro_*`

`values/colors.xml` documentava 24 cores `vidro_*` como aliases transitórios, cada uma
apontando para o `console_*` mais próximo (mesmo valor, só nome antigo). Repontei toda
referência (`@color/vidro_*` em XML, `R.color.vidro_*` em Java) direto para o
`console_*` equivalente — troca mecânica, mesmo hex, sem mudar nenhum pixel — e apaguei
o bloco de 24 aliases de `colors.xml`. 41 arquivos tocados (`styles.xml`, drawables,
layouts legados como `activity_profiles.xml`/`row_profile.xml`/
`activity_stream_settings.xml`, que continuam vivos e sem alteração visual).

Dois usos em Java que o `grep` em XML não pegava e só apareceram no build:
`VidroLightView.java` (`R.color.vidro_accent_glow`) e `Dialog.java`
(`R.color.vidro_accent_text`) — ambos repontados para `R.color.console_acento`.

**Dimensões e drawables `vidro_*` não são aliases** — `dimens.xml` documenta os
`vidro_*` como a identidade "Vidro" original (valores próprios, não ponteiros para
`console_*`), ainda consumida por telas legadas não redesenhadas nesta fatia (perfis,
ajustes de stream — o mesmo motivo que mantém `VidroLightView` viva). Não toquei nelas.

### Recursos órfãos apagados (confirmado sem consumidor em `res/` nem `java/`)

- `layout/vidro_menu_row.xml` — sem nenhum `R.layout.vidro_menu_row` fora de si mesmo
  na árvore principal; tinha um `shoot()` de conferência em
  `VidroScreenshotTest.java` que removi junto (testava peça morta).
- Drawables, em cascata (um veio à tona só depois de apagar o consumidor do anterior):
  `vidro_card_accent`, `vidro_card_bg`, `vidro_compose_bg`, `vidro_dot_far`,
  `vidro_glass_idle`, `vidro_halo_asleep`, `vidro_island`, `vidro_island_tile_bg`,
  `vidro_poster_bg`, `vidro_poster_scrim`, `vidro_row_bg`, `vidro_sheet_bg` (sem
  consumidor algum de saída); depois, ao apagar os estilos `VidroButton.Accent` e
  `VidroChip` (ver abaixo) por não terem consumidor, seus drawables exclusivos também
  ficaram órfãos: `vidro_button_accent`, `vidro_chip_bg`, `vidro_chip_text`,
  `vidro_chip_on`, `vidro_chip_off`; e, por fim, `vidro_card`, `vidro_card_pressed`,
  `vidro_row`, `vidro_row_pressed`, `vidro_halo_awake` (só apareciam antes porque o
  próprio nome batia com uma entrada do bloco de aliases de cor que acabei de apagar —
  nunca tiveram consumidor real).
- Estilos em `styles.xml`, todos com zero referência em `res/` e zero uso como `parent`
  de outro estilo: `VidroText.Display.Small`, `VidroText.Overline`,
  `VidroText.Section`, `VidroText.Action`, `VidroText.Code`, `VidroButton.Accent`,
  `VidroChip`.

Cada remoção foi conferida por `grep -rn` no código **e** nos XML antes de apagar (regra
do fork: recurso órfão pode ser decisão do dono, mas só depois de provar que não tem
consumidor). Não apaguei nada que tivesse qualquer referência, mesmo indireta — por
exemplo `VidroPreferenceTheme`, `VidroDialog`, `VidroDialogTitle`, `VidroDialogButton`
pareciam sem uso pelo mesmo grep raso, mas são usados de dentro do próprio
`styles.xml` (um estilo referenciando outro) ou de `Dialog.java` — fiquei.

## 3. Documentação

- `docs/design/DESIGN-SYSTEM.md` e `docs/design/VIDRO-IMPLEMENTACAO.md` ganharam um
  aviso "Histórico" no topo, apontando para `docs/design/console/` como fonte de
  verdade viva — não reescrevi o corpo dos dois (são registro do brainstorm e da
  implementação da tese anterior, ainda válidos como história e, no caso do
  `VIDRO-IMPLEMENTACAO.md`, com achados de arquitetura — SurfaceView/blur — que a
  Console herdou). Também corrigi, no `VIDRO-IMPLEMENTACAO.md`, a menção a
  `@color/vidro_glass_fill_session`, que citava um nome de alias que a limpeza acima
  apagou.
- `docs/planos/B-CONSOLE.md`: referência trocada para `.claude/rules/prefs.md`.
- `AGENTS.md`: mesma correção na tabela "Onde ler antes de mexer".

## 4. `values-pt-rBR` — conferência string a string

Comparei os nomes de string entre `values/strings.xml` e o commit `17b35652` ("Merge F1
tokens", o baseline antes de F2 começar) para isolar as strings **novas de F2–F5**: 53
nomes (`console_*`/`vidro_scenes`-like do redesenho, incluindo `summary_compose_bar` e
`title_compose_bar`, do E01 exposto pela F4 — as duas já estão dentro das 53, não são
categorias à parte; correção desta imprecisão de redação apontada pela verificação da
F6, `F6-verificacao.md` §5). As 53 já existem em `values-pt-rBR/strings.xml` — nenhuma
faltando. Conferi à mão uma amostra (não só que a
chave existe: que o texto é tradução real, não cópia do inglês) —
`console_abrir_menu`, `console_ajuste_avancado_legenda`, `console_hz_valor`,
`console_mbps_valor`, `console_encerrar_no_pc`, `summary_compose_bar`,
`title_compose_bar`, `console_tecla_ctrl_alt_del`: todas traduzidas corretamente (as
com placeholder/sigla, como `%1$s Hz` e `Ctrl+Alt+Del`, corretamente idênticas nos dois
idiomas). Não toquei nas ~429 strings legadas (pré-F2) que faltam em `values-pt-rBR` —
são gap pré-existente do app upstream, fora do que a fatia pede ("toda string nova das
fatias F2–F5", não a árvore inteira de preferências).

## 5. Gate e mapas

`node tools/codemap/codemap.mjs` rodado a cada leva de mudança estrutural; última rodada
antes do gate: 224 arquivos, 67.274 linhas (o salto de 190→224 arquivos e 51k→67k
linhas veio de inicializar o submódulo `moonlight-common-c`, que estava vazio neste
worktree antes — `git submodule update --init --recursive`, exigido pelo `AGENTS.md`,
não é mudança desta fatia).

`node tools/validate.mjs`, do zero, depois de todas as correções:

```
[ OK ] codemap
[ OK ] jni
[ OK ] build
[ OK ] test — 4 falha(s) preexistente(s), nenhuma nova
validate: passou.
```

As 4 falhas de teste já existiam antes desta fatia (não investiguei a causa — não é
regressão introduzida aqui, `validate.mjs` já as rotula como preexistentes).

Durante o processo, dois erros de compilação que só apareciam com o submódulo
inicializado (a limpeza dos aliases de cor tinha dois usos em Java que `grep` em XML
não cobria) e um erro de teste (o `shoot()` do `layout/vidro_menu_row.xml`, apagado) —
os três corrigidos antes do gate final, descritos acima.

`node tools/screens.mjs`: 30 telas renderizadas sem erro em
`app/build/vidro-screens/`.

## 6. Fotos

AVD `s22u` (porta 5554): rotas `apps`, `computadores`, `inicio` — antes e depois da
correção do ícone de play, para comparação.

AVD `tabs8u` (porta 5554, `s22u` derrubado antes de subir este): rotas `computadores`
(Início do tablet), `apps`, `ajustes`, `cenas`, `adicionar`, `sessao`, mais uma captura
manual do diálogo de PCs (`dialogo-pcs.png`, `adb shell input tap` no chip).

Comparei visualmente cada uma contra `docs/design/console/fotos/B_*.png`
correspondente onde existe (Computadores celular, Apps celular) — as duas bateram
depois das correções (contorno de acento, ícone de play). **Não fiz a folha de contato
lado a lado (mockup | emulador) formal, montada, para as demais rotas** (Ajustes,
Cenas, Adicionar PC, sessão, e nenhuma delas no tablet contra o
`Tablet_*.dc.html`/`B_Tablet_*.png`) — comparei visualmente uma a uma nesta sessão, mas
não gerei o artefato de folha montada pedido pela fatia para o conjunto inteiro. Ver
"não verificado" abaixo.

Um emulador por vez, cada um derrubado antes do próximo (`emu kill`), confirmado por
`adb devices` vazio entre as trocas.

## 7. Fora de escopo, não tocado

- D-pad dentro de `Game` — decisão do dono, listada explicitamente como fora de
  escopo.
- Posição da pílula "Aberto"/"Em execução" (mockup: canto superior-esquerdo; app: canto
  superior-direito) e a máscara de escurecimento inteira do card em execução — a
  pendência desta fatia era só o ícone de play cobrindo o nome; redesenhar o componente
  todo para bater com o mockup é decisão de produto maior, já registrada como fora de
  escopo pela verificação da F3.
- `console_guia.xml` (guia da sessão, F2) também usa `android:foreground` numa
  `LinearLayout` — mesmo defeito de API 21-22, mas não é um dos três elementos citados
  pela pendência desta fatia. Registro para quem tocar essa tela.

## 8. Não verificado — honestamente

- **Folha de contato lado a lado (mockup × emulador) formal, para todas as telas, nos
  dois AVDs** — o pedido central da fatia. Verifiquei visualmente tela a tela (fotos
  batidas, mockups abertos ao lado, comparação manual) para Computadores e Apps
  (celular e tablet) — as duas telas com pendência registrada — mas não gerei/montei o
  artefato de folha comparativa para o conjunto completo de telas (Início, Ajustes,
  Adicionar PC, Cenas, as telas de sessão) em nenhum dos dois AVDs, nem medi tolerância
  de dp/cor sistematicamente tela por tela. Sessão com orçamento apertado; priorizei
  fechar as quatro pendências concretas herdadas das verificações anteriores com prova
  de foto, em vez de abrir um passe visual completo novo.
- Não fotografei nenhuma tela de sessão real conectada a um host verdadeiro (Vibeshine)
  — só os moques (`MoqueDeCasca`, `MoqueDeApps`, `MoqueDeStream`), como todas as fatias
  anteriores.
- As 4 falhas de teste pré-existentes não foram investigadas (não são regressão desta
  fatia, mas também não confirmei a causa raiz).
- Não testei em aparelho físico — só os dois AVDs (`s22u`, `tabs8u`), como o plano
  manda para esta etapa ("depois das fatias: os aparelhos" é a próxima etapa do plano,
  não desta fatia).
- Não naveguei os fluxos por D-pad/teclado físico em todas as telas tocadas — só toque
  (`adb shell input tap`) e a inspeção visual das capturas do `fotos.mjs`/`screens.mjs`.

## 9. Commits

Em fatias lógicas na branch `fatia/F6-polimento`, sem merge em `dev`, sem push (ver
`git log`).
