package de.shellfire.vpn.android.viewmodel;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import java.util.List;

import de.shellfire.vpn.android.BillingRepository;
import de.shellfire.vpn.android.DataRepository;
import de.shellfire.vpn.android.LogRepository;
import de.shellfire.vpn.android.R;
import de.shellfire.vpn.android.Vpn;
import de.shellfire.vpn.android.VpnPreferences;
import de.shellfire.vpn.android.VpnRepository;
import de.shellfire.vpn.android.auth.ActivationStatus;
import de.shellfire.vpn.android.auth.AuthRepository;
import de.shellfire.vpn.android.auth.LoginStatus;

public class SharedViewModel extends ViewModel {
    private static final String TAG = "SharedViewModel";
    private final DataRepository dataRepository;
    private final VpnRepository vpnRepository;
    private final LogRepository logRepository;
    private final BillingRepository billingRepository;
    private MutableLiveData<String> registerResult = new MutableLiveData<>();
    private boolean registrationOngoing;

    private final AuthRepository authRepository;
    private final MutableLiveData<Integer> targetFragment = new MutableLiveData<>();

    private final Observer<List<Vpn>> vpnListObserver = new Observer<List<Vpn>>() {
        @Override
        public void onChanged(List<Vpn> vpns) {
            Log.d(TAG, "VPN list updated, size: " + (vpns == null ? "null" : "non-null") + " / " + (vpns != null ? vpns.size() : 0));
            if (vpns != null && !vpns.isEmpty()) {
                vpnRepository.getVpnList().removeObserver(this);
                updateTargetFragment();
            }
        }
    };
    private final Observer<LoginStatus> loginStatusObserver = new Observer<LoginStatus>() {
        @Override
        public void onChanged(LoginStatus loginStatus) {
            Log.d(TAG, "Login status updated: " + loginStatus);
            if (loginStatus != null) {
                authRepository.getLoginStatus().removeObserver(this);
                updateTargetFragment();
            }
        }
    };
    private final Context context;

    public SharedViewModel(Context context) {
        this.dataRepository = DataRepository.getInstance(context.getApplicationContext());
        this.vpnRepository = VpnRepository.getInstance(context.getApplicationContext());
        this.logRepository = LogRepository.getInstance(context.getApplicationContext());
        this.billingRepository = BillingRepository.getInstance(context.getApplicationContext());
        this.authRepository = AuthRepository.getInstance(context.getApplicationContext());
        this.context = context;
    }

    public LiveData<Integer> getTargetFragment() {
        return targetFragment;
    }

    public void updateTargetFragment() {
        Log.d(TAG, "updateTargetFragment");
        targetFragment.setValue(getTargetFragmentForCurrentState());
    }

    private void startObservingVpnList() {
        vpnRepository.getVpnList().observeForever(vpnListObserver);
    }

    private void startObservingLoginStatus() {
        authRepository.getLoginStatus().observeForever(loginStatusObserver);
    }

