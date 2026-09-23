package com.schiwy.haushaltsbuch;

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
 * Benachrichtigung an. Nutzt bewusst den STANDARD-Benachrichtigungsstil
 * (BigTextStyle) statt custom RemoteViews-Layouts: Custom Layouts werden im
 * SystemUI-Prozess gerendert, nicht im eigenen App-Prozess - schlaegt das
 * dort fehl, kommt in der App selbst KEIN Fehler an (notify() meldet trotzdem
 * Erfolg), was das Problem unsichtbar und schwer diagnostizierbar macht. Der
 * Standardstil ist zuverlaessig und wird von jedem Launcher korrekt
 * gerendert.
 */
public class KontostandNotificationHelper {

    private static final String CHANNEL_ID = "kontostand_balance_channel_v4";
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
                    return;
                }
            }
            ensureChannel(context);

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;

            Intent openIntent = new Intent(context, MainActivity.class);
            openIntent.setAction(Intent.ACTION_MAIN);
            openIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent contentIntent = PendingIntent.getActivity(context, 4711, openIntent, flags);

            String expandedText = (isPositive ? "Im Plus: " : "Im Minus: ") + balanceText;
            android.text.SpannableString spannableExpanded = new android.text.SpannableString(expandedText);
            int amountStart = expandedText.length() - balanceText.length();
            spannableExpanded.setSpan(new android.text.style.RelativeSizeSpan(1.5f),
                    amountStart, expandedText.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableExpanded.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                    amountStart, expandedText.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_small)
                    .setContentTitle("Kontostand")
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(spannableExpanded))
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(false)
                    .setContentIntent(contentIntent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_STATUS);

            manager.notify(NOTIFICATION_ID, builder.build());
        } catch (Exception ignored) {}
    }

    public static void cancel(Context context) {
        try {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.cancel(NOTIFICATION_ID);
        } catch (Exception ignored) {}
    }
}
