# Tokens da Console (Plano B) — F1

Fonte de verdade do que existe **em código** para o redesenho "Console". Deriva de
`docs/design/console/NOTA.md` §2 (que não veio commitada neste worktree ainda — leia-a no
repositório principal, `docs/design/console/NOTA.md`, ou em `docs/planos/B-CONSOLE.md` §F1) e
implementa exatamente os valores de lá. Nenhuma cor ou dimensão aqui é nova em relação à NOTA;
onde uma decisão de implementação não estava explícita na NOTA, isso está marcado.

## Cor

| Token | Recurso | Valor | Onde usar |
|---|---|---|---|
| `console_fundo` | `res/values/colors.xml` | `#0B0B0D` | fundo do app, vídeo fora da sessão |
| `console_superficie` | `res/values/colors.xml` | `#16161A` | cards em repouso, listas, painéis, barra da sessão |
| `console_superficie_elevada` | `res/values/colors.xml` | `#1C1C20` | popover do seletor de PC, item com foco, capa focada |
| `console_superficie_selecionada` | `res/values/colors.xml` | `#22232A` | linha "atual" no seletor, capa focada (fundo) |
| `console_borda` | `res/values/colors.xml` | `#2A2A30` | divisórias, contorno de cards em repouso |
| `console_texto` | `res/values/colors.xml` | `#F2F2F4` | títulos, rótulos, valores |
| `console_texto_2` | `res/values/colors.xml` | `#9A9AA5` | legendas, estado, "on-line", valores inativos |
| `console_texto_desabilitado` | `res/values/colors.xml` | `#5A5A62` | rótulo de item desabilitado |
| `console_texto_desabilitado_2` | `res/values/colors.xml` | `#6A6A72` | legenda de item desabilitado |
| `console_acento` | `res/values/colors.xml` | `#7FE0C7` | contorno de foco, "em execução"/"on-line", botão primário — papel único, nunca decoração |
| `console_acento_sobre` | `res/values/colors.xml` | `#0A1F1A` | texto/ícone sobre superfície de acento |
| `console_destrutivo` | `res/values/colors.xml` | `#F2555B` | "Encerrar no PC" |
| `console_destrutivo_fundo` | `res/values/colors.xml` | `#1FF2555B` (destrutivo a 12%) | fundo do bloco "Encerrar no PC" |

### Aliases `vidro_*` (transitórios — F6 apaga)

Cada `vidro_*` que já existia virou uma referência (`@color/console_*`) em vez de guardar seu
próprio hex, para as telas ainda não redesenhadas (praticamente todas nesta fatia) herdarem a
paleta nova sem trocar de layout. Mapeamento por papel, não por nome — os 24 `vidro_*` e os 13
`console_*` não têm correspondência 1:1:

| `vidro_*` | Alias para | Papel original |
|---|---|---|
| `vidro_void` | `console_fundo` | fundo do app |
| `vidro_surface` | `console_superficie` | cartão, folha |
| `vidro_surface_alt` | `console_superficie_elevada` | cartão elevado |
| `vidro_glass_session` | `console_superficie_elevada` | base do vidro / painel da sessão |
| `vidro_text` | `console_texto` | títulos, rótulos, valores |
| `vidro_text_2` | `console_texto_2` | legendas, estado |
| `vidro_text_3` | `console_texto_2` | terciário — a Console só tem duas camadas de texto vivo |
| `vidro_text_night` | `console_texto` | variante "sala escura"; **sem uso em `res/` hoje** |
| `vidro_accent_glow` | `console_acento` | um dos três papéis antigos do acento |
| `vidro_accent_text` | `console_acento` | idem |
| `vidro_accent_fill` | `console_acento` | idem |
| `vidro_on_accent` | `console_acento_sobre` | tinta sobre o acento |
| `vidro_light_core` | `console_acento` | núcleo do halo; **sem uso em `res/` hoje** |
| `vidro_glass_fill_session` | `console_superficie_elevada` | painel translúcido em sessão → sólido mais próximo |
| `vidro_glass_fill_idle` | `console_superficie` | painel translúcido fora de sessão → sólido mais próximo |
| `vidro_stroke` | `console_borda` | divisórias, contorno — **sem uso em `res/` hoje** |
| `vidro_stroke_glass` | `console_borda` | contorno sobre vidro em sessão |
| `vidro_stroke_hairline` | `console_borda` | contorno de cartão comum |
| `vidro_stroke_faint` | `console_borda` | divisória interna |
| `vidro_surface_overlay` | `console_superficie` | realce sutil sobre fundo |
| `vidro_surface_overlay_alt` | `console_superficie_selecionada` | estado pressionado |
| `vidro_halo_awake` | `console_acento` | halo aceso = estado ativo |
| `vidro_halo_night` | `console_texto_2` | halo noturno; **sem uso em `res/` hoje** |
| `vidro_halo_asleep` | `console_texto_2` | halo dormindo |
| `vidro_halo_none` | `console_texto_2` | nenhum host; **sem uso em `res/` hoje** |

