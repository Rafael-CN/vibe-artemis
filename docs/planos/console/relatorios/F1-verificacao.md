# F1 — Tokens da Console — verificação

Fatia: `docs/planos/B-CONSOLE.md` §F1 (lido no repositório principal). Branch verificada:
`fatia/F1-tokens`, ponto de partida `6b0f5e98` ("Add Console (Plan B) design tokens for F1").
Worktree: `C:\Users\Rafael\Documents\Projetos\Claude\Artemis-F1`. Verificador sem contexto do
implementador — cada item abaixo foi reproduzido, não assumido a partir do relatório dele
(`docs/planos/console/relatorios/F1-implementacao.md`).

Ambiente: Bash quebrado (`exp: command not found`), tudo rodado em PowerShell 5.1, como o
implementador também registrou. `JAVA_HOME` setado para `C:\Program Files\Java\jdk-21.0.12`
antes de cada comando.

## 1. Escopo do diff — ok

`git diff dev...fatia/F1-tokens --stat` mostra só:

```
 .../main/res/drawable/console_botao_neutro_bg.xml  |  14 ++
 .../res/drawable/console_botao_primario_bg.xml     |   7 +
 app/src/main/res/drawable/console_capa_foco.xml    |  51 ++++++
 app/src/main/res/drawable/console_foco.xml         |  64 +++++++
 app/src/main/res/values-sw600dp/dimens.xml         |  19 +-
 app/src/main/res/values-sw600dp/styles.xml         |  14 ++
 app/src/main/res/values/colors.xml                 | 120 ++++++++-----
 app/src/main/res/values/dimens.xml                 |  40 +++++
 app/src/main/res/values/styles.xml                 | 122 ++++++++++++-
 docs/design/console/TOKENS.md                      | 136 ++++++++++++++
 docs/planos/console/relatorios/F1-implementacao.md | 195 +++++++++++++++++++++
 11 files changed, 726 insertions(+), 56 deletions(-)
```

Nenhum layout, nenhum `.java`, nenhuma string tocada. Só recursos (`colors`/`dimens`/`styles`/
`drawable`) e documentação, exatamente o que a F1 pede. `docs/maps/` não foi tocado.

## 2. Valores dos `console_*` em colors.xml — ok

Os 13 tokens batem byte a byte com `NOTA.md` §2:

| Token | Esperado | Encontrado |
|---|---|---|
| `console_fundo` | `#0B0B0D` | `#0B0B0D` |
| `console_superficie` | `#16161A` | `#16161A` |
| `console_superficie_elevada` | `#1C1C20` | `#1C1C20` |
| `console_superficie_selecionada` | `#22232A` | `#22232A` |
| `console_borda` | `#2A2A30` | `#2A2A30` |
| `console_texto` | `#F2F2F4` | `#F2F2F4` |
| `console_texto_2` | `#9A9AA5` | `#9A9AA5` |
| `console_texto_desabilitado` | `#5A5A62` | `#5A5A62` |
| `console_texto_desabilitado_2` | `#6A6A72` | `#6A6A72` |
| `console_acento` | `#7FE0C7` | `#7FE0C7` |
| `console_acento_sobre` | `#0A1F1A` | `#0A1F1A` |
| `console_destrutivo` | `#F2555B` | `#F2555B` |
| `console_destrutivo_fundo` | destrutivo a 12% | `#1FF2555B` (0x1F/255 = 12,16% ✓) |

Nenhuma cor nova além destas 13 em `colors.xml`. `profileAccent` (`#FF4081`) é pré-existente,
fora do inventário da B, não tocado — correto.

## 3. Aliases `vidro_*` → `console_*` — ok, com uma observação cosmética

25 aliases (não 24 — reconferi linha a linha em `colors.xml`, mesma contagem que o
implementador chegou). Comparei cada um contra o hex/comentário original em `dev` (`git show
dev:app/src/main/res/values/colors.xml`) e contra o uso real em `res/`/`java` (`git grep
"@color/vidro_X"`):

- Fundos → fundos (`vidro_void→fundo`, `vidro_surface→superficie`,
  `vidro_surface_alt→superficie_elevada`, `vidro_glass_session→superficie_elevada`): papéis
  batem.
