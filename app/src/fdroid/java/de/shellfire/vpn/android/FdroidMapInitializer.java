package de.shellfire.vpn.android;

import android.util.Log;

public class FdroidMapInitializer implements MapInitializer {

    private static final String TAG = "FdroidMapInitializer";

    @Override
    public void initializeMaps() {
        Log.d(TAG, "Google Maps not available in F-Droid flavor. Skipping map initialization.");
    }
}
