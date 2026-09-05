# ADR 0003 — Stack da interface: migrar para Flutter/React Native, adotar Compose, ou ficar

- **Status:** em estudo — nenhuma decisão tomada
- **Data:** 2026-08-21
- **Decisor:** Rafael (dono do fork)
- **Pedido:** avaliar migrar a interface para outra stack (React, Flutter ou outra) em
  busca de maior liberdade de construção visual, mais fluidez e uma personalidade nova

## Contexto

O app é um fork de fork: Moonlight → Artemis → VibeArtemis. **50.645 linhas de Java em
167 arquivos**, mais o C nativo. Não há Kotlin, e nenhuma dependência de Kotlin no
`build.gradle`.

A pergunta não é "qual stack é melhor em geral". É "onde este app pode ser cortado sem
quebrar o que ele faz de melhor". Então a resposta começa por medir as costuras.

---

## 1. As camadas, de baixo para cima

| Camada | Onde | Linhas | Natureza |
|---|---|---|---|
| **Protocolo nativo** | `jni/moonlight-core/moonlight-common-c/` | submódulo | C. RTP, FEC, controle. **Não editável.** |
| **Fronteira JNI** | `nvstream/jni/MoonBridge.java` | 444 | 34 métodos native, 21 callbacks C→Java |
| **Protocolo Java** | `nvstream/**` | 4.273 | HTTP de pareamento, mDNS, WoL, config |
| **Mídia** | `binding/video/`, `binding/audio/` | 4.494 | MediaCodec, AudioTrack |
| **Entrada** | `binding/input/**` | 14.630 | Teclado, gamepad, toque, drivers USB |
| **Estado do app** | `computers/`, `profiles/`, `preferences/`, `utils/` | 12.401 | Descoberta, perfis, config, ajuda |
| **Interface** | `PcView`, `AppView`, `Game`, `ui/`, `grid/`, layouts | 17.082 + 6.468 XML | Activities e views |

### O corte vertical é bom. O horizontal quase não existe.

As camadas de baixo estão bem separadas: a fronteira JNI é explícita e verificada por
ferramenta, `nvstream/` é quase Java puro, `AudioTrackPlan` e `HostCapabilities` foram
extraídos justamente para serem testáveis fora do Android.

Mas **não há camada de apresentação**. Não existe ViewModel, presenter, store, nada. As
Activities *são* a lógica. Duas medidas mostram o tamanho disso:

- **`PreferenceConfiguration` tem fan-in 33** — um terço dos arquivos do app lê
  configuração diretamente do objeto de preferências, sem intermediário.
- **`Game.java` tem 5.988 linhas** e mistura, no mesmo arquivo: ciclo de vida da Activity,
  flags de janela, roteamento de toque, roteamento de teclado, gestos de vários dedos,
  ciclo de vida do decoder, a ilha de controles, a folha de ajustes, o PiP e a barra de
  composição de texto.

**Só 11,1% do código (5.603 linhas em 48 arquivos) é Java puro** e sobreviveria intacto a
qualquer troca de stack. **76,5% (38.753 linhas) toca API de UI do Android.**

Esse é o dado que mais pesa na conta: não existe um "core" pronto para receber uma
interface nova. Migrar a UI significaria, primeiro, **criar a camada que hoje não existe** —
e esse trabalho é o mesmo com qualquer stack, inclusive ficando onde estamos.

---

## 2. As duas costuras que decidem tudo

### Costura A — o vídeo nunca passa pelo app

Este é o fato mais importante do documento.

```java
// MediaCodecDecoderRenderer.java:609
videoDecoder.configure(format, renderTarget, null, 0);   // renderTarget = a Surface do SurfaceView
// :1378
videoDecoder.releaseOutputBuffer(lastIndex, nowNs);      // entrega com carimbo de tempo
```

