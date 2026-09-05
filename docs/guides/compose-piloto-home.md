# Piloto: a home em Compose

Companheiro do [ADR 0003](../adr/0003-stack-da-interface.md). Aqui está o custo real de
migrar **uma tela**, medido na home, para decidir com número em vez de intuição.

A home foi escolhida porque é o pior caso honesto: é a tela com mais estado vivo do app
(sondagem de rede a cada 1,5 s), tem serviço vinculado, menu de contexto, sobreposição de
pareamento e três variantes de layout. Se o Compose ganhar aqui, ganha em qualquer outra.

---

## 1. O que a home é hoje

`PcView.java` — **1.314 linhas**, `AppCompatActivity implements AdapterFragmentCallbacks`.

| Peça | Estado atual |
|---|---|
| Layout | `activity_pc_view.xml` **×3** (`layout/`, `layout-sw600dp/`, `layout-sw600dp-land/`), 19 ids, 16 `findViewById` |
| Grade | `AdapterFragment` → `GridView` → `PcGridAdapter` (BaseAdapter) dentro de `pcFragmentContainer` |
| Dados | `ComputerManagerService` vinculado por `ServiceConnection`, sondagem por host a cada 1,5 s |
| Ações | 14 `menu.add` num menu de contexto do sistema, aberto por toque longo |
| Sobreposições | Pareamento (`showVidroPairing`/`hideVidroPairing`), estados de busca e vazio |
| Sonda de GL | Um `GLSurfaceView` descartável que **rouba o `setContentView`** no `onCreate` antes do layout real |

### O achado que decide a conta

```java
private final HashMap<String, PcCellSnapshot> celulasDesenhadas = new HashMap<>();
// ...
PcCellSnapshot celula = PcCellSnapshot.of(details);
mudou = !celula.equals(celulasDesenhadas.get(details.uuid));
```

