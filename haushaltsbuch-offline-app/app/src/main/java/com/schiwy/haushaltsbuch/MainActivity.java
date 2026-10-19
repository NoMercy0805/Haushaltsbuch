package com.schiwy.haushaltsbuch;

import android.Manifest;
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

import androidx.fragment.app.FragmentActivity;

public class MainActivity extends FragmentActivity {

    private WebView webView;
    private WebAppInterface bridge;
    private SecurityBridge securityBridge;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST = 777;
    private static final int FOLDER_PICKER_REQUEST = 778;
    private static final int STORAGE_PERMISSION_REQUEST = 502;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 503;

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

    // Der Plus-Kreis bleibt dauerhaft sichtbar und wird nur am Hintergrund
    // transparenter. Dafuer reicht eine statische CSS-Ueberschreibung; kein
    // MutationObserver oder permanentes Eingreifen in die Klassenlogik noetig.
    private static final String FAB_SCROLL_GUARD_JS =
        "(function(){" +
        "  if(window.__androidFabPermanentInstalled) return;" +
        "  window.__androidFabPermanentInstalled = true;" +
        "  var fab = document.getElementById('addEntryFab');" +
        "  if(!fab) return;" +
        "  var style = document.createElement('style');" +
        "  style.id = 'androidFabPermanentStyle';" +
        "  style.textContent = '#addEntryFab{background:rgba(18,104,255,0.52)!important;opacity:1!important;transform:none!important;pointer-events:auto!important;}' +" +
        "    '#addEntryFab.fab-hidden{opacity:1!important;transform:none!important;pointer-events:auto!important;}' +" +
        "    'html[data-theme=\"dark\"] #addEntryFab{background:rgba(91,156,255,0.52)!important;}';" +
        "  document.head.appendChild(style);" +
        "  fab.classList.remove('fab-hidden');" +
        "})();";

    // Kontostand-Eintraege verhalten sich wie die kompakten Listen (z.B.
    // Rechnungen): Ein Tipp auf die Zeile klappt zuerst Bearbeiten/Loeschen
    // auf. Erst der Bearbeiten-Button oeffnet das Formular.
    private static final String BETA_ENTRY_ACTIONS_JS =
        "(function(){" +
        "  var list = document.getElementById('betaList');" +
        "  if(!list || list.__androidExpandableActionsInstalled) return;" +
        "  list.__androidExpandableActionsInstalled = true;" +
        "  if(!document.getElementById('androidBetaEntryActionsStyle')){" +
        "    var style = document.createElement('style');" +
        "    style.id = 'androidBetaEntryActionsStyle';" +
        "    style.textContent = '.beta-entry-row{flex-wrap:wrap;cursor:pointer;}' +" +
        "      '.beta-entry-row .beta-entry-actions{display:flex;justify-content:flex-end;gap:8px;flex-basis:100%;width:100%;max-height:0;opacity:0;overflow:hidden;margin-top:0;transition:max-height .2s ease,opacity .2s ease,margin-top .2s ease;}' +" +
        "      '.beta-entry-row.expanded .beta-entry-actions{max-height:78px;opacity:1;margin-top:10px;}';" +
        "    document.head.appendChild(style);" +
        "  }" +
        "  function ensureActions(row,id){" +
        "    var actions = row.querySelector('.beta-entry-actions');" +
        "    if(actions) return actions;" +
        "    actions = document.createElement('span');" +
        "    actions.className = 'beta-entry-actions';" +
        "    actions.innerHTML = '<span class=\"action-icon-btn-wrap\"><button type=\"button\" class=\"action-icon-btn neutral\" data-beta-row-action=\"edit\" aria-label=\"Bearbeiten\" title=\"Bearbeiten\"><svg viewBox=\"0 0 24 24\" width=\"15\" height=\"15\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M12 20h9\"/><path d=\"M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z\"/></svg></button><span class=\"action-icon-btn-label\">Bearbeiten</span></span>' +" +
        "      '<span class=\"action-icon-btn-wrap\"><button type=\"button\" class=\"action-icon-btn danger\" data-beta-row-action=\"delete\" aria-label=\"Löschen\" title=\"Löschen\"><svg viewBox=\"0 0 24 24\" width=\"15\" height=\"15\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M3 6h18\"/><path d=\"M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2\"/><path d=\"M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6\"/><path d=\"M10 11v6M14 11v6\"/></svg></button><span class=\"action-icon-btn-label\">Löschen</span></span>';" +
        "    row.appendChild(actions);" +
        "    return actions;" +
        "  }" +
        "  async function chooseScope(entry){" +
        "    if(entry.isRecurringTemplate || entry.recurringGroupId){" +
        "      return await betaRecurringChoiceDialog();" +
        "    }" +
        "    return 'this';" +
        "  }" +
        "  list.addEventListener('click', async function(e){" +
        "    if(e.target.closest('.beta-entry-icon')) return;" +
        "    var row = e.target.closest('.beta-entry-row');" +
        "    if(!row) return;" +
        "    var actionButton = e.target.closest('[data-beta-row-action]');" +
        "    e.preventDefault();" +
        "    e.stopImmediatePropagation();" +
        "    var id = row.getAttribute('data-id');" +
        "    var entry = state.betaEntries.filter(function(x){ return x.id === id; })[0];" +
        "    if(!entry) return;" +
        "    if(!actionButton){" +
        "      ensureActions(row,id);" +
        "      row.classList.toggle('expanded');" +
        "      return;" +
        "    }" +
        "    var action = actionButton.getAttribute('data-beta-row-action');" +
        "    var scope = await chooseScope(entry);" +
        "    if(!scope) return;" +
        "    if(action === 'edit'){" +
        "      openBetaEditForm(entry, scope);" +
        "      return;" +
        "    }" +
        "    if(action === 'delete'){" +
        "      editingBetaId = entry.id;" +
        "      editingBetaScope = scope;" +
        "      var deleteButton = document.getElementById('betaDeleteBtn');" +
        "      if(deleteButton) deleteButton.click();" +
        "    }" +
        "  }, true);" +
        "})();";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        webView.setFitsSystemWindows(true);
        boolean isNightMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        webView.setBackgroundColor(isNightMode ? 0xFF484F5A : 0xFFC4CAD3);
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

