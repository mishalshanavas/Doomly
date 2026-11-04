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
        String title = message.getData().get("title");
        String body = message.getData().get("body");
        if (title == null && message.getNotification() != null) title = message.getNotification().getTitle();
        if (body == null && message.getNotification() != null) body = message.getNotification().getBody();
        DoomlyNotifications.show(this, title == null ? "Doomly" : title,
                body == null ? "Your doomscroll report has news." : body);
    }

    static void ensureChannel(android.content.Context context) { DoomlyNotifications.createChannel(context); }
}
