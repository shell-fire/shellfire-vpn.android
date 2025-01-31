package de.shellfire.vpn.android;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.jetbrains.annotations.NotNull;

import de.shellfire.vpn.android.viewmodel.SharedViewModel;

public class MainAndServerSelectionFragment extends Fragment {

    private MainSectionFragment mainSectionFragment;
    private TabletServerListFragment serverSelectSectionFragment;
    private static final String TAG = "MainAndServerSelectFrm";
    private SharedViewModel sharedViewModel;
    private LinearLayout container;
    private FrameLayout mainSectionContainer;
    private FrameLayout serverSelectSectionContainer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_and_serverselect_view, container, false);
        this.container = view.findViewById(R.id.container);
        this.mainSectionContainer = view.findViewById(R.id.mainSectionContainer);
        this.serverSelectSectionContainer = view.findViewById(R.id.serverSelectSectionContainer);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState == null) {
            // First time initialization
            mainSectionFragment = new MainSectionFragment();
            serverSelectSectionFragment = new TabletServerListFragment();

            // Add fragments programmatically
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.mainSectionContainer, mainSectionFragment, "mainSectionFragment")
                    .replace(R.id.serverSelectSectionContainer, serverSelectSectionFragment, "serverSelectSectionFragment")
                    .commit();
        } else {
            // Find existing fragments
            mainSectionFragment = (MainSectionFragment) getChildFragmentManager().findFragmentByTag("mainSectionFragment");
            serverSelectSectionFragment = (TabletServerListFragment) getChildFragmentManager().findFragmentByTag("serverSelectSectionFragment");

            if (mainSectionFragment == null) {
                mainSectionFragment = new MainSectionFragment();
            }

            if (serverSelectSectionFragment == null) {
                serverSelectSectionFragment = new TabletServerListFragment();
            }

            getChildFragmentManager().beginTransaction()
                    .replace(R.id.mainSectionContainer, mainSectionFragment, "mainSectionFragment")
                    .replace(R.id.serverSelectSectionContainer, serverSelectSectionFragment, "serverSelectSectionFragment")
                    .commit();
        }

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // Adjust layout based on the current orientation
        adjustLayout(getResources().getConfiguration().orientation);
    }

    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Change the orientation of the container based on the new configuration
        adjustLayout(newConfig.orientation);
    }

    private void adjustLayout(int orientation) {
        LinearLayout.LayoutParams mainSectionParams = (LinearLayout.LayoutParams) mainSectionContainer.getLayoutParams();
        LinearLayout.LayoutParams serverSelectParams = (LinearLayout.LayoutParams) serverSelectSectionContainer.getLayoutParams();

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            container.setOrientation(LinearLayout.HORIZONTAL);
            mainSectionParams.width = 0;
            mainSectionParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            mainSectionParams.weight = 1;
            serverSelectParams.width = 0;
            serverSelectParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            serverSelectParams.weight = 1;
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            container.setOrientation(LinearLayout.VERTICAL);
            mainSectionParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            mainSectionParams.height = 0;
            mainSectionParams.weight = 1;
            serverSelectParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            serverSelectParams.height = 0;
            serverSelectParams.weight = 1;
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            mainSectionContainer.setLayoutParams(mainSectionParams);
            serverSelectSectionContainer.setLayoutParams(serverSelectParams);
        });

    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && getServerSelectSectionFragment() != null) {
            getServerSelectSectionFragment().updateServerSelect();
        }
    }

    public TabletServerListFragment getServerSelectSectionFragment() {
        return serverSelectSectionFragment;
    }
}
