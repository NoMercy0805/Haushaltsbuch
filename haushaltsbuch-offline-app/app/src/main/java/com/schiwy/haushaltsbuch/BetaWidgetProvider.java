package com.schiwy.haushaltsbuch;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;

/**
 * Gemeinsame Basis fuer die drei Kontostand-Widget-Groessen (2x1/3x1/4x1/5x1).
 * Jede Groesse ist ein eigener AppWidgetProvider (Unterklasse), damit sie im
 * Android-Widget-Picker als getrennte, waehlbare Optionen erscheinen - alle
 * zeigen aber denselben, zentral gespeicherten Kontostand an.
 *
 * Sichtschutz: Der Betrag ist standardmaessig mit einer Punkte-Abdeckung
 * (widget_balance_cover) ueberdeckt. Ein Tipp darauf loest ACTION_REVEAL aus
 * (Broadcast an den Widget-Provider selbst, oeffnet NICHT die App), blendet
 * die Abdeckung fuer dieses eine Widget aus und plant per AlarmManager einen
 * Alarm, der sie nach 3 Sekunden wieder einblendet (ACTION_REHIDE). Der
 * sichtbare Betrag darunter behaelt seinen eigenen Klick, der weiterhin die
 * App oeffnet - dadurch ist kein Doppeltipp-Timing noetig.
 */
public abstract class BetaWidgetProvider extends AppWidgetProvider {

    public static final String PREFS_NAME = "BetaWidgetPrefs";
    public static final String KEY_BALANCE = "beta_balance_text";
    public static final String KEY_POSITIVE = "beta_balance_positive";
    public static final String EXTRA_BETA_ACTION = "beta_widget_action";
    public static final String ACTION_ADD_EINNAHME = "add_einnahme";
    public static final String ACTION_ADD_AUSGABE = "add_ausgabe";
    public static final String ACTION_OPEN = "open";

    private static final String ACTION_REVEAL = "com.schiwy.haushaltsbuch.WIDGET_REVEAL";
    private static final String ACTION_REHIDE = "com.schiwy.haushaltsbuch.WIDGET_REHIDE";
    private static final String EXTRA_WIDGET_ID = "widget_id";
    private static final String EXTRA_LAYOUT_ID = "layout_id";
    private static final long REVEAL_DURATION_MS = 3000L;

    protected abstract int getLayoutId();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId, getLayoutId());
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (action == null) return;
        int widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1);
        int layoutId = intent.getIntExtra(EXTRA_LAYOUT_ID, 0);
        if (widgetId == -1 || layoutId == 0) return;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);

        if (ACTION_REVEAL.equals(action)) {
            prefs.edit().putBoolean(revealedKey(widgetId), true).apply();
            updateWidget(context, mgr, widgetId, layoutId);
            scheduleRehide(context, widgetId, layoutId);
        } else if (ACTION_REHIDE.equals(action)) {
            prefs.edit().putBoolean(revealedKey(widgetId), false).apply();
            updateWidget(context, mgr, widgetId, layoutId);
        }
    }

    private static String revealedKey(int widgetId) {
        return "beta_revealed_" + widgetId;
    }

    private static void scheduleRehide(Context context, int widgetId, int layoutId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        Intent intent = new Intent(context, HaushaltsbuchBetaWidgetReceiver.class);
        intent.setAction(ACTION_REHIDE);
        intent.putExtra(EXTRA_WIDGET_ID, widgetId);
        intent.putExtra(EXTRA_LAYOUT_ID, layoutId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 9000 + widgetId, intent, flags);
        long triggerAt = SystemClock.elapsedRealtime() + REVEAL_DURATION_MS;
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent);
    }

    /** Von WebAppInterface aufgerufen, sobald die Web-App einen neuen Kontostand meldet - aktualisiert alle Groessen. */
    public static void updateAll(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        updateForClass(context, mgr, BetaWidgetProvider2x1.class, R.layout.beta_widget_2x1);
        updateForClass(context, mgr, BetaWidgetProvider3x1.class, R.layout.beta_widget_3x1);
        updateForClass(context, mgr, BetaWidgetProvider4x1.class, R.layout.beta_widget_4x1);
        updateForClass(context, mgr, BetaWidgetProvider5x1.class, R.layout.beta_widget_4x1);
    }

    private static void updateForClass(Context context, AppWidgetManager mgr, Class<?> providerClass, int layoutId) {
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, providerClass));
        for (int id : ids) {
            updateWidget(context, mgr, id, layoutId);
        }
    }

    private static PendingIntent buildPendingIntent(Context context, int requestCode, String action) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(EXTRA_BETA_ACTION, action);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, requestCode, intent, flags);
    }

    private static PendingIntent buildRevealPendingIntent(Context context, int widgetId, int layoutId) {
        Intent intent = new Intent(context, HaushaltsbuchBetaWidgetReceiver.class);
        intent.setAction(ACTION_REVEAL);
        intent.putExtra(EXTRA_WIDGET_ID, widgetId);
        intent.putExtra(EXTRA_LAYOUT_ID, layoutId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, 8000 + widgetId, intent, flags);
    }

    private static void updateWidget(Context context, AppWidgetManager appWidgetManager, int widgetId, int layoutId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String balanceText = prefs.getString(KEY_BALANCE, "0,00 \u20ac");
        boolean isPositive = prefs.getBoolean(KEY_POSITIVE, true);
        boolean revealed = prefs.getBoolean(revealedKey(widgetId), false);

        views.setTextViewText(R.id.widget_balance, balanceText);
        views.setTextColor(R.id.widget_balance, isPositive
                ? context.getResources().getColor(R.color.widget_balance_pos)
                : context.getResources().getColor(R.color.widget_balance_neg));
        views.setInt(R.id.widget_balance_pill, "setBackgroundResource",
                isPositive ? R.drawable.widget_amount_pill_pos : R.drawable.widget_amount_pill_neg);

        views.setViewVisibility(R.id.widget_balance_cover, revealed ? View.GONE : View.VISIBLE);
        views.setOnClickPendingIntent(R.id.widget_balance_cover, buildRevealPendingIntent(context, widgetId, layoutId));

        views.setOnClickPendingIntent(R.id.widget_minus_btn,
                buildPendingIntent(context, 102, ACTION_ADD_AUSGABE));
        PendingIntent openPending = buildPendingIntent(context, 103, ACTION_OPEN);
        views.setOnClickPendingIntent(R.id.widget_balance, openPending);
        views.setOnClickPendingIntent(R.id.widget_label, openPending);

        appWidgetManager.updateAppWidget(widgetId, views);
    }
}
