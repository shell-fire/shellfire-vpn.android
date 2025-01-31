package de.shellfire.vpn.android;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SubscriptionViewModel extends ViewModel {
    private final MutableLiveData<PriceOverview> subscriptionData = new MutableLiveData<>();

    public LiveData<PriceOverview> getSubscriptionData() {
        return subscriptionData;
    }

    public void setSubscriptionData(PriceOverview data) {
        Log.d("SubscriptionViewModel", "setSubscriptionData called with data: " + data);
        subscriptionData.postValue(data);
    }
}

