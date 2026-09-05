# Catálogo de produto

> Só o que o usuário vê, toca e sente. Interface, experiência, funcionalidade,
> fluxo. **Cite o código** para começar a trabalhar em um item.

Este catálogo é separado de propósito. O [CATALOGO.md](CATALOGO.md) cuida do lado de
dentro — decoder, JNI, áudio, robustez, dívida. Aqui não entra nada disso: se a ideia não
muda o que a pessoa vê, toca ou sente, ela não pertence a este arquivo.

Origem: análise multi-agente de 2026-08-21, seis lentes independentes de produto
(dia a dia · tesoura · sessão · identidade · descoberta e erro · poder), 64 propostas
brutas, deduplicadas para 45. Todas as afirmações verificáveis foram conferidas por
varredura própria no repositório antes de entrar aqui — o que está escrito abaixo como
fato foi medido, não relatado.

| Prefixo | Significado | Esforço |
|---|---|---|
| **PE** | Epic de produto — várias frentes, muda o desenho de um fluxo inteiro | vários dias |
| **P** | Item de produto — escopo fechado | curto (horas) ou dia |

---

## O que a varredura achou

Três fatos organizam tudo o que vem depois. Os três foram verificados neste repositório,
não herdados de relatório.

### 1. O desenho já foi feito. Ele só não foi ligado.

Sete ids estão desenhados em XML e **nenhuma linha de Java os procura**:

```
vidroAnticipation  vidroContinueSection  vidroContinueList
vidroLost  vidroLostRetry  vidroLostLeave  vidroLostRetryCount
```

E quinze strings estão escritas em `values/strings.xml` e **não são referenciadas por
nada** — nem Java, nem layout:

```
vidro_host_went_asleep  vidro_wake_pc  vidro_wake_takes
vidro_code_refused  vidro_code_expired  vidro_where_to_look  vidro_ask_new_code
vidro_pair_done  vidro_pair_done_sub  vidro_pair_see_apps  vidro_step_of
vidro_retry_count  vidro_lost_host
vidro_likely_because  vidro_resume_app
```

