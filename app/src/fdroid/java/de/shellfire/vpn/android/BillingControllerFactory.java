package de.shellfire.vpn.android;

import android.content.Context;

public class BillingControllerFactory {
    public static BillingController create(Context context) {
        return new StubBillingController(context);
    }
}
