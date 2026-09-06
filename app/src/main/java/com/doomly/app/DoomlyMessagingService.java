package com.doomly.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.doomly.app.data.PushTokenRegistrar;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/** Receives admin notifications; Firebase is used only as the delivery transport. */
public final class DoomlyMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "doomly_admin";

    @Override public void onNewToken(@NonNull String token) {
        PushTokenRegistrar.register(this);
    }

    @Override public void onMessageReceived(@NonNull RemoteMessage message) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
        ) != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        String title = message.getNotification() != null && message.getNotification().getTitle() != null
                ? message.getNotification().getTitle() : "Doomly";
        String body = message.getNotification() != null && message.getNotification().getBody() != null
                ? message.getNotification().getBody() : "Your doomscroll report has news.";
        NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(),
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_stat_doomly)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setContentIntent(PendingIntent.getActivity(this, 0,
                                new Intent(this, MainActivity.class),
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .build());
    }

    static void ensureChannel(android.content.Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(new NotificationChannel(
                CHANNEL_ID, "Doomly admin updates", NotificationManager.IMPORTANCE_DEFAULT));
    }
}