(Eram dezoito na primeira contagem. As três de pílula estavam órfãs por outro motivo — ver
[P11](#p11) — e já foram apagadas.)

O protótipo tem 7 telas e 4 interrupções. O app entrega as 7 telas e **nenhuma** das 4
interrupções. `vidro_lost.xml` está incluído em `activity_game.xml` em toda sessão e
nasce `gone` para sempre.

Isso muda o custo de metade deste catálogo: não é desenhar, é ligar.

### 2. O caminho feliz é Vidro. O caminho de erro ainda é Moonlight de 2015.

Toda vez que algo dá errado o app troca de personalidade — `AlertDialog` do sistema,
título "Connection Error", código hexadecimal, botão "Ajuda" que joga a pessoa na wiki de
outro projeto, fora do app. É exatamente o momento em que a pessoa mais precisa dele.

### 3. A tela de Ajustes tem 126 preferências em 13 telas.

Contagem exata de `res/xml/preferences.xml`: 139 chaves, das quais 13 são telas.
A tesoura deste catálogo tira ~50 delas da vista e 4 das 13 linhas do índice.
Amostra do que há ali dentro:

- `edit_diy_bitrate` é um campo de texto que grava na **mesma chave** do slider vizinho.
- A lista de perfis de teclado tem cinco opções e as cinco apontam para
  `@string/keyboard_layout_set_1` — na tela, cinco linhas escritas **"Profile 1"**.
  As strings `_2` a `_5` existem e ninguém as usa.
- `LanguagePreference` tem o caminho certo escrito e **inteiramente comentado**: as
  linhas 30-49 abrem `Settings.ACTION_APP_LOCALE_SETTINGS`, e o manifesto já declara
  `android:localeConfig`.
- Um resumo de preferência, em inglês, na tela: *"Does anyone use both together?"*
  O autor do upstream não sabia responder e mandou a dúvida para o usuário.

E dois achados de acabamento, também verificados:

- **A mola do toque não existe onde a pessoa toca.** `app_grid_item.xml` (o card de app) e
  `pc_grid_item.xml` (a linha de host) não têm `stateListAnimator`. `@animator/vidro_press`
  está aplicado em 11 superfícies secundárias e em nenhuma das duas primárias — e
  `styles.xml` ainda o zera com `@null` em três estilos de botão.
- **Dois terços do sistema de luz estão mortos.** `VidroLightView.setQuality()` existe e
  **nenhum chamador no app inteiro** — os vetores REACH e CHROMA que o handoff descreve
  ("a luz encolhe e esfria") são desenho que nunca acontece.
- **O tablet parou no meio.** Existem `layout-sw600dp-land/` para home e cenas. Não existe
  nenhuma variante de `activity_app_view.xml` (o catálogo) nem de
  `activity_stream_settings.xml` (Ajustes) — as duas telas que ficaram esticadas por
  1722 dp são exatamente as duas que ele marcou como "toda zoada".

---

## Fila priorizada

Ordem por ganho sentido ÷ esforço, com peso para: o que já está desenhado e só precisa ser
ligado, o que tira coisa da frente, e o uso diário no Tab S8 Ultra com a capa.

| # | Código | Proposta | Esforço |
|---|---|---|---|
| 1 | [**P24**](#p24) | Apagar os dois botões flutuantes que a ilha já substituiu | curto |
| 2 | [**P26**](#p26) | Tirar a taxa de bits da folha da sessão — ela promete "agora" e entrega "depois" | curto |
| 3 | [**P34**](#p34) | Pôr a mola do toque no card de app e na linha de host | curto |
| 4 | [**P01**](#p01) | Ligar a tela "Perdemos o PC" que está desenhada e nunca aparece | dia |
| 5 | [**P10**](#p10) | Ligar o "Continuar" da home e apagar o cartão de palpite | dia |
| 6 | [**P31**](#p31) | Terminar o tablet nas duas telas que ficaram de fora | dia |
| 7 | [**P16**](#p16) | Tirar de Vídeo o 3D, os experimentais e o interruptor morto | curto |
| 8 | [**P15**](#p15) | Cortar as portas duplicadas de taxa de bits e resolução | curto |
| 9 | [**P08**](#p08) | Abrir a página do PC direto da tela do código | curto |
| 10 | [**P04**](#p04) | Parar de expulsar do catálogo quando o host some | dia |
| 11 | [**P06**](#p06) | Acordar o PC do próprio cartão que diz que ele dorme | curto |
| 12 | [**P22**](#p22) | Consertar a lista dos cinco "Profile 1" | curto |
| 13 | [**P23**](#p23) | Lembrar o modo do mouse sempre | curto |
| 14 | [**P17**](#p17) | Reduzir os seis modos de mouse aos três que a folha já usa | curto |
| 15 | [**P29**](#p29) | Fazer cada escolha da folha dizer o que ela faz | curto |
| 16 | [**P12**](#p12) | Fazer a barra de conectar chegar a 100% antes de sumir | curto |
| 17 | [**P11**](#p11) | ~~Pílulas no catálogo~~ — **retirado**: foram removidas a pedido do dono, não esquecidas | — |
| 18 | [**P02**](#p02) | Recusar o código sem tirar a pessoa da tela | dia |
| 19 | [**P32**](#p32) | A folha esconde o que a capa já resolve | dia |
| 20 | [**P19**](#p19) | Refazer os Números como uma linha e apagar a tela que os configura | dia |
| 21 | [**P03**](#p03) | Errar o endereço dentro da tela do endereço | curto |
| 22 | [**P05**](#p05) | Dizer por que ninguém respondeu | curto |
| 23 | [**P09**](#p09) | Esconder a porta de pareamento que não existe no Vibeshine | curto |
| 24 | [**P25**](#p25) | Abrir a ilha de onde o punho está | dia |
| 25 | [**P14**](#p14) | Apagar dos Ajustes o que este host já disse que não faz | dia |
| 26 | [**P35**](#p35) | A tela do PC nasce do tamanho da sua | dia |
| 27 | [**P27**](#p27) | Matar o menu de Ações e devolver à folha o que sobrou | dia |
| 28 | [**P13**](#p13) | Três alturas de rede, o aviso como faixa, e a luz que nunca acende | dia |
| 29 | [**P30**](#p30) | Mostrar os gestos vivos dentro da sessão | dia |
| 30 | [**P07**](#p07) | Fechar o pareamento nos três passos do protótipo | dia |
| 31 | [**P18**](#p18) | Uma sensibilidade, não seis — e a tela "Trackpad virtual" some | dia |
| 32 | [**P21**](#p21) | A depuração vira folha de leitura com um botão Copiar | dia |
| 33 | [**P20**](#p20) | Devolver o idioma ao sistema | curto |
| 34 | [**P28**](#p28) | Trocar a lista de teclas especiais pela régua na tela | dia |
| 35 | [**P33**](#p33) | Esconder o punho quando a capa está acoplada | curto |
| 36 | [**P36**](#p36) | Levar as cenas de um aparelho para o outro | dia |
| 37 | [**P37**](#p37) | Pôr o que você faz sempre na tela de bloqueio | curto |

Depois da fila, os epics — na ordem [PE05](#pe05), [PE03](#pe03), [PE06](#pe06),
[PE01](#pe01), [PE02](#pe02), [PE04](#pe04), [PE08](#pe08), [PE07](#pe07).

---

## Epics de produto

<a id="pe01"></a>
### PE01 — A sessão se remenda sozinha

**Ganho.** Um cochilo de Wi-Fi — trocar de ponto de acesso andando pela casa — hoje mata a
sessão, sobe uma caixa de erro e devolve a pessoa ao catálogo. Passa a segurar o último
quadro, tentar de novo por conta própria e voltar no mesmo lugar, contando a tentativa em
voz baixa. Ele vê a imagem congelar dois segundos em vez de perder o jogo.

**Onde.** `Game.connectionTerminated`, no caminho em que o vídeo **já estava rodando** —
o caso que [M99](planos/MELHORIAS.md#m99) deixou de fora de propósito. A tela está pronta:
`res/layout/vidro_lost.xml`, com "Tentar de novo", "Sair" e o contador
`vidroLostRetryCount`.

**Como.** Ao cair sem `ML_ERROR_GRACEFUL_TERMINATION`, não desmontar: escurecer o vídeo,
subir a folha com o motivo real que M99 já sabe extrair, e refazer a conexão em até cinco
tentativas com espera crescente. O texto de apoio da especificação — *a sessão continua
aberta no PC por mais alguns instantes* — passa a ser verdade em vez de consolo. Só depois
do teto a folha fica parada, com as duas saídas.

Fatia 1 é [P01](#p01) (ligar a folha, sem contador). O contador **só entra depois** da
política de retentativa: prometer "3ª de 5" sem ela é mentir de novo.

**Risco.** O maior do catálogo. A Activity é `noHistory` + `singleTask` + `excludeFromRecents`
e M99 já avisa que relançar de dentro dela pode chegar como `onNewIntent`. Retentativa
cega contra host realmente desligado vira cinco esperas inúteis — teto, recuo crescente e
desistência visível são o que separa isto de uma regressão. E nunca retentar quando outro
cliente assumiu a sessão: é insistir onde não há vaga. **Medir no aparelho** com o host
desligado no meio da sessão antes de escrever a primeira linha.

<a id="pe02"></a>
### PE02 — Trocar de app sem sair da sessão

**Ganho.** Sair do Desktop e entrar no jogo hoje é: sair da tela → esperar a home → entrar
no lugar → esperar o catálogo → tocar o outro app → ler um menu de contexto → confirmar →
tocar de novo. Quatro telas. Passa a ser: um bloco na ilha, a grade por cima do vídeo, um
toque. É a diferença entre "o PC é duas coisas" e "tenho que escolher qual das duas hoje".

**Onde.** Um quarto bloco na fileira de `vidro_session_island.xml`, ao lado de
`vidroTileKeyboard`/`Mouse`/`Details`. A grade reaproveita `app_grid_view.xml`; a espera
reaproveita `vidro_connecting.xml`, que já mora dentro da Activity `Game` e já mostra os
três passos reais. A execução existe: `ServerHelper.doStart`/`doQuit`.

**Como.** A folha abre sobre o vídeo sem desmontar nada — mesma técnica da folha lateral,
que também é View e não Activity. Escolher encerra o app atual e inicia o novo pela
sequência que o catálogo já chama, com a camada de conectar de pé mostrando o nome do novo
app, em vez de piscar preto. O app aberto continua com a pílula "aberto" ([P11](#p11)), então
tocar nele é só voltar.

**Risco.** O stream reinicia de verdade — a `StreamConfiguration` é montada ao conectar e
não dá para renegociar de pé. A promessa que dá para cumprir é **continuidade visual**, não
"não reconectar"; se aparecer como queda, lê como bug. O encerrar tem de ramificar no
retorno de `quitApp()`, como M99 já corrigiu no caminho de sair. E **medir no Vibeshine**:
confirmar que trocar de app não derruba e recria o display virtual do cliente.

<a id="pe03"></a>
### PE03 — Uma folha só para "mais sobre este cartão"

**Ganho.** Tudo que hoje só existe se você já sabe que existe ganha porta visível. Acordar,
parear, encerrar o que está aberto, remover: hoje são **14 `menu.add` no `PcView` e 12 no
`AppView`** — dois menus de contexto do Android, com a moldura, a tipografia e o
espaçamento do sistema, abertos por um toque longo que nada na tela anuncia, no meio de um
app que passou meses construindo os próprios.

**Onde.** `PcView.onCreateContextMenu` e `AppView.onCreateContextMenu`.

**Como.** Uma folha lateral Vidro — a mesma peça que a sessão já usa — aberta por um alvo
visível no cartão, com **no máximo quatro linhas por vez**, escolhidas pelo estado real:
dormindo mostra acordar; não pareado mostra parear; rodando mostra retomar e encerrar. O
resto desce para "mais" ou some. Metade do valor está na varredura: no catálogo, "Encerrar
o jogo atual e iniciar no display virtual" ao lado de "…no display primário" são duas
linhas para uma decisão que **no Vibeshine o host toma sozinho** — viram uma. "Detalhes"
hoje abre um diálogo com `app.toString()` cru: ou vira informação legível, ou sai.

**Risco.** É o item mais fácil de virar redesenho sem fim. O corte precisa ser decidido item
a item **antes** de escrever código, e cada item removido precisa de destino ou de argumento
— senão vira feature perdida em vez de podada. Começar só pela home; catálogo depois.

<a id="pe04"></a>
### PE04 — A paleta de comandos

**Ganho.** Com a capa acoplada, um acorde abre um campo: ele digita "desk" e entra no
Desktop, "alt f4", "trackpad", "números", "sair". Um lugar em vez de quatro — menu de
sessão, submenu de teclas especiais com quinze linhas, folha lateral e seletor de app. É a
versão de teclado do "tirar três coisas confusas e pôr uma boa", e nada disso pede que ele
tire as mãos do teclado.

**Onde.** A peça já existe e já está resolvida: a barra de composição virou ilha de foco em
[M98](planos/MELHORIAS.md#m98) — `Game.dispatchKeyEvent` isola o teclado, Esc fecha,
Ctrl+Alt+Shift+T abre. Substitui `GameMenu.showMenu` e `GameMenu.showSpecialKeysMenu`.

**Como.** O mesmo campo, em outro modo: em vez de mandar texto ao PC, filtra uma lista
curta e executa. Índice único — comandos do menu, interruptores da folha (com o estado à
mostra), teclas especiais e os apps do host. Setas navegam, Enter executa, Esc fecha. No
celular, sem teclado, nada muda: continua a ilha e a folha, que é onde o dedo manda.

**Risco.** É a proposta que mais pode engordar o app. Dois contratos duros: (a) **só entra na
paleta o que já existe em outra superfície** — ela é atalho, nunca a única casa de um
comando; (b) **os menus morrem no mesmo commit** — paleta somada a menu é engorda pura, e é
exatamente do que ele fugiu no V+. O acorde precisa ser escolhido depois de medir o que a
capa entrega ([B70](planos/BUGS.md#b70)).

<a id="pe05"></a>
### PE05 — O app percebe a capa

**Ganho.** O tablet com a capa é um laptop e o app não sabe. Acoplar deixa de exigir quatro
ajustes na mão — modo do mouse para trackpad, desligar o controle na tela, desligar o
teclado na tela, escolher a forma de desktop. Acoplou, já está assim; tirou a capa, volta a
ser console, com o controle na tela de volta. É o único ajuste que o aparelho pode fazer
sozinho e acertar em 100% dos casos.

**Onde.** O sensor já existe: `Game.hasHardwareKeyboard()` testa `KEYBOARD_QWERTY` junto de
`HARDKEYBOARDHIDDEN_NO`, exatamente para não se enganar com a capa dobrada para trás. Falta
`onConfigurationChanged` reagir. Os interruptores que ele passaria a decidir são os de
`vidro_session_sheet.xml`.

**Como.** Duas formas do app, não um perfil novo: **com capa** e **sem capa**. A troca
acontece sem desmontar o stream, e a folha mostra o que mudou por um instante — quem
discordar mexe ali, e o manual ganha do automático até o fim da sessão. Nenhuma preferência
nova: quem decide é o estado do aparelho. Recolhe [P32](#p32) e [P33](#p33) como fatias.

**Risco.** Trocar coisa debaixo da mão de quem não pediu é o pior tipo de esperteza — só
vale se o que mudou for **dito e desfeito num toque**. Esconder um interruptor que está
LIGADO deixa a pessoa sem como desligar: a regra é esconder só o que está desligado, e
desligar junto o que estiver ligado quando a capa acopla. `keyboard|keyboardHidden` precisa
estar em `configChanges` ou a Activity é recriada. **Medir no S8 Ultra** acoplando e
soltando: a detecção por `Configuration` erra em alguns aparelhos.

<a id="pe06"></a>
### PE06 — A cena de quatro escolhas

**Ganho.** Criar uma cena deixa de ser configurar o app inteiro de novo. Hoje o botão diz
"Salvar a sessão atual como cena" e abre **as 126 preferências** pré-preenchidas — por isso
ninguém salva nenhuma. Passa a ser: você acabou de ajustar a folha no meio da sessão, toca
"Salvar como cena", ela se chama sozinha, e da próxima vez é um toque.

**Onde.** `EditProfileActivity` estende o fragmento de Ajustes e infla o **mesmo**
`R.xml.preferences` — a captura `00-cena-viva.png` mostra "New Profile" seguido de "Video
Settings", "Audio Settings", "Gamepad Settings", a mesma lista de `tab-00-ajustes-viva.png`.
O bloco que esconde item por item traz no código o comentário do upstream: *"FIXME: Shitty
code written by previous implementations, too much to fix"*.

**Como.** A especificação já diz o que uma cena é: *"Cada uma define papel, tela, taxa e
entrada"*. Quatro coisas. O editor vira uma tela única com quatro escolhas em chips — o
mesmo vocabulário da folha de sessão, não outro — e um nome. Tudo mais herda do global e não
aparece. A árvore completa continua atrás de uma porta "Avançado", não como entrada. Isso
apaga também o marcador `*` de `highlightPreferences`, que só existe porque a tela mostra
126 linhas das quais quatro interessam.

**Risco.** O modelo guarda **snapshot completo, não delta** ([B29/M61](CATALOGO.md)): o
cartão de quatro escolhas precisa escrever quatro chaves e parar de congelar o resto junto,
senão a cena continua carregando lixo de runtime, só que invisível. Cenas existentes pedem
migração — ler o snapshot, extrair as quatro dimensões, decidir explicitamente o que fazer
com uma cena que mude algo fora delas.

<a id="pe07"></a>
### PE07 — Aposentar o teclado que o app desenha

**Ganho.** Some a maior fonte de configuração do app inteiro — 13 preferências em duas
telas, uma delas a lista dos cinco "Profile 1" — e some a laje que nasce com metade da
altura da tela por cima do vídeo. O que ele digita passa a ser o teclado da capa e a barra
de composição.

**Onde.** `layout_axixi_keyboard.xml` (899 linhas), o pacote
`binding/input/virtual_controller/keyboard/` (12 arquivos, 4.236 linhas), a tela
`category_special_key_layout` inteira, e seis preferências de tamanho/opacidade/alinhamento
na tela de UI.

**Como.** O motivo de esse teclado existir era que o IME cobria o vídeo.
[E01](epics/E01-teclado-anydesk.md) acabou com isso — o IME empurra e redimensiona — e a
barra de composição resolve o envio de texto. O motivo caiu; a peça pode cair junto.
**Manter a barra de teclas especiais** (Esc, Ctrl, Alt, F-keys): essa não tem substituto no
IME e é a única metade que ainda paga por si. Com isso `toggleFullKeyboard()` e o gesto de
quatro dedos saem, a folha perde a linha "Teclado completo", e metade de
[Q35](planos/QUICK-WINS.md#q35) se resolve apagando a causa em vez do sintoma.

**Risco.** O maior desta lista. **No S22 Ultra na mão não há teclado físico** — pode ser que
o teclado próprio seja o que ele usa lá hoje. Pré-requisito duro: E01 validado nos **dois**
aparelhos antes de apagar a alternativa. Se não estiver, este epic espera. O import/export de
layouts sai junto e não volta.

<a id="pe08"></a>
### PE08 — A capa tocada cresce até virar a tela

**Ganho.** É o movimento assinatura da identidade e o app não o faz. Você toca em "Desktop"
no canto da grade e a tela inteira dá um pop de 94% **a partir do centro** — a capa que você
tocou não vai a lugar nenhum, e na volta o vídeo não encolhe para o card de onde veio. É a
diferença entre um app que responde ao seu dedo e um que troca de página perto de onde você
tocou. É também o momento mais repetido do dia e o único em que o app ainda parece um app
Android comum.

**Onde.** `styles.xml` (`VidroWindowAnimation`) e `res/anim/vidro_enter.xml` — o `scale` de
0.94 usa `pivotX/pivotY=50%p`, o centro da tela. Os tempos já estão certos em
`values/motion.xml`: 420 ms na ida, 340 na volta. Não há `ActivityOptions` nem elemento
compartilhado em lugar nenhum do app.

**Como.** `makeSceneTransitionAnimation` com o `grid_poster` de `app_grid_item.xml` como
elemento compartilhado, de `AppView` para `Game`, com as curvas `@interpolator/vidro_enter`
e `vidro_exit` que já existem. O handoff é literal sobre o que anima: `transform`, `alpha` e
raio do card — nada de blur, nada de sombra. A saída faz o inverso.

**Risco.** Elemento compartilhado com `SurfaceView` é traiçoeiro: a transição precisa
terminar **antes** de o vídeo começar a desenhar, senão o primeiro quadro aparece dentro da
animação e pisca. `Game` monta flags de janela no `onCreate` — região que o AGENTS.md manda
tratar em plan mode. `noHistory` + `singleTask` limita o que dá para animar na volta.
**Medir o atraso até o primeiro quadro nos dois aparelhos; se custar mais que ~1 quadro, cai
fora.**

---

## Fio 1 — O caminho de erro ainda é Moonlight de 2015

<a id="p01"></a>
### P01 — Ligar a tela "Perdemos o PC" · dia · substitui

Quando a rede cai no meio da partida, o vídeo escurece e a tela diz "Perdemos o
PC-RAFAEL", com a frase que importa — a sessão continua aberta do outro lado — e "Tentar de
novo" grande, na faixa de baixo onde o dedo já está. Hoje sobe uma caixa de diálogo por cima
do vídeo, com botões de texto e a linguagem de erro que esta identidade jurou não usar.

`vidro_lost.xml` está pronto e incluído em `activity_game.xml`. Ligar a folha em
`VidroSession` (irmã de `showConnecting`/`dismissConnecting`) e chamá-la de
`connectionTerminated` no caminho em que o stream já subiu. As strings `vidro_lost_host` e
`vidro_retry_count` existem sem uso. A folha de M99 fica só para a falha **antes** do lado
nativo subir, que é o escopo que ela já declarou.

**Primeira fatia sem contador** — ele só faz sentido depois de [PE01](#pe01).

<a id="p02"></a>
### P02 — Recusar o código sem tirar a pessoa da tela · dia · substitui

Erra o código e continua exatamente onde estava, com um código novo num toque — em vez de
ver uma torrada de quatro segundos, ser devolvido para a home e ter que reencontrar o cartão
do host para recomeçar.

A tela 11 já está escrita e nunca foi ligada: `vidro_code_refused`, `vidro_code_expired`,
`vidro_where_to_look`, `vidro_ask_new_code`. Em vez de esconder a tela e torrar, o mesmo
`FrameLayout` troca de estado: os quatro dígitos apagam, entra o título da recusa, a causa
mais provável primeiro (expirou), e "Pedir um código novo" refaz o `pm.pair()` sem sair da
tela. Nada de "erro", nada de código de status.

**Risco:** `ALREADY_IN_PROGRESS` não é expiração e não pode receber a mesma frase; a
retentativa precisa reaproveitar o `cancelPairing`, senão o `/unpair` do
[B69](planos/BUGS.md#b69) dispara em cima de um pareamento novo.

<a id="p03"></a>
### P03 — Errar o endereço dentro da tela do endereço · curto · substitui

Digita o IP errado e a própria tela diz o que houve, com o campo ainda ali para corrigir uma
letra. Hoje abre um `AlertDialog` com o título "Connection Error" — a palavra que o handoff
proíbe — e um botão "Ajuda" que joga você na wiki do Moonlight, fora do app, no meio de
adicionar o primeiro PC.

Os três desfechos de `AddComputerManually` viram uma linha de causa abaixo do campo, dentro
do vazio que já está lá, com o texto preservado e o teclado de pé. E o vazio ganha o que ele
pediu na anotação ("centralizar talvez"): os endereços já tentados, para o segundo aparelho
não redigitar. No `addpc_fail` contra endereço privado a frase sobre firewall nem se aplica —
`NetworkScope` já sabe que é rede local.

<a id="p04"></a>
### P04 — Parar de expulsar do catálogo quando o host some · dia · tira

O PC dorme enquanto você escolhe o jogo e você continua na mesma tela, com a grade que já
carregou, e acorda de lá. Hoje você é jogado de volta para a home com uma torrada de dois
segundos, perde o rolamento e não sabe se fez algo errado.

`AppView` chama `finish()` quando o estado vira OFFLINE e de novo quando o pareamento cai.
Tirar os dois: a tela fica, a grade esmaece, e a faixa que já existe no topo passa a carregar
o estado — "PC-RAFAEL foi dormir" com "Acordar o PC" ao lado. Quando a sondagem devolver o
host, a faixa sai e a grade acende.

**Risco:** o toque na grade esmaecida tem de ficar inerte, ou a pessoa dispara conexão contra
host que não está lá.

<a id="p05"></a>
### P05 — Dizer por que ninguém respondeu · curto · substitui

Na primeira vez o app diz "você está nos dados móveis" ou "a VPN está ligada — nenhum PC da
sua casa aparece assim", em vez de deixar a pessoa esperando por um mDNS que nunca vai
chegar. E depois de meio minuto de silêncio, diz a única coisa que um primeiro usuário não
tem como adivinhar: que o host precisa estar rodando no PC.

O comentário do próprio layout já sabe o problema. O app tem `ACCESS_NETWORK_STATE` e
`ACCESS_WIFI_STATE` no manifesto e não os usa aqui. A mesma frase ganha três versões,
escolhidas pelo `ConnectivityManager`.

**Risco:** é risco de redação, não funcional. Três frases viram quatro viram um parágrafo, e
aí vira o "muito texto" que ele reclamou. Uma linha, no máximo duas, sempre no mesmo lugar.

<a id="p06"></a>
### P06 — Acordar o PC do próprio cartão · curto · substitui

Ver "O PC-RAFAEL está dormindo" e poder acordá-lo dali, com a espera contada, em vez de
descobrir por tentativa que a função existe escondida num toque longo. Hoje tocar devolve
`error_pc_offline` e acabou: beco sem saída, na tela de entrada do app.

As três strings existem sem uso: `vidro_host_went_asleep`, `vidro_wake_pc`,
`vidro_wake_takes`. `WakeOnLanSender` já existe. O cartão entra em espera com o tempo
escrito, a sondagem de 1,5 s que já roda termina a espera, a luz da home sobe de 0,20
conforme o host responde — que é para isso que a luz existe — e o app **continua o gesto**
em vez de devolver à lista. O item do menu de contexto sai.

**Risco:** sem MAC gravado não há WoL — o cartão tem de dizer isso em vez de fingir espera
([M01](planos/MELHORIAS.md#m01) conserta o parsing e o broadcast dirigido). Prometer 20 s e
não entregar é pior que não oferecer.

<a id="p07"></a>
### P07 — Fechar o pareamento nos três passos · dia · substitui

O primeiro encontro com um PC é a primeira impressão do app, e hoje ele tem **um passo dos
três**: os quatro dígitos, sem "PASSO 2 DE 3" em cima, sem o passo 1 de procura, sem o passo
3 que mostra o que o host publica e termina em "Ver os aplicativos".

Oito strings escritas e sem uso: `vidro_step_of`, `vidro_code_refused`, `vidro_code_expired`,
`vidro_where_to_look`, `vidro_ask_new_code`, `vidro_pair_done`, `vidro_pair_done_sub`,
`vidro_pair_see_apps`.

**Risco:** frequência baixíssima para quem já pareou. Vale como acabamento, não como
prioridade — mas é uma das quatro interrupções do protótipo e a única que ainda cai numa
caixa de diálogo do sistema.

<a id="p08"></a>
### P08 — Abrir a página do PC direto da tela do código · curto · substitui

Pareia de primeira com o Vibeshine em vez de ficar olhando quatro dígitos esperando um
diálogo que nunca vai aparecer na tela do PC. **A tela mente para o host dele:**
`vidro_pair_code_help` diz "The host will ask for this code on the PC screen" — verdade no
Apollo; no Vibeshine o código vai na página web do host.

O app já sabe montar essa URL — `ComputerObject.guessManagementUrl()` devolve
`https://<endereço>:<porta+1>` — mas só a oferece dentro de um menu de contexto que fica
atrás da tela de pareamento e não pode ser alcançado enquanto ela está aberta. Trocar a
frase e pôr embaixo um botão "Abrir a página do PC", copiando o código no mesmo toque.

**Risco:** o navegador vai reclamar do certificado autoassinado antes de abrir — o botão
precisa avisar numa linha, ou parece que o app mandou a pessoa para um lugar quebrado.

<a id="p09"></a>
### P09 — Esconder a porta de pareamento que não existe · curto · tira

Na hora de parear o primeiro PC você vê uma opção, não duas com nome de engenharia. Hoje o
menu oferece "OTP Pair" acima de "Legacy Pair"; quem escolhe a primeira — a de cima, a que
parece mais moderna — cai num `AlertDialog` cru com dois campos sem estilo e falha, porque
**OTP só existe no Apollo**. A própria string do app admite isso.

Amarrar o item ao bit de capacidade que [E03](planos/EPICS.md#e03) já modelou. Com Vibeshine
ele não aparece e sobra um caminho só — que é o que o toque simples no cartão já dispara.

**Risco:** o ramo positivo do E03 só existe em teste, porque não há Apollo à mão. Com a
armadilha do `-1` já tratada, o ramo Vibeshine é o seguro.

---

## Fio 2 — Desenhado e nunca ligado

<a id="p10"></a>
### P10 — Ligar o "Continuar" e apagar o cartão de palpite · dia · substitui

Pega o tablet, abre o app, e o que ele abriu ontem já está na primeira dobra. Some a viagem
lugar → catálogo → esperar o `/applist` → achar a capa. **No tablet deitado é exatamente a
coluna esquerda de 420 dp que hoje fica vazia** — o "espaço vazio no tablet" que ele marcou
é este buraco.

Não inventar palpite: usar fato. A sondagem já traz `runningGameId`/`runningGameUUID` — quando
há algo rodando, não é chute. Sem nada rodando, cair para as últimas sessões, gravadas em
`ServerHelper.doStart`, que é o funil único de lançamento. O toque chama `doStart` direto,
sem passar pelo `AppView`.

E **apagar** `vidroAnticipation` dos três layouts, junto com `vidro_likely_because`,
`vidro_resume_app` e `vidro_likely_now`: com um host só ele diria exatamente a mesma frase
que a primeira linha de "Continuar", mas como adivinhação — e palpite que acerta sempre é só
um atalho com roupa de palpite.

**Risco:** o appId pode não existir mais no host — a linha tem de cair para o catálogo em vez
de falhar. Não confundir com [Q33](planos/QUICK-WINS.md#q33): aquilo retoma sessão **aberta**;
isto lança quando não há nada rodando. E o card novo tem de entrar na comparação de
`PcCellSnapshot` ([Q36](planos/QUICK-WINS.md#q36)), senão desfaz o ganho de fluidez.

<a id="p11"></a>
### P11 — ~~As pílulas que separam jogo, tela e entrada~~ · RETIRADO

**Este item foi escrito por engano e não deve ser executado.**

Eu o propus depois de uma varredura que encontrou `vidro_pill_screen`, `vidro_pill_input` e
`vidro_pill_open` sem nenhum consumidor, e li isso como "a peça foi desenhada e nunca
ligada" — que é o padrão real do resto deste catálogo. Aqui a causa era outra: as pílulas
existiam, funcionavam, e tinham sido **removidas a pedido do dono do fork**, que marcou as
três com X e escreveu *"remover"* nas anotações da tela de apps. Numa grade de capas elas
competiam com a arte.

Quem está aberto continua sendo dito duas vezes, e sem custar pixel sobre a capa: pela
máscara escura no card e pela faixa de retomada no topo da tela.

O que sobreviveu, de propósito, é o discriminador em `AppGridAdapter`: os IDs sintéticos
`2147483505` (Remote Monitor) e `2147483506` (Remote Input), medidos contra o Vibeshine de
verdade. É a única coisa no app que separa uma sessão sintetizada pelo host de um jogo, ele
sobrevive a renomeação e ao idioma do host, e não custa nada parado. Qualquer ideia futura
sobre esses dois cards começa por ele — mas **não** por uma pílula sobre a arte.

<a id="p12"></a>
### P12 — A barra de conectar chega a 100% antes de sumir · curto · acrescenta

A tela de conectar tem três passos e a pessoa vê dois. O terceiro — "Chegando o vídeo", 100%
— é escrito e escondido **no mesmo quadro**: `onConnected()` faz `setText`, `setProgress(100)`
e `setVisibility(GONE)` na mesma passada. A barra desaparece parada em 62%, com corte seco
para o vídeo. É o único momento do app em que a espera tem forma, e ela termina no meio.

Segurar o passo 3 pelo tempo de a barra completar e sair com alpha em `vidro_motion_exit`
(340 ms) em vez de `GONE` cru. Nada de temporizador nos passos: eles continuam presos aos
estágios reais do `NvConnection`.

**Risco:** tem de ser alpha por cima do vídeo **já rodando**, não espera antes de conectar,
ou atrasa o primeiro quadro.

<a id="p13"></a>
### P13 — Três alturas de rede, e a luz que nunca acende · dia · substitui

O protótipo tem três qualidades de rede — "tudo fluindo bem", "segurando o sinal", "ruim
agora". O app tem duas: o ponto pinta igual para ótima e instável, e **o estado do meio, que
é justamente o aviso prévio, é invisível**. O aviso, quando enfim aparece, é uma pílula
alinhada à direita com título e apoio grudados por um ponto médio, não a faixa de duas linhas
no topo que a tela 08 desenha.

E o achado maior, verificado: **`VidroLightView.setQuality()` não tem um único chamador no
app inteiro.** Os vetores REACH e CHROMA — "a luz encolhe e esfria", um dos três trabalhos
que o handoff dá à luz — são desenho que nunca acontece. Ou a luz do conectar e do catálogo
passa a usar o canal, ou ele sai da classe. Ter dois terços do sistema de luz mortos é o que
faz a identidade parecer aplicada por fora.

**Risco:** aviso que aparece demais vira ruído, e a regra é explícita: *aparece só quando
piora de verdade, e some sozinha*. O limiar do estado do meio precisa de **medição no
Vibeshine** antes de existir.

---

## Fio 3 — A tesoura: 126 preferências

> Critério, não gosto. Cortei o que cai numa destas cinco peneiras: (1) o app já sabe a
> resposta e mesmo assim pergunta; (2) duas portas para o mesmo valor; (3) vocabulários que
> não batem entre telas; (4) texto que descreve o que já não existe; (5) peça que o protótipo
> já substituiu.
>
> **O que NÃO cortar:** resolução, quadros, taxa de bits, ritmo de quadros, HDR e canais de
> áudio — são as seis decisões que mudam o que ele vê, e ficam *primeiro*. `force_qwerty`,
> `back_as_meta`, `enable_commit_text` e `list_ime_display_mode` — são a capa e o E01, a dor
> principal. A calibração por gesto e `trackpad_swap_axis` — preferência que existe porque o
> hardware mente é preferência legítima. `physical_trackpad_native`
> ([B65](planos/BUGS.md#b65)) — não se apaga o interruptor de uma correção antes de saber se
> a correção funciona. E o menu em lista quando a sessão está em tela externa: lá a ilha está
> na TV, longe do dedo.

<a id="p14"></a>
### P14 — Apagar o que este host já disse que não faz · dia · tira

Ele para de ligar interruptores que nunca acontecem e depois caçar o motivo. No Vibeshine a
linha "Geral" some inteira do índice e Vídeo perde três linhas — sem que ele precise saber o
que é Apollo. Três resumos já trazem "requires Apollo" escrito.

`StreamSettings` já remove item por chave conforme o **aparelho** tem vibração, USB,
giroscópio, HDR. Estender ao **host**: `HostCapabilities` e `vDisplaySupported` já dão a
resposta antes de a tela abrir. Regra de ouro: esconder **apenas** quando a resposta é um NÃO
conhecido; host offline mantém tudo à mostra.

**Risco:** Ajustes abre da home, onde pode não haver host selecionado — precisa de dono claro
da resposta (o último pareado) e degradação para "mostra tudo".

<a id="p15"></a>
### P15 — Cortar as portas duplicadas · curto · tira

Some a dúvida "qual dos dois campos manda?". Há um slider de taxa de bits e, sete linhas
abaixo, `edit_diy_bitrate` — um campo de texto que grava na **mesma chave** e ainda dá um
toast próprio dizendo "sucesso". Mesmo caso em `edit_diy_w_h`: `addNativeResolutionEntries`
já injeta a resolução nativa do aparelho na lista.

Se o teto de 300 Mbps do slider for baixo demais algum dia, **sobe-se o teto** — não se
acrescenta um segundo controle.

**Risco:** `edit_diy_w_h` é a única forma de pedir resolução fora da lista. Se ele já usou
alguma, migrar o valor salvo para uma entrada extra antes de apagar.

<a id="p16"></a>
### P16 — Tirar de Vídeo o 3D, os experimentais e o morto · curto · tira

A tela mais usada dos Ajustes cai de 24 linhas para 12 — cabe numa tela do tablet sem rolar.
E ele deixa de ver **quatro controles de estereoscopia** num aparelho que não é
estereoscópico: `render_mode_list`, `parallax_depth`, `convergence_ratio`, `balance_shift`.

`checkbox_forceTightThresholds` é o mais fácil: [B27](planos/BUGS.md#b27) já confirmou que é
lido por reflection sobre um campo que não existe — está morto. "Fully External Display Mode
(Beta)" tem no resumo *"Matches everything automatically; manual settings ignored"*: um
interruptor que anula os outros da mesma tela não pertence à mesma tela que eles. Os quatro
de 3D são a metade **visível** de M96 — dá para tirar as linhas hoje sem encostar nos 17 MB
de TFLite/OpenCV.

**Risco:** `ultra_low_latency` e o LFR podem estar ligados hoje e influenciando a latência
que ele sente. Ler o valor atual antes: se estiverem ligados, ou promove-se a padrão, ou
mede-se. **Não se apaga preferência ligada sem saber o que ela fazia.**

<a id="p17"></a>
### P17 — Seis modos de mouse viram três · curto · tira

Ele para de escolher em Ajustes um modo que a folha de sessão não sabe mostrar. Hoje, se
escolher "Track pad (Gaming/Long press to drag)", a folha acende o chip "Trackpad" e o
primeiro toque nesse chip **rebaixa o modo em silêncio**.

Reduzir aos três da folha — Multitoque, Absoluto, Trackpad — com os mesmos rótulos, palavra
por palavra. Quem precisar da inversão de botões ganha um interruptor dependente.
`checkbox_absolute_mouse_mode` cai junto: é um terceiro caminho para o mesmo estado.

**Risco:** "Disabled" existe para jogar só de controle sem o toque virar mouse. Não apagar —
vira um interruptor "O toque na tela move o mouse", que é o que ele realmente significa.

<a id="p18"></a>
### P18 — Uma sensibilidade, não seis · dia · substitui

Uma linha inteira do índice some, e ele deixa de escolher entre "Sensibilidade do trackpad
X", "Sensibilidade do touchpad X" e "Ajuste de sensibilidade da tela X" — três nomes quase
iguais em duas telas. A tela `category_virtual_trackpad_settings` inteira (7 itens) some.

Ler o resumo do upstream antes de decidir: ele termina com **"Does anyone use both
together?"**. A documentação da preferência é uma pergunta sem resposta.

**Risco:** o trackpad da capa e o dedo na tela têm escalas diferentes de verdade. Mitigação:
guardar por tipo de ponteiro **por baixo**, mostrar um controle só, trocar o valor conforme o
que está acoplado. O usuário vê um; o app guarda dois.

<a id="p19"></a>
### P19 — Os Números viram uma linha · dia · substitui

Ligar "Números" hoje derrama oito linhas de inglês cru sobre o vídeo — *"Host processing
latency min/max/average: 3.0/9.0/4.1 ms"* — num app cuja tese é que em sessão nada se lê. E o
canto superior esquerdo, que é uma das oito âncoras do punho, fica tomado.

Uma faixa Vidro de uma linha, quatro valores em JetBrains Mono tabular (rede · decodificação
· perda · fps), ancorada no canto **oposto** ao punho. O relatório completo continua em
Ajustes › Depuração, que é onde se lê parado. E a tela `category_perf_monitor_settings` some
inteira: o resumo de uma de suas preferências é *"Útil com Exibir no topo-centro e Modo
reduzido ligados"* — o retrato do problema, uma preferência cuja ajuda é a combinação de
outras duas. Morre junto o comportamento escondido de tocar no overlay para abrir menu.

**Risco:** a string é montada dentro do renderer e reaproveitada pela tela de depuração e
pela tela externa — precisa virar dois formatos com **uma** fonte, não um `if` a mais no laço.
Largura fixa nos números, senão a faixa dança a cada quadro.

<a id="p20"></a>
### P20 — Devolver o idioma ao sistema · curto · tira

A tela "Diversos" deixa de ter um seletor que o Android já dá. `LanguagePreference` tem o
caminho certo escrito e **inteiramente comentado** — as linhas 30-49 abrem
`Settings.ACTION_APP_LOCALE_SETTINGS` — e o manifesto já declara
`android:localeConfig="@xml/locales_config"`. Descomentar e apagar a lista de ~20 idiomas de
dentro do app. Junto: dos dois links de atualização, um só; e os dois links para a wiki de
outro projeto saem.

<a id="p21"></a>
### P21 — A depuração vira folha de leitura · dia · substitui

A tela de depuração — quatro botões de teste com texto cortado e um despejo em inglês cru,
visível em `00-debug-viva.png` — vira algo que ele consegue copiar e colar quando precisa
pedir ajuda: aparelho, Android, host, versão, e um botão **Copiar**. "Test Device Vibration",
"Refresh gamepad list" e "Test gamepad rumble" saem: testar vibração é papel do slider de
vibração, não de um painel separado.

**Risco:** o bloco que lista fontes e faixas de eixo de cada `InputDevice` foi o que permitiu
descobrir que **a capa do S8 Ultra se anuncia deitada**. É diagnóstico real: mantê-lo,
recolhido, dentro do que o "Copiar" leva junto.

<a id="p22"></a>
### P22 — Consertar a lista dos cinco "Profile 1" · curto · substitui

Uma lista de cinco opções em que **as cinco linhas dizem "Profile 1"**. O array aponta cinco
vezes para `@string/keyboard_layout_set_1`; as strings `_2` a `_5` existem e ninguém as usa.
Se [PE07](#pe07) acontecer, isto morre junto; enquanto não acontecer, é uma linha de arquivo
XML para a lista parar de ser impossível de usar.

<a id="p23"></a>
### P23 — Lembrar o modo do mouse sempre · curto · tira

Ele escolhe "Trackpad" para usar o PC como desktop; na sessão seguinte está tudo de volta ao
que era e ele escolhe de novo. Toda vez. `Game.setMouseMode` só grava se
`rememberMouseMode`, e o padrão é `false`.

Gravar sempre e apagar a caixa que pergunta se deve lembrar. `checkbox_remember_zoom_pan` é a
mesma pergunta com outro nome, mas merece segundo olhar: zoom herdado entre sessões pode
surpreender — ou some junto, ou passa a nascer ligada.

---

## Fio 4 — A sessão

<a id="p24"></a>
### P24 — Apagar os dois botões flutuantes · curto · tira

*"O botão flutuante atrapalha"* — e hoje ele só está escondido, não removido: a preferência
continua nos Ajustes, os dois arrastadores continuam montados, e ele **volta sozinho quando a
sessão sai do PiP**. São dois discos de 36 dp (abaixo do alvo mínimo), sem vidro, sem acento,
sem raio de token, com `elevation 9999dp` para ficarem por cima de tudo — inclusive da ilha.

A ilha e o punho já fazem o que os dois faziam, inclusive o arrastar, que o punho faz melhor
porque tem âncoras. Removê-los leva junto `initFloatingButton()`, o método órfão
`toggleFloatingButtonVisibility()` (sem nenhum chamador), a restauração no PiP e as duas
preferências.

**Risco:** a visibilidade está amarrada a `enableBackMenu`, que também decide o que o botão
Voltar faz em sessão — decidir junto com [P27](#p27), senão sobra preferência mandando em
nada. E o resgate de cinco dedos precisa estar funcionando de verdade antes
([B66](planos/BUGS.md#b66)).

<a id="p25"></a>
### P25 — Abrir a ilha de onde o punho está · dia · substitui

Os controles crescem debaixo do dedo que os chamou. Hoje, se ele largou o punho na borda
direita do Tab S8 Ultra — o app guarda a âncora e a respeita — tocá-lo faz a ilha aparecer no
canto de baixo à esquerda: uns 25 cm de viagem, no meio de uma partida, para alcançar o bloco
que ele acabou de pedir.

A ilha passa a receber a **mesma gravidade** que a âncora corrente do punho, pela tabela que
já existe. O cross-fade vira crescimento a partir do ponto do punho — nada de animar largura,
a regra do handoff continua valendo.

**Risco:** as duas âncoras do meio não dizem se a ilha vai para cima ou para baixo — precisa
de regra. E nascendo à direita, a borda esmaecida da rolagem tem de inverter de lado, senão o
corte volta a ler como componente quebrado.

<a id="p26"></a>
### P26 — Tirar a taxa de bits da folha · curto · tira

A folha se contradiz na primeira dobra: abre dizendo *"valem agora, sem cortar o vídeo"* e o
primeiro bloco diz *"vale para a próxima sessão"*. Ele mexe achando que a imagem vai melhorar
agora, e nada acontece.

Não se perde recurso nenhum: **não existe API de troca de taxa com a sessão de pé** — a taxa
entra na `StreamConfiguration` ao conectar. O bloco nunca teve como cumprir o que a folha
promete. Sai, e a folha fica só com o que muda na hora.

Se um dia a folha souber reconectar ([PE02](#pe02) dá o mecanismo), o bloco volta — aí sem a
desculpa embaixo.

<a id="p27"></a>
### P27 — Matar o menu de Ações · dia · tira

Some a terceira superfície de controle da sessão. No Vibeshine o menu de hoje tem cinco
linhas: Girar a tela, Enviar teclas ›, Sair da tela, Encerrar sessão, Cancelar — duas repetem
o que a ilha já faz, uma abre outra lista, e uma é só o botão de fechar. Ele para de ter que
lembrar em qual das três superfícies mora cada coisa.

A folha ganha uma seção "Nesta sessão" no topo. O bloco "Ações" sai da ilha: sobram dois
blocos e a saída, e a fileira encolhe. `Back` deixa de abrir menu e passa a abrir/recolher a
ilha, sem preferência.

**Risco:** `GameMenu` **não pode ser apagada** — `isOnExternalDisplay()` depende dela, e lá a
folha está na TV e não alcança. As linhas de área de transferência mudam de casa, não de
regra.

<a id="p28"></a>
### P28 — A régua no lugar da lista de teclas · dia · substitui

Mandar Alt+F4 hoje custa: achar o punho, abrir a ilha, tocar Ações, tocar Enviar teclas,
rolar 14 rótulos em inglês, tocar. Cinco gestos e uma leitura, com o jogo rodando atrás. A
folha já tem o interruptor "Teclas especiais" que põe Esc/Alt/Ctrl ao alcance — passa a haver
um caminho só, e ele é de um toque.

As combinações que só viviam na lista e valem a pena (Alt+F4, Ctrl+Alt+Del, Win, Win+D) vão
para a régua; as que sobrarem morrem com argumento escrito.

**Risco:** a lista também carrega combos importados de arquivo, herança do V+. Matar isso é
decisão de escopo, não de UI; se ficar, precisa de casa na régua. "Gerenciador de tarefas"
hoje usa `withGameFocus` — conferir que a régua dá o mesmo tratamento.

<a id="p29"></a>
### P29 — Cada escolha da folha diz o que faz · curto · substitui

Escolhe "Latência mínima" sabendo que ela troca suavidade por resposta, sem sair da sessão
para procurar nos Ajustes. Hoje são seis fichas nuas e nada na tela diz qual é a diferença. O
grupo de taxa de bits já tem uma linha de apoio — o padrão existe, só não foi estendido.

Uma linha por grupo, que descreve a consequência da opção **escolhida** e troca quando ele
toca noutra ficha. Não são três descrições empilhadas nem um ícone de interrogação por ficha.

**Risco:** a linha muda de altura conforme a frase e empurra o resto da folha no tablet, onde
ela é painel lateral. Reservar duas linhas fixas resolve.

<a id="p30"></a>
### P30 — Mostrar os gestos vivos · dia · substitui

Descobre em cinco segundos os gestos que hoje só existem para quem leu o código — e, mais
importante, vê qual deles **não funciona no modo em que ele está**, que é a resposta para
"por que três dedos não fizeram nada".

O único aviso do app é a torrada de `vidro_handle_hidden`, que só dispara **depois** de você
já ter achado o toque longo sozinho. Trocar por uma lista curta que lê o estado real: com a
capa acoplada, a linha de três dedos vira "Ctrl+Alt+Shift+T"; no modo em que o gesto não
chega, a linha aparece apagada com o motivo. É a tela que se explica porque está lendo a si
mesma, não um texto de ajuda escrito à mão.

**Risco:** depende de [B66](planos/BUGS.md#b66) para não listar gesto que não funciona. E a
versão "primeira sessão" tem de sumir sozinha e nunca mais voltar — se pedir um toque para
fechar, virou o tutorial chato que ele não quer.

---

## Fio 5 — O tablet e a capa

<a id="p31"></a>
### P31 — Terminar o tablet: catálogo e Ajustes · dia · acrescenta

**Verificado:** existem `layout-sw600dp-land/` para home e cenas. **Não existe nenhuma
variante de `activity_app_view.xml` nem de `activity_stream_settings.xml`.** As duas telas que
ele marcou como "toda zoada" são exatamente as duas que ficaram só em `layout/`.

No Tab deitado, o catálogo é o layout de celular esticado por 1722 dp: "Lugares" no pixel zero
e "Recarregar" no outro extremo — a volta e a ação a um metro uma da outra, com vazio no meio.
Ajustes tem a mesma doença: linhas de texto com largura de tela cheia.

Espelhar a decisão que `layout-sw600dp/activity_pc_view.xml` já documenta: coluna centrada de
720 dp, luz atravessando a largura inteira. A grade continua usando as colunas de
`layout-sw720dp/app_grid_view.xml`; o que muda é a moldura.

**Risco:** Ajustes é `PreferenceFragment` herdado — recuar a coluna sem quebrar o índice de
categorias exige mexer no contêiner, não em cada preferência.

<a id="p32"></a>
### P32 — A folha esconde o que a capa resolve · dia · tira

Com a capa acoplada, três dos quatro interruptores de "NA TELA" desenham teclado sobre o
vídeo para quem tem teclado de verdade no colo — e "Teclado completo" chega a tomar metade do
tablet. A folha pergunta ao aparelho antes de desenhar, como já faz para o modo do mouse no
Remote Input. Nenhuma opção nova: ela só para de oferecer o que a mão dele já tem.

**Risco:** esconder um interruptor **ligado** deixa a pessoa sem como desligar. Regra:
esconde só o que está desligado, e o que estiver ligado quando a capa acopla se desliga junto.

<a id="p33"></a>
### P33 — Esconder o punho quando a capa está acoplada · curto · tira

Com o teclado acoplado ele não toca na tela — e mesmo assim um círculo aceso fica parado
sobre o jogo o tempo todo. Com a capa, o vídeo fica limpo de ponta a ponta e o controle vem
pelo teclado, que é onde as mãos já estão.

**Risco:** sem caminho de volta a sessão fica sem controle. O acorde tem de existir **antes**
de a pílula sumir, e o toque na tela precisa continuar sendo saída de emergência. A detecção
por `Configuration` erra em alguns aparelhos — **medir no S8 Ultra acoplado e destacado antes
de confiar**.

---

## Fio 6 — Movimento e resposta

<a id="p34"></a>
### P34 — A mola do toque onde a pessoa toca · curto · acrescenta

**Verificado:** `app_grid_item.xml` (o card de app) e `pc_grid_item.xml` (a linha de host) não
têm `stateListAnimator`. `@animator/vidro_press` está aplicado em **11 superfícies
secundárias** e em **nenhuma das duas primárias** — e `styles.xml` ainda o zera com `@null` em
três estilos de botão.

Você toca num jogo e não acontece nada até a tela inteira trocar. O handoff especifica "mola
do toque · 120 ms · scale 0.97 e volta", e os valores já estão em `motion.xml`. O `@null`
estava lá só para matar a elevação do Material e ninguém pôs nada no lugar.

Aplicar no card, na linha de host, nos ladrilhos da ilha e nos chips.

**Risco:** na sessão, animar durante o stream é proibido pelo handoff — os ladrilhos da ilha
só podem molar no toque, nunca em laço.

---

## Fio 7 — Poder novo

<a id="p35"></a>
### P35 — A tela do PC nasce do tamanho da sua · dia · substitui

Nunca mais escolher resolução. O mesmo app nos dois aparelhos pede, cada um, exatamente a
tela que tem — o Tab S8 Ultra deitado pede 16:10 na taxa do painel, o S22 Ultra pede a dele —
e o Vibeshine, que cria display virtual por cliente, entrega. Some a imagem esticada, some a
tarja preta, some o aviso de resolução nativa, e some a viagem a Ajustes toda vez que se troca
de aparelho.

A medida já é feita: `addNativeResolutionEntries` usa `getRealMetrics` e `getSupportedModes`.
Uma chave só, ligada de fábrica: "A tela do PC acompanha este aparelho". **Oito preferências
saem do caminho diário** e viram um "Escolher eu mesmo" atrás do índice de categorias.

**Risco:** aparelho de proporção estranha vira desktop que o PC nunca viu — o S22 Ultra é
19,5:9, e uma área de trabalho nessa forma é desconfortável mesmo funcionando. Prever teto e
forma mínima razoável. E medir na **tela cheia**, não no estado empurrado pela barra de
composição.

<a id="p36"></a>
### P36 — Levar as cenas de um aparelho para o outro · dia · acrescenta

Ajustar uma vez e usar nos dois. Hoje tudo que se descobre no Tab tem de ser redescoberto e
refeito à mão no S22 Ultra.

A cena viaja como **intenção**, não como retrato de preferências: papel, ritmo, entrada e
camadas na tela — nunca pixels nem taxa de painel, que são do aparelho. É diferente de
exportar a configuração inteira (M63): exportar valores brutos entre telas diferentes leva o
problema junto.

**Risco:** só faz sentido **depois** de [P35](#p35). Enquanto resolução e taxa forem escolha
manual guardada na cena, a cena não é portátil por definição — fora de ordem, isto vira uma
superfície de compartilhamento a mais sem ganho, que é exatamente a gordura a evitar.

<a id="p37"></a>
### P37 — O que ele faz sempre, na tela de bloqueio · curto · acrescenta

Do aparelho bloqueado ao vídeo em um toque, sem abrir o app, sem esperar a busca na rede. A
base está pronta em `ShortcutHelper` — mas só como atalho **fixado à mão** pelo menu de
contexto do catálogo. Manter um atalho dinâmico que segue a última sessão bem-sucedida,
disparando o `Intent` que `ServerHelper.createStartIntent` já monta, inclusive o certificado —
então não passa pela descoberta. **Zero interface nova dentro do app.**

**Risco:** atalho para host desligado tem de cair com elegância — o app já sabe desabilitar
atalho com motivo, e esse caminho precisa levar a [P06](#p06), não a uma mensagem de erro. E o
mesmo histórico alimenta [P10](#p10): duas fontes de "o que você faz sempre" discordando seria
pior que nenhuma.

---

## O que não entrou, e por quê

- **Qualquer coisa de otimização interna.** Foi a instrução. Alocação, thread, cache, custo
  por quadro, cobertura de teste e dívida técnica seguem no [CATALOGO.md](CATALOGO.md).
- **Features que exigem Apollo.** Nenhuma das 45 exige. As que tocam área de transferência,
  comandos de servidor ou display virtual entram só pelo lado de **esconder** o que o
  Vibeshine não faz ([P09](#p09), [P14](#p14), [PE03](#pe03)).
- **Tutorial, carrossel de boas-vindas, tour.** A explicação aparece onde a dúvida nasce
  ([P05](#p05), [P29](#p29), [P30](#p30)) ou não aparece.
- **Preferência nova.** Nenhum item deste catálogo acrescenta uma. Vários apagam.
- **Redesenho da identidade.** O protótipo está certo; onde a implementação divergir, o
  protótipo ganha. Este catálogo cobra o que foi desenhado e não foi entregue — não propõe
  desenho novo.
