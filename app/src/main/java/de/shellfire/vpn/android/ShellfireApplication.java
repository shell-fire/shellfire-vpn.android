package de.shellfire.vpn.android;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapsSdkInitializedCallback;

import de.shellfire.vpn.android.auth.AuthRepository;
import de.shellfire.vpn.android.openvpn.OpenVPNService;
import de.shellfire.vpn.android.openvpn.StatusListener;
import de.shellfire.vpn.android.widget.BootReceiver;

/**
 * Created by Alina on 19.02.2018.
 */

public class ShellfireApplication extends Application implements OnMapsSdkInitializedCallback {
    private static final String TAG = "ShellfireApplication";
    private static Context mContext;
    private static boolean activityVisible;
    private static boolean isTestMode = false;
    private StatusListener mStatus;
    private BootReceiver bootReceiver;

    public static boolean getIsTestMode() {
        return isTestMode;
    }

    public static void setIsTestMode(boolean b) {
        ShellfireApplication.isTestMode = b;
    }

    public static Context getContext() {
        return mContext;
    }

    public static void setContext(Context context) {
        mContext = context;
    }

    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static void activityResumed() {
        activityVisible = true;
    }

    public static void activityDestroyed() {
        activityVisible = false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        int processId = Process.myPid();
        String processName = getProcessName(processId);

        // Log the process name and ID
        Log.d(TAG, "onCreate called, process ID: " + processId + ", process name: " + processName);


        mContext = this;
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannels();

        mStatus = new StatusListener();
        mStatus.init(this);

        if (!getPackageName().equals(processName)) {
            Log.d(TAG, getPackageName() + " is not the same as " + processName + ", exiting");
            // This is a secondary process (:openvpn), skip initialization
            return;
        }

        registerBootReceiver();

        MapsInitializer.initialize(this, MapsInitializer.Renderer.LATEST, this);
        VpnConnectionManager.getInstance(this);
        DataRepository.getInstance(this);
        VpnRepository.getInstance(this);
        LogRepository.getInstance(this);
        BillingRepository.getInstance(this);
        AuthRepository.getInstance(this);

    }

    private void registerBootReceiver() {
        bootReceiver = new BootReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        registerReceiver(bootReceiver, filter);
    }

    private String getProcessName(int pid) {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo processInfo : activityManager.getRunningAppProcesses()) {
            if (processInfo.pid == pid) {
                return processInfo.processName;
            }
        }
        return null;
    }

    @Override
    public void onMapsSdkInitialized(MapsInitializer.Renderer renderer) {
        switch (renderer) {
            case LATEST:
                Log.d("onMapsSdkInitialized", "The latest version of the renderer is used.");
                break;
            case LEGACY:
                Log.d("onMapsSdkInitialized", "The legacy version of the renderer is used.");
                break;
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannels() {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Background message
        CharSequence name = getString(R.string.channel_name_background);
        NotificationChannel mChannel = new NotificationChannel(OpenVPNService.NOTIFICATION_CHANNEL_BG_ID,
                name, NotificationManager.IMPORTANCE_MIN);

        mChannel.setDescription(getString(R.string.channel_description_background));
        mChannel.enableLights(false);

        mChannel.setLightColor(Color.DKGRAY);
        mNotificationManager.createNotificationChannel(mChannel);

        // Connection status change messages

        name = getString(R.string.channel_name_status);
        mChannel = new NotificationChannel(OpenVPNService.NOTIFICATION_CHANNEL_NEWSTATUS_ID,
                name, NotificationManager.IMPORTANCE_LOW);

        mChannel.setDescription(getString(R.string.channel_description_status));
        mChannel.enableLights(true);

        mChannel.setLightColor(Color.BLUE);
        mNotificationManager.createNotificationChannel(mChannel);


        // Urgent requests, e.g. two factor auth
        name = getString(R.string.channel_name_userreq);
        mChannel = new NotificationChannel(OpenVPNService.NOTIFICATION_CHANNEL_USERREQ_ID,
                name, NotificationManager.IMPORTANCE_HIGH);
        mChannel.setDescription(getString(R.string.channel_description_userreq));
        mChannel.enableVibration(true);
        mChannel.setLightColor(Color.CYAN);
        mNotificationManager.createNotificationChannel(mChannel);
    }
}
