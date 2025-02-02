package de.shellfire.vpn.android;

/**
 * StubMapController is a no‑op implementation of MapController used in FDroid builds.
 */
public class StubMapController implements MapController {
    @Override
    public void initialize() {
        // No operation.
    }

    @Override
    public void updateMapStyle(boolean connected) {
        // No operation.
    }

    @Override
    public void setCoordinates(double latitude, double longitude) {
        // No operation.
    }

    @Override
    public void animateZoom(float zoomLevel) {
        // No operation.
    }
}
