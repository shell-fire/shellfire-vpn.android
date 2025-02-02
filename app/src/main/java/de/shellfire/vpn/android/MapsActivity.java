package de.shellfire.vpn.android;

import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;

/**
 * MapsActivity is flavor-neutral. It delegates map operations to a MapDisplay.
 * In the googlePlay build the MapDisplay will be a full Google Maps implementation;
 * in the FDroid build it will be a stub.
 */
public class MapsActivity extends FragmentActivity {

    private MapDisplay mapDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain a flavor-specific MapDisplay instance.
        mapDisplay = MapDisplayFactory.create(this);
        mapDisplay.init();

        // For demonstration: add a marker and move the camera.
        mapDisplay.addMarker(new Coord(-34, 151), "Marker in Sydney");
        mapDisplay.moveCamera(new Coord(-34, 151));
    }
}
