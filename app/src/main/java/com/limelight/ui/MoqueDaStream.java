package com.limelight.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;

/**
 * Um vídeo falso, para poder testar a sessão sem um host.
 *
 * <h2>Por que isto existe</h2>
 *
 * A sessão é onde o app passa 95% do tempo e é a única parte que nenhuma ferramenta deste
 * repositório conseguia ver. {@code tools/screens.mjs} não a alcança — a {@code SurfaceView}
 * desenha numa camada de hardware que o Robolectric não tem —, e um emulador sem host não tem
 * o que decodificar. O resultado prático é que a gaveta, o menu, a paleta e a moldura só eram
 * conferidos quando o dono do fork gravava a tela do aparelho dele.
 *
 * <p>Aqui a superfície recebe um desenho nosso, num laço de ~30 quadros por segundo. Não é
 * vídeo: é uma imagem que se mexe o bastante para provar que o que está por cima dela está
 * certo — contraste, posição, e se alguma coisa desapareceu.
 *
 * <h2>O que ele NÃO prova</h2>
 *
 * Latência, decodificação, HDR, e qualquer caminho que passe pelo {@code MoonBridge}. Isto é
 * uma tela de teste para a INTERFACE da sessão, e chamá-lo de outra coisa seria a mentira
 * mais cara possível num app de streaming.
 *
 * <h2>Como ligar</h2>
 *
 * <pre>adb shell am start -n com.limelight.noirdebug/com.limelight.Game --ez MoqueDeStream true</pre>
 *
 * Só em compilação de depuração — {@code Game} confere {@code BuildConfig.DEBUG} antes de
 * olhar para o extra.
 */
public class MoqueDaStream {

    /** ~30 quadros por segundo. Suficiente para o olho e barato para o emulador. */
    private static final long INTERVALO_MS = 33;

    private final Handler relogio = new Handler(Looper.getMainLooper());
    private Surface superficie;
    private boolean rodando;
    private int quadro;

    private final Paint tinta = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF caixa = new RectF();

    public MoqueDaStream() {
        texto.setColor(0xFFE8EAF0);
        texto.setTextAlign(Paint.Align.CENTER);
    }

    public void comecar(Surface superficie) {
        if (superficie == null || !superficie.isValid()) {
            return;
        }
        this.superficie = superficie;
        if (rodando) {
            return;
        }
        rodando = true;
        relogio.post(passo);
    }

    public void parar() {
        rodando = false;
        relogio.removeCallbacks(passo);
        superficie = null;
    }

    private final Runnable passo = new Runnable() {
        @Override
        public void run() {
            if (!rodando) {
                return;
            }
            desenhar();
            relogio.postDelayed(this, INTERVALO_MS);
        }
    };

    private void desenhar() {
        Surface s = superficie;
        if (s == null || !s.isValid()) {
            return;
        }

        Canvas c = null;
        try {
            c = s.lockCanvas(null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // A superfície pode ter sido tomada por outro produtor, ou destruída entre a
            // checagem e a chamada. Parar é o desfecho certo: não há o que desenhar.
            parar();
            return;
        }
        if (c == null) {
            return;
        }

        try {
            int w = c.getWidth();
            int h = c.getHeight();
            quadro++;

            // Um fundo que se move devagar — o bastante para provar que há quadro novo, e
            // longe do preto para que qualquer coisa desenhada por cima tenha de se defender
            // num fundo com luminância de verdade.
            float fase = (quadro % 240) / 240f;
            tinta.setShader(new LinearGradient(0, 0, w, h,
                    new int[] {0xFF1B3A6B, 0xFF3A2A63, 0xFF14495C},
                    new float[] {0f, 0.5f + 0.3f * (float) Math.sin(fase * Math.PI * 2), 1f},
                    Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, tinta);
            tinta.setShader(null);

            // Uma grade fina: é ela que denuncia esticamento e proporção errada, que é
            // metade do que se quer olhar numa tela de sessão.
            tinta.setColor(0x22FFFFFF);
            tinta.setStrokeWidth(1f);
            for (int x = 0; x < w; x += w / 16) {
                c.drawLine(x, 0, x, h, tinta);
            }
            for (int y = 0; y < h; y += h / 9) {
                c.drawLine(0, y, w, y, tinta);
            }

            // Um alvo no centro exato: qualquer deslocamento do vídeo fica óbvio.
            tinta.setColor(0x55FFFFFF);
            tinta.setStyle(Paint.Style.STROKE);
            tinta.setStrokeWidth(Math.max(2f, w / 400f));
            c.drawCircle(w / 2f, h / 2f, Math.min(w, h) / 6f, tinta);
            c.drawLine(w / 2f, h / 2f - 30, w / 2f, h / 2f + 30, tinta);
            c.drawLine(w / 2f - 30, h / 2f, w / 2f + 30, h / 2f, tinta);
            tinta.setStyle(Paint.Style.FILL);

            // As quatro quinas nomeadas: se uma sumir, o recorte comeu a borda.
            texto.setTextSize(Math.max(18f, w / 40f));
            texto.setColor(0xCCFFFFFF);
            float m = texto.getTextSize() * 1.4f;
            c.drawText("↖ TOPO-ESQ", m * 3.2f, m, texto);
            c.drawText("TOPO-DIR ↗", w - m * 3.2f, m, texto);
            c.drawText("↙ BASE-ESQ", m * 3.2f, h - m * 0.4f, texto);
            c.drawText("BASE-DIR ↘", w - m * 3.2f, h - m * 0.4f, texto);

            // O carimbo do meio, com a medida real da superfície e o contador de quadros.
            texto.setTextSize(Math.max(26f, w / 22f));
            caixa.set(w / 2f - w / 3f, h / 2f + Math.min(w, h) / 6f + 20,
                    w / 2f + w / 3f, h / 2f + Math.min(w, h) / 6f + 20 + texto.getTextSize() * 3.4f);
            tinta.setColor(0x66000000);
            c.drawRoundRect(caixa, 18, 18, tinta);

            texto.setColor(0xFFFFFFFF);
            float linha = caixa.top + texto.getTextSize() * 1.3f;
            c.drawText("MOQUE DE STREAM", w / 2f, linha, texto);
            texto.setTextSize(Math.max(18f, w / 34f));
            texto.setColor(0xCCD8DCE6);
            c.drawText(w + " x " + h + "  ·  quadro " + quadro, w / 2f,
                    linha + texto.getTextSize() * 1.6f, texto);
        } finally {
            try {
                s.unlockCanvasAndPost(c);
            } catch (IllegalArgumentException | IllegalStateException ignored) {
                // Mesma corrida do lockCanvas. Não há o que salvar aqui.
            }
        }
    }

    /** Para quem quiser pintar o fundo antes de a superfície existir. */
    public static int corDeFundo() {
        return Color.rgb(0x1B, 0x3A, 0x6B);
    }
}