        securityBridge = new SecurityBridge(this, webView);
        webView.addJavascriptInterface(securityBridge, "SecurityBridge");

        // Nach Entfernen der Offline-KI nur deren eigene Modell-Dateien loeschen.
        // Die SQLite-Haushaltsbuchdaten und Backups bleiben unangetastet.
        cleanupRetiredGemmaFiles();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript(DOWNLOAD_HOOK_JS, null);
                view.evaluateJavascript(FAB_SCROLL_GUARD_JS, null);
                view.evaluateJavascript(BETA_ENTRY_ACTIONS_JS, null);
                if (securityBridge != null) {
                    securityBridge.installSettingsUi();
                }
                handleBetaWidgetIntent(getIntent());
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
                    if (securityBridge != null) securityBridge.setExternalActivityInProgress(true);
                    startActivityForResult(
                            Intent.createChooser(intent, "Sicherungsdatei auswählen"),
                            FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    if (securityBridge != null) securityBridge.setExternalActivityInProgress(false);
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        webView.loadUrl("file:///android_asset/index.html");

        requestStoragePermissionIfNeeded();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (securityBridge != null) securityBridge.onHostStart();
    }

    @Override
    protected void onStop() {
        if (securityBridge != null) securityBridge.onHostStop();
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleBetaWidgetIntent(intent);
    }

    /** Reagiert auf einen Tipp auf das Beta-Homescreen-Widget (+/-/Zahl). */
    private void handleBetaWidgetIntent(Intent intent) {
        if (intent == null || webView == null) return;
        String action = intent.getStringExtra(BetaWidgetProvider.EXTRA_BETA_ACTION);
        if (action == null) return;
        intent.removeExtra(BetaWidgetProvider.EXTRA_BETA_ACTION);
        String js = "window.openBetaFromWidget && window.openBetaFromWidget('" + action + "');";
        webView.evaluateJavascript(js, null);
    }

    /** Called by WebAppInterface.pickBackupFolder() (already on the UI thread). */
    void launchBackupFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            if (securityBridge != null) securityBridge.setExternalActivityInProgress(true);
            startActivityForResult(intent, FOLDER_PICKER_REQUEST);
        } catch (Exception e) {
            if (securityBridge != null) securityBridge.setExternalActivityInProgress(false);
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

    // Ab Android 13 (API 33) braucht das Anzeigen einer Benachrichtigung eine
    // explizite Laufzeit-Berechtigung. Wird von WebAppInterface aufgerufen,
    // wenn der Nutzer die Kontostand-Benachrichtigung aktiviert. Gibt true
    // zurueck, wenn die Berechtigung schon vorhanden ist (oder gar nicht
    // noetig, Android < 13) - dann kann die Benachrichtigung sofort gezeigt
    // werden, ohne auf den Dialog zu warten.
    public boolean requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted && bridge != null) {
                bridge.showKontostandNotificationIfEnabled();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST || requestCode == FOLDER_PICKER_REQUEST) {
            if (securityBridge != null) securityBridge.setExternalActivityInProgress(false);
        }

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

    private void cleanupRetiredGemmaFiles() {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    java.io.File folder = new java.io.File(getFilesDir(), "local-ai");
                    java.io.File model = new java.io.File(folder, "model.litertlm");
                    java.io.File incompleteImport = new java.io.File(folder, "model-import.tmp");
                    // Nur die beiden exakt bekannten KI-Dateien loeschen.
                    if (model.isFile()) model.delete();
                    if (incompleteImport.isFile()) incompleteImport.delete();
                    String[] rest = folder.list();
                    if (rest != null && rest.length == 0) folder.delete();
                } catch (Exception ignored) {
                    // Keine Hintergrundbereinigung darf den normalen App-Start blockieren.
                }
            }
        }, "RemoveRetiredGemmaModel").start();
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
