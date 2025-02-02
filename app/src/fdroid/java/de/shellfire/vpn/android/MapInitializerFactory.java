package de.shellfire.vpn.android;

import android.content.Context;

public class MapInitializerFactory {
    public static MapInitializer create(Context context) {
            return new FdroidMapInitializer();
    }
}
