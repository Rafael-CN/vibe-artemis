package com.limelight;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.limelight.binding.input.GameInputDevice;
import com.limelight.binding.input.KeyboardTranslator;

/**
 * A guia da sessão (Plano B / "Console" — B-CONSOLE.md §F2).
 * <p>
 * Substitui a lista do {@code AlertDialog} que o Quick Menu usava até aqui por uma
 * barra horizontal de blocos, dentro do MESMO tipo de janela: um {@link Dialog} comum,
 * porque é a única classe de janela que comprovadamente aparece por cima da
 * {@code SurfaceView} no S22 Ultra do dono do fork (overlay de View na mesma janela da
 * Activity, sobre vídeo vivo, simplesmente não desenha ali — já custou quatro entregas
 * quebradas antes desta).
 * <p>
 * Cada bloco chama a ação que o app já tem — nenhuma ação nova nasce aqui. O bloco
 * "Picture-in-picture" do mockup não existe nesta implementação porque não há, hoje,
 * uma ação de PiP manual equivalente (só entrada automática ao sair do app); ver
 * docs/planos/console/relatorios/F2-implementacao.md.
 */
public class GameMenu implements Game.GameMenuCallbacks {

    public static final long KEY_UP_DELAY = 25;
    private static final long TEST_GAME_FOCUS_DELAY = 10;

    public static final String PREF_NAME = "specialPrefs"; // SharedPreferences的名称

    public static final String KEY_NAME = "special_key"; // 要保存的键名称

    /**
     * Uma ação de bloco. Mantido público porque {@link GameInputDevice#getGameMenuOptions()}
     * (opções específicas de controle, por exemplo alternar emulação de mouse num
     * gamepad — fora do escopo desta fatia, que é sobre a sessão, não sobre entrada)
     * ainda devolve uma lista deste tipo a partir de {@code ControllerHandler}. A guia
     * desta fatia não lê essa lista — ver o relatório da F2 para a decisão.
     */
    public static class MenuOption {
        private final String label;
        private final boolean withGameFocus;
        private final Runnable runnable;

        public MenuOption(String label, boolean withGameFocus, Runnable runnable) {
            this.label = label;
            this.withGameFocus = withGameFocus;
            this.runnable = runnable;
        }

        public MenuOption(String label, Runnable runnable) {
            this(label, false, runnable);
        }
    }

    private final Game game;
    private final Context dialogScreenContext;

    private Dialog guiaDialog;

    public GameMenu(Game game, Context dialogScreenContext) {
        this.game = game;
        this.dialogScreenContext = dialogScreenContext;
    }

    public GameMenu(Game game) {
        this.game = game;
        this.dialogScreenContext = game;
    }

    private void sendKeys(short[] keys) {
        game.sendKeys(keys);
    }

    /**
     * Roda uma ação depois de garantir que a janela da Game recuperou o foco — o
     * Dialog da guia rouba o foco da janela enquanto está de pé, e uma ação que lê
     * estado de jogo (girar tela, estatísticas etc.) correndo antes desse foco voltar
     * historicamente pegava a Activity num estado inconsistente.
     */
    private void runWithGameFocus(Runnable runnable) {
        if (game.isFinishing()) {
            return;
        }
        if (!game.hasWindowFocus() && dialogScreenContext instanceof Game) {
            new Handler().postDelayed(() -> runWithGameFocus(runnable), TEST_GAME_FOCUS_DELAY);
            return;
        }
        runnable.run();
    }

    // ==================== a guia ====================

    @Override
    public void showMenu(GameInputDevice device) {
        if (guiaDialog != null && guiaDialog.isShowing()) {
            guiaDialog.dismiss();
        }

        int themeResId = game.getApplicationInfo().theme;
        Context themedContext = new ContextThemeWrapper(dialogScreenContext, themeResId);

        guiaDialog = new Dialog(themedContext, R.style.ConsoleDialog);
        guiaDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        guiaDialog.setContentView(R.layout.console_guia);
        guiaDialog.setCanceledOnTouchOutside(true);
        guiaDialog.setCancelable(true);

        Window window = guiaDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.gravity = Gravity.TOP;
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
        }

        wireGuia(guiaDialog, device);

