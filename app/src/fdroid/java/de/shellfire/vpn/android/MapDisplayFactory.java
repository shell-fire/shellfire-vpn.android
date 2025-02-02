package de.shellfire.vpn.android;

import androidx.fragment.app.FragmentActivity;

/**
 * Factory that returns a stub MapDisplay for FDroid builds.
 */
public class MapDisplayFactory {
    public static MapDisplay create(FragmentActivity activity) {
        return new StubMapDisplay();
    }
}
