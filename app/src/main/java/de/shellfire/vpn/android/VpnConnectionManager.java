package de.shellfire.vpn.android;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;

import de.shellfire.vpn.android.openvpn.ConnectionStatus;
import de.shellfire.vpn.android.openvpn.IOpenVPNServiceInternal;
import de.shellfire.vpn.android.openvpn.OpenVPNService;
import de.shellfire.vpn.android.openvpn.ProfileManager;
import de.shellfire.vpn.android.openvpn.VPNLaunchHelper;
import de.shellfire.vpn.android.openvpn.VpnProfile;
import de.shellfire.vpn.android.openvpn.VpnStatus;
import de.shellfire.vpn.android.webservice.model.WsFile;
import de.shellfire.vpn.android.widget.WidgetUpdateWorker;

public class VpnConnectionManager implements VpnStatus.StateListener {
    private static final String TAG = "VpnConnectionManager";


    private static VpnConnectionManager instance;
    private final VpnRepository vpnRepository;
    private final Context context;
    private IOpenVPNServiceInternal vpnService;
    private boolean bound = false;
    private final MutableLiveData<SimpleConnectionStatus> connectionStatusLiveData;
    private final MediatorLiveData<VpnProfile> selectedProfileLiveData;
    private boolean mCmfixed;

    private boolean connectionAttemptOngoing;
    private int connectionAttempts = 0;
    private static final int MAX_CONNECTION_ATTEMPTS = 30;
    private static final int MAX_CONNECTION_ATTEMPTS_BEFORE_PROTOCOL_SWITCH_TO_UDP = 15;
    private boolean autoSwitchProtocolPerformed = false;

    // Initialize observers
    // Observers
    private Observer<String> openVpnParamsObserver;
    private Observer<List<WsFile>> certificatesObserver;
    private final ProfileManager profileManager;
    private VpnProfile profile;

    private VpnConnectionManager(Context context) {
        Log.d(TAG, "VpnConnectionManager() - constructor called");
        this.context = context;
        bindVpnService();
        vpnRepository = VpnRepository.getInstance(context);
        connectionStatusLiveData = new MutableLiveData<>(SimpleConnectionStatus.Disconnected);
        selectedProfileLiveData = new MediatorLiveData<>();
        profileManager = ProfileManager.getInstance(context);


        VpnStatus.addStateListener(this);

        vpnRepository.getOpenVpnParams().observeForever(params -> {
            Log.d(TAG, "vpnRepository.getOpenVpnParams().observeForever() - start, params: " + params);
            VpnConnectionManager.this.createVpnProfile(params, vpnRepository.getCertificates().getValue());
        });

        vpnRepository.getCertificates().observeForever(certs -> {
            Log.d(TAG, "vpnRepository.getCertificates().observeForever() - start, certs: " + certs);
            VpnConnectionManager.this.createVpnProfile(vpnRepository.getOpenVpnParams().getValue(), certs);
        });


    }

    public static synchronized VpnConnectionManager getInstance(Context context) {
        if (instance == null) {
            instance = new VpnConnectionManager(context);
        }
        return instance;
    }

