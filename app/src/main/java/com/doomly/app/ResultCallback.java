package com.doomly.app;

/**
 * Shared callback for async repository operations.
 * Shared by UI, Supabase, and local repository layers.
 */
public interface ResultCallback<T> {
    void onSuccess(T value);
    void onError(String message);
}
