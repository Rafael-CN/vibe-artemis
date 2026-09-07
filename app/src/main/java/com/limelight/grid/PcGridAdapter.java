package com.limelight.grid;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.limelight.PcView;
import com.limelight.R;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.PairingManager;
import com.limelight.preferences.PreferenceConfiguration;

import java.util.Collections;
import java.util.Comparator;

public class PcGridAdapter extends GenericGridAdapter<PcView.ComputerObject> {

    public PcGridAdapter(Context context, PreferenceConfiguration prefs) {
        super(context, getLayoutIdForPreferences(prefs));
    }

    private static int getLayoutIdForPreferences(PreferenceConfiguration prefs) {
        return R.layout.pc_grid_item;
    }

    public void updateLayoutWithPreferences(Context context, PreferenceConfiguration prefs) {
        // This will trigger the view to reload with the new layout
        setLayoutId(getLayoutIdForPreferences(prefs));
    }

    public void addComputer(PcView.ComputerObject computer) {
        itemList.add(computer);
        sortList();
    }

    private void sortList() {
        Collections.sort(itemList, new Comparator<PcView.ComputerObject>() {
            @Override
            public int compare(PcView.ComputerObject lhs, PcView.ComputerObject rhs) {
                return lhs.details.name.toLowerCase().compareTo(rhs.details.name.toLowerCase());
            }
        });
    }

    public boolean removeComputer(PcView.ComputerObject computer) {
        return itemList.remove(computer);
    }

    @Override
    public void populateView(View parentView, ImageView imgView, RelativeLayout gridMask, ProgressBar prgView, TextView txtView, ImageView overlayView, PcView.ComputerObject obj) {
        Context context = parentView.getContext();
        ComputerDetails.State state = obj.details.state;
        boolean paired = obj.details.pairState == PairingManager.PairState.PAIRED;
        boolean online = state == ComputerDetails.State.ONLINE;

        txtView.setText(obj.details.name);

        TextView stateView = parentView.findViewById(R.id.grid_state);
        boolean probing = state == ComputerDetails.State.UNKNOWN;

        // Enquanto o polling não respondeu não afirmamos nada: some a palavra e fica só
        // o giro. Dizer "fora do ar" antes de saber é a mentira mais fácil desta tela.
        prgView.setVisibility(probing ? View.VISIBLE : View.GONE);
        if (stateView != null) {
            stateView.setVisibility(probing ? View.GONE : View.VISIBLE);
        }

        // O ponto NUNCA muda de cor por estado (NOTA.md §2 / B-CONSOLE.md §F3): acento
        // só quando on-line e emparelhado, console_texto_2 em qualquer outro caso. O
        // rótulo ao lado é quem diz a diferença — "fora do ar" tanto para o PC que dorme
        // (tem MAC, WOL acorda) quanto para o que só está inacessível; o menu de
        // contexto (Wake on LAN) continua distinguindo os dois por baixo, só o texto
        // desta linha uniu no vocabulário literal do protótipo.
        if (online && paired) {
            imgView.setImageResource(R.drawable.vidro_dot_on);
            if (stateView != null) {
                stateView.setText(R.string.console_estado_online);
            }
        }
        else {
            imgView.setImageResource(R.drawable.console_dot_neutro);
            if (stateView != null) {
                stateView.setText(online ? R.string.console_estado_emparelhar : R.string.console_estado_offline);
            }
        }

        // Endereço em mono quando on-line; "endereço indisponível" fora do ar — nunca
        // um IP que pode já não valer mais nada.
        TextView enderecoView = parentView.findViewById(R.id.consolePcEndereco);
        if (enderecoView != null) {
            if (online && obj.details.activeAddress != null) {
                enderecoView.setText(obj.details.activeAddress.address);
            } else {
                enderecoView.setText(R.string.console_endereco_indisponivel);
            }
        }

        // O chevron só existe quando há algo do outro lado para abrir — PC on-line e
        // emparelhado. Ele e o cadeado (abaixo) e o giro de "procurando" nunca
        // aparecem ao mesmo tempo: os três estados são mutuamente exclusivos.
        ImageView chevronView = parentView.findViewById(R.id.consolePcChevron);
        if (chevronView != null) {
            chevronView.setVisibility(online && paired ? View.VISIBLE : View.GONE);
        }

        // O cadeado só aparece quando há o que destrancar.
        if (state == ComputerDetails.State.ONLINE && !paired) {
            overlayView.setImageResource(R.drawable.ic_lock);
            overlayView.setVisibility(View.VISIBLE);
        }
        else {
            overlayView.setVisibility(View.GONE);
        }

        // A linha inteira recua quando o lugar não está acessível — é a mesma gramática
        // da luz: intensidade dizendo estado, sem trocar de cor.
        parentView.setAlpha(state == ComputerDetails.State.ONLINE ? 1.0f : 0.55f);
    }
}
