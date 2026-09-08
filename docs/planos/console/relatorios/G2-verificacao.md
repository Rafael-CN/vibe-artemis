# G2 — verificação independente da "sessão no aparelho"

Verificador independente, não implementou nada desta fatia. Worktree `Artemis-G2`,
branch `fatia/G2-sessao2` (6 commits à frente de `dev`=`e91d3999`, árvore limpa antes de
começar). Emulador `s22u` na porta 5556 — `submodule update --init --recursive` não foi
necessário (submódulo já populado nesta cópia). Build: `assembleNonRoot_gameDebug`,
instalado em `emulator-5556` como `com.limelight.noirdebug`. `emulator-5554` (G1, em
paralelo) nunca foi tocado. Emulador derrubado (`emu kill`) ao final.

**Veredito: DEVOLVIDA.**

O defeito 1 (duplo toque) está genuinamente corrigido, e a proporção dos blocos (defeito
3) e a medição do retrato (defeito 5) se sustentam. Mas o defeito 2 — o pedido central do
dono, "o menu na stream tá ruim de ativar" — **não está resolvido**: a técnica de
`TouchDelegate` para alargar a área tocável da aba **não funciona nunca**, em nenhuma
borda testada, porque a própria superfície de stream intercepta o toque antes do
`TouchDelegate` ser consultado. E o defeito 4 tem uma regressão nova: o alternador da
fileira de teclas especiais fica majoritariamente **debaixo do teclado real** quando
recolhido (comportamento padrão no celular), tornando-o quase impossível de tocar — o que
o implementador descreveu como "não consegui provar", eu reproduzi como "não funciona na
prática, só numa fresta de ~9dp".

## 1. Toque único na guia — CONFIRMADO (sem defeito)

Testado com `adb shell input tap` em landscape (guia aberta por toque na aba,
`s22u`/5556, `MoqueDeStream=true`), um toque por vez, conferindo o efeito (não o anel de
foco) via `uiautomator dump` antes/depois:

| bloco | efeito de UM toque | resultado |
|---|---|---|
| aba (`consoleAba`) | abre a guia | OK |
| Teclado | guia fecha + `mInputShown=true` (IME real) | OK |
| Barra de texto | guia fecha + `consoleBarraTextoTeclasToggle` aparece | OK |
| Modo do toque | guia fecha | OK |
| Teclas especiais | troca para o subpainel (`consoleBlocoVoltar` aparece) | OK |
| Voltar | volta ao painel principal (`consoleBlocoGirar` reaparece) | OK |
| Girar a tela | guia fecha + `mDisplayRotation` mudou de fato (90→0) | OK |
| Estatísticas | guia fecha + `performanceOverlayBig` aparece/some (testei ida e volta) | OK |

Desconectar/Encerrar **não foram acionados ao vivo** (encerrariam a sessão moque sem
necessidade) — confirmados por leitura de código: usam o mesmo `onBlock()` genérico
(`GameMenu.java:274`), sem `focusableInTouchMode` em nenhuma das duas XML. Mesmo padrão
dos blocos testados, risco de regressão específica é baixo.

Também conferi o próprio texto explicativo nas 3 XML (`console_guia.xml`,
`console_teclas_fileira.xml`, `console_barra_texto.xml`) contra o diff: as 15
ocorrências de `focusableInTouchMode="true"` descritas no relatório do implementador
realmente sumiram (10 em `console_guia.xml`, 5 em `console_teclas_fileira.xml`), e o novo
alternador da barra de texto (`consoleBarraTextoTeclasToggle`) nasceu **sem** o atributo,
seguindo o padrão certo. Nenhum bloco pediu segundo toque em nenhum teste.

## 2. Área tocável da aba — DEFEITO (a expansão não funciona)

**Medição da aba desenhada** (`uiautomator dump`, `s22u`/5556, landscape):
`bounds="[1148,0][1295,42]"` → 147×42px. Densidade do aparelho: 2,625px/dp (conferido:
56dp de largura nominal × 2,625 = 147px, bate). Ou seja a aba segue com **56×16dp** de
desenho — igual a antes desta fatia.

**A expansão via `TouchDelegate` (código em `Game.java#expandConsoleAbaToqueArea`,
+20dp cima/baixo, +32dp laterais) foi testada e não produz efeito algum.** Toquei em 4
pontos fora do retângulo desenhado mas dentro da área que o código diz expandir:

| toque (px) | posição relativa à aba | guia abriu? |
|---|---|---|
| (1074,20) | 74px à esquerda do desenho | não |
| (1140,20) | 8px à esquerda | não |
| (1303,20) | 8px à direita | não |
| (1221,47) / (1221,60) / (1221,90) | 5–48px abaixo | não |

