package com.schiwy.haushaltsbuch;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.RemoteViews;
import androidx.core.app.NotificationCompat;

/**
 * Zeigt den Kontostand als dauerhafte (ongoing), nicht wegwischbare
 * Benachrichtigung an - Design 2 ("Farbiger Akzent"): ein farbiger Rand links
 * (Rot bei Minus, Gruen bei Plus), eingeklappt nur der Titel "Kontostand",
 * ausgeklappt zusaetzlich der Betrag als farbige Pille. App-Icon und
 * App-Name kommen automatisch von Android selbst (DecoratedCustomViewStyle) -
 * die eigenen Layouts zeigen nur noch die Kontostand-spezifischen Inhalte,
 * ohne das doppelt zu wiederholen.
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
                    .setSmallIcon(context.getApplicationInfo().icon)
                    .setCustomContentView(collapsed)
                    .setCustomBigContentView(expanded)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
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