O `MediaCodec` decodifica **direto na `Surface` do `SurfaceView`**. O quadro decodificado
nunca entra na memória do app, nunca vira textura, nunca é desenhado pela hierarquia de
views. Um `SurfaceView` é uma **camada de hardware separada**, composta pelo SurfaceFlinger.
É o caminho de menor latência que o Android oferece, e este fork investiu especificamente
nele — `releaseOutputBuffer(index, timestampNs)` é apresentação com carimbo, e o trabalho
de B20/B21 (`FramePacingRoute`) existe só para acertar essa política.

O que cada stack faz com isso:

- **React Native (Fabric)** renderiza **Views reais do Android**. Um `SurfaceView` dentro
  da árvore continua sendo um `SurfaceView` com camada própria. **O caminho do vídeo
  sobrevive intacto.** É a única vantagem técnica decisiva do RN aqui.
- **Flutter** desenha tudo na própria superfície. Para mostrar o vídeo há dois caminhos, e
  os dois cobram: (1) `Texture` + `SurfaceTexture` — o quadro vira textura GL que o Flutter
  compõe, o que acrescenta uma cópia e desmonta a apresentação por carimbo; (2)
  `AndroidView` em composição híbrida — mantém o `SurfaceView` real, mas o conteúdo Flutter
  que precisa aparecer **por cima** vai para superfícies de sobreposição separadas. E o
  caso difícil é exatamente o nosso: ilha, folha e barra de composição vivem por cima do
  vídeo o tempo todo.
- **Compose** não muda nada: continua a mesma Activity, o mesmo `SurfaceView`, a mesma
  `Surface`. Custo zero nesta costura.

> **Isto precisa ser medido, não acreditado.** O Flutter melhorou muito nessa área e eu não
> tenho medição própria em Tab S8 Ultra. Nenhuma decisão deveria ser tomada sem um *spike*
> — ver a seção 6.

### Costura B — a entrada é quase toda API crua de plataforma

Contagem de usos no código:

```
KeyEvent 455 · MotionEvent 359 · InputDevice 252 · SOURCE_ 114 · AXIS_ 89
UsbDevice 59 · getScanCode 19 · getAxisValue 17 · requestPointerCapture 6
onGenericMotionEvent 8 · onCapturedPointer 3
```

`binding/input/` sozinho é **28,9% do app**. E não é entrada genérica: é enumeração de
`InputDevice`, faixas de eixo cruas do trackpad da capa, scancodes de teclado físico,
captura de ponteiro, drivers USB de gamepad, `AXIS_GESTURE_PINCH_SCALE_FACTOR`.

Nada disso existe na abstração de entrada do Flutter ou do RN. Tudo teria de continuar
nativo e ser exposto por ponte. E, em sessão, entrada **não pode** atravessar ponte: o
produto é a latência.

### O que as duas costuras implicam, juntas

**A sessão não pode migrar.** Nem para Flutter, nem para RN. Ela é 8.647 linhas (incluindo
`Game.java`) mais os 14.630 de entrada — grosso modo **46% do app**.

Então qualquer migração para Flutter/RN produz um **app com duas stacks de UI para sempre**:
a casca numa, a sessão em Java/Android. Duas formas de construir tela, dois modelos de
estado, duas formas de animar, e uma ponte no meio. Para um app mantido por uma pessoa
(mais agentes), esse é um custo permanente, não de transição.

O que restaria de fato para a stack nova é a **casca: 8.435 linhas em 21 arquivos** —
home, catálogo, ajustes, cenas, endereço, pareamento.

---

## 3. A premissa merece ser examinada

O pedido diz "maior liberdade de construção visual" e "mais fluido, como produto Apple". Vale
confrontar isso com o que de fato quebrou nesta semana de trabalho, defeito por defeito:

