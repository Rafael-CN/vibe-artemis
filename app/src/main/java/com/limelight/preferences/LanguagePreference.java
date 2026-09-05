package com.limelight.preferences;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;

public class LanguagePreference extends ListPreference {

    public LanguagePreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public LanguagePreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public LanguagePreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LanguagePreference(@NonNull Context context) {
        super(context);
    }

    /**
     * No Android 13+ o idioma de um app é assunto do sistema: existe uma página por app,
     * alimentada pelo android:localeConfig que o manifesto já declara, e é lá que a pessoa
     * espera encontrar a escolha — junto com as dos outros apps, e sobrevivendo a
     * reinstalação. Uma segunda lista dentro dos Ajustes seria uma cópia que envelhece
     * sozinha e que só o nosso app respeita.
     *
     * Abaixo do 13 essa página não existe, e o minSdk aqui é 21: o diálogo antigo continua
     * sendo o único caminho, alimentado pelas entries declaradas em preferences.xml. É por
     * isso que aquelas listas não puderam ser apagadas junto com esta.
     */
    @Override
    protected void onClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                Intent intent = new Intent(Settings.ACTION_APP_LOCALE_SETTINGS);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("package:" + getContext().getPackageName()));
                getContext().startActivity(intent, null);
                return;
            } catch (ActivityNotFoundException e) {
                // A página de idioma por app deveria existir em todo Android 13, mas se o
                // fabricante a removeu ainda há para onde cair.
            }
        }

        super.onClick();
    }
}
