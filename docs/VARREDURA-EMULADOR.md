# Varredura de produto — 26/08, emulador em navegação por gestos

O dono do fork instalou o APK no S22 Ultra e o veredito foi: *"tem muita coisa quebrada… o
menu flutuante virou um gesto de puxar na tela e ficou muito ruim, porque nisso eu puxo as
notificações e a bottom bar do celular sem querer e fica saindo do app"*.

Ele estava certo, e a causa de quase tudo é uma só: **o app estava disputando as bordas da
tela com o Android**, e perdendo.

## O que faltava na verificação, e por isso nada disso aparecia

Duas coisas, e as duas foram consertadas antes de procurar defeito:

1. **O emulador estava em navegação por três botões.** O aparelho do dono usa **gestos**, onde
   as bordas laterais são o Voltar e a borda de baixo é o home. Nenhum conflito de gesto podia
   aparecer no emulador configurado daquele jeito.

   ```bash
   adb shell cmd overlay enable com.android.internal.systemui.navbar.gestural
   ```

2. **Não havia como medir alvo de toque.** Olhar captura de tela não diz que um botão tem
   38 dp. `scratchpad/auditor.py` lê a árvore do `uiautomator`, converte para dp e reclama do
   que está abaixo dos 48 dp mínimos, do que não tem rótulo e do que se sobrepõe.

   O auditor **errou duas vezes** antes de servir, e as duas isenções que ele ganhou estão
   escritas nele: alvo pequeno dentro de um alvo grande não é problema (o interruptor mora
   numa linha de 320 dp), e caixa cortada pela borda da tela é conteúdo abaixo da dobra, não
   alvo pequeno. Uma terceira correção veio depois de eu perseguir três consertos de semântica
   que não eram necessários — um clicável sem texto próprio **não** é um clicável sem rótulo
   quando ele tem um filho com texto, que é como todo botão com `TextView` dentro funciona.

## Os defeitos, e o que cada um era de verdade

### 1. O gesto da gaveta era o Voltar do Android

Três erros somados no mesmo lugar:

| | |
|---|---|
| O puxador tinha **3 dp de largura** a 22% de opacidade | invisível, e alvo de 3 dp não existe — o mínimo é 48 |
| A banda de arrasto era a **borda direita inteira** (24 dp de largura, altura toda) | é exatamente onde o Android põe o Voltar por gesto |
| `setSystemGestureExclusionRects` pedia a **altura inteira** | o sistema honra no máximo 200 dp por borda; pedindo ~800 dp, quase nada era de fato excluído |

E o arrasto **vertical** na mesma banda — que servia para sair da sessão — era o que puxava a
aba de notificações.

**A correção é uma reversão, e ela custa a promessa de "zero pixel de interface em repouso":**
o gesto agora só começa **dentro do puxador**, que é uma View de 48×96 dp crescendo para
dentro da tela. O resto da tela voltou a ser do jogo e do Android, inteiro. A exclusão de
gesto pede só o retângulo do puxador, que cabe folgado nos 200 dp e por isso funciona.

Medido depois: arrasto na borda direita a 400 px do puxador não abre mais nada, e o app
continua em foco.

### 2. Todo rodapé da casca ficava embaixo da barra de gestos

```
Cenas · Ajustes    y = 2270 … 2396
barra do sistema   y = 2337 … 2400
```

Os últimos 59 px do alvo estavam debaixo da barra de home. **As sete telas da casca tinham o
mesmo erro**, pela mesma linha copiada: elas acolchoavam `WindowInsets.statusBars` — só a
barra de cima. Agora é `systemBars`, que é as duas.

### 3. Alvos abaixo do mínimo

| onde | media | agora |
|---|---|---|
| chips de RITMO e MODO DO MOUSE, na gaveta | 101×**44,2** dp | 101×48 |
| "Adicionar", na home | 48×**41,5** dp | 48×48 |
| "Voltar", em Cenas | 48×**38,5** dp | 48×48 |
| "Recarregar", no catálogo | 57×**45,7** dp | 57×48 |

O chip é o caso mais feio dos quatro: o equivalente em Compose
(`TelaEditorDeCena.Chip`) já usava `@dimen/vidro_touch_target`, então as duas metades do app
discordavam sobre a mesma peça.

