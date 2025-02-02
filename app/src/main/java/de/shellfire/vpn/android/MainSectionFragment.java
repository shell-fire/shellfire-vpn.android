package de.shellfire.vpn.android;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.squareup.picasso.Callback;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

import de.shellfire.vpn.android.openvpn.IOpenVPNServiceInternal;
import de.shellfire.vpn.android.openvpn.OpenVPNService;
import de.shellfire.vpn.android.openvpn.VpnStatus;
import de.shellfire.vpn.android.utils.CountryUtils;
import de.shellfire.vpn.android.viewmodel.SharedViewModel;
import de.shellfire.vpn.android.viewmodel.SharedViewModelFactory;

/**
 * MainSectionFragment – Common functionality for both builds.
 * All map‐related functionality is delegated to a MapController.
 */
public class MainSectionFragment extends Fragment {

    public static final String ARG_USER_NAME = "user_name";
    public static final int DELAY = 5;
    private static final String TAG = "MainSectionFragment";
    public static SimpleConnectionStatus connectionStatus;
    protected static IOpenVPNServiceInternal mService = null;
    private final Map<String, String> cityMap = new LinkedHashMap<>();
    private final Map<String, String> countryMap = new LinkedHashMap<>();
    private View rootView;
    private SharedViewModel sharedViewModel;
    private ProgressBar progressBar;
    private Button buttonConnect;
    private TextView stateView;
    private RelativeLayout statusContainer;
    private ImageView serverImageView;
    private ShimmerFrameLayout shimmerFrameLayout;

