# Vidro — especificação extraída do handoff

Fonte: `docs/design/vidro/Vidro-Handoff.html`, seção por seção. Este arquivo é a
transcrição dos dados que lá são gerados por script — o handoff mostra tabelas, mas os
valores vivem no bloco `<script>`, então ficam aqui em texto para poderem ser lidos por
grep e por quem não abrir o HTML.

**Onde divergir, o protótipo está certo.** Este documento é cópia, não interpretação.

## Alvo

Celular 393×852 dp retrato, exceto a sessão, que é 852×393 paisagem.

## Telas — 7 + 4 interrupções

```js
const TELAS = [
  { id: "01", nome: "Home · início", formato: "393×852", proposito: "chuta o que ele vai fazer",
    hierarquia: "barra de estado (hora · Wi-Fi · bateria)\nsaudação — \"Boa noite, Rafael\"\ntítulo de estado do host\ncard de antecipação (quando há palpite)\n\"Continuar\" — lista das últimas sessões\n\"Seus lugares\" — hosts + \"Adicionar\"\nrodapé — Cenas · Ajustes\nluz atrás do topo",
    regras: "Título muda com o host:\n  aceso → \"O PC está acordado\"\n  dormindo → \"O PC está dormindo\"\n  nenhum → \"Nenhum PC por perto\"\nCard: rótulo \"PROVÁVEL AGORA\", título \"Retomar Desktop\",\nexplicação do porquê, e as saídas \"Agora não\" / \"Hoje não\".\nHosts mostram estado em palavra: pronto · dormindo · fora do ar.\nZero número técnico nesta tela." },
  { id: "02", nome: "Catálogo do host", formato: "393×852", proposito: "o que abrir",
    hierarquia: "voltar — \"‹ Lugares\"\nnome do host + linha de estado\nlinha da lista — \"6 aplicativos publicados pelo host\" · \"Recarregar\"\nfaixa de sessão aberta (quando existe) + \"Voltar\"\ngrade 2 colunas de capas 132 dp\nluz atrás do app aberto",
    regras: "Sem categorias, sem abas: o protocolo não tem taxonomia.\nRemote Display e Remote Control são cards iguais aos outros,\ndiferenciados só por pílula (\"tela\", \"entrada\").\nApp em execução ganha pílula \"aberto\" e borda de acento.\nSem box art do host, usar placeholder listrado com o nome." },
  { id: "03", nome: "Conectando", formato: "393×852 → paisagem", proposito: "cobre a espera",
    hierarquia: "a capa tocada cresce até tela cheia (transição compartilhada)\npasso atual em texto\nbarra de progresso fina\nluz sobe junto com o progresso",
    regras: "Passos, nesta ordem:\n  \"Falando com PC-RAFAEL\" · 18%\n  \"Reservando a tela no PC\" · 62%\n  \"Chegando o vídeo\" · 100%\nNo app cada passo espera evento real da conexão.\nNada de spinner: a barra e a luz já dizem que está vivo." },
  { id: "04", nome: "Em sessão", formato: "852×393 paisagem", proposito: "sair do caminho",
    hierarquia: "vídeo em tela cheia (SurfaceView)\nilha de controle embaixo à esquerda\n  → recolhe em pílula após 6,2 s\nbotão de sair à direita\nambilight fraco nas bordas\nfolha de ajustes (sob demanda)",
    regras: "A ilha mostra: nome do app, qualidade em palavra, ações.\nQualidade: \"tudo fluindo bem\" · \"segurando o sinal\" · \"ruim agora\".\nToque em qualquer lugar reabre a ilha; ela recolhe sozinha.\nTudo que é ação vive nos 96 dp de baixo (alcance do polegar).\nNúmero só se o usuário ligar \"Números na tela\"." },
  { id: "05", nome: "Folha de ajustes", formato: "sobre a sessão", proposito: "mudar sem sair",
    hierarquia: "fundo escurecido\nfolha de baixo para cima, fosca\ntaxa de bits · ritmo de quadros\nmodo do mouse\nchave \"Números na tela\"",
    regras: "Abre pelo botão da ilha, nunca por menu do sistema.\nO stream não desmonta e o aparelho não volta para retrato.\nMudança de modo do mouse reflete na ilha imediatamente.\n\"Números na tela\" é a mesma coisa que o Detalhes do Moonlight." },
  { id: "06", nome: "Cenas", formato: "393×852", proposito: "trocar quatro coisas num toque",
    hierarquia: "título\nlista de cenas em cards grandes\ncada card: nome + o que ela muda\ncena ativa com borda de acento",
    regras: "Três cenas: \"Trabalho\", \"Jogo à noite\", \"Só entrada\".\nCada uma define papel, tela, taxa e entrada.\nA cena escolhida aparece no rodapé da sessão.\nÉ preferência local do cliente — o host não sabe de cena." },
  { id: "07", nome: "Pareamento", formato: "393×852 · 3 passos", proposito: "primeiro encontro",
    hierarquia: "passo 1 — procurando na rede (mDNS)\npasso 2 — código de 4 dígitos em JetBrains Mono\npasso 3 — pareado, mostra o que o host publica\nluz atrás do código",
    regras: "Rótulo \"PASSO n DE 3\" em caixa alta.\nO código é gerado pelo host e expira: dizer isso em texto.\nEntrada manual por IP e por link art:// como saída alternativa.\nNunca chamar de \"emparelhar dispositivo\": é \"novo lugar\"." },
  { id: "08", nome: "Interrupção · rede tropeça", formato: "852×393", proposito: "avisar sem assustar",
    hierarquia: "faixa no topo, fosca\n\"Segurando o sinal\"\napoio — \"a imagem pode enrolar um pouco\"\nluz encolhe e esfria",
    regras: "Aparece só quando piora de verdade, e some sozinha.\nNão se toca: aviso não é botão.\nNenhum número, nem aqui." },
  { id: "09", nome: "Interrupção · caiu", formato: "852×393", proposito: "não mentir",
    hierarquia: "vídeo escurece\n\"Perdemos o PC-RAFAEL\"\nexplicação de que a sessão continua no host\n\"Tentar de novo\" (cheio) · \"Sair\"\ncontador de tentativa",
    regras: "Texto exato do apoio: \"A sessão continua aberta no PC por mais\nalguns instantes. Se você voltar agora, volta no mesmo lugar.\"\nTentativa automática, mostrando \"3ª de 5\"." },
  { id: "10", nome: "Interrupção · PC dormiu", formato: "393×852", proposito: "oferecer o acordar",
    hierarquia: "home com luz em 0.20\n\"O PC-RAFAEL foi dormir\"\ncard com hora do adormecer e o que estava aberto\n\"Acordar o PC\" (cheio) · \"Agora não\"",
    regras: "Acordar é Wake-on-LAN de verdade — dizer que leva ~20 s.\nSe falhar, volta ao mesmo card sem drama, com \"tentar de novo\"." },
  { id: "11", nome: "Interrupção · código recusado", formato: "393×852", proposito: "explicar a causa",
    hierarquia: "passo 2 do pareamento\n\"Esse código não foi aceito\"\nexplicação da expiração\nos 4 dígitos apagados\ncard \"Onde olhar\"\n\"Pedir um código novo\" · alternativa art://",
    regras: "Nunca dizer \"erro\" ou mostrar código de status.\nA causa mais comum é expiração — dizer isso primeiro." }
];
```

