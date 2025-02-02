package de.shellfire.vpn.android;

/**
 * MapDisplay abstracts the map operations.
 */
public interface MapDisplay {
    /**
     * Initialize the map (or map stub).
     */
    void init();

    /**
     * Add a marker at the given coordinate with a title.
     * @param coord the coordinate.
     * @param title the marker title.
     */
    void addMarker(Coord coord, String title);

    /**
     * Move the camera to the given coordinate.
     * @param coord the coordinate.
     */
    void moveCamera(Coord coord);
}
