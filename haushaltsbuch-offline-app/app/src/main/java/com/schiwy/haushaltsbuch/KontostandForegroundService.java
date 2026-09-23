package com.schiwy.haushaltsbuch;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.widget.RemoteViews;

/**
 * Zeigt den Kontostand als dauerhafte, farbig eingefaerbte Benachrichtigung
 * an - nach demselben bewaehrten Muster wie Arbeitszeit+'s
 * PunchForegroundService: nur als echter Foreground Service funktioniert
 * setColorized() zuverlaessig und werden custom RemoteViews-Layouts
 * zuverlaessig gerendert (ein einfaches notify() ohne Service hat das nicht
 * geschafft).
 *
 * Auf Android 14+ kann der Nutzer die Benachrichtigung trotzdem wegwischen
 * (System-Vorgabe fuer Foreground-Service-Benachrichtigungen) - sie erscheint
 * dann sofort erneut (siehe dismissReceiver), wie bei Arbeitszeit+.
 */
public class KontostandForegroundService extends Service {

    public static final String EXTRA_BALANCE_TEXT = "balance_text";
    public static final String EXTRA_IS_POSITIVE = "is_positive";
    private static final String CHANNEL_ID = "kontostand_fgs_channel";
    private static final int NOTIFICATION_ID = 4711;
    private static final String ACTION_DISMISSED =
            "com.schiwy.haushaltsbuch.action.KONTOSTAND_NOTIFICATION_DISMISSED";

    private String balanceText = "0,00 \u20ac";
    private boolean isPositive = true;
    private boolean receiverRegistered = false;

    private final BroadcastReceiver dismissReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            repostNow();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createChannelIfNeeded();
        registerReceiver(
                dismissReceiver,
                new IntentFilter(ACTION_DISMISSED),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        ? Context.RECEIVER_NOT_EXPORTED
                        : 0
        );
        receiverRegistered = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra(EXTRA_BALANCE_TEXT)) {
            balanceText = intent.getStringExtra(EXTRA_BALANCE_TEXT);
            isPositive = intent.getBooleanExtra(EXTRA_IS_POSITIVE, true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, buildNotification());
        }
        return START_STICKY;
    }

    private void repostNow() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private Notification buildNotification() {
        Intent tapIntent = new Intent(this, MainActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, tapIntent, flags);

        Intent dismissIntent = new Intent(ACTION_DISMISSED).setPackage(getPackageName());
        PendingIntent deleteIntent = PendingIntent.getBroadcast(this, 0, dismissIntent, flags);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        String accentHex = isPositive ? "#22A559" : "#E5484D";

        builder
                .setContentTitle("Haushaltsbuch+")
                .setContentText("Kontostand")
                .setSmallIcon(R.drawable.ic_notification_small)
                .setContentIntent(contentIntent)
                .setDeleteIntent(deleteIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setColor(Color.parseColor(accentHex));

        try {
            Bitmap logo = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            if (logo != null) builder.setLargeIcon(logo);
        } catch (Exception ignored) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            int stripColor = Color.WHITE;
            RemoteViews collapsedView = new RemoteViews(getPackageName(), R.layout.notification_kontostand_collapsed);
            collapsedView.setInt(R.id.notif_accent_strip, "setBackgroundColor", stripColor);

            RemoteViews expandedView = new RemoteViews(getPackageName(), R.layout.notification_kontostand_expanded);
            expandedView.setInt(R.id.notif_accent_strip, "setBackgroundColor", stripColor);
            expandedView.setTextViewText(R.id.notif_amount, balanceText);

            builder.setStyle(new Notification.DecoratedCustomViewStyle());
            builder.setCustomContentView(collapsedView);
            builder.setCustomBigContentView(expandedView);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setColorized(true);
        }

        return builder.build();
    }

    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Kontostand",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Zeigt deinen aktuellen Kontostand dauerhaft an.");
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        if (receiverRegistered) {
            try { unregisterReceiver(dismissReceiver); } catch (Exception ignored) {}
            receiverRegistered = false;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
