package de.shellfire.vpn.android.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Arrays;
import java.util.Locale;

import de.shellfire.vpn.android.R;
import de.shellfire.vpn.android.Server;
import de.shellfire.vpn.android.SimpleConnectionStatus;
import de.shellfire.vpn.android.VpnConnectionManager;
import de.shellfire.vpn.android.VpnRepository;
import de.shellfire.vpn.android.utils.CountryUtils;

public class ShellfireWidget extends AppWidgetProvider {

    public static final String ACTION_UPDATE_WIDGET = "de.shellfire.vpn.android.widget.ACTION_UPDATE_WIDGET";

    public static final String EXTRA_SERVER_INFO = "SERVER_INFO";
    protected static final String MyOnClick = "myOnClickTag";
    private final static String TAG = ShellfireWidget.class.getCanonicalName();
    public static final String EXTRA_CONNECTION_STATUS = "CONNECTION_STATUS";
    protected static int[] appWidgetIds;
    private static RemoteViews views;
    private String currentLanguage;

    @Override
    public void onReceive(final Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.d(TAG, "onReceive " + intent.getAction());
        if (MyOnClick.equals(intent.getAction())) {
            Log.d(TAG, "Button clicked. Toggling connection state...");
            VpnConnectionManager vpnConnectionManager = VpnConnectionManager.getInstance(context.getApplicationContext());
            vpnConnectionManager.toggleConnect();

            // Send immediate broadcast for syncing widgets
            SimpleConnectionStatus status = vpnConnectionManager.getConnectionStatus().getValue();
            Server server = VpnRepository.getInstance(context.getApplicationContext()).getSelectedServer().getValue();
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                updateWidget(context, server, status, appWidgetId);
            }
        } else if (ACTION_UPDATE_WIDGET.equals(intent.getAction())) {
            Log.d(TAG, "Received widget update broadcast.");
            SimpleConnectionStatus status = (SimpleConnectionStatus) intent.getSerializableExtra(EXTRA_CONNECTION_STATUS);
            Server server = (Server) intent.getSerializableExtra(EXTRA_SERVER_INFO);

            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                updateWidget(context, server, status, appWidgetId);
            }
        }
    }



    private void updateWidget(Context context, Server server, SimpleConnectionStatus status, int appWidgetId) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        RemoteViews views = getViews(context);

        if (status == SimpleConnectionStatus.Connected) {
            setConnectedView(context, views);
        } else if (status == SimpleConnectionStatus.Connecting) {
            setConnectingView(context, views);
        } else {
            setDisconnectedView(context, views);
        }

        if (server != null) {
            setCountryCityServer(context, views, server);
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate called for widgets: " + Arrays.toString(appWidgetIds));

        // Fetch the current state from the repository
        VpnConnectionManager vpnConnectionManager = VpnConnectionManager.getInstance(context.getApplicationContext());
        VpnRepository vpnRepository = VpnRepository.getInstance(context.getApplicationContext());
        SimpleConnectionStatus currentStatus = vpnConnectionManager.getConnectionStatus().getValue();
        Server currentServer = vpnRepository.getSelectedServer().getValue();

        for (int appWidgetId : appWidgetIds) {
            Log.d(TAG, "Updating widget with ID: " + appWidgetId);

            RemoteViews views = getViews(context);

            // Apply the current status to the widget
            if (currentStatus == SimpleConnectionStatus.Connected) {
                setConnectedView(context, views);
            } else if (currentStatus == SimpleConnectionStatus.Connecting) {
                setConnectingView(context, views);
            } else {
                setDisconnectedView(context, views);
            }

            // Apply the current server information to the widget
            if (currentServer != null) {
                setCountryCityServer(context, views, currentServer);
            }

            // Set click action for the button
            views.setOnClickPendingIntent(R.id.button, getPendingSelfIntent(context, MyOnClick));

            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }


    protected void setCountryCityServer(Context context, RemoteViews views, Server selectedServer) {
        Log.d(TAG, "setCountryCityServer");
        if (currentLanguage == null) {
            currentLanguage = Locale.getDefault().getDisplayLanguage();
        }
        String locale = Locale.getDefault().getDisplayLanguage();

        Log.d(TAG, "selectedServer = " + selectedServer);
        Log.d(TAG, "locale = " + locale + ", currentLanguage = " + currentLanguage);

        Log.d(TAG, "currentLanguage.equalsIgnoreCase(locale) = " + currentLanguage.equalsIgnoreCase(locale));
        currentLanguage = locale;
        if (selectedServer != null && views != null) {
            int resId = CountryUtils.getCountryFlagImageResId(selectedServer.getCountryEnum());
            views.setImageViewResource(R.id.widget_flag, resId);
            views.setTextViewText(R.id.widget_country, selectedServer.getCountryPrint());
            views.setTextViewText(R.id.widget_city, selectedServer.getCity());
        }
    }

    protected RemoteViews getViews(Context context) {
        return new RemoteViews(context.getPackageName(), R.layout.widget_layout_large);
    }

    private void setConnectedView(Context context, RemoteViews views) {

            views.setInt(R.id.widget_line_status, "setBackgroundColor", context.getResources().getColor(R.color.green_connect));
            views.setInt(R.id.button, "setText", R.string.disconnect);
            views.setInt(R.id.button, "setBackgroundResource", R.drawable.connect_btn_unpressed);
            views.setImageViewResource(R.id.widget_map_bg, R.drawable.widget_bg_connected);
            views.setViewVisibility(R.id.progress, View.GONE);
            views.setInt(R.id.widget_connection_status, "setText", R.string.connected);

    }

    public void setDisconnectedView(Context context, RemoteViews views) {
        Log.d(TAG, "setDisconnectedView");
        views.setInt(R.id.widget_line_status, "setBackgroundColor", context.getResources().getColor(R.color.red_disconnect));
        views.setInt(R.id.button, "setText", R.string.connect);
        views.setInt(R.id.button, "setBackgroundResource", R.drawable.connect_btn_pressed);
        views.setImageViewResource(R.id.widget_map_bg, R.drawable.widget_bg_disconnected);
        views.setViewVisibility(R.id.progress, View.GONE);
        views.setInt(R.id.widget_connection_status, "setText", R.string.disconnected);
    }

    public void setConnectingView(Context context, RemoteViews views) {
        Log.d(TAG, "setConnectingView");
        views.setInt(R.id.widget_line_status, "setBackgroundColor", context.getResources().getColor(R.color.red_disconnect));
        views.setInt(R.id.button, "setBackgroundResource", R.drawable.connect_btn_unpressed);
        views.setImageViewResource(R.id.widget_map_bg, R.drawable.widget_bg_disconnected);
        views.setInt(R.id.widget_connection_status, "setText", R.string.connecting);
        views.setInt(R.id.button, "setText", R.string.connecting);
        views.setViewVisibility(R.id.progress, View.VISIBLE);
    }


    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    protected boolean isInternetAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    protected ComponentName getWidgetComponentName(Context context) {
        return new ComponentName(context, ShellfireWidget.class);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        Log.d(TAG, "onDeleted " + Arrays.toString(appWidgetIds));
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(TAG, "onDisabled");
    }
}
