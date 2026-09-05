package com.limelight.ui;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.limelight.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A paleta de comandos — a interface da sessão para quem está com a capa acoplada.
 *
 * <h2>O caso que ninguém desenhava</h2>
 *
 * O tablet com a capa é um laptop, e é o uso principal deste fork. As mãos estão no teclado.
 * Toda a interface da sessão foi desenhada para um dedo que, nessa situação, não está na tela:
 * a gaveta pede a borda direita, o controle na tela pede toque, e o caminho até qualquer coisa
 * começava procurando um alvo num painel de 1722 dp.
 *
 * <p>Aqui não há alvo. Há um campo, e ele decide pelo que você digita.
 *
 * <h2>O contrato, que não se negocia</h2>
 *
 * <b>Enter é SEMPRE texto para o PC.</b> Sempre. Digitar "sair" não encerra a sessão, digitar
 * "trackpad" não troca o modo do mouse. Comando só existe depois de a pessoa DESCER com a
 * seta, e aí o Enter passa a valer para a linha realçada.
 *
 * <p>Isto é o oposto de uma barra de busca que executa o primeiro resultado, e é deliberado:
 * o campo existe primariamente para mandar texto a uma máquina remota, que é a dor de origem
 * deste fork. Um campo que às vezes manda texto e às vezes executa é um campo em que não se
 * pode confiar depressa — e depressa é a única velocidade que serve com as mãos no teclado.
 *
 * <h2>Um realce, e ele viaja</h2>
 *
 * Quando não há dedo, a lei da posse fica sem sujeito: não há o que agarrar, não há velocidade
 * para herdar. A substituta honesta é a persistência. Em vez de cada linha ter um estado de
 * foco próprio que acende e apaga, existe <b>um</b> realce, e ele desliza de linha para linha
 * pela mola CORPO. É o que separa uma lista com foco de uma coisa que você está conduzindo.
 */
public class PaletaDeComandos {

    /** Uma linha da paleta. */
    public static class Comando {
        final int rotulo;
        final Runnable acao;

        public Comando(int rotulo, Runnable acao) {
            this.rotulo = rotulo;
            this.acao = acao;
        }
    }

    /** Nada realçado: o Enter pertence ao campo. */
    private static final int NO_CAMPO = -1;

    private final Context contexto;
    private final EditText campo;
    private final ScrollView rolagem;
    private final LinearLayout lista;
    private final View realce;
    private final Mola molaDoRealce;

    private final List<Comando> todos = new ArrayList<>();
    private final List<Comando> visiveis = new ArrayList<>();

    private int selecionado = NO_CAMPO;

