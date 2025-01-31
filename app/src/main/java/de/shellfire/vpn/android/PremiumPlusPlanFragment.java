package de.shellfire.vpn.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.facebook.shimmer.ShimmerFrameLayout;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.shellfire.vpn.android.auth.AuthRepository;
import de.shellfire.vpn.android.databinding.ActivityPlansBinding;
import de.shellfire.vpn.android.utils.CommonUtils;
import de.shellfire.vpn.android.utils.Util;
import de.shellfire.vpn.android.viewmodel.SharedViewModel;
import de.shellfire.vpn.android.viewmodel.SharedViewModelFactory;
import de.shellfire.vpn.android.webservice.model.UpgradeVpnResponse;

public class PremiumPlusPlanFragment extends DialogFragment implements PurchasesUpdatedListener, PurchaseFlowListener {
    private AuthRepository authRepository;
    private boolean isDiscountEligible = true;
    private MainBaseActivity mActivity;

    private final String TAG = "PremiumPlusPlanFragment";
    private BillingClient billingClient;
    private BillingRepository billingRepository;
    private VpnRepository vpnRepository;
    private View currentView;

    private DataRepository dataRepository;


    private SubscriptionViewModel viewModel;

    public static PremiumPlusPlanFragment newInstance() {
        return new PremiumPlusPlanFragment();
    }

    @Override
    public void onAttach(@NotNull Activity activity) {
        super.onAttach(activity);
        mActivity = (MainBaseActivity) activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        setRetainInstance(true);

        // Initialize SharedViewModel
        SharedViewModel sharedViewModel = new ViewModelProvider(requireActivity(), new SharedViewModelFactory(
                requireActivity()
        )).get(SharedViewModel.class);

        // Initialize BillingClient
        billingClient = BillingClient.newBuilder(requireActivity())
                .setListener(this)
                .enablePendingPurchases()
                .build();
        startBillingConnection();

        billingRepository = BillingRepository.getInstance(getContext());
        vpnRepository = VpnRepository.getInstance(getContext());
        authRepository = AuthRepository.getInstance(getContext());
        dataRepository = DataRepository.getInstance(getContext());

    }

    private void startBillingConnection() {
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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.screen_default_subscription, container, false);
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        currentView = view; // Initialize with the default view
        initListeners(currentView);

        viewModel = new ViewModelProvider(requireActivity()).get(SubscriptionViewModel.class);

        // Observe VPN changes to dynamically update layout
        VpnRepository vpnRepository = VpnRepository.getInstance(getContext());
        vpnRepository.getSelectedVpn().observe(getViewLifecycleOwner(), selectedVpn -> {
            Log.d(TAG, "VPN account type changed: " + (selectedVpn != null ? selectedVpn.getAccountType() : "null"));
            if (selectedVpn != null && selectedVpn.getAccountType() != null) {
                clearObservers(); // Clear any existing observers
                if (selectedVpn.getAccountType() != ServerType.Free) {
                    Log.d(TAG, "VPN account type is not free, showing plan info layout");
                    replaceWithPlanInfoLayout();
                } else {
                    Log.d(TAG, "VPN account type is free, reverting to default subscription layout");
                    revertToDefaultSubscriptionLayout();
                }
            }
        });

