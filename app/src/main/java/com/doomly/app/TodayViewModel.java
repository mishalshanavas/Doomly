package com.doomly.app;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.doomly.app.data.AuthRepository;
import com.doomly.app.data.StatsRepository;


/**
 * ViewModel for the Today tab.
 * Exposes stats as LiveData so the Fragment can observe changes.
 */
public class TodayViewModel extends AndroidViewModel {

    private static final int WEEK_CHECKPOINTS = 7;

    private final StatsRepository statsRepo;
    private final AuthRepository authRepo;

    private final MutableLiveData<DoomStatsStore.Snapshot> snapshot = new MutableLiveData<>();
    private final MutableLiveData<String> coachMessage = new MutableLiveData<>();
    private final MutableLiveData<Integer> coachEmotion = new MutableLiveData<>();
    private final MutableLiveData<Boolean> signedIn = new MutableLiveData<>();
    private final MutableLiveData<int[]> streakDays = new MutableLiveData<>();

    public TodayViewModel(@NonNull Application application) {
        super(application);
        statsRepo = StatsRepository.getInstance();
        authRepo = AuthRepository.getInstance();
        refresh();
    }

    public void refresh() {
        DoomStatsStore.Snapshot snap = statsRepo.getSnapshot(getApplication());
        snapshot.setValue(snap);
        coachMessage.setValue(buildCoachMessage(snap));
        coachEmotion.setValue(snap.todayReels == 0 ? 0  // BORED
                : snap.todayReels >= snap.dailyTarget ? 3  // GOAL_REACHED
                : snap.todayReels > snap.dailyTarget * 0.7f ? 2  // NEAR_GOAL
                : 1);  // IDLE
        signedIn.setValue(authRepo.isSignedIn(getApplication()));
        streakDays.setValue(buildStreakDays(snap.streak));
    }

    public LiveData<DoomStatsStore.Snapshot> getSnapshot() { return snapshot; }
    public LiveData<String> getCoachMessage() { return coachMessage; }
    public LiveData<Integer> getCoachEmotion() { return coachEmotion; }
    public LiveData<Boolean> getSignedIn() { return signedIn; }
    public LiveData<int[]> getStreakDays() { return streakDays; }

    public void signIn(ResultCallback<Void> callback) {
        callback.onError("Sign-in must be started from an Activity.");
    }

    public AuthRepository getAuthRepo() { return authRepo; }
    public StatsRepository getStatsRepo() { return statsRepo; }

    // ── Coach message ─────────────────────────────────────────────────────

    private String buildCoachMessage(DoomStatsStore.Snapshot snap) {
        return MotivationEngine.message(snap.todayReels, snap.dailyTarget, snap.streak);
    }

    // ── Streak dots ───────────────────────────────────────────────────────

    private int[] buildStreakDays(int streak) {
        int completed = dayInCycle(streak);
        int active = completed <= 0 ? 1 : Math.min(WEEK_CHECKPOINTS, completed);
        int[] states = new int[WEEK_CHECKPOINTS];
        for (int i = 1; i <= WEEK_CHECKPOINTS; i++) {
            if (completed > 0 && i <= completed) states[i - 1] = 2;      // active
            else if (i == active) states[i - 1] = 1;                      // current
            else states[i - 1] = 0;                                        // inactive
        }
        return states;
    }

    private int dayInCycle(int streak) {
        if (streak <= 0) return 0;
        int day = streak % WEEK_CHECKPOINTS;
        return day == 0 ? WEEK_CHECKPOINTS : day;
    }
}
