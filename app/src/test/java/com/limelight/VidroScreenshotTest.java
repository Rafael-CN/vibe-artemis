package com.limelight;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;

import com.limelight.DebugInfoActivity;

import static org.junit.Assert.assertNotNull;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Renderiza as telas em PNG sem emulador e sem APK.
 *
 * O problema que isto resolve: até aqui a única forma de ver o desenho era gerar um APK,
 * instalar e olhar — um ciclo de minutos que só o dono do aparelho conseguia fechar. Com
 * o Robolectric em modo de gráficos NATIVO o layout é rasterizado de verdade na JVM, com
 * as fontes empacotadas e os drawables reais, em segundos.
 *
 * O que ele NÃO pega, e é bom saber antes de confiar: o vídeo (SurfaceView desenha numa
 * camada de hardware que não existe aqui), animação, gesto, e qualquer coisa que dependa
 * de dado que só chega pela rede. Serve para conferir composição, medida, cor, contraste
 * e tipografia — que é justamente o que a comparação com o protótipo precisa.
 *
 * Fica inerte no teste normal e só escreve quando pedido:
 *
 * <pre>node tools/screens.mjs</pre>
 */
// ShadowMoonBridge anula o inicializador estático que chama System.loadLibrary — sem
// ele qualquer Activity que toque a ponte nativa derruba a captura com
// UnsatisfiedLinkError, e não há .so nenhum numa JVM de teste.
@Config(sdk = {33}, shadows = {com.limelight.shadows.ShadowMoonBridge.class,
        com.limelight.shadows.ShadowGameManager.class})
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner.class)
public class VidroScreenshotTest {

    /** Onde as imagens saem. O runner lê daqui. */
    private static final String OUT_DIR = "build/vidro-screens";

    @BeforeClass
    public static void suppressInvalidIdLogs() {
        TestLogSuppressor.install();
    }

    // ==================== retrato · 393×852 ====================

    @Test
    @Config(qualifiers = "w393dp-h852dp-port-night-xhdpi")
    public void telasEmRetrato() throws IOException {
        onlyWhenAsked();

        // A Activity de verdade, com o cabeçalho preenchido e a lista montada pelo
        // adapter. Inflar o layout sozinho mostra a casca; isto mostra a tela.
        shootActivity("00-cenas-viva", ProfilesActivity.class, 393, 852);
        // Ajustes só existe de verdade com a Activity de pé: o layout tem um container
        // vazio que o SettingsFragment preenche em runtime. Enquanto a captura era só do
        // layout inflado, esta tela aparecia como um título sobre preto — e era
        // exatamente onde a linha de preferência, a régua, o cabeçalho de seção e a caixa
        // de seleção tinham acabado de ser reescritos, sem nada olhando.
        shootActivity("00-ajustes-viva",
                com.limelight.preferences.StreamSettings.class, 393, 852);
        shootActivity("00-cena-viva", com.limelight.EditProfileActivity.class, 393, 852);

        // A mesma home com o card de antecipação e o pareamento à mostra, que é como
        // ela fica quando há palpite — o estado que mais precisa ser olhado.

        // Peças soltas, no tamanho em que a grade as usa.
        shoot("peca-card-de-app", R.layout.app_grid_item, 180, 230);
        shoot("peca-linha-de-menu", R.layout.vidro_menu_row, 320, 0);
        shoot("10-ajustes", R.layout.activity_stream_settings, 393, 852);
    }

    // ==================== paisagem · 852×393 ====================

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-night-xhdpi")
    public void telasEmPaisagem() throws IOException {
        onlyWhenAsked();

        shoot("03-conectando", R.layout.vidro_connecting, 852, 393, true);
        shootConectandoComCapa("03-conectando-com-capa", 852, 393);
        shoot("05-folha-de-ajustes", R.layout.vidro_session_sheet, 852, 393, true);
        shoot("09-perdemos-o-host", R.layout.vidro_lost, 852, 393, true);
    }

    // ==================== tablet · S8 Ultra em paisagem ====================

    /**
     * 1722x1075 dp é o Tab S8 Ultra real (2960x1848 a ~1,72 de escala). É a tela onde as
     * medidas fixas do desenho de celular aparecem — painel de 352dp, grade de colunas,
     * qualquer altura cravada que a largura da tela desminta.
     */
    @Test
    /*
     * sw1075dp NÃO é decoração: sem ele o Robolectric mantém smallestScreenWidthDp no
     * padrão e o capturador escolhe res/layout/ mesmo pedindo 1722dp de largura — ou
     * seja, o que saía nestas capturas era o layout de CELULAR esticado, e todo defeito
     * de tablet passava batido até chegar no aparelho do dono. w/h dizem o tamanho da
     * janela; quem escolhe a pasta de recurso é o sw.
     */
    @Config(qualifiers = "sw1075dp-w1722dp-h1075dp-land-night-xhdpi")
    public void telasNoTablet() throws IOException {
        onlyWhenAsked();

        shoot("tab-05-folha-de-ajustes", R.layout.vidro_session_sheet, 1722, 1075, true);

        // O pareamento faltava aqui e era exatamente onde o defeito morava: sem uma pasta
        // de tablet ele caía no layout de celular e os quatro dígitos ficavam encostados
        // na borda esquerda. Nasce gone, daí o forceVisible.

        // O índice de Ajustes na geometria do tablet: treze linhas de navegação numa tela
        // de 1722 dp de largura é onde a coluna de celular esticada aparece. Precisa da
        // Activity de pé — o layout sozinho traz só o container vazio.
        shootActivity("tab-00-ajustes-viva",
                com.limelight.preferences.StreamSettings.class, 1722, 1075);
    }

