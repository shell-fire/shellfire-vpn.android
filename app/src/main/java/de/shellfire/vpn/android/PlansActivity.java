package de.shellfire.vpn.android;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import org.greenrobot.eventbus.EventBus;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import de.shellfire.vpn.android.databinding.ActivityPlansBinding;

public class PlansActivity extends AppCompatActivity {

    private ActivityPlansBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlansBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        setUserPlan();

        binding.upgradeBtn.setOnClickListener(v -> onClickUpgradeButton());
        binding.backBtn.setOnClickListener(v -> finish());
    }

    private void setUserPlan() {
        VpnRepository vpnRepository = VpnRepository.getInstance(this);
        vpnRepository.getSelectedVpn().observe(this, selectedVpn -> {
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
        switch (serverType) {
            case Free:
                binding.star1.setVisibility(View.VISIBLE);
                break;
            case Premium:
                binding.star1.setVisibility(View.VISIBLE);
                binding.star2.setVisibility(View.VISIBLE);
                break;
            case PremiumPlus:
                binding.star1.setVisibility(View.VISIBLE);
                binding.star2.setVisibility(View.VISIBLE);
                binding.star3.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.about, menu);
        Objects.requireNonNull(getSupportActionBar()).setIcon(R.mipmap.ic_launcher);
        return true;
    }

    private void showPremiumLayout() {
        binding.premiumLayout.setVisibility(View.VISIBLE);
        binding.freeLayout.setVisibility(View.GONE);
    }

    private void showFreeLayout() {
        binding.premiumLayout.setVisibility(View.GONE);
        binding.freeLayout.setVisibility(View.VISIBLE);
    }

    public void onClickUpgradeButton() {
        UpgradeMessage msg = new UpgradeMessage();
        msg.setUpgrade(true);
        EventBus.getDefault().postSticky(msg);
        finish();
    }
}