Nenhum desses toques abriu a guia — todos caíram na área do vídeo (`backgroundTouchView`
/`streamContainer`) sem efeito visível (no moque; ao vivo seriam repassados como entrada
de jogo). Só o toque **dentro** do retângulo desenhado (testado no centro, 1221×20) abriu
a guia.

**Causa raiz:** em `activity_game.xml`, `backgroundTouchView` e `streamContainer` são
`View`s irmãs da aba dentro do mesmo `merge`, cobrindo a tela inteira, e
`Game#onTouch()` (`backgroundTouchView.setOnTouchListener(this)`, linha 559) **consome
todo toque que caia nelas** (`handleMotionEvent`, é assim que o toque vira entrada
remota). O algoritmo de despacho do Android (`ViewGroup.dispatchTouchEvent`) percorre os
filhos do mais recente ao mais antigo procurando quem contenha o ponto tocado; a aba está
mais acima na ordem de desenho (incluída depois de `streamContainer` no XML), mas só
"ganha" quando o ponto cai dentro do **retângulo real** dela. Fora dele, o próximo filho
que contém o ponto é `streamContainer`/`backgroundTouchView`, que consome o toque —
`TouchDelegate` só é consultado se **nenhum filho** consumir, o que nunca acontece aqui,
porque a superfície de vídeo consome tudo. Ou seja: `setTouchDelegate` no pai
(`android.R.id.content`) nunca é alcançado nas bordas do vídeo, que é exatamente onde a
área "expandida" deveria valer.

**O deslize para baixo funciona e é a única melhoria real de defeito 2:** testado
`adb shell input swipe 1221 20 1221 300 300` a partir de dentro da aba — abriu a guia
(`consoleBlocoEstatisticas` presente no dump seguinte). Não substitui o toque simples (o
toque no centro continua abrindo normalmente) e não colidiu com nada do sistema nos meus
testes.

**Conclusão:** o alvo de toque real da aba continua em 16dp de altura — bem abaixo dos
48dp mínimos — e a técnica escolhida para alargá-lo não tem efeito nenhum contra a
superfície de stream, que é exatamente onde o dono vai errar o toque. O deslize ajuda,
mas é um atalho adicional, não o alvo maior que o defeito pedia. **Não verificável ao
vivo:** a "textura" do toque sobre vídeo de verdade decodificando — meu teste confirma
que a lógica do `TouchDelegate` não dispara nem no moque, o que já basta para reprovar,
independente de vídeo real.

## 3. Proporção dos blocos no celular — OK

`node tools/screens.mjs` → `04-sessao-guia.png`: com a guia renderizada larga o
suficiente para não cortar rótulo nenhum, "Special keys", "Rotate screen" e "Statistics"
aparecem por inteiro, blocos finos (ícone 24dp + rótulo curto), no mesmo estilo do
mockup `B_Celular_Sessao_Menu.png` (blocos discretos, não quadrados).

Medi ao vivo (`uiautomator dump`, `s22u`/5556, retrato): bloco "Teclado" com
`bounds="[32,149][209,273]"` → 177px de largura → 177/2,625 ≈ **67,4dp**, batendo com o
novo token `console_bloco_min_w=64dp` (a diferença é o conteúdo — ícone+padding — que
excede ligeiramente o mínimo). Contra os 96dp+padding de antes (~115dp+), é uma redução
real e na direção certa. `values-sw600dp/dimens.xml` preserva 96/88/104dp — não testei o
tablet ao vivo (não usei `tabs8u` nesta verificação, ver seção "não verificado"), mas o
diff mostra os valores idênticos aos de antes, coerente com a alegação do implementador.

## 4. Barra de texto e teclado virtual — PARCIAL (defeito novo)

**Recuos menores:** confirmados via bounds. Sem teclado, `vidroComposeBar` mede
`[0,157][2316,457]` em landscape (altura 300px≈114dp) — consistente com os tokens
menores (`console_barra_texto_pad_h/v/gap` = 12/8/4dp no celular).

**Fileira de teclas especiais recolhida por padrão:** confirmado — logo após
`openComposeBar`, `consoleBarraTextoTeclasRow` não aparece no dump, só o alternador
`consoleBarraTextoTeclasToggle`.

**Vídeo restante com o teclado real (Gboard) aberto, em retrato** (`s22u`, tela
1080×2316): abri a barra de texto, toquei no campo de texto para trazer o Gboard de
verdade (`mInputShown=true`), medi:

- `streamContainer`: `[0,508][1080,1935]` (1427px de altura, letterbox — não muda com o
  teclado, porque a barra desenha por cima, não redimensiona o vídeo).
- `vidroComposeBar`: `[0,1196][1080,1496]` — topo em y=1196.
- Vídeo **não coberto pela barra** acima dela: de y=508 a y=1196 → 688px ≈ **262dp**
  visíveis, de uma tela de 2316px (882dp) — cerca de 30% da altura da tela é vídeo
  descoberto com a barra+teclado abertos, o resto é barra opaca ou teclado.