| Defeito real encontrado | Era limite da stack? |
|---|---|
| Telas de tablet cortadas e esticadas | **Não.** Faltava `layout-sw600dp-land/` — arquivo que ninguém tinha escrito |
| Capturas de tablet mentiram por meses | **Não.** Qualificador `w`/`h` no lugar de `sw` |
| Card de app e linha de host sem resposta ao toque | **Não.** `@animator/vidro_press` existia e não estava aplicado; três estilos ainda o zeravam com `@null` |
| Tela "Perdemos o PC" nunca aparece | **Não.** Layout pronto, incluído em toda sessão, e nenhuma linha de Java o procura |
| 15 strings escritas e nunca usadas | **Não.** Trabalho não terminado |
| Dois terços do sistema de luz mortos | **Não.** `VidroLightView.setQuality()` sem um único chamador |
| Botões flutuantes atrapalhando | **Não.** Peça herdada que ninguém tinha apagado |

**Nenhum dos defeitos desta semana teria sido evitado por Flutter ou React Native.** Todos
eram trabalho inacabado. E uma migração jogaria fora justamente as partes que *estão*
acabadas — tokens, tipografia empacotada, a tabela de movimento, os layouts de tablet
escritos ontem.

### O limite real que a stack atual impõe

Existe, e é este: **XML não tem modelo de composição.** São 43 arquivos de layout, 6.468
linhas, com o mesmo desenho duplicado entre `layout/`, `layout-sw600dp/` e
`layout-sw600dp-land/`. O agente que escreveu os layouts de tablet deixou o aviso exato:

> *"activity_app_view.xml passou a ter MAIS DE UMA VERSÃO. Quem acrescentar uma view com id
> novo precisa acrescentar o mesmo id, com o mesmo tipo, nos três arquivos. O Java infla por
> id e não sabe que existem duas versões — no tablet o findViewById volta null e a tela
> quebra só lá."*

Isso é uma armadilha estrutural que vai cobrar pedágio em cada tela nova. É o argumento
honesto a favor de trocar — e ele aponta para uma stack **declarativa com composição**, não
para "outra stack qualquer".

Segundo limite, menor mas real: só **3 views desenham por conta própria** (`onDraw`), e
`Canvas` aparece em 2 arquivos de `ui/`. Quase toda a identidade Vidro é XML + 114 drawables
de shape. Efeito que peça desenho livre (a luz é a única hoje) custa uma classe nova.

---

## 4. As opções, avaliadas

### Flutter

**A favor:** o modelo mais próximo de "desenhar o que eu quiser" — canvas próprio,
composição real, fidelidade pixel a pixel, ótimo alvo para transformar protótipo em código.

**Contra, e é grave aqui:**
- A costura A é o pior caso do Flutter — vídeo em tela cheia com sobreposições vivas em cima.
- 46% do app não migra. App de duas stacks para sempre.
- Motor + runtime somados ao APK, que já tem 45 MB.
- Toda a entrada continua nativa e exposta por ponte.

### React Native

**A favor:** preserva o caminho do vídeo, porque renderiza Views reais. Adoção incremental
é possível.

**Contra:**
- **Entrega *menos* liberdade visual do que o pedido supõe.** Estilizar em RN é um
  subconjunto de flexbox sobre views nativas. Desenho arbitrário significa escrever uma view
  nativa — ou seja, voltar a Java/Kotlin com `Canvas`. Para o objetivo declarado, é a pior
  das três.
- Traz um runtime JS e sua cadeia de build para um app que não tem nenhum código JS a
  reaproveitar.
- Mesmo problema das duas stacks.

### Jetpack Compose

**A favor:**
- Resolve o limite real medido: composição de verdade, uma função por peça, variação por
  largura em vez de arquivo duplicado por qualificador. A armadilha dos ids em três arquivos
  desaparece.
- Desenho livre é de primeira classe (`Canvas`, `drawBehind`, `graphicsLayer`) — liberdade
  visual comparável à do Flutter.
- **Custo zero nas duas costuras.** Mesma Activity, mesmo `SurfaceView`, mesmo `MotionEvent`.
- **Adoção tela a tela, sem big bang.** `ComposeView` convive com XML no mesmo app e até na
  mesma tela. A sessão pode nunca migrar, e isso não é dívida — é a escolha certa.
