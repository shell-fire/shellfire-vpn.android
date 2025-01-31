package de.shellfire.vpn.android.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * Created by Alina on 04.01.2018.
 */

public class CommonUtils {
    private static final String TAG = "CommonUtils";

    private CommonUtils() {
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    public static boolean isTablet(Context context) {
        return context != null && ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE);
    }

    /**
     * Convert pixel value to dp.
     *
     * @param context Context to access resources and device-specific display metrics
     * @param px      Value in pixels to be converted to dp
     * @return Converted value in dp
     */
    public static int convertPxToDp(Context context, float px) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                px,
                context.getResources().getDisplayMetrics()
        );
    }

    /**
     * Convert dp value to pixels.
     *
     * @param context Context to access resources and device-specific display metrics
     * @param dp      Value in dp to be converted to pixels
     * @return Converted value in pixels
     */
    public static int convertDpToPx(Context context, int dp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return dp * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }
}
