package com.schiwy.haushaltsbuch;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.RemoteViews;

/**
 * 4x1-Homescreen-Widget fuer den Beta-Tab: zeigt den zuletzt von der Web-App
 * gemeldeten Kontostand, "+" und "-" oeffnen die App direkt im
 * Eintragen-Formular (Einnahme bzw. Ausgabe), Antippen der Zahl oeffnet
 * einfach den Beta-Tab. Da RemoteViews keinen Zugriff auf die WebView-Daten
 * hat, wird der Anzeigetext separat in SharedPreferences zwischengespeichert
 * (siehe WebAppInterface.updateBetaWidgetBalance).
 */
public class BetaWidgetProvider extends AppWidgetProvider {

    public static final String PREFS_NAME = "BetaWidgetPrefs";
    public static final String KEY_BALANCE = "beta_balance_text";
    public static final String KEY_POSITIVE = "beta_balance_positive";
    public static final String EXTRA_BETA_ACTION = "beta_widget_action";
    public static final String ACTION_ADD_EINNAHME = "add_einnahme";
    public static final String ACTION_ADD_AUSGABE = "add_ausgabe";
    public static final String ACTION_OPEN = "open";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId);
        }
    }

    /** Von WebAppInterface aufgerufen, sobald die Web-App einen neuen Kontostand meldet. */
    public static void updateAll(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, BetaWidgetProvider.class));
        for (int id : ids) {
            updateWidget(context, mgr, id);
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

    private static void updateWidget(Context context, AppWidgetManager appWidgetManager, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.beta_widget);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String balanceText = prefs.getString(KEY_BALANCE, "0,00 \u20ac");
        boolean isPositive = prefs.getBoolean(KEY_POSITIVE, true);
        views.setTextViewText(R.id.widget_balance, balanceText);
        views.setTextColor(R.id.widget_balance, isPositive
                ? context.getResources().getColor(R.color.widget_balance_pos)
                : context.getResources().getColor(R.color.widget_balance_neg));

        views.setOnClickPendingIntent(R.id.widget_plus_btn,
                buildPendingIntent(context, 101, ACTION_ADD_EINNAHME));
        views.setOnClickPendingIntent(R.id.widget_minus_btn,
                buildPendingIntent(context, 102, ACTION_ADD_AUSGABE));
        PendingIntent openPending = buildPendingIntent(context, 103, ACTION_OPEN);
        views.setOnClickPendingIntent(R.id.widget_balance, openPending);
        views.setOnClickPendingIntent(R.id.widget_label, openPending);

        appWidgetManager.updateAppWidget(widgetId, views);
    }
}