## Movimento

```js
const MOTION = [
  { nome: "capa → vídeo", ms: "420", curva: "cubic-bezier(.22,.72,0,1)", oque: "transform + raio da capa tocada" },
  { nome: "vídeo → card (saída)", ms: "340", curva: "cubic-bezier(.32,.72,0,1)", oque: "transform + raio + alpha da luz" },
  { nome: "ilha recolhe", ms: "260", curva: "cubic-bezier(.4,0,.2,1)", oque: "largura via transform, alpha do conteúdo" },
  { nome: "folha sobe", ms: "240", curva: "cubic-bezier(.2,.8,.2,1)", oque: "translateY + alpha do fundo escuro" },
  { nome: "folha desce", ms: "180", curva: "cubic-bezier(.4,0,1,1)", oque: "translateY + alpha" },
  { nome: "luz muda de estado", ms: "900", curva: "ease-out", oque: "alpha e raio do gradiente" },
  { nome: "mola do toque", ms: "120", curva: "ease-out", oque: "scale 0.97 e volta" },
  { nome: "voltar por gesto", ms: "220", curva: "segue o dedo, solta com ease-out", oque: "translateX das duas telas" }
];
```

Sequência de conexão: 0 ms card cresce · 0 ms "Falando com PC-RAFAEL" 18% · 800 ms
"Reservando a tela no PC" 62% · 1600 ms "Chegando o vídeo" 100% · 2500 ms vídeo entra ·
6200 ms ilha recolhe. **No app cada passo espera o evento real do NvConnection** — só o
recolhimento de 6,2 s é temporizador de verdade.

