package de.shellfire.vpn.android;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class FilterViewModel extends ViewModel {
    private final ServerFilterHelper filterHelper;

    public FilterViewModel(Context context) {
        filterHelper = new ServerFilterHelper(VpnRepository.getInstance(context));
    }

    public LiveData<List<Server>> getFilteredServers(LiveData<List<Server>> serversLiveData, boolean isPressedOne, boolean isPressedTwo,
                                                     boolean isPressedThree, int load, boolean isAdvancedList, String searchText) {
        return filterHelper.getFilteredServers(serversLiveData, isPressedOne, isPressedTwo, isPressedThree, load, isAdvancedList, searchText);
    }
}
