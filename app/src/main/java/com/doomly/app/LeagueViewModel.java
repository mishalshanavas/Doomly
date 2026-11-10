package com.doomly.app;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.doomly.app.data.AuthRepository;
import com.doomly.app.data.LeaderboardRepository;
import com.doomly.app.data.StatsRepository;

import java.util.List;

/**
 * ViewModel for the League tab.
 */
public class LeagueViewModel extends AndroidViewModel {

    private String cachedUid = "", cachedDay = "";
    private final StatsRepository statsRepo;
    private final AuthRepository authRepo;
    private final LeaderboardRepository leaderboardRepo;

    private final MutableLiveData<List<LeaderboardRepository.LeaderboardEntry>> entries = new MutableLiveData<>();
    private final MutableLiveData<Boolean> signedIn = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorTitle = new MutableLiveData<>();
    private final MutableLiveData<String> errorBody = new MutableLiveData<>();

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
        String uid = authRepo.currentUid(getApplication());
        String day = java.time.LocalDate.now().toString();
        if (!uid.equals(cachedUid) || !day.equals(cachedDay)) entries.setValue(java.util.Collections.emptyList());
        cachedUid = uid; cachedDay = day;
        if (Boolean.TRUE.equals(loading.getValue())) return;
        errorTitle.setValue(null);
        errorBody.setValue(null);

        if (!authRepo.isConfigured(getApplication())) {
            showErrorState("League unavailable", "Rankings are not available right now. Your local counts are safe.");
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
        final String requestedUid = authRepo.currentUid(getApplication());
        final String requestedDay = java.time.LocalDate.now().toString();
        leaderboardRepo.loadLeaderboard(getApplication(), new ResultCallback<List<LeaderboardRepository.LeaderboardEntry>>() {
            @Override
            public void onSuccess(List<LeaderboardRepository.LeaderboardEntry> list) {
                if (!requestedUid.equals(authRepo.currentUid(getApplication())) || !requestedDay.equals(java.time.LocalDate.now().toString())) { loading.postValue(false); return; }
                loading.postValue(false);
                errorTitle.postValue(null);
                errorBody.postValue(null);
                if (list == null || list.isEmpty()) {
                    entries.postValue(java.util.Collections.emptyList());
                    showErrorState("You are not ranked yet",
                            "Watch your first Reels to join today's board.");
                    return;
                }
                entries.postValue(list);
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                errorTitle.postValue("League unavailable");
                errorBody.postValue("Couldn't refresh rankings. Check your connection and try again.");
            }
        });
    }

    private void showSignInState() {
        entries.setValue(java.util.Collections.emptyList());
        loading.setValue(false);
        signedIn.setValue(false);
    }

    private void showErrorState(String title, String body) {
        loading.postValue(false);
        errorTitle.postValue(title);
        errorBody.postValue(body);
    }

    public LiveData<List<LeaderboardRepository.LeaderboardEntry>> getEntries() { return entries; }
    public LiveData<Boolean> getSignedIn() { return signedIn; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getErrorTitle() { return errorTitle; }
    public LiveData<String> getErrorBody() { return errorBody; }
}
