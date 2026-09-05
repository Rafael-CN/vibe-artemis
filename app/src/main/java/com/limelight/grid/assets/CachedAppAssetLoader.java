package com.limelight.grid.assets;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.limelight.R;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CachedAppAssetLoader {
    /**
     * Avisa quem monta a grade da proporção em que o host publicou a box art.
     *
     * O card assume essa proporção para a capa preencher a caixa inteira — sem corte e sem
     * faixa de fundo sobrando nas beiradas. Só o carregador sabe o tamanho original da
     * arte: o bitmap que chega à ImageView já foi reamostrado.
     */
    public interface ArtAspectListener {
        void onArtAspectMeasured(int width, int height);

        /**
         * Esta arte cobre a caixa da capa inteira, sem sobra nas beiradas?
         *
         * Só quem monta a grade sabe: a proporção da caixa é UMA para todos os cards. A
         * resposta decide se o fundo listrado do placeholder pode ser largado — arte que
         * sobra beirada ainda precisa dele atrás.
         */
        boolean artFillsCard(int width, int height);
    }

    private static final int MAX_CONCURRENT_DISK_LOADS = 3;
    private static final int MAX_CONCURRENT_NETWORK_LOADS = 3;
    private static final int MAX_CONCURRENT_CACHE_LOADS = 1;

    private static final int MAX_PENDING_CACHE_LOADS = 100;
    private static final int MAX_PENDING_NETWORK_LOADS = 40;
    private static final int MAX_PENDING_DISK_LOADS = 40;

    private final ThreadPoolExecutor cacheExecutor = new ThreadPoolExecutor(
            MAX_CONCURRENT_CACHE_LOADS, MAX_CONCURRENT_CACHE_LOADS,
            Long.MAX_VALUE, TimeUnit.DAYS,
            new LinkedBlockingQueue<Runnable>(MAX_PENDING_CACHE_LOADS),
            new ThreadPoolExecutor.DiscardOldestPolicy());

    private final ThreadPoolExecutor foregroundExecutor = new ThreadPoolExecutor(
            MAX_CONCURRENT_DISK_LOADS, MAX_CONCURRENT_DISK_LOADS,
            Long.MAX_VALUE, TimeUnit.DAYS,
            new LinkedBlockingQueue<Runnable>(MAX_PENDING_DISK_LOADS),
            new ThreadPoolExecutor.DiscardOldestPolicy());

    private final ThreadPoolExecutor networkExecutor = new ThreadPoolExecutor(
            MAX_CONCURRENT_NETWORK_LOADS, MAX_CONCURRENT_NETWORK_LOADS,
            Long.MAX_VALUE, TimeUnit.DAYS,
            new LinkedBlockingQueue<Runnable>(MAX_PENDING_NETWORK_LOADS),
            new ThreadPoolExecutor.DiscardOldestPolicy());

    private final ComputerDetails computer;
    private final double scalingDivider;
    private final NetworkAssetLoader networkLoader;
    private final MemoryAssetLoader memoryLoader;
    private final DiskAssetLoader diskLoader;
    private final Bitmap placeholderBitmap;
    private final Bitmap noAppImageBitmap;

    private ArtAspectListener artAspectListener;

    public CachedAppAssetLoader(ComputerDetails computer, double scalingDivider,
                                NetworkAssetLoader networkLoader, MemoryAssetLoader memoryLoader,
                                DiskAssetLoader diskLoader, Bitmap noAppImageBitmap) {
        this.computer = computer;
        this.scalingDivider = scalingDivider;
        this.networkLoader = networkLoader;
        this.memoryLoader = memoryLoader;
        this.diskLoader = diskLoader;
        this.noAppImageBitmap = noAppImageBitmap;
        this.placeholderBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }

    public void cancelBackgroundLoads() {
        Runnable r;
        while ((r = cacheExecutor.getQueue().poll()) != null) {
            cacheExecutor.remove(r);
        }
    }

    public void cancelForegroundLoads() {
        Runnable r;

        while ((r = foregroundExecutor.getQueue().poll()) != null) {
            foregroundExecutor.remove(r);
        }

        while ((r = networkExecutor.getQueue().poll()) != null) {
            networkExecutor.remove(r);
        }
    }

    public void setArtAspectListener(ArtAspectListener listener) {
        this.artAspectListener = listener;
    }

    /**
     * Chamado só da thread da UI, dos dois pontos em que uma capa real vai para a tela.
     *
     * Placeholder do GFE não conta: tem tamanho próprio e daria a forma da grade a um asset
     * que nem é capa de app.
     */
    private void reportArtAspect(ScaledBitmap bmp) {
        if (artAspectListener != null && bmp != null && !isBitmapPlaceholder(bmp)) {
            artAspectListener.onArtAspectMeasured(bmp.originalWidth, bmp.originalHeight);
        }
    }

    public void freeCacheMemory() {
        memoryLoader.clearCache();
    }

    private ScaledBitmap doNetworkAssetLoad(LoaderTuple tuple, LoaderTask task) {
        // Try 3 times
        for (int i = 0; i < 3; i++) {
            // Check again whether we've been cancelled or the image view is gone
            if (task != null && (task.isCancelled() || task.imageViewRef.get() == null)) {
                return null;
            }

            InputStream in = networkLoader.getBitmapStream(tuple);
            if (in != null) {
                // Write the stream straight to disk
                diskLoader.populateCacheWithStream(tuple, in);

                // Close the network input stream
                try {
                    in.close();
                } catch (IOException ignored) {}

                // If there's a task associated with this load, we should return the bitmap
                if (task != null) {
                    // If the cached bitmap is valid, return it. Otherwise, we'll try the load again
                    ScaledBitmap bmp = diskLoader.loadBitmapFromCache(tuple, (int) scalingDivider);
                    if (bmp != null) {
                        return bmp;
                    }
                }
                else {
                    // Otherwise it's a background load and we return nothing
                    return null;
                }
            }

            // Wait 1 second with a bit of fuzz
            try {
                Thread.sleep((int) (1000 + (Math.random() * 500)));
            } catch (InterruptedException e) {
                e.printStackTrace();

                // InterruptedException clears the thread's interrupt status. Since we can't
                // handle that here, we will re-interrupt the thread to set the interrupt
                // status back to true.
                Thread.currentThread().interrupt();

                return null;
            }
        }

        return null;
    }

    private class LoaderTask extends AsyncTask<LoaderTuple, Void, ScaledBitmap> {
        private final WeakReference<ImageView> imageViewRef;
        private final WeakReference<TextView> textViewRef;
        private final boolean diskOnly;

        private LoaderTuple tuple;

        public LoaderTask(ImageView imageView, TextView textView, boolean diskOnly) {
            this.imageViewRef = new WeakReference<>(imageView);
            this.textViewRef = new WeakReference<>(textView);
            this.diskOnly = diskOnly;
        }

        @Override
        protected ScaledBitmap doInBackground(LoaderTuple... params) {
            tuple = params[0];

            // Check whether it has been cancelled or the views are gone
            if (isCancelled() || imageViewRef.get() == null || textViewRef.get() == null) {
                return null;
            }

            ScaledBitmap bmp = diskLoader.loadBitmapFromCache(tuple, (int) scalingDivider);
            if (bmp == null) {
                if (!diskOnly) {
                    // Try to load the asset from the network
                    bmp = doNetworkAssetLoad(tuple, this);
                } else {
                    // Report progress to display the placeholder and spin
                    // off the network-capable task
                    publishProgress();
                }
            }

            // Cache the bitmap
            if (bmp != null) {
                memoryLoader.populateCache(tuple, bmp);
            }

            return bmp;
        }

        @Override
        protected void onProgressUpdate(Void... nothing) {
            // Do nothing if cancelled
            if (isCancelled()) {
                return;
            }

            // If the current loader task for this view isn't us, do nothing
            final ImageView imageView = imageViewRef.get();
            final TextView textView = textViewRef.get();
            if (getLoaderTask(imageView) == this) {
                // Set off another loader task on the network executor. This time our AsyncDrawable
                // will use the app image placeholder bitmap, rather than an empty bitmap.
                LoaderTask task = new LoaderTask(imageView, textView, false);
                AsyncDrawable asyncDrawable = new AsyncDrawable(imageView.getResources(), noAppImageBitmap, task);
                imageView.setImageDrawable(asyncDrawable);
                imageView.startAnimation(AnimationUtils.loadAnimation(imageView.getContext(), R.anim.boxart_fadein));
                imageView.setVisibility(View.VISIBLE);
                textView.setVisibility(View.VISIBLE);
                task.executeOnExecutor(networkExecutor, tuple);
            }
        }

        @Override
        protected void onPostExecute(final ScaledBitmap bitmap) {
            // Do nothing if cancelled
            if (isCancelled()) {
                return;
            }

            final ImageView imageView = imageViewRef.get();
            final TextView textView = textViewRef.get();
            if (getLoaderTask(imageView) == this) {
                // Fade in the box art
                if (bitmap != null) {
                    // Show the text if it's a placeholder
                    boolean placeholder = isBitmapPlaceholder(bitmap);
                    textView.setVisibility(placeholder ? View.VISIBLE : View.GONE);

                    // Antes de perguntar se a arte preenche o card: é aqui que a grade
                    // aprende a proporção, se esta for a primeira capa a carregar.
                    reportArtAspect(bitmap);
                    final boolean largarFundo = !placeholder && artFillsCard(bitmap);

                    if (imageView.getVisibility() == View.VISIBLE) {
                        // Fade out the placeholder first
                        Animation fadeOutAnimation = AnimationUtils.loadAnimation(imageView.getContext(), R.anim.boxart_fadeout);
                        fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {}

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                // Durante o fade a grade pode ter reciclado esta view para
                                // outro app. Aí quem manda nela é outra tarefa, e largar o
                                // fundo daqui deixaria o card do outro app sem o listrado
                                // enquanto a capa dele ainda carrega. A pergunta vem antes
                                // do setImageBitmap, que é o que apaga o AsyncDrawable.
                                boolean aindaNossa = getLoaderTask(imageView) == LoaderTask.this;

                                // Fade in the new box art
                                imageView.setImageBitmap(bitmap.bitmap);
                                if (largarFundo && aindaNossa) {
                                    setPlaceholderBackground(imageView, false);
                                }
                                imageView.startAnimation(AnimationUtils.loadAnimation(imageView.getContext(), R.anim.boxart_fadein));
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {}
                        });
                        imageView.startAnimation(fadeOutAnimation);
                    }
                    else {
                        // View is invisible already, so just fade in the new art
                        imageView.setImageBitmap(bitmap.bitmap);
                        if (largarFundo) {
                            setPlaceholderBackground(imageView, false);
                        }
                        imageView.startAnimation(AnimationUtils.loadAnimation(imageView.getContext(), R.anim.boxart_fadein));
                        imageView.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<LoaderTask> loaderTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             LoaderTask loaderTask) {
            super(res, bitmap);
            loaderTaskReference = new WeakReference<>(loaderTask);
        }

        public LoaderTask getLoaderTask() {
            return loaderTaskReference.get();
        }
    }

    private static LoaderTask getLoaderTask(ImageView imageView) {
        if (imageView == null) {
            return null;
        }

        final Drawable drawable = imageView.getDrawable();

        // If our drawable is in play, get the loader task
        if (drawable instanceof AsyncDrawable) {
            final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
            return asyncDrawable.getLoaderTask();
        }

        return null;
    }

    private static boolean cancelPendingLoad(LoaderTuple tuple, ImageView imageView) {
        final LoaderTask loaderTask = getLoaderTask(imageView);

        // Check if any task was pending for this image view
        if (loaderTask != null && !loaderTask.isCancelled()) {
            final LoaderTuple taskTuple = loaderTask.tuple;

            // Cancel the task if it's not already loading the same data
            if (taskTuple == null || !taskTuple.equals(tuple)) {
                loaderTask.cancel(true);
            } else {
                // It's already loading what we want
                return false;
            }
        }

        // Allow the load to proceed
        return true;
    }

    public void queueCacheLoad(NvApp app) {
        final LoaderTuple tuple = new LoaderTuple(computer, app);

        if (memoryLoader.loadBitmapFromCache(tuple) != null) {
            // It's in memory which means it must also be on disk
            return;
        }

        // Queue a fetch in the cache executor
        cacheExecutor.execute(new Runnable() {
            @Override
            public void run() {
                // Check if the image is cached on disk
                if (diskLoader.checkCacheExists(tuple)) {
                    return;
                }

                // Try to load the asset from the network and cache result on disk
                doNetworkAssetLoad(tuple, null);
            }
        });
    }

    /**
     * O fundo listrado da ImageView da capa.
     *
     * Ele existe para o card SEM capa — o lugar continua lá, com o nome por cima. Quando a
     * box art real entra, ela cobre esse fundo 100% e ele nunca mais sai: overdraw
     * permanente em toda a grade, e num Tab S8 Ultra deitado são seis colunas de cards
     * visíveis o tempo todo enquanto se rola o catálogo.
     *
     * Sai quando a arte real entra e volta no próximo bind, por onde toda view reciclada
     * passa. É pintado em UM ponto por bind, e não zerado-e-reposto: setBackground(null)
     * limpa o registro de qual recurso está lá, então alternar os dois na mesma passada
     * custaria duas remedidas por card.
     */
    private static void setPlaceholderBackground(ImageView imgView, boolean visible) {
        if (visible) {
            imgView.setBackgroundResource(R.drawable.vidro_placeholder);
        }
        else {
            imgView.setBackground(null);
        }
    }

    /**
     * A arte preenche a caixa do card? Sem quem responda — grade sem listener —, a
     * resposta é não, e o fundo fica onde estava.
     */
    private boolean artFillsCard(ScaledBitmap bitmap) {
        return artAspectListener != null
                && bitmap != null
                && artAspectListener.artFillsCard(bitmap.originalWidth, bitmap.originalHeight);
    }

    private boolean isBitmapPlaceholder(ScaledBitmap bitmap) {
        return (bitmap == null) ||
                (bitmap.originalWidth == 130 && bitmap.originalHeight == 180) || // GFE 2.0
                (bitmap.originalWidth == 628 && bitmap.originalHeight == 888); // GFE 3.0
    }

    public boolean populateImageView(NvApp app, ImageView imgView, TextView textView) {
        LoaderTuple tuple = new LoaderTuple(computer, app);

        // If there's already a task in progress for this view,
        // cancel it. If the task is already loading the same image,
        // we return and let that load finish.
        if (!cancelPendingLoad(tuple, imgView)) {
            return true;
        }

        // Always set the name text so we have it if needed later
        textView.setText(app.getAppName());

        // First, try the memory cache in the current context
        ScaledBitmap bmp = memoryLoader.loadBitmapFromCache(tuple);
        if (bmp != null) {
            // Show the bitmap immediately
            imgView.setVisibility(View.VISIBLE);
            imgView.setImageBitmap(bmp.bitmap);

            // Show the text if it's a placeholder bitmap
            boolean placeholder = isBitmapPlaceholder(bmp);
            textView.setVisibility(placeholder ? View.VISIBLE : View.GONE);

            // reportArtAspect vem ANTES: é ele que ensina a grade a proporção da primeira
            // capa, e é contra essa proporção que artFillsCard responde.
            reportArtAspect(bmp);
            setPlaceholderBackground(imgView, placeholder || !artFillsCard(bmp));
            return true;
        }

        // If it's not in memory, create an async task to load it. This task will be attached
        // via AsyncDrawable to this view.
        final LoaderTask task = new LoaderTask(imgView, textView, true);
        final AsyncDrawable asyncDrawable = new AsyncDrawable(imgView.getResources(), placeholderBitmap, task);
        textView.setVisibility(View.INVISIBLE);
        imgView.setVisibility(View.INVISIBLE);
        imgView.setImageDrawable(asyncDrawable);

        // Enquanto a capa não chega o lugar dela é o fundo listrado. A view pode vir
        // reciclada de um card que já tinha capa e estar sem fundo nenhum.
        setPlaceholderBackground(imgView, true);

        // Run the task on our foreground executor
        task.executeOnExecutor(foregroundExecutor, tuple);
        return false;
    }

    public static class LoaderTuple {
        public final ComputerDetails computer;
        public final NvApp app;

        public LoaderTuple(ComputerDetails computer, NvApp app) {
            this.computer = computer;
            this.app = app;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof LoaderTuple)) {
                return false;
            }

            LoaderTuple other = (LoaderTuple) o;
            return computer.uuid.equals(other.computer.uuid) && app.getAppId() == other.app.getAppId();
        }

        @Override
        public String toString() {
            return "("+computer.uuid+", "+app.getAppId()+")";
        }
    }
}
