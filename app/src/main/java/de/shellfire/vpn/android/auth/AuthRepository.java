package de.shellfire.vpn.android.auth;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.Locale;
import java.util.concurrent.Executors;

import de.shellfire.vpn.android.AppDatabase;
import de.shellfire.vpn.android.ObfuscatedAccountId;
import de.shellfire.vpn.android.ObfuscatedAccountIdDao;
import de.shellfire.vpn.android.VpnRepository;
import de.shellfire.vpn.android.webservice.JsonWebService;
import de.shellfire.vpn.android.webservice.ShellfireWebService;
import de.shellfire.vpn.android.webservice.WebService;
import de.shellfire.vpn.android.webservice.model.BaseResponse;
import de.shellfire.vpn.android.webservice.model.GetActivationStatusResponse;
import de.shellfire.vpn.android.webservice.model.LoginRequest;
import de.shellfire.vpn.android.webservice.model.LoginResponse;
import de.shellfire.vpn.android.webservice.model.RegisterRequest;
import de.shellfire.vpn.android.webservice.model.RegisterWithGoogleRequest;
import de.shellfire.vpn.android.webservice.model.StringResponse;
import retrofit2.Call;

public class AuthRepository {
    private static AuthRepository instance;
    private final ShellfireWebService webService;
    private final JsonWebService jsonWebService;


    private final String lang = Locale.getDefault().getLanguage();
    private final Context context;
    private VpnRepository vpnRepository;
    private final String TAG = "AuthRepository";
    private final MutableLiveData<LoginStatus> loginStatusLiveData;
    private final MediatorLiveData<ActivationStatus> activationStatusLiveData;

    private final MediatorLiveData<String> obfuscatedAccountIdLiveData;
    private final ObfuscatedAccountIdDao obfuscatedAccountIdDao;

    private AuthRepository(Context context) {
        webService = ShellfireWebService.getInstance(context);
        jsonWebService = WebService.getInstance(context).getJsonWebService();
        this.context = context;
        obfuscatedAccountIdDao = AppDatabase.getDatabase(context).obfuscatedAccountIdDao();
        obfuscatedAccountIdLiveData = new MediatorLiveData<>();

        // Load data from local database on startup for developer payload
        LiveData<ObfuscatedAccountId> localObfuscatedAccountId = obfuscatedAccountIdDao.getObfuscatedAccountId();
        obfuscatedAccountIdLiveData.addSource(localObfuscatedAccountId, obfuscatedAccountIdEntity -> {
            if (obfuscatedAccountIdEntity != null) {
                obfuscatedAccountIdLiveData.setValue(obfuscatedAccountIdEntity.getObfuscatedAccountId());
            }
        });

        loginStatusLiveData = new MutableLiveData<>();
        activationStatusLiveData = new MediatorLiveData<>();

        updateActivationStatus();
        observeActivationStatusForLoginStatus();
    }

    private VpnRepository getVpnRepository() {
        if (vpnRepository == null)
            vpnRepository = VpnRepository.getInstance(context);

        return vpnRepository;
    }

