# F6 — Polimento contra o mockup e limpeza — verificação

Verificador independente, sem o contexto do implementador. Worktree `Artemis-F6`, branch
`fatia/F6-polimento`, base `57b697c6`. Porta 5554, um AVD por vez (`s22u` depois `tabs8u`,
cada um derrubado antes do próximo — confirmado por `adb devices` vazio entre as trocas e ao
final).

## Veredito: **DEVOLVIDA**

Dois defeitos concretos encontrados foram corrigidos nesta verificação (commit
`f6a5ead1`, ver §3). O motivo da devolução é um único item que exige decisão de produto,
não um ajuste pontual — a seção "Apps" do celular tem uma faixa e uma linha inteiras que
não existem no mockup (§2, tela "Apps"). O resto da fatia — as quatro pendências herdadas,
a limpeza dos aliases `vidro_*`, `values-pt-rBR` — está correto e não precisa voltar.

## 1. Comandos executados

```
node tools/validate.mjs          → passou (codemap OK, jni OK, build OK,
                                    test OK — 4 falha(s) preexistente(s), nenhuma nova)
node tools/screens.mjs           → 30 telas renderizadas sem erro em app/build/vidro-screens/
./gradlew assembleNonRoot_gameDebug  → BUILD SUCCESSFUL (rodado 1x, depois das duas correções)
node tools/fotos.mjs --avd s22u   --port 5554 --rotas computadores,apps,ajustes-basico,sessao-guia
node tools/fotos.mjs --avd tabs8u --port 5554 --rotas inicio,ajustes-basico,sessao-guia,sessao-texto
```

`JAVA_HOME=C:\Program Files\Java\jdk-21.0.12`, `ANDROID_HOME` apontado para o SDK do
usuário. Submódulo `moonlight-common-c` já estava inicializado neste worktree
(`git submodule status` mostrou o commit, não vazio).

Cada AVD foi religado com `settings put system system_locales pt-BR` + `adb reboot` antes das
capturas — os dois nascem em `en-US` por padrão, e a comparação com o mockup (que é pt-BR)
não faz sentido em outro idioma.

## 2. Folha de contato lado a lado

`docs/planos/console/fotos/F6-comparacao-s22u.png` e `F6-comparacao-tabs8u.png` (commitadas).
Cada linha é mockup à esquerda, emulador à direita.

| Tela | Delta encontrado | Ação |
|---|---|---|
| **Computadores** (celular) | Nenhum. Estrutura, cor, contorno de acento do card on-line, espaçamento — batem. | — |
| **Apps** (celular) | **Faixa inteira ausente do mockup**: entre o título "Apps" e a grade, o app mostra uma linha "N aplicativos publicados pelo host · Recarregar" e, quando há sessão aberta, um banner "X está aberto / Volte de onde parou / Voltar". O mockup (`B_Celular_Apps.png`) vai direto do título pra grade — nenhum dos dois existe nele. São strings/views de antes do redesenho (`vidro_reload`, `vidro_published_apps`, `vidro_session_open`, `vidro_session_return`, `activity_app_view.xml:81-184`) que a F3 manteve conscientemente ("a faixa de sessão aberta e a contagem/recarregar continuam, só re-vestidas", F3-implementacao.md) e a verificação da F3 não pegou (nenhuma menção no F3-verificacao.md). **Devolvido, não corrigido** — apagar a faixa tira a única forma rápida de voltar ao app aberto sem achar o card certo na grade, e "Recarregar" é a única forma de atualizar a lista sem sair da tela; onde essas duas ações deveriam morar no desenho novo (dentro da grade? em outro lugar do cabeçalho?) é decisão de quem aprova o mockup, não algo para eu inventar layout novo. |
| **Ajustes** (celular e tablet) | Nenhum, além de "ARTEMIS · 192.168.0.10" não aparecer no subtítulo — mas é porque `tools/fotos.mjs` abre `StreamSettingsBasico` direto por `am start`, sem os extras `EXTRA_PC_NAME`/`EXTRA_PC_ADDRESS` que só `PcView` passa; não é o app perdendo o dado, é limite desta ferramenta de foto. Grade de 720dp centralizada no tablet confere (medi: coluna a ~626dp de cada borda, spec pede (1973-720)/2≈626dp). Resolução sem segmento marcado quando o valor não bate com nenhum dos três é comportamento documentado da F4 (`configurarSegmentado`), não bug desta fatia. | — |
| **Sessão com menu** (celular e tablet) | **Corrigido nesta verificação**: nenhum bloco da guia mostrava o anel de foco inicial ("Teclado" deveria vir focado ao abrir, por toque ou D-pad — B-CONSOLE.md §F2, "Foco inicial no primeiro bloco"). Ver §3. "Picture-in-picture" ausente da fileira é intencional (F2-implementacao.md: sem ação hoje). "Disconnect" em inglês é string legada pré-F2 (`game_menu_disconnect`), gap já divulgado pelo relatório da F6 como fora do escopo desta fatia. | Corrigido |
| **Sessão com barra de texto** (tablet) | **Corrigido nesta verificação**: os cantos da capa focada/vizinhas na fileira "APPS" da Início não tinham a borda de repouso (só o foco tinha contorno; em repouso ficavam com canto quadrado, capas coladas). Ver §3. A rota automática `sessao-texto` de `tools/fotos.mjs` não capturou a barra de verdade (guia ficava aberta na foto, mesmo repetindo) — reproduzi manualmente por `adb shell input tap`/`screencap` com esperas maiores e a barra abre normalmente, batendo em estrutura com o mockup (cabeçalho BARRA DE TEXTO, campo, Enviar, fileira Esc·Tab·Win·Alt+Tab·Ctrl+Alt+Del — sem "18 ms · 35 Mbps" porque o moque não publica esses números, como a regra manda). Isso é limite de tempo da rota da ferramenta (não vi o tempo exato; parece ligado ao aquecimento do teclado/dicionário do Gboard na primeira vez que o IME abre no emulador), registrado como não verificado por automação — ver §5. | Verificado manualmente |
| **Início** (tablet) | **Corrigido nesta verificação**: capas da fileira "APPS" sem contorno de repouso, coladas umas nas outras sem a ilusão de canto arredondado que `app_grid_item.xml` já tem (via `grid_frame`/`vidro_poster_frame`) — `console_capa_carousel_item.xml` nunca teve o equivalente. Ver §3. Fora isso, "Continuar" e a grade batem com o mockup (medidas, cores, textos). | Corrigido |

