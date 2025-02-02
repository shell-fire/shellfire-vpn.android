package de.shellfire.vpn.android;

import androidx.fragment.app.Fragment;

/**
 * Factory to create a MapController instance.
 * In the googlePlay flavor, returns a full GoogleMapController.
 */
public class MapControllerFactory {
    public static MapController create(Fragment fragment) {
        return new GoogleMapController(fragment);
    }
}
