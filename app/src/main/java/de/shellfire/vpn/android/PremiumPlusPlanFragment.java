package de.shellfire.vpn.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.facebook.shimmer.ShimmerFrameLayout;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import de.shellfire.vpn.android.auth.AuthRepository;
import de.shellfire.vpn.android.databinding.ActivityPlansBinding;
import de.shellfire.vpn.android.viewmodel.SharedViewModel;
import de.shellfire.vpn.android.viewmodel.SharedViewModelFactory;

public class PremiumPlusPlanFragment extends DialogFragment implements BillingListener, PurchaseFlowListener {

    private static final String TAG = "PremiumPlusPlanFragment";
    private MainBaseActivity mActivity;
    private BillingController billingController;
    private VpnRepository vpnRepository;
    private AuthRepository authRepository;
    private DataRepository dataRepository;
    private SubscriptionViewModel viewModel;
    private View currentView;

    public static PremiumPlusPlanFragment newInstance() {
        return new PremiumPlusPlanFragment();
    }

    @Override
    public void startPurchaseFlow(boolean isSubscription, ServerType accountType, int billingPeriod) {
        if (billingController != null && !getResources().getBoolean(R.bool.isFdroidBuild)) {
            billingController.launchPurchaseFlow(isSubscription, accountType, billingPeriod);
        } else {
            openOffersWebsite();
        }
    }


    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        mActivity = (MainBaseActivity) activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        setRetainInstance(true);

        // Initialize SharedViewModel
        SharedViewModel sharedViewModel = new ViewModelProvider(requireActivity(),
                new SharedViewModelFactory(requireActivity()))
                .get(SharedViewModel.class);

        // Create billing controller via a flavor-specific factory.
        billingController = BillingControllerFactory.create(requireActivity());
        billingController.setBillingListener(this);
        billingController.startBillingConnection();

