package com.limelight;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.compose.ui.platform.ComposeView;

import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.PairingManager;
import com.limelight.ui.compose.EstadoDaHome;
import com.limelight.ui.compose.EstadoDoHostNoCatalogo;
import com.limelight.ui.compose.PonteDoCatalogo;
import com.limelight.ui.compose.PonteDoEndereco;
import com.limelight.ui.compose.PonteDoPareamento;
import com.limelight.ui.compose.Historico;
import com.limelight.ui.compose.PonteDaHome;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Captura de tela para o que é desenhado em Compose.
 *
 * O piloto do Compose (docs/guides/compose-piloto-home.md) apontou isto como o risco mais
 * subestimado da migração, e ele é real: `tools/screens.mjs` infla layout XML e desenha a
 * View. Um composable não é uma View que se infla — ele precisa de uma Activity com ciclo de
 * vida, porque `ComposeView` só compõe depois de anexado a uma janela cujo `LifecycleOwner`
 * chegou a CREATED. Sem isto o loop de conferência visual sem APK — a ferramenta mais útil
 * deste projeto — simplesmente não alcançaria nada em Compose, e a migração andaria cega.
 *
 * A saída vai para o mesmo diretório das outras capturas, com o mesmo nome de arquivo, então
 * comparar Compose com XML é abrir dois PNGs lado a lado.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class})
public class VidroComposeScreenshotTest {

    private static final String OUT_DIR = "build/vidro-screens";

    /** Celular em pé — a mesma geometria que o S22 Ultra usa nas outras capturas. */
    @Test
    @Config(qualifiers = "w393dp-h852dp-port-night-xhdpi")
    public void gradeNoCelular() throws IOException {
        shootGrade("compose-lista-de-lugares", 393, 520);
    }

    /**
     * A faixa que a lista recebe no Tab S8 Ultra deitado.
     *
     * 1302 dp é a largura que sobra para o conteúdo depois da coluna de moldura de 420 dp —
     * é o número que o layout de tablet do catálogo documenta. Aqui ele não escolhe pasta
     * nenhuma: é a largura de verdade que chega ao `BoxWithConstraints`, que é justamente o
     * ponto do piloto.
     */
    @Test
    @Config(qualifiers = "sw1075dp-w1722dp-h1075dp-land-night-xhdpi")
    public void gradeNoTablet() throws IOException {
        shootGrade("compose-tab-lista-de-lugares", 1302, 620);
    }

    /** A home inteira no celular. */
    @Test
    @Config(qualifiers = "w393dp-h852dp-port-night-xhdpi")
    public void homeNoCelular() throws IOException {
        shoot("compose-home", 393, 852, false);
    }

    /** A home no Tab S8 Ultra deitado: coluna de moldura à esquerda, lista à direita. */
    @Test
    @Config(qualifiers = "sw1075dp-w1722dp-h1075dp-land-night-xhdpi")
    public void homeNoTablet() throws IOException {
        shoot("compose-tab-home", 1722, 1075, false);
    }

    /** A home sem host nenhum: o estado que explica por que ninguém respondeu (P05). */
    @Test
    @Config(qualifiers = "w393dp-h852dp-port-night-xhdpi")
    public void homeSemLugares() throws IOException {
        shoot("compose-home-vazia", 393, 852, true);
    }

    /** O catalogo no celular. */
    @Test
    @Config(qualifiers = "w393dp-h852dp-port-night-xhdpi")
    public void catalogoNoCelular() throws IOException {
        shootCatalogo("compose-catalogo", 393, 852, EstadoDoHostNoCatalogo.NORMAL);
    }

    /** O catalogo no Tab S8 Ultra deitado, a tela marcada como "toda zoada". */
    @Test
    @Config(qualifiers = "sw1075dp-w1722dp-h1075dp-land-night-xhdpi")
    public void catalogoNoTablet() throws IOException {
        shootCatalogo("compose-tab-catalogo", 1722, 1075, EstadoDoHostNoCatalogo.NORMAL);
    }

