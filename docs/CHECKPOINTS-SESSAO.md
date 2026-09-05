# Os próximos cinco checkpoints

> Sequência de trabalho da sessão, saída do time de UX de 2026-08-25 (quatro lentes:
> teclado, geometria, controles, invisível) mais o que sobrou dos checkpoints 1–3.
> **Cada achado abaixo foi ancorado em arquivo e linha por quem o encontrou.**

Os três primeiros checkpoints já foram entregues e estão registrados no fim deste arquivo.
A ordem daqui em diante não é por tamanho — é por **dependência**: cada um destrava o
seguinte, e furar a fila cria armadilha.

---

## C4 — O teclado que se comporta

O tema: parar de perder tecla, e parar de exigir que ele descubra coisas.

| # | O quê | Por quê | Tam |
|---|---|---|---|
| 4.1 | **Repetição de tecla ao segurar** | Segurar Backspace apaga um caractere; segurar a seta anda uma posição. Num desktop remoto isso é o dia inteiro. `Game.java` come os repeat downs de propósito — o padrão do Moonlight, que conta com o auto-repeat do host. **Medir primeiro** se o Vibeshine repete agora que os modificadores foram consertados; só reenviar o repeat se ele não repetir, senão vira repetição dobrada | P |
| 4.2 | **Teto no estado de acorde** | Se um KEY_UP se perder, o acorde engole tudo. O reset único já entrou no C1; falta o teto de tempo — passados N segundos sem os modificadores subirem, desistir | P |
| 4.3 | **Acordes para a folha e para o menu** | Com a capa acoplada, trocar o modo do mouse ou abrir a folha exige tirar a mão do teclado e mirar um punho que pode estar no canto oposto de 1722 dp. O mecanismo já reserva a família Ctrl+Alt+Shift e consome até os modificadores subirem: cada acorde novo é uma linha em dois `switch` | M |
| 4.4 | **`Ctrl+Alt+Shift+Q` deixa de ser do cliente** | Ele encerra a sessão na hora, sem confirmação — e uma string do próprio app anuncia esse acorde como *ação do PC*. Sai da lista consumida e atravessa para o host, como está escrito | P |
| 4.5 | **`Ctrl+Alt+Shift+Z` ganha estado visível** | Ele desliga TODA a entrada e nada na tela muda. Quem não sabe do acorde conclui "o teclado bugou" e derruba a sessão. É uma das causas do "às vezes funciona, às vezes não" | P |
| 4.6 | **Os gestos vivos** (P30) | Quatro gestos de vários dedos que só o código conhece, e a lista lê o estado real: com a capa acoplada a linha de três dedos vira o acorde; no modo em que o gesto não chega, aparece apagada com o motivo | M |

**Armadilha:** 4.3 precisa vir antes de qualquer coisa que esconda o punho por padrão. Esconder o punho com a capa acoplada tira o acesso a **tudo** que a ilha oferece, não só ao punho.

---

## C5 — A sessão que sobrevive

O tema: sair do app deixar de ser o fim.

