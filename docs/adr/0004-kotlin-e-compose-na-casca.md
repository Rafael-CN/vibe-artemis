# ADR 0004 — Kotlin e Jetpack Compose, restritos à casca

- **Status:** aceito
- **Data:** 2026-08-21
- **Decisor:** Rafael (dono do fork)
- **Contexto anterior:** [ADR 0003](0003-stack-da-interface.md) · [piloto](../guides/compose-piloto-home.md)

## Contexto

O ADR 0003 mediu as camadas do app e concluiu que migrar a interface para Flutter ou React
Native produziria um app com duas stacks de UI permanentes, porque **46% do código — sessão,
entrada e vídeo — não pode migrar**: o `MediaCodec` decodifica direto na `Surface` de um
`SurfaceView`, e a entrada é API crua de plataforma (455 `KeyEvent`, 359 `MotionEvent`, 252
`InputDevice`, drivers USB, captura de ponteiro).

O limite real da stack atual não era liberdade visual: era **XML não ter modelo de
composição**. 43 arquivos de layout com o mesmo desenho duplicado em três pastas, e a
armadilha de que um id novo exige editar os três — senão `findViewById` volta `null` só no
tablet.

O Compose resolve isso sem custo nenhum nas duas costuras críticas, mas exige Kotlin, que a
regra do `AGENTS.md` proibia.

## Decisão

**Kotlin e Compose entram, restritos à casca.**

Casca = as telas fora da sessão: home, catálogo, cenas, endereço, pareamento, ajustes.
Fora da casca — `Game.java`, `binding/input/**`, `binding/video/**`, a fronteira JNI —
**continua Java, e a mudança não é negociada por conveniência**: ela exigiria outro ADR e
teria de refutar a costura A do ADR 0003 com medição em aparelho.

Ficou fora da dependência, de propósito, o **Material 3**. Ele traz ripple, elevação, formas
e paleta do Material — a lista PROIBIDO do handoff Vidro. Só `androidx.compose.ui` e
`androidx.compose.foundation` entram, e a identidade continua vindo dos tokens de
`res/values/`, lidos por `colorResource`/`dimensionResource`/`Font`. **Uma fonte de verdade
só, durante toda a convivência.**

## O que foi feito no piloto

A grade da home. `PcGridAdapter`, `PcCellSnapshot` e o `AdapterFragment` da home saíram;
entraram `HostUi` (imutável), `EstadoDaHome` (estado observável) e `GradeDeHosts`
(composable). A fronteira com o Java é um arquivo só, `PonteDaHome.kt` — o que torna o
piloto reversível em um commit.

O ganho não foi capacidade nova, foi **obrigação removida**: o Q36 existiu porque a
sondagem entrega sempre a mesma instância de `ComputerDetails` mutada no lugar, então
comparar referência nunca acusava mudança e foi preciso copiar campo a campo, com um
`HashMap` paralelo, para decidir se valia redesenhar. Com `data class` imutável isso é
igualdade estrutural, e quem a usa é a recomposição.

E as três cópias de layout viraram um `BoxWithConstraints` — que checa a largura **real
disponível**, não a menor largura do aparelho, e por isso acerta quando a lista divide a tela
com outra coluna, que é a home deitada no Tab S8 Ultra.

## Consequências

- **`AGENTS.md` mudou.** "Não há Kotlin" virou "há Kotlin só na casca", e a linha de
  "pergunte antes" passou a ser *levar Kotlin/Compose além da casca*.
- **A captura sem APK teve de ser portada**, e foi: `VidroComposeScreenshotTest` sobe uma
  `ComponentActivity` — `ComposeView` só compõe depois de anexado a uma janela cujo
  `LifecycleOwner` chegou a CREATED — e `tools/screens.mjs` agora roda as duas classes. Sem
  isso a migração andaria cega, que era o risco que o piloto apontou como o mais
  subestimado.
- **Dois achados que valem para toda tela migrada daqui em diante:**
  1. `painterResource` **não aceita `<shape>`** — só VectorDrawable e bitmap. Os três pontos
     de estado eram `<shape oval>` e a captura falhou. Foram redesenhados em `Canvas`, que é
     o caminho da stack e custou seis linhas.
  2. **Animação infinita trava `Looper.idle()`** no Robolectric: ela nunca esvazia a fila de
     quadros. Captura de Compose com animação em laço precisa avançar por tempo
     (`idleFor`), não drenar.
- **APK cresce.** Medir na entrega, não estimar.
- `androidx.preference` não tem equivalente em Compose. **Ajustes é a última tela a migrar**,
  não a primeira — e só depois que o catálogo de produto cortar as ~50 preferências que já
  estão marcadas para sair.
