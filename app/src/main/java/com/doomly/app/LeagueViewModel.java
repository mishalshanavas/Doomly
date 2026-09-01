package com.doomly.app;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.doomly.app.ResultCallback;
import com.doomly.app.data.AuthRepository;
import com.doomly.app.data.LeaderboardRepository;
import com.doomly.app.data.StatsRepository;

import java.util.List;

/**
 * ViewModel for the League tab.
 */
public class LeagueViewModel extends AndroidViewModel {

    private final StatsRepository statsRepo;
    private final AuthRepository authRepo;
    private final LeaderboardRepository leaderboardRepo;

    private final MutableLiveData<DoomStatsStore.Snapshot> snapshot = new MutableLiveData<>();
    private final MutableLiveData<List<LeaderboardRepository.LeaderboardEntry>> entries = new MutableLiveData<>();
    private final MutableLiveData<Boolean> signedIn = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorTitle = new MutableLiveData<>();
    private final MutableLiveData<String> errorBody = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showEmpty = new MutableLiveData<>(true);
    private final MutableLiveData<String> summaryReels = new MutableLiveData<>();
    private final MutableLiveData<String> summaryMeta = new MutableLiveData<>();
    private final MutableLiveData<String> summaryActionText = new MutableLiveData<>();
    private final MutableLiveData<Integer> maxReels = new MutableLiveData<>(1);

    public LeagueViewModel(@NonNull Application application) {
        super(application);
        statsRepo = StatsRepository.getInstance();
        authRepo = AuthRepository.getInstance();
        leaderboardRepo = LeaderboardRepository.getInstance();
        refresh();
    }

    public void refresh() {
        boolean isSignedIn = authRepo.isSignedIn(getApplication());
        signedIn.setValue(isSignedIn);

        if (!authRepo.isConfigured(getApplication())) {
            showErrorState("Supabase not connected", "Check your supabase_url and anon_key in strings.xml.");
            return;
        }

        if (!isSignedIn) {
            showSignInState();
            return;
        }

        // Auto-sync local stats to Supabase, then load the leaderboard
        loading.setValue(true);
        statsRepo.syncToCloud(getApplication(),
                authRepo.currentUid(getApplication()),
                authRepo.currentDisplayName(getApplication()),
                "",
                new ResultCallback<Void>() {
                    @Override public void onSuccess(Void v) { loadLeaderboard(); }
                    @Override public void onError(String m) { loadLeaderboard(); }
                });
    }

    public void loadLeaderboard() {
        loading.postValue(true);
        leaderboardRepo.loadLeaderboard(getApplication(), new ResultCallback<List<LeaderboardRepository.LeaderboardEntry>>() {
            @Override
            public void onSuccess(List<LeaderboardRepository.LeaderboardEntry> list) {
                loading.postValue(false);
                if (list == null || list.isEmpty()) {
                    showErrorState("You are not ranked yet",
                            "Watch and sync your first session to join the board.");
                    return;
                }
                int max = 1;
                for (LeaderboardRepository.LeaderboardEntry e : list) max = Math.max(max, e.reels);
                maxReels.postValue(max);
                entries.postValue(list);
                showEmpty.postValue(false);
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                errorTitle.postValue("League unavailable");
                errorBody.postValue(message);
                showEmpty.postValue(true);
            }
        });
    }

    private void showSignInState() {
        signedIn.setValue(false);
        showEmpty.setValue(true);
    }

    private void showErrorState(String title, String body) {
        errorTitle.setValue(title);
        errorBody.setValue(body);
        showEmpty.setValue(true);
    }

    public LiveData<DoomStatsStore.Snapshot> getSnapshot() { return snapshot; }
    public LiveData<List<LeaderboardRepository.LeaderboardEntry>> getEntries() { return entries; }
    public LiveData<Boolean> getSignedIn() { return signedIn; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getErrorTitle() { return errorTitle; }
    public LiveData<String> getErrorBody() { return errorBody; }
    public LiveData<Boolean> getShowEmpty() { return showEmpty; }
    public LiveData<String> getSummaryReels() { return summaryReels; }
    public LiveData<String> getSummaryMeta() { return summaryMeta; }
    public LiveData<String> getSummaryActionText() { return summaryActionText; }
    public LiveData<Integer> getMaxReels() { return maxReels; }
    public AuthRepository getAuthRepo() { return authRepo; }
    public StatsRepository getStatsRepo() { return statsRepo; }
}