    private int getTargetFragmentForCurrentState() {
        Log.d(TAG, "getTargetFragmentForCurrentState");
        int nextFragmentAction = 0;

        LoginStatus loginStatus = authRepository.getLoginStatus().getValue();
        if (loginStatus == null) {
            Log.d(TAG, "loginStatus is null, waiting...");
            startObservingLoginStatus();  // Start observing the login status
            return nextFragmentAction;  // Return 0 to indicate waiting
        }

        boolean vpnIsSelected = vpnRepository.getSelectedVpn().getValue() != null;
        Log.d(TAG, "loginStatus: " + loginStatus + ", vpnIsSelected: " + vpnIsSelected);
        if (loginStatus == LoginStatus.LoggedIn) {
            if (vpnIsSelected) {
                Log.d(TAG, "returning R.id.mainSectionFragment");
                nextFragmentAction = R.id.mainSectionFragment;
            } else {
                List<Vpn> vpnList = vpnRepository.getVpnList().getValue();
                Log.d(TAG, "vpnList is null: " + (vpnList == null) + ", vpnList size: " + (vpnList != null ? vpnList.size() : 0));

                if (vpnList == null || vpnList.isEmpty()) {
                    Log.d(TAG, "vpnList is not loaded yet, waiting...");
                    startObservingVpnList();  // Start observing the VPN list
                    return nextFragmentAction;  // Return 0 to indicate waiting
                }

                if (vpnList.size() == 1) {
                    Log.d(TAG, "selecting the one VPN in the list and returning R.id.mainSectionFragment");
                    vpnRepository.setSelectedVpn(vpnList.get(0).getVpnId());
                    nextFragmentAction = R.id.mainSectionFragment;
                } else {
                    Log.d(TAG, "vpnList has more than 1 element, returning R.id.selectVpnFragment");
                    nextFragmentAction = R.id.selectVpnFragment;
                }
            }
        } else if (loginStatus == LoginStatus.LoggedOut) {
            Log.d(TAG, "returning R.id.loginFragment");
            nextFragmentAction = R.id.loginFragment;
        } else if (loginStatus == LoginStatus.LoggedInInactive) {
            Log.d(TAG, "returning R.id.registerFragment");
            nextFragmentAction = R.id.registerFragment;
        } else if (loginStatus == LoginStatus.LoginFailed) {
            Log.d(TAG, "returning R.id.loginFragment");
            nextFragmentAction = R.id.loginFragment;
        }

        Log.d(TAG, "nextFragmentAction - returning value: " + nextFragmentAction);
        return nextFragmentAction;
    }


