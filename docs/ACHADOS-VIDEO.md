# Achados do vídeo de 26/08 — 3 min no S22 Ultra

Análise quadro a quadro de `WhatsApp Video 2026-08-26 at 00.42.40.mp4` (184 s, 396×850,
7165 quadros). Amostrados 184 quadros a 1 fps, montados em folhas de contato, e os momentos
críticos relidos em resolução cheia.

**Método, e o que ele custou:** os pontos cinza que aparecem espalhados pelas telas são o
**indicador de toque da gravação**, não elementos de interface. Cheguei a marcar "interruptor
sem trilho" como defeito com base nas folhas de contato e desmarquei ao abrir o quadro
inteiro — o interruptor está correto. Fica registrado porque é o erro que essa análise
convida.

---

## A. Quebras de layout

| | onde | o que está errado |
|---|---|---|
| **A1** | editor de cena, 11 s | **Quatro cabeçalhos empilhados**: a Toolbar do XML (←, lápis, ✓), o título "Novo perfil", o "Voltar" do Compose e o título "Nova cena". Dois cabeçalhos de duas eras na mesma tela. |
| **A2** | catálogo, 34 s | **Células de alturas diferentes** na mesma linha: DESKTOP mede ~120 px e REMOTE INPUT ~220 px. E a arte do DESKTOP não preenche o cartão — sobra fundo preto dos dois lados. |
| ~~A3~~ | Ajustes, 24 s | **Não era defeito.** O fragmento "de mouse" é um item cortado no meio pela rolagem normal da lista, não conteúdo escapando do recorte do cabeçalho. Confirmado ao reler o quadro inteiro. Fica riscado em vez de apagado: um mapa que some com os próprios enganos deixa de ser verificável. |
| **A4** | home | Uma entrada do histórico se chama **"app"** — o nome-marcador de `ServerHelper.doStart` vazando para a tela. |
| **A5** | editor de cena | Chips de alturas diferentes na mesma fileira: "Controle na tela" quebra em duas linhas e cresce; os vizinhos não. |

## B. Identidade e cor

| | onde | o que está errado |
|---|---|---|
| **B1** | sair da sessão, 75 s | O botão "Sim" é **lilás do Material** (`#D0BCFF`). É um `AlertDialog` construído sem o tema `VidroDialog`. |
| **B2** | menu do host, 48 s | O menu de contexto é um **`AlertDialog` cinza-claro do sistema**, empilhado POR CIMA da folha Vidro. Duas linguagens modais na tela ao mesmo tempo. |
| **B3** | "Ver Detalhes", 56 s | Um **despejo de depuração** mostrado ao usuário: `Active Address: null`, `MAC Address: null`, `Pair State: null`, `Running Game ID: 0`. Com botões "Ajuda" e "OK" do sistema. |
| **B4** | catálogo, endereço, cenas, editor | **Azul continua sendo "clicável"** em "Recarregar", "Conectar", "Salvar", "Salvar a sessão atual como cena", "Volte de onde parou" — enquanto na lista de lugares o mesmo azul quer dizer "dormindo". A colisão foi corrigida só na home. |
| **B5** | conectando, 79 s | A **torrada cai exatamente em cima do texto do passo**: "Desktop encerrado" sobre "Reservando a tela no PC". |
| **B6** | folha do host, 46 s | Uma **explicação** ("Sem endereço guardado para acordar daqui") é pintada no vermelho de alerta, ao lado de uma ação destrutiva de verdade ("Remover este lugar"). Dois vermelhos, dois significados. |

### A regra de cor, decidida por causa de B4

As três temperaturas passaram a significar coisas diferentes, e nenhuma delas é "clicável":

- **Quente** (`vidro_quente`) — o que está **em vigor**: a máquina acordada, a cena ativa, a
  sessão rodando atrás da casca.
- **Azul** (`vidro_accent_text`) — a opção **escolhida nesta superfície**: o chip aceso, o
  modo de mouse selecionado.
- **Cinza** (`vidro_text_2`) — ação. Toda ação em linha, inclusive as que antes eram azuis
  por serem tocáveis: "Fechar", "Recarregar", "Conectar", "Salvar".

