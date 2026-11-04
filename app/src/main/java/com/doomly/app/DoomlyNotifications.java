package com.doomly.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/** One replaceable notification per category, each with independent Android controls. */
public final class DoomlyNotifications {
    public static final String MILESTONES = "doomly_milestones";
    public static final String UPDATES = "doomly_admin";
    private DoomlyNotifications() {}
    public static void createChannel(Context ctx) {
        NotificationManager manager = ctx.getSystemService(NotificationManager.class);
        NotificationChannel legacy = manager.getNotificationChannel("doomly_custom");
        NotificationChannel milestones = new NotificationChannel(MILESTONES, "Milestone celebrations",
                legacy == null ? NotificationManager.IMPORTANCE_DEFAULT : legacy.getImportance());
        milestones.setDescription("Occasional celebrations, at most four regular milestones per day plus your daily goal.");
        NotificationChannel updates = new NotificationChannel(UPDATES, "App updates", NotificationManager.IMPORTANCE_DEFAULT);
        updates.setDescription("Updates sent by Doomly. Managed independently from milestones.");
        manager.createNotificationChannel(milestones);
        manager.createNotificationChannel(updates);
        if (legacy != null) manager.deleteNotificationChannel("doomly_custom");
    }
    public static boolean enabled(Context ctx, String channel) {
        createChannel(ctx);
        return NotificationManagerCompat.from(ctx).areNotificationsEnabled()
            && ctx.getSystemService(NotificationManager.class).getNotificationChannel(channel).getImportance() != NotificationManager.IMPORTANCE_NONE;
    }
    public static boolean showMilestone(Context ctx, String body) { return post(ctx, MILESTONES, 101, "Thumb achievement unlocked", body); }
    public static void show(Context ctx, String title, String body) { post(ctx, UPDATES, 102, title, body); }
    private static boolean post(Context ctx, String channel, int id, String title, String body) {
        if (!enabled(ctx, channel)) return false;
        Intent intent = new Intent(ctx, MainActivity.class).putExtra("notification_destination", channel.equals(UPDATES) ? "profile" : "home")
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent open = PendingIntent.getActivity(ctx, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(com.doomly.app.R.drawable.ic_stat_doomly).setColor(0xFF0ACF70)
            .setContentTitle(title).setContentText(body).setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open).setAutoCancel(true).setGroup("doomly." + channel)
            .addAction(0, channel.equals(UPDATES) ? "View updates" : "View progress", open);
        try { NotificationManagerCompat.from(ctx).notify(id, builder.build()); return true; }
        catch (SecurityException ignored) { return false; }
    }
}
