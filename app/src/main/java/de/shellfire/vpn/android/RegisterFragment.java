package de.shellfire.vpn.android;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import java.security.SecureRandom;

import de.shellfire.vpn.android.auth.ActivationStatus;
import de.shellfire.vpn.android.utils.Util;
import de.shellfire.vpn.android.viewmodel.SharedViewModel;
import de.shellfire.vpn.android.viewmodel.SharedViewModelFactory;

public class RegisterFragment extends Fragment {

    private static final String TAG = "RegisterFragment";
    private TextView mRegisterStatusMessageView;
    private View mRegisterFormView;
    private View mRegisterStatusView;
    private EditText mEmailView;
    private EditText mPasswordView;
    private ProgressDialog progressDialog;
    private SharedViewModel sharedViewModel;
    private Observer<ActivationStatus> activationStatusObserver;

    private CredentialManager credentialManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        setRetainInstance(true);

        // Initialize SharedViewModel
        sharedViewModel = new ViewModelProvider(requireActivity(), new SharedViewModelFactory(
                getContext().getApplicationContext()
        )).get(SharedViewModel.class);


        sharedViewModel.getRegisterResult().observe(this, result -> {
            finishRegistration(result);
            showProgress(false);
        });


        credentialManager = CredentialManager.create(requireContext());


    }



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "Inflating fragment_register layout");
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        Log.d(TAG, "onCreateView called");

        mRegisterFormView = view.findViewById(R.id.textRulesRegs);
        mRegisterStatusView = view.findViewById(R.id.register_status);
        mRegisterStatusMessageView = view.findViewById(R.id.register_status_message);
        Button registerButton = view.findViewById(R.id.register_button);
        mEmailView = view.findViewById(R.id.email_register);
        mPasswordView = view.findViewById(R.id.password_register);


        // Set up the register form.
        TextView mTosTextView = view.findViewById(R.id.textAcceptRulesRegs);
        String tos = Util.getRegisterInfo();
        mTosTextView.setMovementMethod(LinkMovementMethod.getInstance());
        mTosTextView.setText(fromHtml(tos));
        mTosTextView.setMovementMethod(LinkMovementMethod.getInstance());

        CheckBox mTosCheckBox = view.findViewById(R.id.checkRulesRegs);
        mTosTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        CharSequence text = mTosTextView.getText();
                        int offset = mTosTextView.getOffsetForPosition(event.getX(), event.getY());
                        URLSpan[] types = ((Spanned) text).getSpans(offset, offset, URLSpan.class);
                        if (types.length > 0) {
                            String url = types[0].getURL();
                            if (url != null) {
                                String query = Uri.encode(url, "UTF-8");
                                if (query != null) {
                                    Intent browserIntent = new Intent(Intent.CATEGORY_BROWSABLE, Uri.parse(Uri.decode(query)));
                                    browserIntent.setAction(Intent.ACTION_VIEW);
                                    startActivity(browserIntent);
                                }
                            }
                            return true;
                        }
                        mTosCheckBox.setPressed(true);
                        break;
                    case MotionEvent.ACTION_UP:
                        mTosCheckBox.setChecked(!mTosCheckBox.isChecked());
                        mTosCheckBox.setPressed(false);
                        break;
                    default:
                        mTosCheckBox.setPressed(false);
                        break;
                }
                return true;
            }
        });

        registerButton.setOnClickListener(v -> onClickRegister_button(v));

        Button googleSignInButton = view.findViewById(R.id.google_sign_in_button);
        googleSignInButton.setOnClickListener(v -> showGoogleSignInConfirmationDialog());

        Log.d(TAG, "Observers set");

        // Initialize views and set onClick listeners if needed
        Button haveAccountButton = view.findViewById(R.id.have_an_account_button);
        haveAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController navController = NavHostFragment.findNavController(RegisterFragment.this);
                navController.navigate(R.id.action_registerFragment_to_loginFragment);
            }
        });

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        hideProgressDialog();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hideProgressDialog();
        if (activationStatusObserver != null) {
            sharedViewModel.getActivationStatus().removeObserver(activationStatusObserver);
        }
    }

    public void onClickRegister_button(View view) {
        attemptRegister();
    }

    public void attemptRegister() {
        Log.d(TAG, "attemptRegister");
        // Store values at the time of the login attempt.
        String mUser = mEmailView.getText().toString().trim();
        String mPassword = mPasswordView.getText().toString().trim();

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        CheckBox mCheckRulesRegsBox = getView().findViewById(R.id.checkRulesRegs);
        CheckBox mCheckNewsletterBox = getView().findViewById(R.id.checkNewsletter);
        int mSubscribeToNewsletter = mCheckNewsletterBox.isChecked() ? 1 : 0;

        boolean cancel = false;
        View focusView = null;

        if (!mCheckRulesRegsBox.isChecked()) {
            mCheckRulesRegsBox.setError(getString(R.string.error_field_required));
            cancel = true;
        }

        if (TextUtils.isEmpty(mUser)) {
            mEmailView.setError(getString(R.string.error_field_required));
            cancel = true;
        }

        if (TextUtils.isEmpty(mPassword)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            cancel = true;
        } else if (mPassword.length() < 4) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            cancel = true;
        }

        if (!cancel) {
            mRegisterStatusMessageView.setText(R.string.register_process_registering);
            showProgress(true);
            sharedViewModel.register(mUser, mPassword, mSubscribeToNewsletter);

        }
    }

    private void showProgress(final boolean show) {
        getActivity().runOnUiThread(() -> {
            mRegisterStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
            mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        });
    }

    public void observeActivationStatusAfterRegistration() {
        Log.d(TAG, "observeActivationStatusAfterRegistration");
        // Show the progress dialog
        showProgressDialog();

        activationStatusObserver = new Observer<ActivationStatus>() {
            @Override
            public void onChanged(ActivationStatus activationStatus) {
                Log.d(TAG, "observeActivationStatusAfterRegistration.onChanged, activationStatus: " + activationStatus);
                if (ActivationStatus.Active == activationStatus) {
                    // Hide the progress dialog
                    hideProgressDialog();

                    sharedViewModel.getActivationStatus().removeObserver(this);
                    Log.d(TAG, "Activation status: Active, triggering navigation update in SharedViewModel");
                    sharedViewModel.updateTargetFragment();
                } else {
                    // TODO: eventually this should be handled by sharedViewModel instead
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sharedViewModel.updateActivationStatus();
                        }
                    }, 2000); // Delay for 2 seconds
                }
            }
        };

        sharedViewModel.getActivationStatus().observeForever(activationStatusObserver);
    }

    private void showProgressDialog() {
        Log.d(TAG, "showProgressDialog");
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage(getString(R.string.account_created_succesfully));
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false); // Prevent the user from canceling the dialog
        }
        progressDialog.show();
    }

    private void hideProgressDialog() {
        Log.d(TAG, "hideProgressDialog");
        boolean isAdded = isAdded();
        if (isAdded && progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        } else {
            Log.d(TAG, "cant hide progressDialog - isAdded: " + isAdded + " progressDialog is null: " + (progressDialog == null) + " progressDialog isShowing: " + (progressDialog != null && progressDialog.isShowing()));
        }
    }

    public void finishRegistration(String token) {
        Log.d(TAG, "finishRegistration, token: " + token);
        if (token != null) {
            observeActivationStatusAfterRegistration();
        } else if (!isRemoving()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Registration Error")
                    .setMessage("Registration failed. Please try again.")
                    .setCancelable(false)
                    .setPositiveButton("OK", (dialog, id) -> {
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        ActivationStatus activationStatus = sharedViewModel.getActivationStatus().getValue();
        Log.d(TAG, "onResume - activationStatus: " + activationStatus);
        if (activationStatus == ActivationStatus.Inactive) {
            showProgressDialog();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        hideProgressDialog();
    }

    @SuppressWarnings("deprecation")
    private Spanned fromHtml(String html) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(html);
        }
    }

    private void signInWithGoogle(boolean subscribeToNewsletter) {
        // Generate a nonce
        String nonce = Base64.encodeToString(
                new SecureRandom().generateSeed(32),
                Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP
        );

        // Create Google ID option
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("544016459596-gj2llvp27l9fido43cpcuncs4i2ahqfg.apps.googleusercontent.com")
                .setAutoSelectEnabled(true)
                .setNonce(nonce)
                .build();

        // Build the Credential Request
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        // Request Credentials (Callback-based)
        CancellationSignal cancellationSignal = new CancellationSignal();
        credentialManager.getCredentialAsync(
                requireContext(),
                request,
                cancellationSignal,
                Runnable::run, // Executor
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleSignIn(result, subscribeToNewsletter);
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        handleFailure(e); // Show error to the user
                    }
                });
    }


    private void handleSignIn(GetCredentialResponse result, boolean subscribeToNewsletter) {
        try {
            CustomCredential credential = (CustomCredential) result.getCredential();
            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());
                String idToken = googleIdTokenCredential.getIdToken();
                Log.d(TAG, "Google ID Token: " + idToken);
                mRegisterStatusMessageView.setText(R.string.register_process_registering);
                showProgress(true);
                // Initiate registration with Google token and newsletter preference
                sharedViewModel.registerWithGoogleToken(idToken, subscribeToNewsletter ? 1 : 0);
            } else {
                Log.e(TAG, "Unexpected credential type: " + credential.getType());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing Google ID Token", e);
            showRegistrationError(); // Handle parsing failure
        }
    }


    private void showRegistrationError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Registration Error")
                .setMessage("Failed to register using Google Sign-In. Please try again.")
                .setPositiveButton("OK", null)
                .show();
    }



    private void handleFailure(Exception e) {
        Log.e(TAG, "Google Sign-In failed", e);

        if (e instanceof androidx.credentials.exceptions.NoCredentialException) {
            // Specific message for NoCredentialException
            Toast.makeText(
                    requireContext(),
                    "No credentials found. Please ensure your account is set up correctly.",
                    Toast.LENGTH_LONG
            ).show();
        } else {
            // Generic fallback message
            Toast.makeText(
                    requireContext(),
                    "An error occurred during Google Sign-In. Please try again later.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }
    private void showGoogleSignInConfirmationDialog() {
        // Inflate the dialog layout
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_google_sign_in_confirmation, null);

        // Get references to dialog elements
        TextView tosTextWithLinks = dialogView.findViewById(R.id.tos_text_with_links);
        CheckBox checkDialogTos = dialogView.findViewById(R.id.checkDialogTos);
        CheckBox checkDialogNewsletter = dialogView.findViewById(R.id.checkDialogNewsletter);
        Button dialogCancelButton = dialogView.findViewById(R.id.dialog_cancel_button);
        Button dialogConfirmButton = dialogView.findViewById(R.id.dialog_confirm_button);

        // Set up the ToS text with clickable links
        String tosHtml = Util.getRegisterInfo();
        tosTextWithLinks.setMovementMethod(LinkMovementMethod.getInstance());
        tosTextWithLinks.setText(fromHtml(tosHtml));
        tosTextWithLinks.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    CharSequence text = tosTextWithLinks.getText();
                    int offset = tosTextWithLinks.getOffsetForPosition(event.getX(), event.getY());
                    URLSpan[] spans = ((Spanned) text).getSpans(offset, offset, URLSpan.class);
                    if (spans.length > 0) {
                        String url = spans[0].getURL();
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(browserIntent);
                        return true;
                    }
                    checkDialogTos.setPressed(true);
                    break;
                case MotionEvent.ACTION_UP:
                    checkDialogTos.setPressed(false);
                    break;
                default:
                    checkDialogTos.setPressed(false);
                    break;
            }
            return true;
        });

        // Create the dialog
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Handle Cancel button
        dialogCancelButton.setOnClickListener(v -> dialog.dismiss());

        // Handle Confirm button
        dialogConfirmButton.setOnClickListener(v -> {
            if (!checkDialogTos.isChecked()) {
                Toast.makeText(requireContext(), "You must accept the Terms of Service to continue.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Pass the newsletter subscription preference to Google Sign-In logic
            boolean subscribeToNewsletter = checkDialogNewsletter.isChecked();

            // Launch Google Sign-In
            dialog.dismiss();
            signInWithGoogle(subscribeToNewsletter);
        });

        dialog.show();
    }


    private void validateIdTokenWithBackend(String idToken) {
        Log.d(TAG, "Validating ID Token: " + idToken);
        // Send the ID token to your backend or Firebase for validation and authentication
        // Example for Firebase:

    }
}
