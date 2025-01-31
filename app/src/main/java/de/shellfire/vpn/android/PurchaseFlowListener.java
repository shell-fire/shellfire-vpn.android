package de.shellfire.vpn.android;

public interface PurchaseFlowListener {
    void startPurchaseFlow(boolean isSubscription, ServerType accountType, int billingPeriod);
}
