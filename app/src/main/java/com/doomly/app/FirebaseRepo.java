package com.doomly.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

final class FirebaseRepo {
    static final int REQUEST_GOOGLE_SIGN_IN = 7301;

    private static final long SYNC_DEBOUNCE_MS = 30_000L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static Runnable pendingSync;
    private static ResultCallback<Void> pendingLegacySignInCallback;

    private FirebaseRepo() {
    }

    static boolean isConfigured(Context context) {
        try {
            if (!FirebaseApp.getApps(context).isEmpty()) {
                return true;
            }
            return FirebaseApp.initializeApp(context) != null;
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    static boolean isSignedIn(Context context) {
        return isConfigured(context) && FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    static FirebaseUser currentUser(Context context) {
        if (!isConfigured(context)) {
            return null;
        }
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    static void signIn(Activity activity, ResultCallback<Void> callback) {
        if (!isConfigured(activity)) {
            callback.onError("Firebase is not connected yet.");
            return;
        }
        startLegacySignIn(activity, callback);
    }

    static boolean handleActivityResult(Activity activity, int requestCode, Intent data) {
        if (requestCode != REQUEST_GOOGLE_SIGN_IN) {
            return false;
        }

        ResultCallback<Void> callback = pendingLegacySignInCallback;
        pendingLegacySignInCallback = null;
        if (callback == null) {
            return true;
        }

        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account == null || account.getIdToken() == null) {
                callback.onError("Google sign-in did not return an ID token.");
                return true;
            }
            signInWithFirebaseCredential(activity, GoogleAuthProvider.getCredential(account.getIdToken(), null), callback);
        } catch (ApiException e) {
            callback.onError(cleanError(e));
        }
        return true;
    }

    static void signOut(Context context) {
        if (isConfigured(context)) {
            FirebaseAuth.getInstance().signOut();
        }

        CredentialManager credentialManager = CredentialManager.create(context);
        credentialManager.clearCredentialStateAsync(
                new ClearCredentialStateRequest(),
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(Void result) {
                    }

                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                    }
                }
        );
    }

    static void updateDisplayName(Context context, String displayName, ResultCallback<Void> callback) {
        DoomStatsStore.setDisplayName(context, displayName);
        if (!isSignedIn(context)) {
            callback.onSuccess(null);
            return;
        }

        ensureUserDocument(context, callback);
    }

    static void updateDailyTarget(Context context, int target, ResultCallback<Void> callback) {
        DoomStatsStore.setDailyTarget(context, target);
        syncNow(context, callback);
    }

