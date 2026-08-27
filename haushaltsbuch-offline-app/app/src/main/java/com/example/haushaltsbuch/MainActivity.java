package com.example.haushaltsbuch;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView webView;
    private WebAppInterface bridge;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST = 777;
    private static final int FOLDER_PICKER_REQUEST = 778;
    private static final int STORAGE_PERMISSION_REQUEST = 502;

    private static final String DOWNLOAD_HOOK_JS =
        "(function(){" +
        "  if(window.__androidDownloadHookInstalled) return;" +
        "  window.__androidDownloadHookInstalled = true;" +
        "  document.addEventListener('click', function(e){" +
        "    var el = e.target;" +
        "    while(el && el.tagName !== 'A'){ el = el.parentElement; }" +
        "    if(!el || !el.hasAttribute('download')) return;" +
        "    var href = el.getAttribute('href') || '';" +
        "    if(href.indexOf('blob:') !== 0) return;" +
        "    if(!window.AndroidBridge || typeof window.AndroidBridge.saveTextFile !== 'function') return;" +
        "    e.preventDefault();" +
        "    e.stopPropagation();" +
        "    var filename = el.getAttribute('download') || 'download';" +
        "    fetch(href).then(function(r){ return r.blob(); }).then(function(blob){" +
        "      var reader = new FileReader();" +
        "      reader.onloadend = function(){" +
        "        var dataUrl = reader.result || '';" +
        "        var base64 = dataUrl.indexOf(',') >= 0 ? dataUrl.split(',')[1] : '';" +
        "        window.AndroidBridge.saveTextFile(base64, filename, blob.type || 'application/octet-stream');" +
        "      };" +
        "      reader.readAsDataURL(blob);" +
        "    }).catch(function(err){ alert('Speichern fehlgeschlagen: ' + err); });" +
        "  }, true);" +
        "})();";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Die Web-App steuert Hell/Dunkel selbst ueber prefers-color-scheme.
        // Auf API 29-32 wuerde WebView sonst zusaetzlich automatisch nachdunkeln
        // (Doppel-Effekt); ab API 33 ist das bereits standardmaessig aus.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && Build.VERSION.SDK_INT < 33) {
            settings.setForceDark(WebSettings.FORCE_DARK_OFF);
        }

        bridge = new WebAppInterface(this);
        webView.addJavascriptInterface(bridge, "AndroidBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript(DOWNLOAD_HOOK_JS, null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback,
                                              FileChooserParams params) {
                filePathCallback = callback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                try {
                    startActivityForResult(
                            Intent.createChooser(intent, "Sicherungsdatei auswählen"),
                            FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        webView.loadUrl("file:///android_asset/index.html");

        requestStoragePermissionIfNeeded();
    }

    /** Called by WebAppInterface.pickBackupFolder() (already on the UI thread). */
    void launchBackupFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, FOLDER_PICKER_REQUEST);
        } catch (Exception e) {
            notifyFolderPicked(false, null);
        }
    }

    private void notifyFolderPicked(boolean success, String folderName) {
        String safeName = folderName == null ? "null" : "'" + folderName.replace("'", "\\'") + "'";
        String js = "window.onBackupFolderPicked && window.onBackupFolderPicked("
                + success + ", " + safeName + ");";
        webView.evaluateJavascript(js, null);
    }

    private void requestStoragePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= 28) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_REQUEST);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                results = new Uri[]{data.getData()};
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
            return;
        }

        if (requestCode == FOLDER_PICKER_REQUEST) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                Uri treeUri = data.getData();
                try {
                    getContentResolver().takePersistableUriPermission(
                            treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                } catch (Exception ignored) {}

                String displayName = friendlyFolderName(treeUri);
                bridge.storeBackupFolder(treeUri, displayName);
                notifyFolderPicked(true, displayName);
            } else {
                notifyFolderPicked(false, null);
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private String friendlyFolderName(Uri treeUri) {
        try {
            String docId = android.provider.DocumentsContract.getTreeDocumentId(treeUri);
            int colon = docId.indexOf(':');
            String path = colon >= 0 ? docId.substring(colon + 1) : docId;
            return path.isEmpty() ? "Gewählter Ordner" : path;
        } catch (Exception e) {
            return "Gewählter Ordner";
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Live-Update, wenn sich der System-Dunkelmodus aendert, waehrend die App offen ist.
        int nightMode = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDark = nightMode == Configuration.UI_MODE_NIGHT_YES;
        if (webView != null) {
            webView.evaluateJavascript(
                    "window.onNativeThemeChanged && window.onNativeThemeChanged(" + isDark + ");",
                    null);
        }
    }

    @Override
    public void onBackPressed() {
        // Zuerst der Web-App die Chance geben, ein offenes Overlay-Fenster
        // (Einstellungen, Formulare, Bestaetigungs-Dialoge) selbst zu schliessen,
        // bevor wir die App verlassen oder in der WebView-Historie zurueckgehen.
        webView.evaluateJavascript(
            "(function(){ return window.closeAnyOpenOverlay ? window.closeAnyOpenOverlay() : false; })();",
            new android.webkit.ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    boolean overlayWasClosed = "true".equals(value);
                    if (overlayWasClosed) {
                        return;
                    }
                    if (webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        MainActivity.super.onBackPressed();
                    }
                }
            }
        );
    }
}