    // Delegate all map operations to this instance.
    private MapController mapController;

    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = IOpenVPNServiceInternal.Stub.asInterface(service);
        }
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    @Override
    public void onAttach(@NotNull Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getActivity(), OpenVPNService.class);
        intent.setAction(OpenVPNService.START_SERVICE);
        if (getActivity() != null) {
            getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getActivity() != null) {
            getActivity().unbindService(mConnection);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        sharedViewModel = new ViewModelProvider(requireActivity(), new SharedViewModelFactory(requireActivity()))
                .get(SharedViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        if (rootView != null) {
            Log.d(TAG, "Reusing the existing rootView");
            return rootView;
        }
        rootView = inflater.inflate(R.layout.fragment_status, container, false);

        View detailsView = rootView.findViewById(R.id.details);
        View mapContainerView = rootView.findViewById(R.id.map_container);
        if (getResources().getBoolean(R.bool.isFdroidBuild)) {
            ViewGroup.LayoutParams params = detailsView.getLayoutParams();
            if (params instanceof LinearLayout.LayoutParams) {
                ((LinearLayout.LayoutParams) params).weight = 0.75f;
                detailsView.setLayoutParams(params);
            }
            ViewGroup.LayoutParams paramsMap = mapContainerView.getLayoutParams();
            if (paramsMap instanceof LinearLayout.LayoutParams) {
                ((LinearLayout.LayoutParams) paramsMap).weight = 0.15f;
                mapContainerView.setLayoutParams(paramsMap);
            }
        }

        setRetainInstance(true);

        progressBar = rootView.findViewById(R.id.progressBar);
        buttonConnect = rootView.findViewById(R.id.buttonConnect);
        stateView = rootView.findViewById(R.id.connectionState);
        statusContainer = rootView.findViewById(R.id.status_container);
        shimmerFrameLayout = rootView.findViewById(R.id.shimmer_view_container);
        serverImageView = rootView.findViewById(R.id.server_img);

        buttonConnect.setOnClickListener(this::onClickConnect);

        // Create the MapController instance via the flavor-specific factory.
        mapController = MapControllerFactory.create(this);
        mapController.initialize();

        return rootView;
    }

    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize other components (e.g. PicassoCache, VpnConnectionManager, etc.)
        VpnConnectionManager.getInstance(requireContext()).getConnectionStatus().observe(getViewLifecycleOwner(), this::setSimpleConnectionStatus);
        VpnRepository.getInstance(requireContext()).getSelectedServer().observe(getViewLifecycleOwner(), server -> {
            if (server != null) {
                setServerValues(view, server);
            }
        });
    }

    void setServerValues(View view, Server server) {
        Log.d(TAG, "setServerValues called, server= " + server.toString());
        view.setVisibility(View.VISIBLE);
        serverImageView.setImageResource(R.drawable.sky_blue);

        int countryFlagImageResId = CountryUtils.getCountryFlagImageResId(server.getCountryEnum());
        ImageView countryFlagImageView = view.findViewById(R.id.flag_img);
        countryFlagImageView.setImageResource(countryFlagImageResId);

        TextView cityView = view.findViewById(R.id.server_city);
        cityView.setText(server.getCity());

        TextView countryView = view.findViewById(R.id.server_country);
        countryView.setText(server.getCountryPrint());

        // Load server image via PicassoCache.
        PicassoCache.getInstance(requireContext()).getAndLoadInto(server, serverImageView, new Callback() {
            @Override
            public void onSuccess() {
                serverImageView.setVisibility(View.VISIBLE);
                shimmerFrameLayout.hideShimmer();
            }
            @Override
            public void onError(Exception e) {
                shimmerFrameLayout.hideShimmer();
            }
        });

        // Delegate map update (for example, to show the server location).
        if (mapController != null) {
            mapController.setCoordinates(server.getLatitude(), server.getLongitude());
        }
    }

    public void onClickConnect(View view) {
        Log.d(TAG, "onClickConnect called");
        SimpleConnectionStatus status = VpnConnectionManager.getInstance(requireContext()).getConnectionStatus().getValue();
        if (status == null || status == SimpleConnectionStatus.Disconnected) {
            connect();
        } else if (status == SimpleConnectionStatus.Connected || status == SimpleConnectionStatus.Connecting) {
            disconnect();
        }
    }

    private void connect() {
        VpnConnectionManager.getInstance(requireContext()).connect();
    }

    private void disconnect() {
        VpnConnectionManager.getInstance(requireContext()).disconnect();
        AppRater appRater = new AppRater(requireContext());
        appRater.logAction();
        appRater.triggerReview();
    }
    private void setSimpleConnectionStatus(final SimpleConnectionStatus conStatus) {
        Log.d(TAG, "setSimpleConnectionStatus called, new status: " + conStatus);
        connectionStatus = conStatus;
        FragmentActivity activity = getActivity();

        if (activity != null) {
            activity.runOnUiThread(() -> {
                if (conStatus == SimpleConnectionStatus.Connecting) {
                    buttonConnect.setEnabled(true);
                    progressBar.setVisibility(View.VISIBLE);
                    buttonConnect.setText(R.string.abort);
                    buttonConnect.setBackground(ContextCompat.getDrawable(activity, R.drawable.connect_btn_unpressed));
                    stateView.setText(R.string.connecting);

                } else if (conStatus == SimpleConnectionStatus.Connected) {
                    buttonConnect.setText(R.string.disconnect);
                    buttonConnect.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    buttonConnect.setBackground(ContextCompat.getDrawable(activity, R.drawable.connect_btn_unpressed));
                    stateView.setText(R.string.connected);
                    statusContainer.setBackgroundColor(ContextCompat.getColor(activity, R.color.green_connect));

                    if (mapController != null) {
                        Log.d(TAG, "Updating map style to green for connected state");
                        mapController.updateMapStyle(true);
                    } else {
                        Log.d(TAG, "MapController is null");
                    }

                    activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                } else if (conStatus == SimpleConnectionStatus.Disconnected) {
                    progressBar.setVisibility(View.GONE);
                    activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                    buttonConnect.setText(R.string.connect);
                    buttonConnect.setEnabled(true);
                    buttonConnect.setBackground(ContextCompat.getDrawable(activity, R.drawable.connect_btn_pressed));

                    stateView.setText(R.string.disconnected);
                    statusContainer.setBackgroundColor(ContextCompat.getColor(activity, R.color.red_disconnect));

                    if (mapController != null) {
                        Log.d(TAG, "Updating map style to default for disconnected state");
                        mapController.updateMapStyle(false);
                    } else {
                        Log.d(TAG, "MapController is null");
                    }

                    if (mService != null) {
                        try {
                            mService.stopVPN(false);
                        } catch (RemoteException e) {
                            VpnStatus.logException(e);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("SIMPLE_CONNECTION_STATUS", connectionStatus);
    }
}
