package com.doomly.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class MainActivity extends Activity {
    private static final int PAGE_TODAY = 0;
    private static final int PAGE_LEADERBOARD = 1;
    private static final int PAGE_PROFILE = 2;
    private static final int WEEK_CHECKPOINTS = 7;

    private int selectedPage = PAGE_TODAY;
    private Palette palette;
    private LinearLayout pageContainer;
    private LinearLayout tabBar;
    private TextView todayTab;
    private TextView leaderboardTab;
    private TextView profileTab;
    private boolean receiverRegistered;

    private final BroadcastReceiver statsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            render();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        palette = Palette.from(isDarkMode());
        applySystemBars();
        setContentView(buildShell());
        render();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (FirebaseRepo.handleActivityResult(this, requestCode, data)) {
            render();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
        FirebaseRepo.scheduleSync(this);
        if (!receiverRegistered) {
            IntentFilter filter = new IntentFilter(DoomStatsStore.ACTION_STATS_CHANGED);
            ContextCompat.registerReceiver(this, statsReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
            receiverRegistered = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (receiverRegistered) {
            unregisterReceiver(statsReceiver);
            receiverRegistered = false;
        }
    }

    private View buildShell() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(palette.background);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        shell.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(18), dp(22), dp(18));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        pageContainer = new LinearLayout(this);
        pageContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(pageContainer, matchWrap());

        tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setGravity(Gravity.CENTER);
        tabBar.setPadding(dp(10), dp(8), dp(10), dp(12));
        tabBar.setBackgroundColor(palette.card);
        shell.addView(tabBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(70)
        ));

        todayTab = tab("Today", PAGE_TODAY);
        leaderboardTab = tab("League", PAGE_LEADERBOARD);
        profileTab = tab("Profile", PAGE_PROFILE);
        tabBar.addView(todayTab, tabParams());
        tabBar.addView(leaderboardTab, tabParams());
        tabBar.addView(profileTab, tabParams());

        return shell;
    }

    private void render() {
        if (pageContainer == null) {
            return;
        }

        boolean enabled = isAccessibilityServiceEnabled();
        pageContainer.removeAllViews();
        tabBar.setVisibility(enabled ? View.VISIBLE : View.GONE);

        if (!enabled) {
            renderSetupGate();
            return;
        }

        styleTab(todayTab, selectedPage == PAGE_TODAY);
        styleTab(leaderboardTab, selectedPage == PAGE_LEADERBOARD);
        styleTab(profileTab, selectedPage == PAGE_PROFILE);

        if (selectedPage == PAGE_LEADERBOARD) {
            renderLeaderboard();
        } else if (selectedPage == PAGE_PROFILE) {
            renderProfile();
        } else {
            renderToday();
        }
    }

    private void renderToday() {
        DoomStatsStore.Snapshot snapshot = DoomStatsStore.snapshot(this);
        addLargeTitle("Today", "Daily goal, streak, and a tiny bit of XP.");

        LinearLayout hero = card();
        hero.addView(label("Reels watched"));
        TextView count = text(String.valueOf(snapshot.todayReels), 64, palette.primaryText, Typeface.BOLD);
        count.setPadding(0, dp(4), 0, 0);
        hero.addView(count);
        hero.addView(progressBar(snapshot.targetProgressPercent));
        TextView target = secondary(snapshot.todayReels + " / " + snapshot.dailyTarget + " daily target");
        target.setPadding(0, dp(10), 0, 0);
        hero.addView(target);
        pageContainer.addView(withBottomMargin(hero, dp(14)));

        pageContainer.addView(streakCard(snapshot));
        pageContainer.addView(metricRow("XP", "+" + snapshot.xp, true));

        if (!FirebaseRepo.isSignedIn(this)) {
            pageContainer.addView(signInPrompt("Sign in to join the leaderboard."));
        }

        TextView open = primaryAction("Open Instagram");
        open.setOnClickListener(v -> openInstagram());
        pageContainer.addView(open, actionParams());
    }

    private void renderLeaderboard() {
        addLargeTitle("League", "Top doomscrollers today.");

        if (!FirebaseRepo.isConfigured(this)) {
            pageContainer.addView(infoCard("Firebase not connected", "Add app/google-services.json from Firebase Console, then rebuild."));
            return;
        }

        if (!FirebaseRepo.isSignedIn(this)) {
            pageContainer.addView(signInPrompt("Sign in with Google to see the leaderboard."));
            return;
        }

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        pageContainer.addView(list, matchWrap());
        list.addView(infoCard("Loading", "Fetching today’s league."));

        FirebaseRepo.loadLeaderboard(this, new FirebaseRepo.ResultCallback<List<FirebaseRepo.LeaderboardEntry>>() {
            @Override
            public void onSuccess(List<FirebaseRepo.LeaderboardEntry> entries) {
                runOnUiThread(() -> {
                    list.removeAllViews();
                    if (entries.isEmpty()) {
                        list.addView(infoCard("No league yet", "Watch a Reel and sync to appear here."));
                        return;
                    }
                    for (FirebaseRepo.LeaderboardEntry entry : entries) {
                        list.addView(leaderboardRow(entry));
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    list.removeAllViews();
                    list.addView(infoCard("Leaderboard unavailable", message));
                });
            }
        });
    }

    private void renderProfile() {
        DoomStatsStore.Snapshot snapshot = DoomStatsStore.snapshot(this);
        FirebaseUser user = FirebaseRepo.currentUser(this);
        addLargeTitle("Profile", "Name and daily target.");

        if (!FirebaseRepo.isConfigured(this)) {
            pageContainer.addView(infoCard("Firebase not connected", "Add app/google-services.json to enable Google sign-in."));
        }

        if (user == null) {
            pageContainer.addView(signInPrompt("Sign in to sync your profile and league score."));
        } else {
            LinearLayout profile = card();
            String name = displayName(snapshot, user);
            TextView avatar = text(initials(name), 22, palette.card, Typeface.BOLD);
            avatar.setGravity(Gravity.CENTER);
            avatar.setBackground(circle(palette.primaryText, 0));
            LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(54), dp(54));
            avatarParams.bottomMargin = dp(14);
            profile.addView(avatar, avatarParams);
            profile.addView(label("Signed in"));
            profile.addView(text(name, 28, palette.primaryText, Typeface.BOLD));
            profile.addView(secondary(user.getEmail() == null ? "Google account" : user.getEmail()));
            pageContainer.addView(withBottomMargin(profile, dp(14)));
        }

        EditText nameInput = input(displayName(snapshot, user));
        pageContainer.addView(fieldCard("Profile name", nameInput));

        EditText targetInput = input(String.valueOf(snapshot.dailyTarget));
        targetInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        pageContainer.addView(fieldCard("Daily target", targetInput));

        TextView save = primaryAction("Save");
        save.setOnClickListener(v -> saveProfile(nameInput, targetInput));
        pageContainer.addView(save, actionParams());

        if (user != null) {
            TextView signOut = secondaryAction("Sign out");
            signOut.setOnClickListener(v -> {
                FirebaseRepo.signOut(this);
                render();
            });
            pageContainer.addView(signOut, actionParams());
        }
    }

    private void saveProfile(EditText nameInput, EditText targetInput) {
        String name = nameInput.getText().toString().trim();
        int target;
        try {
            target = Integer.parseInt(targetInput.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter a valid target.", Toast.LENGTH_SHORT).show();
            return;
        }

        DoomStatsStore.setDisplayName(this, name);
        DoomStatsStore.setDailyTarget(this, target);
        FirebaseRepo.updateDisplayName(this, name, quietCallback("Profile saved"));
        FirebaseRepo.updateDailyTarget(this, target, quietCallback("Profile saved"));
        render();
    }

    private View signInPrompt(String message) {
        LinearLayout prompt = card();
        prompt.addView(label("Google"));
        TextView title = text("Join the league", 26, palette.primaryText, Typeface.BOLD);
        title.setPadding(0, dp(6), 0, dp(8));
        prompt.addView(title);
        prompt.addView(secondary(message));

        TextView signIn = primaryAction("Sign in with Google");
        signIn.setOnClickListener(v -> FirebaseRepo.signIn(this, new FirebaseRepo.ResultCallback<Void>() {
            @Override
            public void onSuccess(Void value) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Signed in", Toast.LENGTH_SHORT).show();
                    render();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());
            }
        }));
        LinearLayout.LayoutParams params = actionParams();
        params.topMargin = dp(14);
        prompt.addView(signIn, params);

        return withBottomMargin(prompt, dp(14));
    }

    private FirebaseRepo.ResultCallback<Void> quietCallback(String successMessage) {
        return new FirebaseRepo.ResultCallback<Void>() {
            @Override
            public void onSuccess(Void value) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, successMessage, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());
            }
        };
    }

    private View leaderboardRow(FirebaseRepo.LeaderboardEntry entry) {
        LinearLayout row = card();
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView rank = text(String.valueOf(entry.rank), 16, palette.secondaryText, Typeface.BOLD);
        rank.setGravity(Gravity.CENTER);
        row.addView(rank, new LinearLayout.LayoutParams(dp(34), LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView avatar = text(entry.initials(), 14, palette.card, Typeface.BOLD);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(circle(palette.primaryText, 0));
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(38), dp(38));
        avatarParams.leftMargin = dp(8);
        avatarParams.rightMargin = dp(12);
        row.addView(avatar, avatarParams);

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.addView(text(entry.displayName, 16, palette.primaryText, Typeface.BOLD));
        names.addView(secondary(entry.targetProgress + "% target"));
        row.addView(names, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout score = new LinearLayout(this);
        score.setOrientation(LinearLayout.VERTICAL);
        score.setGravity(Gravity.RIGHT);
        score.addView(text(String.valueOf(entry.reels), 22, palette.primaryText, Typeface.BOLD));
        score.addView(text("+" + entry.xp + " XP", 12, palette.tertiaryText, Typeface.NORMAL));
        row.addView(score);

        return withBottomMargin(row, dp(10));
    }

    private View fieldCard(String label, EditText input) {
        LinearLayout card = card();
        card.addView(label(label));
        LinearLayout.LayoutParams inputParams = matchWrap();
        inputParams.topMargin = dp(8);
        card.addView(input, inputParams);
        return withBottomMargin(card, dp(12));
    }

    private EditText input(String value) {
        EditText editText = new EditText(this);
        editText.setText(value);
        editText.setTextSize(18);
        editText.setSingleLine(true);
        editText.setTextColor(palette.primaryText);
        editText.setHintTextColor(palette.tertiaryText);
        editText.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        editText.setBackground(roundRect(palette.fill, dp(14), 0));
        editText.setPadding(dp(12), 0, dp(12), 0);
        return editText;
    }

    private View metricRow(String label, String value, boolean subtle) {
        LinearLayout row = card();
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView labelView = text(label, 16, subtle ? palette.secondaryText : palette.primaryText, Typeface.BOLD);
        row.addView(labelView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView valueView = text(value, subtle ? 18 : 24, subtle ? palette.secondaryText : palette.primaryText, Typeface.BOLD);
        row.addView(valueView);

        return withBottomMargin(row, dp(12));
    }

    private View streakCard(DoomStatsStore.Snapshot snapshot) {
        LinearLayout card = card();
        card.addView(label("Streak"));

        TextView streak = text(snapshot.streak + (snapshot.streak == 1 ? " day" : " days"), 34, palette.primaryText, Typeface.BOLD);
        streak.setPadding(0, dp(6), 0, dp(14));
        card.addView(streak);

        addCheckpointBar(card, snapshot.streak);

        int day = dayInCycle(snapshot.streak);
        String caption = day == WEEK_CHECKPOINTS
                ? "Weekly checkpoint reached."
                : day + " of " + WEEK_CHECKPOINTS + " checkpoints complete.";
        TextView note = secondary(caption);
        note.setPadding(0, dp(12), 0, 0);
        card.addView(note);

        return withBottomMargin(card, dp(14));
    }

    private void addCheckpointBar(LinearLayout parent, int streak) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        int filled = dayInCycle(streak);
        for (int i = 1; i <= WEEK_CHECKPOINTS; i++) {
            boolean active = i <= filled;
            TextView dot = text(active ? "✓" : "", 13, active ? Color.WHITE : palette.tertiaryText, Typeface.BOLD);
            dot.setGravity(Gravity.CENTER);
            dot.setBackground(circle(active ? palette.accent : palette.fill, active ? 0 : palette.separator));
            row.addView(dot, new LinearLayout.LayoutParams(dp(28), dp(28)));

            if (i < WEEK_CHECKPOINTS) {
                View line = new View(this);
                line.setBackgroundColor(i < filled ? palette.accent : palette.separator);
                LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(0, dp(2), 1f);
                lineParams.leftMargin = dp(6);
                lineParams.rightMargin = dp(6);
                row.addView(line, lineParams);
            }
        }

        parent.addView(row, matchWrap());
    }

    private View progressBar(int percent) {
        LinearLayout outer = new LinearLayout(this);
        outer.setBackground(roundRect(palette.fill, dp(5), 0));
        outer.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams outerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(10)
        );
        outerParams.topMargin = dp(8);
        outer.setLayoutParams(outerParams);

        View inner = new View(this);
        inner.setBackground(roundRect(palette.accent, dp(5), 0));
        outer.addView(inner, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                Math.max(1, percent)
        ));
        View spacer = new View(this);
        outer.addView(spacer, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                Math.max(0, 100 - percent)
        ));
        return outer;
    }

    private void renderSetupGate() {
        addLargeTitle("Enable Doomly", "Turn on the counter to continue.");

        LinearLayout card = card();
        card.addView(label("Required"));
        TextView title = text("Accessibility", 28, palette.primaryText, Typeface.BOLD);
        title.setPadding(0, dp(6), 0, dp(8));
        card.addView(title);
        card.addView(secondary("Open settings and enable Doomly Reel Counter. Android requires this step manually."));
        pageContainer.addView(withBottomMargin(card, dp(14)));

        TextView enable = primaryAction("Open Accessibility Settings");
        enable.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        pageContainer.addView(enable, actionParams());

        if (!isInstagramInstalled()) {
            TextView install = secondaryAction("Install Instagram");
            install.setOnClickListener(v -> openInstagram());
            pageContainer.addView(install, actionParams());
        }
    }

    private void addLargeTitle(String title, String subtitle) {
        TextView appName = text("Doomly", 15, palette.secondaryText, Typeface.BOLD);
        appName.setPadding(0, dp(4), 0, dp(6));
        pageContainer.addView(appName);

        TextView titleView = text(title, 36, palette.primaryText, Typeface.BOLD);
        pageContainer.addView(titleView);

        TextView subtitleView = text(subtitle, 16, palette.secondaryText, Typeface.NORMAL);
        subtitleView.setPadding(0, dp(4), 0, dp(22));
        pageContainer.addView(subtitleView);
    }

    private View infoCard(String title, String body) {
        LinearLayout card = card();
        card.addView(text(title, 20, palette.primaryText, Typeface.BOLD));
        TextView message = secondary(body);
        message.setPadding(0, dp(8), 0, 0);
        card.addView(message);
        return withBottomMargin(card, dp(14));
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(roundRect(palette.card, dp(24), palette.separator));
        return card;
    }

    private TextView tab(String title, int page) {
        TextView tab = text(title, 13, palette.secondaryText, Typeface.BOLD);
        tab.setGravity(Gravity.CENTER);
        tab.setOnClickListener(v -> {
            selectedPage = page;
            render();
        });
        return tab;
    }

    private void styleTab(TextView tab, boolean selected) {
        tab.setTextColor(selected ? palette.card : palette.secondaryText);
        tab.setBackground(roundRect(selected ? palette.primaryText : Color.TRANSPARENT, dp(18), 0));
    }

    private TextView primaryAction(String label) {
        return action(label, palette.card, palette.primaryText, 0);
    }

    private TextView secondaryAction(String label) {
        return action(label, palette.primaryText, palette.card, palette.separator);
    }

    private TextView action(String label, int textColor, int background, int stroke) {
        TextView action = text(label, 16, textColor, Typeface.BOLD);
        action.setGravity(Gravity.CENTER);
        action.setBackground(roundRect(background, dp(18), stroke));
        action.setClickable(true);
        action.setFocusable(true);
        return action;
    }

    private TextView label(String value) {
        TextView label = text(value, 13, palette.secondaryText, Typeface.BOLD);
        label.setAllCaps(true);
        return label;
    }

    private TextView secondary(String value) {
        return text(value, 14, palette.secondaryText, Typeface.NORMAL);
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", style));
        view.setLineSpacing(0, 1.08f);
        return view;
    }

    private View withBottomMargin(View view, int margin) {
        LinearLayout.LayoutParams params = matchWrap();
        params.bottomMargin = margin;
        view.setLayoutParams(params);
        return view;
    }

    private LinearLayout.LayoutParams actionParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
        );
        params.bottomMargin = dp(12);
        return params;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams tabParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
        );
        params.leftMargin = dp(3);
        params.rightMargin = dp(3);
        return params;
    }

    private GradientDrawable roundRect(int color, int radius, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeColor != 0) {
            drawable.setStroke(dp(1), strokeColor);
        }
        return drawable;
    }

    private GradientDrawable circle(int color, int strokeColor) {
        GradientDrawable drawable = roundRect(color, dp(14), strokeColor);
        drawable.setShape(GradientDrawable.OVAL);
        return drawable;
    }

    private int dayInCycle(int streak) {
        if (streak <= 0) {
            return 0;
        }
        int day = streak % WEEK_CHECKPOINTS;
        return day == 0 ? WEEK_CHECKPOINTS : day;
    }

    private String displayName(DoomStatsStore.Snapshot snapshot, FirebaseUser user) {
        if (!snapshot.displayName.isEmpty()) {
            return snapshot.displayName;
        }
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            return user.getDisplayName().trim();
        }
        return "Doomly user";
    }

    private String initials(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            return "D";
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    private boolean isAccessibilityServiceEnabled() {
        String enabledServices = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabledServices)) {
            return false;
        }
        ComponentName expected = new ComponentName(this, DoomlyAccessibilityService.class);
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabledServices);
        while (splitter.hasNext()) {
            ComponentName enabled = ComponentName.unflattenFromString(splitter.next());
            if (expected.equals(enabled)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInstagramInstalled() {
        try {
            getPackageManager().getPackageInfo("com.instagram.android", 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    private void openInstagram() {
        Intent launch = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage("com.instagram.android")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (launch.resolveActivity(getPackageManager()) != null) {
            startActivity(launch);
            return;
        }

        launch = getPackageManager().getLaunchIntentForPackage("com.instagram.android");
        if (launch != null) {
            startActivity(launch);
            return;
        }

        Intent reelsWeb = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/reels/"));
        reelsWeb.setPackage("com.instagram.android");
        if (reelsWeb.resolveActivity(getPackageManager()) != null) {
            startActivity(reelsWeb);
            return;
        }

        Intent browserReels = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/reels/"));
        if (browserReels.resolveActivity(getPackageManager()) != null) {
            startActivity(browserReels);
            return;
        }

        Intent playStore = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.instagram.android"));
        if (playStore.resolveActivity(getPackageManager()) != null) {
            startActivity(playStore);
            return;
        }

        Toast.makeText(this, "Instagram is not installed.", Toast.LENGTH_LONG).show();
    }

    private void applySystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(palette.background);
        window.setNavigationBarColor(palette.card);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = palette.dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !palette.dark) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }

    private boolean isDarkMode() {
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class Palette {
        final boolean dark;
        final int background;
        final int card;
        final int fill;
        final int primaryText;
        final int secondaryText;
        final int tertiaryText;
        final int separator;
        final int accent;

        private Palette(boolean dark, int background, int card, int fill, int primaryText, int secondaryText, int tertiaryText, int separator, int accent) {
            this.dark = dark;
            this.background = background;
            this.card = card;
            this.fill = fill;
            this.primaryText = primaryText;
            this.secondaryText = secondaryText;
            this.tertiaryText = tertiaryText;
            this.separator = separator;
            this.accent = accent;
        }

        static Palette from(boolean dark) {
            if (dark) {
                return new Palette(true, 0xFF000000, 0xFF1C1C1E, 0xFF2C2C2E, 0xFFFFFFFF, 0xFFAEAEB2, 0xFF636366, 0xFF38383A, 0xFF0A84FF);
            }
            return new Palette(false, 0xFFF2F2F7, 0xFFFFFFFF, 0xFFE5E5EA, 0xFF000000, 0xFF6E6E73, 0xFF8E8E93, 0xFFD1D1D6, 0xFF007AFF);
        }
    }
}