Proibido animar: raio de blur, sombra, largura/altura de layout, e qualquer coisa por
quadro durante a sessão. Só `transform`, `alpha` e cor.

## Modelo de estado

```js
const ESTADOS = [
  { chave: "tela", valores: "home · catalogo · conectando · sessao · cenas · pareamento · procurando", efeito: "raiz da navegação, com pilha para o gesto de voltar" },
  { chave: "pc", valores: "aceso · dormindo · nenhum", efeito: "título da home, intensidade da luz, o que é clicável" },
  { chave: "rede", valores: "otima · instavel · ruim", efeito: "raio e croma da luz, palavra de qualidade na ilha" },
  { chave: "palpite", valores: "certo · errado · mudo", efeito: "existência e teor do card de antecipação" },
  { chave: "pareado", valores: "true · false", efeito: "catálogo real ou fluxo de primeiro encontro" },
  { chave: "app", valores: "Desktop · Remote Display · Remote Control · Steam · RetroArch · OBS", efeito: "título da sessão e origem da transição compartilhada" },
  { chave: "cena", valores: "Trabalho · Jogo à noite · Só entrada", efeito: "papel, tela, taxa e entrada; aparece no rodapé da sessão" },
  { chave: "ilha", valores: "aberta · pilula", efeito: "tamanho do controle em sessão; recolhe em 6,2 s" },
  { chave: "folha", valores: "true · false", efeito: "folha de ajustes por cima do vídeo" },
  { chave: "detalhes", valores: "true · false", efeito: "overlay de números — equivale ao Detalhes do Moonlight" },
  { chave: "tomada", valores: "true · false", efeito: "outro cliente assumiu a sessão" },
  { chave: "passo", valores: "0 · 1 · 2", efeito: "etapa da conexão ou do pareamento" }
];
```

## Mapa do código

```js
const MAPA = [
  { tela: "Home", arquivo: "com.limelight.PcView + res/layout/activity_pc_view.xml", obs: "grade de PCs vira lista de lugares + antecipação" },
  { tela: "Catálogo", arquivo: "com.limelight.AppView + res/layout/app_grid_item.xml", obs: "capa 132 dp, pílulas de estado, sem abas" },
  { tela: "Sessão", arquivo: "com.limelight.Game + com.limelight.ui.StreamView", obs: "SurfaceView: nada de blur por cima" },
  { tela: "Ilha e menu", arquivo: "com.limelight.binding.input.* + GameMenu", obs: "substitui o menu de lista atual pela ilha" },
  { tela: "Ajustes em sessão", arquivo: "GameMenu → nova BottomSheet", obs: "não pode desmontar o stream" },
  { tela: "Pareamento", arquivo: "PcView.doPair + NvHTTP.pair", obs: "código de 4 dígitos, expiração, art://" },
  { tela: "Acordar", arquivo: "com.limelight.computers.WakeOnLanSender", obs: "já existe; só falta a tela" },
  { tela: "Estado do host", arquivo: "computers.ComputerManagerService", obs: "polling de 1,5 s alimenta rede e estado" },
  { tela: "Cenas", arquivo: "preferences.PreferenceConfiguration", obs: "preferência local; nada vai para o host" },
  { tela: "Números na tela", arquivo: "PerformanceOverlay (strings já traduzidas)", obs: "desligado por padrão" }
];
```