    /** O P04: o host dormiu enquanto ele escolhia. A tela fica, e a faixa explica. */
    @Test
    @Config(qualifiers = "w393dp-h852dp-port-night-xhdpi")
    public void catalogoComHostDormindo() throws IOException {
        shootCatalogo("compose-catalogo-dormindo", 393, 852, EstadoDoHostNoCatalogo.DORMINDO);
    }

    /** O endereço, com a causa do erro dentro da tela (P03). */
    @Test
    @Config(qualifiers = "w393dp-h852dp-port-night-xhdpi")
    public void enderecoComErro() throws IOException {
        shootEndereco("compose-endereco-erro", R.string.addpc_unknown_host);
    }

    /** O endereço em repouso, com os já tentados. */
    @Test
    @Config(qualifiers = "w393dp-h852dp-port-night-xhdpi")
    public void endereco() throws IOException {
        shootEndereco("compose-endereco", 0);
    }

    /** O passo 2: os quatro digitos, com a pagina do host. */
    @Test
    @Config(qualifiers = "w393dp-h852dp-port-night-xhdpi")
    public void pareamentoCodigo() throws IOException {
        shootPareamento("compose-pareamento-codigo", PonteDoPareamento.passoCodigo());
    }

    /** O P02: o codigo recusado, sem derrubar a tela. */
    @Test
    @Config(qualifiers = "w393dp-h852dp-port-night-xhdpi")
    public void pareamentoRecusado() throws IOException {
        shootPareamento("compose-pareamento-recusado", PonteDoPareamento.passoRecusado());
    }

    /** O passo 3, que o prototipo desenha e o app pulava. */
    @Test
    @Config(qualifiers = "w393dp-h852dp-port-night-xhdpi")
    public void pareamentoPronto() throws IOException {
        shootPareamento("compose-pareamento-pronto", PonteDoPareamento.passoPronto());
    }

    private void shootPareamento(String nome, com.limelight.ui.compose.PassoDoPareamento passo)
            throws IOException {
        ActivityController<ComponentActivity> controller = null;
        try {
            controller = Robolectric.buildActivity(ComponentActivity.class).setup();
            ComponentActivity activity = controller.get();

            // O pareamento e uma SOBREPOSICAO: o veu dele e translucido de proposito, e
            // por baixo dele mora a home. A ComponentActivity de teste nasce com fundo
            // BRANCO, e 55% de quase-preto sobre branco da cinza medio -- a captura mostrava
            // um cinza que nao existe no app. Pintar o fundo da janela de void poe a captura
            // no contexto real.
            activity.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                    activity.getResources().getColor(R.color.vidro_void)));

            ComposeView view = new ComposeView(activity);
            PonteDoPareamento.montar(view, passo, "PC-RAFAEL", "4827",
                    "https://192.168.0.12:47990",
                    () -> null, () -> null, () -> null, () -> null);
            activity.setContentView(view);
            avancar(activity);

            View decor = activity.getWindow().getDecorView();
            float density = activity.getResources().getDisplayMetrics().density;
            int widthPx = Math.round(393 * density);
            int heightPx = Math.round(852 * density);

