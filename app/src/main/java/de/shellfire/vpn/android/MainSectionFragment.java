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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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
 * Created by Alina on 05.01.2018.
 */

public class MainSectionFragment extends Fragment implements OnMapReadyCallback {

    /**
     * The fragment argument representing the section number for this fragment.
     */
    public static final String ARG_USER_NAME = "user_name";
    public static final int DELAY = 5;
    private static final String TAG = "MainSctnFrgm";
    private static final String SIMPLE_CONNECTION_STATUS = "SimpleConnectionStatus";
    public static SimpleConnectionStatus connectionStatus;
    public GoogleMap googleMap;
    protected static IOpenVPNServiceInternal mService = null;
    private final Map<String, String> cityMap = new LinkedHashMap<>();
    private final Map<String, String> countryMap = new LinkedHashMap<>();
    private View rootView;
    private SharedViewModel sharedViewModel;
    private ProgressBar progressBar;

    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = IOpenVPNServiceInternal.Stub.asInterface(service);
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    private Button buttonConnect;
    private TextView stateView;
    private RelativeLayout statusContainer;
    private Marker marker;
    private PicassoCache picassoCache;
    private String packageName;
    private VpnConnectionManager vpnConnectionManager;
    private VpnRepository vpnRepository;
    private ImageView  serverImageView;
    private ShimmerFrameLayout shimmerFrameLayout;
    private View stars_container;


    public static SimpleConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public static void setConnectionStatus(SimpleConnectionStatus connectionStatus) {
        MainSectionFragment.connectionStatus = connectionStatus;
    }

    public void onAttach(@NotNull Activity activity) {
        super.onAttach(activity);
        packageName = activity.getPackageName();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getActivity() != null) {
            getActivity().unbindService(mConnection);
        }

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

    void setServerValues(View view, Server server) {
        Log.d(TAG, "setServerValues called, server= " + server.toString());

        view.setVisibility(View.VISIBLE);
        stars_container.setVisibility(View.INVISIBLE);
        serverImageView.setImageResource(R.drawable.sky_blue);
        // assign country-flag
        int countryFlagImageResId = CountryUtils.getCountryFlagImageResId(server.getCountryEnum());
        ImageView countryFlagImageView = view.findViewById(R.id.flag_img);
        countryFlagImageView.setImageResource(countryFlagImageResId);

        // assign City + Country
        TextView cityView = view.findViewById(R.id.server_city);
        cityView.setText(server.getCity());

        TextView countryView = view.findViewById(R.id.server_country);
        countryView.setText(server.getCountryPrint());


        // display correct number of stars
        int numStars;
        if (server.getServerType() == ServerType.Free) {
            numStars = 1;
        } else if (server.getServerType() == ServerType.Premium) {
            numStars = 2;
        } else {
            numStars = 3;
        }

        for (int i = 3; i >= 1; i--) {
            int id;
            if (i == 1) {
                id = R.id.star_1;
            } else if (i == 2) {
                id = R.id.star_2;
            } else {
                id = R.id.star_3;
            }

            ImageView starView = view.findViewById(id);

            if (i > numStars) {
                starView.setVisibility(android.view.View.GONE);
            } else {
                starView.setVisibility(android.view.View.VISIBLE);
            }
        }

        shimmerFrameLayout.showShimmer(true);
        picassoCache.getAndLoadInto(server, serverImageView, new Callback() {
            @Override
            public void onSuccess() {
                // Stop the shimmer effect and hide the placeholder
                Log.d(TAG, "picassoCache.getAndLoadInto.onSuccess");
                serverImageView.setVisibility(View.VISIBLE);
                stars_container.setVisibility(View.VISIBLE);
                shimmerFrameLayout.hideShimmer();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "picassoCache.getAndLoadInto.onError", e);
                // Stop the shimmer effect even if there's an error
                shimmerFrameLayout.hideShimmer();
                shimmerFrameLayout.setVisibility(View.INVISIBLE);
            }
        });

