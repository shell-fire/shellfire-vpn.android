package de.shellfire.vpn.android.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";
    private static final int RETRY_DELAY_MS = 5000; // Retry after 5 seconds
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private int retryCount = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive, intent.getAction()=" + intent.getAction());
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()) ||
                "android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {
            startVpnStatusService(context);
        }
    }


    private void startVpnStatusService(Context context) {
        // WorkManager direkt starten statt ForegroundService
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(WidgetUpdateWorker.class).build();
        WorkManager.getInstance(context).enqueue(workRequest);
        Log.d(TAG, "Widget update triggered via WorkManager.");
    }


    private void handleRetry(Context context) {
        if (retryCount < MAX_RETRY_ATTEMPTS) {
            retryCount++;
            Log.d(TAG, "Retrying to start VpnStatusService, attempt " + retryCount);
            new Handler(Looper.getMainLooper()).postDelayed(() -> startVpnStatusService(context), RETRY_DELAY_MS);
        } else {
            Log.e(TAG, "Max retry attempts reached. VpnStatusService could not be started.");
        }
    }
}