Isto é **reconciliação escrita à mão**. O [Q36](../planos/QUICK-WINS.md#q36) existiu porque
a sondagem entrega sempre a mesma instância mutada, então comparar referência não servia e
foi preciso copiar campo a campo (`PcCellSnapshot`) para descobrir se algo mudou de verdade
— senão a home se redesenhava em ciclo e o painel nunca baixava a taxa de atualização.

**Em Compose isso é o comportamento padrão.** Um `State<List<HostUi>>` com hosts imutáveis, e
a recomposição só toca o que mudou. `PcCellSnapshot`, o `HashMap` e o raciocínio inteiro
deixam de precisar existir.

Esse é o argumento concreto, e não retórico, a favor: a tela mais dinâmica do app já pagou o
preço de implantar à mão o que a stack nova daria de graça — e vai pagar de novo em cada
tela nova com estado vivo (o "Continuar" do [P10](../CATALOGO-PRODUTO.md#p10), a faixa de
host que some do [P04](../CATALOGO-PRODUTO.md#p04), a espera do WoL do [P06](../CATALOGO-PRODUTO.md#p06)).

E o segundo ganho, o das três cópias de layout: a variação por tamanho deixa de ser
**arquivo duplicado** e vira **condição dentro da função**. Some junto a armadilha que o
agente do tablet documentou — id novo hoje exige editar três arquivos, ou `findViewById`
volta `null` só no tablet.

---

## 2. O que NÃO muda

Vale dizer primeiro, porque é a maior parte:

- `ComputerManagerService`, `ComputerDetails`, `NvHTTP`, `WakeOnLanSender`, pareamento — nada.
- `Game.java` e a sessão inteira — nada. Nunca.
- A identidade Vidro: as cores, espaçamentos e tipos já são tokens em `res/values/`. Compose
  lê `colorResource`/`dimensionResource`/`FontFamily` direto, então **os tokens continuam
  sendo a fonte única** durante toda a convivência. Nada de dois sistemas de design.
- Strings: `stringResource(R.string.x)`. Mesmo arquivo, mesma regra, mesmo pt-BR.

---

## 3. A montagem

### 3.1 Ligar o Kotlin e o Compose

Em `app/build.gradle` — **fixe as versões correntes no momento de fazer, não copie número
daqui**:

```gradle
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'org.jetbrains.kotlin.plugin.compose'   // desde Kotlin 2.0 o compilador vem junto
}

android {
    buildFeatures { compose true }
}

dependencies {
    implementation platform('androidx.compose:compose-bom:<versão corrente>')
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.foundation:foundation'
    implementation 'androidx.activity:activity-compose'
    // Material NÃO entra. Ver 3.2.
}
```

`minSdk 21` já atende o Compose. `compileSdk 36` também. Nenhum arquivo `.java` precisa
mudar para o Kotlin passar a compilar — os dois convivem no mesmo módulo.

**Custo no APK:** some algo na casa de 1,5–2,5 MB por ABI. O APK arm64 hoje tem 45 MB, e o
[M96](../planos/MELHORIAS.md) já mira os ~17 MB de TFLite/OpenCV do 3D que o
[P16](../CATALOGO-PRODUTO.md#p16) acabou de tirar da tela. Medir depois de montar, não
estimar.

### 3.2 Não trazer o Material

Isto é regra, não preferência. O `androidx.compose.material3` traz ripple, elevação, formas e
paleta do Material — exatamente o que a lista PROIBIDO do handoff Vidro exclui. O piloto usa
só `foundation` + `ui`, e a identidade vem dos tokens que já existem.

Na prática: `Box`, `Column`, `Row`, `Text`, `Canvas`, `clickable(indication = null)` com a
mola própria. Nada de `Button`, `Card`, `Surface` do Material.

### 3.3 A camada que falta, e que vale sem Compose nenhum

Antes de desenhar, extrair de `PcView` um estado observável. Em Kotlin:

```kotlin
data class HostUi(
    val uuid: String,
    val nome: String,
    val estado: EstadoHost,       // ONLINE, DORMINDO, OFFLINE
    val pareado: Boolean,
    val jogoAberto: String?,      // null quando nada roda
)

class HomeState {
    var hosts by mutableStateOf(emptyList<HostUi>())
        private set
    fun aoResponder(details: ComputerDetails) { /* mapeia e substitui a entrada */ }
}
```

`HostUi` é **imutável** — é aí que `PcCellSnapshot` se dissolve: a comparação que ele fazia à
mão vira igualdade estrutural de `data class`, e a recomposição usa isso sozinha.

O `ServiceConnection` continua no `PcView`; ele só passa a alimentar `HomeState` em vez de
chamar `pcGridAdapter.notifyDataSetChanged()`.

**Este passo tem valor mesmo se o Compose for abandonado depois.** É o item 2 da ordem
recomendada no ADR.

### 3.4 A troca do `setContentView`

A sonda de GL fica intacta — ela roda antes e chama `completeOnCreate()`. Só a segunda
chamada muda:

```kotlin
// em completeOnCreate(), no lugar de setContentView(R.layout.activity_pc_view)
setContent {
    VidroTema {
        TelaHome(
            estado = homeState,
            aoTocarHost = ::aoTocarHost,       // métodos que já existem em PcView
            aoPedirMais = ::abrirFolhaDoHost,
        )
    }
}
```

`setContent` é extensão de `ComponentActivity`, e `AppCompatActivity` herda dela — a Activity
continua sendo a mesma classe, com o mesmo ciclo de vida e o mesmo serviço vinculado.

### 3.5 A ordem de dentro da tela

Migrar a home inteira de uma vez não é necessário nem recomendável:

1. **A grade primeiro** — `pcFragmentContainer` vira um `ComposeView` dentro do XML atual.
   O resto da tela não muda uma linha. É aqui que `PcCellSnapshot` some e o ganho aparece
   isolado, medível.
2. **O cabeçalho e o rodapé** depois, quando a grade estiver de pé.
3. **A luz** (`VidroLightView`) por último, ou nunca: ela já é `onDraw` próprio e funciona.
   Em Compose vira `Canvas` com o mesmo `RadialGradient`. Boa hora para ligar o
   `setQuality()` que hoje não tem chamador ([P13](../CATALOGO-PRODUTO.md#p13)).
4. **As três variantes de layout viram uma função** com `BoxWithConstraints`: a coluna de
   420 dp aparece acima de um limiar de largura, e o achado do agente do tablet — 720 dp
   cravados vazando em telas de 600–719 dp — se resolve com `min(720.dp, maxWidth)`, que
   em XML exigia `ConstraintLayout` e um ADR próprio.

---

## 4. Os riscos específicos desta tela

| Risco | Por quê | Mitigação |
|---|---|---|
| **Menu de contexto** | 14 `menu.add` presos ao `registerForContextMenu` da `GridView`. Compose não tem menu de contexto do sistema | É o [PE03](../CATALOGO-PRODUTO.md#pe03), que já quer matar esse menu e pôr uma folha Vidro no lugar. **Fazer o PE03 junto com o piloto**, não antes nem depois |
| **Foco por teclado/D-pad** | O Tab S8 Ultra com a capa navega por teclado. `AbsListView` derrama seleção nos filhos — comportamento herdado que já mordeu no P11 | Compose tem foco próprio e mais previsível, mas **precisa ser testado no aparelho com a capa**, não presumido |
| **Sonda de GL** | Rouba o `setContentView` antes do layout | Não encostar. Só a segunda chamada vira `setContent` |
| **Captura de tela sem aparelho** | `tools/screens.mjs` renderiza layouts XML via Robolectric. Compose exige `createComposeRule`, outro caminho | **É o risco mais subestimado.** O loop de conferência visual sem APK é a ferramenta mais útil deste projeto. Portá-lo é pré-requisito do piloto, não consequência |
| **Duas formas de fazer tela** | Durante a convivência, XML e Compose no mesmo app | Aceitável se a fronteira for por TELA. Inaceitável se ficar metade e metade por anos |

---

## 5. O que eu faria, e em que ordem

O piloto só se justifica se produzir uma resposta clara. A pergunta a responder é:
**a home em Compose fica mais fácil de mexer, sem custar fluidez, no Tab S8 Ultra?**

1. Portar `tools/screens.mjs` para capturar um composable. Sem isso, voa-se cego.
2. Extrair `HomeState` — vale sozinho (§3.3).
3. Migrar **só a grade** para `ComposeView` dentro do XML atual, e apagar `PcCellSnapshot`.
4. Medir no aparelho: rolagem da grade com dois hosts (um dormindo), e a home parada durante
   a sondagem — que é o caso que o Q36 consertou e não pode regredir.
5. Decidir com isso na mão: seguir para cabeçalho e rodapé, ou voltar atrás. Passo 3 é
   reversível em um commit.

**Custo do piloto até o passo 4:** dois a três dias de trabalho concentrado, sendo o passo 1
o mais incerto porque não tem precedente no projeto.

**Se seguir para a home inteira:** mais três a quatro dias, e aí a decisão de fazer o resto
da casca (catálogo, cenas, endereço, pareamento — ~7 mil linhas) passa a ser sobre ritmo, não
sobre viabilidade.

## 6. O que não fazer

- **Não migrar Ajustes primeiro.** É `androidx.preference`, não tem equivalente em Compose,
  e seria escrita à mão do zero — e o catálogo de produto ainda vai cortar ~50 das 126
  preferências. Migrar antes disso é reescrever o que vai ser apagado.
- **Não começar pela sessão.** Nunca. Ver a costura A do ADR.
- **Não trazer Material 3** junto, "só para ter os componentes". Ele traz o Material inteiro
  de volta pela porta dos fundos.