`profileAccent` (`#FF4081`, marcador de perfil) não é um token `vidro_*` do inventário da B e
fica fora deste plano — nenhuma fatia da Console pediu para tocá-lo.

## Dimensão

Convenção usada nos dois pares que variam por aparelho: `res/values/dimens.xml` (sem
qualificador) é o bucket de **celular** — porque `values-sw600dp/` sempre vence nele em telas
≥600dp, sobra "o resto" (celular) como default. `values-sw600dp/dimens.xml` guarda só os
overrides de **tablet**.

| Token | Recurso | Valor | Onde usar |
|---|---|---|---|
| `console_espaco_1..6` | `values/dimens.xml` | 4 / 8 / 12 / 16 / 24 / 32 dp | ritmo de espaçamento geral |
| `console_raio_controle` | `values/dimens.xml` | 8dp | botões, chips, controle segmentado, gatilho da sessão |
| `console_raio_card` | `values/dimens.xml` | 12dp | cards, capas, painéis |
| `console_linha_ajuste` | `values/dimens.xml` = **56dp** (celular) · `values-sw600dp/dimens.xml` = **64dp** (tablet) | altura de linha de Ajustes (F4) |
| `console_botao` | `values/dimens.xml` | 48dp | altura de `ConsoleBotao.*` |
| `console_capa_w` / `console_capa_h` | `values/dimens.xml` | 160dp / 213dp | capa de app, 3:4 (valor único; ver "decisões" abaixo) |
| `console_foco_borda` | `values/dimens.xml` | 3dp | espessura do contorno de foco |
| `console_foco_anel` | `values/dimens.xml` | 4dp | espessura do anel suave por fora do contorno |
| `console_aba_w` / `console_aba_h` | `values/dimens.xml` = **56×16dp** (celular) · `values-sw600dp/dimens.xml` = **64×20dp** (tablet) | gatilho do menu da sessão (F2) |

## Tipografia (`res/values/styles.xml`, override de `Rotulo` em `values-sw600dp/styles.xml`)

| Estilo | Fonte / peso / tamanho | Onde usar |
|---|---|---|
| `ConsoleText.Titulo` | Manrope ExtraBold(800) · 32sp · lineSpacing 1.15 | "Ajustes", "Computadores", "Apps" |
| `ConsoleText.Card` | Manrope Bold(700) · 20sp | nome do app focado, "Área de trabalho" |
| `ConsoleText.Rotulo` | Manrope SemiBold(600) · **14sp** no celular (`values/`), **16sp** no tablet (`values-sw600dp/`) | linhas de ajuste, nome de PC, blocos de menu |
| `ConsoleText.Rotulo2` | Manrope Medium(500) · 14sp · cor `console_texto_2` | "· on-line", legendas de estado |
| `ConsoleText.Legenda` | Manrope Medium(500) · 12sp · caixa alta · tracking 0,08em · cor `console_texto_2` | uppercase de seção, ajuda de item desabilitado |
| `ConsoleText.Mono` | JetBrains Mono Medium · 13sp · `tnum` | endereço IP, "18 ms · 35 Mbps", teclas especiais |

## Componentes

