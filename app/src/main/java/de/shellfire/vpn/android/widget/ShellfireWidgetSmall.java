package de.shellfire.vpn.android.widget;

import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

import de.shellfire.vpn.android.R;

/**
 * Created by Alina on 13.02.2018.
 */

public class ShellfireWidgetSmall extends ShellfireWidget {
    @Override
    public RemoteViews getViews(Context context) {
        return new RemoteViews(context.getPackageName(), R.layout.widget_layout_small);
    }

    @Override
    public ComponentName getWidgetComponentName(Context context) {
        return new ComponentName(context, ShellfireWidgetSmall.class);
    }
}
