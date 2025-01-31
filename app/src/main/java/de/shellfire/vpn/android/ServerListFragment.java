package de.shellfire.vpn.android;


import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import de.shellfire.vpn.android.adapters.ServerListAdapter;
import de.shellfire.vpn.android.utils.CommonUtils;
import de.shellfire.vpn.android.viewmodel.SharedViewModel;
import de.shellfire.vpn.android.viewmodel.SharedViewModelFactory;

public class ServerListFragment extends Fragment {
    private static final String TAG = "ServerListFragment";
    public static boolean isVisibleET;
    private Observer<Boolean> setServerToObserver;
    private Observer<Boolean> isSettingServerObserver;
    private Observer<Vpn> selectedVpnObserver;
    private Observer<Server> selectedServerObserver;
    private Observer<List<Vpn>> vpnListObserver;
    public Dialog dialog;
    String mUser;
    ListView listview;
    LinearLayout rateOne;
    LinearLayout rateTwo;
    LinearLayout rateThree;
    RelativeLayout loadFilter;
    ServerListAdapter adapter;
    ImageView search;
    ImageView loadImg;
    EditText searchedText;
    TextView header;
    SeekBar loadSeekBar;
    Button advancedModeButton;
    RelativeLayout filterLayout;
    LinearLayout serverListLayout;
    boolean isPressedOne;
    boolean isPressedTwo;
    boolean isPressedThree;
    boolean isVisibleFilter;
    int posList = 0;
    Context context;
    private VpnConnectionManager vpnConnectionManager;
    private VpnRepository vpnRepository;
    private DataRepository dataRepository;
    private FilterViewModel filterViewModel;
    private ProgressDialog progressDialog;
    private Dialog serverChangeDialog;
    private boolean isAdvancedMode = false;
    private SharedViewModel sharedViewModel;



    void setBackgroundIfRatePressed(View view, boolean isPressed) {
        if (isPressed) {
            setRateBg(view, R.drawable.rate_blue_bg);
        } else {
            setRateBg(view, R.drawable.rate_light_gray_bg);
        }
    }

