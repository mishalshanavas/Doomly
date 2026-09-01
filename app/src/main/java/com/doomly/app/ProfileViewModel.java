package com.doomly.app;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.doomly.app.data.AuthRepository;
import com.doomly.app.data.StatsRepository;

import java.util.Locale;

/**
 * ViewModel for the Profile tab.
 */
public class ProfileViewModel extends AndroidViewModel {

    private final StatsRepository statsRepo;
    private final AuthRepository authRepo;

    private final MutableLiveData<String> displayName = new MutableLiveData<>();
    private final MutableLiveData<String> initials = new MutableLiveData<>();
    private final MutableLiveData<String> email = new MutableLiveData<>();
    private final MutableLiveData<String> streak = new MutableLiveData<>();
    private final MutableLiveData<String> totalReels = new MutableLiveData<>();
    private final MutableLiveData<String> totalLikes = new MutableLiveData<>();
    private final MutableLiveData<String> rank = new MutableLiveData<>();
    private final MutableLiveData<Integer> dailyTarget = new MutableLiveData<>();
    private final MutableLiveData<Boolean> signedIn = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        statsRepo = StatsRepository.getInstance();
        authRepo = AuthRepository.getInstance();
        refresh();
    }

    public void refresh() {
        DoomStatsStore.Snapshot snap = statsRepo.getSnapshot(getApplication());
        boolean isSignedIn = authRepo.isSignedIn(getApplication());

        String name = isSignedIn ? authRepo.currentDisplayName(getApplication()) : "";
        if (name.isEmpty()) name = snap.displayName;
        if (name.isEmpty()) name = "Doomly user";

        displayName.setValue(name);
        initials.setValue(computeInitials(name));
        email.setValue(isSignedIn ? authRepo.currentEmail(getApplication()) : "Local profile");
        if (email.getValue() == null || email.getValue().isEmpty())
            email.setValue("Local profile");
        streak.setValue(String.valueOf(snap.streak));
        totalReels.setValue(String.valueOf(snap.totalReels));
        totalLikes.setValue("-");
        rank.setValue("-");
        dailyTarget.setValue(snap.dailyTarget);
        signedIn.setValue(isSignedIn);
    }

    public LiveData<String> getDisplayName() { return displayName; }
    public LiveData<String> getInitials() { return initials; }
    public LiveData<String> getEmail() { return email; }
    public LiveData<String> getStreak() { return streak; }
    public LiveData<String> getTotalReels() { return totalReels; }
    public LiveData<String> getTotalLikes() { return totalLikes; }
    public LiveData<String> getRank() { return rank; }
    public LiveData<Integer> getDailyTarget() { return dailyTarget; }
    public LiveData<Boolean> getSignedIn() { return signedIn; }
    public StatsRepository getStatsRepo() { return statsRepo; }
    public AuthRepository getAuthRepo() { return authRepo; }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String computeInitials(String name) {
        String t = name == null ? "" : name.trim();
        if (t.isEmpty()) return "D";
        String[] parts = t.split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase(Locale.US);
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.US);
    }
}