### 4. Coisas sem nome

- **O catálogo inteiro era uma parede de botões anônimos.** Cada célula sai como um `View` de
  182×242 dp sem texto e sem descrição, e sem filho de onde tirar um: o nome do jogo está
  **desenhado dentro da imagem** que o host gera. Agora a célula leva o nome do próprio item
  do adaptador.
- **O véu de dispensa da folha** era um clicável de tela cheia sem rótulo. Quem usa leitor de
  tela achava um botão anônimo cobrindo tudo, sem saber que ele é a saída.

## O que continua faltando, dito em vez de escondido

- **O estado da máquina não é legível por leitor de tela.** Acordada, dormindo e fora do ar
  são desenhados como ponto de luz num `Canvas`, e ponto não tem texto. O nome sai; o estado
  não. Consertar é dar rótulo ao ponto, e é tarefa própria.
- **A varredura mede alvo, rótulo e sobreposição — não julga desenho.** Ela diz que um botão
  tem 48 dp e que ele leva a algum lugar; não diz se o lugar é o certo, nem se a tela está
  bonita. Isso continua sendo captura de tela e olho.
- **O moque não prova latência, decodificação, HDR nem nada que passe pelo `MoonBridge`.** O
  vídeo dele é `lockCanvas` a 30 fps.

## Como repetir

```bash
adb shell cmd overlay enable com.android.internal.systemui.navbar.gestural
python scratchpad/percurso.py     # varre a casca inteira, tela por tela
python scratchpad/auditor.py "nome da tela"   # mede a tela que estiver aberta
```

O percurso terminou com **0 problemas** em: home, ajustes e as oito categorias dele, cenas,
endereço, folha do host, catálogo. A sessão e a gaveta também.

---

# O menu invisível — segunda gravação, 27/08

O dono instalou o APK corrigido e o veredito foi *"tem bastante bug ainda e muita coisa
quebrada, principalmente o menu invisível kk"*. Gravação de 107 s, 2316×1080, **paisagem**, em
sessão real.

## O que a medição mostrou

Extraí 212 quadros a 2/s e medi, por quadro, as faixas verticais quase totalmente pretas
(`scratchpad/faixas_pretas.py`). O padrão apareceu sozinho:

```
    5.1s  1129..1807 (678 px)
    7.1s  1129..2293 (1164 px)
    9.1s  1129..1807 (678 px)
   13.1s  1247..1807 (560 px)
```

Uma faixa preta que começa onde o vídeo termina. Ampliando a região 4×: **nada desenhado**.
Nem fundo, nem texto, nem chip. Só a barra de navegação do sistema e uma barra vertical clara
na borda — que é o próprio puxador.

E aos **52,6 s o painel aparece inteiro**. Então ele não é sempre invisível: ele some durante o
**arrasto** e volta quando assenta.

## A causa

A ordem dos filhos em `activity_game.xml`:

```
GavetaDaSessao
├── vidro_session_sheet    ← filho 0, ATRÁS da SurfaceView
├── StreamContainer        ← filho 1, a SurfaceView
└── vidroPuxadorDaGaveta   ← filho 2, POR CIMA
```

Uma `SurfaceView` de z-order padrão abre um **furo transparente** na janela sobre os próprios
limites e apaga o que estiver desenhado atrás dela ali. Quando o container é transladado, o
furo deveria acompanhar. No emulador acompanha; no compositor do aparelho do dono, não durante
o gesto.

O puxador sobrevivia porque já era desenhado por cima. O painel não, porque estava atrás. É
exatamente o que a gravação mostra: **puxador visível, painel apagado**.

A ideia original — *"o controle não fica sobre o vídeo, ele fica atrás dele, e o vídeo anda
para o lado para descobri-lo"* — depende de o furo acompanhar uma translação em todo
compositor. Não depende mais.

## As três correções

1. **O painel passou para o topo da pilha** e desliza sozinho: repousa fora da tela, à direita
   da própria largura, e entra à medida que o vídeo sai. O movimento na tela é o mesmo; o que
   mudou é que agora ele não pode ser apagado por ninguém.

