package de.shellfire.vpn.android;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import de.shellfire.vpn.android.auth.AuthRepository;
import de.shellfire.vpn.android.auth.LoginStatus;
import de.shellfire.vpn.android.webservice.JsonWebService;
import de.shellfire.vpn.android.webservice.ShellfireWebService;
import de.shellfire.vpn.android.webservice.WebService;
import de.shellfire.vpn.android.webservice.model.BaseResponse;
import de.shellfire.vpn.android.webservice.model.ProductIdRequest;
import de.shellfire.vpn.android.webservice.model.SetProtocolRequest;
import de.shellfire.vpn.android.webservice.model.SetServerToRequest;
import de.shellfire.vpn.android.webservice.model.WsFile;
import de.shellfire.vpn.android.widget.WidgetUpdateWorker;
import retrofit2.Call;

public class VpnRepository {

    private static final String TAG = "VpnRepo";

    private static VpnRepository instance;
    private final ShellfireWebService webService;
    private final JsonWebService jsonWebService;
    private final Context context;
    private final AuthRepository authRepository;
    private final MediatorLiveData<List<Vpn>> vpnListLiveData;
    private final MutableLiveData<Vpn> selectedVpnLiveData;
    private final MutableLiveData<Server> selectedServerLiveData;
    private final MutableLiveData<Protocol> selectedProtocolLiveData;

    private final MutableLiveData<String> openVpnParamsLiveData;
    private final MutableLiveData<List<WsFile>> certificatesLiveData;
    private final VpnDao vpnDao;
    private final DataRepository dataRepository;

    // Loading indicators
    private final MutableLiveData<Boolean> isVpnListLoading;
    private final MutableLiveData<Boolean> isSettingServer;
    private final MutableLiveData<Boolean> isSettingProtocol;
    private final MutableLiveData<Boolean> isCertificatesLoading;
    private final MutableLiveData<Boolean> isOpenVpnParamsLoading;
    private Boolean isPremium;
    private final MutableLiveData<Boolean> isPremiumLiveData;
    private final MutableLiveData<Boolean> setServerToLiveData;
    private final MutableLiveData<Boolean> setProtocolLiveData;
    private VpnConnectionManager vpnConnectionManager;
    private boolean initialized;

    private VpnRepository(Context context) {
        Log.d(TAG, "VpnRepository() - constructor called");

        webService = ShellfireWebService.getInstance(context);
        jsonWebService = WebService.getInstance(context).getJsonWebService();
        authRepository = AuthRepository.getInstance(context);
        vpnListLiveData = new MediatorLiveData<>();
        selectedVpnLiveData = new MutableLiveData<>();
        selectedServerLiveData = new MutableLiveData<>();
        selectedProtocolLiveData = new MutableLiveData<>();

        openVpnParamsLiveData = new MutableLiveData<>();
        certificatesLiveData = new MutableLiveData<>();
        isPremiumLiveData = new MutableLiveData<>();
        setServerToLiveData = new MutableLiveData<>();
        setProtocolLiveData = new MutableLiveData<>();

        this.context = context;
        dataRepository = DataRepository.getInstance(context);
        vpnDao = AppDatabase.getDatabase(context).vpnDao();

        // Initialize loading indicators
        isVpnListLoading = new MutableLiveData<>(false);
        isSettingServer = new MutableLiveData<>(false);
        isSettingProtocol = new MutableLiveData<>(false);
        isCertificatesLoading = new MutableLiveData<>(false);
        isOpenVpnParamsLoading = new MutableLiveData<>(false);

        observeLoginStatus();
    }

    // observes the login status and initializes vpn data once logged in
    public void observeLoginStatus() {
        this.initialized = false;
        Observer<LoginStatus> loginStatusObserver = new Observer<LoginStatus>() {

            @Override
            public void onChanged(LoginStatus loginstatus) {
                Log.d(TAG, "observeLoginStatus loginstatus changed: " + loginstatus);
                if (LoginStatus.LoggedIn == loginstatus) {
                    Log.d(TAG, "observeLoginStatus logged in - initializing VpnRepository and stopping login status observer");
                    initialize();
                    authRepository.getLoginStatus().removeObserver(this);
                }
            }
        };

        authRepository.getLoginStatus().observeForever(loginStatusObserver);
    }

