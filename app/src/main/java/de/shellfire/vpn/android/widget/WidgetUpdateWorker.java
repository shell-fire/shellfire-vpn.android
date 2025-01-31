package de.shellfire.vpn.android.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import de.shellfire.vpn.android.Server;
import de.shellfire.vpn.android.SimpleConnectionStatus;
import de.shellfire.vpn.android.VpnConnectionManager;
import de.shellfire.vpn.android.VpnRepository;

public class WidgetUpdateWorker extends Worker {

    public WidgetUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        // LiveData-Instanzen abrufen
        SimpleConnectionStatus status = VpnConnectionManager.getInstance(context).getConnectionStatus().getValue();
        Server server = VpnRepository.getInstance(context).getSelectedServer().getValue();

        // Broadcast an Widgets senden
        Class<?>[] widgetClasses = {ShellfireWidget.class, ShellfireWidgetSmall.class, ShellfireWidgetLarge.class};
        for (Class<?> widgetClass : widgetClasses) {
            ComponentName componentName = new ComponentName(context, widgetClass);
            int[] widgetIds = appWidgetManager.getAppWidgetIds(componentName);

            for (int widgetId : widgetIds) {
                Intent intent = new Intent(context, widgetClass);
                intent.setAction(ShellfireWidget.ACTION_UPDATE_WIDGET);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);

                // Status und Server hinzufügen
                if (server != null) {
                    intent.putExtra(ShellfireWidget.EXTRA_SERVER_INFO, server);
                }

                if (status != null) {
                    intent.putExtra(ShellfireWidget.EXTRA_CONNECTION_STATUS, status);
                }

                context.sendBroadcast(intent);
            }
        }

        return Result.success();
    }
}