    void setRateBg(View view, int drawableId) {
        if (view != null) {
            FragmentActivity activity = getActivity();
            if (activity != null) {
                view.setBackground(ResourcesCompat.getDrawable(getResources(), drawableId, activity.getTheme()));
            }

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        if (searchedText != null && !searchedText.getText().toString().isEmpty()) {
            header.setVisibility(View.GONE);
            searchedText.setVisibility(View.VISIBLE);
        }
        // Re-add observers
        addObservers();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
        // Remove observers
        removeObservers();
    }

    private void addObservers() {
        vpnRepository.getSetServerTo().observe(getViewLifecycleOwner(), setServerToObserver);
        vpnRepository.getIsSettingServer().observe(getViewLifecycleOwner(), isSettingServerObserver);
        vpnRepository.getSelectedVpn().observe(getViewLifecycleOwner(), selectedVpnObserver);
        vpnRepository.getSelectedServer().observe(getViewLifecycleOwner(), selectedServerObserver);
        vpnRepository.getVpnList().observe(getViewLifecycleOwner(), vpnListObserver);
    }

    private void removeObservers() {
        vpnRepository.getSetServerTo().removeObserver(setServerToObserver);
        vpnRepository.getIsSettingServer().removeObserver(isSettingServerObserver);
        vpnRepository.getSelectedVpn().removeObserver(selectedVpnObserver);
        vpnRepository.getSelectedServer().removeObserver(selectedServerObserver);
        vpnRepository.getVpnList().removeObserver(vpnListObserver);
    }

    public void updateFilterUI() {
        if (getContext() != null) {
            if (dataRepository == null) {
                dataRepository = DataRepository.getInstance(getContext());
            }
            LiveData<List<Server>> allServersLiveData = dataRepository.getServerList();
            updateFilterUI(allServersLiveData);
        }
    }

    public void updateFilterUI(LiveData<List<Server>> baseServerList) {
        Log.d(TAG, "updateFilterUI - start");

        if (loadSeekBar != null && filterViewModel != null) {
            Log.d(TAG, "updateFilterUI - loadSeekBar and filterViewModel are not null");

            filterViewModel.getFilteredServers(baseServerList, isPressedOne, isPressedTwo, isPressedThree,
                            loadSeekBar.getProgress(), isAdvancedMode, searchedText.getText().toString().trim())
                    .observe(getViewLifecycleOwner(), this::handleFilteredServers);
        } else {
            Log.d(TAG, "updateFilterUI - loadSeekBar or filterViewModel is null");
        }

        Log.d(TAG, "updateFilterUI - end");
    }

    private void handleFilteredServers(List<Server> filteredServers) {
        Log.d(TAG, "updateFilterUI - observed LiveData change");

        if (filteredServers != null) {
            Log.d(TAG, "updateFilterUI - servers.size: " + filteredServers.size());
        } else {
            Log.d(TAG, "updateFilterUI - servers are null");
        }

        Log.d(TAG, "updateFilterUI - servers: " + filteredServers);

        Context context = getContext();
        if (context != null) {
            Log.d(TAG, "updateFilterUI - context is not null");

            adapter.setFilterList(filteredServers);
            adapter.notifyDataSetChanged();
            if (isAdvancedMode) {
                Log.d(TAG, "updateFilterUI - advancedServersShowModeEnabled is true");
                advancedModeButton.setText(getString(R.string.STANDARD));
                adapter.setAdvancedList(true);
                loadImg.setVisibility(View.VISIBLE);
                filterLayout.setVisibility(View.VISIBLE);

                if (isVisibleFilter) {
                    Log.d(TAG, "updateFilterUI - isVisibleFilter is true");
                    loadFilter.setVisibility(View.VISIBLE);

                    if (getActivity() != null) {
                        Log.d(TAG, "updateFilterUI - getActivity is not null");
                        loadImg.setColorFilter(ContextCompat.getColor(getActivity(), R.color.base_blue_color));
                    }

                    adapter.setDisplayLoadProgressBarNoNotify(true);
                    isVisibleFilter = true;
                } else {
                    Log.d(TAG, "updateFilterUI - isVisibleFilter is false");
                    loadFilter.setVisibility(View.GONE);
                    loadImg.setColorFilter(null);
                    adapter.setDisplayLoadProgressBarNoNotify(false);
                    isVisibleFilter = false;
                }
            } else {
                Log.d(TAG, "updateFilterUI - advancedServersShowModeEnabled is false");
                loadFilter.setVisibility(View.GONE);
                loadImg.setVisibility(View.INVISIBLE);
            }

        } else {
            Log.d(TAG, "updateFilterUI - context is null");
        }
    }


    private void setupAdvancedMode(List<Server> filteredServers) {
        Log.d(TAG, "setupAdvancedMode - start");

        advancedModeButton.setText(getString(R.string.STANDARD));
        adapter.setAdvancedList(true);
        loadImg.setVisibility(View.VISIBLE);
        filterLayout.setVisibility(View.VISIBLE);

        if (isVisibleFilter) {
            loadFilter.setVisibility(View.VISIBLE);
            loadImg.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.base_blue_color));
            adapter.setDisplayLoadProgressBarNoNotify(true);
            isVisibleFilter = true;
        } else {
            loadFilter.setVisibility(View.GONE);
            loadImg.setColorFilter(null);
            adapter.setDisplayLoadProgressBarNoNotify(false);
            isVisibleFilter = false;
        }
    }

    private void setupStandardMode() {
        Log.d(TAG, "setupStandardMode - start");

        loadFilter.setVisibility(View.GONE);
        loadImg.setVisibility(View.INVISIBLE);
    }


    void toggleAdvancedMode() {
        Log.d(TAG, "disableAdvancedMode - start");

        isAdvancedMode = !isAdvancedMode;
        // Save the new mode to SharedPreferences
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.IS_ADVANCED_SHOW_LIST, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Constants.IS_ADVANCED_SHOW_LIST, isAdvancedMode);
        editor.apply();

        updateLayoutAccordingToMode();
    }

    private void updateLayoutAccordingToMode() {
        if (getContext() != null) {
            if (dataRepository == null) {
                dataRepository = DataRepository.getInstance(getContext());
            }
            LiveData<List<Server>> servers = dataRepository.getServerList();
            LiveData<List<Server>> filteredServerListLiveData = null;

            filteredServerListLiveData = servers;
            if (isAdvancedMode) {
                advancedModeButton.setText(getString(R.string.STANDARD));
            } else {
                advancedModeButton.setText(getString(R.string.ADVANCED));
            }

            if (adapter != null)
                adapter.setAdvancedList(isAdvancedMode);

            if (isAdded()) {
                filterLayout.setVisibility(View.INVISIBLE);
                loadFilter.setVisibility(View.GONE);
                loadImg.setVisibility(View.INVISIBLE);
                updateFilterUI(filteredServerListLiveData);
            }
        }

    }


    public void setUser(String user) {
        mUser = user;
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(Constants.ONE_CROWN, isPressedOne);
        outState.putBoolean(Constants.TWO_CROWN, isPressedTwo);
        outState.putBoolean(Constants.THREE_CROWN, isPressedThree);
        outState.putBoolean(Constants.IS_VISIBLE_FILTER, isVisibleFilter);
        outState.putBoolean(Constants.IS_ADVANCED_SHOW_LIST, isAdvancedMode);
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        setRetainInstance(true);

        // Initialize SharedViewModel
        sharedViewModel = new ViewModelProvider(requireActivity(), new SharedViewModelFactory(
                requireActivity()
        )).get(SharedViewModel.class);


        // Load the mode from SharedPreferences
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.IS_ADVANCED_SHOW_LIST, Context.MODE_PRIVATE);
        isAdvancedMode = prefs.getBoolean(Constants.IS_ADVANCED_SHOW_LIST, false);
    }

    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        vpnConnectionManager = VpnConnectionManager.getInstance(view.getContext());
        vpnRepository = VpnRepository.getInstance(view.getContext());
        dataRepository = DataRepository.getInstance(view.getContext());
        FilterViewModelFactory factory = new FilterViewModelFactory(requireContext());
        filterViewModel = new ViewModelProvider(this, factory).get(FilterViewModel.class);


        // Initialize and start observing setServerToResult
        setServerToObserver = setServerToResult -> {
            if (setServerToResult != null) {
                if (setServerToResult) {
                    updateFilterUI();
                    vpnRepository.getSetServerTo().setValue(null);
                    showDialogServerChangeSuccessfulDoYouWantToConnect();
                }
            }
        };

            // Initialize and start observing isSettingServer
            isSettingServerObserver = isSettingServer -> {
                Log.d(TAG, "isSettingServerObserver. isSettingServer: " + (isSettingServer ? "true" : "false"));
                if (isSettingServer) {
                    showProgressDialogServerChangeInProcess();
                } else {
                    hideProgressDialogServerChangeInProcess();
                }
            };

            // Initialize and start observing selectedVpn
            selectedVpnObserver = selectedVpn -> {
                Log.d(TAG, "selectedVpnObserver - selectedVpn is: " + selectedVpn);
                if (selectedVpn != null) {
                    Log.d(TAG, "selectedVpnObserver - selectedVpn is not null");
                    updateFilterUI();
                }
            };

            // Initialize and start observing selectedVpn
            selectedServerObserver = selectedServer -> {
                Log.d(TAG, "selectedServerObserver - selectedServer is: " + selectedServer);
                if (selectedServer != null) {
                    Log.d(TAG, "selectedServerObserver - selectedServer is not null");
                    updateFilterUI();
                }
            };

            vpnListObserver = vpnList -> {
                Log.d(TAG, "vpnListObserver - vpnList is: " + vpnList);
                if (vpnList != null) {
                    Log.d(TAG, "vpnListObserver - vpnList is not null");
                    updateFilterUI();
                }
            };

            // Add observers
            addObservers();

    }


    private void showProgressDialogServerChangeInProcess() {
        if (getContext() != null) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage(getString(R.string.serverchange_is_being_processed_might_take_a_while));
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(true);
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), (dialog, which) -> {
                // User cancels the operation
                handleServerChangeTimeout();
            });
            progressDialog.show();

            // Dismiss the progress dialog after a timeout (e.g., 30 seconds)
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (progressDialog != null && progressDialog.isShowing()) {
                    handleServerChangeTimeout();
                }
            }, 30000); // 30 seconds timeout
        }
    }

    private void handleServerChangeTimeout() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        Toast.makeText(getContext(), R.string.serverchange_timeout, Toast.LENGTH_LONG).show();
    }

    private void hideProgressDialogServerChangeInProcess() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        final View rootView = inflater.inflate(R.layout.fragment_serverselect_view, container, false);
        createList(rootView);

        setBackgroundIfRatePressed(rateOne, isPressedOne);
        setBackgroundIfRatePressed(rateTwo, isPressedTwo);
        setBackgroundIfRatePressed(rateThree, isPressedThree);
        if (isVisibleFilter) {
            loadFilter.setVisibility(View.VISIBLE);
            FragmentActivity activity = getActivity();
            if (activity != null) {
                loadImg.setColorFilter(ContextCompat.getColor(activity, R.color.base_blue_color));
            }

            adapter.setDisplayLoadProgressBar(true);
            isVisibleFilter = true;
        }
        return rootView;
    }

    public void createList(View rootView) {
        Log.d(TAG, "createList - start");

        initViews(rootView);
        Log.d(TAG, "createList - views initialized");

        setClickListeners();
        initializeAdapterAndView();
        setListViewItemClickListener();

        Log.d(TAG, "createList - end");
    }

    private void setClickListeners() {
        rateOne.setOnClickListener(new RateOneClickListener());
        Log.d(TAG, "createList - RateOneClickListener set");

        rateTwo.setOnClickListener(new RateTwoClickListener());
        Log.d(TAG, "createList - RateTwoClickListener set");

        rateThree.setOnClickListener(new RateThreeClickListener());
        Log.d(TAG, "createList - RateThreeClickListener set");

        search.setOnClickListener(new SearchClickListener());
        Log.d(TAG, "createList - SearchClickListener set");

        loadImg.setOnClickListener(new LoadImgClickListener());
        Log.d(TAG, "createList - LoadImgClickListener set");

        searchedText.addTextChangedListener(new FilterTextWatcher());
        Log.d(TAG, "createList - FilterTextWatcher added");

        searchedText.setOnEditorActionListener(new FinishTypingListener());
        Log.d(TAG, "createList - FinishTypingListener set");

        loadSeekBar.setOnSeekBarChangeListener(new LoadSeekbarListener());
        Log.d(TAG, "createList - LoadSeekbarListener set");

        advancedModeButton.setOnClickListener(new AdvancedStandardModeListener());
        Log.d(TAG, "createList - AdvancedStandardModeListener set");
    }

    private void initializeAdapterAndView() {
        if (listview != null) {
            Log.d(TAG, "createList - listview is not null");

            final AppCompatActivity a = (AppCompatActivity) getActivity();
            if (a != null) {
                Log.d(TAG, "createList - AppCompatActivity is not null");

                if (adapter == null) {
                    Log.d(TAG, "createList - adapter is null, creating new adapter");

                    if (dataRepository == null && getContext() != null) {
                        dataRepository = DataRepository.getInstance(getContext());
                        Log.d(TAG, "createList - dataRepository instance created");
                    }

                    if (dataRepository != null) {
                        dataRepository.getServerList().observe(getViewLifecycleOwner(), servers -> {
                            Log.d(TAG, "createList - server list observed");
                            if (servers != null && !servers.isEmpty()) {
                                Log.d(TAG, "createList - servers list is not null or empty, size: " + servers.size());
                                adapter = new ServerListAdapter(a, android.R.layout.simple_list_item_1, servers, isAdvancedMode, this, getViewLifecycleOwner());

                                listview.setAdapter(adapter);
                                Log.d(TAG, "createList - adapter set to listview");
                            } else {
                                Log.d(TAG, "createList - servers list is null or empty");
                            }
                        });
                    }
                } else {
                    Log.d(TAG, "createList - adapter is not null, updating filter UI");
                    updateFilterUI();
                }

                listview.setAdapter(adapter);
                Log.d(TAG, "createList - adapter set to listview");

                listview.setSelection(posList);
                Log.d(TAG, "createList - listview selection set to posList: " + posList);
            } else {
                Log.d(TAG, "createList - AppCompatActivity is null, setting adapter to null");
                adapter = null;
            }
        }
    }

    private void setListViewItemClickListener() {
        Log.d(TAG, "setListViewItemClickListener - start");
        if (listview != null) {
            listview.setOnItemClickListener((parent, view, position, id) -> {
                Log.d(TAG, "setListViewItemClickListener - listview item clicked, position: " + position);

                if (parent != null) {
                    final Server item = (Server) parent.getItemAtPosition(position);
                    posList = position;
                    Log.d(TAG, "setListViewItemClickListener - item: " + item.getName() + ", posList: " + posList);

                    if (view != null) {
                        view.setAlpha(1);
                        handleServerChange(item);
                        Log.d(TAG, "setListViewItemClickListener - view alpha set to 1");
                    }
                }
            });
        }
    }

    public void handleServerChange(Server item) {
        Log.d(TAG, "handleServerChange(item) - start");
        // Define observers explicitly
        Observer<Vpn> selectedVpnObserver = new Observer<Vpn>() {
            @Override
            public void onChanged(Vpn selectedVpn) {
                if (selectedVpn != null) {
                    Log.d(TAG, "handleServerChange - selectedVpn is not null");

                    Observer<Server> selectedServerObserver = new Observer<Server>() {
                        @Override
                        public void onChanged(Server selectedServer) {
                            if (selectedServer != null && selectedServer.getVpnServerId() == item.getVpnServerId()) {
                                Log.d(TAG, "createList - already on this server, do nothing");
                            } else {
                                Log.d(TAG, "createList - server is different, checking eligibility");
                                handleServerChange(item, selectedVpn);
                            }
                            vpnRepository.getSelectedServer().removeObserver(this);
                        }
                    };

                    vpnRepository.getSelectedServer().observe(getViewLifecycleOwner(), selectedServerObserver);
                } else {
                    Log.d(TAG, "handleServerChange - selectedVpn is null, triggering updateTargetFragment()");
                    sharedViewModel.updateTargetFragment();
                }
                vpnRepository.getSelectedVpn().removeObserver(this);
            }
        };

        // Start observing
        vpnRepository.getSelectedVpn().observe(getViewLifecycleOwner(), selectedVpnObserver);
        Log.d(TAG, "handleServerChange(item) - stop");
    }

    private void handleServerChange(Server item, Vpn selectedVpn) {
        ServerType vpnLevel = selectedVpn.getAccountType();
        ServerType serverLevel = item.getServerType();
        int vpnLevelInt = vpnLevel.ordinal();
        int serverLevelInt = serverLevel.ordinal();

        if (serverLevelInt > vpnLevelInt) {
            Log.d(TAG, "createList - server not available to user, showing upgrade dialog");
            showDialogDoYouWantToUpgrade();
        } else {
            Log.d(TAG, "createList - server available to user, showing confirmation dialog");
            showDialogAreYouSureYouWantToChangeTheServer(item);
        }
    }

    public void selectTab(int tabIndex) {
        ViewPager2 viewPager = getActivity().findViewById(R.id.view_pager);
        if (viewPager != null) {
            viewPager.setCurrentItem(tabIndex, true); // 'true' for smooth scrolling
            Log.d(TAG, "Selected tab at index: " + tabIndex);
        } else {
            Log.e(TAG, "ViewPager2 not found in the layout");
        }
    }

    private void showDialogDoYouWantToUpgrade() {
        View.OnClickListener viewOnClickListener = v -> {
            Log.d(TAG, "showDialogDoYouWantToUpgrade - user clicked yes or no");

            SparseArray<Runnable> actions = new SparseArray<>();
            actions.put(R.id.btnOk, () -> {
                dialog.dismiss();
                if (getActivity() != null) {
                    int navItemNo = CommonUtils.isTablet(context) ? 1 : 2;
                    selectTab(navItemNo);
                }
            });
            actions.put(R.id.btnNo, () -> {
                dialog.dismiss();
                Log.d(TAG, "showDialogDoYouWantToUpgrade - user clicked no - upgrade dialog dismissed");
            });

            Runnable action = actions.get(v.getId());
            if (action != null) {
                action.run();
            }
        };


        if (context != null) {
            dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.setContentView(R.layout.dialog_server_upgrade);
            TextView question = dialog.findViewById(R.id.attention_text);
            question.setText(R.string.you_are_not_eligible_to_use_this_server);
            Button btnYes = dialog.findViewById(R.id.btnOk);
            btnYes.setOnClickListener(viewOnClickListener);
            Button btnNo = dialog.findViewById(R.id.btnNo);
            btnNo.setOnClickListener(viewOnClickListener);
            dialog.setCancelable(false);
            dialog.show();
            Log.d(TAG, "showDialogDoYouWantToUpgrade - upgrade dialog shown");
        }
    }

    private void showDialogAreYouSureYouWantToChangeTheServer(Server item) {
        VpnConnectionManager vpnConnectionManager = VpnConnectionManager.getInstance(getContext());

        Observer<SimpleConnectionStatus> connectionStatusObserver = new Observer<SimpleConnectionStatus>() {
            @Override
            public void onChanged(SimpleConnectionStatus connectionStatus) {
                View.OnClickListener viewOnClickListenerSureChangeServer = v -> {
                    SparseArray<Runnable> actions = new SparseArray<>();
                    actions.put(R.id.btnOk, () -> {
                        dialog.dismiss();
                        final boolean connected = connectionStatus == SimpleConnectionStatus.Connected;

                        if (connected) {
                            View.OnClickListener viewOnClickListenerAlreadyConnected = v1 -> {
                                SparseArray<Runnable> innerActions = new SparseArray<>();
                                innerActions.put(R.id.btnOk, () -> {
                                    dialog.dismiss();
                                    vpnConnectionManager.disconnect();
                                    changeServerConfirmed(item);
                                });
                                innerActions.put(R.id.btnNo, dialog::dismiss);

                                Runnable innerAction = innerActions.get(v1.getId());
                                if (innerAction != null) {
                                    innerAction.run();
                                }
                            };

                            if (context != null) {
                                dialog = new Dialog(context);
                                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                                dialog.setContentView(R.layout.dialog_server_upgrade);
                                TextView question = dialog.findViewById(R.id.attention_text);
                                question.setText(R.string.you_are_currently_connecting_disconnect);
                                Button btnYes = dialog.findViewById(R.id.btnOk);
                                btnYes.setOnClickListener(viewOnClickListenerAlreadyConnected);
                                Button btnNo = dialog.findViewById(R.id.btnNo);
                                btnNo.setOnClickListener(viewOnClickListenerAlreadyConnected);
                                dialog.setCancelable(false);
                                dialog.show();
                            }
                        } else {
                            changeServerConfirmed(item);
                        }
                    });
                    actions.put(R.id.btnNo, dialog::dismiss);

                    Runnable action = actions.get(v.getId());
                    if (action != null) {
                        action.run();
                    }
                };

                if (dialog != null && dialog.isShowing()) {
                    Log.d(TAG, "createList - confirmation dialog is already showing");
                    return;
                }

                if (context != null) {
                    dialog = new Dialog(context);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                    dialog.setContentView(R.layout.dialog_server_upgrade);
                    TextView question = dialog.findViewById(R.id.attention_text);
                    question.setText(R.string.sure_to_change_server);
                    Button btnYes = dialog.findViewById(R.id.btnOk);
                    btnYes.setOnClickListener(viewOnClickListenerSureChangeServer);
                    Button btnNo = dialog.findViewById(R.id.btnNo);
                    btnNo.setOnClickListener(viewOnClickListenerSureChangeServer);

                    if (connectionStatus == SimpleConnectionStatus.Connected) {
                        vpnConnectionManager.disconnect();
                    }

                    dialog.setCancelable(false);
                    dialog.show();
                }

                vpnConnectionManager.getConnectionStatus().removeObserver(this);
            }
        };


        vpnConnectionManager.getConnectionStatus().observe(getViewLifecycleOwner(), connectionStatusObserver);
    }

    private void initViews(View rootView) {
        Log.d(TAG, "initViews called");
        if (rootView == null) {
            FragmentActivity a = getActivity();
            if (a != null) {
                Log.d(TAG, "initViews - creating ServerListAdapter");
                if (dataRepository == null) {
                    dataRepository = DataRepository.getInstance(getContext());
                }
                dataRepository.getServerList().observe(getViewLifecycleOwner(), servers -> {
                    adapter = new ServerListAdapter(a, android.R.layout.simple_list_item_1, servers, isAdvancedMode, this, getViewLifecycleOwner());
                });
                listview = a.findViewById(R.id.listview);
                rateOne = a.findViewById(R.id.rate_one);
                rateTwo = a.findViewById(R.id.rate_two);
                rateThree = a.findViewById(R.id.rate_three);
                loadFilter = a.findViewById(R.id.load_filter);
                search = a.findViewById(R.id.search);
                loadImg = a.findViewById(R.id.load);
                searchedText = a.findViewById(R.id.search_et);
                header = a.findViewById(R.id.header);
                loadSeekBar = a.findViewById(R.id.seekbar);
                advancedModeButton = a.findViewById(R.id.advanced_mode_button);
                filterLayout = a.findViewById(R.id.filter_layout);
                serverListLayout = a.findViewById(R.id.server_list_layout);
            }

        } else {
            serverListLayout = rootView.findViewById(R.id.server_list_layout);
            listview = rootView.findViewById(R.id.listview);
            rateOne = rootView.findViewById(R.id.rate_one);
            rateTwo = rootView.findViewById(R.id.rate_two);
            rateThree = rootView.findViewById(R.id.rate_three);
            loadFilter = rootView.findViewById(R.id.load_filter);
            search = rootView.findViewById(R.id.search);
            loadImg = rootView.findViewById(R.id.load);
            searchedText = rootView.findViewById(R.id.search_et);
            header = rootView.findViewById(R.id.header);
            loadSeekBar = rootView.findViewById(R.id.seekbar);
            advancedModeButton = rootView.findViewById(R.id.advanced_mode_button);
            filterLayout = rootView.findViewById(R.id.filter_layout);
        }

        updateLayoutAccordingToMode();
    }

    void changeServerConfirmed(Server item) {
        FragmentActivity a = getActivity();
        Log.d("change server", "fragmentactivity= " + a);
        if (a != null) {
            changeServer(item);
        }
    }

    private void showKeyboard() {
        searchedText.requestFocus();
        FragmentActivity activity = getActivity();
        if (activity != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchedText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    public void hideKeyboardWithSearch(Activity activity) {
        CommonUtils.hideKeyboard(activity);
        searchedText.clearFocus();
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        this.context = context;
        Log.d(TAG, "on Attach");
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        Log.d(TAG, "setUserVisibleHint called");
        super.setUserVisibleHint(isVisibleToUser);
        updateFilterUI();
    }

    public ServerListAdapter getAdapter() {
        return adapter;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated called");
        if (savedInstanceState != null) {
            isPressedOne = savedInstanceState.getBoolean(Constants.ONE_CROWN);
            isPressedTwo = savedInstanceState.getBoolean(Constants.TWO_CROWN);
            isPressedThree = savedInstanceState.getBoolean(Constants.THREE_CROWN);
            isVisibleFilter = savedInstanceState.getBoolean(Constants.IS_VISIBLE_FILTER);
            isAdvancedMode = savedInstanceState.getBoolean(Constants.IS_ADVANCED_SHOW_LIST);
        }
    }

    private class RateOneClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (!isPressedOne) {
                setRateBg(view, R.drawable.rate_blue_bg);
            } else {
                setRateBg(view, R.drawable.rate_light_gray_bg);
            }
            listview.setEnabled(false);
            isPressedOne = true;
            updateFilterUI();
            listview.setEnabled(true);
        }
    }

    private class RateTwoClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (!isPressedTwo) {
                setRateBg(view, R.drawable.rate_blue_bg);
                isPressedTwo = true;
                updateFilterUI();
            } else {
                setRateBg(view, R.drawable.rate_light_gray_bg);
                isPressedTwo = false;
                updateFilterUI();
            }
        }
    }

    private class RateThreeClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (!isPressedThree) {
                setRateBg(view, R.drawable.rate_blue_bg);
                isPressedThree = true;
                updateFilterUI();
            } else {
                setRateBg(view, R.drawable.rate_light_gray_bg);
                isPressedThree = false;
                updateFilterUI();
            }
        }
    }

    private class SearchClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (!isVisibleET) {
                header.setVisibility(View.GONE);
                searchedText.setVisibility(View.VISIBLE);
                showKeyboard();
                isVisibleET = true;
            } else {
                header.setVisibility(View.VISIBLE);
                searchedText.setVisibility(View.GONE);
                hideKeyboardWithSearch(getActivity());
                isVisibleET = false;
            }
        }
    }

    private class LoadImgClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (!isVisibleFilter) {
                loadFilter.setVisibility(View.VISIBLE);
                FragmentActivity activity = getActivity();
                if (activity != null) {
                    loadImg.setColorFilter(ContextCompat.getColor(activity, R.color.base_blue_color));
                }
                adapter.setDisplayLoadProgressBarNoNotify(true);
                isVisibleFilter = true;
                updateFilterUI();
            } else {
                loadFilter.setVisibility(View.GONE);
                loadImg.setColorFilter(null);
                adapter.setDisplayLoadProgressBarNoNotify(false);
                adapter.setLoadProgressNotNotify(100);
                isVisibleFilter = false;
                updateFilterUI();
            }
        }
    }

    private class FilterTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            updateFilterUI();
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    private class FinishTypingListener implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                // the user is done typing.
                hideKeyboardWithSearch(getActivity());
                return true; // consume.

            } else
                return false;
        }
    }

    private class LoadSeekbarListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (adapter != null)
                adapter.setLoadProgressNotNotify(progress);

            postUpdateFilterUI();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // No-op
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // No-op
        }

        private void postUpdateFilterUI() {
            new Handler(Looper.getMainLooper()).post(() -> {
                updateFilterUI();
            });
        }
    }



    private class AdvancedStandardModeListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            FragmentActivity activity = getActivity();
            if (activity != null) {
                CommonUtils.hideKeyboard(activity);
            }

            toggleAdvancedMode();
        }
    }

    private void changeServer(Server serverToSelect) {
        vpnRepository.setServerTo(serverToSelect.getVpnServerId());
    }

    private void showDialogServerChangeSuccessfulDoYouWantToConnect() {
        Log.d(TAG, "showDialogServerChangeSuccessfulDoYouWantToConnect - start");

        serverChangeDialog = new Dialog(requireContext());
        serverChangeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        serverChangeDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        serverChangeDialog.setContentView(R.layout.dialog_server_upgrade);

        TextView question = serverChangeDialog.findViewById(R.id.attention_text);
        question.setText(R.string.server_change_successful_Do_you_want_to_connect_now);

        Button btnYes = serverChangeDialog.findViewById(R.id.btnOk);
        btnYes.setOnClickListener(v -> {
            serverChangeDialog.dismiss();
            requireActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

            Log.d(TAG, "showDialogServerChangeSuccessfulDoYouWantToConnect - user clicked yes, navigating to MainSectionFragment");
            selectTab(0);

            new Handler(Looper.getMainLooper()).post(() -> vpnConnectionManager.connect());
        });

        Button btnNo = serverChangeDialog.findViewById(R.id.btnNo);
        btnNo.setOnClickListener(v -> {
            Log.d(TAG, "showDialogServerChangeSuccessfulDoYouWantToConnect - user clicked no, dismissing dialog");
            serverChangeDialog.dismiss();
        });

        serverChangeDialog.setCancelable(false);
        serverChangeDialog.show();
    }


}