- Alvo excelente para converter protótipo em código, pelo mesmo motivo que o Flutter é:
  declarativo e sem intermediário XML.

**Contra:**
- **Exige Kotlin.** Não dá para escrever composable em Java. Hoje o projeto tem zero Kotlin,
  e o `AGENTS.md` manda perguntar antes de introduzir. **É a única pergunta de verdade
  deste documento.**
- Conviveria com Java por muito tempo — mas convivendo *na mesma linguagem-plataforma*, não
  atrás de uma ponte.
- `androidx.preference` não tem equivalente em Compose: a tela de Ajustes seria escrita à
  mão. Atenuante: o catálogo de produto já prevê cortar ~50 das 126 preferências.

### Ficar no XML e terminar o que está começado

**A favor:** é o que a evidência da seção 3 recomenda no curto prazo. A fila do
`CATALOGO-PRODUTO.md` tem 44 itens vivos, e a maioria dos que estão no topo é *ligar coisa
já desenhada*, não desenhar coisa nova.

**Contra:** a armadilha da duplicação de layout continua, e cresce a cada tela de tablet.

---

## 5. Recomendação

**Não migrar para Flutter nem para React Native.** O motivo não é conservadorismo: é que os
dois produziriam um app de duas stacks permanentes, sem tocar em 46% do código, e o RN em
particular entrega menos liberdade visual do que o objetivo pede.

**A candidata real é o Compose**, e a decisão que ela exige é uma só: **aceitar Kotlin no
projeto**. Se a resposta for não, o Compose sai e a recomendação passa a ser terminar a fila
atual em XML.

**Mas nada disso antes de resolver a ordem.** A camada que falta — separar estado de tela —
é pré-requisito de qualquer migração e tem valor mesmo sem nenhuma. Com `Game.java` em 5.988
linhas e `PreferenceConfiguration` com fan-in 33, migrar a UI hoje seria reescrever tela por
tela **carregando a lógica junto**, que é o jeito mais caro possível de fazer isso.

Ordem sugerida:

1. **Terminar a fila do catálogo de produto.** Ela é barata, o ganho é imediato, e vários
   itens (P14–P23) **encolhem a superfície** que uma migração teria de cobrir. Migrar depois
   de cortar 50 preferências custa menos que migrar antes.
2. **Extrair estado de tela**, começando pelo pior caso: tirar de `Game.java` o que é
   decisão e não desenho. Isso vale por si, independentemente de stack.
3. **Só então** decidir sobre Kotlin/Compose, com a casca já menor e a lógica já separável.

## 6. O spike que deve vir antes de qualquer decisão

Nada acima sobre desempenho de Flutter deve ser aceito sem medição — inclusive o que eu
escrevi. Se você quiser explorar a sério, o experimento que resolve a dúvida é pequeno:

- Um app mínimo em Flutter que hospede um `SurfaceView` com `MediaCodec` decodificando
  H.264, com uma sobreposição animada por cima, **no Tab S8 Ultra**.
- Medir: latência do primeiro quadro, quadros perdidos com a sobreposição visível, e se
  `releaseOutputBuffer` com carimbo continua respeitado.
- Comparar contra o app atual na mesma cena.

Se o Flutter passar nesse teste, meia seção deste documento muda. Se não passar, a pergunta
fica respondida com número em vez de argumento.

## Consequências

- Enquanto este ADR estiver **em estudo**, seguir investindo na stack atual é seguro: a fila
  do catálogo é majoritariamente *ligar o que já existe*, e nada disso é jogado fora numa
  migração futura — o desenho e as decisões de produto sobrevivem, só a sintaxe muda.
- O passo 2 (extrair estado de tela) é o único trabalho que **precisa** acontecer antes, e
  ele não aposta em stack nenhuma.
- Se Kotlin for aceito, este ADR vira dois: um para a linguagem, outro para o Compose.
