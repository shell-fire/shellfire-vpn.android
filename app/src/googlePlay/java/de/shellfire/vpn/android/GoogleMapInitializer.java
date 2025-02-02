package de.shellfire.vpn.android;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapsSdkInitializedCallback;

public class GoogleMapInitializer implements MapInitializer, OnMapsSdkInitializedCallback {

    private static final String TAG = "GoogleMapInitializer";
    private final Context context;

    public GoogleMapInitializer(Context context) {
        this.context = context;
    }

    @Override
    public void initializeMaps() {
        Log.d(TAG, "Initializing Google Maps");
        MapsInitializer.initialize(context, MapsInitializer.Renderer.LATEST, this);
    }

    @Override
    public void onMapsSdkInitialized(MapsInitializer.Renderer renderer) {
        if (renderer == MapsInitializer.Renderer.LATEST) {
            Log.d(TAG, "Google Maps Renderer: LATEST");
        } else {
            Log.d(TAG, "Google Maps Renderer: LEGACY");
        }
    }
}