        // Initialize subscription data observer for the default layout
        initSubscriptionDataObserver(currentView);
    }

    private void replaceWithPlanInfoLayout() {
        ViewGroup parent = (ViewGroup) currentView.getParent();
        if (parent != null) {
            parent.removeAllViews(); // Remove current layout

            LayoutInflater inflater = LayoutInflater.from(getContext());
            currentView = inflater.inflate(R.layout.activity_plans, parent, false); // Use PlanInfo layout

            parent.addView(currentView);

            // Set up PlanInfo layout
            setupPlanInfoLayout(currentView);

            // Reinitialize subscription data observer for the new layout
            initSubscriptionDataObserver(currentView);
        }
    }

    private void revertToDefaultSubscriptionLayout() {
        ViewGroup parent = (ViewGroup) currentView.getParent();
        if (parent != null) {
            parent.removeAllViews(); // Remove current layout

            LayoutInflater inflater = LayoutInflater.from(getContext());
            currentView = inflater.inflate(R.layout.screen_default_subscription, parent, false); // Default layout

            parent.addView(currentView);

            // Initialize listeners for the default layout
            initListeners(currentView);

            // Reinitialize subscription data observer for the reverted layout
            initSubscriptionDataObserver(currentView);
        }
    }


    // Reinitialize subscription data observer for the provided layout
    private void initSubscriptionDataObserver(View view) {
        viewModel.getSubscriptionData().observe(getViewLifecycleOwner(), data -> {
            Log.d(TAG, "Subscription data observed: " + data);
            updateSubscriptionUI(view, data);
        });
    }


    private void clearObservers() {
        if (viewModel != null) {
            viewModel.getSubscriptionData().removeObservers(getViewLifecycleOwner());
        }
    }

    // Update subscription UI for the current layout
    private void updateSubscriptionUI(View view, PriceOverview data) {
        if (view.findViewById(R.id.banner_text) == null) {
            Log.w(TAG, "updateSubscriptionUI: Views not found, skipping update.");
            return; // Skip if the expected views are not present in the current layout
        }

        TextView tryOutView = view.findViewById(R.id.banner_text);
        TextView thenGetDiscountView = view.findViewById(R.id.then_get_discount);
        TextView forTheFirstTwoYears = view.findViewById(R.id.for_the_first_two_years);
        TextView yearlyPerMonthView = view.findViewById(R.id.plan_yearly_price_per_month);
        TextView yearlyPerYearView = view.findViewById(R.id.plan_yearly_price_per_year);
        TextView trialInfoView = view.findViewById(R.id.trial_info);
        TextView trialLinkView = view.findViewById(R.id.trial_link);
        TextView ctaButtonView = view.findViewById(R.id.cta_button);

        if (data != null) {
            if (!data.hasDiscountForYearly()) {
                trialInfoView.setVisibility(View.GONE);
                trialLinkView.setVisibility(View.GONE);
                ctaButtonView.setText(R.string.start_now);
            } else {
                trialInfoView.setVisibility(View.VISIBLE);
                trialLinkView.setVisibility(View.VISIBLE);
                ctaButtonView.setText(R.string.try_now_for_free);
            }

            updateTryOutText(tryOutView, data.getFormattedDiscountForYearly());
            updateThenGetDiscountText(thenGetDiscountView, forTheFirstTwoYears, data.getFormattedDiscountForYearly());
            updateYearlyPricePerMonth(yearlyPerMonthView, data.getYearlyPricePerMonthDiscounted().getFormattedPrice());
            updateYearlyPricePerYear(yearlyPerYearView, data.getYearlyPriceDiscounted().getFormattedPrice());
        }

    }


    // Setup PlanInfo layout for premium users
    private void setupPlanInfoLayout(View newView) {
        ActivityPlansBinding binding = ActivityPlansBinding.bind(newView);
        setUserPlan(binding);
        binding.backBtn.setVisibility(View.GONE);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.BOTTOM;
        params.setMargins(0, 0, 0, getNavigationBarHeight());

    }

    // Retrieve navigation bar height for accurate placement
    private int getNavigationBarHeight() {
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return resourceId > 0 ? getResources().getDimensionPixelSize(resourceId) : 0;
    }

    // Set up user plan information
    private void setUserPlan(ActivityPlansBinding binding) {
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
                setCrowns(binding, selectedVpn);

                if (selectedVpn.getAccountType() != null) {
                    ServerType accountType = selectedVpn.getAccountType();
                    if (accountType == ServerType.Free) {
                        showFreeLayout(binding);
                    } else {
                        showPremiumLayout(binding);
                    }
                }
            }
        });
    }

    // Set up crown icons based on the account type
    private void setCrowns(ActivityPlansBinding binding, Vpn selectedVpn) {
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

    // Show/hide layouts based on the account type
    private void showPremiumLayout(ActivityPlansBinding binding) {
        binding.premiumLayout.setVisibility(View.VISIBLE);
        binding.freeLayout.setVisibility(View.GONE);
    }

    private void showFreeLayout(ActivityPlansBinding binding) {
        binding.premiumLayout.setVisibility(View.GONE);
        binding.freeLayout.setVisibility(View.VISIBLE);
    }

    // Show PlanInfoDialogFragment for premium users
    private void showPlanInfoDialog() {
        PlanInfoDialogFragment dialog = PlanInfoDialogFragment.newInstance();
        dialog.show(getChildFragmentManager(), "PlanInfoDialog");
    }

    private void initListeners(View view) {
        view.findViewById(R.id.trial_link).setOnClickListener(v -> showTrialExplainerDialog());
        view.findViewById(R.id.show_all_subscriptions).setOnClickListener(v -> showAllSubscriptions());
        view.findViewById(R.id.cta_button).setOnClickListener(v -> startPurchaseFlow(true, ServerType.PremiumPlus, 12));
    }

    private void updateTryOutText(TextView textView, String discount) {
        String text = getString(Objects.equals(discount, "") ? R.string.get_premium_plus : R.string.try_out_for_7_days, discount);
        textView.setText(text);
        ShimmerFrameLayout shimmerFrameLayout = (ShimmerFrameLayout) textView.getParent();
        shimmerFrameLayout.stopShimmer(); // Stop the shimmer animation
        shimmerFrameLayout.hideShimmer(); // Hide the shimmer layout
        textView.setBackground(null);

    }


    private void updateThenGetDiscountText(TextView textView, TextView forTheFirstTwoYears, String discount) {
        String text;
        if (Objects.equals(discount, "")) {
            textView.setVisibility(View.GONE);
            forTheFirstTwoYears.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.VISIBLE);
            forTheFirstTwoYears.setVisibility(View.VISIBLE);
            text = getString(R.string.then_get_discount, discount);
            textView.setText(text);
            ShimmerFrameLayout shimmerFrameLayout = (ShimmerFrameLayout) textView.getParent();
            shimmerFrameLayout.stopShimmer(); // Stop the shimmer animation
            shimmerFrameLayout.hideShimmer(); // Hide the shimmer layout
            textView.setBackground(null);


            shimmerFrameLayout = (ShimmerFrameLayout) forTheFirstTwoYears.getParent();
            shimmerFrameLayout.stopShimmer(); // Stop the shimmer animation
            shimmerFrameLayout.hideShimmer(); // Hide the shimmer layout

            forTheFirstTwoYears.setBackground(null);

        }
    }


    private void updateYearlyPricePerMonth(TextView textView, String monthlyPrice) {
        // Parent shimmer container
        ShimmerFrameLayout shimmerFrameLayout = (ShimmerFrameLayout) textView.getParent();
        shimmerFrameLayout.stopShimmer(); // Stop the shimmer animation
        shimmerFrameLayout.hideShimmer(); // Hide the shimmer layout

        // Set the actual text
        String text = getString(R.string.price_yearly_per_month, monthlyPrice);
        textView.setText(text);
        textView.setBackground(null);
    }

    private void updateYearlyPricePerYear(TextView textView, String yearlyPrice) {
        // Parent shimmer container
        ShimmerFrameLayout shimmerFrameLayout = (ShimmerFrameLayout) textView.getParent();
        shimmerFrameLayout.stopShimmer(); // Stop the shimmer animation
        shimmerFrameLayout.hideShimmer(); // Hide the shimmer layout
        String text = getString(R.string.price_yearly_per_year, yearlyPrice);
        textView.setText(text);
        textView.setBackground(null);
    }

    private void showTrialExplainerDialog() {
        TrialExplainerDialogFragment trialDialog = new TrialExplainerDialogFragment();
        trialDialog.setPurchaseFlowListener(this);
        trialDialog.show(getChildFragmentManager(), "TrialExplainerDialog");
    }

    private void showAllSubscriptions() {
        AllSubscriptionsFragment allSubscriptionsDialog = new AllSubscriptionsFragment();
        allSubscriptionsDialog.setPurchaseFlowListener(this);
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.add(allSubscriptionsDialog, "AllSubscriptionsDialog");
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        Log.d(TAG, "onPurchasesUpdated called, billingResult: " + billingResult + ", purchases: " + purchases);
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            Log.d(TAG, "onPurchasesUpdated, Response OK, purchases != null");
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "Purchase was canceled by the user");
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Log.d(TAG, "Item already owned, attempting to consume");
            billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build(),
                    (billingResult1, purchaseList) -> {
                        if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            for (Purchase purchase : purchaseList) {
                                // For queryPurchasesAsync, you need to check all owned items
                                if (purchase.getProducts().get(0).startsWith("item")) {
                                    billingClient.consumeAsync(
                                            ConsumeParams.newBuilder()
                                                    .setPurchaseToken(purchase.getPurchaseToken())
                                                    .build(),
                                            (billingResult2, purchaseToken) -> {
                                                if (billingResult2.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                                    Log.d(TAG, "Successfully consumed purchase: " + purchaseToken);
                                                    handlePurchase(purchase);
                                                } else {
                                                    Log.d(TAG, "Failed to consume purchase: " + purchaseToken + ", response code: " + billingResult2.getResponseCode());
                                                }
                                            });
                                }
                            }
                        }
                    }
            );
        } else {
            Log.d(TAG, "Purchase failed with response code: " + billingResult.getResponseCode());
        }
    }


    private void handlePurchase(Purchase purchase) {
        Log.d(TAG, "handlePurchase called, purchase: " + purchase);
        // Handle the purchase
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            Log.d(TAG, "handlePurchase: PurchaseState.PURCHASED");
            // Grant the item to the user
            Vpn vpn = vpnRepository.getSelectedVpn().getValue();
            if (vpn != null) {
                Log.d(TAG, "handlePurchase: vpn != null, incorporating billingRepository.upgradeToPremiumAndroid");
                LiveData<UpgradeVpnResponse> upgradeToPremiumLiveData = billingRepository.upgradeToPremiumAndroid(
                        vpn.getVpnId(),
                        purchase.getOriginalJson(),
                        purchase.getSignature()
                );
                mActivity.runOnUiThread(() -> upgradeToPremiumLiveData.observe(this, result -> {
                    Log.d(TAG, "handlePurchase - upgradeToPremiumAndroid.observe: " + result);
                    if (result != null) {
                        Log.d(TAG, "handlePurchase: Result OK, showing status + upgradesuccess");
                        // Handle success
                        new Handler(Looper.getMainLooper()).post(() -> {
                            vpnRepository.updateVpnList();
                            vpnRepository.getVpnList().observe(this, vpnList -> mActivity.triggerDataSetChanged());
                        });

                        if (purchase.getProducts().get(0).startsWith("item")) {
                            billingClient.consumeAsync(
                                    ConsumeParams.newBuilder()
                                            .setPurchaseToken(purchase.getPurchaseToken())
                                            .build(),
                                    (billingResult2, purchaseToken) -> {
                                        if (billingResult2.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                            Log.d(TAG, "Successfully consumed purchase: " + purchaseToken);
                                            handlePurchase(purchase);
                                        } else {
                                            Log.d(TAG, "Failed to consume purchase: " + purchaseToken + ", response code: " + billingResult2.getResponseCode());
                                        }
                                    });
                        }
                        showUpgradeSuccessDialog();
                        // TODO: need to handle what happens after succesful update. not only here, but also the initial state
                        // when the user opens the tab again
                    } else {
                        Log.d(TAG, "handlePurchase: Result NOT OK, showing error message");
                        // Handle failure
                        showMessage(getString(R.string.upgrade_not_successful_check_log));
                    }
                }));
            } else {
                Log.d(TAG, "handlePurchase: vpn is null, not incorporating billingRepository.upgradeToPremiumAndroid");
            }


        }
    }


    void showMessage(final String msg) {
        if (TextUtils.isEmpty(msg)) return;
        mActivity.runOnUiThread(() -> Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show());
    }

    private void showUpgradeSuccessDialog() {
        new AlertDialog.Builder(requireActivity())
                .setMessage(R.string.premium_upgrade_succesful_show_serverlist)
                .setPositiveButton(R.string.yes, (dialog1, which) -> {
                    if (CommonUtils.isTablet(requireActivity())) {
                        mActivity.setCurrentItem(0);
                    } else {
                        mActivity.setCurrentItem(1);
                    }
                })
                .setNegativeButton(R.string.no, (dialog12, which) -> {
                    // stay on premium info tab, do nothing
                })
                .show();
    }

    public void startPurchaseFlow(boolean isSubscription, ServerType accountType, int billingPeriod) {
        Log.d(TAG, "startPurchaseFlow: " + isSubscription + ", " + accountType + ", " + billingPeriod);

        dataRepository.getSku(isSubscription, accountType, billingPeriod).observe(getViewLifecycleOwner(), sku -> {
            if (sku != null) {
                List<String> skuList = new ArrayList<>();
                skuList.add(sku.getSku()); // Add your SKU here
                SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                params.setSkusList(skuList).setType(isSubscription ? BillingClient.SkuType.SUBS : BillingClient.SkuType.INAPP);

                billingClient.querySkuDetailsAsync(params.build(), (billingResult, skuDetailsList) -> {
                    Log.d(TAG, "startPurchaseFlow - responseCode: " + billingResult.getResponseCode() + ", skuDetailsList size: " + (skuDetailsList != null ? skuDetailsList.size() : null));
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                        Log.d(TAG, "startPurchaseFlow: Result OK - launchBillingFlow for each of " + skuDetailsList);
                        for (SkuDetails skuDetails : skuDetailsList) {
                            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                    .setSkuDetails(skuDetails)
                                    .setObfuscatedAccountId(Objects.requireNonNull(authRepository.getObfuscatedAccountId().getValue()))
                                    .build();
                            billingClient.launchBillingFlow(requireActivity(), flowParams);
                        }
                    } else {
                        Log.d(TAG, "startPurchaseFlow: Result NOT OK, " + billingResult.getDebugMessage());
                    }
                });
            } else {
                Log.d(TAG, "startPurchaseFlow: sku is null");
            }

        });
    }
    private void updatePrices() {
        Log.d(TAG, "updatePrices called");

        billingClient.queryProductDetailsAsync(buildProductQuery(), (billingResult, productDetailsList) -> {
            Log.d(TAG, "updatePrices - responseCode: " + billingResult.getResponseCode() + ", productDetailsList size: " + productDetailsList.size());

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                PriceOverview priceOverview = new PriceOverview();

                for (ProductDetails productDetails : productDetailsList) {
                    Log.d(TAG, "updatePrices - processing sku: " + productDetails.getProductId());
                    List<ProductDetails.SubscriptionOfferDetails> subscriptionOfferDetails = productDetails.getSubscriptionOfferDetails();

                    if (subscriptionOfferDetails != null) {
                        PriceDetails basePlanPriceDetails = null;
                        PriceDetails cheapestOfferPriceDetails = null;

                        for (ProductDetails.SubscriptionOfferDetails offerDetails : subscriptionOfferDetails) {
                            // Get base plan price
                            PriceDetails currentBasePlanPriceDetails = getBasePlanPrice(offerDetails);

                            // Update base plan price if not null
                            if (currentBasePlanPriceDetails != null) {
                                basePlanPriceDetails = currentBasePlanPriceDetails;
                            }

                            // Get offer price and determine the cheapest
                            PriceDetails currentOfferPriceDetails = getLowestEffectivePriceFromOffer(offerDetails);
                            if (currentOfferPriceDetails != null) {
                                if (cheapestOfferPriceDetails == null ||
                                        currentOfferPriceDetails.getRawPrice() < cheapestOfferPriceDetails.getRawPrice()) {
                                    cheapestOfferPriceDetails = currentOfferPriceDetails;
                                }
                            }
                        }

                        if (productDetails.getProductId().equals("sub_monthly_premium_plus_2")) {
                            // Assign monthly subscription price
                            if (cheapestOfferPriceDetails != null) {
                                priceOverview.setMonthlyPrice(cheapestOfferPriceDetails);
                                Log.d(TAG, "updatePrices - Monthly price set: " + cheapestOfferPriceDetails);
                            }
                        } else if (productDetails.getProductId().equals("sub_yearly_premium_plus")) {
                            // Assign yearly subscription prices
                            if (basePlanPriceDetails != null) {
                                priceOverview.setYearlyPriceNormal(basePlanPriceDetails);

                                // Calculate and set not-discounted yearly per-month price
                                double yearlyMonthlyPriceNormalValue = basePlanPriceDetails.getRawPrice() / 12;
                                String yearlyMonthlyPriceNormalFormatted = Util.formatPrice(
                                        yearlyMonthlyPriceNormalValue, basePlanPriceDetails.getCurrencyCode());
                                PriceDetails yearlyMonthlyPriceNormalDetails = new PriceDetails(
                                        yearlyMonthlyPriceNormalValue,
                                        yearlyMonthlyPriceNormalFormatted,
                                        basePlanPriceDetails.getCurrencyCode()
                                );
                                priceOverview.setYearlyPricePerMonthNormal(yearlyMonthlyPriceNormalDetails);

                                Log.d(TAG, "updatePrices - Yearly normal price set: " + basePlanPriceDetails +
                                        ", Monthly derived from yearly (normal): " + yearlyMonthlyPriceNormalDetails);
                            }

                            if (cheapestOfferPriceDetails != null) {
                                priceOverview.setYearlyPriceDiscounted(cheapestOfferPriceDetails);

                                // Calculate and set discounted yearly per-month price
                                double yearlyMonthlyPriceDiscountedValue = cheapestOfferPriceDetails.getRawPrice() / 12;
                                String yearlyMonthlyPriceDiscountedFormatted = Util.formatPrice(
                                        yearlyMonthlyPriceDiscountedValue, cheapestOfferPriceDetails.getCurrencyCode());
                                PriceDetails yearlyMonthlyPriceDiscountedDetails = new PriceDetails(
                                        yearlyMonthlyPriceDiscountedValue,
                                        yearlyMonthlyPriceDiscountedFormatted,
                                        cheapestOfferPriceDetails.getCurrencyCode()
                                );
                                priceOverview.setYearlyPricePerMonthDiscounted(yearlyMonthlyPriceDiscountedDetails);

                                Log.d(TAG, "updatePrices - Yearly discounted price set: " + cheapestOfferPriceDetails +
                                        ", Monthly derived from yearly (discounted): " + yearlyMonthlyPriceDiscountedDetails);
                            }
                        }
                    } else {
                        Log.d(TAG, "updatePrices - No subscription offer details found for SKU: " + productDetails.getProductId());
                    }
                }

                // Log final PriceOverview details for debugging
                Log.d(TAG, "updatePrices - Final PriceOverview: " + priceOverview);

                // Update ViewModel with the new price data
                viewModel.setSubscriptionData(priceOverview);
            } else {
                Log.e(TAG, "Failed to fetch product details: " + billingResult.getDebugMessage());
            }
        });
    }



    private PriceDetails getLowestEffectivePriceFromOffer(ProductDetails.SubscriptionOfferDetails offerDetails) {
        List<ProductDetails.PricingPhase> pricingPhases = offerDetails.getPricingPhases().getPricingPhaseList();
        double lowestPrice = Double.MAX_VALUE;
        String lowestPriceString = "";
        String currencyCode = "";

        for (ProductDetails.PricingPhase phase : pricingPhases) {
            double price = phase.getPriceAmountMicros() / 1_000_000.0;

            if (price > 0.0 && price < lowestPrice) {
                lowestPrice = price;
                lowestPriceString = phase.getFormattedPrice();
                currencyCode = phase.getPriceCurrencyCode();
            }
        }

        return new PriceDetails(
                lowestPrice == Double.MAX_VALUE ? 0.0 : lowestPrice,
                lowestPriceString,
                currencyCode
        );
    }

    private PriceDetails getBasePlanPrice(ProductDetails.SubscriptionOfferDetails offerDetails) {
        List<ProductDetails.PricingPhase> pricingPhases = offerDetails.getPricingPhases().getPricingPhaseList();
        double lastRecurringPrice = 0.0;
        String lastRecurringPriceString = "";
        String currencyCode = "";

        for (ProductDetails.PricingPhase phase : pricingPhases) {
            double price = phase.getPriceAmountMicros() / 1_000_000.0;


            Log.d(TAG, "getBasePlanPrice - phase " + phase.getBillingCycleCount() + ": price=" + price +
                    ", formattedPrice=" + phase.getFormattedPrice() +
                    ", billingPeriod=" + phase.getBillingPeriod() +
                    ", recurrenceMode=" + phase.getRecurrenceMode());

            if (phase.getRecurrenceMode() == 1) { // INFINITE_RECURRING
                lastRecurringPrice = price;
                lastRecurringPriceString = phase.getFormattedPrice();
                currencyCode = phase.getPriceCurrencyCode();
            }
        }

        return new PriceDetails(lastRecurringPrice, lastRecurringPriceString, currencyCode);
    }

    private QueryProductDetailsParams buildProductQuery() {
        List<QueryProductDetailsParams.Product> products = new ArrayList<>();
        products.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId("sub_monthly_premium_plus_2")
                .setProductType(BillingClient.ProductType.SUBS)
                .build());
        products.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId("sub_yearly_premium_plus")
                .setProductType(BillingClient.ProductType.SUBS)
                .build());
        return QueryProductDetailsParams.newBuilder()
                .setProductList(products)
                .build();
    }

    public static class TrialExplainerDialogFragment extends DialogFragment {
        private PurchaseFlowListener purchaseFlowListener;
        private String TAG = "TrialExplainerDialogFragment";

        public void setPurchaseFlowListener(PurchaseFlowListener listener) {
            this.purchaseFlowListener = listener;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.trial_explainer_dialog, container, false);
            return view;
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            view.findViewById(R.id.cta_button).setOnClickListener(v -> {
                if (purchaseFlowListener != null) {
                    purchaseFlowListener.startPurchaseFlow(true, ServerType.PremiumPlus, 12);
                }
                dismiss();
            });
            view.findViewById(R.id.close_button).setOnClickListener(v -> dismiss());

            TextView trialPriceView = view.findViewById(R.id.trial_explainer_price);

            SubscriptionViewModel viewModel = new ViewModelProvider(requireActivity()).get(SubscriptionViewModel.class);

            viewModel.getSubscriptionData().observe(getViewLifecycleOwner(), data -> {
                if (data != null) {
                    Log.d(TAG, "onViewCreated.viewModel.getSubscriptionData(): " + data);
                    updatePriceInfoYearlyPerYearTrialExplainer(trialPriceView, data.getYearlyPriceDiscounted().getFormattedPrice(), data.getYearlyPriceNormal().getFormattedPrice());
                }
            });


        }

        private void updatePriceInfoYearlyPerYearTrialExplainer(TextView textView, String yearlyPrice, String monthlyPrice) {
            String text = getString(R.string.price_info_yearly_per_year_trial_explainer, yearlyPrice, monthlyPrice);
            textView.setText(text);
        }


        @Override
        public void onStart() {
            super.onStart();
            // Adjust the width and height of the dialog here
            if (getDialog() != null && getDialog().getWindow() != null) {
                getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        }


    }

    public static class AllSubscriptionsFragment extends DialogFragment {
        private PurchaseFlowListener purchaseFlowListener;
        private String TAG = "AllSubscriptionsFragment";
        private SubscriptionViewModel viewModel;

        public void setPurchaseFlowListener(PurchaseFlowListener listener) {
            this.purchaseFlowListener = listener;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.screen_all_subscriptions, container, false);
            return view;
        }


        @Override
        public void onStart() {
            super.onStart();
            // Adjust the width and height of the dialog here
            if (getDialog() != null && getDialog().getWindow() != null) {
                getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            view.findViewById(R.id.subscribe_button).setOnClickListener(v -> {
                RadioButton yearlyButton = view.findViewById(R.id.yearly_plan);
                boolean isYearly = yearlyButton.isChecked();
                if (purchaseFlowListener != null) {
                    purchaseFlowListener.startPurchaseFlow(true, ServerType.PremiumPlus, isYearly ? 12 : 1);
                }
            });
            view.findViewById(R.id.trial_explainer_button).setOnClickListener(v -> showTrialExplainerDialog());
            view.findViewById(R.id.back_button).setOnClickListener(v -> getParentFragmentManager().popBackStack());

            RadioButton yearlyPlanButton = view.findViewById(R.id.yearly_plan);
            TextView rabattTextView = view.findViewById(R.id.discount_also_in_next_two_years);
            RadioButton monthlyPlanButton = view.findViewById(R.id.monthly_plan);


            viewModel = new ViewModelProvider(requireActivity()).get(SubscriptionViewModel.class);

            viewModel.getSubscriptionData().observe(getViewLifecycleOwner(), data -> {
                Log.d(TAG, "onViewCreated.viewModel.getSubscriptionData(): " + data);
                if (data != null) {
                    updateYearlyPricePerMonthAllSubs(yearlyPlanButton, data.getYearlyPricePerMonthDiscounted().getFormattedPrice());
                    updateDiscountAlsoInSubsequentYears(rabattTextView, data.getFormattedDiscountForYearly());
                    updateMonthlyPricePerMonthAllSubs(monthlyPlanButton, data.getMonthlyPrice().getFormattedPrice());
                    TextView firstPaymentInfoView = view.findViewById(R.id.first_payment_info);

                    if (!data.hasDiscountForYearly()) {
                        view.findViewById(R.id.discount_also_in_next_two_years).setVisibility(View.GONE);
                        view.findViewById(R.id.seven_days_free).setVisibility(View.GONE);
                        view.findViewById(R.id.trial_explainer_button).setVisibility(View.GONE);

                        firstPaymentInfoView.setText(R.string.first_payment_info_today);


                    } else {
                        view.findViewById(R.id.discount_also_in_next_two_years).setVisibility(View.VISIBLE);
                        view.findViewById(R.id.seven_days_free).setVisibility(View.VISIBLE);
                        view.findViewById(R.id.trial_explainer_button).setVisibility(View.VISIBLE);
                        firstPaymentInfoView.setText(R.string.first_payment_info_seven_days);
                    }
                }
            });

            initListeners(view);
        }

        private void updateYearlyPricePerMonthAllSubs(RadioButton radioButton, String monthlyPrice) {
            String text = getString(R.string.price_yearly_per_month_all_subs, monthlyPrice);
            radioButton.setText(text);
        }


        private void updateDiscountAlsoInSubsequentYears(TextView textView, String discount) {
            if (discount == "") {
                textView.setVisibility(View.GONE);
            } else {
                textView.setVisibility(View.VISIBLE);
                String text = getString(R.string.discount_also_in_next_two_years, discount);
                textView.setText(text);
            }

        }


        private void updateMonthlyPricePerMonthAllSubs(RadioButton radioButton, String monthlyPrice) {
            String text = getString(R.string.price_monthly_per_month_all_subs, monthlyPrice);
            radioButton.setText(text);
        }

        private void updateSubscribeButton(Button button, boolean isYearly) {
            String price = "";
            if (viewModel != null) {
                PriceOverview data = viewModel.getSubscriptionData().getValue();
                if (data != null) {
                    price = isYearly ? data.getYearlyPriceDiscounted().getFormattedPrice() : data.getMonthlyPrice().getFormattedPrice();
                }
            }

            String monthText = getString(isYearly ? R.string.twelve_months : R.string.one_month);
            String text = getString(R.string.price_per_month_selected_sub, price, monthText);
            button.setText(text);
        }


        private void initListeners(View view) {
            LinearLayout yearlyPlanContainer = view.findViewById(R.id.yearly_plan_container);
            LinearLayout monthlyPlanContainer = view.findViewById(R.id.monthly_plan_container);

            RadioButton yearlyPlan = view.findViewById(R.id.yearly_plan);
            RadioButton monthlyPlan = view.findViewById(R.id.monthly_plan);

            View.OnClickListener planSelectionListener = v -> {
                if (v == yearlyPlanContainer || v == yearlyPlan) {
                    yearlyPlanContainer.setBackground(requireContext().getDrawable(R.drawable.radio_button_selected_background));
                    monthlyPlanContainer.setBackground(requireContext().getDrawable(R.drawable.border_background));
                    yearlyPlan.setChecked(true);
                    monthlyPlan.setChecked(false);
                } else if (v == monthlyPlanContainer || v == monthlyPlan) {
                    yearlyPlanContainer.setBackground(requireContext().getDrawable(R.drawable.border_background));
                    monthlyPlanContainer.setBackground(requireContext().getDrawable(R.drawable.radio_button_selected_background));

                    monthlyPlan.setChecked(true);
                    yearlyPlan.setChecked(false);
                }
                updateSubscribeButton((Button) view.findViewById(R.id.subscribe_button), yearlyPlan.isChecked());
            };

            // Attach listeners
            yearlyPlanContainer.setOnClickListener(planSelectionListener);
            yearlyPlan.setOnClickListener(planSelectionListener);
            monthlyPlanContainer.setOnClickListener(planSelectionListener);
            monthlyPlan.setOnClickListener(planSelectionListener);

            planSelectionListener.onClick(yearlyPlanContainer);
        }

        private void showTrialExplainerDialog() {
            TrialExplainerDialogFragment trialDialog = new TrialExplainerDialogFragment();
            trialDialog.setPurchaseFlowListener(purchaseFlowListener);
            trialDialog.show(getChildFragmentManager(), "TrialExplainerDialog");
        }

    }
}