package de.shellfire.vpn.android;

import androidx.fragment.app.FragmentActivity;

/**
 * Factory that returns a Google Maps–enabled MapDisplay.
 */
public class MapDisplayFactory {
    public static MapDisplay create(FragmentActivity activity) {
        return new GoogleMapDisplay(activity);
    }
}
