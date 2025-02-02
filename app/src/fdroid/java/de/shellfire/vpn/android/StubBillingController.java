package de.shellfire.vpn.android;

import android.content.Context;
import android.util.Log;

public class StubBillingController implements BillingController {
    private static final String TAG = "StubBillingController";
    private BillingListener billingListener;
    private Context context;

    public StubBillingController(Context context) {
        this.context = context;
    }

    @Override
    public void startBillingConnection() {
        Log.d(TAG, "startBillingConnection called: Billing not available in FDroid build.");
    }

    @Override
    public void launchPurchaseFlow(boolean isSubscription, ServerType accountType, int billingPeriod) {
        if (billingListener != null) {
            billingListener.onPurchaseFailure("In FDroid version, please visit our website for current offers.");
        }
    }

    @Override
    public void updatePrices() {
        // In FDroid, we don’t display dynamic pricing.
        if (billingListener != null) {
            billingListener.onPricesUpdated(new PriceOverview()); // or simply do nothing
        }
    }

    @Override
    public void setBillingListener(BillingListener listener) {
        this.billingListener = listener;
    }
}