    public PaletaDeComandos(Context contexto, EditText campo, ScrollView rolagem,
                            LinearLayout lista, View realce) {
        this.contexto = contexto;
        this.campo = campo;
        this.rolagem = rolagem;
        this.lista = lista;
        this.realce = realce;
        this.molaDoRealce = new Mola(Mola.Perfil.CORPO, 0f, new Mola.Alvo() {
            @Override
            public void aplicar(float valor) {
                PaletaDeComandos.this.realce.setTranslationY(valor);
            }
        });

        campo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable e) {
                filtrar(e.toString());
            }
        });
    }

    public void definirComandos(List<Comando> comandos) {
        todos.clear();
        todos.addAll(comandos);
        filtrar(campo.getText().toString());
    }

    /** Volta ao estado de repouso: nada realçado, lista escondida. */
    public void recolher() {
        selecionado = NO_CAMPO;
        molaDoRealce.cancelar();
        realce.setVisibility(View.GONE);
        rolagem.setVisibility(View.GONE);
    }

    public boolean estaNaLista() {
        return selecionado != NO_CAMPO;
    }

    /**
     * As teclas que a paleta reivindica, e só elas.
     *
     * <p>Tudo o que não estiver aqui continua sendo texto: as letras chegam ao campo, o Enter
     * manda para o PC, e o Backspace apaga. A paleta não é um modo — ela é uma lista que
     * aparece quando a seta desce e some quando ela sobe demais.
     *
     * @return true quando a paleta consumiu a tecla
     */
    public boolean aoTeclar(int keyCode, KeyEvent evento) {
        if (evento.getAction() != KeyEvent.ACTION_DOWN) {
            // O UP da tecla que o DOWN consumiu também é nosso, ou ele vaza para o campo.
            return selecionado != NO_CAMPO
                    && (keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                        || keyCode == KeyEvent.KEYCODE_DPAD_UP
                        || keyCode == KeyEvent.KEYCODE_ENTER);
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (visiveis.isEmpty()) {
                    return false;
                }
                mover(selecionado == NO_CAMPO ? 0 : Math.min(selecionado + 1, visiveis.size() - 1));
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
                if (selecionado == NO_CAMPO) {
                    return false;
                }
                if (selecionado == 0) {
                    // Subir acima da primeira linha DEVOLVE o Enter ao campo. Sem isto, quem
                    // desceu por engano ficaria com o Enter sequestrado e teria de adivinhar
                    // como sair.
                    recolher();
                    return true;
                }
                mover(selecionado - 1);
                return true;

            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                if (selecionado == NO_CAMPO) {
                    // O Enter é do campo. Este é o contrato inteiro desta classe.
                    return false;
                }
                executar(visiveis.get(selecionado));
                return true;

            case KeyEvent.KEYCODE_ESCAPE:
                if (selecionado == NO_CAMPO) {
                    return false;
                }
                recolher();
                return true;

            default:
                return false;
        }
    }

    private void executar(Comando comando) {
        recolher();
        campo.setText("");
        if (comando.acao != null) {
            comando.acao.run();
        }
    }

    private void filtrar(String texto) {
        String alvo = texto == null ? "" : texto.trim().toLowerCase(Locale.getDefault());

        visiveis.clear();
        for (Comando c : todos) {
            if (alvo.isEmpty()
                    || contexto.getString(c.rotulo).toLowerCase(Locale.getDefault()).contains(alvo)) {
                visiveis.add(c);
            }
        }

        lista.removeAllViews();
        for (Comando c : visiveis) {
            lista.addView(criarLinha(contexto.getString(c.rotulo)));
        }

        // Filtrar reinicia a seleção porque a linha de índice 2 de antes não é a mesma de
        // agora. Manter o índice seria realçar uma coisa e executar outra.
        if (selecionado != NO_CAMPO) {
            if (visiveis.isEmpty()) {
                recolher();
            } else {
                mover(0);
            }
        }
    }

    private TextView criarLinha(String texto) {
        TextView linha = new TextView(contexto);
        linha.setText(texto);
        linha.setTextColor(ContextCompat.getColor(contexto, R.color.vidro_text));
        linha.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        linha.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int recuo = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f,
                contexto.getResources().getDisplayMetrics());
        linha.setPadding(recuo, 0, recuo, 0);
        linha.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, alturaDaLinha()));
        return linha;
    }

    private int alturaDaLinha() {
        return contexto.getResources().getDimensionPixelSize(R.dimen.vidro_touch_target);
    }

    private void mover(int indice) {
        boolean primeiraDescida = selecionado == NO_CAMPO;
        selecionado = indice;

        rolagem.setVisibility(View.VISIBLE);
        realce.setVisibility(View.VISIBLE);

        ViewGroup.LayoutParams lp = realce.getLayoutParams();
        if (lp != null && lp.height != alturaDaLinha()) {
            lp.height = alturaDaLinha();
            realce.setLayoutParams(lp);
        }

        float destino = indice * alturaDaLinha();
        if (primeiraDescida) {
            // A primeira descida NÃO viaja: não há de onde vir. O realce nasce onde vai
            // ficar; animar do topo daria a impressão de que ele estava escondido ali.
            molaDoRealce.fixar(destino);
        } else {
            molaDoRealce.lancar(destino, Float.NaN);
        }

        final int alvoY = indice * alturaDaLinha();
        rolagem.post(new Runnable() {
            @Override
            public void run() {
                int topo = rolagem.getScrollY();
                int base = topo + rolagem.getHeight();
                if (alvoY < topo) {
                    rolagem.smoothScrollTo(0, alvoY);
                } else if (alvoY + alturaDaLinha() > base) {
                    rolagem.smoothScrollTo(0, alvoY + alturaDaLinha() - rolagem.getHeight());
                }
            }
        });
    }
}