- Textos → textos (`vidro_text→texto`, `vidro_text_2/_3→texto_2`, `vidro_text_night→texto`,
  sem uso hoje): batem; a Console só tem duas camadas de texto vivo, então `text_3` colapsar
  em `texto_2` é o que a F1 pede.
- Os três acentos (`vidro_accent_glow/_text/_fill`) → `console_acento` único; `vidro_on_accent
  → acento_sobre`; `vidro_light_core → acento` (sem uso hoje): bate com "papel único, nunca
  decoração".
- Bordas (`vidro_stroke*` — 4 pesos diferentes no Vidro) → `console_borda` único: bate com o
  princípio de "uma borda só" que a própria B adota.
- `vidro_halo_awake → acento` (estado ligado), `vidro_halo_asleep/_night/_none → texto_2`
  (não-ligado): bate com a regra de estado de host que F3 formaliza (acento / texto_2 /
  texto_2) — antecipar isso em F1 é razoável e documentado.
- `vidro_glass_fill_session/_idle` (antes translúcidos, alpha 0,88/0,55) → sólidos
  (`superficie_elevada`/`superficie`): mudança de opacidade documentada em `colors.xml` e em
  `TOKENS.md`, coerente com "painéis translúcidos viram sólidos" — aceitável como transição.

Observação sem impacto funcional: `vidro_surface_overlay` e `vidro_surface_overlay_alt`
(linhas 85-86 de `colors.xml`) ficaram agrupados sob o comentário "Bordas por peso", mas
mapeiam para `console_superficie`/`console_superficie_selecionada` — são fundos, não bordas.
Os valores em si estão corretos (confirmado por uso: `vidro_surface_overlay_alt` é o estado
"pressionado" em `vidro_scene_card_bg.xml`, e `console_superficie_selecionada` é exatamente o
papel certo). É só a seção do comentário que categoriza errado; não vale como defeito de
código, registro para quem limpar o arquivo depois.

Usos confirmados por `git grep` antes de aceitar como "sem uso hoje" (implementador reportou
4; recontei e achei mais 2 igualmente órfãos, sem risco nenhum porque um alias não aplicado
nunca quebra nada): `vidro_light_core`, `vidro_halo_night`, `vidro_halo_none`,
`vidro_text_night` (0 usos, como reportado) e também `vidro_glass_session`, `vidro_stroke_faint`
(0 usos, não reportados, mas irrelevantes pelo mesmo motivo).

## 4. Segunda cor cravada — defeito, corrigido pelo verificador

`Select-String` por `#[0-9A-Fa-f]{6,8}` em `res/drawable/`, `res/layout/`, `res/values/` (exceto
`colors.xml`) e `*.java` em `ui/` (0 ocorrências em `ui/`). Achado confirmado do implementador
mais 3 que ele não reportou — todos a mesma causa raiz: hex cravado do azul antigo do Vidro
(`#1E8FEE`) que não é nenhum dos 25 `vidro_*` nomeados em `colors.xml`, logo o alias desta
fatia não alcança:

| Arquivo | Antes | Depois |
|---|---|---|
| `vidro_card_accent.xml` (reportado pelo implementador) | `#141E8FEE` / `#3D1E8FEE` | `#147FE0C7` / `#3D7FE0C7` |
| `vidro_chip_on.xml` (achado pelo verificador) | `#291E8FEE` | `#297FE0C7` |
| `vidro_halo_asleep.xml` (achado pelo verificador) | `#141E8FEE` / `#001E8FEE` | `#147FE0C7` / `#007FE0C7` |
| `vidro_halo_awake.xml` (achado pelo verificador) | `#3D1E8FEE` / `#001E8FEE` | `#3D7FE0C7` / `#007FE0C7` |
| `vidro_scene_card_bg.xml` (achado pelo verificador) | `#1F1E8FEE` / `#8C1E8FEE` | `#1F7FE0C7` / `#8C7FE0C7` |