    private void initialize() {
        Log.d(TAG, "initialize() - start");

        if (!this.initialized) {
            Log.d(TAG, "VpnRepository not initialized, initializing");
            // Load data from local database on startup for vpns
            LiveData<List<Vpn>> localVpnData = vpnDao.getAllVpns();

            vpnListLiveData.addSource(localVpnData, vpns -> {
                if (vpns != null && !vpns.isEmpty()) {
                    Log.d(TAG, "VPN list loaded from local database, size: " + vpns.size());
                    vpnListLiveData.setValue(vpns);
                } else {
                    Log.d(TAG, "VPN list is empty, refreshing from server");
                    updateVpnList();
                }
            });

            // Load the remembered VPN on startup
            int rememberedVpnId = VpnPreferences.getRememberedVpnSelection(context);
            if (rememberedVpnId > 0) {
                // Fetch and set the remembered VPN if available
                Log.d(TAG, "Remembered VPN ID: " + rememberedVpnId);
                setSelectedVpn(rememberedVpnId);
            } else {
                Log.d(TAG, "No remembered VPN ID found");
                // No VPN selected yet
                selectedVpnLiveData.setValue(null);
            }
        } else {
            Log.d(TAG, "VpnRepository already initialized");
        }

        this.initialized = true;
    }

    public static synchronized VpnRepository getInstance(Context context) {
        if (instance == null) {
            instance = new VpnRepository(context);
        }
        return instance;
    }

    public LiveData<List<Vpn>> getVpnList() {
        Log.d(TAG, "getVpnList() - start");

        return this.vpnListLiveData;
    }

