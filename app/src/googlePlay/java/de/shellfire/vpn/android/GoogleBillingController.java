package de.shellfire.vpn.android;

import android.content.Context;
import android.util.Log;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;

import java.util.ArrayList;
import java.util.List;

public class GoogleBillingController implements BillingController {
    private static final String TAG = "GoogleBillingController";
    private Context context;
    private BillingClient billingClient;
    private BillingListener billingListener;

    public GoogleBillingController(Context context) {
        this.context = context;
        billingClient = BillingClient.newBuilder(context)
                .setListener((billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                        for (com.android.billingclient.api.Purchase purchase : purchases) {
                            if (billingListener != null) {
                                billingListener.onPurchaseSuccess();
                            }
                        }
                    } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                        if (billingListener != null) {
                            billingListener.onPurchaseFailure("Purchase canceled by user");
                        }
                    } else {
                        if (billingListener != null) {
                            billingListener.onPurchaseFailure("Purchase failed: " + billingResult.getDebugMessage());
                        }
                    }
                })
                .enablePendingPurchases()
                .build();
    }

    @Override
    public void startBillingConnection() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing setup finished successfully");
                }
                updatePrices();
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.d(TAG, "Billing service disconnected");
            }
        });
    }

    @Override
    public void launchPurchaseFlow(boolean isSubscription, ServerType accountType, int billingPeriod) {
        List<String> skuList = new ArrayList<>();
        skuList.add("premium_plus_plan"); // Use your real SKU here.
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder()
                .setSkusList(skuList)
                .setType(isSubscription ? BillingClient.SkuType.SUBS : BillingClient.SkuType.INAPP);
        billingClient.querySkuDetailsAsync(params.build(), (billingResult, skuDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                for (SkuDetails skuDetails : skuDetailsList) {
                    BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(skuDetails)
                            .build();
                    billingClient.launchBillingFlow((android.app.Activity) context, flowParams);
                }
            } else {
                if (billingListener != null) {
                    billingListener.onPurchaseFailure("Failed to launch purchase flow: " + billingResult.getDebugMessage());
                }
            }
        });
    }

    @Override
    public void updatePrices() {
        List<QueryProductDetailsParams.Product> products = new ArrayList<>();
        products.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_plus_plan")
                .setProductType(BillingClient.ProductType.SUBS)
                .build());
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(products)
                .build();
        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK &&
                    productDetailsList != null && !productDetailsList.isEmpty()) {
                // Convert the product details into a PriceOverview object.
                PriceOverview priceOverview = new PriceOverview();
                // ... (populate priceOverview as needed) ...
                if (billingListener != null) {
                    billingListener.onPricesUpdated(priceOverview);
                }
            } else {
                Log.e(TAG, "Failed to fetch product details: " + billingResult.getDebugMessage());
            }
        });
    }

    @Override
    public void setBillingListener(BillingListener listener) {
        this.billingListener = listener;
    }
}