Em `vidro_chip_on.xml` e nos dois `vidro_halo_*.xml` o bug era visualmente pior do que "sobrou
azul": a borda/`startColor` já resolvia por alias para `console_acento`/`console_texto_2`,
então a peça saía com metade menta e metade azul (ex.: chip com borda menta e fundo azul; halo
com centro cinza e anel azul). Corrigido preservando exatamente os mesmos alphas do original
(0x14, 0x29, 0x3D, 0x8C, 0x1F, 0x00) e só trocando o matiz para `#7FE0C7` (`console_acento`) —
mesma técnica que o próprio `console_foco.xml` já usa para o anel a 15% (`#267FE0C7`), então
não introduz um padrão novo no código. 5 arquivos, 1-2 linhas cada, dentro do critério de
correção trivial. Confirmado visualmente: `02-catalogo-com-sessao.png` (a mesma tela que o
implementador citou com o azul) agora mostra o card "Pick up where you left off" com o realce
em verde-menta, não azul — ver screenshot descrito no item 7.

Após a correção, `Select-String -Pattern "1E8FEE"` nos 5 arquivos só bate nos comentários que
documentam a correção (texto explicativo), não em valor de cor — confirmado lendo cada match.

Não fixei `ic_keyboard_setting.xml` (`#F7B52C`, laranja) nem os demais hex em
`facebutton_*.xml`/`ic_*.xml`/`layout_axixi_keyboard.xml`: são ícones de gamepad/teclado
pré-existentes, sem relação com o inventário `vidro_*`/`console_*` desta fatia, não regrediram
com esta mudança, e mexer neles está fora do que a F1 pede.

## 5. Dimensões e estilos — defeito de escopo em dimens, corrigido; resto ok

**Dimensões da F1** (`values/dimens.xml` + override `values-sw600dp/dimens.xml`): todos os
valores batem com a F1 — `console_espaco_1..6` (4/8/12/16/24/32dp), `console_raio_controle`
8dp, `console_raio_card` 12dp, `console_linha_ajuste` 56dp celular / 64dp tablet,
`console_botao` 48dp, `console_capa_w/h` 160/213dp (sem variação por aparelho — decisão
documentada, aceitável: a F1 dá um valor só, a F3 decide a variação com a grade na frente),
`console_foco_borda` 3dp, `console_foco_anel` 4dp, `console_aba_w/h` 56×16 celular / 64×20
tablet.

**Defeito achado**: `git diff dev...fatia/F1-tokens -- app/src/main/res/values-sw600dp/dimens.xml`
mostrava que o implementador **substituiu o conteúdo inteiro** do arquivo em vez de acrescentar
a ele, apagando 5 dimens de tablet pré-existentes e usados: `vidro_space_5` (13 arquivos),
`vidro_space_6` (4), `vidro_touch_target` (7), `vidro_thumb_reach` (3), `vidro_sheet_width` (1
— a largura da folha de ajustes em sessão). Sem eles, essas 5 chaves caem no default de
celular em `values/dimens.xml` também no tablet (ex.: a folha de ajustes em sessão ficaria
352dp em vez de 420dp no Tab S8 Ultra) — regressão silenciosa fora do escopo da F1, que só
pede mexer em `console_linha_ajuste` e `console_aba_w/h`. **Corrigido**: restaurei as 5 linhas
originais no topo do arquivo, mantendo os dois overrides Console abaixo. `git grep` confirma
os 5 nomes de volta e nenhum uso quebrado.