    static void scheduleSync(Context context) {
        Context appContext = context.getApplicationContext();
        if (pendingSync != null) {
            MAIN.removeCallbacks(pendingSync);
        }

        pendingSync = () -> syncNow(appContext, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void value) {
            }

            @Override
            public void onError(String message) {
            }
        });
        MAIN.postDelayed(pendingSync, SYNC_DEBOUNCE_MS);
    }

    static void syncNow(Context context, ResultCallback<Void> callback) {
        if (!isSignedIn(context)) {
            callback.onSuccess(null);
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onSuccess(null);
            return;
        }

        DoomStatsStore.Snapshot snapshot = DoomStatsStore.snapshot(context);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();
        String displayName = displayName(context, user, snapshot);
        String photoUrl = photoUrl(user);

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("displayName", displayName);
        userMap.put("photoUrl", photoUrl);
        userMap.put("dailyTarget", snapshot.dailyTarget);
        userMap.put("totalReels", snapshot.totalReels);
        userMap.put("totalLikes", snapshot.totalLikes);
        userMap.put("xp", snapshot.xp);
        userMap.put("currentStreak", snapshot.streak);
        userMap.put("updatedAt", FieldValue.serverTimestamp());

        Map<String, Object> dailyMap = new HashMap<>();
        dailyMap.put("uid", uid);
        dailyMap.put("displayName", displayName);
        dailyMap.put("photoUrl", photoUrl);
        dailyMap.put("reels", snapshot.todayReels);
        dailyMap.put("likedReels", snapshot.todayLikes);
        dailyMap.put("xp", snapshot.xp);
        dailyMap.put("target", snapshot.dailyTarget);
        dailyMap.put("targetProgress", snapshot.targetProgressPercent);
        dailyMap.put("streak", snapshot.streak);
        dailyMap.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("users").document(uid)
                .set(userMap, SetOptions.merge())
                .continueWithTask(task -> db.collection("dailyStats")
                        .document(DoomStatsStore.todayKey())
                        .collection("users")
                        .document(uid)
                        .set(dailyMap, SetOptions.merge()))
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(cleanError(e)));
    }

    static void loadLeaderboard(Context context, ResultCallback<List<LeaderboardEntry>> callback) {
        if (!isSignedIn(context)) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("dailyStats")
                .document(DoomStatsStore.todayKey())
                .collection("users")
                .orderBy("reels", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<LeaderboardEntry> entries = new ArrayList<>();
                    int rank = 1;
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        entries.add(new LeaderboardEntry(
                                rank++,
                                stringValue(document, "displayName", "Doomly user"),
                                intValue(document, "reels"),
                                intValue(document, "xp"),
                                intValue(document, "targetProgress")
                        ));
                    }
                    callback.onSuccess(entries);
                })
                .addOnFailureListener(e -> callback.onError(cleanError(e)));
    }

    private static void handleCredential(Activity activity, Credential credential, ResultCallback<Void> callback) {
        if (!(credential instanceof CustomCredential)
                || !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
            activity.runOnUiThread(() -> startLegacySignIn(activity, callback));
            return;
        }

        GoogleIdTokenCredential googleCredential = GoogleIdTokenCredential.createFrom(((CustomCredential) credential).getData());
        signInWithFirebaseCredential(activity, GoogleAuthProvider.getCredential(googleCredential.getIdToken(), null), callback);
    }

    private static void startLegacySignIn(Activity activity, ResultCallback<Void> callback) {
        String webClientId = googleWebClientId(activity);
        if (webClientId.isEmpty()) {
            callback.onError("Missing Google web client ID.");
            return;
        }

        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        GoogleSignInClient client = GoogleSignIn.getClient(activity, options);
        pendingLegacySignInCallback = callback;
        activity.startActivityForResult(client.getSignInIntent(), REQUEST_GOOGLE_SIGN_IN);
    }

    private static void signInWithFirebaseCredential(Activity activity, AuthCredential firebaseCredential, ResultCallback<Void> callback) {
        FirebaseAuth.getInstance()
                .signInWithCredential(firebaseCredential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null && DoomStatsStore.snapshot(activity).displayName.isEmpty()) {
                        DoomStatsStore.setDisplayName(activity, safeDisplayName(user.getDisplayName()));
                    }
                    syncNow(activity, callback);
                })
                .addOnFailureListener(e -> callback.onError(cleanError(e)));
    }

    private static void ensureUserDocument(Context context, ResultCallback<Void> callback) {
        syncNow(context, callback);
    }

    private static String googleWebClientId(Context context) {
        int generatedId = context.getResources().getIdentifier("default_web_client_id", "string", context.getPackageName());
        if (generatedId != 0) {
            String generated = context.getString(generatedId).trim();
            if (!generated.isEmpty()) {
                return generated;
            }
        }
        return context.getString(com.doomly.app.R.string.doomly_google_web_client_id).trim();
    }

    private static String displayName(Context context, FirebaseUser user, DoomStatsStore.Snapshot snapshot) {
        if (!snapshot.displayName.isEmpty()) {
            return snapshot.displayName;
        }
        return safeDisplayName(user.getDisplayName());
    }

    private static String safeDisplayName(String displayName) {
        String safe = displayName == null ? "Doomly user" : displayName.trim();
        if (safe.isEmpty()) {
            return "Doomly user";
        }
        if (safe.length() > 24) {
            return safe.substring(0, 24);
        }
        return safe;
    }

    private static String photoUrl(FirebaseUser user) {
        Uri photo = user.getPhotoUrl();
        return photo == null ? "" : photo.toString();
    }

    private static int intValue(DocumentSnapshot document, String key) {
        Long value = document.getLong(key);
        return value == null ? 0 : value.intValue();
    }

    private static String stringValue(DocumentSnapshot document, String key, String fallback) {
        String value = document.getString(key);
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static String cleanError(Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return e.getClass().getSimpleName();
        }
        return message.trim();
    }

    interface ResultCallback<T> {
        void onSuccess(T value);

        void onError(String message);
    }

    static final class LeaderboardEntry {
        final int rank;
        final String displayName;
        final int reels;
        final int xp;
        final int targetProgress;

        LeaderboardEntry(int rank, String displayName, int reels, int xp, int targetProgress) {
            this.rank = rank;
            this.displayName = displayName;
            this.reels = reels;
            this.xp = xp;
            this.targetProgress = targetProgress;
        }

        String initials() {
            String trimmed = displayName.trim();
            if (trimmed.isEmpty()) {
                return "D";
            }
            String[] parts = trimmed.split("\\s+");
            if (parts.length == 1) {
                return parts[0].substring(0, 1).toUpperCase(Locale.US);
            }
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.US);
        }
    }
}