        setCoordinatesOnMap(server);

    }


    public void onCreate(Bundle save) {
        super.onCreate(save);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        // Initialize SharedViewModel
        sharedViewModel = new ViewModelProvider(requireActivity(), new SharedViewModelFactory(
                requireActivity()
        )).get(SharedViewModel.class);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");

        // Reuse the retained view if it exists
        if (rootView != null) {
            Log.d(TAG, "Reusing the existing rootView");
            return rootView;
        }

        rootView = inflater.inflate(R.layout.fragment_status, container, false);
        setRetainInstance(true);

        if (savedInstanceState != null) {
            connectionStatus = (SimpleConnectionStatus) savedInstanceState
                    .getSerializable("SIMPLE_CONNECTION_STATUS");
        }

        View mainLayout = rootView.findViewById(R.id.mainLayout);
        mainLayout.setVisibility(View.VISIBLE);

        progressBar = rootView.findViewById(R.id.progressBar);
        buttonConnect = rootView.findViewById(R.id.buttonConnect);
        stateView = rootView.findViewById(R.id.connectionState);
        statusContainer = rootView.findViewById(R.id.status_container);
        shimmerFrameLayout = rootView.findViewById(R.id.shimmer_view_container);
        // Start the shimmer effect
        shimmerFrameLayout.showShimmer(true);
        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.showShimmer(true);
        serverImageView = rootView.findViewById(R.id.server_img);
        stars_container = rootView.findViewById(R.id.stars_container);

        buttonConnect.setOnClickListener(this::onClickConnect);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "Map Fragment is null");
        }



        return rootView;
    }


    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("MainSectionFragment", "onViewCreated called");

        // Initialize PicassoCache
        picassoCache = PicassoCache.getInstance(requireContext());

        // Get VpnConnectionManager instance
        vpnConnectionManager = VpnConnectionManager.getInstance(requireContext());

        // Get VpnRepository instance
        vpnRepository = VpnRepository.getInstance(requireContext());

        // Observe connection status
        vpnConnectionManager.getConnectionStatus().observe(getViewLifecycleOwner(), this::setSimpleConnectionStatus);

        // Observe selected server and update UI
        vpnRepository.getSelectedServer().observe(getViewLifecycleOwner(), server -> {
            if (server != null) {
                setServerValues(view, server);
            }
        });
    }

    public void onClickConnect(View view) {
        Log.d(TAG, "called onClickConnect() ");
        VpnConnectionManager vpnConnectionManager = VpnConnectionManager.getInstance(requireContext());
        SimpleConnectionStatus status = vpnConnectionManager.getConnectionStatus().getValue();
        Log.d(TAG, "onClickConnect() - status: " + status);
        if (null == status || SimpleConnectionStatus.Disconnected == status) {
            connect();
        } else if (SimpleConnectionStatus.Connected == status || SimpleConnectionStatus.Connecting == status) {
            disconnect();
        }
    }

    private void connect() {
        vpnConnectionManager.connect();
    }

    protected void disconnect() {
        vpnConnectionManager.disconnect();

        AppRater appRater = new AppRater(requireContext());
        appRater.logAction();
        appRater.triggerReview();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called");
        vpnConnectionManager.getConnectionStatus().removeObservers(getViewLifecycleOwner());
        vpnRepository.getSelectedServer().removeObservers(getViewLifecycleOwner());
    }
    private void setSimpleConnectionStatus(final SimpleConnectionStatus conStatus) {
        Log.d(TAG, "setSimpleConnectionStatus called, new status: " + conStatus);
        connectionStatus = conStatus;
        // don't remove {runOnUiThread} because when it loses the Internet app will be crashed
        final FragmentActivity activity = getActivity();
        if (activity != null) {

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
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
                        if (googleMap != null) {
                            Log.d(TAG, "run: googleMap != null, setMapStyle to green");
                            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.style_json_green));

                            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        } else {
                            Log.d(TAG, "run: googleMap == null");
                        }
                    } else if (conStatus == SimpleConnectionStatus.Disconnected) {
                        progressBar.setVisibility(View.GONE);
                        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                        buttonConnect.setText(R.string.connect);
                        buttonConnect.setEnabled(true);
                        buttonConnect.setBackground(ContextCompat.getDrawable(activity, R.drawable.connect_btn_pressed));

                        stateView.setText(R.string.disconnected);
                        statusContainer.setBackgroundColor(ContextCompat.getColor(activity, R.color.red_disconnect));

                        if (googleMap != null) {
                            Log.d(TAG, "run: googleMap != null, setMapStyle to default");
                            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(activity, R.raw.style_json));
                        } else {
                            Log.d(TAG, "run: googleMap == null");
                        }
                        if (mService != null) {
                            try {
                                mService.stopVPN(false);
                            } catch (RemoteException e) {
                                VpnStatus.logException(e);
                            }
                        }
                    }
                }
            }
            );
        }
    }


    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(SIMPLE_CONNECTION_STATUS, connectionStatus);
    }


    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        if (connectionStatus == SimpleConnectionStatus.Connected) {
            if (getActivity() != null) {
                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getActivity(), R.raw.style_json_green));
            }
        } else {
            if (getActivity() != null) {
                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getActivity(), R.raw.style_json));
            }
        }
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setZoomGesturesEnabled(false);
        googleMap.getUiSettings().setScrollGesturesEnabled(false);
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(3.0f));
        googleMap.getUiSettings().setMapToolbarEnabled(false);

    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

    }

    public void setCoordinatesOnMap(Server server) {
        if (googleMap != null) {
            BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.marker);
            if (server != null) {
                double latitude = server.getLatitude();
                double longitude = server.getLongitude();
                LatLng newCoordinates = new LatLng(latitude, longitude);

                if (marker != null) {
                    marker.remove();
                }
                marker = googleMap.addMarker(new MarkerOptions().position(newCoordinates).icon(icon));
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(newCoordinates));
            }
        }
    }

}
