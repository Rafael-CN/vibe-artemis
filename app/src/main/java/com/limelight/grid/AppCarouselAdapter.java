package com.limelight.grid;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.limelight.AppView;
import com.limelight.R;

/**
 * Fileira horizontal de capas da Início (tablet) — B-CONSOLE.md §F3.
 *
 * Não é um adapter de carregamento novo: é um invólucro fino em torno de um
 * {@link AppGridAdapter} já existente, que continua sendo o único dono do caminho de
 * dados (lista de apps, {@link com.limelight.grid.assets.CachedAppAssetLoader}, cache
 * em memória/disco). Cada {@link ViewHolder} é uma View inflada de
 * {@code console_capa_carousel_item.xml}; o bind delega para
 * {@code AppGridAdapter#getView(int, View, ViewGroup)} passando essa mesma View como
 * {@code convertView} — é o mecanismo de reuso de {@link GenericGridAdapter} (só
 * preenche os ids que já existem na View, nunca reinfla) — então toda a lógica de
 * "achar a capa em cache, senão carregar, senão placeholder" roda exatamente igual à
 * grade vertical do celular. Nada daquilo é duplicado aqui.
 *
 * O que este adapter FAZ acrescentar, porque a fileira horizontal com foco de D-pad
 * não existe em {@link AppGridAdapter}: a escala 1,2x no foco (NOTA.md §2) e o aviso
 * de qual app está focado/ativado para quem desenha "nome + Abrir" abaixo da fileira.
 */
public class AppCarouselAdapter extends RecyclerView.Adapter<AppCarouselAdapter.ViewHolder> {

    /** Duração da escala de foco: 120ms, sem mola (ViewPropertyAnimator simples). */
    private static final long FOCUS_SCALE_DURATION_MS = 120;
    private static final float FOCUS_SCALE = 1.2f;

    public interface Listener {
        /** Chamado quando um item ganha foco (teclado/D-pad) ou é tocado. */
        void onAppFocused(AppView.AppObject app);

        /** Chamado quando o usuário ativa o item (toque ou Enter/D-pad center). */
        void onAppActivated(AppView.AppObject app);
    }

    private final AppGridAdapter appGridAdapter;
    private final Listener listener;

    // Sem isto o primeiro DPAD_RIGHT depois de a fileira aparecer não move nada: uma
    // RecyclerView recém-montada não tem foco nenhum dentro dela (Activity abre em
    // modo de toque), e um ViewGroup.requestFocus() de fora nem sempre desce até o
    // item — depende de layoutManager já ter posicionado os filhos. Pedir o foco no
    // próprio onBindViewHolder do primeiro item, uma vez só, é o ponto em que a View
    // garantidamente já existe e está anexada.
    private boolean focoInicialPedido;

    public AppCarouselAdapter(AppGridAdapter appGridAdapter, Listener listener) {
        this.appGridAdapter = appGridAdapter;
        this.listener = listener;

        // O layout padrão de AppGridAdapter (app_grid_item, grade vertical do celular)
        // não serve para a fileira: aqui a capa é console_capa_w/h e não tem nome por
        // baixo. setLayoutId é package-private de propósito — só quem já entende o
        // contrato de GenericGridAdapter (mesmo pacote) deveria trocar isso.
        appGridAdapter.setLayoutId(R.layout.console_capa_carousel_item);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.console_capa_carousel_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // convertView não-nulo: GenericGridAdapter#getView só faz findViewById +
        // populateView nele, sem reinflar (o terceiro parâmetro só importa quando
        // precisa inflar, o que não é o caso aqui). É o mesmo caminho que o GridView
        // do celular percorre a cada reciclagem de item.
        appGridAdapter.getView(position, holder.itemView, null);

        final AppView.AppObject app = (AppView.AppObject) appGridAdapter.getItem(position);

        holder.itemView.setOnClickListener(v -> {
            listener.onAppFocused(app);
            listener.onAppActivated(app);
        });

        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            v.animate()
                    .scaleX(hasFocus ? FOCUS_SCALE : 1f)
                    .scaleY(hasFocus ? FOCUS_SCALE : 1f)
                    .setDuration(FOCUS_SCALE_DURATION_MS)
                    .start();

            if (hasFocus) {
                listener.onAppFocused(app);
            }
        });

        if (position == 0 && !focoInicialPedido) {
            focoInicialPedido = true;
            holder.itemView.post(() -> holder.itemView.requestFocus());
        }
    }

    @Override
    public int getItemCount() {
        return appGridAdapter.getCount();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