2. **O vídeo desliza só o necessário.** Ele é 16:9 e a tela em paisagem não é, então o
   `StreamContainer` já deixa uma tarja preta à direita da imagem. Deslizar pela largura
   inteira do painel arrastava essa tarja para dentro da tela — medido em 2316×1080: **236 px
   de costura preta** entre a imagem e o painel. Descontando a tarja, a imagem encosta no
   painel e ainda sobra mais vídeo visível do que antes.

3. **O puxador some com a gaveta aberta.** Medido com uiautomator em paisagem: o alvo dele
   cruzava as caixas de "Least delay" e "Trackpad", os chips da coluna direita. Alguns pixels
   do chip abriam a gaveta já aberta em vez de trocar o modo.

## Como reproduzir a geometria do aparelho no emulador

O bug só existe em paisagem, e o `Game` fixa a própria orientação — girar o emulador não
bastava. O caminho é mudar a tela:

```bash
adb shell wm size 2316x1080
adb shell wm density 420
python scratchpad/repro_land.py          # abre o moque e ARRASTA o puxador
python scratchpad/meio_arrasto.py        # captura NO MEIO do arrasto
adb shell wm size reset; adb shell wm density reset
```

**O erro que me custou a primeira rodada:** no emulador eu só tinha **tocado** o puxador, nunca
arrastado, e só em retrato. O toque abre a gaveta de uma vez e o furo tem tempo de reassentar;
o arrasto é onde ela some.

## O que continua não verificado

- **Só o moque foi usado.** Ele prova camada, gesto, geometria e alvo. Não prova latência,
  decodificação, HDR nem nada que passe pelo `MoonBridge`.
- **O compositor do aparelho do dono não está no emulador.** A correção remove a *dependência*
  do comportamento que falhou — o painel deixou de estar atrás da superfície — em vez de
  ajustá-lo. É por isso que ela deve valer lá; mas quem confirma é o aparelho.

---

# A janela própria — terceira rodada, 27/08

A mudança da folha para a raiz do layout **não resolveu** no aparelho do dono. O relato dele
trouxe a pista decisiva: *"só aparece se eu deixo aberto e coloco em picture in picture — por
meio segundo ele aparece na miniatura do vídeo"*.

Isso prova que o painel **está desenhado na janela o tempo todo**: o PiP é o momento em que o
sistema rearranja a camada da superfície de vídeo, e as views por baixo aparecem. Alguma coisa
naquele compositor compõe a superfície POR CIMA do conteúdo da janela — e não é este código:
as duas únicas chamadas de z-order do app desligam o on-top.

Contra isso não há ordem de filho que resolva. A resposta é estrutural:

## A folha mora numa janela própria

`Game.mostrarJanelaDaFolha()` transplanta a folha, na primeira abertura, para um
`PopupWindow` não-focável ancorado à direita. **Janelas compõem sobre janelas** — o jogo de
z-order da SurfaceView é interno à janela dela e não alcança outra. O recorte na borda da
janela produz o mesmo desenho de sempre: o painel entrando pela direita enquanto o vídeo
desliza.

A janela nasce na primeira abertura e a folha continua no `activity_game.xml` até lá — é o
que mantém todos os `findViewById` do `onCreate` funcionando.

## Os três defeitos que a mudança expôs, todos medidos no emulador

1. **Chips gravavam a preferência e não acendiam.** O clique funciona pela referência
   guardada no listener, mas todo repintar (`select`, `check`, `refreshSheet`) procurava por
   `activity.findViewById` — e a folha já não está na hierarquia da Activity: null, e o
   método pulava em silêncio. `VidroSession.achar()` procura primeiro na folha.
2. **Tocar no vídeo com a gaveta aberta não fechava nada.** O doc da GavetaDaSessao prometia
   isso desde a primeira versão; a implementação só tratava arrasto — o toque parado
   atravessava para o jogo, virando clique dentro da stream com o painel aberto do lado.
   Agora, com a gaveta aberta, o DOWN no vídeo é consumido pela gaveta: toque curto fecha,
   dedo que anda vira arrasto.
3. **O aviso de abertura não cobria o arrasto.** `refreshSheet` morava só no toque do
   puxador; o funil virou a própria gaveta (`aoComecarAAbrir`), que também mostra a janela.
   E `aoAssentarFechada` — que dispara em TODO assentamento em zero, não só quando estava
   "aberta" — é o que dispensa a janela, cobrindo o caso do arrasto que desiste no meio.

