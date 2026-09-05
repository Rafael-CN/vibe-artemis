package com.limelight;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/**
 * O arranque frio do app — a Activity que o launcher abre.
 *
 * <h2>O que ela é agora, e o que ela era</h2>
 *
 * Ela era a casca inteira: 1475 linhas com descoberta de PCs, ligação ao
 * {@code ComputerManagerService}, pareamento, menu de contexto e a home. Tudo isso virou
 * {@link CascaDoApp}, que é controlador e não tela.
 *
 * <p>A separação existe por um motivo concreto, e não por gosto de arquitetura: a casca
 * precisa existir em <b>dois</b> lugares. Aqui ela é a tela inteira; dentro da {@code Game}
 * ela é uma camada por cima do vídeo — e é isso que faz sair de uma sessão deixar de matá-la.
 * Antes, sair era {@code finish()}, e {@code Game.onStop()} derruba a conexão sem condição.
 *
 * <p>O que sobrou aqui é o que só uma Activity sabe fazer: receber o ciclo de vida, receber o
 * menu de contexto do sistema, e ser o dono da janela. Tudo é repassado.
 */
public class PcView extends AppCompatActivity {

    private CascaDoApp casca;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Quem infla é o hospedeiro. O controlador só encontra as views por id.
        setContentView(R.layout.activity_pc_view);

        casca = new CascaDoApp(
                this,
                findViewById(R.id.vidroHomeCompose),
                // Aqui a sonda de GL pode rodar: ela troca o conteúdo da janela por uma
                // GLSurfaceView por um instante, e neste arranque não há nada na tela para
                // ela atrapalhar. Ver o campo `podeSondarGl`.
                true,
                // O Voltar na raiz da casca fecha o app — é o arranque frio, não há vídeo
                // atrás. Dentro da Game esta mesma saída volta para a imagem.
                this::finish);
        casca.criar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        casca.retomar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        casca.pausar();
    }

    @Override
    protected void onStop() {
        super.onStop();
        casca.parar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        casca.destruir();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        casca.aoTrocarConfiguracao(newConfig);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        casca.aoCriarMenuDeContexto(menu, v, menuInfo);
    }

    @Override
    public void onContextMenuClosed(@NonNull Menu menu) {
        super.onContextMenuClosed(menu);
        casca.aoFecharMenuDeContexto(menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        return casca.aoEscolherNoMenuDeContexto(item) || super.onContextItemSelected(item);
    }
}