## 3. Correções aplicadas (commit `f6a5ead1`)

1. **`app/src/main/res/drawable/console_capa_borda.xml`** (novo) + `console_capa_carousel_item.xml`:
   contorno de repouso (transparente, 1px `console_borda`, raio `console_raio_card`) para a
   capa da fileira "APPS" da Início — o par que faltava de `console_capa_foco.xml`, que por
   design só cuida do foco. Confirmado por foto antes/depois (zoom em
   `app/build/fotos/tabs8u/inicio.png`): cantos arredondados, capas com a mesma ilusão de
   "card" que o celular já tinha.
2. **`console_guia.xml` (10 blocos) e `console_teclas_fileira.xml` (5 teclas)**: adicionado
   `android:focusableInTouchMode="true"`. Sem isto, `View#requestFocus()` — que
   `GameMenu.java` já chama no bloco "Teclado" ao abrir a guia — falha silenciosamente
   porque a janela está em modo de toque (o gatilho documentado da guia é justamente o
   toque na aba). Resultado: a guia sempre abria sem nenhum destaque de foco, diferente do
   mockup e do requisito "foco inicial no primeiro bloco" (B-CONSOLE.md §F2). Confirmado
   por foto nos dois AVDs, antes (nenhum anel) e depois (anel em "Teclado" logo após o
   toque na aba).

Rodei `node tools/validate.mjs` de novo depois das duas correções: passou igual (codemap OK
sem diffs em `docs/maps/`, build OK, test OK com as mesmas 4 falhas preexistentes).

## 4. Auditoria da limpeza dos aliases `vidro_*`

- `grep -rn` por cada um dos ~24 nomes de drawable/estilo apagados (listados no relatório
  da F6) em `res/` (XML) e `java/` (incluindo `getIdentifier` — só uma ocorrência no
  código inteiro, em `Game.java:1018`, e é um comentário dizendo que a técnica NÃO é usada
  ali): nenhum consumidor restante.
- `values/colors.xml` hoje só tem os 13 tokens `console_*` — os 24 aliases `vidro_*` de
  cor não existem mais no arquivo (confirmado, `grep -c vidro` = 4, todas em comentário).
- Estilos apagados (`VidroText.Display.Small`, `.Overline`, `.Section`, `.Action`, `.Code`,
  `VidroButton.Accent`, `VidroChip`): zero ocorrência em `res/` fora de um comentário em
  `styles.xml:209` que só os cita como histórico.
- `layout/vidro_menu_row.xml`: confirmado ausente, e o `shoot()` de teste que o
  referenciava em `VidroScreenshotTest.java` também não existe mais.
- **Nada que ainda serve telas legadas foi levado**: `VidroLightView` está viva
  (`grep` confirma uso em `activity_stream_settings`/perfis); as dimensões `vidro_*`
  (`vidro_space_*`, `vidro_stroke_width` etc.) e vários drawables `vidro_*` continuam —
  corretamente, são valores próprios, não aliases, e ainda usados pelas telas de perfis e
  ajustes de stream que esta fatia não redesenha.
- Os dois usos em Java que o relatório da F6 diz ter corrigido (`VidroLightView.java`,
  `Dialog.java`) de fato apontam para `R.color.console_acento` hoje — conferido por
  `grep`, não só lido no relatório.