            decor.measure(
                    View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY));
            decor.layout(0, 0, widthPx, heightPx);
            avancar(activity);

            Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(activity.getResources().getColor(R.color.vidro_void));
            decor.draw(canvas);

            write(nome, bitmap, widthPx, heightPx);
            bitmap.recycle();
        } catch (Throwable e) {
            System.out.println("[vidro-screens] !! FALHOU " + nome + ": " + e);
            throw new AssertionError("captura de composable falhou: " + nome, e);
        } finally {
            if (controller != null) {
                try {
                    controller.close();
                } catch (RuntimeException ignored) {
                    // Fechar a Activity nao e o objetivo da captura.
                }
            }
        }
    }

    /** O indice de Ajustes: onze categorias, sem cartao e sem regua. */
    @Test
    @Config(qualifiers = "w393dp-h852dp-port-night-xhdpi")
    public void ajustesIndice() throws IOException {
        shootAjustes("compose-ajustes-indice", 393, 852, null);
    }

    /** Uma categoria aberta: interruptor, chips, deslizante e porta na mesma tela. */
    @Test
    @Config(qualifiers = "w393dp-h852dp-port-night-xhdpi")
    public void ajustesVideo() throws IOException {
        shootAjustes("compose-ajustes-video", 393, 852, "category_video_settings");
    }

    /** O tablet deitado, que e o aparelho do dono: teto de leitura e moldura centrada. */
    @Test
    @Config(qualifiers = "w1722dp-h1075dp-land-night-xhdpi")
    public void ajustesTablet() throws IOException {
        shootAjustes("compose-tab-ajustes-video", 1722, 1075, "category_video_settings");
    }

    /**
     * O editor de cena — o PE06.
     *
     * Quatro escolhas e um nome, no lugar das 111 preferencias que a cena guardava. A
     * captura usa preferencias reais do Robolectric porque e assim que a ponte le.
     */
    @Test
    @Config(qualifiers = "w393dp-h852dp-port-night-xhdpi")
    public void editorDeCena() throws IOException {
        shootComposable("compose-editor-de-cena", 393, 852, (activity, view) ->
                com.limelight.ui.compose.PonteDoEditorDeCena.montar(
                        view,
                        androidx.preference.PreferenceManager
                                .getDefaultSharedPreferences(activity),
                        "Jogo a noite", false,
                        () -> null, n -> null, () -> null, () -> null)
        );
    }

    private void shootAjustes(String nome, int widthDp, int heightDp, String categoria)
            throws IOException {
        shootComposable(nome, widthDp, heightDp, (activity, view) ->
                com.limelight.ui.compose.PonteDeAjustes.montar(
                        view, categoria, () -> null, c -> null));
    }

    /** O molde comum das capturas de composable: monta, mede, desenha, grava. */
    private void shootComposable(String nome, int widthDp, int heightDp, Montagem montagem)
            throws IOException {
        ActivityController<ComponentActivity> controller = null;
        try {
            controller = Robolectric.buildActivity(ComponentActivity.class).setup();
            ComponentActivity activity = controller.get();

            ComposeView view = new ComposeView(activity);
            montagem.montar(activity, view);
            activity.setContentView(view);
            avancar(activity);

            View decor = activity.getWindow().getDecorView();
            float density = activity.getResources().getDisplayMetrics().density;
            int widthPx = Math.round(widthDp * density);
            int heightPx = Math.round(heightDp * density);

            decor.measure(
                    View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY));
            decor.layout(0, 0, widthPx, heightPx);
            avancar(activity);

            Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(activity.getResources().getColor(R.color.vidro_void));
            decor.draw(canvas);

            write(nome, bitmap, widthPx, heightPx);
            bitmap.recycle();
        } catch (Throwable e) {
            System.out.println("[vidro-screens] !! FALHOU " + nome + ": " + e);
            throw new AssertionError("captura de composable falhou: " + nome, e);
        } finally {
            if (controller != null) {
                try {
                    controller.close();
                } catch (RuntimeException ignored) {
                    // Fechar a Activity nao e o objetivo da captura.
                }
            }
        }
    }

    /** O que muda entre uma captura e outra: quem monta o conteudo na ComposeView. */
    private interface Montagem {
        void montar(ComponentActivity activity, ComposeView view);
    }

    private void shootEndereco(String nome, int erro) throws IOException {
        ActivityController<ComponentActivity> controller = null;
        try {
            controller = Robolectric.buildActivity(ComponentActivity.class).setup();
            ComponentActivity activity = controller.get();

            ComposeView view = new ComposeView(activity);
            PonteDoEndereco.montar(view, "192.168.0.", erro, false,
                    java.util.Arrays.asList("192.168.0.12", "meu.host:47990"),
                    t -> null, () -> null, () -> null, e -> null);
            activity.setContentView(view);
            avancar(activity);

            View decor = activity.getWindow().getDecorView();
            float density = activity.getResources().getDisplayMetrics().density;
            int widthPx = Math.round(393 * density);
            int heightPx = Math.round(852 * density);

            decor.measure(
                    View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY));
            decor.layout(0, 0, widthPx, heightPx);
            avancar(activity);

            Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(activity.getResources().getColor(R.color.vidro_void));
            decor.draw(canvas);

            write(nome, bitmap, widthPx, heightPx);
            bitmap.recycle();
        } catch (Throwable e) {
            System.out.println("[vidro-screens] !! FALHOU " + nome + ": " + e);
            throw new AssertionError("captura de composable falhou: " + nome, e);
        } finally {
            if (controller != null) {
                try {
                    controller.close();
                } catch (RuntimeException ignored) {
                    // Fechar a Activity nao e o objetivo da captura.
                }
            }
        }
    }

    private void shootCatalogo(String nome, int widthDp, int heightDp,
                               EstadoDoHostNoCatalogo estadoDoHost) throws IOException {
        ActivityController<ComponentActivity> controller = null;
        try {
            controller = Robolectric.buildActivity(ComponentActivity.class).setup();
            ComponentActivity activity = controller.get();

            // Um adapter de mentira, so para a grade ter o que desenhar. A celula de verdade
            // carrega box art pela rede; aqui o que se confere e a MOLDURA -- cabecalho,
            // contagem, faixa e arranjo --, que e onde os defeitos estavam.
            final int quantos = 8;
            android.widget.BaseAdapter falso = new android.widget.BaseAdapter() {
                @Override public int getCount() { return quantos; }
                @Override public Object getItem(int i) { return null; }
                @Override public long getItemId(int i) { return i; }
                @Override public View getView(int i, View reusar, android.view.ViewGroup pai) {
                    View v = reusar != null ? reusar
                            : android.view.LayoutInflater.from(pai.getContext())
                                    .inflate(R.layout.app_grid_item, pai, false);
                    android.widget.TextView t = v.findViewById(R.id.grid_text);
                    if (t != null) {
                        t.setText("App " + (i + 1));
                    }
                    return v;
                }
            };

            ComposeView view = new ComposeView(activity);
            PonteDoCatalogo.montar(view, "PC-RAFAEL", quantos, false,
                    estadoDoHost == EstadoDoHostNoCatalogo.NORMAL ? "Desktop" : null,
                    estadoDoHost,
                    () -> null, () -> null, () -> null, () -> null, () -> null,
                    falso, i -> null, i -> null, (i, v) -> null);
            activity.setContentView(view);
            avancar(activity);

            View decor = activity.getWindow().getDecorView();
            float density = activity.getResources().getDisplayMetrics().density;
            int widthPx = Math.round(widthDp * density);
            int heightPx = Math.round(heightDp * density);

            decor.measure(
                    View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY));
            decor.layout(0, 0, widthPx, heightPx);
            avancar(activity);

            Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(activity.getResources().getColor(R.color.vidro_void));
            decor.draw(canvas);

            write(nome, bitmap, widthPx, heightPx);
            bitmap.recycle();
        } catch (Throwable e) {
            System.out.println("[vidro-screens] !! FALHOU " + nome + ": " + e);
            for (StackTraceElement frame : e.getStackTrace()) {
                if (frame.getClassName().startsWith("com.limelight")
                        || frame.getClassName().startsWith("androidx.compose")) {
                    System.out.println("[vidro-screens] !!   em " + frame);
                }
            }
            throw new AssertionError("captura de composable falhou: " + nome, e);
        } finally {
            if (controller != null) {
                try {
                    controller.close();
                } catch (RuntimeException ignored) {
                    // Fechar a Activity nao e o objetivo da captura.
                }
            }
        }
    }

    private void shootGrade(String nome, int widthDp, int heightDp) throws IOException {
        ActivityController<ComponentActivity> controller = null;
        try {
            controller = Robolectric.buildActivity(ComponentActivity.class).setup();
            ComponentActivity activity = controller.get();

            ComposeView view = new ComposeView(activity);
            PonteDaHome.montar(view, hostsDeExemplo(), h -> null, h -> null);
            activity.setContentView(view);

            // A composição acontece numa mensagem posta na thread principal, então sem
            // avançar o looper o desenho sairia antes de existir conteúdo — um PNG do void.
            //
            // Mas avançar com idle() TRAVA: a sonda gira numa animação infinita, e uma
            // animação infinita nunca esvazia a fila de quadros. idle() drena até a fila
            // acabar, e ela não acaba. Avançar por tempo é o que funciona — e este é um
            // achado do piloto que vale para toda captura de Compose com animação em laço.
            avancar(activity);

            View decor = activity.getWindow().getDecorView();

            float density = activity.getResources().getDisplayMetrics().density;
            int widthPx = Math.round(widthDp * density);
            int heightPx = Math.round(heightDp * density);

            decor.measure(
                    View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY));
            decor.layout(0, 0, widthPx, heightPx);
            avancar(activity);

            Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(activity.getResources().getColor(R.color.vidro_void));
            decor.draw(canvas);

            write(nome, bitmap, widthPx, heightPx);
            bitmap.recycle();
        } catch (Throwable e) {
            // Mesma regra da captura de XML: gritar com a pilha em vez de gravar um PNG
            // preto que eu leria como "tela vazia".
            System.out.println("[vidro-screens] !! FALHOU " + nome + ": " + e);
            for (StackTraceElement frame : e.getStackTrace()) {
                if (frame.getClassName().startsWith("com.limelight")
                        || frame.getClassName().startsWith("androidx.compose")) {
                    System.out.println("[vidro-screens] !!   em " + frame);
                }
            }
            throw new AssertionError("captura de composable falhou: " + nome, e);
        } finally {
            if (controller != null) {
                try {
                    controller.close();
                } catch (RuntimeException ignored) {
                    // Fechar a Activity não é o objetivo da captura.
                }
            }
        }
    }

    private void shoot(String nome, int widthDp, int heightDp, boolean vazia) throws IOException {
        ActivityController<ComponentActivity> controller = null;
        try {
            controller = Robolectric.buildActivity(ComponentActivity.class).setup();
            ComponentActivity activity = controller.get();

            EstadoDaHome estado = vazia ? new EstadoDaHome() : hostsDeExemplo();
            java.util.List<Historico.Entrada> continuar = vazia
                    ? java.util.Collections.emptyList()
                    : java.util.Arrays.asList(
                            new Historico.Entrada("1", 12345, "Desktop", "PC-RAFAEL"),
                            new Historico.Entrada("1", 881, "Hollow Knight", "PC-RAFAEL"));

            ComposeView view = new ComposeView(activity);
            PonteDaHome.montarHome(view, estado, continuar,
                    h -> null, h -> null, e -> null,
                    () -> null, () -> null, () -> null,
                    activity.getString(R.string.vidro_scenes),
                    vazia ? R.string.vidro_why_host_off : 0,
                    h -> null, h -> null, h -> null, h -> null, h -> null, h -> null);
            activity.setContentView(view);
            avancar(activity);

            View decor = activity.getWindow().getDecorView();
            float density = activity.getResources().getDisplayMetrics().density;
            int widthPx = Math.round(widthDp * density);
            int heightPx = Math.round(heightDp * density);

            decor.measure(
                    View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY));
            decor.layout(0, 0, widthPx, heightPx);
            avancar(activity);

            Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(activity.getResources().getColor(R.color.vidro_void));
            decor.draw(canvas);

            write(nome, bitmap, widthPx, heightPx);
            bitmap.recycle();
        } catch (Throwable e) {
            System.out.println("[vidro-screens] !! FALHOU " + nome + ": " + e);
            for (StackTraceElement frame : e.getStackTrace()) {
                if (frame.getClassName().startsWith("com.limelight")
                        || frame.getClassName().startsWith("androidx.compose")) {
                    System.out.println("[vidro-screens] !!   em " + frame);
                }
            }
            throw new AssertionError("captura de composable falhou: " + nome, e);
        } finally {
            if (controller != null) {
                try {
                    controller.close();
                } catch (RuntimeException ignored) {
                    // Fechar a Activity não é o objetivo da captura.
                }
            }
        }
    }

    /** Avança o relógio o bastante para a composição acontecer, sem esperar o fim dela. */
    private static void avancar(ComponentActivity activity) {
        for (int i = 0; i < 4; i++) {
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(16));
        }
    }

    /**
     * Os cinco estados que a linha sabe dizer, para a captura mostrar todos de uma vez.
     *
     * Os tempos saem do MESMO relógio que o app usa. Semeá-los com 0 e 9500 tornava a
     * captura instável: o composable agenda uma reavaliação 500 ms depois de encontrar uma
     * sonda em curso, e essa reavaliação usa SystemClock — que está a horas de distância de
     * um 9500 inventado. O prazo vencia no meio da captura, e a linha que devia estar
     * girando saía escrita "sem resposta". Não era defeito do código: era eu misturando dois
     * relógios.
     */
    private static EstadoDaHome hostsDeExemplo() {
        EstadoDaHome estado = new EstadoDaHome();
        final long agora = android.os.SystemClock.uptimeMillis();
        estado.aoResponder(host("1", "PC-RAFAEL", ComputerDetails.State.ONLINE, true, null, 12345), agora);
        estado.aoResponder(host("2", "SALA", ComputerDetails.State.OFFLINE, true, "aa:bb:cc:dd:ee:ff", 0), agora);
        estado.aoResponder(host("3", "NOTEBOOK", ComputerDetails.State.ONLINE, false, null, 0), agora);
        estado.aoResponder(host("4", "REMOTO", ComputerDetails.State.UNKNOWN, true, null, 0), agora - 60_000L);
        estado.aoResponder(host("5", "PORAO", ComputerDetails.State.OFFLINE, true, null, 0), agora);
        // REMOTO entrou em UNKNOWN um minuto atrás: o prazo já venceu e ele diz "sem
        // resposta" em vez de girar para sempre. CHECANDO acabou de entrar e ainda gira. A
        // captura mostra os dois estados de espera lado a lado, que é o par que o prazo de
        // sonda existe para separar.
        estado.aoResponder(host("6", "CHECANDO", ComputerDetails.State.UNKNOWN, true, null, 0), agora);

        // Sem esta linha o REMOTO tambem giraria: aoResponder registra o inicio da sonda no
        // MESMO instante que passa como agora, entao responder no passado nao envelhece
        // nada. Quem envelhece e reavaliar o prazo olhando do presente.
        estado.reavaliarPrazos(agora);
        return estado;
    }

    private static ComputerDetails host(String uuid, String nome, ComputerDetails.State s,
                                        boolean pareado, String mac, int jogoAberto) {
        ComputerDetails d = new ComputerDetails();
        d.uuid = uuid;
        d.name = nome;
        d.state = s;
        d.pairState = pareado ? PairingManager.PairState.PAIRED : PairingManager.PairState.NOT_PAIRED;
        d.macAddress = mac;
        d.runningGameId = jogoAberto;
        d.manualAddress = new ComputerDetails.AddressTuple("192.168.0." + uuid, 47989);
        return d;
    }

    private static void write(String name, Bitmap bitmap, int w, int h) throws IOException {
        File dir = new File(OUT_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("não consegui criar " + dir.getAbsolutePath());
        }
        File out = new File(dir, name + ".png");
        try (FileOutputStream fos = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        System.out.println("[vidro-screens] " + out.getName() + "  " + w + "x" + h);
    }
}
