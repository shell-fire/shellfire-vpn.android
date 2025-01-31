package de.shellfire.vpn.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class TransparentActivity extends Activity {

    private static final String TAG = "TransparentActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve the VPN intent passed from the VpnConnectionManager
        Intent vpnIntent = getIntent().getParcelableExtra("vpn_intent");
        if (vpnIntent != null) {
            startActivityForResult(vpnIntent, MainBaseActivity.START_VPN_PROFILE);
        } else {
            // VPN is already prepared or something went wrong
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MainBaseActivity.START_VPN_PROFILE) {
            if (resultCode == RESULT_OK) {
                // VPN is prepared, you can proceed with your VPN connection
                Log.d(TAG, "VPN is prepared, proceed with VPN connection");
                VpnConnectionManager vpnConnectionManager = VpnConnectionManager.getInstance(this.getApplicationContext());
                vpnConnectionManager.handleActivityResult(resultCode);
            } else {
                Log.d(TAG, "VPN preparation failed or was cancelled by the user");
            }
            finish();
        }
    }
}
