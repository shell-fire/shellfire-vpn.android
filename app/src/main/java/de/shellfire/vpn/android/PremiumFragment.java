package de.shellfire.vpn.android;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import org.jetbrains.annotations.NotNull;

import de.shellfire.vpn.android.viewmodel.SharedViewModel;
import de.shellfire.vpn.android.viewmodel.SharedViewModelFactory;

public class PremiumFragment extends Fragment {

    private static final String TAG = "PremiumFragment";
    private MainBaseActivity mainBaseActivity;
    private SharedViewModel sharedViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        setRetainInstance(true);

        // Initialize SharedViewModel
        sharedViewModel = new ViewModelProvider(requireActivity(), new SharedViewModelFactory(
                requireActivity()
        )).get(SharedViewModel.class);
    }

    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        // Load PremiumPlusPlanFragment
        if (savedInstanceState == null) {
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, PremiumPlusPlanFragment.newInstance());
            transaction.commit();
        }
    }

    @Override
    public void onAttach(@NotNull Activity activity) {
        super.onAttach(activity);
        this.mainBaseActivity = (MainBaseActivity) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mainBaseActivity = null;
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        return inflater.inflate(R.layout.fragment_billing, container, false);
    }
}