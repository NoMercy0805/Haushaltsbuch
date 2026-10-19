package com.schiwy.haushaltsbuch;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Modelldatei liegt ausschließlich im privaten App-Speicher.
 * JavaScript kann keinen beliebigen Dateipfad an die Engine übergeben.
 */
public final class LocalAiBridge {
    private static final long MIN_MODEL_BYTES = 100L * 1024L * 1024L;
    private static final long MAX_MODEL_BYTES = 6L * 1024L * 1024L * 1024L;
    private final MainActivity activity;
    private final WebView webView;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean importing = new AtomicBoolean(false);
    private final AtomicBoolean generating = new AtomicBoolean(false);

    public LocalAiBridge(MainActivity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
    }

    private File modelFile() {
        return new File(new File(activity.getFilesDir(), "local-ai"), "model.litertlm");
    }

    @JavascriptInterface
    public boolean hasModel() {
        File file = modelFile();
        return file.isFile() && file.length() >= MIN_MODEL_BYTES;
    }

    @JavascriptInterface
    public void importModel() {
        if (!importing.get() && !generating.get()) {
            main.post(new Runnable() {
                @Override public void run() { activity.launchAiModelPicker(); }
            });
        }
    }

    private void js(String method, String a, String b, String c) {
        // Alles aus URI, Modell oder Fehlertext wird als JSON-String escaped.
        final String script = "window." + method + " && window." + method + "("
                + JSONObject.quote(a == null ? "" : a) + ","
                + JSONObject.quote(b == null ? "" : b) + ","
                + JSONObject.quote(c == null ? "" : c) + ");";
        main.post(new Runnable() {
            @Override public void run() { webView.evaluateJavascript(script, null); }
        });
    }

    void cancelledImport() {
        js("__aiModelEvent", "cancel", "", "");
    }

    void importSelectedModel(final Uri uri) {
        if (uri == null || !importing.compareAndSet(false, true)) return;
        executor.execute(new Runnable() {
            @Override public void run() {
                File destination = modelFile();
                File dir = destination.getParentFile();
                File temp = new File(dir, "model-import.tmp");
                try {
                    if (!dir.exists() && !dir.mkdirs()) throw new Exception("App-Speicher konnte nicht angelegt werden.");
                    long total = 0L;
                    long lastNotice = 0L;
                    try (InputStream in = activity.getContentResolver().openInputStream(uri);
                         FileOutputStream out = new FileOutputStream(temp)) {
                        if (in == null) throw new Exception("Modell-Datei konnte nicht geöffnet werden.");
                        byte[] buffer = new byte[1024 * 1024];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            if (read == 0) continue;
                            total += read;
                            if (total > MAX_MODEL_BYTES) throw new Exception("Modell-Datei ist größer als 6 GB.");
                            out.write(buffer, 0, read);
                            if (total - lastNotice > 64L * 1024L * 1024L) {
                                lastNotice = total;
                                js("__aiModelEvent", "progress", (total / (1024L * 1024L)) + " MB übertragen", "");
                            }
                        }
                        out.getFD().sync();
                    }
                    if (total < MIN_MODEL_BYTES) throw new Exception("Datei zu klein. Bitte die originale .litertlm-Datei auswählen.");
                    if (destination.exists() && !destination.delete()) throw new Exception("Altes Modell konnte nicht ersetzt werden.");
                    if (!temp.renameTo(destination)) throw new Exception("Modell konnte nicht gespeichert werden.");
                    js("__aiModelEvent", "done", "", "");
                } catch (Throwable error) {
                    if (temp.exists()) temp.delete();
                    js("__aiModelEvent", "error", message(error), "");
                } finally {
                    importing.set(false);
                }
            }
        });
    }

    @JavascriptInterface
    public void generate(final String prompt, final String requestId) {
        if (prompt == null || prompt.trim().isEmpty() || prompt.length() > 200000
                || requestId == null || requestId.length() > 80) {
            js("__aiLocalResult", requestId, "", "Ungültige Anfrage.");
            return;
        }
        if (!hasModel()) {
            js("__aiLocalResult", requestId, "", "Kein importiertes Modell vorhanden.");
            return;
        }
        if (!generating.compareAndSet(false, true)) {
            js("__aiLocalResult", requestId, "", "Die lokale KI bearbeitet bereits eine Auswertung.");
            return;
        }
        executor.execute(new Runnable() {
            @Override public void run() {
                try {
                    String response = GemmaRunner.generate(modelFile().getAbsolutePath(), prompt);
                    js("__aiLocalResult", requestId, response, "");
                } catch (Throwable error) {
                    js("__aiLocalResult", requestId, "", message(error));
                } finally {
                    generating.set(false);
                }
            }
        });
    }

    private static String message(Throwable error) {
        String msg = error.getMessage();
        return msg == null || msg.trim().isEmpty() ? error.getClass().getSimpleName() : msg;
    }

    void shutdown() { executor.shutdown(); }
}