    public void setSelectedVpn(int vpnId) {
        LiveData<Boolean> setSelectedVpnLiveData = vpnRepository.setSelectedVpn(vpnId);
        setSelectedVpnLiveData.observeForever(new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isSuccess) {
                if (isSuccess != null) {
                    Log.d(TAG, "Set selected VPN " + (isSuccess ? "success" : "failed") + ", updating target fragment");
                    updateTargetFragment();
                    setSelectedVpnLiveData.removeObserver(this);
                }
            }
        });
    }


    public LiveData<Boolean> initializeAfterStartup() {
        Log.d(TAG, "initializeAfterStartup");
        authRepository.resetLoginStatus();
        MutableLiveData<Boolean> initializationComplete = new MutableLiveData<>();

        Observer<LoginStatus> loginStatusObserver = new Observer<LoginStatus>() {
            @Override
            public void onChanged(LoginStatus loginStatus) {
                Log.d(TAG, "Login status updated: " + loginStatus);
                if (loginStatus != null) {
                    authRepository.getLoginStatus().removeObserver(this);

                    if (loginStatus == LoginStatus.LoggedIn) {
                        vpnRepository.updateVpnList();
                        Observer<List<Vpn>> vpnListObserver = new Observer<List<Vpn>>() {
                            @Override
                            public void onChanged(List<Vpn> vpnList) {
                                Log.d(TAG, "VPN list updated, size: "
                                        + (vpnList == null ? "null" : (vpnList.size() + "")));
                                if (vpnList != null) {
                                    vpnRepository.getVpnList().removeObserver(this);

                                    int rememberedVpnId = VpnPreferences.getRememberedVpnSelection(context);
                                    boolean validVpnSelected = false;

                                    if (rememberedVpnId > 0) {
                                        // Check if the remembered VPN ID is valid
                                        for (Vpn vpn : vpnList) {
                                            if (vpn.getVpnId() == rememberedVpnId) {
                                                validVpnSelected = true;
                                                break;
                                            }
                                        }
                                    }

                                    if (validVpnSelected) {
                                        // Wait until the selected VPN is actually loaded
                                        Observer<Vpn> selectedVpnObserver = new Observer<Vpn>() {
                                            @Override
                                            public void onChanged(Vpn selectedVpn) {
                                                if (selectedVpn != null) {
                                                    vpnRepository.getSelectedVpn().removeObserver(this);
                                                    initializationComplete.setValue(true);
                                                    updateTargetFragment();
                                                }
                                            }
                                        };
                                        vpnRepository.getSelectedVpn().observeForever(selectedVpnObserver);
                                    } else {
                                        // If remembered ID is invalid, reset it and conclude
                                        VpnPreferences.setRememberedVpnSelection(context, 0);
                                        initializationComplete.setValue(true);
                                        updateTargetFragment();
                                    }
                                }
                            }
                        };
                        vpnRepository.getVpnList().observeForever(vpnListObserver);
                    } else {
                        // If not logged in (or any other status), initialization is done
                        initializationComplete.setValue(true);
                        updateTargetFragment();
                    }
                }
            }
        };

        authRepository.getLoginStatus().observeForever(loginStatusObserver);
        return initializationComplete;
    }

    public AuthRepository getAuthRepository() {
        return authRepository;
    }

    public boolean isRegistrationOngoing() {
        Log.d(TAG, "isRegistrationOngoing: " + registrationOngoing);
        return registrationOngoing;
    }

    public LiveData<String> getRegisterResult() {
        return registerResult;
    }

    public void register(String email, String password, int subscribeToNewsletter) {
        register(email, password, subscribeToNewsletter, 0);
    }

    public void register(String email, String password, int subscribeToNewsletter, int resend) {
        Log.d(TAG, "register: " + email + ", setting RegistrationOngoing to true");
        this.registrationOngoing = true;

        authRepository.register(email, password, subscribeToNewsletter, resend).observeForever(result -> {
            Log.d(TAG, "register: " + result + " observing activation status");

            Observer<ActivationStatus> observer = (new Observer<ActivationStatus>() {
                @Override
                public void onChanged(ActivationStatus activationStatus) {
                    Log.d(TAG, "register.onChanged: ActivationStatus " + activationStatus);
                    if (activationStatus == ActivationStatus.Active) {
                        Log.d(TAG, "register.onChanged: Active, setting RegistrationOngoing to false and removing observer");
                        registrationOngoing = false;
                        getActivationStatus().removeObserver(this);
                    }
                }
            });
            getActivationStatus().observeForever(observer);

            registerResult.setValue(result);
        });
    }

    public LiveData<ActivationStatus> getActivationStatus() {
        return authRepository.getActivationStatus();
    }

    public void updateActivationStatus() {
        Log.d("AuthViewModel", "Updating activation status");
        authRepository.updateActivationStatus();
    }


    public LiveData<LoginStatus> getLoginStatus() {
        return authRepository.getLoginStatus();
    }

    public void login(String email, String password) {
        Log.d(TAG, "login: " + email);
        authRepository.login(email, password);
    }


    public void logout() {
        LiveData<Boolean> logoutLiveData = authRepository.logout();
        logoutLiveData.observeForever(new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isSuccess) {
                Log.d(TAG, "Logout " + (isSuccess ? "success" : "failed") + ", resetting registerResult and updating target fragment");
                registerResult = new MutableLiveData<>();
                updateTargetFragment();
                logoutLiveData.removeObserver(this);
            }
        });
    }

    public void init() {
        Log.d(TAG, "init: ");
    }

    public void registerWithGoogleToken(String idToken, int subscribeToNewsletter) {
        Log.d(TAG, "registerWithGoogleToken: Delegating to AuthRepository");
        this.registrationOngoing = true;
        new Handler(Looper.getMainLooper()).post(() -> {
            authRepository.registerWithGoogleSignInByToken(idToken, subscribeToNewsletter).observeForever(result -> {
                Log.d(TAG, "register: " + result + " observing activation status");

                Observer<ActivationStatus> observer = (new Observer<ActivationStatus>() {
                    @Override
                    public void onChanged(ActivationStatus activationStatus) {
                        Log.d(TAG, "register.onChanged: ActivationStatus " + activationStatus);
                        if (activationStatus == ActivationStatus.Active) {
                            Log.d(TAG, "register.onChanged: Active, setting RegistrationOngoing to false and removing observer");
                            registrationOngoing = false;
                            getActivationStatus().removeObserver(this);
                        }
                    }
                });
                getActivationStatus().observeForever(observer);

                registerResult.setValue(result);
            });
        });
    }

}
