package de.shellfire.vpn.android.widget;

import android.util.Log;

import androidx.lifecycle.LifecycleService;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class VpnStatusService extends LifecycleService {

    private static final String TAG = "VpnStatusService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "VpnStatusService onCreate - Starting WorkManager for widget updates");

        // Direkt den WorkManager-Task starten
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(WidgetUpdateWorker.class).build();
        WorkManager.getInstance(this).enqueue(workRequest);

        // Keine Foreground-Notification notwendig
        stopSelf(); // Service kann beendet werden
    }
}