    private void bindVpnService() {
        Log.d(TAG, "bindVpnService() - start");

        Intent intent = new Intent(context, OpenVPNService.class);
        intent.setAction(OpenVPNService.START_SERVICE);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }


    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service){
            Log.d(TAG, "serviceConnection.onServiceConnected() - start");
            vpnService = IOpenVPNServiceInternal.Stub.asInterface(service);
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "serviceConnection.onServiceDisconnected() - start");
            bound = false;
        }
    };

    public LiveData<SimpleConnectionStatus> getConnectionStatus() {
        return connectionStatusLiveData;
    }

    public void updateState(String state, String logmessage, int localizedResId, ConnectionStatus level, Intent intent) {
        SimpleConnectionStatus status = mapVpnStatusToSimpleConnectionStatus(level, state);
        connectionStatusLiveData.postValue(status);
        handleStateUpdate(status);
        updateWidgets(status);
    }

    private void updateWidgets(SimpleConnectionStatus status) {
        Log.d(TAG, "Triggering one-time WidgetUpdateWorker");

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(WidgetUpdateWorker.class).build();
        WorkManager.getInstance(context).enqueue(workRequest);
    }

    private void handleStateUpdate(SimpleConnectionStatus newStatus) {
        switch (newStatus) {
            case Connected:
                handleStateUpdateConnected();
                break;
            case Connecting:
                handleStateUpdateConnecting();
                break;
            case Disconnected:
                handleStateUpdateDisconnected();
                break;
        }
    }

    private void handleStateUpdateConnecting() {
        Log.d(TAG, "handleStateUpdateConnecting() - start");
        connectionAttempts++;
        connectionAttemptOngoing = true;
        Log.d(TAG, "Connection attempts: " + connectionAttempts);

        if (connectionAttempts >= MAX_CONNECTION_ATTEMPTS_BEFORE_PROTOCOL_SWITCH_TO_UDP && !autoSwitchProtocolPerformed) {
            autoSwitchProtocolPerformed = true;
            Log.d(TAG, "Auto switch protocol performed: " + autoSwitchProtocolPerformed);

            if (vpnRepository.getSelectedVpn() != null && vpnRepository.getSelectedVpn().getValue() != null && vpnRepository.getSelectedVpn().getValue().getProtocol() == Protocol.UDP) {
                Log.d(TAG, "Current protocol is UDP, switching to TCP");
                disconnect();
                vpnRepository.setProtocol(Protocol.TCP);
                connect();
            }
        }



        if (connectionAttempts >= MAX_CONNECTION_ATTEMPTS) {
            Log.d(TAG, "Max connection attempts reached, disconnecting");
            connectionAttempts = 0;
            disconnect();
        }

        Log.d(TAG, "handleStateUpdateConnecting() - end");
    }

    private void handleStateUpdateConnected() {
        Log.d(TAG, "handleStateUpdateConnected() - start");
        connectionAttempts = 0;
        connectionAttemptOngoing = false;
        Log.d(TAG, "Connection attempts reset, connection attempt ongoing: " + connectionAttemptOngoing);
        Log.d(TAG, "handleStateUpdateConnected() - end");
    }

    private void handleStateUpdateDisconnected() {
        Log.d(TAG, "handleStateUpdateDisconnected() - start");

        if (connectionAttemptOngoing && connectionAttempts < MAX_CONNECTION_ATTEMPTS) {
            Log.d(TAG, "Reattempting connection, attempt number: " + (connectionAttempts + 1));
            connectionAttempts++;
            // connect();
        } else {
            Log.d(TAG, "Max connection attempts reached or no ongoing attempt, resetting state");
            connectionAttempts = 0;
            connectionAttemptOngoing = false;
            autoSwitchProtocolPerformed = false;
        }

        Log.d(TAG, "handleStateUpdateDisconnected() - end");
    }

    @Override
    public void setConnectedVPN(String uuid) {}

    private SimpleConnectionStatus mapVpnStatusToSimpleConnectionStatus(ConnectionStatus level, String state) {
        switch (level) {
            case LEVEL_CONNECTING_NO_SERVER_REPLY_YET:
            case LEVEL_CONNECTING_SERVER_REPLIED:
            case LEVEL_WAITING_FOR_USER_INPUT:
            case LEVEL_START:
                return SimpleConnectionStatus.Connecting;
            case LEVEL_CONNECTED:
                return SimpleConnectionStatus.Connected;
            case LEVEL_NOTCONNECTED:
            case LEVEL_NONETWORK:
            case LEVEL_VPNPAUSED:
            case LEVEL_AUTH_FAILED:
                return SimpleConnectionStatus.Disconnected;
            default:
                return SimpleConnectionStatus.Disconnected;
        }
    }

    public LiveData<VpnProfile> getSelectedProfile() {
        Log.d(TAG, "getSelectedProfile() - start");
        return selectedProfileLiveData;
    }

    private void createVpnProfile(String openVpnParams, List<WsFile> certs) {
        Log.d(TAG, "createVpnProfile - openVpnParams: " + openVpnParams + ", certs: " + certs);
        if (openVpnParams == null || certs == null) {
            Log.d(TAG, "createVpnProfile - openVpnParams or certs is null, setting profile to null and returning");
            selectedProfileLiveData.setValue(null);
            return;
        }
        if (selectedProfileLiveData.getValue() != null) {
            Log.d(TAG, "createVpnProfile - selectedProfileLiveData.getValue() != null, returning (profile already exists)");
            return;
        }

        if (openVpnParams != null && certs != null) {
            Log.d(TAG, "createVpnProfile both parameter != null, creating VpnProfile");
            profile = new VpnProfile(openVpnParams, certs);

            Log.d(TAG, "Deleting all profiles");
            profileManager.deleteAllProfiles(context);

            Log.d(TAG, "Adding new profile");
            profileManager.addProfile(profile);

            Log.d(TAG, "Saving profile");
            profileManager.saveProfile(context, profile);

            Log.d(TAG, "Saving profile list");
            profileManager.saveProfileList(context);
            selectedProfileLiveData.setValue(profile);
        }
    }

    public void disconnect() {
        Log.d(TAG, "disconnect() - start");

        // Implement disconnect logic
        VpnProfile selectedProfile = selectedProfileLiveData.getValue();
        if (selectedProfile != null) {
            Log.d(TAG, "disconnect.selectedProfileLiveData.getValue() - Selected profile found: " + selectedProfile.getName());
            ProfileManager profileManager = ProfileManager.getInstance(context);

            Log.d(TAG, "Removing selected profile");
            profileManager.removeProfile(context, selectedProfile);

            Log.d(TAG, "Updating connection status to Disconnected");
            connectionStatusLiveData.postValue(SimpleConnectionStatus.Disconnected);

            try {
                vpnService.stopVPN(false);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException while stopping VPN", e);
            }
        } else {
            Log.d(TAG, "disconnect.selectedProfileLiveData.getValue() - No selected profile found, must be Disconnected.");
            connectionStatusLiveData.postValue(SimpleConnectionStatus.Disconnected);
        }

        Log.d(TAG, "disconnect() - end");
    }

    public void connect() {
        Log.d(TAG, "connect() - start");

        VpnStatus.updateStateString("PREPARING_PROFILE", "", R.string.empty, ConnectionStatus.LEVEL_START);

        if (vpnRepository.getOpenVpnParams().getValue() == null) {
            Log.d(TAG, "Updating OpenVPN parameters because getValues() returned null - triggering update and observing livedata");
            vpnRepository.updateOpenVpnParams();

            Observer<String> connectOpenVpnParamsObserver = new Observer<String>() {
                @Override
                public void onChanged(String params) {
                    if (params == null) {
                        Log.d(TAG, "vpnRepository.connect().getOpenVpnParams() - params is null, ignoring");
                        return;
                    }
                    Log.d(TAG, "vpnRepository.connect().getOpenVpnParams() - connecting again");
                    vpnRepository.getOpenVpnParams().removeObserver(this);
                    try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                    VpnConnectionManager.this.connect();
                }
            };

            vpnRepository.getOpenVpnParams().observeForever(connectOpenVpnParamsObserver);

            return;
        }

        if (vpnRepository.getCertificates().getValue() == null) {
            Log.d(TAG, "Updating certificates because getValues() returned null - triggering update and observing livedata");
            vpnRepository.updateCertificates();

            Observer<List<WsFile>> connectGetCertificatesObserver = new Observer<List<WsFile>>() {
                @Override
                public void onChanged(List<WsFile> certs) {
                    if (certs == null) {
                        Log.d(TAG, "vpnRepository.connect().getCertificates() - certs is null, ignoring");
                        return;
                    }
                    Log.d(TAG, "vpnRepository.connect().getCertificates() - connecting again");
                    vpnRepository.getCertificates().removeObserver(this);
                    VpnConnectionManager.this.connect();
                }
            };

            vpnRepository.getCertificates().observeForever(connectGetCertificatesObserver);

            return;
        }



        if (this.profile == null) {
            Log.d(TAG, "Updating profile because getValues() returned null");
            createVpnProfile(vpnRepository.getOpenVpnParams().getValue(), vpnRepository.getCertificates().getValue());
        } else {
            Log.d(TAG, "Profile is not null, checking if profile is stored in ProfileManager");
            VpnProfile profileManagerProfile = profileManager.getProfileByName(profile.getName());
            if (profileManagerProfile == null) {
                Log.d(TAG, "Updating profile because profile was not stored in ProfileManager");
                createVpnProfile(vpnRepository.getOpenVpnParams().getValue(), vpnRepository.getCertificates().getValue());
            } else {
                Log.d(TAG, "ProfileManagerProfile is not null, should be good!");
            }
        }



        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean useCm9Fix = prefs.getBoolean("useCM9Fix", false);
        boolean loadTunModule = prefs.getBoolean("loadTunModule", false);

        Log.d(TAG, "useCm9Fix: " + useCm9Fix + ", loadTunModule: " + loadTunModule);

        if (loadTunModule) {
            Log.d(TAG, "Loading TUN module");
            executeSuCmd("insmod /system/lib/modules/tun.ko");
        }
        if (useCm9Fix && !mCmfixed) {
            Log.d(TAG, "Applying CM9 fix");
            executeSuCmd("chown system /dev/tun");
        }


        Intent intent = VpnService.prepare(context.getApplicationContext());
        if (intent != null) {
            Log.d(TAG, "VpnService.prepare() returned non-null intent, requesting user permission");
            Log.d(TAG, "Intent details: " + intent.getAction() + ", " + intent.getType() + ", " + intent.getDataString());
            VpnStatus.updateStateString("USER_VPN_PERMISSION", "", R.string.state_user_vpn_permission, ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT);

            // Start the TransparentActivity to handle the VPN preparation
            Intent startIntent = new Intent(context, TransparentActivity.class);
            startIntent.putExtra("vpn_intent", intent);
            startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(startIntent);
        } else {
            Log.d(TAG, "No user permission needed, starting OpenVPN");
            startOpenVpn();
        }

        Log.d(TAG, "connect() - end");
    }

    public void handleActivityResult(int resultCode) {
        Log.d(TAG, "handleActivityResult() - start, resultCode: " + resultCode);

        if (resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "Result is OK, calling connect()");
            connect();
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.d(TAG, "Result is CANCELED, updating VPN status");
            VpnStatus.updateStateString("USER_VPN_PERMISSION_CANCELLED", "", R.string.state_user_vpn_permission_cancelled, ConnectionStatus.LEVEL_NOTCONNECTED);
        }

        Log.d(TAG, "handleActivityResult() - end");
    }

    private void startOpenVpn() {
        Log.d(TAG, "startOpenVpn() - start");

        new Handler(Looper.getMainLooper()).post(() -> {
            Log.d(TAG, "startOpenVpn - Posting observeForever to main thread");

            Observer<VpnProfile> profileObserver = new Observer<VpnProfile>() {
                @Override
                public void onChanged(VpnProfile selectedProfile) {
                    Log.d(TAG, "startOpenVpn.onChanged() - start, selectedProfile: " + selectedProfile);
                    if (selectedProfile != null) {
                        Log.d(TAG, "startOpenVpn.profileObserver.onChanged - Selected profile found: " + selectedProfile.getName());
                        Executors.newSingleThreadExecutor().execute(() -> {
                            VPNLaunchHelper.startOpenVpn(selectedProfile, context);
                        });
                        selectedProfileLiveData.removeObserver(this);
                    } else {
                        Log.d(TAG, "startOpenVpn.profileObserver.onChanged - No selected profile found.");
                    }
                }
            };

            selectedProfileLiveData.observeForever(profileObserver);
        });

        Log.d(TAG, "startOpenVpn() - end");
    }

    private void executeSuCmd(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("su", "-c", command);
            Process p = pb.start();
            int ret = p.waitFor();
            if (ret == 0)
                mCmfixed = true;
        } catch (InterruptedException | IOException e) {
            VpnStatus.logException("SU command", e);
        }
    }

    public void toggleConnect() {
        Log.d(TAG, "toggleConnect() - start");
        SimpleConnectionStatus status = connectionStatusLiveData.getValue();
        Log.d(TAG, "Current connection status: " + status);
        if (SimpleConnectionStatus.Disconnected == status) {
            Log.d(TAG, "Current connection status is Disconnected, connecting");
            connect();
        } else {
            Log.d(TAG, "Current connection status is not Disconnected, disconnecting");
            disconnect();
        }
        Log.d(TAG, "toggleConnect() - end");
    }
}
