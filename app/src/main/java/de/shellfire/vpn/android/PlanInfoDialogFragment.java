package de.shellfire.vpn.android;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import de.shellfire.vpn.android.databinding.ActivityPlansBinding;

public class PlanInfoDialogFragment extends DialogFragment {

    private ActivityPlansBinding binding;

    public static PlanInfoDialogFragment newInstance() {
        return new PlanInfoDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ActivityPlansBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up the plan details
        setUserPlan();

        // Hide the upgrade button and back button to make it informational-only
        binding.upgradeBtn.setVisibility(View.GONE);
        binding.backBtn.setVisibility(View.GONE);
    }

    private void setUserPlan() {
        VpnRepository vpnRepository = VpnRepository.getInstance(requireContext());
        vpnRepository.getSelectedVpn().observe(getViewLifecycleOwner(), selectedVpn -> {
            if (selectedVpn != null) {
                if (selectedVpn.getAccountType() != null) {
                    if (selectedVpn.getAccountType() == ServerType.PremiumPlus) {
                        binding.planUser.setText(ServerType.ShellfireVPN.toString());
                    } else {
                        binding.planUser.setText(selectedVpn.getAccountType().toString());
                    }
                }
                if (selectedVpn.getPremiumUntil() != null) {
                    Date paidUntil = selectedVpn.getPremiumUntil();
                    DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
                    String paidUntilStr = dateFormat.format(paidUntil);
                    binding.paidUntilDate.setText(paidUntilStr);
                }
                setCrowns(selectedVpn);

                if (selectedVpn.getAccountType() != null) {
                    ServerType accountType = selectedVpn.getAccountType();
                    if (accountType == ServerType.Free) {
                        showFreeLayout();
                    } else {
                        showPremiumLayout();
                    }
                }
            }

        });
    }

    private void setCrowns(Vpn selectedVpn) {
        ServerType serverType = selectedVpn.getAccountType();
        binding.star1.setVisibility(View.GONE);
        binding.star2.setVisibility(View.GONE);
        binding.star3.setVisibility(View.GONE);

        if (serverType == ServerType.PremiumPlus) {
            binding.star3.setVisibility(View.VISIBLE);
        }
        if (serverType == ServerType.Premium || serverType == ServerType.PremiumPlus) {
            binding.star2.setVisibility(View.VISIBLE);
        }
        if (serverType == ServerType.Premium || serverType == ServerType.Free || serverType == ServerType.PremiumPlus) {
            binding.star1.setVisibility(View.VISIBLE);
        }
    }

    private void showPremiumLayout() {
        binding.premiumLayout.setVisibility(View.VISIBLE);
        binding.freeLayout.setVisibility(View.GONE);
    }

    private void showFreeLayout() {
        binding.premiumLayout.setVisibility(View.GONE);
        binding.freeLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        // Make the dialog full screen
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        return dialog;
    }
}
