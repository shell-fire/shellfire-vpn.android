package de.shellfire.vpn.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import de.shellfire.vpn.android.utils.CommonUtils;
import de.shellfire.vpn.android.viewmodel.SharedViewModel;
import de.shellfire.vpn.android.viewmodel.SharedViewModelFactory;

public class RoutingActivity extends AppCompatActivity {

    private static final String TAG = "RoutingActivity";
    public static boolean loaded = false;
    private SharedViewModel sharedViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Handle the splash screen transition.
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate(savedInstanceState=" + savedInstanceState + ")");
        loaded = false;
        splashScreen.setKeepOnScreenCondition(() -> !loaded);

        // Initialize the SharedViewModel
        sharedViewModel = new ViewModelProvider(this, new SharedViewModelFactory(getApplicationContext()))
                .get(SharedViewModel.class);

        // Handle the intent when the activity is first created
        handleIntent(getIntent());

        // Show splash screen and proceed
        showSplash();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent: " + intent);

        // If a new Intent arrives, re-init splash logic
        loaded = false;
        showSplash();
        // Optionally re-handle the intent if needed
        // handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Log.d(TAG, "handleIntent: " + intent);

        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data != null) {
                String url = data.toString();
                Log.d("RoutingActivity", "Received URL: " + url);
                // Handle deep links, etc.
            }
        }
    }

    /**
     * UPDATED METHOD: Removed the background Thread + CountDownLatch and directly observe LiveData.
     * This prevents blocking the main thread (and avoids potential ANR).
     */
    private void showSplash() {
        Log.d(TAG, "showSplash");
        LiveData<Boolean> initializationFinished = sharedViewModel.initializeAfterStartup();

        initializationFinished.observe(this, isInitialized -> {
            Log.d(TAG, "Initialization finished observed: " + isInitialized);
            // Mark loaded once initialization completes
            loaded = true;

            // Now transition away from the splash to the main activity
            if (CommonUtils.isTablet(RoutingActivity.this)) {
                Log.d(TAG, "showSplash: isTablet");
                startActivity(new Intent(RoutingActivity.this, MainTabletActivity.class));
            } else {
                Log.d(TAG, "showSplash: isPhone");
                startActivity(new Intent(RoutingActivity.this, MainPhoneActivity.class));
            }
            finish();
        });
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult - requestCode: " + requestCode
                + " resultCode: " + resultCode
                + " data: " + data);
    }
}