    // ==================== celular alto · S22 Ultra ====================

    /** 460x988 dp — mais estreito e mais alto que o alvo de 393x852 do protótipo. */
    @Test
    @Config(qualifiers = "w460dp-h988dp-port-night-xhdpi")
    public void telasNoS22Ultra() throws IOException {
        onlyWhenAsked();

    }

    // ==================== máquina ====================

    /**
     * Não roda junto com a suíte normal: escrever PNG a cada `gradlew test` encheria o
     * build de artefato que ninguém pediu.
     */
    private static void onlyWhenAsked() {
        Assume.assumeTrue("defina -Dvidro.screens=1 para capturar",
                System.getProperty("vidro.screens") != null);
    }

    /**
     * @param height 0 = deixa a view dizer a altura dela (para peças, não para telas).
     */
    /*
     * A HOME e o CATALOGO nao aparecem mais aqui.
     *
     * Os dois layouts viraram cascas finas com um ComposeView dentro, e um ComposeView
     * inflado sem dono de ciclo de vida estoura na hora -- as capturas falhavam em silencio
     * e sumiam da pasta sem ninguem notar. Quem captura as duas agora e
     * VidroComposeScreenshotTest, que sobe uma ComponentActivity de verdade e por isso tem
     * LifecycleOwner. As duas suites escrevem na MESMA pasta, entao comparar continua sendo
     * abrir dois PNGs.
     */
    private void shoot(String name, int layoutId, int widthDp, int heightDp) throws IOException {
        shoot(name, layoutId, widthDp, heightDp, false);
    }

    /**
     * @param forceVisible sobe as camadas que nascem GONE. As telas de sobreposição —
     *                     ilha, folha, conectando, pareamento — ficam escondidas até o
     *                     código mandar aparecer, então sem isto a captura sairia vazia
     *                     e eu concluiria que a tela não existe.
     */
    /**
     * A espera COM a capa do aplicativo — a metade que a captura sem capa não mostra.
     *
     * A arte de jogo é o pior fundo possível para texto: clara, saturada e cheia de contraste
     * local. Esta captura existe para provar que os três passos continuam legíveis sobre ela,
     * porque é o véu que garante isso e véu é fácil de afrouxar sem perceber.
     *
     * A arte é sintética e não vem do cache: o que se quer ver aqui é o LAYOUT — capa, véu e
     * texto empilhados —, e uma arte de verdade traria a variação dela para dentro de um
     * arquivo que deve mudar só quando o desenho mudar. Um gradiente claro é o caso difícil.
     */
    private void shootConectandoComCapa(String name, int widthDp, int heightDp)
            throws IOException {
        Context base = ApplicationProvider.getApplicationContext();
        Context context = new androidx.appcompat.view.ContextThemeWrapper(base, R.style.AppTheme);

        float density = context.getResources().getDisplayMetrics().density;
        int widthPx = Math.round(widthDp * density);
        int heightPx = Math.round(heightDp * density);

        FrameLayout root = new FrameLayout(context);
        LayoutInflater.from(context).inflate(R.layout.vidro_connecting, root, true);
        revealAll(root);

        android.widget.ImageView capa = root.findViewById(R.id.vidroConnectingCapa);
        assertNotNull("a captura precisa da ImageView da capa", capa);
        capa.setImageBitmap(arteSintetica(300, 400));

        // O texto que só o runtime põe. Sem ele a captura mostra a barra e o "Cancelar"
        // sobre a capa e não prova nada sobre legibilidade — que é o motivo desta captura
        // existir.
        ((android.widget.TextView) root.findViewById(R.id.vidroConnectingApp))
                .setText("Hollow Knight");
        ((android.widget.TextView) root.findViewById(R.id.vidroConnectingStep))
                .setText(context.getString(R.string.vidro_step_talking, "PC-RAFAEL"));

        int widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY);
        root.measure(widthSpec, heightSpec);
        root.layout(0, 0, widthPx, heightPx);

        Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        root.draw(canvas);

