package de.shellfire.vpn.android;

/**
 * A simple coordinate class to avoid direct references to Google’s LatLng.
 */
public class Coord {
    public final double latitude;
    public final double longitude;

    public Coord(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
