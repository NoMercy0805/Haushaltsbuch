package com.example.haushaltsbuch;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Simple, real native storage (SQLite) that replaces the WebView's
 * localStorage as the actual persistence layer. localStorage is still used
 * as a fallback when the app runs outside our WebView (e.g. plain browser
 * testing), but inside the app all reads/writes go through this class via
 * WebAppInterface.nativeGetItem/nativeSetItem/nativeRemoveItem.
 *
 * Deliberately a single simple key-value table (mirrors what localStorage
 * already provided) rather than a fully normalized schema - the app's own
 * JSON-blob-per-key structure stays unchanged, only WHERE it is stored
 * changes. This keeps the migration low-risk: every existing key/value pair
 * is copied over as-is, byte for byte.
 */
public class NativeKeyValueStore extends SQLiteOpenHelper {

    private static final String DB_NAME = "app_storage.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "kv_store";

    private static NativeKeyValueStore instance;

    public static synchronized NativeKeyValueStore getInstance(Context context) {
        if (instance == null) {
            instance = new NativeKeyValueStore(context.getApplicationContext());
        }
        return instance;
    }

    private NativeKeyValueStore(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (key TEXT PRIMARY KEY, value TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Kein Schema-Wechsel bisher noetig.
    }

    public synchronized String getItem(String key) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE, new String[]{"value"}, "key = ?",
                new String[]{key}, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    public synchronized void setItem(String key, String value) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("key", key);
        values.put("value", value);
        db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public synchronized void removeItem(String key) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE, "key = ?", new String[]{key});
    }
}