O funil de B4 é `VidroText.Action` em `res/values/styles.xml` — trocar a cor lá troca em todas
as ações em XML de uma vez. Vermelho (`vidro_alerta`) ficou reservado para o que **destrói**,
e só: a folha da sessão perdeu o vermelho que tinha porque nada nela destrói coisa alguma.

## C. Sessão — a queixa principal

| | o que está errado |
|---|---|
| **C1** | **Não há controle nenhum sobre o vídeo.** O punho e a ilha foram removidos e a gaveta ficou sem afordância: ela abre por um arrasto na borda direita que nada anuncia. Na prática o dono ficou sem acesso. |
| **C2** | O único menu alcançável é o **`GameMenu` legado** ("Ações desta sessão") — um cartão de lista, não a gaveta. |
| **C3** | "Enviar teclas especiais" é outro **diálogo de lista cru**. |

## D. Transições

| | o que está errado |
|---|---|
| **D1** | O esmaecimento entre home e catálogo mostra **as duas telas inteiras ao mesmo tempo**, por vários quadros. O nome do host aparece por cima do subtítulo do catálogo ("ARTEMI" sobre "…cados pelo host"). |
| **D2** | O nome que viaja aparece **recortado** no meio do caminho ("ARTF"). |

## E. A capa da espera

| | o que está errado |
|---|---|
| **E1** | A "capa" é o **ladrilho gerado pelo Moonlight, com o nome do app desenhado dentro da imagem**. Ampliada em `centerCrop` para tela cheia, ela corta o próprio nome: "REMOTE MONITO". |

---

## F. O que só apareceu depois, no emulador

O vídeo não podia mostrar estes: eles estão do outro lado das correções de C, e só ficaram
alcançáveis quando a sessão passou a ser dirigível sem host — `MoqueDaStream` desenhando um
vídeo falso na `Surface` e `Game` exportada em `src/debug/AndroidManifest.xml`.

| | o que estava errado | como se sabe |
|---|---|---|
| **F1** | Com a gaveta aberta, o **Voltar a reabria** em vez de fechá-la. `onBackPressed` caía no ramo do `enableBackMenu`, que agora abre a gaveta — dois Voltares seguidos e o painel continuava na tela. | `emu06`: gaveta idêntica depois de dois `KEYCODE_BACK`. |
| **F2** | A gaveta tinha uma linha chamada **"Ações"**, dentro da seção "AÇÕES", que chamava `showGameMenu(null)` — ou seja, **abria a própria gaveta**. Sobra da absorção do menu legado. | Leitura do `setOnClickListener` depois de vê-la na captura. |
| **F3** | "Soltar a tela" saía em **vermelho de alerta** com o subtítulo "a sessão continua aberta no PC" — vermelho e não-destrutiva na mesma linha. | `emu08`. |
| **F4** | **Não havia saída segura nenhuma.** A gaveta só oferecia encerrar. O que o plano chama de "sair para a casca com o vídeo vivo atrás" não tinha como ser acionado por ninguém. | Inventário dos ouvintes da folha. |
| **F5** | Com a casca aberta por cima do vídeo, **nada dizia que havia vídeo atrás**. A home era idêntica à de quem nunca abriu nada, e o único caminho de volta era o Voltar — que não se vê. | `emu09`. |
| **F6** | O grupo TELA do editor de cena quebrava em **3 + 3 + 1**, deixando "1704x786" sozinho na última linha — exatamente o que o comentário do próprio arquivo dizia que não podia acontecer, escrito para o caso de quatro e nunca generalizado. | `compose-editor-de-cena.png`. |

**O que a ida e volta prova agora:** sessão → gaveta → "Escolher outra coisa" → casca com a
faixa "Moque está rodando" → toque na faixa → **a mesma sessão**, quadro 358 continuando de
onde parou. Nenhuma reconexão, nenhuma troca de janela. É a ideia 1 fechando o ciclo.

**O que o moque não prova, e continua sem prova:** latência, decodificação, HDR, e qualquer
coisa que dependa do `MoonBridge`. O vídeo dele é um `lockCanvas` a 30 fps.
