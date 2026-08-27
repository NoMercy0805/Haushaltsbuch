package com.example.haushaltsbuch;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Exposed to the web app as "window.AndroidBridge".
 */
public class WebAppInterface {

    private final MainActivity activity;
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final String PREFS = "backup_prefs";
    private static final String KEY_TREE_URI = "backup_tree_uri";
    private static final String KEY_TREE_NAME = "backup_tree_name";

    public WebAppInterface(MainActivity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
    }

    // ---------- Native Speicherung (ersetzt localStorage) ----------

    @JavascriptInterface
    public String nativeGetItem(String key) {
        return NativeKeyValueStore.getInstance(context).getItem(key);
    }

    @JavascriptInterface
    public void nativeSetItem(String key, String value) {
        NativeKeyValueStore.getInstance(context).setItem(key, value);
    }

    @JavascriptInterface
    public void nativeRemoveItem(String key) {
        NativeKeyValueStore.getInstance(context).removeItem(key);
    }

    @JavascriptInterface
    public boolean isSystemDark() {
        int nightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    // ---------- Manuelles Backup (Downloads) ----------

    @JavascriptInterface
    public void saveTextFile(String base64Data, String filename, String mimeType) {
        try {
            byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);

            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                values.put(MediaStore.MediaColumns.MIME_TYPE,
                        mimeType != null ? mimeType : "application/octet-stream");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = context.getContentResolver()
                        .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    showToast("Speichern fehlgeschlagen.");
                    return;
                }
                try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                    if (out != null) out.write(bytes);
                }
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) downloadsDir.mkdirs();
                File file = new File(downloadsDir, filename);
                try (FileOutputStream out = new FileOutputStream(file)) {
                    out.write(bytes);
                }
            }

            showToast("Gespeichert in Downloads: " + filename);
        } catch (Exception e) {
            showToast("Speichern fehlgeschlagen: " + e.getMessage());
        }
    }

    // ---------- Automatisches Backup (gewählter Ordner) ----------

    @JavascriptInterface
    public void pickBackupFolder() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                activity.launchBackupFolderPicker();
            }
        });
    }

    @JavascriptInterface
    public String getBackupFolderName() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TREE_NAME, null);
    }

    @JavascriptInterface
    public void clearBackupFolder() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String uriString = prefs.getString(KEY_TREE_URI, null);
        if (uriString != null) {
            try {
                Uri treeUri = Uri.parse(uriString);
                context.getContentResolver().releasePersistableUriPermission(
                        treeUri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Exception ignored) {}
        }
        prefs.edit().remove(KEY_TREE_URI).remove(KEY_TREE_NAME).apply();
    }

    void storeBackupFolder(Uri treeUri, String displayName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_TREE_URI, treeUri.toString())
                .putString(KEY_TREE_NAME, displayName)
                .apply();
    }

    @JavascriptInterface
    public void saveToBackupFolder(String base64Data, String filename, String mimeType) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String uriString = prefs.getString(KEY_TREE_URI, null);
        if (uriString == null) return;

        try {
            Uri treeUri = Uri.parse(uriString);
            byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
            Uri docTreeUri = DocumentsContract.buildDocumentUriUsingTree(
                    treeUri, DocumentsContract.getTreeDocumentId(treeUri));

            Uri existing = findChildByName(docTreeUri, filename);
            if (existing != null) {
                try {
                    DocumentsContract.deleteDocument(context.getContentResolver(), existing);
                } catch (Exception ignored) {}
            }

            Uri newDoc = DocumentsContract.createDocument(
                    context.getContentResolver(), docTreeUri,
                    mimeType != null ? mimeType : "text/csv", filename);
            if (newDoc == null) {
                showToast("Automatisches Backup fehlgeschlagen.");
                return;
            }
            try (OutputStream out = context.getContentResolver().openOutputStream(newDoc)) {
                if (out != null) out.write(bytes);
            }
        } catch (Exception e) {
            showToast("Automatisches Backup fehlgeschlagen: " + e.getMessage());
        }
    }

    private Uri findChildByName(Uri docTreeUri, String displayName) {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                docTreeUri, DocumentsContract.getDocumentId(docTreeUri));
        android.database.Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    childrenUri,
                    new String[]{
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME
                    }, null, null, null);
            if (cursor == null) return null;
            while (cursor.moveToNext()) {
                String name = cursor.getString(1);
                if (displayName.equals(name)) {
                    String docId = cursor.getString(0);
                    return DocumentsContract.buildDocumentUriUsingTree(docTreeUri, docId);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    void showToast(final String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