| # | O quê | Por quê | Tam |
|---|---|---|---|
| 5.1 | **PiP ligado de fábrica, com o texto honesto** | `onStop()` derruba a conexão e chama `finish()`. O que evita isso é o PiP, desligado de fábrica, cuja preferência se chama "Assiste sem controlar enquanto usa outros apps" — descreve observação, não sobrevivência. Nada ali diz *"isto é o que impede a sessão de morrer quando você sai do app"* | P |
| 5.2 | **Janela de carência no `onStop`** | O vídeo escurece, a conexão fica de pé alguns segundos, e voltar reaproveita a sessão em vez de reconectar. Atender uma ligação deixa de custar catálogo → app → conectar | G |
| 5.3 | **Reconexão automática** ([PE01](CATALOGO-PRODUTO.md#pe01)) | A tela "Perdemos o PC" está desenhada, incluída em toda sessão, e nasce `gone` para sempre | G |
| 5.4 | **Trocar de app sem sair** ([PE02](CATALOGO-PRODUTO.md#pe02)) | Sair do Desktop e entrar no jogo custa quatro telas | G |

**Ordem obrigatória:** 5.1 **depois** do PiP estar bom — que o C1 começou mas não terminou. Ligado por padrão com defeito, o primeiro contato dele com o recurso é o estado quebrado.

---

## C6 — A folha que não mente

O tema: todo controle da sessão faz o que diz, e lembra.

| # | O quê | Por quê | Tam |
|---|---|---|---|
| 6.1 | **Os chips de ritmo persistem ou saem** | `bindPacing` só escreve no objeto em memória — nada é gravado, e a rota do decodificador foi escolhida quando a thread nasceu. Ele toca, o chip acende, o vídeo não muda, e na sessão seguinte está tudo como estava. Ou persistem e dizem "vale na próxima conexão", ou vão para Ajustes junto com a taxa de bits que já saiu ([P26](CATALOGO-PRODUTO.md#p26)) | M |
| 6.2 | **A folha esconde o que a capa resolve** ([P32](CATALOGO-PRODUTO.md#p32)) | Três interruptores desenham teclado sobre o vídeo para quem tem teclado de verdade no colo | M |
| 6.3 | **O teclado completo que se tranca** | Ele toma metade da tela e cobre a própria folha — a única superfície onde há como desligá-lo. Precisa sair na mesma rodada que 6.2, ou a regra "esconde só o que está desligado" não protege ninguém | P |
| 6.4 | **Matar o menu de Ações** ([P27](CATALOGO-PRODUTO.md#p27)) | Cinco linhas: duas repetem a ilha, uma abre outra lista, uma é o botão de fechar | M |
| 6.5 | **A régua no lugar da lista de teclas** ([P28](CATALOGO-PRODUTO.md#p28)) | Mandar Alt+F4 custa cinco gestos e uma leitura de catorze rótulos em inglês | M |
| 6.6 | **Aceleração no trackpad da capa** | O delta vai direto para o host sem nenhum fator: uma varrida atravessa a tela e não há aceleração. As duas seekbars de sensibilidade que ficam a 40 linhas do interruptor do pad nativo **não o alcançam** — só chegam ao dedo na tela. E a geometria do pad é resolvida uma vez e nunca invalidada: girar o tablet deixa a escala presa | M |

---

## C7 — Movimento por toda parte

O tema: o app inteiro vivo, sem exagero. O C3 começou pelo punho e pela ilha.

| # | O quê | Tam |
|---|---|---|
| 7.1 | **A capa tocada cresce até virar a tela** ([PE08](CATALOGO-PRODUTO.md#pe08)) — o movimento assinatura da identidade, que o app não faz | G |
| 7.2 | **A ilha abre de onde o punho está** ([P25](CATALOGO-PRODUTO.md#p25)) — hoje ela nasce sempre no canto de baixo à esquerda, a 25 cm do dedo que a chamou | M |
| 7.3 | **Três alturas de rede, e a luz que nunca acende** ([P13](CATALOGO-PRODUTO.md#p13)) — `VidroLightView.setQuality()` não tem um único chamador no app inteiro | M |
| 7.4 | **A barra de conectar chega a 100%** ([P12](CATALOGO-PRODUTO.md#p12)) | curto |
| 7.5 | **Transições entre as telas da casca** — hoje cada troca é a animação de fábrica do Android | M |
| 7.6 | **Feedback de envio na barra de composição** — enquanto o texto drena, "Enviar" fica apagado; devia virar "Parar", que é a única ação que faz sentido no momento | P |

---

## C8 — A tesoura, e o que fica no lugar

O tema: as 126 preferências. Deixado por último de propósito — cortar depois que as telas estabilizarem custa menos que cortar antes.

| # | O quê | Tam |
|---|---|---|
| 8.1 | **Apagar o que este host já disse que não faz** ([P14](CATALOGO-PRODUTO.md#p14)) — inclusive a preferência de área de transferência, que agora não faz nada em Vibeshine | M |
| 8.2 | **Uma sensibilidade, não seis** ([P18](CATALOGO-PRODUTO.md#p18)) — depende de 6.6 | M |
| 8.3 | **Os Números como uma linha** ([P19](CATALOGO-PRODUTO.md#p19)) — hoje derramam oito linhas de inglês cru sobre o vídeo | M |
| 8.4 | **A depuração vira folha de leitura** ([P21](CATALOGO-PRODUTO.md#p21)) | M |
| 8.5 | **Aposentar o teclado que o app desenha** ([PE07](CATALOGO-PRODUTO.md#pe07)) — 13 preferências, 899 linhas de layout, 4.236 de código. **Só depois do E01 validado nos dois aparelhos** | G |
| 8.6 | **A cena de quatro escolhas** ([PE06](CATALOGO-PRODUTO.md#pe06)) — hoje "salvar a sessão como cena" abre as 126 preferências | G |

---

## O que já foi entregue

### C1 — Os estados presos
Backspace (a causa era `releaseHeldModifiers` não zerar `modifierFlags` — regressão introduzida ao consertar teclas presas no host); reset único do estado de teclado; PiP fecha **toda** camada de controle e guarda/devolve o zoom; toque na imagem fecha a ilha; punho com `Ctrl+Alt+Shift+H`, memória entre sessões, e resgate que não apaga a escolha; ponto de qualidade sobrevive a "desabilitar avisos". Preso em `ModificadoresPresosTest`.

### C2 — Geometria e digitação
Proporção informada **sempre** — o atalho `|| aspectRatioMatch` deixava o container sem proporção, e o vídeo esticava quando o IME encolhia a caixa; padrão passou de `fit` para `fill`; aproximação de 1,6× no último toque ao abrir a barra de composição; o ouvinte de insets do IME passou a ser instalado sempre (escolher "cobrir" matava a barra de composição em silêncio); fechar a barra cancela o envio pela metade.

### C3 — Movimento e verdade
Punho gruda com passada (`vidro_snap`, 320 ms); a ilha rearma os 6,2 s quando ele mexe nela; os Números dizem "sem vídeo há Ns" em vez de congelar na última boa notícia; a área de transferência pergunta ao host antes de agir — no Vibeshine ela escrevia a página de erro do Sunshine no clipboard do tablet; o interruptor dos Números persiste.

---

## Duas coisas que precisam de aparelho antes de virar tarefa

1. **A repetição de tecla (4.1).** Com os modificadores consertados, é possível que o host já repita e não haja nada a fazer. Reenviar repeats às cegas produz repetição dobrada.
2. **A captura de ponteiro no S8 Ultra.** `hasCaptureCompatibleInputDevice()` descarta todo dispositivo que anuncia `SOURCE_TOUCHSCREEN`, e a capa anuncia as duas fontes no mesmo dispositivo — provavelmente a captura nativa nunca é pedida nesse aparelho. Isso muda o que `Ctrl+Alt+Shift+Z` deveria significar lá.
