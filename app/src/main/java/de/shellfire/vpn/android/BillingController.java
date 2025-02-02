package de.shellfire.vpn.android;

/**
 * Abstraction for billing operations.
 */
public interface BillingController {
    /**
     * Establish a connection with the billing service.
     */
    void startBillingConnection();

    /**
     * Launch the purchase flow.
     *
     * @param isSubscription whether the purchase is a subscription
     * @param accountType the account type (e.g. PremiumPlus)
     * @param billingPeriod an integer representing the billing period (e.g. 12 for yearly)
     */
    void launchPurchaseFlow(boolean isSubscription, ServerType accountType, int billingPeriod);

    /**
     * Query pricing and update the UI.
     */
    void updatePrices();

    /**
     * Set a listener to receive billing events.
     */
    void setBillingListener(BillingListener listener);
}
