package de.shellfire.vpn.android;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

public class VpnPreferences {

    protected final static String REMEMBERED_VPN_SELECTION = "REMEMBERED_VPN_SELECTION";
    protected final static String CONNECTION_MODE_SELECTION = "CONNECTION_MODE_SELECTION";
    protected final static String CONNECTION_MODE_SELECTION_CHANGED = "CONNECTION_MODE_SELECTION_CHANGED";
    protected final static String SHOW_LOG = "SHOW_LOG";
    protected final static String IGNORE_NEW_VERSION_WARNING = "IGNORE_NEW_VERSION_WARNING";

    protected final static String USER = "user";
    protected final static String PASS = "pass";
    protected final static String TOKEN = "token";
    protected final static String VPN_PRODUCT_ID = "vpnProductId";
    private final static String MOST_RECENT_AUTOSWITCH_FROM_UDP_TO_TCP_DATE = "most_recent_autoswitch_from_udp_to_tcp_date";

    private static boolean isTestMode = false;

    public static boolean getIsTestMode() {
        return isTestMode;
    }

    public static void setIsTestMode(boolean b) {
        isTestMode = b;
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        if (ShellfireApplication.getIsTestMode()) {
            return new MockSharedPreferences();
        } else {
            return PreferenceManager.getDefaultSharedPreferences(context);
        }
    }

    public static int getRememberedVpnSelection(Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        int result = sp.getInt(REMEMBERED_VPN_SELECTION, 0);
        Log.d("VpnPreferences", "getRememberedVpnSelection: " + result);
        return result;
    }

    public static void setRememberedVpnSelection(Context context, Integer value) {
        Log.d("VpnPreferences", "setRememberedVpnSelection: " + value);
        SharedPreferences sp = getSharedPreferences(context);
        Editor edit = sp.edit();
        if (value == null) {
            edit.remove(REMEMBERED_VPN_SELECTION);
        } else {
            edit.putInt(REMEMBERED_VPN_SELECTION, value);
        }

        edit.apply();
    }

    public static void setConnectionModeSelection(Context context, Protocol protocol) {
        if (protocol != null) {
            SharedPreferences sp = getSharedPreferences(context);
            Editor edit = sp.edit();
            edit.putString(CONNECTION_MODE_SELECTION, protocol.name());
            edit.apply();
        }
    }

    public static Protocol getConnectionModeSelection(Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        String connectionModeString = sp.getString(CONNECTION_MODE_SELECTION, null);
        return connectionModeString != null ? Protocol.valueOf(connectionModeString) : Protocol.UDP;
    }

    public static void setConnectionProtocolChanged(Context context, boolean changed) {
        SharedPreferences sp = getSharedPreferences(context);
        Editor edit = sp.edit();
        edit.putBoolean(CONNECTION_MODE_SELECTION_CHANGED, changed);
        edit.apply();
    }

    public static boolean getConnectionProtocolChanged(Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        return sp.getBoolean(CONNECTION_MODE_SELECTION_CHANGED, false);
    }

    public static boolean getShowLog(Activity activity) {
        SharedPreferences sp = getSharedPreferences(activity);
        return sp.getBoolean(SHOW_LOG, true);
    }

    public static void setShowLog(Activity activity, Boolean bol) {
        SharedPreferences sp = getSharedPreferences(activity);
        Editor edit = sp.edit();

        if (bol == null)
            edit.remove(SHOW_LOG);
        else
            edit.putBoolean(SHOW_LOG, bol);

        edit.apply();
    }

    public static boolean getIgnoreNewVersionWarning(Activity activity) {
        SharedPreferences sp = getSharedPreferences(activity);
        return sp.getBoolean(IGNORE_NEW_VERSION_WARNING, false);
    }

    public static void setIgnoreNewVersionWarning(Activity activity, Boolean bol) {
        SharedPreferences sp = getSharedPreferences(activity);
        Editor edit = sp.edit();

        if (bol == null)
            edit.remove(IGNORE_NEW_VERSION_WARNING);
        else
            edit.putBoolean(IGNORE_NEW_VERSION_WARNING, bol);

        edit.apply();
    }

    public static String getUser(Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        return sp.getString(USER, null);
    }

    public static String getPASS(Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        return sp.getString(PASS, null);
    }

    public static int getVpnProductId(Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        return sp.getInt(VPN_PRODUCT_ID, 0);
    }

    public static void setUser(Context context, String user) {
        SharedPreferences sp = getSharedPreferences(context);
        Editor edit = sp.edit();
        edit.putString(USER, user);
        edit.apply();
    }

    public static void setToken(Context context, String token) {
        SharedPreferences sp = getSharedPreferences(context);
        Editor edit = sp.edit();
        edit.putString(TOKEN, token);
        edit.apply();
    }

    public static void setPass(Context context, String pass) {
        SharedPreferences sp = getSharedPreferences(context);
        Editor edit = sp.edit();
        edit.putString(PASS, pass);
        edit.apply();
    }

    public static void setVpnProductId(Context context, int vpnProductId) {
        SharedPreferences sp = getSharedPreferences(context);
        Editor edit = sp.edit();
        edit.putInt(VPN_PRODUCT_ID, vpnProductId);
        edit.apply();
    }

    public static void setMostRecentAutoSwitchFromUdpToTcpDate(Context context) {
        long currentTimeMills = System.currentTimeMillis();

        SharedPreferences sp = getSharedPreferences(context);
        Editor edit = sp.edit();
        edit.putLong(MOST_RECENT_AUTOSWITCH_FROM_UDP_TO_TCP_DATE, currentTimeMills);
        edit.apply();
    }

    static void unsetMostRecentAutoSwitchFromUdpToTcpDate(Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        Editor edit = sp.edit();
        edit.putLong(MOST_RECENT_AUTOSWITCH_FROM_UDP_TO_TCP_DATE, 0);
        edit.apply();
    }

    public static long getMostRecentAutoSwitchFromUdpToTcpDate(Context context) {
        SharedPreferences sp = getSharedPreferences(context);
        return sp.getLong(MOST_RECENT_AUTOSWITCH_FROM_UDP_TO_TCP_DATE, 0);
    }

    public enum Language {
        ENGLISH("english"),
        DEUTSCH("deutsch"),
        FRENCH("français"),
        SPANISH("español"),
        TURKISH("türkçe"),
        ARABIC("العربية"),
        ITALIAN("italiano"),
        PORTUGUESE("português");

        private final String name;

        Language(final String name) {
            this.name = name;
        }

        public static Language getLanguageForValue(String val) {
            switch (val.toLowerCase()) {
                case "deutsch":
                    return DEUTSCH;
                case "français":
                    return FRENCH;
                case "english":
                    return ENGLISH;
                case "español":
                    return SPANISH;
                case "türkçe":
                    return TURKISH;
                case "العربية":
                    return ARABIC;
                case "italiano":
                    return ITALIAN;
                case "português":
                    return PORTUGUESE;
                default:
                    return ENGLISH;
            }
        }

        @NotNull
        @Override
        public String toString() {
            return name;
        }
    }
}