        vpnRepository = VpnRepository.getInstance(getContext());
        authRepository = AuthRepository.getInstance(getContext());
        dataRepository = DataRepository.getInstance(getContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        // Choose layout based on whether this is the FDroid build.
        int layoutResId = getResources().getBoolean(R.bool.isFdroidBuild) ?
                R.layout.screen_default_subscription_fdroid : R.layout.screen_default_subscription;
        return inflater.inflate(layoutResId, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        currentView = view;

        viewModel = new ViewModelProvider(requireActivity()).get(SubscriptionViewModel.class);

        vpnRepository.getSelectedVpn().observe(getViewLifecycleOwner(), selectedVpn -> {
            if (selectedVpn != null) {
                Log.d(TAG, "VPN Account Type: " + selectedVpn.getAccountType());

                if (selectedVpn.getAccountType() != ServerType.Free) {
                    replaceWithPlanInfoLayout();
                } else {
                    revertToDefaultSubscriptionLayout();
                }
            }
        });

        // Ensure price updates only for Play Store version
        if (!getResources().getBoolean(R.bool.isFdroidBuild)) {
            billingController.updatePrices();
        }
    }


    private void replaceWithPlanInfoLayout() {
        ViewGroup parent = (ViewGroup) currentView.getParent();
        if (parent != null) {
            parent.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            currentView = inflater.inflate(R.layout.activity_plans, parent, false);
            parent.addView(currentView);

            // Bind new view
            setupPlanInfoLayout(currentView);

            // 🔥 Force observer to trigger with updated data
            vpnRepository.getSelectedVpn().removeObservers(getViewLifecycleOwner());
            vpnRepository.getSelectedVpn().observe(getViewLifecycleOwner(), selectedVpn -> {
                if (selectedVpn != null) {
                    Log.d(TAG, "VPN Data Updated: " + selectedVpn.getAccountType());
                    updatePlanUI(selectedVpn);
                }
            });
        }
    }
    private void updatePlanUI(Vpn selectedVpn) {
        if (selectedVpn != null) {
            TextView planUser = currentView.findViewById(R.id.plan_user);
            TextView paidUntilText = currentView.findViewById(R.id.paid_until_date);
            View premiumLayout = currentView.findViewById(R.id.premium_layout);
            View freeLayout = currentView.findViewById(R.id.free_layout);

            // 🔹 1. Set correct account type text
            if (selectedVpn.getAccountType() != null) {
                if (selectedVpn.getAccountType() == ServerType.PremiumPlus) {
                    planUser.setText(ServerType.ShellfireVPN.toString());  // Matches PlansActivity logic
                } else {
                    planUser.setText(selectedVpn.getAccountType().toString());
                }
            }

            // 🔹 2. Set "Paid Until" date correctly
            if (selectedVpn.getPremiumUntil() != null) {
                Date paidUntil = selectedVpn.getPremiumUntil();
                DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
                String paidUntilStr = dateFormat.format(paidUntil);
                paidUntilText.setText(paidUntilStr);
            }

            // 🔹 3. Show correct UI (Free vs. Premium)
            if (selectedVpn.getAccountType() == ServerType.Free) {
                premiumLayout.setVisibility(View.GONE);
                freeLayout.setVisibility(View.VISIBLE);
            } else {
                premiumLayout.setVisibility(View.VISIBLE);
                freeLayout.setVisibility(View.GONE);
            }

            // 🔹 4. Set correct crown visibility
            updateCrowns(selectedVpn.getAccountType());
        }
    }
    private void updateCrowns(ServerType accountType) {
        ImageView star1 = currentView.findViewById(R.id.star_1);
        ImageView star2 = currentView.findViewById(R.id.star_2);
        ImageView star3 = currentView.findViewById(R.id.star_3);

        // Hide all crowns initially
        star1.setVisibility(View.GONE);
        star2.setVisibility(View.GONE);
        star3.setVisibility(View.GONE);

        // Show crowns based on subscription type
        if (accountType == ServerType.Premium) {
            star1.setVisibility(View.VISIBLE);
            star2.setVisibility(View.VISIBLE);
        } else if (accountType == ServerType.PremiumPlus) {
            star1.setVisibility(View.VISIBLE);
            star2.setVisibility(View.VISIBLE);
            star3.setVisibility(View.VISIBLE);
        }
    }

    private void revertToDefaultSubscriptionLayout() {
        ViewGroup parent = (ViewGroup) currentView.getParent();
        if (parent != null) {
            parent.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(getContext());

            int layout = getResources().getBoolean(R.bool.isFdroidBuild) ? R.layout.screen_default_subscription_fdroid : R.layout.screen_default_subscription;

            currentView = inflater.inflate(layout, parent, false);
            parent.addView(currentView);
            initListeners(currentView);
            initSubscriptionDataObserver(currentView);
        }
    }

    private void initSubscriptionDataObserver(View view) {
        viewModel.getSubscriptionData().observe(getViewLifecycleOwner(), data -> {
            if (data != null && !getResources().getBoolean(R.bool.isFdroidBuild)) {
                updateSubscriptionUI(view, data);
            }
        });
    }

    private void updateSubscriptionUI(View view, PriceOverview data) {
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

    private void updateTryOutText(TextView textView, String discount) {
        String text = getString(TextUtils.isEmpty(discount) ? R.string.get_premium_plus : R.string.try_out_for_7_days, discount);
        // Stop shimmer and remove placeholder background
        View parent = (View) textView.getParent();
        if (parent instanceof ShimmerFrameLayout) {
            ((ShimmerFrameLayout) parent).stopShimmer();
            ((ShimmerFrameLayout) parent).hideShimmer();
        }
        textView.setBackground(null);
        textView.setText(text);
    }

    private void updateThenGetDiscountText(TextView textView, TextView forTheFirstTwoYears, String discount) {
        if (TextUtils.isEmpty(discount)) {
            textView.setVisibility(View.GONE);
            forTheFirstTwoYears.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.VISIBLE);
            forTheFirstTwoYears.setVisibility(View.VISIBLE);
            String text = getString(R.string.then_get_discount, discount);
            textView.setText(text);
            // Stop shimmer on both views and remove backgrounds.
            View parent1 = (View) textView.getParent();
            if (parent1 instanceof ShimmerFrameLayout) {
                ((ShimmerFrameLayout) parent1).stopShimmer();
                ((ShimmerFrameLayout) parent1).hideShimmer();
            }
            textView.setBackground(null);
            View parent2 = (View) forTheFirstTwoYears.getParent();
            if (parent2 instanceof ShimmerFrameLayout) {
                ((ShimmerFrameLayout) parent2).stopShimmer();
                ((ShimmerFrameLayout) parent2).hideShimmer();
            }
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
        View parent = (View) textView.getParent();
        if (parent instanceof ShimmerFrameLayout) {
            ((ShimmerFrameLayout) parent).stopShimmer();
            ((ShimmerFrameLayout) parent).hideShimmer();
        }
        textView.setBackground(null);
        String text = getString(R.string.price_yearly_per_year, yearlyPrice);
        textView.setText(text);
    }

    private void setupPlanInfoLayout(View newView) {
        ActivityPlansBinding binding = ActivityPlansBinding.bind(newView);
        setUserPlan(binding);
        binding.backBtn.setVisibility(View.GONE);
    }

    private void setUserPlan(ActivityPlansBinding binding) {
        vpnRepository.getSelectedVpn().observe(getViewLifecycleOwner(), selectedVpn -> {
            if (selectedVpn != null) {
                binding.planUser.setText(selectedVpn.getAccountType() == ServerType.PremiumPlus
                        ? ServerType.ShellfireVPN.toString() : selectedVpn.getAccountType().toString());
                if (selectedVpn.getPremiumUntil() != null) {
                    Date paidUntil = selectedVpn.getPremiumUntil();
                    DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
                    String paidUntilStr = dateFormat.format(paidUntil);
                    binding.paidUntilDate.setText(paidUntilStr);
                }
            }
        });
    }

    private void initListeners(View view) {
        View trialLink = view.findViewById(R.id.trial_link);
        if (trialLink != null) {
            trialLink.setOnClickListener(v -> {
                if (billingController != null && !getResources().getBoolean(R.bool.isFdroidBuild)) {
                    billingController.launchPurchaseFlow(true, ServerType.PremiumPlus, 12);
                } else {
                    openOffersWebsite();
                }
            });
        }

        View showSubscriptions = view.findViewById(R.id.show_all_subscriptions);
        if (showSubscriptions != null) {
            showSubscriptions.setOnClickListener(v -> showAllSubscriptions());
        }

        View ctaButton = view.findViewById(R.id.cta_button);
        if (ctaButton != null) {
            ctaButton.setOnClickListener(v -> {
                if (billingController != null && !getResources().getBoolean(R.bool.isFdroidBuild)) {
                    billingController.launchPurchaseFlow(true, ServerType.PremiumPlus, 12);
                } else {
                    openOffersWebsite();
                }
            });
        }
    }


    private void showAllSubscriptions() {
        AllSubscriptionsFragment allSubscriptionsDialog = new AllSubscriptionsFragment();
        allSubscriptionsDialog.setPurchaseFlowListener(this);
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.add(allSubscriptionsDialog, "AllSubscriptionsDialog");
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void openOffersWebsite() {
        String url = "https://www.shellfire.net/prices/";
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        if (intent.resolveActivity(getContext().getPackageManager()) != null) {
            getContext().startActivity(intent);
        } else {
            Toast.makeText(getContext(), R.string.no_browser_available_to_open_the_link, Toast.LENGTH_SHORT).show();
        }
    }

    // --- BillingListener callbacks ---

    @Override
    public void onPricesUpdated(PriceOverview priceOverview) {
        if (currentView != null && !getResources().getBoolean(R.bool.isFdroidBuild)) {
            updateSubscriptionUI(currentView, priceOverview);
        }
    }

    @Override
    public void onPurchaseSuccess() {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(getContext(), R.string.premium_upgrade_succesful_show_serverlist, Toast.LENGTH_SHORT).show();
            vpnRepository.updateVpnList();
            mActivity.triggerDataSetChanged();
        });
    }

    @Override
    public void onPurchaseFailure(String errorMessage) {
        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
    }

    // --- Inner Classes ---

    public static class TrialExplainerDialogFragment extends DialogFragment {
        private PurchaseFlowListener purchaseFlowListener;
        private final String TAG = "TrialExplainerDialogFragment";

        public void setPurchaseFlowListener(PurchaseFlowListener listener) {
            this.purchaseFlowListener = listener;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.trial_explainer_dialog, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view,
                                  @Nullable Bundle savedInstanceState) {
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
                    updatePriceInfoYearlyPerYearTrialExplainer(trialPriceView,
                            data.getYearlyPriceDiscounted().getFormattedPrice(),
                            data.getYearlyPriceNormal().getFormattedPrice());
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
            if (getDialog() != null && getDialog().getWindow() != null) {
                getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
                getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        }
    }

    public static class AllSubscriptionsFragment extends DialogFragment {
        private PurchaseFlowListener purchaseFlowListener;
        private final String TAG = "AllSubscriptionsFragment";
        private SubscriptionViewModel viewModel;

        public static AllSubscriptionsFragment newInstance() {
            return new AllSubscriptionsFragment();
        }

        public void setPurchaseFlowListener(PurchaseFlowListener listener) {
            this.purchaseFlowListener = listener;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.screen_all_subscriptions, container, false);
        }

        @Override
        public void onStart() {
            super.onStart();
            if (getDialog() != null && getDialog().getWindow() != null) {
                getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
                getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        }

        @Override
        public void onViewCreated(@NonNull View view,
                                  @Nullable Bundle savedInstanceState) {
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
            String text = radioButton.getContext().getString(R.string.price_yearly_per_month_all_subs, monthlyPrice);
            radioButton.setText(text);
        }

        private void updateDiscountAlsoInSubsequentYears(TextView textView, String discount) {
            if (discount.isEmpty()) {
                textView.setVisibility(View.GONE);
            } else {
                textView.setVisibility(View.VISIBLE);
                String text = textView.getContext().getString(R.string.discount_also_in_next_two_years, discount);
                textView.setText(text);
            }
        }

        private void updateMonthlyPricePerMonthAllSubs(RadioButton radioButton, String monthlyPrice) {
            String text = radioButton.getContext().getString(R.string.price_monthly_per_month_all_subs, monthlyPrice);
            radioButton.setText(text);
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

            yearlyPlanContainer.setOnClickListener(planSelectionListener);
            yearlyPlan.setOnClickListener(planSelectionListener);
            monthlyPlanContainer.setOnClickListener(planSelectionListener);
            monthlyPlan.setOnClickListener(planSelectionListener);

            planSelectionListener.onClick(yearlyPlanContainer);
        }

        private void updateSubscribeButton(Button button, boolean isYearly) {
            String price = "";
            PriceOverview data = viewModel.getSubscriptionData().getValue();
            if (data != null) {
                price = isYearly ? data.getYearlyPriceDiscounted().getFormattedPrice() : data.getMonthlyPrice().getFormattedPrice();
            }
            String monthText = getString(isYearly ? R.string.twelve_months : R.string.one_month);
            String text = getString(R.string.price_per_month_selected_sub, price, monthText);
            button.setText(text);
        }

        private void showTrialExplainerDialog() {
            TrialExplainerDialogFragment trialDialog = new TrialExplainerDialogFragment();
            trialDialog.setPurchaseFlowListener(purchaseFlowListener);
            trialDialog.show(getChildFragmentManager(), "TrialExplainerDialog");
        }
    }
}