- Inset real do IME (`dumpsys window displays`): `type=ime frame=[0,1433][1080,2316]`.

**Alternador — expandi e recolhi de verdade, com o Gboard aberto (o que o implementador
não conseguiu provar):**

1ª tentativa, coordenada do centro do alternador (`bounds=[32,1409][404,1475]`, centro
`(218,1442)`): **toque caiu no próprio Gboard** (dumpsys `input_method` mudou de estado;
screenshot mostrou o painel de figurinhas do Gboard aberto, não a fileira do app). Bati
de novo, mesmo resultado, uma vez até abrindo o seletor de figurinhas do teclado — a
mesma frustração que o relatório do implementador documentou.

**Causa, medida:** o inset do IME começa em y=1433. O alternador mede `[32,1409][404,
1475]` — **1409 a 1433 (24px≈9dp) fica acima do teclado; 1433 a 1475 (42px≈16dp, quase
dois terços da altura do próprio alternador) fica embaixo dele**, coberto de verdade pela
janela do teclado (que é uma janela do sistema, desenhada por cima da do app). Um toque
no meio ou na parte de baixo do alternador — onde o rótulo "TECLAS ESPECIAIS" e o
chevron visualmente aparecem — cai no Gboard, não no app.

**Só toquei com sucesso mirando a fresta de 9dp no topo do alternador** (`y=1412`, dentro
da faixa 1409–1433): aí sim `consoleBarraTextoTeclasRow` apareceu, com as 5 teclas
completas (Esc·Tab·Win·Alt+Tab·Ctrl+Alt+Del) — screenshot em
`docs/planos/console/fotos/G2-verificacao-teclas-expandidas-s22u.png`. Recolher de volta
(toque no alternador já reposicionado mais acima, fora da faixa do teclado, dessa vez sem
problema) também funcionou num toque só.

**A rolagem horizontal funciona:** com a fileira expandida, `consoleKeyCtrlAltDel` media
`[851,1349][1048,1475]` (197px, cortada pela borda do viewport); um `swipe` para a
esquerda revelou a tecla inteira, `[723,1349][1027,1475]` (304px ≈ 112dp, o mínimo
declarado). As 5 teclas ficam alcançáveis rolando, como pedido.

**Conclusão — defeito novo, não é só "não verificado":** o alternador, no estado padrão
(recolhido) do celular, fica **fisicamente embaixo do teclado real** na maior parte da
sua área tocável. Não é uma falha de instrumentação do `adb` (como o implementador
supôs) — reproduzi 3 vezes, inclusive com coordenadas "frescas" tiradas de um dump
recém-feito no estado já assentado, e o toque ainda caiu no Gboard. Só uma fresta de 9dp
no topo do próprio alternador está de fato acima do inset do IME. Isso é abaixo de
qualquer alvo de toque confortável — na prática, o dono vai tocar no rótulo/chevron
visível (que ocupa a parte coberta) e vai abrir o próprio teclado (figurinhas, GIFs etc.)
em vez da fileira do app.

`sendTextBlock`/`openComposeBar`/`closeComposeBar` e `ImeDisplayMode.RESIZE`: conferidos
por leitura de código, nenhuma das duas foi tocada nesta fatia (confirmado pelo diff:
`Game.java` só ganhou os métodos de aba/alternador, nada em torno do envio de texto ou do
`softInputMode`).

## 5. Retrato — a conclusão do implementador se sustenta (defeito 5)

Reproduzi a medição do implementador, de forma independente: `pm clear`, rotação para
retrato (`user_rotation=0`, `accelerometer_rotation=0`), `am start
com.limelight.Game --ez MoqueDeStream true` (preferências realmente padrão). Sem abrir
guia nem barra de texto:

`streamContainer` → `bounds="[0,918][1080,1525]"`, tela `1080×2316`. **1080×607px,
largura cheia, altura reduzida — bate exatamente com o que o relatório da implementação
alega** (mesmos números, `1080×607`). Foto: `G2-retrato-s22u.png` (já existente, moque
desenha preto sólido — não há forma de ver o letterbox no pixel, só no bounds, porque
**moque não é vídeo vivo**).

Não toquei nada aqui (nem o implementador tocou) e não reproduzi corte nenhum com
preferências padrão. A conclusão do relatório — que o defeito só reaparece com
`checkbox_show_onscreen_controls=true` ou `videoScaleMode=FILL`, ambos escolhas de
configuração e não bug de medida — não testei essas duas variantes (fora do escopo do
que o dono relatou como problema recorrente com preferências padrão), mas a lógica
apresentada (ramo `SCREEN_ORIENTATION_FULL_USER`, `StreamContainer.onMeasure` não
tocado) é consistente com o que o diff mostra: nada em torno do letterbox foi alterado
nesta fatia.

