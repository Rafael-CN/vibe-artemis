package com.limelight.preferences;

import com.limelight.ui.VidroToast;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;

import com.limelight.CatalogoDoLugar;
import com.limelight.Game;
import com.limelight.PcView;
import com.limelight.ShortcutTrampoline;
import com.limelight.computers.ComputerManagerService;
import com.limelight.R;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.utils.Dialog;
import com.limelight.utils.ServerHelper;
import com.limelight.utils.SpinnerDialog;
import com.limelight.ui.compose.PonteDoEndereco;
import com.limelight.utils.UiHelper;

import android.app.AlertDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AddComputerManually extends AppCompatActivity {
    private TextView hostText;
    private ComputerManagerService.ComputerManagerBinder managerBinder;
    private final LinkedBlockingQueue<String> computersToAdd = new LinkedBlockingQueue<>();
    private volatile Thread addThread;

    // "Estou encerrando" e "cancelei esta adição" chegam à thread pelo mesmo interrupt, e
    // ela precisa distinguir os dois: no encerramento morre, no cancelamento volta a
    // esperar o próximo endereço. Sem esta flag, cancelar uma vez mataria a thread e o
    // botão Adicionar ficaria inerte pelo resto da tela — trocaríamos um beco por outro.
    private volatile boolean addThreadShuttingDown;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, final IBinder binder) {
            managerBinder = ((ComputerManagerService.ComputerManagerBinder)binder);
            startAddThread();
        }

        public void onServiceDisconnected(ComponentName className) {
            joinAddThread();
            managerBinder = null;
        }
    };

    private boolean isWrongSubnetSiteLocalAddress(String address) {
        try {
            InetAddress targetAddress = InetAddress.getByName(address);
            if (!(targetAddress instanceof Inet4Address) || !targetAddress.isSiteLocalAddress()) {
                return false;
            }

            // We have a site-local address. Look for a matching local interface.
            for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InterfaceAddress addr : iface.getInterfaceAddresses()) {
                    if (!(addr.getAddress() instanceof Inet4Address) || !addr.getAddress().isSiteLocalAddress()) {
                        // Skip non-site-local or non-IPv4 addresses
                        continue;
                    }

                    byte[] targetAddrBytes = targetAddress.getAddress();
                    byte[] ifaceAddrBytes = addr.getAddress().getAddress();

                    // Compare prefix to ensure it's the same
                    boolean addressMatches = true;
                    for (int i = 0; i < addr.getNetworkPrefixLength(); i++) {
                        if ((ifaceAddrBytes[i / 8] & (1 << (i % 8))) != (targetAddrBytes[i / 8] & (1 << (i % 8)))) {
                            addressMatches = false;
                            break;
                        }
                    }

                    if (addressMatches) {
                        return false;
                    }
                }
            }

            // Couldn't find a matching interface
            return true;
        } catch (Exception e) {
            // Catch all exceptions because some broken Android devices
            // will throw an NPE from inside getNetworkInterfaces().
            e.printStackTrace();
            return false;
        }
    }

    private Uri parseRawUserInputToUri(String rawUserInput) {
        // Try adding a scheme and parsing the remaining input.
        // This handles input like 127.0.0.1:47989, [::1], [::1]:47989, and 127.0.0.1.
        Uri uri = Uri.parse("art://" + rawUserInput);
        if (uri.getHost() != null && !uri.getHost().isEmpty()) {
            return uri;
        }

        // Attempt to escape the input as an IPv6 literal.
        // This handles input like ::1.
        uri = Uri.parse("art://[" + rawUserInput + "]");
        if (uri.getHost() != null && !uri.getHost().isEmpty()) {
            return uri;
        }

        return null;
    }

    private androidx.compose.ui.platform.ComposeView enderecoCompose;
    private String textoDoEndereco = "";
    private int erroDoEndereco;
    private boolean tentandoConectar;

    /** Os enderecos que ja foram tentados. Ver o comentario em TelaEndereco. */
    private static final String PREF_TENTADOS = "vidro_enderecos_tentados";
    private static final int QUANTOS_TENTADOS = 3;

    /**
     * Liga a tela ao Compose.
     *
     * Um lugar so, chamado quando o texto muda, quando uma tentativa comeca e quando ela
     * termina. Nao ha TextWatcher e nao ha setEnabled: o botao le o mesmo estado que o campo.
     */
    private void montarEndereco() {
        if (enderecoCompose == null) {
            return;
        }
        PonteDoEndereco.montar(
                enderecoCompose,
                textoDoEndereco,
                erroDoEndereco,
                tentandoConectar,
                lerTentados(),
                texto -> {
                    // Digitar apaga o erro: ele fala da tentativa ANTERIOR, e continuar
                    // mostrando-o enquanto a pessoa corrige e culpa-la pelo que ela ja esta
                    // consertando.
                    textoDoEndereco = texto;
                    if (erroDoEndereco != 0) {
                        erroDoEndereco = 0;
                    }
                    montarEndereco();
                    return kotlin.Unit.INSTANCE;
                },
                () -> { finish(); return kotlin.Unit.INSTANCE; },
                () -> { aoConectar(); return kotlin.Unit.INSTANCE; },
                endereco -> {
                    textoDoEndereco = endereco;
                    erroDoEndereco = 0;
                    montarEndereco();
                    return kotlin.Unit.INSTANCE;
                });
    }

    private void aoConectar() {
        String endereco = textoDoEndereco.trim();
        if (endereco.isEmpty()) {
            // O campo vazio nao merece nem erro: o botao ja diz o que falta.
            return;
        }
        erroDoEndereco = 0;
        tentandoConectar = true;
        montarEndereco();
        guardarTentado(endereco);
        computersToAdd.add(endereco);
    }

    private void mostrarErro(final int qual) {
        runOnUiThread(() -> {
            erroDoEndereco = qual;
            tentandoConectar = false;
            montarEndereco();
        });
    }

    private java.util.List<String> lerTentados() {
        String cru = getSharedPreferences(PREF_TENTADOS, MODE_PRIVATE).getString("lista", "");
        java.util.List<String> saida = new java.util.ArrayList<>();
        if (cru == null || cru.isEmpty()) {
            return saida;
        }
        for (String e : cru.split("\u001F")) {
            if (!e.isEmpty() && !e.equals(textoDoEndereco)) {
                saida.add(e);
            }
        }
        return saida;
    }

    private void guardarTentado(String endereco) {
        java.util.List<String> lista = new java.util.ArrayList<>();
        lista.add(endereco);
        for (String e : lerTentados()) {
            if (!e.equals(endereco) && lista.size() < QUANTOS_TENTADOS) {
                lista.add(e);
            }
        }
        getSharedPreferences(PREF_TENTADOS, MODE_PRIVATE).edit()
                .putString("lista", android.text.TextUtils.join("\u001F", lista))
                .apply();
    }

    private void doAddPc(String rawUserInput) throws InterruptedException {
        boolean wrongSiteLocal = false;
        boolean invalidInput = false;
        boolean success;
        int portTestResult;

        // Cancelável: endereço errado são uns dez segundos de sondagem mais o teste de
        // portas contra a internet, e uma sondagem que trava fica presa para sempre —
        // parallelPollPc espera no wait() do endereço manual sem prazo nenhum. O interrupt
        // é o que corta essa espera; addComputerBlocking propaga a InterruptedException e
        // só chama addTuple/updateComputer DEPOIS da sondagem, então parar antes disso não
        // deixa PC pela metade no banco.
        SpinnerDialog dialog = SpinnerDialog.displayCancellableDialog(this,
            getResources().getString(R.string.title_add_pc),
            getResources().getString(R.string.msg_add_pc),
            new Runnable() {
                @Override
                public void run() {
                    interruptAddThread();
                }
            });

        Uri uri = parseRawUserInputToUri(rawUserInput);
        try {
            ComputerDetails details = new ComputerDetails();

            // Check if we parsed a host address successfully
            if (uri != null && uri.getHost() != null && !uri.getHost().isEmpty()) {
                String host = uri.getHost();
                int port = uri.getPort();

                // If a port was not specified, use the default
                if (port == -1) {
                    port = NvHTTP.DEFAULT_HTTP_PORT;
                }

                details.manualAddress = new ComputerDetails.AddressTuple(host, port);
                success = managerBinder.addComputerBlocking(details);
                if (!success){
                    wrongSiteLocal = isWrongSubnetSiteLocalAddress(host);
                }
            } else {
                // Invalid user input
                success = false;
                invalidInput = true;
            }
        } catch (InterruptedException e) {
            // Propagate the InterruptedException to the caller for proper handling
            dialog.dismiss();
            throw e;
        } catch (IllegalArgumentException e) {
            // This can be thrown from OkHttp if the host fails to canonicalize to a valid name.
            // https://github.com/square/okhttp/blob/okhttp_27/okhttp/src/main/java/com/squareup/okhttp/HttpUrl.java#L705
            e.printStackTrace();
            success = false;
            invalidInput = true;
        }

        // Keep the SpinnerDialog open while testing connectivity
        if (!success && !wrongSiteLocal && !invalidInput) {
            // Run the test before dismissing the spinner because it can take a few seconds.
            portTestResult = MoonBridge.testClientConnectivity(ServerHelper.CONNECTION_TEST_SERVER, 443,
                    MoonBridge.ML_PORT_FLAG_TCP_47984 | MoonBridge.ML_PORT_FLAG_TCP_47989);
        } else {
            // Don't bother with the test if we succeeded or the IP address was bogus
            portTestResult = MoonBridge.ML_TEST_RESULT_INCONCLUSIVE;
        }

        dialog.dismiss();

        // O cancelamento pode ter chegado durante o teste de portas, que é nativo e não
        // olha o interrupt. Sair aqui evita anunciar a falha de uma tentativa que o
        // usuário já desistiu de fazer.
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        // O P03: a causa aparece DENTRO da tela, abaixo do campo.
        //
        // Eram tres AlertDialog com o titulo "Connection Error" -- a palavra que o handoff
        // proibe --, e o do caminho generico ainda oferecia um botao que levava a wiki de
        // outro projeto, fora do app, no meio de adicionar o primeiro PC. Fechar um dialogo
        // para so entao reencontrar o campo e voltar a digitar era o custo de cada letra
        // errada.
        if (invalidInput) {
            mostrarErro(R.string.addpc_unknown_host);
        }
        else if (wrongSiteLocal) {
            mostrarErro(R.string.addpc_wrong_sitelocal);
        }
        else if (!success) {
            // A frase sobre firewall so vale quando o teste de portas de fato acusou
            // bloqueio. Contra um endereco privado o NetworkScope nem roda o teste, e a
            // frase generica sobre firewall nao se aplica.
            mostrarErro(portTestResult != MoonBridge.ML_TEST_RESULT_INCONCLUSIVE && portTestResult != 0
                    ? R.string.nettest_text_blocked
                    : R.string.addpc_fail);
        }
        else {
            AddComputerManually.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    VidroToast.show(AddComputerManually.this, getResources().getString(R.string.addpc_success), Toast.LENGTH_LONG);

                    if (!isFinishing()) {
                        // Close the activity
                        AddComputerManually.this.finish();
                    }

                    String pin = uri.getQueryParameter("pin");
                    String passphrase = uri.getQueryParameter("passphrase");
                    if (pin != null && passphrase != null) {
                        Intent intent = new Intent(AddComputerManually.this, PcView.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("hostname", uri.getHost());
                        intent.putExtra("port", uri.getPort());
                        intent.putExtra("pin", pin);
                        intent.putExtra("passphrase", passphrase);

                        startActivity(intent);
                    }

                }
            });
        }

    }

    private void startAddThread() {
        addThreadShuttingDown = false;
        addThread = new Thread() {
            @Override
            public void run() {
                while (!addThreadShuttingDown) {
                    try {
                        String computer = computersToAdd.take();
                        doAddPc(computer);
                    } catch (InterruptedException e) {
                        // take() e doAddPc limpam o status de interrupção ao lançar. Se não
                        // é encerramento, foi o usuário cancelando a espera: a thread segue
                        // viva e volta a esperar o próximo endereço.
                        if (addThreadShuttingDown) {
                            return;
                        }
                    }
                }
            }
        };
        addThread.setName("UI - AddComputerManually");
        addThread.start();
    }

    /**
     * Corta a adição em curso sem matar a thread que a executa.
     *
     * É o que o Cancelar da roda de espera dispara. Chamado na thread da UI, então não
     * pode esperar pela thread: só levanta o interrupt e volta.
     */
    private void interruptAddThread() {
        Thread thread = addThread;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void joinAddThread() {
        if (addThread != null) {
            addThreadShuttingDown = true;
            addThread.interrupt();

            try {
                addThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();

                // InterruptedException clears the thread's interrupt status. Since we can't
                // handle that here, we will re-interrupt the thread to set the interrupt
                // status back to true.
                Thread.currentThread().interrupt();
            }

            addThread = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        Dialog.closeDialogs();
        SpinnerDialog.closeDialogs(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (managerBinder != null) {
            joinAddThread();
            unbindService(serviceConnection);
        }
    }

    /**
     * Faz o conteúdo subir junto com o teclado, em vez de ficar embaixo dele.
     *
     * O adjustResize do manifesto não estava chegando até aqui. Da API 29 em diante
     * UiHelper.notifyNewRootView põe a janela para desenhar sob as barras
     * (SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION) e, nesse modo, o sistema não encolhe mais o
     * quadro da janela: ele entrega o teclado como INSET, e quem tinha que virá-lo em
     * recuo era o tratamento padrão do android.R.id.content — justamente o que aquele
     * ouvinte substitui, e ele só olha as barras. O inset do IME chegava e era descartado.
     *
     * O recuo é aplicado no mesmo android.R.id.content porque este ouvinte SUBSTITUI o de
     * notifyNewRootView (um só por view), e por isso repete aqui o recuo lateral, o do
     * topo e a barra de navegação translúcida. Encolhendo o container, o rodapé de ação do
     * celular sobe e a explicação, que está num ScrollView, cede altura; no tablet o bloco
     * centrado se recentra no que sobrou. É o adjustResize refeito à mão.
     *
     * Chamar DEPOIS de notifyNewRootView(), senão ela desfaz isto.
     */
    private void setupImeInsets() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Abaixo da API 29 notifyNewRootView não instala ouvinte nenhum, a janela não
            // desenha sob as barras e o adjustResize encolhe o quadro sozinho. Não há o
            // que consertar ali, e recuar por conta própria só somaria ao que o sistema
            // já fez.
            return;
        }

        final View content = findViewById(android.R.id.content);

        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            // Tappable, como em notifyNewRootView: no modo por gestos a barra de navegação
            // não recebe toque e não precisa de recuo.
            Insets tappable = insets.getTappableElementInsets();

            // WindowInsetsCompat porque Type.ime() nativo é API 30+ e minSdk é 21.
            int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;

            // max, e não soma: o recuo do teclado é medido do fundo da janela e já engloba
            // a barra de navegação sobre a qual ele se apoia. Somando, o campo pararia uma
            // barra de navegação acima do teclado. O max também é o que mantém isto seguro
            // onde o sistema AINDA encolhe o quadro sozinho — ali o IME não sobrepõe a
            // janela, imeBottom chega zero, e o recuo continua sendo só o das barras.
            v.setPadding(tappable.left, tappable.top, tappable.right,
                    Math.max(tappable.bottom, imeBottom));

            // Show a translucent navigation bar if we can't tap there
            if (tappable.bottom != 0) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            }
            else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            }

            // Devolvidos intactos: consumir aqui apagaria o inset para qualquer filho que
            // venha a olhá-lo.
            return insets;
        });

        ViewCompat.requestApplyInsets(content);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String action = getIntent().getAction();

        String server;
        String query;
        Uri data = getIntent().getData();
        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            int port = data.getPort();

            if (port == -1) {
                String urlAction = data.getHost();
                if (Objects.equals(urlAction, "launch")) {
                    String hostUUID = data.getQueryParameter("host_uuid");
                    String hostName = data.getQueryParameter("host_name");
                    String appUUID = data.getQueryParameter("app_uuid");
                    String appName = data.getQueryParameter("app_name");
                    String appID = data.getQueryParameter("app_id");

                    Intent intent = new Intent(AddComputerManually.this, ShortcutTrampoline.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(CatalogoDoLugar.UUID_EXTRA, hostUUID);
                    intent.putExtra(CatalogoDoLugar.NAME_EXTRA, hostName);
                    intent.putExtra(Game.EXTRA_APP_UUID, appUUID);
                    intent.putExtra(Game.EXTRA_APP_NAME, appName);
                    intent.putExtra(Game.EXTRA_APP_ID, appID);

                    finish();

                    startActivity(intent);

                    return;
                }
            }

            server = data.getAuthority();
            query = data.getQuery();
        } else {
            query = null;
            server = null;
        }

        UiHelper.setLocale(this);

        setContentView(R.layout.activity_add_computer_manually);

        UiHelper.notifyNewRootView(this);

        enderecoCompose = findViewById(R.id.vidroEnderecoCompose);
        montarEndereco();

        // Bind to the ComputerManager service
        bindService(new Intent(AddComputerManually.this,
                    ComputerManagerService.class), serviceConnection, Service.BIND_AUTO_CREATE);


        // Check if we have been called from deep link
        if (data == null || server == null || query == null) {
            return;
        }

        hostText.setText(server);

        if (!query.isEmpty()) {
            String hostName = data.getQueryParameter("name");
            if (hostName != null && !hostName.isEmpty()) {
                hostName = hostName + " (" + server + ")";
            } else {
                hostName = server;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.VidroDialogPlatform);
            builder.setTitle(R.string.pair_pc_confirm_title);
            builder.setMessage(getString(R.string.pair_pc_confirm_message, hostName));

            builder.setPositiveButton(getString(R.string.proceed), (dialog, which) -> {
                dialog.dismiss();
                finish();
                computersToAdd.add(server + '?' + query);
            });

            builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    // Returns true if the event should be eaten
    private boolean handleDoneEvent() {
        String hostAddress = hostText.getText().toString().trim();

        if (hostAddress.length() == 0) {
            VidroToast.show(AddComputerManually.this, getResources().getString(R.string.addpc_enter_ip), Toast.LENGTH_LONG);
            return true;
        }

        computersToAdd.add(hostAddress);
        return false;
    }
}
