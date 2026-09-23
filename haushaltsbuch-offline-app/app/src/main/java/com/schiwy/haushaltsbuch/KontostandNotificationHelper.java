package com.schiwy.haushaltsbuch;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.RemoteViews;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;

/**
 * Zeigt den Kontostand als dauerhafte (ongoing), nicht wegwischbare
 * Benachrichtigung an - Design 2 ("Farbiger Akzent").
 *
 * Enthaelt vorerst sichtbare Diagnose-Meldungen (Toast) an mehreren Stellen,
 * damit beim naechsten Testlauf auf dem Geraet genau erkennbar ist, an
 * welcher Stelle es fehlschlaegt - werden entfernt, sobald das Problem
 * gefunden ist.
 */
public class KontostandNotificationHelper {

    private static final String CHANNEL_ID = "kontostand_balance_channel_v3";
    public static final int NOTIFICATION_ID = 4711;

    private static void toast(final Context context, final String msg) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;
            NotificationChannel existing = manager.getNotificationChannel(CHANNEL_ID);
            if (existing == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Kontostand",
                        NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("Zeigt deinen aktuellen Kontostand dauerhaft an");
                channel.setShowBadge(false);
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void showOrUpdate(Context context, String balanceText, boolean isPositive) {
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    toast(context, "Diagnose: Keine Benachrichtigungs-Berechtigung erteilt (POST_NOTIFICATIONS fehlt).");
                    return;
                }
            }
            ensureChannel(context);

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) {
                toast(context, "Diagnose: NotificationManager ist null.");
                return;
            }

            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel channel = manager.getNotificationChannel(CHANNEL_ID);
                if (channel != null && channel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                    toast(context, "Diagnose: Kanal \"Kontostand\" ist in den Android-Einstellungen blockiert (Importance NONE).");
                    return;
                }
            }

            Intent openIntent = new Intent(context, MainActivity.class);
            openIntent.setAction(Intent.ACTION_MAIN);
            openIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent contentIntent = PendingIntent.getActivity(context, 4711, openIntent, flags);

            int accentColor = isPositive
                    ? context.getResources().getColor(R.color.widget_balance_pos)
                    : context.getResources().getColor(R.color.widget_balance_neg);
            int pillDrawable = isPositive ? R.drawable.widget_amount_pill_pos : R.drawable.widget_amount_pill_neg;

            RemoteViews collapsed = new RemoteViews(context.getPackageName(), R.layout.notification_kontostand_collapsed);
            collapsed.setInt(R.id.notif_accent_strip, "setBackgroundColor", accentColor);

            RemoteViews expanded = new RemoteViews(context.getPackageName(), R.layout.notification_kontostand_expanded);
            expanded.setInt(R.id.notif_accent_strip, "setBackgroundColor", accentColor);
            expanded.setTextViewText(R.id.notif_amount, balanceText);
            expanded.setTextColor(R.id.notif_amount, accentColor);
            expanded.setInt(R.id.notif_amount, "setBackgroundResource", pillDrawable);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_small)
                    .setCustomContentView(collapsed)
                    .setCustomBigContentView(expanded)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(false)
                    .setContentIntent(contentIntent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_STATUS);

            manager.notify(NOTIFICATION_ID, builder.build());
            toast(context, "Diagnose: notify() wurde ohne Fehler aufgerufen.");
        } catch (Exception e) {
            toast(context, "Diagnose-Fehler: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    public static void cancel(Context context) {
        try {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.cancel(NOTIFICATION_ID);
        } catch (Exception ignored) {}
    }
}
