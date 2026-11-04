package com.doomly.app;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.doomly.app.data.StatsRepository;

/**
 * ViewModel for the Today tab.
 * Exposes the stats and coach message consumed by the Today screen.
 */
public class TodayViewModel extends AndroidViewModel {

    private final StatsRepository statsRepo;

    private final MutableLiveData<DoomStatsStore.Snapshot> snapshot = new MutableLiveData<>();
    private final MutableLiveData<String> coachMessage = new MutableLiveData<>();

    public TodayViewModel(@NonNull Application application) {
        super(application);
        statsRepo = StatsRepository.getInstance();
        refresh();
    }

    public void refresh() {
        DoomStatsStore.Snapshot snap = statsRepo.getSnapshot(getApplication());
        snapshot.setValue(snap);
        coachMessage.setValue(buildCoachMessage(snap));
    }

    public LiveData<DoomStatsStore.Snapshot> getSnapshot() { return snapshot; }
    public LiveData<String> getCoachMessage() { return coachMessage; }

    // ── Coach message ─────────────────────────────────────────────────────

    private String buildCoachMessage(DoomStatsStore.Snapshot snap) {
        return MotivationEngine.message(snap.todayReels, snap.dailyTarget, snap.streak);
    }
}