## Verificado no emulador (x86_64, gestos, 1080×2400 e 2316×1080)

- abrir por arrasto e por toque no puxador — painel visível durante o gesto inteiro
- chips de pacing e de mouse trocam e acendem; interruptores viram
- toque no vídeo fecha; Voltar fecha; "Close" fecha
- gate completo verde, 242+ testes

## O que segue sem prova

O compositor do S22 Ultra não existe no emulador. Desta vez a correção não depende de
comportamento dele — janela sobre janela é contrato da plataforma, não detalhe de
implementação — mas quem confirma é o aparelho.

## A matriz das ações da sessão — 27/08, emulador, depois da janela própria

Cada linha da gaveta exercitada de ponta a ponta, com captura:

| ação | resultado |
|---|---|
| abrir por toque no puxador | painel entra, chips sincronizados |
| abrir por arrasto | painel acompanha o dedo do primeiro pixel |
| chips de pacing e de mouse | trocam e acendem na hora |
| interruptores (Pan and zoom etc.) | viram na hora |
| **Keyboard** | barra de composição sobe com o IME, e **o vídeo sobe junto em vez de ser coberto** — o comportamento AnyDesk do E01, funcionando |
| Rotate Screen | gira a sessão |
| Send special keys | lista temática abre (a exceção documentada: 18 combinações não cabem na gaveta) |
| Choose something else | casca por cima, vídeo vivo atrás, faixa quente no topo |
| toque na faixa | de volta à MESMA sessão, sem reconectar |
| gesto de sair (arrasto vertical no puxador) | casca com faixa |
| toque no vídeo com gaveta aberta | fecha, e o jogo não vê o toque |
| Voltar / Close | fecham |
| PiP | dispensa a janela da folha |

Um falso alarme registrado: "Keyboard não abre o IME" no primeiro teste era estado sujo do
próprio roteiro (a casca estava aberta por cima); com o app em estado limpo, funciona.

---

# As caminhadas funcionais — 29/08

Com a matriz de superfícies verde, a verificação mudou de natureza: em vez de medir telas,
**percorrer fluxos inteiros e conferir o efeito de cada toque**. Seis defeitos reais caíram,
nenhum deles visível em captura estática:

| fluxo | defeito | conserto |
|---|---|---|
| Cenas | "Apagar cena" destruía NO TOQUE, sem confirmação — a única destrutiva do app a um toque | pergunta antes, com o Delete no funil âmbar |
| Barra de texto | com a conexão caída, "enviado ao PC" + campo limpo — texto ia para o nada | sem conexão a torrada fala a verdade e o texto fica |
| Barra de texto | fechar destruía o rascunho (e um comentário jurava o contrário) | o rascunho sobrevive a qualquer fechamento; enviar limpa |
| Sessão | "Girar a tela" era no-op: a política FULL_USER desfazia o pedido no quadro seguinte | pedido explícito vence a política até o próximo toque |
| Diálogos | "OK"/"Sim" saíam apagados ou lilás — backgroundTint do Material por cima do tema | funil UiHelper.vestirBotoesDoDialogo nos quatro sítios |
| Diálogos | três diálogos crus restantes (OTP, renomear, apagar) | mesmos papéis pelo funil |

Fluxos percorridos e confirmados limpos: Cenas completo (criar→salvar→cancelar→apagar),
endereço com IP inalcançável, Try again da falha, teclas especiais com link morto (sem
crash), persistência dos interruptores (reabrir e relançar), rascunho da barra, rotação em
runtime + gaveta, gesto de sair nas duas orientações.

## O pacote de diagnóstico do menu

`scratchpad/diagnostico-menu.bat` (na área de trabalho da sessão do Claude) colhe, num
comando, o que decide a próxima hipótese se o menu seguir invisível em janela de diálogo:
`dumpsys window windows` (o que o Android acha que está na tela), `dumpsys SurfaceFlinger`
(o que o compositor está de fato compondo) e uma captura. Rodar com o celular no USB, dentro
da stream, logo após tocar o puxador.
