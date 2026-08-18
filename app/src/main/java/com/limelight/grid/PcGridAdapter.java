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

        txtView.setText(obj.details.name);

        TextView stateView = parentView.findViewById(R.id.grid_state);
        boolean probing = state == ComputerDetails.State.UNKNOWN;

        // Enquanto o polling não respondeu não afirmamos nada: some a palavra e fica só
        // o giro. Dizer "fora do ar" antes de saber é a mentira mais fácil desta tela.
        prgView.setVisibility(probing ? View.VISIBLE : View.GONE);
        if (stateView != null) {
            stateView.setVisibility(probing ? View.GONE : View.VISIBLE);
        }

        if (state == ComputerDetails.State.ONLINE) {
            imgView.setImageResource(R.drawable.vidro_dot_on);
            if (stateView != null) {
                stateView.setText(paired ? R.string.vidro_state_ready : R.string.vidro_state_unpaired);
            }
        }
        else if (probing) {
            imgView.setImageResource(R.drawable.vidro_dot_off);
        }
        else {
            // Dormindo e fora do ar são estados diferentes para quem está olhando: um
            // pode ser acordado daqui, o outro não. O que separa os dois é ter MAC
            // guardado, que é o que o Wake-on-LAN precisa.
            boolean canWake = obj.details.macAddress != null && !obj.details.macAddress.isEmpty();
            imgView.setImageResource(canWake ? R.drawable.vidro_dot_off : R.drawable.vidro_dot_far);
            if (stateView != null) {
                stateView.setText(canWake ? R.string.vidro_state_asleep : R.string.vidro_state_offline);
            }
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