        write(name, bitmap, widthPx, heightPx);
        bitmap.recycle();
    }

    /** Um gradiente claro e saturado — o caso difícil para o véu. */
    private static Bitmap arteSintetica(int largura, int altura) {
        Bitmap arte = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888);
        Canvas pincel = new Canvas(arte);
        android.graphics.Paint tinta = new android.graphics.Paint();
        tinta.setShader(new android.graphics.LinearGradient(
                0, 0, largura, altura,
                0xFFF2C46A, 0xFF7E3FA8,
                android.graphics.Shader.TileMode.CLAMP));
        pincel.drawRect(0, 0, largura, altura, tinta);
        return arte;
    }

    private void shoot(String name, int layoutId, int widthDp, int heightDp, boolean forceVisible)
            throws IOException {
        Context base = ApplicationProvider.getApplicationContext();

        // O tema do app, não o Theme_AppCompat: sem ele os tokens Vidro e a Manrope não
        // entram, e a captura mostraria um app que não existe.
        Context context = new androidx.appcompat.view.ContextThemeWrapper(base, R.style.AppTheme);

        float density = context.getResources().getDisplayMetrics().density;
        int widthPx = Math.round(widthDp * density);
        int heightPx = Math.round(heightDp * density);

        FrameLayout root = new FrameLayout(context);
        View view;
        try {
            view = LayoutInflater.from(context).inflate(layoutId, null);
            root.addView(view);
        } catch (android.view.InflateException e) {
            // Layout com raiz <merge> precisa de um pai de verdade.
            LayoutInflater.from(context).inflate(layoutId, root, true);
        }

        if (forceVisible) {
            revealAll(root);
        }

        int widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY);
        int heightSpec = heightPx > 0
                ? View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
                : View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

        root.measure(widthSpec, heightSpec);
        int finalHeight = heightPx > 0 ? heightPx : Math.max(1, root.getMeasuredHeight());
        root.layout(0, 0, widthPx, finalHeight);

        Bitmap bitmap = Bitmap.createBitmap(widthPx, finalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // O void por baixo. Sem isto o PNG sai com alpha zero onde a view não pinta, e
        // qualquer visualizador mostra branco — que é exatamente a cor que este app
        // nunca tem, e me faria caçar um bug que não existe.
        canvas.drawColor(context.getResources().getColor(R.color.vidro_void));

        root.draw(canvas);

        File dir = new File(OUT_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("não consegui criar " + dir.getAbsolutePath());
        }

        File out = new File(dir, name + ".png");
        try (FileOutputStream fos = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        bitmap.recycle();

        System.out.println("[vidro-screens] " + out.getPath()
                + "  " + widthPx + "x" + finalHeight);
    }


    /**
     * Renderiza uma Activity de verdade: onCreate, onStart, onResume, adapter montado.
     *
     * É a diferença entre ver a casca e ver a tela. O layout inflado mostra caixas vazias
     * onde o texto só chega em runtime — cabeçalho, blocos da ilha, nome do host.
     */
    private <T extends android.app.Activity> void shootActivity(
            String name, Class<T> activityClass, int widthDp, int heightDp) throws IOException {
        ActivityController<T> controller = null;
        try {
            controller = Robolectric.buildActivity(activityClass).setup();
            T activity = controller.get();

            View decor = activity.getWindow().getDecorView();

            float density = activity.getResources().getDisplayMetrics().density;
            int widthPx = Math.round(widthDp * density);
            int heightPx = Math.round(heightDp * density);

            decor.measure(
                    View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY));
            decor.layout(0, 0, widthPx, heightPx);

            Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(activity.getResources().getColor(R.color.vidro_void));
            decor.draw(canvas);

            write(name, bitmap, widthPx, heightPx);
            bitmap.recycle();
        } catch (Throwable e) {
            // Throwable, e não RuntimeException: UnsatisfiedLinkError é Error, escapava
            // daqui e levava junto todas as telas seguintes da mesma tanda.
            // Uma Activity que não sobe não invalida a captura das outras, mas engolir a
            // causa é pior que não capturar: numa passada anterior um ClassCastException
            // de verdade virou uma imagem preta que eu li como "tela vazia". Agora ela
            // grita, com a pilha, e o runner destaca a linha.
            System.out.println("[vidro-screens] !! FALHOU " + name + ": " + e);
            for (StackTraceElement frame : e.getStackTrace()) {
                if (frame.getClassName().startsWith("com.limelight")) {
                    System.out.println("[vidro-screens] !!   em " + frame);
                }
            }
        } finally {
            if (controller != null) {
                try {
                    controller.close();
                } catch (RuntimeException ignored) {
                    // Já reportamos o que importava.
                }
            }
        }
    }

    private void write(String name, Bitmap bitmap, int w, int h) throws IOException {
        File dir = new File(OUT_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("não consegui criar " + dir.getAbsolutePath());
        }

        File out = new File(dir, name + ".png");
        try (FileOutputStream fos = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }

        System.out.println("[vidro-screens] " + out.getPath() + "  " + w + "x" + h);
    }

    /** Sobe tudo que nasce GONE, recursivamente. */
    private static void revealAll(View view) {
        if (view.getVisibility() == View.GONE) {
            view.setVisibility(View.VISIBLE);
        }

        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                revealAll(group.getChildAt(i));
            }
        }
    }
}
