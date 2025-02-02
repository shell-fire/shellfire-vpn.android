package de.shellfire.vpn.android;

import android.content.Context;
import android.util.Log;

/**
 * FDroid stub for AppRater.
 * Google review functionality is disabled in this variant.
 */
public class AppRater {

    private static final String TAG = "AppRater";

    public AppRater(Context context) {
        // Optionally, initialize anything needed for the FDroid version.
    }

    public void logAction() {
        // Stub: record the action if needed, or do nothing.
    }

    public void triggerReview() {
        // Since review functionality is not available, simply log or show a fallback.
        Log.d(TAG, "triggerReview called: review functionality is disabled in FDroid build.");
    }

    public void rateApp() {
        // Optionally, you can show a simple dialog informing the user or simply do nothing.
        Log.d(TAG, "rateApp called: review functionality is disabled in FDroid build.");
    }
}
