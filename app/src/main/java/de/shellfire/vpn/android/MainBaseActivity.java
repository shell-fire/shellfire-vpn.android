package de.shellfire.vpn.android;

import static de.shellfire.vpn.android.auth.LoginFragment.ARG_ACCOUNT_NAME;
import static de.shellfire.vpn.android.auth.LoginFragment.REGISTERACTIVITYSHOWN2;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBar.Tab;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.EOFException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import de.shellfire.vpn.android.auth.AuthRepository;
import de.shellfire.vpn.android.openvpn.VpnStatus;
import de.shellfire.vpn.android.utils.CommonUtils;
import de.shellfire.vpn.android.viewmodel.SharedViewModel;
import de.shellfire.vpn.android.viewmodel.SharedViewModelFactory;
import de.shellfire.vpn.android.widget.VpnStatusService;

public class MainBaseActivity extends AppCompatActivity implements ActionBar.TabListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int REQUEST_FOREGROUND_PERMISSION = 1022;
    protected static final String SELECTED_VPN_ID = "selectedVpnId";
    protected static final int LOGIN = 1;
    protected static final int SELECT_VPN = 2;
    protected static final int SHOW_PREFERENCES = 3;
    protected static final int REQUEST_PURCHASE = 124;
    public static final int START_VPN_PROFILE = 70;
    public static String packageName;

    public static String currentLanguage;

    public static boolean everythingLoaded;
    public static int trials = 0;
    /* private static final String PREMIUM_PLUS = "Premium\nPlus";*/

    private static ProgressDialog progressDialog;
    private static Float scale;

    public String localeLanguage;
    protected ProgressDialog progress;
    boolean isTablet;
    final String TAG = MainBaseActivity.class.getSimpleName();
    /**
     * The {@link PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentStatePagerAdapter} derivative,
     * which will keep every loaded fragment in memory. If this becomes too
     * memory intensive, it may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter sectionsPagerAdapter;
    private boolean mCmfixed;
    private int mPageCount;
    private int currentIdCounter = 105430;
    private AuthRepository authRepository;
    private LogRepository logRepository;
    private VpnConnectionManager vpnConnectionManager;
    private VpnRepository vpnRepository;
    private DataRepository dataRepository;
    private BillingRepository billingRepository;
    private Observer<Vpn> selectedVpnObserver;
    private Observer<List<Vpn>> vpnListObserver;
    private Observer<Integer> targetFragmentObserver;
    private Menu menu;
    private NavController navController;
    private SharedViewModel sharedViewModel;
    private final Map<Integer, String> tabletTitles = new HashMap<>();
    private final Map<Integer, String> phoneTitles = new HashMap<>();
    private  Observer<Boolean> isSettingProtocolObserver;
    private ViewPager2 viewPager;


    public static String getCurrentLanguage() {
        return currentLanguage;
    }

    public static void setCurrentLanguage(String currentLanguage) {
        MainBaseActivity.currentLanguage = currentLanguage;
    }

    public static int dpToPixel(int dp, Context context) {
        if (scale == null) scale = context.getResources().getDisplayMetrics().density;
        return (int) ((float) dp * scale);
    }

    int getContentView() {
        return -1;
    }

    String getLocaleLanguage() {
        return localeLanguage;
    }

    void setLocaleLanguage(String localeLanguage) {
        this.localeLanguage = localeLanguage;
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // unbindService(mConnection);
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }
        progress = null;
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = null;
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        // bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        localeLanguage = Locale.getDefault().getDisplayLanguage();
        if (currentLanguage == null || !currentLanguage.equalsIgnoreCase(localeLanguage)) {
            currentLanguage = localeLanguage;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        enableStrictMode();
        initializeTitles();

        packageName = getApplicationContext().getPackageName();

        isTablet = CommonUtils.isTablet(this);
        mPageCount = isTablet ? 1 : 2;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            Log.e(TAG, "NavHostFragment is null");
            return;
        }

        navController = navHostFragment.getNavController();

        // Check if the graph is already set
        if (savedInstanceState == null) {
            navController.getGraph();
        }

        viewPager = findViewById(R.id.view_pager);
        sectionsPagerAdapter = new SectionsPagerAdapter(this);
        viewPager.setAdapter(sectionsPagerAdapter);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();

        new TabLayoutMediator(findViewById(R.id.tab_layout), viewPager, (tab, position) -> {
            CharSequence title = getPageTitle(position);
            Log.d(TAG, "TabLayoutMediator.onTabSelected: Position: " + position + ", Title: " + title);
            tab.setText(title);
        }).attach();

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.loginFragment || destination.getId() == R.id.registerFragment || destination.getId() == R.id.selectVpnFragment) {
                findViewById(R.id.view_pager).setVisibility(View.GONE);
                findViewById(R.id.nav_host_fragment).setVisibility(View.VISIBLE);
                findViewById(R.id.toolbar).setVisibility(View.GONE);
                findViewById(R.id.tab_layout).setVisibility(View.GONE);
            } else {
                findViewById(R.id.view_pager).setVisibility(View.VISIBLE);
                findViewById(R.id.nav_host_fragment).setVisibility(View.GONE);
                findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
                findViewById(R.id.tab_layout).setVisibility(View.VISIBLE);
            }
        });

        authRepository = AuthRepository.getInstance(this);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        sharedViewModel = new ViewModelProvider(this, new SharedViewModelFactory(this)).get(SharedViewModel.class);
        logRepository = LogRepository.getInstance(this);
        vpnConnectionManager = VpnConnectionManager.getInstance(this);
        vpnRepository = VpnRepository.getInstance(this);
        dataRepository = DataRepository.getInstance(this);
        billingRepository = BillingRepository.getInstance(this);

        isSettingProtocolObserver = isSettingProtocol -> {
            if (isSettingProtocol) {
                showProgressDialogProtocolChangeInProcess();
            } else {
                hideProgressDialogProtocolChangeInProcess();
            }
        };
        vpnRepository.isSettingProtocol().observe(this, isSettingProtocolObserver);
    }



    private void showProgressDialogProtocolChangeInProcess() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.protocol_is_being_changed));
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(true);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), (dialog, which) -> {
            // User cancels the operation
            handleProtocolChangeTimeout();
        });
        progressDialog.show();

        // Dismiss the progress dialog after a timeout (e.g., 30 seconds)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                handleProtocolChangeTimeout();
            }
        }, 30000); // 30 seconds timeout
    }


    private void hideProgressDialogProtocolChangeInProcess() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void handleProtocolChangeTimeout() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        Toast.makeText(this, R.string.protocol_change_timeout, Toast.LENGTH_LONG).show();
    }


    private void initializeTitles() {
        Locale locale = Locale.getDefault();

        // Initialize tablet titles
        tabletTitles.put(0, getString(R.string.title_connect).toUpperCase(locale));
        tabletTitles.put(1, getString(R.string.title_billing).toUpperCase(locale));
        //tabletTitles.put(2, getString(R.string.title_log).toUpperCase(locale));

        // Initialize phone titles
        phoneTitles.put(0, getString(R.string.title_connect).toUpperCase(locale));
        phoneTitles.put(1, getString(R.string.title_locations).toUpperCase(locale));
        phoneTitles.put(2, getString(R.string.title_billing).toUpperCase(locale));
        // phoneTitles.put(3, getString(R.string.title_log).toUpperCase(locale));
    }

    public CharSequence getPageTitle(int position) {
        return isTablet ? tabletTitles.get(position) : phoneTitles.get(position);
    }



    private static void enableStrictMode() {
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build());


        try {
            Class.forName("dalvik.system.CloseGuard")
                    .getMethod("setEnabled", boolean.class)
                    .invoke(null, true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }


    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        // Observe login status
        observeTargetFragment();
        observeVpnList();
        EventBus.getDefault().register(this);

        Context context = getApplicationContext();
        if (!isServiceRunning(VpnStatusService.class)) {
            Intent serviceIntent = new Intent(context, VpnStatusService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            Log.d(TAG, "VpnStatusService started on app start.");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        unobserveTargetFragment();
        unobserveVpnList();
        EventBus.getDefault().unregister(this);
    }

    private void navigateToTarget(int targetId) {
        Log.d(TAG, "navigateToTarget");
        if (targetId == 0) {
            Log.e(TAG, "Invalid targetId, no navigation performed.");
            return; // Prevent navigating to an undefined fragment
        }

        final int currentFragment = navController.getCurrentDestination().getId();
        Log.d(TAG, "targetId: " + targetId + ", currentFragment: " + currentFragment);

        if (targetId != currentFragment) {
            try {
                navController.navigate(targetId);
                Log.d(TAG, "Navigated to targetId: " + targetId);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Navigation error: " + e.getMessage());
            }
        } else {
            Log.d(TAG, "Already at targetId: " + targetId);
        }
    }


    @Override
    public void onBackPressed() {
        if (shouldDisableBackButton()) {
            // Do nothing, which effectively disables the back button
            Log.d(TAG, "Back button disabled for this fragment.");
        } else {
            super.onBackPressed(); // Handle back press normally
        }
    }

    private boolean shouldDisableBackButton() {
        // Get the current fragment ID
        int currentFragmentId = navController.getCurrentDestination().getId();

        // List of fragment IDs for which the back button should be disabled
        int[] fragmentsToDisableBackButton = new int[]{
                R.id.mainSectionFragment,
                R.id.selectVpnFragment,
                R.id.registerFragment,
                R.id.loginFragment
        };

        // Check if the current fragment ID is in the list
        for (int fragmentId : fragmentsToDisableBackButton) {
            if (fragmentId == currentFragmentId) {
                return true;
            }
        }
        return false;
    }

    private void observeTargetFragment() {
        Log.d(TAG, "observeTargetFragment");
        targetFragmentObserver = new Observer<Integer>() {
            @Override
            public void onChanged(Integer targetId) {
                Log.d(TAG, "observeTargetFragment.onChanged - triggering nav update");
                navigateToTarget(targetId);
            }
        };

        sharedViewModel.getTargetFragment().observe(this, targetFragmentObserver);
    }

    private void unobserveTargetFragment() {
        sharedViewModel.getTargetFragment().removeObserver(targetFragmentObserver);
    }

    private void observeVpnList() {
        Log.d(TAG, "observeVpnList");
        vpnListObserver = new Observer<List<Vpn>>() {
            @Override
            public void onChanged(List<Vpn> vpnList) {
                if (menu == null) return; // Ensure the menu is available
                MenuItem selectVpnItem = menu.findItem(R.id.action_select_vpn);
                if (vpnList != null) {
                    if (vpnList.size() <= 1) {
                        Log.d(TAG, "observeVpnList - retrieved vpnList sized 1 - hiding pref option to select vpn");
                        if (selectVpnItem != null) selectVpnItem.setVisible(false);
                    } else {
                        Log.d(TAG, "observeVpnList - retrieved vpnList sized > 1 - showing pref option to select vpn");
                        if (selectVpnItem != null) selectVpnItem.setVisible(true);
                    }
                }
            }
        };

        vpnRepository.getVpnList().observe(this, vpnListObserver);
    }


    private void unobserveVpnList() {
        vpnRepository.getVpnList().removeObserver(vpnListObserver);
    }


    protected void handleException(final Exception e) {
        VpnStatus.logException(e);
        final MainBaseActivity mainActivity = this;

        if (e instanceof UnknownHostException || e instanceof EOFException || e instanceof SocketTimeoutException || e instanceof SSLException) {
            runOnUiThread(new Runnable() {
                public void run() {
                    AlertDialog.Builder alert = new AlertDialog.Builder(mainActivity);
                    alert.setTitle(getString(R.string.network_problem_title));
                    alert.setMessage(getString(R.string.network_problem_message) + "\n\n" + e);
                    alert.setPositiveButton(getString(android.R.string.ok), null);
                    alert.show();
                }
            });
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_mode_connection_key))) {
            String value = sharedPreferences.getString(key, getResources().getString(R.string.pref_mode_connection_key));
            Protocol protocolFromValue = Protocol.valueOf(value);
            switch (protocolFromValue) {
                case UDP:
                    changeTCPtoUDP(this);
                    break;
                case TCP:
                    changeUDPtoTCP(this);
                    break;
            }
        }
    }

    public boolean isEverythingLoaded() {
        return everythingLoaded;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }


        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }
    private void startRegisterFragment() {
        Log.d(TAG, "startRegisterFragment - start");
        final Runnable action = new Runnable() {
            public void run() {
                Log.d(TAG, "startRegisterFragment.run() - start");

                Bundle args = new Bundle();

                Intent callingIntent = getIntent();
                if (callingIntent != null && Intent.ACTION_VIEW.equals(callingIntent.getAction())) {
                    Uri uri = callingIntent.getData();
                    String path = uri.getPath();
                    String email = uri.getQueryParameter("email");
                    Log.d(TAG, "retrieved email from uri parameter: " + email);

                    if (path.equals("/activation")) {
                        args.putBoolean(REGISTERACTIVITYSHOWN2, true);
                        args.putString(ARG_ACCOUNT_NAME, email);
                    }
                }

                navController.navigate(R.id.action_mainSectionFragment_to_registerFragment, args);
                Log.d(TAG, "startRegisterFragment.run() - end");
            }
        };

        if (isInternetAvailable()) {
            Log.d(TAG, "internet available");
            action.run();
        } else {
            Log.d(TAG, "internet not available");
            showDialogInternetRequired(R.string.retry, action);
        }
        Log.d(TAG, "startRegisterFragment() - end");
    }


    private void afterLoginOk() {
        Log.d(TAG, "afterLoginOk() - start");
        // only if inflation already sucesful
        if (!isFinishing()) {
            Log.d(TAG, "NOT isFinishing() - starting InitializeEverythingTask");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final Runnable action = new Runnable() {
                        public void run() {
                            initializeEverything();
                        }
                    };

                    if (isInternetAvailable()) {
                        action.run();
                    } else {
                        showDialogInternetRequired(R.string.retry, action);
                    }
                }
            });

        } else {
            Log.d(TAG, "isFinishing() - not starting InitializeEverythingTask");
        }
    }

    protected void updateVPNProtocol(List<Vpn> vpnList, int vpnId) {
        for (int i = 0; i < vpnList.size(); i++) {
            Vpn vpn = vpnList.get(i);
            if (vpn.getVpnId() == vpnId) {
                VpnPreferences.setConnectionModeSelection(this, vpn.getProtocol());
            }
        }
    }

    protected void updateVPNProtocol(Vpn vpn) {
        VpnPreferences.setConnectionModeSelection(this, vpn.getProtocol());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);
        this.menu = menu; // Store the menu reference
        return true;
    }

    private void handleSelectVpn() {
        vpnRepository.setSelectedVpn(0);
        sharedViewModel.updateTargetFragment();
    }

    private void handleConnectionMode() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String currentMode = sharedPreferences.getString(getString(R.string.pref_mode_connection_key), getString(R.string.pref_mode_connection_default));

        // Inflate custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_connection_mode, null);
        RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroupConnectionMode);
        RadioButton radioUDP = dialogView.findViewById(R.id.radioUDP);
        RadioButton radioTCP = dialogView.findViewById(R.id.radioTCP);

        // Set the current selection
        if ("UDP".equals(currentMode)) {
            radioUDP.setChecked(true);
        } else {
            radioTCP.setChecked(true);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.action_connection_mode)
                .setView(dialogView)
                .create();

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (checkedId == R.id.radioUDP) {
                if (!"UDP".equals(currentMode)) {
                    editor.putString(getString(R.string.pref_mode_connection_key), "UDP");
                    editor.apply();
                }
            } else if (checkedId == R.id.radioTCP) {
                if (!"TCP".equals(currentMode)) {
                    editor.putString(getString(R.string.pref_mode_connection_key), "TCP");
                    editor.apply();
                }
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void handleAbout() {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    private void handleRate() {
        new AppRater(this).rateApp();
    }

    private void handleShare() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getText(R.string.share_text));
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.share_with)));
    }

    private void handleCurrentPlan() {
        Intent intentCurrentPlan = new Intent(this, PlansActivity.class);
        startActivity(intentCurrentPlan);
    }

    private void handleLog() {
        Intent logStartIntent = new Intent(this, LogActivity.class);
        startActivity(logStartIntent);
    }

    private void handleLogout() {
        sharedViewModel.logout();

        // Clear navigation stack
        navController.popBackStack(R.id.mainSectionFragment, true);
        navController.navigate(R.id.loginFragment);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SparseArray<Runnable> actions = new SparseArray<>();
        actions.put(R.id.action_select_vpn, this::handleSelectVpn);
        actions.put(R.id.action_connection_mode, this::handleConnectionMode);
        actions.put(R.id.action_about, this::handleAbout);
        actions.put(R.id.action_rate, this::handleRate);
        actions.put(R.id.action_share, this::handleShare);
        actions.put(R.id.about_current_plan, this::handleCurrentPlan);
        actions.put(R.id.action_log, this::handleLog);
        actions.put(R.id.logout, this::handleLogout);

        Runnable action = actions.get(item.getItemId());
        if (action != null) {
            action.run();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        Log.d(TAG, "onActivityResult: " + requestCode + ", " + resultCode);
        // Check which request we're responding to
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOGIN) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "onActivityResult: LOGIN, RESULT_OK - calling startLoginOk()");
                afterLoginOk();

            } else {
                Log.d(TAG, "onActivityResult: LOGIN, RESULT NOT OK - calling startLoginActivity()");
                startRegisterFragment();
            }
        }
        if (requestCode == SELECT_VPN) {
            Log.d(TAG, "onActivityResult: SELECT_VPN");
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "onActivityResult: SELECT_VPN RESULT_OK");
                Bundle extras = data.getExtras();
                final int vpnId = extras.getInt(SELECTED_VPN_ID);
                Runnable action = new Runnable() {
                    public void run() {
                        registerSelectedVpn(vpnId);
                    }
                };

                if (isInternetAvailable()) {
                    action.run();
                } else {
                    showDialogInternetRequired(R.string.retry, action);
                }
            }
        }

        if (requestCode == START_VPN_PROFILE) {
            Log.d(TAG, "onActivityResult: START_VPN_PROFILE");
            // Delegate handling to VpnConnectionManager
            vpnConnectionManager.handleActivityResult(resultCode);
        }


    }

    private void registerSelectedVpn(int vpnId) {
        Log.d(TAG, "registerSelectedVpn: " + vpnId);
        vpnRepository.setSelectedVpn(vpnId);

    }

    private boolean checkReconnectionSevenDaysPass() {
        long savedTimeMilles = VpnPreferences.getMostRecentAutoSwitchFromUdpToTcpDate(this);
        if (savedTimeMilles != 0) {
            long daysDifference = getDifferenceBetweenCurrentAndSavedTimeInDays(savedTimeMilles);
            if (daysDifference > Constants.DAYS_NUMBER_MAX_FOR_SWITCH_TCP_TO_UDP) {
                VpnPreferences.unsetMostRecentAutoSwitchFromUdpToTcpDate(this);
                return true;
            }
        }
        return false;
    }

    private long getDifferenceBetweenCurrentAndSavedTimeInDays(long savedTimeMilles) {
        long currentTimeMills = System.currentTimeMillis();
        long difference = currentTimeMills - savedTimeMilles;
        return TimeUnit.MILLISECONDS.toDays(difference);
    }


    private void showDialogInternetRequired(int id, final Runnable action) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getString(R.string.network_problem_title));
        alert.setMessage(getString(R.string.network_problem_message));
        alert.setPositiveButton(getString(id), new OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                if (isInternetAvailable()) {
                    action.run();
                } else {
                    showDialogInternetRequired(R.string.retry, action);
                }
            }

        });
        alert.show();
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        if (viewPager != null) {
            viewPager.setCurrentItem(tab.getPosition());
        }
    }

    protected boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }


    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(UpgradeMessage event) {
        navController.navigate(R.id.action_global_to_serverSelectSectionFragment);
    }



    public void initializeEverything() {
        Log.d(TAG, "initializeEverything() - start");
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.loading));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.show();

        vpnRepository.getSelectedVpn().observe(this, vpn -> {
            Log.d(TAG, "initializeEverything.getSelectedVpn observed vpn: " + vpn);


            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            View mainlayout = findViewById(R.id.mainLayout);
            if (mainlayout != null) {
                mainlayout.setVisibility(android.view.View.VISIBLE);
            } else {
                Log.d(TAG, "mainlayout is null, not setting visibility of mainlayout to VISIBLE");
            }

            everythingLoaded = true;
        });
    }

    public void triggerDataSetChanged() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (!fragmentManager.isStateSaved() && !fragmentManager.isDestroyed()) {
            fragmentManager.executePendingTransactions(); // Ensure all pending transactions are executed
            sectionsPagerAdapter.notifyDataSetChanged(); // Update the adapter
        }
    }

    public void setCurrentItem(int i) {
        if (viewPager != null) {
            viewPager.setCurrentItem(i);
        }
    }


    /**
     * A {@link FragmentStatePagerAdapter} that returns a fragment corresponding
     * to one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStateAdapter {

        private final Map<Integer, Class<? extends Fragment>> phoneFragmentMap = new HashMap<>();
        private final Map<Integer, Class<? extends Fragment>> tabletFragmentMap = new HashMap<>();
        private final SparseArray<Fragment> registeredFragments = new SparseArray<>();
        private final Map<Integer, String> tabletTitles = new HashMap<>();
        private final Map<Integer, String> phoneTitles = new HashMap<>();
        private int mPageCount;
        private Map<Integer, Fragment> fragmentMap;

        public SectionsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
            initializeFragmentMaps();
        }
        private void initializeFragmentMaps() {
            phoneFragmentMap.put(0, MainSectionFragment.class);
            phoneFragmentMap.put(1, PhoneServerListFragment.class);
            phoneFragmentMap.put(2, PremiumFragment.class);
            //phoneFragmentMap.put(3, LogSectionFragment.class);

            tabletFragmentMap.put(0, MainAndServerSelectionFragment.class);
            tabletFragmentMap.put(1, PremiumFragment.class);
            // tabletFragmentMap.put(2, LogSectionFragment.class);
        }

        <T extends Fragment> T createFragmentInstance(Class<T> fragmentClass) throws IllegalAccessException, InstantiationException {
            Log.d(TAG, "Creating fragment instance for class: " + fragmentClass.getSimpleName());
            Fragment fragment = fragmentClass.newInstance();

            return (T) fragment;
        }

        public Fragment createFragment(int position) {
            Map<Integer, Class<? extends Fragment>> currentFragmentMap = isTablet ? tabletFragmentMap : phoneFragmentMap;

            if (!currentFragmentMap.containsKey(position)) {
                throw new IllegalArgumentException("Invalid position: " + position + ", Map size: " + currentFragmentMap.size());
            }

            Class<? extends Fragment> fragmentClass = currentFragmentMap.get(position);
            Fragment fragment;
            try {
                fragment = createFragmentInstance(fragmentClass);
            } catch (IllegalAccessException | InstantiationException e) {
                throw new RuntimeException(e);
            }
            return fragment;
        }

        @Override
        public int getItemCount() {
            return isTablet ? tabletFragmentMap.size() : phoneFragmentMap.size();
        }

        @Override
        public long getItemId(int position) {
            return position; // Use position as the unique ID
        }

        @Override
        public boolean containsItem(long itemId) {
            return itemId >= 0 && itemId < getItemCount();
        }

    }

    private void changeProtocol(Protocol protocol) {
        vpnRepository.setProtocol(protocol);
    }

    private void changeTCPtoUDP(Context context) {
        changeProtocol(Protocol.UDP);
    }

    private void changeUDPtoTCP(Context context) {
        changeProtocol(Protocol.TCP);
    }


}