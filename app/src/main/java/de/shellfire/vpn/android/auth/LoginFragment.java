package de.shellfire.vpn.android.auth;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import de.shellfire.vpn.android.R;
import de.shellfire.vpn.android.viewmodel.SharedViewModel;
import de.shellfire.vpn.android.viewmodel.SharedViewModelFactory;

public class LoginFragment extends Fragment {

    public static final String REGISTERACTIVITYSHOWN2 = "REGISTERACTIVITYSHOWN";
    public final static String PARAM_USER_HAS_LOGIN = "PARAM_USER_HAS_LOGIN";

    public final static String ARG_AUTH_TYPE = "AUTH_TYPE";
    public final static String ARG_ACCOUNT_NAME = "ACCOUNT_NAME";
    public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";
    public static final String EXTRA_EMAIL = "de.shellfire.vpn.android.auth.EMAIL";
    public static final String KEY_ERROR_MESSAGE = "ERR_MSG";
    private final String TAG = "LoginFragment";
    private String mAuthTokenType;
    private String mUser;
    private String mPassword;
    private EditText mEmailView;
    private EditText mPasswordView;
    private View mLoginFormView;
    private View mLoginStatusView;
    private TextView mLoginStatusMessageView;
    private Observer<LoginStatus> loginStatusObserver;
    private NavController navController;
    private SharedViewModel sharedViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);

        // Initialize sharedViewModel
        sharedViewModel = new ViewModelProvider(requireActivity(), new SharedViewModelFactory(
                getContext().getApplicationContext()
        )).get(SharedViewModel.class);

        // In case already logged in, trigger navigation. sharedViewModel decides based on overall state, MainBaseActivity observes and navigates
        sharedViewModel.updateTargetFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_login, container, false);
        navController = NavHostFragment.findNavController(this);

        mEmailView = rootView.findViewById(R.id.email_login);
        mPasswordView = rootView.findViewById(R.id.password_login);
        mLoginFormView = rootView.findViewById(R.id.login_form);
        mLoginStatusView = rootView.findViewById(R.id.login_status);
        mLoginStatusMessageView = rootView.findViewById(R.id.register_status_message);
        Button loginButton = rootView.findViewById(R.id.login_button);
        Button lostAccountDataButton = rootView.findViewById(R.id.btnLostAccountData);

        mPasswordView.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLoginChecked();
                return true;
            }
            return false;
        });

        loginButton.setOnClickListener(view -> attemptLoginChecked());
        lostAccountDataButton.setOnClickListener(this::lostAccountDataClicked);

        Button registerButton = rootView.findViewById(R.id.register_button);
        registerButton.setOnClickListener(this::registerButtonClicked);


        observeLoginStatus();

        return rootView;
    }

    private void registerButtonClicked(View view) {
        Log.d(TAG, "registerButtonClicked");
        navController.navigate(R.id.action_loginFragment_to_registerFragment);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void observeLoginStatus() {
        // Observe the LiveData from the sharedViewModel
        loginStatusObserver = loginStatus -> {
            Log.d(TAG, "sharedViewModel.getLoginStatus().observer.onChanged, loginStatus = " + loginStatus);
            showProgress(false);
            if (isDetached() || isRemoving()) {
                Log.d(TAG, "Fragment is detached or removing. Not showing dialog.");
                return;
            }

            if (LoginStatus.LoggedInInactive == loginStatus) {
                if (sharedViewModel.isRegistrationOngoing()) {
                    Log.d(TAG, "Loginstatus LoggedInInactive and registration ongoing, doing nothing");
                } else {
                    Log.d(TAG, "Loginstatus LoggedInInactive and no registration ongoing, showing message that email is not verified");

                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    String alertTitle = getString(R.string.email_validation_required_title);
                    String alertText = getString(R.string.email_validation_required);

                    builder.setTitle(alertTitle).setMessage(alertText).setCancelable(false)
                            .setPositiveButton("OK", (dialog, id) -> {
                                // No action needed
                            });
                    AlertDialog alert = builder.create();
                    alert.show();

                }
            } else if (LoginStatus.LoginFailed == loginStatus) {
                Log.d(TAG, "Loginstatus logged out, finish login");
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setMessage(getString(R.string.login_error)).setCancelable(false)
                        .setPositiveButton("OK", (dialog, id) -> {
                            // Do nothing
                        });
                AlertDialog alert = builder.create();
                alert.show();
            } else if (LoginStatus.LoggedIn == loginStatus) {
                Log.d(TAG, "Loginstatus active, finish login. Triggering navController updated through sharedViewModel / MainBaseActivity");
                sharedViewModel.updateTargetFragment();
                sharedViewModel.getLoginStatus().removeObserver(loginStatusObserver);
            }
        };

        sharedViewModel.getLoginStatus().observe(getViewLifecycleOwner(), loginStatusObserver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (loginStatusObserver != null) {
            sharedViewModel.getLoginStatus().removeObserver(loginStatusObserver);
        }
        Log.d(TAG, "LoginFragment onDestroyView: observer removed");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
    }

    public void lostAccountDataClicked(View view) {
        String url = "https://www.shellfire.de/passwort-verloren/"; // Replace with actual URL
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);

        // Check if there is a browser to handle the intent
        if (intent.resolveActivity(view.getContext().getPackageManager()) != null) {
            view.getContext().startActivity(intent);
        } else {
            // Handle the case where no browser is available
            Toast.makeText(view.getContext(), R.string.no_browser_available_to_open_the_link, Toast.LENGTH_SHORT).show();
        }
    }


    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public void attemptLoginChecked() {
        Runnable action = this::attemptLogin;
        if (isInternetAvailable()) {
            action.run();
        } else {
            showDialogInternetRequired(R.string.retry, action);
        }
    }

    private void showDialogInternetRequired(int id, final Runnable action) {
        AlertDialog.Builder alert = new AlertDialog.Builder(requireContext());
        alert.setTitle(getString(R.string.network_problem_title));
        alert.setMessage(getString(R.string.network_problem_message));
        alert.setPositiveButton(getString(id), (arg0, arg1) -> {
            if (isInternetAvailable()) {
                action.run();
            } else {
                showDialogInternetRequired(R.string.retry, action);
            }
        });
        alert.show();
    }

    public void attemptLogin() {
        Log.d(TAG, "attemptLogin");
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEmailView.getWindowToken(), 0);

        mEmailView.setError(null);
        mPasswordView.setError(null);

        mUser = mEmailView.getText().toString().trim();
        mPassword = mPasswordView.getText().toString().trim();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(mPassword)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (mPassword.length() < 4) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        if (TextUtils.isEmpty(mUser)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            Log.d(TAG, "attemptLogin, all checks passed");
            mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
            showProgress(true);
            sharedViewModel.login(mUser, mPassword);
        }
    }

    private void showProgress(final boolean show) {
        Log.d(TAG, "showProgress, show = " + show);
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginStatusView.setVisibility(View.VISIBLE);
        mLoginStatusView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });

        mLoginFormView.setVisibility(View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.login, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
}
