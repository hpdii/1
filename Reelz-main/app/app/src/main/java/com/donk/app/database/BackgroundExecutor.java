package com.donk.app.database;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Chay cac thao tac nang o mot thread rieng de khong block UI,
 * roi tra ket qua ve main thread.
 */
public final class BackgroundExecutor {

    public interface Query<T> {
        T execute();
    }

    public interface OnResult<T> {
        void onResult(T result);
    }

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private BackgroundExecutor() {
    }

    public static <T> void runQuery(Query<T> query, OnResult<T> onResult) {
        executorService.execute(() -> {
            T result = query.execute();
            mainHandler.post(() -> onResult.onResult(result));
        });
    }

    public static void execute(Runnable task) {
        executorService.execute(task);
    }
}
