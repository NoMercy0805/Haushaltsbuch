package com.schiwy.haushaltsbuch;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;

/**
 * Zeigt den Kontostand als dauerhafte (ongoing), nicht wegwischbare
 * Benachrichtigung an. Im eingeklappten Zustand ist nur ein kurzer Hinweis
 * sichtbar ("Kontostand"), erst beim Herunterziehen (Expand) erscheint der
 * eigentliche Betrag - dafuer wird der Betrag ausschliesslich ueber den
 * BigTextStyle-Zusatztext gesetzt, der nur in der ausgeklappten Ansicht
 * angezeigt wird, waehrend Titel/Kurztext im eingeklappten Zustand knapp
 * bleiben.
 */
public class KontostandNotificationHelper {

    private static final String CHANNEL_ID = "kontostand_balance_channel";
    public static final int NOTIFICATION_ID = 4711;

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;
            NotificationChannel existing = manager.getNotificationChannel(CHANNEL_ID);
            if (existing == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Kontostand",
                        NotificationManager.IMPORTANCE_LOW);
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
                    return;
                }
            }
            ensureChannel(context);

            Intent openIntent = new Intent(context, MainActivity.class);
            openIntent.setAction(Intent.ACTION_MAIN);
            openIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent contentIntent = PendingIntent.getActivity(context, 4711, openIntent, flags);

            String expandedText = (isPositive ? "Im Plus: " : "Im Minus: ") + balanceText;

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(context.getApplicationInfo().icon)
                    .setContentTitle("Haushaltsbuch+")
                    .setContentText("Kontostand")
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(expandedText))
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(false)
                    .setContentIntent(contentIntent)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setCategory(NotificationCompat.CATEGORY_STATUS);

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, builder.build());
            }
        } catch (Exception ignored) {}
    }

    public static void cancel(Context context) {
        try {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.cancel(NOTIFICATION_ID);
        } catch (Exception ignored) {}
    }
}
