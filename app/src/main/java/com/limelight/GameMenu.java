package com.limelight;

import com.limelight.ui.VidroToast;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.limelight.binding.input.GameInputDevice;
import com.limelight.binding.input.KeyboardTranslator;
import com.limelight.nvstream.http.HostCapabilities;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.utils.KeyConfigHelper;
import com.limelight.utils.KeyMapper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * As AÇÕES da sessão em curso — só o que se faz uma vez e acontece.
 * <p>
 * Era uma lista de onze itens com um segundo diálogo escondido atrás, e mais da metade
 * dela não era ação nenhuma: eram interruptores disfarçados de comando ("Toggle X"), que
 * uma lista sabe mandar alternar mas não sabe mostrar em que estado estão. Esses foram
 * para a folha lateral de ajustes, onde viraram chip de escolha e interruptor de verdade.
 * <p>
 * O que ficou aqui cabe numa tela sem rolar e tem todo o mesmo formato: uma frase no
 * imperativo, um efeito, nenhum estado a lembrar. O menu avançado deixou de existir —
 * ninguém precisa atravessar um modal para chegar a outro.
 */
public class GameMenu implements Game.GameMenuCallbacks {

    public static final long KEY_UP_DELAY = 25;
    private static final long TEST_GAME_FOCUS_DELAY = 10;

    public static final String PREF_NAME = "specialPrefs"; // SharedPreferences的名称

    public static final String KEY_NAME = "special_key"; // 要保存的键名称

    public static class MenuOption {
        private final String label;
        /**
         * Linha de apoio, ou 0 quando a ação se explica sozinha.
         *
         * Só as duas saídas e as duas ações que mexem no PC a usam. É o que faltava para
         * "Sair da tela" e "Encerrar sessão" pararem de parecer dois nomes da mesma coisa.
         */
        private final int hintRes;
        private final boolean withGameFocus;
        private final Runnable runnable;

        public MenuOption(String label, int hintRes, boolean withGameFocus, Runnable runnable) {
            this.label = label;
            this.hintRes = hintRes;
            this.withGameFocus = withGameFocus;
            this.runnable = runnable;
        }

        public MenuOption(String label, boolean withGameFocus, Runnable runnable) {
            this(label, 0, withGameFocus, runnable);
        }

        public MenuOption(String label, Runnable runnable) {
            this(label, 0, false, runnable);
        }

        // O adaptador desenha o rótulo a partir daqui — é o que dispensa procurar a opção
        // de volta pelo texto da linha na hora do toque.
        @Override
        public String toString() {
            return label;
        }
    }

    private final Game game;
    private final Context dialogScreenContext;

    private AlertDialog currentDialog;

    public GameMenu(Game game, Context dialogScreenContext) {
        this.game = game;
        this.dialogScreenContext = dialogScreenContext;
    }

    public GameMenu(Game game) {
        this.game = game;
        this.dialogScreenContext = game;
    }

    private String getString(int id) {
        return game.getResources().getString(id);
    }


    private void sendKeys(short[] keys) {
        game.sendKeys(keys);
    }

    private void runWithGameFocus(Runnable runnable) {
        // Ensure that the Game activity is still active (not finished)
        if (game.isFinishing()) {
            return;
        }
        // Check if the game window has focus again, if not try again after delay
        if (!game.hasWindowFocus() && dialogScreenContext instanceof Game) {
            new Handler().postDelayed(() -> runWithGameFocus(runnable), TEST_GAME_FOCUS_DELAY);
            return;
        }
        // Game Activity has focus, run runnable
        runnable.run();
    }

    private void run(MenuOption option) {
        if (option.runnable == null) {
            return;
        }

        if (option.withGameFocus) {
            runWithGameFocus(option.runnable);
        } else {
            option.runnable.run();
        }
    }

    /*
     * "Enviar texto…" não existe mais aqui.
     *
     * Era um diálogo com um campo de texto dentro de um menu, e fazia exatamente o que a
     * barra de composição da ilha faz — montar o texto no aparelho e mandar de uma vez —,
     * só que sem o teclado empurrando a tela e sem a barra continuar aberta para a frase
     * seguinte. Duas portas para a mesma coisa, e esta era a pior das duas. O bloco
     * Teclado da ilha é a que ficou.
     */