    public void updateVpnList() {
        Log.d(TAG, "updateVpnList() - start");

        if (Boolean.TRUE.equals(isVpnListLoading.getValue())) {
            Log.d(TAG, "isVpnListLoading is true, returning");
            // A request is already in progress, no need to initiate another one
            return;
        }

        if (webService.getToken() == null) {
            Log.d(TAG, "WebService has no token, so we're obviously not logged in - returning");
            return;
        }

        isVpnListLoading.postValue(true); // Indicate that a request is in progress

        // Use an executor to ensure this runs on a background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            Call<BaseResponse<List<Vpn>>> call = jsonWebService.getAllVpnDetails();
            webService.makeAsyncCall(call, "Failed to refresh VPN list", new ShellfireWebService.BaseCallback<List<Vpn>>() {
                @Override
                public void onSuccess(List<Vpn> vpnList) {
                    Log.d(TAG, "VPN list refreshed successfully: " + vpnList);
                    if (vpnListLiveData.getValue() == null || !vpnListLiveData.getValue().equals(vpnList))  {
                        Log.d(TAG, "VPN list has changed, updating livedata and local database");
                        vpnListLiveData.setValue(vpnList);
                        Vpn selectedVpn = selectedVpnLiveData.getValue();
                        if (selectedVpn != null) {
                            for (Vpn vpn : vpnList) {
                                if (vpn.getVpnId() == selectedVpn.getVpnId()) {
                                    selectedVpnLiveData.setValue(vpn);
                                }
                            }
                        }


                        new Thread(() -> {
                            Log.d(TAG + ".updVpnList", "Starting thread to update local database with new vpn list");
                            vpnDao.clearAll();
                            vpnDao.insertVpns(vpnList);
                            Log.d(TAG + ".updVpnList", "Thread finished updating local database with new vpn list");
                        }).start();
                    } else {
                        Log.d(TAG, "VPN list has not changed, not updating livedata or local database");
                    }
                    isVpnListLoading.setValue(false); // Request completed
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Failed to refresh VPN list", e);
                    isVpnListLoading.setValue(false); // Request completed with failure
                }
            });
        });

        Log.d(TAG, "updateVpnList() - end");
        return;
    }

    public MutableLiveData<Boolean> getSetServerTo() {
        return this.setServerToLiveData;
    }
    public LiveData<Boolean> getIsSettingServer() {
        return this.isSettingServer;
    }

    public void setServerTo(int serverId) {
        Log.d(TAG, "setServerTo("+serverId+") - start");

        new Handler(Looper.getMainLooper()).post(() -> {
            if (Boolean.TRUE.equals(isSettingServer.getValue())) {
                // A request is already in progress, no need to initiate another one
                return;
            }
            isSettingServer.setValue(true); // Indicate that a request is in progress

            selectedVpnLiveData.observeForever(new Observer<Vpn>() {
                @Override
                public void onChanged(Vpn vpn) {
                    Log.d(TAG, "setServerTo.selectedVpnLiveData changed");
                    if (vpn != null) {
                        setServerTo(vpn, serverId);
                    } else {
                        setServerToLiveData.setValue(false);
                        isSettingServer.setValue(false); // Ensure the loading state is reset
                    }
                    selectedVpnLiveData.removeObserver(this);

                }
            });
        });

        Log.d(TAG, "setServerTo() - end");
    }

    private void setServerTo(Vpn vpn, int serverId) {
        Log.d(TAG, "setServerTo(Vpn, serverId) - start");

        new Handler(Looper.getMainLooper()).post(() -> {
            // ensure no broken vpn config data is left so that no broken vpn profiles can be generated by VpnConnectionManager
            invalidateVpnData();

            Executors.newSingleThreadExecutor().execute(() -> {
                SetServerToRequest request = new SetServerToRequest(vpn.getVpnId(), serverId);
                Call<BaseResponse<Void>> call = jsonWebService.setServerTo(request);
                webService.makeAsyncCall(call, "Failed to set server", new ShellfireWebService.BaseCallback<Void>() {
                    @Override
                    public void onSuccess(Void response) {
                        Log.d(TAG, "Server set successfully");

                        // Update LiveData on the main thread
                        new Handler(Looper.getMainLooper()).post(() -> {
                            LiveData<Server> serverLiveData = getServerById(serverId);
                            Observer<Server> observer = new Observer<Server>() {
                                @Override
                                public void onChanged(Server selectedServer) {
                                    Log.d(TAG, "setServerTo.selectedServerLiveData changed, server: " + selectedServer);
                                    isSettingServer.setValue(false); // Request completed
                                    selectedServerLiveData.setValue(selectedServer);
                                    Log.d(TAG, "setServerTo.selectedServerLiveData changed, updateVpnData()");
                                    updateVpnData();
                                    setServerToLiveData.setValue(true);
                                    serverLiveData.removeObserver(this);
                                }
                            };
                            serverLiveData.observeForever(observer);

                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failed to set server", e);
                        setServerToLiveData.postValue(false);
                        isSettingServer.postValue(false); // Request completed with failure
                    }
                });
            });
        });


        Log.d(TAG, "setServerTo(Vpn, serverId) - end");
    }

    public void invalidateVpnData() {
        invalidateVpnData(false);
    }

    VpnConnectionManager getVpnConnectionManager() {
        if (vpnConnectionManager == null) {
            vpnConnectionManager = VpnConnectionManager.getInstance(context);
        }

        return this.vpnConnectionManager;
    }

    public void invalidateVpnData(boolean includeSelectedVpn) {
        Log.d(TAG, "invalidateVpnData(includeSelectedVpn: " + includeSelectedVpn + ") - start");
        this.vpnListLiveData.setValue(null);

        Executors.newSingleThreadExecutor().execute(() -> vpnDao.clearAll());

        this.isPremiumLiveData.setValue(null);
        this.certificatesLiveData.setValue(null);
        this.openVpnParamsLiveData.setValue(null);
        this.selectedProtocolLiveData.setValue(null);
        getVpnConnectionManager();
        if (includeSelectedVpn) {
            setSelectedVpn(0);
        }

        Log.d(TAG, "invalidateVpnData() - end");
    }

    public void updateVpnData() {
        Log.d(TAG, "updateVpnData() - start");
        updateVpnList();
        updateCertificates();
        updateOpenVpnParams();
        updateIsPremium();
        updateWidgets();
        Log.d(TAG, "updateVpnData() - end");
    }

    private void updateWidgets() {
        Log.d(TAG, "Triggering one-time WidgetUpdateWorker");

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(WidgetUpdateWorker.class).build();
        WorkManager.getInstance(context).enqueue(workRequest);
    }


    public LiveData<Boolean> getSetProtocol() {
        return this.setProtocolLiveData;
    }

    public void setProtocol(Protocol protocol) {
        Log.d(TAG, "setProtocol() - start");

        new Handler(Looper.getMainLooper()).post(() -> {
            if (Boolean.TRUE.equals(isSettingProtocol.getValue())) {
                // A request is already in progress, no need to initiate another one
                return;
            }

            isSettingProtocol.postValue(true); // Indicate that a request is in progress
            selectedVpnLiveData.observeForever(new Observer<Vpn>() {
                @Override
                public void onChanged(Vpn vpn) {
                    Log.d(TAG, "selectedVpnLiveData changed");
                    if (vpn != null) {
                        setProtocol(vpn, protocol);
                    } else {
                        setProtocolLiveData.setValue(false);
                    }
                    selectedVpnLiveData.removeObserver(this);
                    isSettingProtocol.setValue(false);
                }
            });
        });

        Log.d(TAG, "setProtocol() - end");
    }

    private void setProtocol(Vpn vpn, Protocol protocol) {
        Log.d(TAG, "setProtocol() - start");

        new Handler(Looper.getMainLooper()).post(() -> {
            boolean doReconnect = false;
            SimpleConnectionStatus currentStatus = getVpnConnectionManager().getConnectionStatus().getValue();
            if (currentStatus == SimpleConnectionStatus.Connected || currentStatus == SimpleConnectionStatus.Connecting) {
                doReconnect = true;
                getVpnConnectionManager().disconnect();
            }

            // ensure no broken vpn config data is left so that no broken vpn profiles can be generated by VpnConnectionManager
            invalidateVpnData();

            boolean finalDoReconnect = doReconnect;
            Executors.newSingleThreadExecutor().execute(() -> {
                SetProtocolRequest request = new SetProtocolRequest(vpn.getVpnId(), protocol.name());
                Call<BaseResponse<Void>> call = jsonWebService.setProtocol(request);

                webService.makeAsyncCall(call, "Failed to set protocol", new ShellfireWebService.BaseCallback<Void>() {
                    @Override
                    public void onSuccess(Void response) {
                        Log.d(TAG, "Protocol set successfully");
                        selectedProtocolLiveData.setValue(protocol);
                        Log.d(TAG, "setProtocol.onSuccess changed, updateVpnData()");
                        updateVpnData();
                        setProtocolLiveData.setValue(true);
                        isSettingProtocol.setValue(false); // Request completed

                        if (finalDoReconnect) {
                            getVpnConnectionManager().connect();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failed to set protocol", e);
                        setProtocolLiveData.setValue(false);
                        isSettingProtocol.setValue(false); // Request completed with failure
                    }
                });
            });
        });
        Log.d(TAG, "setProtocol() - end");
    }

    public LiveData<Boolean> isSettingProtocol() {
        return isSettingProtocol;
    }

    public LiveData<List<WsFile>> getCertificates() {
        return certificatesLiveData;
    }

    public void updateCertificates() {
        Log.d(TAG, "updateCertificates() - start");

        new Handler(Looper.getMainLooper()).post(() -> {
            if (Boolean.TRUE.equals(isCertificatesLoading.getValue())) {
                Log.d(TAG, "isCertificatesLoading is true, returning (paralel request already in progress)");
                // A request is already in progress, no need to initiate another one
                return;
            }

            isCertificatesLoading.setValue(true); // Indicate that a request is in progress

            selectedVpnLiveData.observeForever(new Observer<Vpn>() {
                @Override
                public void onChanged(Vpn vpn) {
                    // TODO: Find out why this line is not reached!?
                    Log.d(TAG, "updateCertificates.selectedVpnLiveData changed, vpn: " + vpn);
                    if (vpn != null) {
                        updateCertificates(vpn);
                    } else {
                        certificatesLiveData.setValue(null);
                    }
                    isCertificatesLoading.setValue(false);
                    selectedVpnLiveData.removeObserver(this);
                }
            });
        });

        Log.d(TAG, "updateCertificates() - end");
    }

    private void updateCertificates(Vpn vpn) {
        Log.d(TAG, "updateCertificates(Vpn=" + vpn + ") - start");

        // Ensure this runs on a background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            ProductIdRequest request = new ProductIdRequest(vpn.getVpnId());
            Call<BaseResponse<Map<String, String>>> call = jsonWebService.getCertificates(request);
            webService.makeAsyncCall(call, "Failed to get certificates", new ShellfireWebService.BaseCallback<Map<String, String>>() {
                @Override
                public void onSuccess(Map<String, String> response) {
                    Log.d(TAG, "Certificates retrieved successfully");
                    List<WsFile> wsFiles = new ArrayList<>();
                    for (Map.Entry<String, String> entry : response.entrySet()) {
                        wsFiles.add(new WsFile(entry.getKey(), entry.getValue()));
                    }

                    certificatesLiveData.setValue(wsFiles);
                    isCertificatesLoading.setValue(false);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Failed to get certificates", e);
                    certificatesLiveData.setValue(null);
                    isCertificatesLoading.setValue(false); // Request completed with failure
                }
            });
        });

        Log.d(TAG, "updateCertificates(Vpn) - end");
    }

    public LiveData<Boolean> isCertificatesLoading() {
        return isCertificatesLoading;
    }

    public MutableLiveData<String> getOpenVpnParams() {
        Log.d(TAG, "getOpenVpnParams() - start");

        return openVpnParamsLiveData;
    }

    public LiveData<Boolean> isOpenVpnParamsLoading() {
        return isOpenVpnParamsLoading;
    }

    void updateOpenVpnParams() {
        Log.d(TAG, "updateOpenVpnParams() - start");

        new Handler(Looper.getMainLooper()).post(() -> {
            if (Boolean.TRUE.equals(isOpenVpnParamsLoading.getValue())) {
                Log.d(TAG, "isOpenVpnParamsLoading is true, returning (paralel request already in progress)");
                // A request is already in progress, no need to initiate another one
                return;
            }

            isOpenVpnParamsLoading.setValue(true); // Indicate that a request is in progress

            selectedVpnLiveData.observeForever(new Observer<Vpn>() {
                @Override
                public void onChanged(Vpn vpn) {
                    Log.d(TAG, "selectedVpnLiveData changed");
                    selectedVpnLiveData.removeObserver(this);
                    if (vpn != null) {
                        updateOpenVpnParams(vpn);
                    } else {
                        openVpnParamsLiveData.setValue(null);
                        isOpenVpnParamsLoading.setValue(false);
                    }


                }
            });
        });

        Log.d(TAG, "updateOpenVpnParams() - end");
    }

    private void updateOpenVpnParams(Vpn vpn) {
        Log.d(TAG, "updateOpenVpnParams(Vpn) - start");

        // Use an executor to ensure this runs on a background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            ProductIdRequest request = new ProductIdRequest(vpn.getVpnId());
            Call<BaseResponse<Map<String, String>>> call = jsonWebService.getOpenVpnParams(request);
            webService.makeAsyncCall(call, "Failed to get OpenVPN parameters", new ShellfireWebService.BaseCallback<Map<String, String>>() {
                @Override
                public void onSuccess(Map<String, String> response) {
                    Log.d(TAG, "OpenVPN parameters retrieved successfully");
                    String params = response.get("params");
                    openVpnParamsLiveData.setValue(params);

                    isOpenVpnParamsLoading.setValue(false); // Request completed
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Failed to get OpenVPN parameters", e);
                    openVpnParamsLiveData.setValue(null);
                    isOpenVpnParamsLoading.setValue(false); // Request completed with failure
                }
            });
        });

        Log.d(TAG, "updateOpenVpnParams(Vpn) - end");
    }

    public LiveData<Boolean> setSelectedVpn(int vpnId) {
        Log.d(TAG, "setSelectedVpn(vpnId="+vpnId+") - start");
        MediatorLiveData<Boolean> resultLiveData = new MediatorLiveData<>();

        selectedVpnLiveData.setValue(null);

        if (vpnId == 0) {
            Log.d(TAG, "setSelectedVpn(vpnList) called with vpnId=0, setting selectedVpnLiveData.setValue(null)");
            VpnPreferences.setRememberedVpnSelection(context, 0);
            return null;
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            vpnListLiveData.observeForever(new Observer<List<Vpn>>() {
                @Override
                public void onChanged(List<Vpn> vpnList) {
                    Log.d(TAG, "setSelectedVpn.vpnListLiveData changed, vpnList: " + vpnList);
                    vpnListLiveData.removeObserver(this);
                    if (vpnList != null) {
                        setSelectedVpn(vpnList, vpnId, resultLiveData);
                    } else {
                        selectedVpnLiveData.setValue(null);
                        resultLiveData.setValue(false);
                    }
                }
            });
        });

        Log.d(TAG, "setSelectedVpn() - end");
        return resultLiveData;
    }

    private void setSelectedVpn(List<Vpn> vpnList, int vpnId, MediatorLiveData<Boolean> resultLiveData) {
        Log.d(TAG, "setSelectedVpn(vpnList, vpnId) - start");

        SimpleConnectionStatus currentStatus = getVpnConnectionManager().getConnectionStatus().getValue();
        if (currentStatus == SimpleConnectionStatus.Connected || currentStatus == SimpleConnectionStatus.Connecting) {
            getVpnConnectionManager().disconnect();
        }

        if (vpnId == 0) {
            Log.d(TAG, "setSelectedVpn(vpnList, vpnId) called with vpnId=0, setting selectedVpnLiveData.setValue(null)");
            selectedVpnLiveData.setValue(null);
            VpnPreferences.setRememberedVpnSelection(context, 0);
            return;
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            invalidateVpnData();

            for (Vpn vpn : vpnList) {
                if (vpn.getVpnId() == vpnId) {
                    selectedVpnLiveData.setValue(vpn);
                    VpnPreferences.setRememberedVpnSelection(context, vpnId);
                    resultLiveData.setValue(false);
                    LiveData<Server> serverLiveData = getServerById(vpn.getServerId());
                    serverLiveData.observeForever(new Observer<Server>() {
                        @Override
                        public void onChanged(Server server) {
                            Log.d(TAG, "setSelectedVpn - selectedServerLiveData changed, updating selected server to the server of the vpn: server: " + server);
                            serverLiveData.removeObserver(this);
                            selectedServerLiveData.setValue(server);
                            Log.d(TAG, "setSelectedVpn.onChanged changed, updateVpnData()");
                            updateVpnData();
                        }
                    });
                    return;
                }
            }
            Log.d(TAG, "setSelectedVpn(vpnList, vpnId) - no vpn found with id: " + vpnId + " in list " + vpnList);
            Log.d(TAG, "setSelectedVpn(vpnList, vpnId) - setting selectedVpnLiveData.setValue(null) + calling updateVpnData()");
            selectedVpnLiveData.setValue(null);
            updateVpnData();
        });


        Log.d(TAG, "setSelectedVpn(vpnList, vpnId) - end");
    }

    public LiveData<Vpn> getSelectedVpn() {
        return selectedVpnLiveData;
    }

    public LiveData<Server> getSelectedServer() {
        return selectedServerLiveData;
    }

    private LiveData<Server> getServerById(int serverId) {
        Log.d(TAG, "getServerById() - start");

        MutableLiveData<Server> resultLiveData = new MutableLiveData<>();
        new Handler(Looper.getMainLooper()).post(() -> {
            dataRepository.getServerList().observeForever(new Observer<List<Server>>() {
                @Override
                public void onChanged(List<Server> serverList) {
                    Log.d(TAG, "getServerById().serverListLiveData changed");
                    dataRepository.getServerList().removeObserver(this);
                    if (serverList != null) {
                        for (Server server : serverList) {
                            if (server.getVpnServerId() == serverId) {
                                resultLiveData.setValue(server);
                                break;
                            }
                        }
                    } else {
                        resultLiveData.setValue(null);
                    }
                }
            });
        });

        Log.d(TAG, "getServerById() - end");
        return resultLiveData;
    }

    public LiveData<Boolean> isPremium() {
        return isPremiumLiveData;
    }

    public void updateIsPremium() {
        Log.d(TAG, "updateIsPremium() - start");

        isPremiumLiveData.setValue(false);

        new Handler(Looper.getMainLooper()).post(() -> {
            vpnListLiveData.observeForever(new Observer<List<Vpn>>() {
                @Override
                public void onChanged(List<Vpn> vpnList) {
                    Log.d(TAG, "updateIsPremium.vpnListLiveData changed");
                    vpnListLiveData.removeObserver(this);
                    if (vpnList != null && !vpnList.isEmpty()) {
                        for (Vpn vpn : vpnList) {
                            if (vpn.getAccountType() != ServerType.Free) {
                                isPremiumLiveData.setValue(true);
                                return;
                            }
                        }
                    }
                    isPremiumLiveData.setValue(false);
                }
            });
        });

        Log.d(TAG, "updateIsPremium() - end");
    }
}
