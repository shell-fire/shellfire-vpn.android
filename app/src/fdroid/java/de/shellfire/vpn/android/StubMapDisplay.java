package de.shellfire.vpn.android;

import android.util.Log;

/**
 * StubMapDisplay is a no‑operation implementation of MapDisplay used for FDroid.
 */
public class StubMapDisplay implements MapDisplay {

    private static final String TAG = "StubMapDisplay";

    @Override
    public void init() {
        Log.d(TAG, "init() called: Map functionality is disabled in FDroid build.");
    }

    @Override
    public void addMarker(Coord coord, String title) {
        Log.d(TAG, "addMarker() called: No operation in FDroid build.");
    }

    @Override
    public void moveCamera(Coord coord) {
        Log.d(TAG, "moveCamera() called: No operation in FDroid build.");
    }
}