    private void showMenuDialog(String title, MenuOption[] options) {
        int themeResId = game.getApplicationInfo().theme;

        Context themedContext = new ContextThemeWrapper(dialogScreenContext, themeResId);
        AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
        builder.setTitle(title);

        // Linha própria em vez de simple_list_item_1: aquele layout traz Roboto e o
        // espaçamento do sistema para dentro do menu.
        //
        // O adaptador carrega a MenuOption inteira, e não o rótulo dela. Antes o toque
        // procurava a opção de volta comparando o texto da linha com o de cada opção — o
        // que já era frágil e ficou impossível agora que os comandos do servidor entram
        // aqui: eles vêm do host e nada impede dois com o mesmo nome, e o primeiro da
        // lista responderia pelos dois.
        final ArrayAdapter<MenuOption> actions =
                new ArrayAdapter<MenuOption>(themedContext, R.layout.vidro_menu_row, R.id.menu_row_label) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = super.getView(position, convertView, parent);

                // A linha é reciclada: sem apagar a dica da opção anterior, a explicação
                // de "Sair da tela" reapareceria embaixo do primeiro comando do servidor
                // que rolasse para o lugar dela.
                TextView hint = row.findViewById(R.id.menu_row_hint);
                if (hint != null) {
                    MenuOption option = getItem(position);
                    int hintRes = option != null ? option.hintRes : 0;
                    hint.setText(hintRes != 0 ? GameMenu.this.getString(hintRes) : "");
                    hint.setVisibility(hintRes != 0 ? View.VISIBLE : View.GONE);
                }

                return row;
            }
        };

        builder.setAdapter(actions, (dialog, which) -> {
            MenuOption option = actions.getItem(which);
            if (option != null) {
                run(option);
            }
        });

        builder.setOnCancelListener(dialog -> hideMenu());

        if (currentDialog != null) {
            currentDialog.dismiss();
        }
        currentDialog = builder.show();

        final ListView menuList = currentDialog.getListView();
        if (menuList != null) {
            // A barra de rolagem da lista é a do sistema: um fio cinza neutro colado na
            // borda do card, e cinza neutro é justamente o que esta identidade não tem.
            // Quem passa a dizer que a lista continua é o esmaecido da borda — mesma
            // linguagem do resto do app, e só alpha por quadro.
            menuList.setVerticalScrollBarEnabled(false);
            menuList.setVerticalFadingEdgeEnabled(true);
            menuList.setFadingEdgeLength(
                    game.getResources().getDimensionPixelSize(R.dimen.vidro_space_5));

            // Faixa morta em cima e embaixo da lista.
            //
            // O título ficava a 33 px do primeiro item enquanto os itens andam de 49 em 49:
            // o cabeçalho estava mais perto do conteúdo do que o conteúdo de si mesmo, e ao
            // rolar o texto passava raspando por baixo dele. Embaixo era o mesmo, com a
            // última linha encostada no corte da folha. O clipe fica ligado de propósito —
            // é o que faz da margem um vão de verdade, e não uma faixa por onde o texto
            // continua desfilando.
            int band = game.getResources().getDimensionPixelSize(R.dimen.vidro_space_3);
            menuList.setPadding(menuList.getPaddingLeft(), band,
                    menuList.getPaddingRight(), band);
        }

        Window window = currentDialog.getWindow();

        if (window != null) {
            View decorView = window.getDecorView();
            decorView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {

                    decorView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        for (MenuOption option : options) {
                            actions.add(option);
                        }
                        actions.notifyDataSetChanged();

                        // A lista é medida vazia e só depois recebe os itens, e sem
                        // ancorar o topo ela abre já rolada — o começo do menu nascia fora
                        // da área visível. O menu de ações agora cabe em qualquer tela,
                        // mas a paleta de teclas continua sendo quinze linhas em paisagem.
                        if (menuList != null) {
                            menuList.setSelection(0);
                        }

                        capMenuHeight(window);
                    });
                }
            });
        }
    }

    /**
     * A folha do menu não encosta nas bordas da tela.
     *
     * Em paisagem o menu tem mais itens do que altura disponível e a janela cresce até os
     * 100%: os cantos arredondados ficam cortados rente ao topo e ao rodapé, o começo da
     * lista entra na faixa da barra de status e a última linha fica colada no corte de
     * baixo. Um cartão que toca as duas bordas não lê como cartão, lê como tela recortada.
     *
     * O teto só entra quando a folha realmente passa dele — menu curto continua do tamanho
     * do conteúdo, senão a folha ficaria esticada em volta de três linhas.
     *
     * Ouvinte de layout e não um post(): a lista nasce vazia e só recebe os itens no quadro
     * seguinte, e a ordem entre a fila de mensagens e a passada de layout não é garantida —
     * um post podia medir a folha ainda do tamanho de nada. O ouvinte fica: depois de
     * aplicado o teto a altura passa a ser exatamente ele, então a condição não dispara de
     * novo e o custo por quadro é uma comparação de inteiro.
     */
    private void capMenuHeight(final Window window) {
        final View decorView = window.getDecorView();
        decorView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int margin = game.getResources().getDimensionPixelSize(R.dimen.vidro_space_5);
                int ceiling = game.getResources().getDisplayMetrics().heightPixels - 2 * margin;
                if (ceiling <= 0 || decorView.getHeight() <= ceiling) {
                    return;
                }

                WindowManager.LayoutParams params = window.getAttributes();
                params.height = ceiling;
                window.setAttributes(params);
            }
        });
    }

    /**
     * A paleta de teclas — o único lugar do menu que ainda abre uma segunda lista.
     *
     * E abre porque é o que ela é: quinze combinações que a pessoa dispara e pronto, sem
     * estado nenhum para lembrar. O que não se sustentava era o caminho antigo — menu,
     * avançado, teclas: três camadas para mandar um Esc. Agora está a um toque do menu, e
     * a primeira linha é "Voltar", porque uma lista que só sai por "Cancelar" faz quem
     * entrou por engano fechar tudo e recomeçar.
     */
    /** A gaveta da sessao chama esta, sem controle especifico. */
    public void showSpecialKeysMenu() {
        showSpecialKeysMenu(null);
    }

    private void showSpecialKeysMenu(final GameInputDevice device) {
        List<MenuOption> options = new ArrayList<>();

        options.add(new MenuOption(getString(R.string.vidro_back), () -> showMenu(device)));

        if(!PreferenceConfiguration.readPreferences(game).disableDefaultExtraKeys){
            options.add(new MenuOption(getString(R.string.game_menu_send_keys_esc),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_ESCAPE})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_f11),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_F11})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_alt_f4),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_F4})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_alt_enter),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_RETURN})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_ctrl_v),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LCONTROL, KeyboardTranslator.VK_V})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_win),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LWIN})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_win_d),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_D})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_win_g),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_G})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_ctrl_alt_tab),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LCONTROL, KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_TAB})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_shift_tab),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_TAB})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_win_shift_left),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_LEFT})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_ctrl_alt_shift_f1),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LCONTROL,KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_F1})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_ctrl_alt_shift_f12),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LCONTROL,KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_F12})));

            options.add(new MenuOption(getString(R.string.game_menu_send_keys_alt_b),
                    () -> sendKeys(new short[]{KeyboardTranslator.VK_LWIN, KeyboardTranslator.VK_LMENU, KeyboardTranslator.VK_B})));
        }

        // Import custom shortcuts
        SharedPreferences preferences = game.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);
        String value = preferences.getString(KEY_NAME,"");

        if(!TextUtils.isEmpty(value)){
            try {
                KeyConfigHelper.ShortcutFile shortcutFile = KeyConfigHelper.parseShortcutFile(value);
                if (shortcutFile != null && shortcutFile.data != null && !shortcutFile.data.isEmpty()) {
                    List<KeyConfigHelper.Shortcut> data = shortcutFile.data;
                    for (KeyConfigHelper.Shortcut sc : data) {
                        List<String> keys = sc.keys;
                        short[] keyCodes = new short[keys.size()];

                        for (int i = 0; i < keys.size(); i++) {
                            String code = keys.get(i);
                            int keycode;

                            if (code.startsWith("0x")) {               // literal hex value
                                keycode = Integer.parseInt(code.substring(2), 16);
                            } else if (code.startsWith("VK_")) {       // symbolic constant in KeyMapper
                                Field field = KeyMapper.class.getDeclaredField(code);
                                keycode = field.getInt(null);
                            } else {                                   // unsupported
                                throw new IllegalArgumentException("Unknown key code: " + code);
                            }
                            keyCodes[i] = (short) keycode;
                        }

                        // Whatever MenuOption looks like in your project
                        MenuOption option = new MenuOption(sc.name, () -> sendKeys(keyCodes));
                        options.add(option);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                VidroToast.show(game,getString(R.string.wrong_import_format),Toast.LENGTH_SHORT);
            }
        }

        // O Gerenciador de Tarefas é Ctrl + Shift + Esc e nada mais — estava no menu
        // avançado como se fosse outra categoria de coisa. Fica fora do bloco das teclas
        // padrão porque quem desliga as teclas padrão desliga atalhos de jogo, não a
        // porta de emergência para destravar o PC.
        options.add(new MenuOption(getString(R.string.game_menu_task_manager), true,
                () -> sendKeys(new short[]{KeyboardTranslator.VK_LCONTROL, KeyboardTranslator.VK_LSHIFT, KeyboardTranslator.VK_ESCAPE})));

        options.add(new MenuOption(getString(R.string.game_menu_cancel), null));

        showMenuDialog(getString(R.string.game_menu_send_keys), options.toArray(new MenuOption[options.size()]));
    }

    /**
     * A folha de ajustes só existe na janela do próprio Game, e no aparelho.
     *
     * Quando a sessão está numa tela externa, quem desenha os controles é a
     * ExternalDisplayControlActivity — a ilha e a folha estão na TV, longe do dedo. Aí
     * este menu volta a ser a única superfície de controle que existe, e os interruptores
     * precisam continuar alcançáveis por ele. Continuam em lista, mas em UM nível: o menu
     * avançado não volta em situação nenhuma.
     */
    private boolean isSheetReachable() {
        return dialogScreenContext == game && !game.isOnExternalDisplay();
    }

    public void showMenu(GameInputDevice device) {
        List<MenuOption> options = new ArrayList<>();

        // Comandos do servidor entram AQUI, no primeiro nível, e só quando o host tem
        // algum.
        //
        // Eram uma linha que abria outra lista, e num host sem suporte — Sunshine e os
        // forks dele, o que este fork usa — essa lista abria um aviso dizendo que estava
        // vazia. Uma ação que só sabe informar que não existe não precisa estar na tela.
        // Com poucos comandos a lista aninhada também não se justificava: eles são
        // comandos como os outros e ficam junto dos outros.
        ArrayList<String> serverCmds = game.getServerCmds();
        if (serverCmds != null && !serverCmds.isEmpty()) {
            AtomicInteger index = new AtomicInteger(0);
            for (String cmd : serverCmds) {
                final int cmdId = index.getAndIncrement();
                options.add(new MenuOption("> " + cmd, true, () -> game.sendExecServerCmd(cmdId)));
            }
        }

        // A área de transferência volta ao menu — mas só onde ela existe.
        //
        // Ela saiu daqui na rodada passada porque no Vibeshine as duas linhas apareciam e
        // não funcionavam: `actions/clipboard` é extensão do Apollo, e o Sunshine devolve
        // a página de erro dele com status 200, que o cliente lia como "deu certo". Tirar
        // as linhas resolvia o sintoma para este host e tirava o recurso de quem tem
        // Apollo. Agora quem decide é o host: o modelo de permissões dele diz, antes de
        // qualquer tentativa, se cada direção é permitida — e as direções são
        // independentes, então cada linha pergunta pela sua.
        //
        // Host sem modelo de permissões (Sunshine, Vibeshine, GFE) não responde nada, e
        // não responder é resposta: nenhuma das duas aparece.
        //
        // Os argumentos são os do pedido EXPLÍCITO, e não os da sincronização automática.
        // `getClipboard(0)` desiste calado enquanto o menu está na tela, e o menu AINDA
        // está na tela quando o toque é atendido — o dispensar vem depois do clique;
        // `sendClipboard(false)` recusa devolver ao PC um texto que veio do PC. Nos dois
        // casos quem pediu foi a pessoa, e desistir sem dizer nada é o mesmo defeito que
        // este bloco existe para tirar da tela.
        HostCapabilities hostCaps = game.getHostCapabilities();
        if (hostCaps.supportsClipboardFetch()) {
            options.add(new MenuOption(getString(R.string.game_menu_fetch_clipboard),
                    R.string.vidro_clipboard_fetch_hint, true, () -> game.getClipboard(-1)));
        }
        if (hostCaps.supportsClipboardSend()) {
            options.add(new MenuOption(getString(R.string.game_menu_upload_clipboard),
                    R.string.vidro_clipboard_send_hint, true, () -> game.sendClipboard(true)));
        }

        if (dialogScreenContext == game) {
            options.add(new MenuOption(getString(R.string.game_menu_rotate_screen),
                    R.string.vidro_rotate_hint, true, game::rotateScreen));
        }

        // O teclado fica SEMPRE aqui, fora do gate de isSheetReachable().
        //
        // Este menu é a superfície de socorro: no aparelho do dono a folha ficou invisível
        // por defeito de compositor, e "alcançável" no código não era visível na tela. Preso
        // numa stream, a ação que mais faz falta é exatamente o teclado — escondê-la porque a
        // folha "existe" seria repetir o defeito que este menu existe para cobrir.
        options.add(new MenuOption(getString(R.string.vidro_keyboard),
                R.string.vidro_compose_hint, true, game::openComposeBar));

        options.add(new MenuOption(getString(R.string.game_menu_send_keys),
                R.string.vidro_special_keys_hint, false, () -> showSpecialKeysMenu(device)));

        if (!isSheetReachable()) {
            if (game.allowChangeMouseMode) {
                options.add(new MenuOption(getString(R.string.game_menu_select_mouse_mode), true,
                        () -> game.selectMouseMode(dialogScreenContext)));
            }

            options.add(new MenuOption(getString(game.isZoomModeEnabled()
                    ? R.string.game_menu_disable_zoom_mode
                    : R.string.game_menu_enable_zoom_mode), true, game::toggleZoomMode));

            options.add(new MenuOption(getString(R.string.game_menu_toggle_hud), true,
                    game::toggleHUD));

            options.add(new MenuOption(getString(R.string.game_menu_toggle_keyboard_model), true,
                    game::toggleKeyboardController));

            options.add(new MenuOption(getString(R.string.game_menu_toggle_virtual_keyboard_model),
                    true, game::toggleFullKeyboard));
        }

        // As opções do gamepad continuam vindo do próprio dispositivo — hoje é a emulação
        // de mouse, e ela só aparece quando há um controle capaz disso ligado. Nasce e
        // morre com o aparelho, então não tem onde morar na folha.
        if (device != null) {
            options.addAll(device.getGameMenuOptions());
        }

        // As duas saídas ficam NO FIM, e não no começo.
        //
        // Eram os dois primeiros itens, exatamente onde o polegar cai quando o menu abre, e
        // pintados igual a "Enviar área de transferência": mesmo peso, mesma cor, sem
        // divisória. Errar o alvo por uma linha derrubava a sessão. Irreversível não pede
        // destaque, pede distância — a mesma regra que tirou o preenchimento de acento do
        // "Sair da tela" na ilha.
        //
        // E o rótulo é o MESMO da ilha: "Desconectar" e "Sair da tela" eram dois nomes para
        // a mesma coisa, ao lado de "Encerrar sessão", que faz outra. Três nomes para duas
        // ações não deixavam ninguém saber qual delas resolve.
        //
        // Faltava ainda dizer o que cada uma faz. Os dois nomes descrevem o que acontece
        // AQUI e as duas coisas parecem a mesma daqui; a diferença está do outro lado, no
        // PC, e é lá que a linha de apoio aponta.
        options.add(new MenuOption(getString(R.string.vidro_leave_screen),
                R.string.vidro_leave_screen_hint, false, game::disconnect));

        options.add(new MenuOption(getString(R.string.game_menu_quit_session),
                R.string.vidro_quit_session_hint, false, game::quit));

        options.add(new MenuOption(getString(R.string.game_menu_cancel), null));

        showMenuDialog(getString(R.string.vidro_menu_title), options.toArray(new MenuOption[options.size()]));
    }

    @Override
    public void hideMenu() {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }
        currentDialog = null;
    }

    @Override
    public boolean isMenuOpen() {
        return currentDialog != null && currentDialog.isShowing();
    }
}