Nenhum recurso apagado sobrou com consumidor. Auditoria bate com o que o relatório
alega.

## 5. `values-pt-rBR`

Comparei `values/strings.xml` contra o commit `17b35652` (baseline pré-F2, o mesmo que o
relatório da F6 usa) com `comm`: **53 chaves novas** (não 55 — o relatório da F6 soma "53
+ summary_compose_bar + title_compose_bar" como se fossem categorias diferentes, mas essas
duas já estão dentro das 53; é uma imprecisão de redação no relatório, não um problema de
tradução: as 53, incluindo essas duas, existem todas em `values-pt-rBR/strings.xml` —
`comm -23` não sobrou nenhuma). Nenhuma delas vazou para `values-de`, `values-fr`,
`values-es` etc. — varri as 26 pastas `values-*` do repositório com `grep -Fxf` e nenhuma
bateu.

## 6. Pendências herdadas — confirmadas resolvidas

- **Contorno do card de PC on-line**: `console_card_bg_online.xml` (borda 1.5dp
  `console_acento`) aplicado por `PcGridAdapter` quando `online && paired`; foto do card
  ARTEMIS com a borda, NOTEBOOK sem — bate com o mockup.
- **Referência ao howto**: `docs/planos/B-CONSOLE.md` e `AGENTS.md` apontam para
  `.claude/rules/prefs.md`, que existe.
- **Anel de foco API 21-22**: `consolePcChipAtual`, `settingsButton`
  (`layout-sw600dp/activity_pc_view.xml`) e `console_pc_dialog_row.xml` têm raiz
  `FrameLayout` com `android:foreground` — técnica correta (nativa desde API 1 em
  `FrameLayout`, ao contrário de `ViewGroup` genérico). Foto do tablet confirma os dois
  controles lado a lado, nenhum sumiu (o bug que a F5 pegou com a técnica de View-por-cima
  não se repete aqui, porque não há View irmã nenhuma). **Achado à parte, não uma
  regressão desta fatia**: `console_guia.xml:356` (bloco "Encerrar no PC") ainda tem
  `android:foreground` numa `LinearLayout` — o mesmo defeito, não corrigido. O próprio
  relatório da F6 já divulga isso na seção "Fora de escopo" ("não é um dos três elementos
  citados pela pendência") — confirmo que continua lá, registro para quem tocar essa tela,
  não é motivo de devolução porque foi corretamente declarado, não escondido.
- **Ícone de play**: movido para o canto inferior-direito em `app_grid_item.xml`
  (`grid_overlay`, 40dp, margem `vidro_space_3`) — foto do celular mostra "Área de
  trabalho" legível por inteiro.

## 7. Fronteiras

- `git diff --stat` desta verificação: só XML (`console_capa_carousel_item.xml`,
  `console_guia.xml`, `console_teclas_fileira.xml`) e um drawable novo. Nenhum Java, nenhum
  Kotlin, nada em `app/src/main/jni/`, nada em `Game.java` (flags de janela intactas).
- Nenhuma cor literal nova — o drawable novo reusa `console_borda`/`console_raio_card`.

## 8. Não verificável a partir daqui — honestamente

- **Nada disto prova comportamento em aparelho físico.** Só os dois AVDs (`s22u`,
  `tabs8u`), como o plano manda para esta etapa.
- A rota `sessao-texto` de `tools/fotos.mjs` não produziu uma foto confiável da barra de
  texto em nenhuma das duas tentativas (a guia continuava visível na captura); usei uma
  sequência manual de `adb shell input tap`/`screencap` com esperas maiores para confirmar
  visualmente que a barra abre e bate com o mockup, mas não decidi se é um problema da
  ferramenta (tempo de espera curto demais) ou do tempo real que o `dismiss()` do diálogo
  leva — não investiguei a causa raiz, só contornei para conseguir a foto.
- Não testei D-pad/teclado físico em nenhuma tela — só toque (`adb shell input tap`) e a
  correção de foco inicial foi confirmada por foto, não por navegação completa com
  `KEYCODE_DPAD_*` fileira afora.
- Não fotografei nenhuma tela de sessão contra um host real (Vibeshine) — só os moques,
  como todas as fatias anteriores.
- As 4 falhas de teste preexistentes não foram investigadas (mesmo estado que o relatório
  da F6 encontrou; não são regressão desta verificação).
- Não medi tolerância de dp/cor pixel a pixel em todas as telas — comparação visual da
  folha de contato mais, em alguns casos, medição manual (célula por célula, PowerShell +
  System.Drawing) quando a diferença não era óbvia a olho.

## 9. Commits

`f6a5ead1` — as duas correções desta verificação, na própria branch `fatia/F6-polimento`.
Sem merge em `dev`, sem push.
