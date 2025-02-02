package de.shellfire.vpn.android;

/**
 * Listener for billing events.
 */
public interface BillingListener {
    /**
     * Called when updated pricing info is available.
     */
    void onPricesUpdated(PriceOverview priceOverview);

    /**
     * Called when a purchase completes successfully.
     */
    void onPurchaseSuccess();

    /**
     * Called when a purchase fails.
     */
    void onPurchaseFailure(String errorMessage);
}