## Correções de contraste (APCA)

```js
const APCA_LINHAS = [
  { nome: "Secundário na ilha", antes: "57", depois: "80", fix: "texto L 77% → 88%, vidro .82 → .88" },
  { nome: "Rótulo de chip", antes: "30", depois: "65", fix: "texto L 60% → 80%" },
  { nome: "Acento como texto", antes: "36", depois: "64", fix: "acento de texto em L 80%" },
  { nome: "Texto na pílula recolhida", antes: "38", depois: "80", fix: "alpha .55 → .88" },
  { nome: "Tinta no botão cheio", antes: "43", depois: "64", fix: "preenchimento L 64% → 78%" },
  { nome: "Terciário da home", antes: "35", depois: "60", fix: "texto L 60% → 76%" }
];
```

Piso: **Lc 60** para qualquer texto de interface. Abaixo disso sobe a luz do texto —
nunca se resolve com peso ou tamanho.

## Cores

```js
const CORES = [
  ["vidro_void", "oklch(9.5% .008 265)", "#020305", "fundo de tudo"],
  ["vidro_surface", "oklch(15% .010 265)", "#090B0F", "card em repouso"],
  ["vidro_surface_alt", "oklch(19% .012 265)", "#111419", "card destacado"],
  ["vidro_glass_session", "oklch(14% .012 265)", "#07090E", "ilha e folha, com alpha .88"],
  ["vidro_stroke", "oklch(26% .010 265)", "#222429", "borda de 1 px"],
  ["vidro_text", "oklch(97% .004 265)", "#F4F5F8", "título e corpo"],
  ["vidro_text_2", "oklch(88% .008 265)", "#D5D7DD", "apoio"],
  ["vidro_text_3", "oklch(76% .008 265)", "#AEB1B6", "terciário — piso Lc 60"],
  ["vidro_accent_glow", "oklch(64% .17 250)", "#1E8FEE", "luz, ponto, halo"],
  ["vidro_accent_text", "oklch(80% .12 250)", "#80C3FF", "link, rótulo aceso"],
  ["vidro_accent_fill", "oklch(78% .145 250)", "#69BDFF", "botão cheio"],
  ["vidro_on_accent", "oklch(14% .04 265)", "#030819", "tinta sobre o acento"],
  ["vidro_light_core", "oklch(56% .10 250)", "#4378AD", "núcleo do gradiente da luz"]
];
```

Alpha: vidro em sessão **0.88** · vidro fora de sessão **0.55 + blur** · luz aceso 0.72 ·
dormindo 0.20 · nenhum host 0.07 · borda de vidro `oklch(100% 0 0 / .16)`.

Raio: aparelho 34 · card grande/folha 22–24 · card de app/linha 18–20 · botão 18–20 ·
pílula e chip 99.

Espaço: margem lateral 18–22 · respiro entre grupos 18–22 · gap dentro de grupo 8–12 ·
alvo de toque 48 mín / 52 padrão · faixa de ação em paisagem 96 de baixo.

## Tipografia

| papel | tamanho | peso | tracking | onde |
|---|---|---|---|---|
| Título de tela | 29–34 sp | 800 | -0.035em | "O PC está acordado", "PC-RAFAEL" |
| Título de card | 19–21 sp | 700 | -0.03em | "Retomar Desktop", nome de cena |
| Item de lista | 15–16 sp | 600 | -0.02em | nome de host, nome de app |
| Corpo / apoio | 13–14 sp | 400–500 | 0 | explicação do palpite, estado do host |
| Rótulo de seção | 12 sp | 700–800 | +0.10em caixa alta | "PROVÁVEL AGORA", "PASSO 2 DE 3" |
| Número técnico | 12–28 sp | 400–700 | 0 · tabular | código de pareamento, ms, IP |

Manrope carrega a voz. JetBrains Mono só onde o valor é técnico. **Nenhuma fonte de
sistema, nenhum Roboto sobrando.**
