package de.shellfire.vpn.android.openvpn;

import android.content.Context;
import android.content.Intent;

public class VariantConfig {
    static Intent getOpenUrlIntent(Context c) {
        return new Intent(Intent.ACTION_VIEW);
    }

}