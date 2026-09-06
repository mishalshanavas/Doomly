package com.doomly.app.data;

import android.content.Context;
import android.util.Log;

import com.doomly.app.ResultCallback;
import com.google.firebase.messaging.FirebaseMessaging;

/** Registers the current device token with Supabase after OAuth sign-in. */
public final class PushTokenRegistrar {
    private static final String TAG = "DoomlyPush";

    private PushTokenRegistrar() {}

    public static void register(Context context) {
        SupabaseClient client = SupabaseClient.getInstance();
        String uid = AuthRepository.getInstance().currentUid(context);
        if (client == null || uid.isEmpty()) return;

        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token ->
                client.registerDeviceToken(uid, token, new ResultCallback<Void>() {
                    @Override public void onSuccess(Void value) { Log.d(TAG, "Push token registered"); }
                    @Override public void onError(String message) { Log.w(TAG, "Token registration failed: " + message); }
                })
        ).addOnFailureListener(error -> Log.w(TAG, "FCM token unavailable", error));
    }
}
