# ADR 0003 — Stack da interface: Java + XML, não Compose

- **Status:** aceito
- **Data:** 2026-09-04
- **Decisor:** Rafael (dono do fork), via Fable (planejamento)

## Contexto

O redesenho anterior (opção "Vidro") tentou casca em Jetpack Compose sobre a sessão nativa
em Java. `docs/REPLANEJAMENTO.md` documenta o resultado: o menu da sessão ficou invisível no
S22 Ultra em quatro entregas seguidas, todas "verificadas" só no emulador — que não tem o
compositor da Samsung nem a navegação por gestos do One UI. O problema não era Compose em
si, mas a decisão foi tomada sem medir o que ela custava no único lugar que importa: uma
segunda pilha de UI (Views nativas para `Game`/`SurfaceView`, Compose para o resto) dobra a
superfície de bugs de composição de janela exatamente onde o fork já vinha errando.

O plano B ("Console") — `docs/planos/B-CONSOLE.md` — precisa de uma decisão de stack antes
da primeira fatia, porque toda fatia depois dela assume Views: os layouts XML existentes
(`activity_pc_view.xml`, `activity_game.xml`, etc.), os moques de F0, `tools/screens.mjs`
(Robolectric sobre `View`, não sobre `@Composable`).

## Decisão

**Java + XML. Sem Kotlin, sem Compose**, para a casca inteira (PcView, AppView, Ajustes,
Cenas) e para a sessão (`Game`, já era View).

Motivos, na ordem que pesaram:

1. **`AGENTS.md` já exige perguntar antes de introduzir Kotlin** ("Pergunte antes de:
   Introduzir Kotlin"). Compose sem Kotlin não é uma opção real — seria introduzir as duas
   de uma vez, no meio de um plano que existe para reduzir risco, não para aumentá-lo.
2. **`tools/screens.mjs` já rasteriza layouts XML na JVM** via Robolectric, em segundos, sem
   emulador. Portar isso para Compose exigiria Roborazzi (dependência nova, pergunta à parte)
   e reescrever o harness que acabou de provar seu valor neste mesmo replanejamento.
3. **Foco por D-pad é território nativo de `View`** (`View.OnFocusChangeListener`,
   `focusSearch`, `nextFocusUp/Down/Left/Right` no XML). A B usa foco como mecanismo
   principal de navegação (capas, blocos da guia da sessão) — é exatamente o caso de uso que
   `View` resolve com menos código e mais previsibilidade do que o foco declarativo do
   Compose, que ainda está em desenvolvimento ativo rio acima.
4. **A sessão nunca saiu de View.** `Game.java` e `SurfaceView` são nativos e continuam
   sendo — misturar Compose na casca criaria duas linguagens de UI no mesmo app sem
   necessidade, com o custo de manutenção caindo sobre quem vier depois.
5. **Zero dependência nova.** Fica tudo dentro do que o projeto já usa e o gate já verifica.

## Consequências

**Positivas**
- Uma pilha de UI só, do início ao fim: `Game`, `PcView`, `AppView`, `StreamSettings`.
- `tools/screens.mjs` continua sendo o loop rápido de composição/tipografia para toda tela
  nova da B, sem escrever um segundo harness.
- Foco por D-pad (celular sem toque, tablet com capa-teclado) fica em cima de uma API madura
  e bem entendida no projeto.

**Negativas**
- XML programático dá mais código boilerplate que `@Composable` para telas com muito estado
  (a tela de Ajustes básica, por exemplo). Aceito: é o preço de não abrir uma segunda stack.
- Sem *live preview* de IDE. Mitigado por `tools/screens.mjs` (mais rápido que a maioria dos
  previews de qualquer forma, porque roda fora do Android Studio).

## Revisitar se

A B pedir algo que `View` genuinamente não entrega bem — animação físico-baseada complexa
encadeada com gesto, por exemplo. Até lá, introduzir Compose é decisão de outro ADR, não uma
conveniência de fatia.