| Estilo | Composição | Onde usar |
|---|---|---|
| `ConsoleBotao.Primario` | fundo `console_botao_primario_bg` (acento sólido, raio `console_raio_controle`), texto `console_acento_sobre`, altura `console_botao` | ação principal ("Abrir", "Retomar", "Enviar") |
| `ConsoleBotao.Neutro` | fundo `console_botao_neutro_bg` (`console_superficie` + borda `console_borda`, mesmo raio), texto `console_texto` | ação secundária |
| `ConsoleSegmento` | Manrope SemiBold(600) · 13sp · altura mínima 38dp | opção de controle segmentado (Resolução, Taxa, Codec, Modo do toque) |
| `console_foco.xml` | selector: `state_focused` → contorno `console_foco_borda` em `console_acento` + anel `console_foco_anel` a 15% de opacidade do acento; `state_pressed` → `console_superficie_selecionada`; default transparente | `android:foreground`/`background` de item focável genérico (raio `console_raio_controle`) |
| `console_capa_foco.xml` | mesma composição do `console_foco.xml`, raio `console_raio_card`, só o contorno (sem pressed, sem fundo) | contorno de foco da capa de app — a escala 1,2× e o fundo `console_superficie_selecionada` da capa ficam para a F3 |

`AppTheme` (`res/values/styles.xml`): `colorPrimary`/`colorAccent`/`android:colorAccent`/
`colorControlActivated` → `console_acento`; `android:windowBackground` → `console_fundo`;
`colorError` → `console_destrutivo`. `android:statusBarColor`/`navigationBarColor`/
`colorBackground`/`textColorPrimary`/`textColorSecondary` continuam apontando para `vidro_*` —
resolvem para o mesmo valor pelo alias, sem necessidade de tocar neles agora.

## Decisões desta fatia (não estavam explícitas na F1 nem na NOTA)

- **`console_botao_primario_bg.xml` e `console_botao_neutro_bg.xml`** (`res/drawable/`) foram
  criados porque `ConsoleBotao.Primario`/`.Neutro` precisam de um `android:background` real para
  existir; a F1 só nomeava os drawables de foco explicitamente. São shapes de uma linha só, sem
  cor nova (reaproveitam `console_acento`/`console_superficie`/`console_borda` e o dimen
  `vidro_stroke_width` já existente para a espessura de 1px da borda).
- **Anel de foco a 15% de opacidade** (`#267FE0C7`) não virou um token novo em `colors.xml` — é
  literal nos dois drawables de foco, calculado como `console_acento` a 15% de alfa (mesma conta
  do `console_destrutivo_fundo`, que já é 12%). Isso respeita "nenhuma cor nova além da NOTA":
  não é uma cor nova, é a mesma cor do contorno, esmaecida, exatamente como a NOTA descreve.
- **`console_capa_w`/`h` sem variação por aparelho.** A NOTA fala em "180×240 aprox. no celular,
  2 colunas" para o tablet vs. celular, mas a lista de dimensões da F1 dá um valor só
  (160×213). Segui a F1 literalmente — ela é a fonte de verdade desta fatia — e deixei a
  variação por aparelho, se necessária, para a F3 (que constrói a grade de capas de verdade e
  pode decidir com a tela na frente).
- **Estilos `VidroText.*`/`VidroButton.*`/`VidroChip`/`VidroDialog*` não foram reparentados para
  `Console*`.** A F1 permite ("se fizer sentido, faça-os herdar dos Console*"), mas o parent de
  um estilo também herda `letterSpacing`/`lineSpacingMultiplier`/tamanho — trocar o parent
  mudaria a métrica das telas antigas, que esta fatia não deveria redesenhar. A cor delas já muda
  sozinha, via os aliases `vidro_*` → `console_*` em `colors.xml`; a fatia que reconstruir cada
  tela decide então se herda de `Console*` ou substitui o layout inteiro.
- **`console_foco.xml`/`console_capa_foco.xml` não foram aplicados a nenhum layout existente.**
  A F1 pede o recurso pronto, não a aplicação — "sem redesenhar telas ainda". Quem aplicar deve
  lembrar que `android:foreground` em `View` comum (não `ViewGroup`) só existe a partir da API 23;
  o app tem `minSdk 21`.
