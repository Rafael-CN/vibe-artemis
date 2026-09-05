package com.limelight.grid;

import com.limelight.nvstream.http.NvApp;

/**
 * Um aplicativo publicado por um host, com o que a grade precisa saber além do nome.
 *
 * Morava dentro de {@code AppView} como classe aninhada, porque naquele tempo a tela e o
 * modelo eram a mesma coisa. Quando a tela deixou de ser uma Activity — para o nome da máquina
 * poder viajar da lista até o título do catálogo — esta classe foi a única que segurava a
 * antiga de pé, referenciada por {@code AppGridAdapter}.
 *
 * Mudou de lugar e mais nada: os três campos, o construtor e o {@code toString} são os
 * mesmos. Fica em {@code grid} porque é de lá que ela é usada.
 */
public class AppObject {
    public final NvApp app;
    public boolean isRunning;
    public boolean isHidden;

    public AppObject(NvApp app) {
        if (app == null) {
            throw new IllegalArgumentException("app must not be null");
        }
        this.app = app;
    }

    @Override
    public String toString() {
        return app.getAppName();
    }
}