**Estilos** (`values/styles.xml` + `values-sw600dp/styles.xml`): `ConsoleText.Titulo` (32sp/
ExtraBold/1.15), `.Card` (20sp/Bold), `.Rotulo` (16sp tablet via override / 14sp celular
default /SemiBold), `.Rotulo2` (14sp/Medium/`console_texto_2`), `.Legenda` (12sp/Medium/caixa
alta/tracking 0.08), `.Mono` (13sp/JetBrains Mono Medium/`fontFeatureSettings="tnum"`) — todos
batem. `ConsoleBotao` base + `.Primario` (fundo `console_botao_primario_bg` = `console_acento`
sólido, texto `console_acento_sobre`, altura `console_botao`=48dp, raio `console_raio_controle`
=8dp) e `.Neutro` (fundo `console_botao_neutro_bg` = `console_superficie`+borda `console_borda`,
mesmo raio) batem exatamente com a F1. `ConsoleSegmento` 38dp (meio de 36-40dp, F1 não nomeia
dimen próprio — aceitável). `AppTheme`: `colorPrimary`/`colorAccent`/`android:colorAccent`/
`colorControlActivated` → `console_acento`; `android:windowBackground` → `console_fundo`;
`colorError` → `console_destrutivo` (novo, não existia antes) — todos presentes e corretos.
Fontes: `@font/manrope*` nos `ConsoleText.*`, `@font/jetbrains_mono_medium` no `.Mono` com
`tnum`; todos os 9 arquivos de fonte referenciados existem em `res/font/`. `VidroText.*`/
`VidroButton.*`/`VidroChip`/`VidroDialog*` não foram reparentados — decisão documentada e
correta (mudar `parent` mudaria métricas das telas antigas).

## 6. Drawables de foco — ok

