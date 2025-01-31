package de.shellfire.vpn.android;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class SelectVpnViewModel extends ViewModel {

    private final LiveData<List<Vpn>> vpnListLiveData;

    public SelectVpnViewModel() {
        VpnRepository vpnRepository = VpnRepository.getInstance(ShellfireApplication.getContext());
        vpnListLiveData = vpnRepository.getVpnList();
    }

    public LiveData<List<Vpn>> getVpnListLiveData() {
        return vpnListLiveData;
    }
}
