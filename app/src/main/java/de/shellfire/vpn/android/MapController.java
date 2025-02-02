package de.shellfire.vpn.android;

/**
 * MapController abstracts map operations so that MainSectionFragment need not directly reference
 * any Google Maps classes. Flavor-specific implementations will be provided.
 */
public interface MapController {
    /**
     * Initializes the map controller (for example, by loading the map asynchronously).
     */
    void initialize();

    /**
     * Updates the map style based on connection status.
     * @param connected true if VPN is connected; false otherwise.
     */
    void updateMapStyle(boolean connected);

    /**
     * Sets the map’s coordinates (for example, to show the server location).
     * @param latitude  the latitude value.
     * @param longitude the longitude value.
     */
    void setCoordinates(double latitude, double longitude);

    /**
     * Animates the map zoom level.
     * @param zoomLevel the desired zoom level.
     */
    void animateZoom(float zoomLevel);
}