**O que precisa ser confirmado no aparelho do dono, com clareza:** se a stream ainda
corta em retrato com vídeo de verdade e as preferências "Mostrar controle na tela"
desligada e "Escala de vídeo" em "Ajustar", isso é uma diferença de comportamento entre
`MediaCodec` real e a superfície falsa do moque — não reproduzível nesta verificação nem
na implementação, por falta de host.

## 6. Fronteiras invioláveis — respeitadas

Revisei o diff completo de `Game.java` (207 linhas, 6 hunks) linha a linha: nenhuma
menção a flags de janela, `Surface`/decodificação ou dispatch de entrada da Activity
(`dispatchTouchEvent` da própria `Activity` não foi tocado — só `setOnClickListener`/
`setOnTouchListener` em views novas, um padrão já usado em outros lugares do arquivo,
como o `overlayToggleZoomButton`). `GameMenu` continua abrindo a guia como
`android.app.Dialog` — não vi nenhuma tentativa de overlay na mesma janela. Sem
Kotlin/Compose. Strings novas (`console_barra_teclas_recolhida_por_padrao` é bool, não
string) só em `values/bools.xml` e `values/dimens.xml` — nada em `values-pt-rBR/` foi
tocado, e não há string nova de UI (o alternador reaproveita
`console_guia_teclas_especiais`/"Special keys" já existente, como o relatório afirma).

## Gate

- `node tools/validate.mjs` (`JAVA_HOME` = JDK 21): **passou** — codemap, jni, build,
  test (4 falhas pré-existentes, nenhuma nova). Mesmo resultado que o implementador
  relatou.
- `node tools/screens.mjs`: 30 telas renderizadas sem erro, incluindo
  `04-sessao-guia.png`/`tab-04-sessao-guia.png` usadas na verificação do item 3.
- Não rodei `tools/fotos.mjs` (bateria completa) nem usei o `tabs8u` — economizei tempo
  de emulador reproduzindo os pontos específicos do relatório da implementação com
  `adb shell input tap`/`swipe`/`uiautomator dump` diretamente, o que create prova mais
  granular por bloco do que uma nova rodada de fotos teria dado. Não afirmo ter rodado a
  bateria de 22 rotas — só os testes descritos acima.

## O que não é verificável daqui

- **Vídeo vivo de verdade em nenhum dos 5 defeitos** — moque não é vídeo vivo, e a
  aparência/textura do toque sobre `SurfaceView` decodificando quadros reais só o
  aparelho do dono prova.
- Defeito 5 com decodificação real e host Vibeshine (a recomendação de conferir "Mostrar
  controle na tela" e "Escala de vídeo" no aparelho do dono continua de pé).
- Desconectar/Encerrar não foram *acionados* ao vivo (só conferidos por código) — evitar
  encerrar a única sessão moque ativa no meio da verificação.
- `tabs8u` não foi usado nesta rodada — a coerência do tablet (defeito 3) foi conferida só
  via `screens.mjs` (Robolectric) e diff dos dimens, não ao vivo no emulador do tablet.

## Recomendação para a próxima rodada

1. Repensar defeito 2: `TouchDelegate` não é a ferramenta certa quando o irmão que cobre
   a área expandida consome touch incondicionalmente (`backgroundTouchView`/
   `streamContainer`). Alternativas a avaliar: aumentar de verdade o `hitRect` só quando
   a guia está fechada e a expansão não sobrepor stream ativo de forma a interceptar
   ANTES do `backgroundTouchView`/`streamContainer` (ex.: um `onInterceptTouchEvent` na
   view pai que verifica a região antes de repassar), ou aceitar que só o desenho maior
   (contrariando o mockup) resolve de fato.
2. Defeito 4: dar ao alternador (e à fileira, quando expandida) uma margem inferior que
   respeite de verdade o inset do IME antes de desenhar — hoje sobra só 9dp acima do
   teclado real, o resto do alternador fica embaixo dele. Vale conferir se
   `ImeDisplayMode.RESIZE`/o consumo de `WindowInsetsCompat` está subtraindo a barra de
   navegação duas vezes ou nenhuma (a folga que falta, 63px, bate exatamente com o inset
   de navigationBar medido nesta verificação — `nonDecorInsets` bottom=63 — o que é um
   bom primeiro lugar para olhar).

Nenhuma correção foi commitada nesta verificação — os dois problemas acima não são
pequenos/óbvios o bastante para consertar com segurança dentro desta sessão (a área de
toque e o IME são justamente os pontos que o próprio pedido pediu para não mexer sem
cuidado redobrado).