    public static synchronized AuthRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AuthRepository(context);
        }
        return instance;
    }

    public LiveData<String> registerWithGoogleSignInByToken(String idToken, int subscribeToNewsletter) {
        Log.d(TAG, "registerWithGoogleSignInByToken: Starting registration with Google ID Token");

        MediatorLiveData<String> resultLiveData = new MediatorLiveData<>();
        getVpnRepository().observeLoginStatus();
        Executors.newSingleThreadExecutor().execute(() -> {
            // Make a request to the backend API

            RegisterWithGoogleRequest request = new RegisterWithGoogleRequest(idToken, subscribeToNewsletter);

            Call<BaseResponse<LoginResponse>> call = jsonWebService.registerWithGoogleToken(request);
            webService.makeAsyncCall(call, "Failed to register with Google Sign-In", new ShellfireWebService.BaseCallback<>() {
                @Override
                public void onSuccess(LoginResponse response) {
                    webService.setToken(response.getToken()); // Save token
                    Log.d(TAG, "registerWithGoogleSignInByToken: Registration successful");
                    resultLiveData.postValue(response.getToken()); // Update LiveData with the response
                    updateActivationStatus(); // Refresh activation status
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "registerWithGoogleSignInByToken: Registration failed", e);
                    resultLiveData.postValue(null); // Set null in case of failure
                }
            });
        });

        return resultLiveData;
    }


    public LiveData<String> register(String email, String password, int subscribeToNewsletter, int resend) {

        MediatorLiveData<String> resultLiveData = new MediatorLiveData<>();
        getVpnRepository().observeLoginStatus();
        Executors.newSingleThreadExecutor().execute(() -> {

            RegisterRequest request = new RegisterRequest(lang, email, password, subscribeToNewsletter, resend);

            // Make an asynchronous call to register
            Call<BaseResponse<LoginResponse>> call = jsonWebService.register(request);
            webService.makeAsyncCall(call, "Failed to register a new free account", new ShellfireWebService.BaseCallback<>() {
                @Override
                public void onSuccess(LoginResponse response) {
                    webService.setToken(response.getToken()); // Save token after successful registration
                    resultLiveData.postValue(response.getToken()); // Update LiveData with the response

                    updateActivationStatus();
                }

                @Override
                public void onFailure(Exception e) {
                    // Handle error
                    resultLiveData.postValue(null); // Set null in case of failure
                    updateActivationStatus();
                }
            });
        });

        return resultLiveData;
    }

    public void login(String token) {
        webService.setToken(token);
        updateActivationStatus();
        Log.d(TAG, "login() - updateVpnData()");
        getVpnRepository().updateVpnData();
    }

    public void login(String email, String password) {
        Log.d(TAG, "login: called, email=" + email);
        Executors.newSingleThreadExecutor().execute(() -> {
            LoginRequest request = new LoginRequest(email, password);

            // Make an asynchronous call to login
            Call<BaseResponse<LoginResponse>> call = jsonWebService.login(request);
            webService.makeAsyncCall(call, "Failed to login", new ShellfireWebService.BaseCallback<LoginResponse>() {
                @Override
                public void onSuccess(LoginResponse response) {
                    Log.d(TAG, "login.onSuccess: response=" + response);
                    webService.setToken(response.getToken()); // Save token after successful login
                    updateActivationStatus();
                    Log.d(TAG, "login(email, password).onSuccess - updateVpnData()");
                    getVpnRepository().updateVpnData();
                }

                @Override
                public void onFailure(Exception e) {
                    // Handle error
                    Log.e(TAG, "login.onFailure: e=" + e.getMessage(), e);
                    loginStatusLiveData.setValue(LoginStatus.LoginFailed); // Indicate failure
                }
            });
        });
    }

    public LiveData<LoginStatus> getLoginStatus() {
        return loginStatusLiveData;
    }

    public void resetLoginStatus() {
        Log.d(TAG, "resetLoginStatus: called");
        loginStatusLiveData.setValue(null);
        updateActivationStatus();
    }

    private void observeActivationStatusForLoginStatus() {
        Log.d(TAG, "observeActivationStatusForLoginStatus: called");
        Observer<ActivationStatus> activationStatusObserver = new Observer<ActivationStatus>() {

            @Override
            public void onChanged(ActivationStatus activationStatus) {
                Log.d(TAG, "observeActivationStatusForLoginStatus.onChanged: activationStatus = " + activationStatus);
                // Not only login token required, but also successful activation - so to know if we're really logged in, we need to make a call to
                // the api and check the activation status
                if (ActivationStatus.Active == activationStatus) {
                    Log.d(TAG, "observeActivationStatusForLoginStatus.onChanged: activationStatus is Active");
                    loginStatusLiveData.setValue(LoginStatus.LoggedIn);
                    updateObfuscatedAccountId();
                } else if (ActivationStatus.Inactive == activationStatus) {
                    Log.d(TAG, "observeActivationStatusForLoginStatus.onChanged: activationStatus is Inactive");
                    loginStatusLiveData.setValue(LoginStatus.LoggedInInactive);
                } else {
                    Log.d(TAG, "observeActivationStatusForLoginStatus.onChanged: activationStatus is neither Active nor Inactive, setting LoggedOut");
                    loginStatusLiveData.setValue(LoginStatus.LoggedOut);
                }
            }
        };
        activationStatusLiveData.observeForever(activationStatusObserver);
        Log.d(TAG, "observeActivationStatusForLoginStatus: observer registered");
    }

    public LiveData<ActivationStatus> getActivationStatus() {
        Log.d(TAG, "getActivationStatus: called");
        return activationStatusLiveData;
    }

    public void updateActivationStatus() {
        Log.d(TAG, "updateActivationStatus: called");
        if (webService.getToken() == null) {
            Log.d(TAG, "updateActivationStatus: token is null, setting Unknown and resetting RegisterResult");
            activationStatusLiveData.setValue(ActivationStatus.Unknown); // Unknown as not logged in

            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            Log.d(TAG, "updateActivationStatus: executing async task to check activation status");
            // Make an asynchronous call to check activation status

            Call<BaseResponse<GetActivationStatusResponse>> call = jsonWebService.getActivationStatus();
            webService.makeAsyncCall(call, "Failed to check activation status", new ShellfireWebService.BaseCallback<GetActivationStatusResponse>() {
                @Override
                public void onSuccess(GetActivationStatusResponse activationStatusResponse) {
                    Log.d(TAG, "updateActivationStatus.onSuccess: activationStatusResponse received");
                    if (activationStatusResponse.isActive()) {
                        Log.d(TAG, "updateActivationStatus.onSuccess: activationStatusResponse is Active");
                        activationStatusLiveData.setValue(ActivationStatus.Active);
                    } else {
                        Log.d(TAG, "updateActivationStatus.onSuccess: activationStatusResponse is Inactive");
                        activationStatusLiveData.setValue(ActivationStatus.Inactive);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Log.d(TAG, "updateActivationStatus.onFailure: error occurred while checking activation status", e);
                    // Would be called in case not yet logged in
                    activationStatusLiveData.setValue(ActivationStatus.Unknown); // Indicate failure
                }
            });
        });
    }


    private void clearObfuscatedAccountId() {
        Executors.newSingleThreadExecutor().execute(() -> {
            obfuscatedAccountIdDao.clearObfuscatedAccountId();
            obfuscatedAccountIdLiveData.postValue(null);
        });
    }

    public LiveData<Boolean> logout() {
        Log.d(TAG, "logout() - start()");
        MediatorLiveData<Boolean> resultLiveData = new MediatorLiveData<>();

        Executors.newSingleThreadExecutor().execute(() -> {
            // Make an asynchronous call to logout
            Call<BaseResponse<Void>> call = jsonWebService.logout();
            webService.makeAsyncCall(call, "Failed to logout", new ShellfireWebService.BaseCallback<Void>() {
                @Override
                public void onSuccess(Void response) {
                    Log.d(TAG, "logout.onSuccess: response=" + response);
                    webService.clearToken(); // Clear token after successful logout
                    updateActivationStatus();
                    clearObfuscatedAccountId();
                    getVpnRepository().invalidateVpnData(true);
                    resultLiveData.setValue(true); // Indicate success
                }

                @Override
                public void onFailure(Exception e) {
                    // Handle error
                    Log.e(TAG, "logout.onFailure: e=" + e.getMessage(), e);
                    webService.clearToken(); // Clear token after failed logout, need to assume something is really kaput.
                    updateActivationStatus();
                    clearObfuscatedAccountId();
                    getVpnRepository().invalidateVpnData();
                    resultLiveData.setValue(false); // Indicate failure
                }
            });
        });

        return resultLiveData;
    }


    public LiveData<String> getObfuscatedAccountId() {
        return obfuscatedAccountIdLiveData;
    }

    public void updateObfuscatedAccountId() {
        Log.d(TAG, "updateObfuscatedAccountId: called");
        Executors.newSingleThreadExecutor().execute(() -> {
            // Fetch data from the network if not already cached
            Call<BaseResponse<StringResponse>> call = jsonWebService.getObfuscatedAccountId();
            webService.makeAsyncCall(call, "Failed to get ObfuscatedAccountId", new ShellfireWebService.BaseCallback<>() {
                @Override
                public void onSuccess(StringResponse obfuscatedAccountId) {
                    Log.d(TAG, "updateObfuscatedAccountId.onSuccess: obfuscatedAccountId received: " + obfuscatedAccountId.getData());
                    obfuscatedAccountIdLiveData.setValue(obfuscatedAccountId.getData()); // Update the LiveData object with new data
                    // Update the local database
                    Executors.newSingleThreadExecutor().execute(() -> {
                        obfuscatedAccountIdDao.clearObfuscatedAccountId();
                        obfuscatedAccountIdDao.inserObfuscatedAccountId(new ObfuscatedAccountId(obfuscatedAccountId.getData()));
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    Log.d(TAG, "updateObfuscatedAccountId.onFailure: error occurred while fetching ObfuscatedAccountId", e);
                    // Handle error
                    Executors.newSingleThreadExecutor().execute(() -> {
                        obfuscatedAccountIdLiveData.postValue(null);
                    });
                }
            });
        });

    }
}