`console_foco.xml`: o `<item android:state_focused="true">` vem **antes** do
`<item android:state_pressed="true">` no selector — em Android, o primeiro item cuja condição
bate vence, então foco vence pressionado quando os dois coincidem (ex.: Enter segurado num
item já focado). Composição: anel externo 4dp (`console_foco_anel`) a `#267FE0C7`
(`console_acento` a 15% — mesma conta do `console_destrutivo_fundo` a 12%, documentada no
próprio arquivo) ocupando a borda externa da view, com um contorno sólido de 3dp
(`console_foco_borda`) em `console_acento` inserido 4dp para dentro — visualmente um anel
suave por fora de uma borda sólida, a melhor aproximação nativa de "box-shadow" que dá para
fazer com um `foreground`/`background` de View (drawable não desenha fora dos próprios
limites). `console_capa_foco.xml` é a mesma composição com `console_raio_card` no lugar de
`console_raio_controle`, sem estado pressionado nem fundo — como o plano pede ("aqui só o
contorno").

Nenhum layout usa os dois ainda (`git grep` em `res/layout` e `app/src/test` = 0 hits) — é o
esperado, aplicar foco é F2/F3. "Renderizam sem erro de inflação" só pôde ser verificado no
nível que a F1 permite: o build (`assembleNonRoot_gameDebug`, via `validate.mjs`) compila os
recursos com aapt2, o que valida XML e referências de todos os drawables em `res/` mesmo sem
uso — e passou nas duas rodadas do gate (antes e depois das minhas correções). Não existe
teste que infle estes dois drawables dentro de uma View de verdade, porque nenhuma tela os usa
— isso não é uma lacuna desta fatia, é consequência de "sem redesenhar telas ainda".

## 7. Gate e telas — ok

`node tools/validate.mjs`, duas rodadas (antes e depois das correções do item 4/5), mesmo
resultado nas duas:

```
[ OK ] codemap
[ OK ] jni
[ OK ] build
[ OK ] test — 4 falha(s) preexistente(s), nenhuma nova
validate: passou.
```

Comparei os 4 nomes de falha contra `tools/test-baseline.json` (`measuredAt: 2026-08-17`,
`total: 51`, `knownFailures` com 4 entradas: `LayoutInflationTest#allLayoutsInflateSuccessfully`,
`SimpleStartupTest#testApplicationOnCreate`, `StartupTest#testApplicationStartup`,
`ProfilesNavigationTest` ×2 — na verdade 5 entradas no arquivo, mas o gate roda com JDK 21/
Robolectric atual e reporta 4; não abri o log completo do Gradle para casar nome a nome, mas a
contagem e a classificação do próprio `validate.mjs` — que já compara por baseline — bateram
nas duas rodadas, então não há falha nova introduzida por esta fatia nem pelas minhas
correções).

`node tools/screens.mjs`, também duas rodadas — a segunda depois de restaurar os dimens de
tablet (a primeira rodada capturou os PNGs de tablet ainda com o bug do item 5, então descartei
essas capturas e usei só a segunda rodada como evidência final). 29 telas em
`app/build/vidro-screens/`, todas com tamanho de arquivo plausível, sem falha no runner.
Abri e olhei:

- `app/build/vidro-screens/01-home.png` (`PcView`) — fundo quase preto (`console_fundo`),
  "Add" em verde-menta (`console_acento`), título "Looking around your network" em Manrope
  ExtraBold visível.
- `app/build/vidro-screens/10-ajustes.png` (`StreamSettings`) — título "Settings" em Manrope
  ExtraBold sobre fundo `console_fundo` com o halo tingido de menta (não mais azul).
- `app/build/vidro-screens/09-endereco.png` (`AddComputerManually`) — "NEW PLACE" em caixa
  alta cinza (`console_texto_2`), título "PC address" em ExtraBold, campo de IP em fonte
  monoespaçada, botão "Connect" sólido em `console_acento` com texto escuro
  (`console_acento_sobre`).
- `app/build/vidro-screens/04-sessao-ilha.png` (ilha/menu da sessão) — ponto de estado
  "controls" e botão "Leave the screen" em `console_acento`.
- `app/build/vidro-screens/tab-01-home.png` (home de tablet, mesma `PcView` em 1973dp de
  largura) — mesma paleta do celular, "Add" em menta, título em ExtraBold, proporção
  condizente com os dimens de tablet já corrigidos.
- `app/build/vidro-screens/02-catalogo-com-sessao.png` — confirmação visual direta da
  correção do item 4: o card "Pick up where you left off" agora sai com o realce em
  verde-menta (antes, azul `#1E8FEE`), coerente com "uma cor só".

Todas via `shootActivity` (Activity real) — nenhuma é layout inflado isolado, então o texto e
o estado vêm de código real rodando, não de um mock de tela.

## 8. `docs/design/console/TOKENS.md` — ok

A tabela cobre os 13 tokens de cor com recurso/valor/onde-usar, os 25 aliases `vidro_*` com o
papel original de cada um, os 8 dimens com valor por aparelho, os 6 estilos de texto e os 4
componentes (`ConsoleBotao.*`, `ConsoleSegmento`, os dois drawables de foco), mais uma seção de
decisões da fatia. Conferi cada linha contra o código depois das minhas correções: nenhuma
diverge (o documento já descrevia os valores corretos — meus consertos foram em drawables que
o TOKENS.md não precisava listar, porque não são tokens novos, são usos indevidos de um token
antigo que a tabela nem nomeia).

## Defeitos corrigidos pelo verificador

Commit `b9e2a37c` ("Fix leftover blue hex and restore deleted tablet dimens (F1 review)") na
branch `fatia/F1-tokens`:

1. **Azul cravado sobrando em 5 drawables** (item 4): `vidro_card_accent.xml`,
   `vidro_chip_on.xml`, `vidro_halo_asleep.xml`, `vidro_halo_awake.xml`,
   `vidro_scene_card_bg.xml` — hex `#1E8FEE` trocado por `#7FE0C7` (`console_acento`),
   mesmos alphas.
2. **`values-sw600dp/dimens.xml` com 5 dimens de tablet pré-existentes apagados** (item 5):
   `vidro_space_5`, `vidro_space_6`, `vidro_touch_target`, `vidro_thumb_reach`,
   `vidro_sheet_width` restaurados.

## Defeitos abertos

Nenhum que bloqueie a fatia. Registro não-bloqueante:

- Categorização de comentário em `colors.xml`: `vidro_surface_overlay`/
  `vidro_surface_overlay_alt` estão sob o cabeçalho "Bordas por peso" mas são fundos — só o
  comentário está no lugar errado, os valores estão certos (item 3). Reproduzir: abrir
  `app/src/main/res/values/colors.xml` linhas 80-86.

## Veredito

**APROVADA** (pode integrar) — depois das duas correções acima, que já estão commitadas nesta
branch. Gate verde nas duas rodadas, telas conferidas nos dois AVDs simulados via
`screens.mjs` (celular e tablet), nenhuma cor fora da paleta de 13 tokens, nenhum arquivo fora
do escopo de recursos/documentação da F1.
