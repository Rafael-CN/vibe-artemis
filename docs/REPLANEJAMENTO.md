# Replanejamento — 4 de setembro de 2026

O Rafael reprovou o resultado do redesenho. Este documento é o passo atrás que ele pediu: o
diagnóstico honesto, onde o código está de fato, a decisão de stack, **como garantir que o que
eu construo é exatamente o que ele instala**, e como o trabalho passa a rodar.

## 1. O que deu errado

1. **Verifiquei no emulador o que só quebra no aparelho dele.** O menu da sessão ficou invisível
   no S22 Ultra em quatro entregas seguidas, todas "verificadas". O emulador não tem o compositor
   da Samsung, a navegação por gestos do One UI, nem o teclado da capa do tablet. "Verificado"
   virou uma palavra sem valor.
2. **Efeito antes de base.** Molas, gaveta atrás do vídeo, transições compartilhadas, paleta com
   temperatura — tudo em cima de fluxos que não estavam sólidos no aparelho.
3. **Entregas grandes.** Cada APK levava dezenas de mudanças; quando algo quebrava, não dava
   para saber o quê.
4. **`Game.java` chegou a 6.800 linhas** e a folha da sessão trocou de camada quatro vezes.
5. **Nada foi commitado desde 19/08.** São 224 arquivos soltos, sem ponto de retorno.

## 2. Onde o código está (fato, não opinião)

- `dev` HEAD = `788c40e0` (19/08/2026, "Drop a scratch file…"). Esse commit **não contém nada do
  redesenho**: sem gaveta, sem molas, sem casca em Compose, sem paleta girada. É, na prática, a
  build que o Rafael aprovou no tablet (telas do tablet, rolagem e a barra de texto do E01).
- Tudo desde então — a refatoração visual inteira e a sessão de hoje — está **solto no working
  tree**: 136 arquivos alterados (+38.672 / −27.772), 88 novos, 11 apagados.

**Proposta:** commitar o solto, como está, numa branch de arquivo `arquivo/redesenho-vidro-2026-08`
e devolver `dev` ao HEAD de 19/08. Nada se perde; nada daquilo volta sem passar pelo processo novo.
O que ainda vale de lá (as correções funcionais verificadas — apagar sem confirmar, o Girar a tela,
os diálogos com botão apagado) volta por cherry-pick, uma por vez, com foto do aparelho.

## 3. A stack: continuar nativo, trocar a matéria e o processo

**Por que não Flutter nem React Native.** O coração do app — vídeo por `MediaCodec` numa
`SurfaceView`, entrada de teclado/mouse/gamepad, captura de mouse, áudio Opus, o protocolo do
Sunshine, o emparelhamento com OpenSSL — é Android nativo em Java e C, e continuaria sendo. Flutter
só embrulharia a casca (lista de PCs, apps, ajustes) e teria que entregar a sessão a uma Activity
nativa por uma ponte. O custo: um segundo runtime no APK, duas linguagens, uma ponte para cada
preferência, e **nenhum ganho onde a dor mora** — o menu dentro da stream e o teclado são
exatamente a parte que ficaria igual. O problema não foi o XML; foi verificar no lugar errado.

**Por que Jetpack Compose para a casca.** Já está no projeto (ADR 0004), é o stack moderno da
própria plataforma, tem Material 3, adapta tablet/celular por classe de janela, e as telas
**renderizam na JVM sem emulador** (Roborazzi): eu vejo a tela como imagem, de forma
determinística, no gate. Estado declarativo elimina a família de bugs de `findViewById` nulo que
caçamos hoje.

**A sessão (`Game.java`) volta ao que o Artemis original faz e que funciona no S22:** o menu é um
`Dialog` (é assim no upstream, e é por isso que o menu do Artemis aparece no aparelho dele), o
gatilho é um controle flutuante pequeno, a barra de texto do E01 fica. Sem truques de camada sobre
a `SurfaceView`, nunca mais.

## 4. Zero divergência: como se garante

A única garantia de que o app é igual no celular e no tablet é **instalar e fotografar no
celular e no tablet**. Não existe emulador que prometa isso. Então:

1. **Depuração por Wi-Fi nos dois aparelhos** (Android 11+): Configurações → Opções do
   desenvolvedor → Depuração por Wi-Fi → "Parear dispositivo com código". Uma vez pareado, todo
   build é instalado e fotografado **nos aparelhos**, pelos mesmos scripts que hoje rodam no
   emulador (`auditor.py`, `percurso.py`). O emulador vira opcional.
2. **A referência é o desenho aprovado.** Cada tela do aparelho é comparada ao mockup escolhido
   (diferença de pixels com tolerância). Divergiu, o gate fica vermelho.
3. **Roborazzi na JVM** para a casca, nas duas larguras, dentro de `node tools/validate.mjs`.
4. **Entrega pequena**: uma mudança por APK, com as fotos dos dois aparelhos anexadas.
   "Verificado" passa a significar uma coisa só: *no aparelho*.
5. **Commit por fatia.** Sempre há ponto de retorno.

## 5. Como o trabalho passa a rodar

- **Eu (Fable)**: plano, fatias, revisão de diff e de fotos. Não escrevo código de implementação.
- **Times de Sonnet 5**: cada fatia num worktree, com uma spec de uma página (o que muda, como
  verificar, quais telas fotografar em qual aparelho).
- Uma fatia só fecha com gate verde **e** fotos dos dois aparelhos **e** minha revisão.

## 6. As três opções de desenho

Três direções, todas "o básico, super bem feito", com os mesmos oito artboards (tablet 1280×800:
início, ajustes, sessão com menu, sessão com barra de texto; celular 412×883: computadores, apps,
ajustes; celular 883×412: sessão com menu):

| opção | ideia | tradeoff |
|---|---|---|
| **A — Sistema** | Material 3 pelo livro: componentes padrão do Android, trilho de navegação no tablet, lista+detalhe, menu da sessão como folha modal. O app se comporta como o resto do aparelho. | Menos identidade própria. |
| **B — Console** | Lançador: capas grandes, navegação por foco (setas da capa, gamepad, toque), "continuar" primeiro, menu da sessão como guia deslizante. O uso real é abrir um jogo e ficar lá. | Ajustes ficam menos densos; foco exige cuidado. |
| **C — Ferramenta** | Utilitário de acesso remoto: barra lateral fixa com os PCs, área principal densa, dados técnicos em monoespaçada, teclado-first, menu da sessão como barra fina encaixável. O tablet com teclado é um mini-notebook. | No celular a densidade precisa recuar. |

Canvas com as três (uma página por opção, mais a visão geral):
<https://claude.ai/code/artifact/eaeb1de3-56c3-4d49-83f0-32021b980858>

Os tokens de cada opção (cores em hex, escala tipográfica, raios, alturas de controle) e a
forma de convocar o menu da sessão estão na `NOTA.md` de cada direção, na área de trabalho da
sessão do Claude (`scratchpad/design/A|B|C/`), e serão copiados para `docs/design/` quando a
direção for escolhida.

## 7. Ordem depois da escolha

1. Arquivo do solto e volta ao HEAD de 19/08.
2. Depuração por Wi-Fi nos dois aparelhos; scripts de foto apontando para eles; Roborazzi no gate.
3. **Sessão primeiro**: menu como `Dialog` + gatilho flutuante + barra de texto — verificado no
   S22 antes de qualquer outra coisa, porque é o bug que prende o Rafael na stream.
4. Casca em Compose, uma tela por fatia, na ordem: PCs → apps → conectando/erro → ajustes → cenas.
5. Polimento, contra o mockup, com diff de pixels.

## 8. O que preciso do Rafael

1. A escolha da opção (ou a mistura: "A com o menu do C").
2. Depuração por Wi-Fi ligada no celular e no tablet, com `IP:porta` e o código de pareamento
   de cada um (ou rodar `adb pair` e `adb connect` ele mesmo).
3. O modelo do tablet (para as medidas exatas).
4. Um "sim" para o arquivamento do trabalho solto.
