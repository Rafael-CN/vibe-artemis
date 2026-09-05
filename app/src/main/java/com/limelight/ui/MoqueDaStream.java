package com.limelight.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;

/**
 * Preenche a {@code Surface} da sessão de preto, sem host nenhum.
 *
 * <h2>Por que isto existe</h2>
 *
 * A sessão é a única parte deste app que nenhuma ferramenta do repositório conseguia
 * ver: {@code tools/screens.mjs} não desenha {@code SurfaceView} (é Robolectric, sem
 * camada de hardware), e um emulador sem host não tem o que decodificar. Sem isto, a
 * aba, o menu e a barra de texto só eram conferidos quando alguém gravava a tela de um
 * aparelho de verdade — tarde demais para pegar regressão.
 *
 * <p>O pedido é literal: uma superfície preta. Não é vídeo, não prova decodificação,
 * HDR ou latência — só dá à camada de interface um fundo estável para se apoiar.
 *
 * <h2>Como ligar</h2>
 *
 * <pre>adb shell am start -n com.limelight.noirdebug/com.limelight.Game --ez MoqueDeStream true</pre>
 *
 * Só em compilação de depuração — {@code Game} confere {@code BuildConfig.DEBUG} antes
 * de olhar para o extra (ver {@code Game.emModoDeMoque()}).
 */
public class MoqueDaStream {

    // Uma SurfaceView costuma ter 2-3 buffers; um único drawColor não alcança os que
    // ainda não vieram à tona. Repintar de vez em quando, em vez de uma vez só,
    // garante que todo buffer receba preto sem apostar em quantos existem.
    private static final long INTERVALO_MS = 500;

    private final Handler relogio = new Handler(Looper.getMainLooper());
    private Surface superficie;
    private boolean rodando;

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
            pintarDePreto();
            relogio.postDelayed(this, INTERVALO_MS);
        }
    };

    private void pintarDePreto() {
        Surface s = superficie;
        if (s == null || !s.isValid()) {
            return;
        }

        Canvas c;
        try {
            c = s.lockCanvas(null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // A superfície pode ter sido tomada por outro produtor, ou destruída entre
            // a checagem e a chamada. Parar é o desfecho certo: não há o que desenhar.
            parar();
            return;
        }
        if (c == null) {
            return;
        }

        try {
            c.drawColor(Color.BLACK);
        } finally {
            try {
                s.unlockCanvasAndPost(c);
            } catch (IllegalArgumentException | IllegalStateException ignored) {
                // Mesma corrida do lockCanvas. Não há o que salvar aqui.
            }
        }
    }
}