        // Esc fecha a guia — o Dialog já fecha sozinho no Voltar (setCancelable).
        guiaDialog.setOnKeyListener((dialog, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ESCAPE && event.getAction() == KeyEvent.ACTION_UP) {
                dialog.dismiss();
                return true;
            }
            return false;
        });

        guiaDialog.setOnDismissListener(dialog -> game.setConsoleGuiaOpen(false));

        guiaDialog.show();
        game.setConsoleGuiaOpen(true);
    }

    @Override
    public void hideMenu() {
        if (guiaDialog != null && guiaDialog.isShowing()) {
            guiaDialog.dismiss();
        }
        guiaDialog = null;
    }

    @Override
    public boolean isMenuOpen() {
        return guiaDialog != null && guiaDialog.isShowing();
    }

    /**
     * Liga cada bloco à ação que já existe no app. A ordem segue NOTA.md §2: Teclado ·
     * Barra de texto · Modo do toque · Teclas especiais · Girar a tela · Estatísticas ·
     * Controles na tela, com Desconectar/Encerrar no PC fixos à direita.
     */
    private void wireGuia(final Dialog dialog, final GameInputDevice device) {
        // A alça de recolher: mesmo layout da aba, chevron pra cima, fecha ao toque.
        View alca = dialog.findViewById(R.id.consoleGuiaAlca);
        if (alca != null) {
            alca.setVisibility(View.VISIBLE);
            android.widget.ImageView chevron = dialog.findViewById(R.id.consoleAbaChevron);
            if (chevron != null) {
                chevron.setImageResource(R.drawable.ic_console_chevron_up);
            }
            alca.setOnClickListener(v -> dialog.dismiss());
        }

        final View blocosScroll = dialog.findViewById(R.id.consoleGuiaScrollBlocos);
        final View teclasScroll = dialog.findViewById(R.id.consoleGuiaScrollTeclas);

        onBlock(dialog, R.id.consoleBlocoTeclado, true, game::toggleKeyboard);

        onBlock(dialog, R.id.consoleBlocoBarraTexto, true, game::openComposeBarFromGuia);

        final TextView modoToqueRotulo = dialog.findViewById(R.id.consoleBlocoModoToqueRotulo);
        refreshModoToqueLabel(modoToqueRotulo);
        onBlock(dialog, R.id.consoleBlocoModoToque, true, game::toggleTouchscreenTrackpad);

        // "Teclas especiais" troca a fileira em vez de fechar a guia.
        View teclasEspeciais = dialog.findViewById(R.id.consoleBlocoTeclasEspeciais);
        if (teclasEspeciais != null) {
            teclasEspeciais.setOnClickListener(v -> {
                if (blocosScroll != null) blocosScroll.setVisibility(View.GONE);
                if (teclasScroll != null) {
                    teclasScroll.setVisibility(View.VISIBLE);
                    View primeiraTecla = dialog.findViewById(R.id.consoleBlocoVoltar);
                    if (primeiraTecla != null) {
                        primeiraTecla.requestFocus();
                    }
                }
            });
        }

        View voltar = dialog.findViewById(R.id.consoleBlocoVoltar);
        if (voltar != null) {
            voltar.setOnClickListener(v -> {
                if (teclasScroll != null) teclasScroll.setVisibility(View.GONE);
                if (blocosScroll != null) {
                    blocosScroll.setVisibility(View.VISIBLE);
                    if (teclasEspeciais != null) {
                        teclasEspeciais.requestFocus();
                    }
                }
            });
        }

        wireTecla(dialog, R.id.consoleKeyEsc, new short[]{KeyboardTranslator.VK_ESCAPE});
        wireTecla(dialog, R.id.consoleKeyTab, new short[]{KeyboardTranslator.VK_TAB});
        wireTecla(dialog, R.id.consoleKeyWin, new short[]{KeyboardTranslator.VK_LWIN});
        wireTecla(dialog, R.id.consoleKeyAltTab,
                new short[]{KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_TAB});
        wireTecla(dialog, R.id.consoleKeyCtrlAltDel, new short[]{
                KeyboardTranslator.VK_LCONTROL, KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_DELETE});

        // Girar a tela: só faz sentido dentro da própria Game (não no display externo,
        // igual ao Quick Menu de antes desta fatia).
        View girar = dialog.findViewById(R.id.consoleBlocoGirar);
        if (girar != null) {
            if (dialogScreenContext == game) {
                onBlock(dialog, R.id.consoleBlocoGirar, true, game::rotateScreen);
            } else {
                girar.setVisibility(View.GONE);
            }
        }

        onBlock(dialog, R.id.consoleBlocoEstatisticas, true, game::toggleHUD);

        if (!game.isOnExternalDisplay()) {
            onBlock(dialog, R.id.consoleBlocoControles, true, game::toggleVirtualController);
        } else {
            View controles = dialog.findViewById(R.id.consoleBlocoControles);
            if (controles != null) {
                controles.setVisibility(View.GONE);
            }
        }

        onBlock(dialog, R.id.consoleBlocoDesconectar, false, game::disconnect);

        onBlock(dialog, R.id.consoleBlocoEncerrar, false, game::quit);

        // Foco inicial no primeiro bloco (B-CONSOLE.md §F2).
        View primeiro = dialog.findViewById(R.id.consoleBlocoTeclado);
        if (primeiro != null) {
            primeiro.post(primeiro::requestFocus);
        }
    }

    private void refreshModoToqueLabel(TextView rotulo) {
        if (rotulo == null) {
            return;
        }
        rotulo.setText(game.isTouchscreenTrackpad()
                ? R.string.console_modo_touchpad
                : R.string.console_modo_toque_direto);
    }

    /**
     * Fecha a guia e roda a ação — nessa ordem, sempre. {@code runWithGameFocus}
     * espera {@code game.hasWindowFocus()} voltar a {@code true}, e isso só acontece
     * DEPOIS que o Dialog é dispensado (mostrar um Dialog tira o foco de janela da
     * Activity por baixo). Fechar a guia dentro da própria ação — como o código fazia
     * antes desta correção — trava num laço que nunca termina: a ação some enquanto
     * espera o foco, e o foco nunca volta porque é a mesma ação que fecharia a guia.
     * Achado fotografando a guia de verdade (input keyevent trava em "Modo do
     * toque"/"Estatísticas" sem soltar); ver docs/planos/console/relatorios/F2-implementacao.md.
     */
    private void onBlock(Dialog dialog, int viewId, boolean withGameFocus, Runnable action) {
        View block = dialog.findViewById(viewId);
        if (block == null) {
            return;
        }
        block.setOnClickListener(v -> {
            dialog.dismiss();
            if (withGameFocus) {
                runWithGameFocus(action);
            } else {
                action.run();
            }
        });
    }

    private void wireTecla(Dialog dialog, int viewId, short[] keys) {
        View key = dialog.findViewById(viewId);
        if (key == null) {
            return;
        }
        key.setOnClickListener(v -> {
            dialog.dismiss();
            sendKeys(keys);
        });
    }
}
