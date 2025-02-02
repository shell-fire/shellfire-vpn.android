package de.shellfire.vpn.android;

import android.util.Log;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * GoogleMapDisplay implements MapDisplay using the Google Maps API.
 */
public class GoogleMapDisplay implements MapDisplay, OnMapReadyCallback {

    private static final String TAG = "GoogleMapDisplay";
    private GoogleMap mMap;
    private FragmentActivity activity;

    public GoogleMapDisplay(FragmentActivity activity) {
        this.activity = activity;
    }

    @Override
    public void init() {
        // Assumes that the layout (activity_maps.xml) contains a SupportMapFragment with id "map".
        SupportMapFragment mapFragment = (SupportMapFragment)
                activity.getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "MapFragment not found in layout.");
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Optional: Set any default settings for the map here.
        mMap.getUiSettings().setZoomControlsEnabled(false);
    }

    @Override
    public void addMarker(Coord coord, String title) {
        if (mMap != null) {
            LatLng position = new LatLng(coord.latitude, coord.longitude);
            mMap.addMarker(new MarkerOptions().position(position).title(title));
        } else {
            Log.w(TAG, "addMarker: Map not ready.");
        }
    }

    @Override
    public void moveCamera(Coord coord) {
        if (mMap != null) {
            LatLng position = new LatLng(coord.latitude, coord.longitude);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
        } else {
            Log.w(TAG, "moveCamera: Map not ready.");
        }
    }
}
