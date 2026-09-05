package com.limelight.ui;

import android.content.Context;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.limelight.R;

/**
 * Aviso passageiro na identidade Vidro.
 *
 * Substitui Toast.makeText nos pontos em que o app fala com a pessoa de passagem. O
 * Toast do sistema desenha uma cápsula de cinza neutro com a tipografia do Android e o
 * ícone do app ao lado — e ele aparecia por cima da sessão, do catálogo e da home, que é
 * onde a identidade custou mais para ser construída.
 *
 * Sobre a view customizada: ela é depreciada desde a API 30 e BLOQUEADA para app em
 * segundo plano desde o Android 12. Aqui isso não morde, porque todo aviso deste app
 * nasce de um toque — o app está em primeiro plano por definição. Ainda assim, se o
 * sistema recusar a view, o texto continua aparecendo: o fallback é o Toast comum, e é
 * melhor um aviso cinza do que aviso nenhum.
 */
public final class VidroToast {

    private VidroToast() {
    }

    public static void show(Context context, CharSequence text) {
        show(context, text, Toast.LENGTH_SHORT);
    }

    public static void show(Context context, int textResId) {
        show(context, context.getResources().getText(textResId), Toast.LENGTH_SHORT);
    }

    public static void show(Context context, int textResId, int duration) {
        show(context, context.getResources().getText(textResId), duration);
    }

    public static void show(Context context, CharSequence text, int duration) {
        if (context == null || text == null) {
            return;
        }

        Toast toast = Toast.makeText(context, text, duration);

        try {
            View view = LayoutInflater.from(context).inflate(R.layout.vidro_toast, null);
            ((TextView) view.findViewById(R.id.vidroToastText)).setText(text);
            toast.setView(view);

            // AVISO MORA NO TOPO, e ele mudou de lugar por uma colisão medida.
            //
            // No rodapé ele caía exatamente sobre o que estava ancorado ali: na gravação de
            // 26/08, "Desktop encerrado" apareceu por cima de "Reservando a tela no PC", na
            // tela de espera, tapando o único texto que explicava o que estava acontecendo.
            // E o rodapé é disputado por três coisas — o passo da espera, o rodapé da home e
            // a barra de composição.
            //
            // No topo ele não disputa com nada, e a regra da identidade já dizia isso desde
            // sempre: aviso não se toca, e o que não se toca não precisa estar ao alcance do
            // polegar.
            int offset = Math.round(72 * context.getResources().getDisplayMetrics().density);
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, offset);
        } catch (Exception e) {
            // Fica com o Toast comum, já construído acima.
        }

        toast.show();
    }

    /**
     * Só para deixar explícito de onde vem o limite: em API 30+ a view customizada é
     * ignorada quando o app está em segundo plano. Nenhum aviso deste app é disparado
     * dali, mas quem for mexer merece saber sem ter que procurar.
     */
    public static boolean customViewMayBeIgnored() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
    }
}
