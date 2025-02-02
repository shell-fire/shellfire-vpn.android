package de.shellfire.vpn.android;

import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * GoogleMapController implements MapController using Google Maps.
 */
public class GoogleMapController implements MapController, OnMapReadyCallback {
    private static final String TAG = "GoogleMapController";
    private GoogleMap googleMap;
    private Fragment parentFragment;
    private Marker marker;
    private Boolean pendingMapStyle = null; // Stores the pending style change
    public GoogleMapController(Fragment fragment) {
        this.parentFragment = fragment;
    }

    @Override
    public void initialize() {
        // Look for the SupportMapFragment in the layout (assumed to have id "map")
        SupportMapFragment mapFragment = (SupportMapFragment)
                parentFragment.getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "Map fragment not found in layout");
        }
    }

    // Ensure style is applied in onMapReady()
    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setZoomGesturesEnabled(false);
        googleMap.getUiSettings().setScrollGesturesEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(3.0f));

        // Apply stored style if updateMapStyle() was called before the map was ready
        if (pendingMapStyle != null) {
            applyMapStyle(pendingMapStyle);
            pendingMapStyle = null; // Reset after applying
        }
    }

    @Override
    public void updateMapStyle(boolean connected) {
        if (googleMap != null) {
            applyMapStyle(connected);
        } else {
            // Store the requested style to be applied later when map is ready
            pendingMapStyle = connected;
        }
    }

    // Apply the style when the map is ready
    private void applyMapStyle(boolean connected) {
        FragmentActivity activity = parentFragment.getActivity();
        if (activity != null) {
            int styleResId = connected ? R.raw.style_json_green : R.raw.style_json;
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(activity, styleResId));
        }
    }



    @Override
    public void setCoordinates(double latitude, double longitude) {
        if (googleMap != null) {
            LatLng newCoordinates = new LatLng(latitude, longitude);
            if (marker != null) {
                marker.remove();
            }
            marker = googleMap.addMarker(new MarkerOptions().position(newCoordinates));
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(newCoordinates));
        }
    }

    @Override
    public void animateZoom(float zoomLevel) {
        if (googleMap != null) {
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(zoomLevel));
        }
    }
}
