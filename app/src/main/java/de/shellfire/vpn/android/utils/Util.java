package de.shellfire.vpn.android.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileReader;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

import javax.net.ssl.SSLException;

import de.shellfire.vpn.android.R;
import de.shellfire.vpn.android.ServerType;
import de.shellfire.vpn.android.VpnPreferences;
import de.shellfire.vpn.android.openvpn.VpnStatus;

public class Util {

    private static final String TAG = "Util";

    private Util() {
    }

    public static int getServerTypeResId(ServerType serverType) {

        switch (serverType) {
            case Free:
            default:
                return R.string.default_product_type;
            case Premium:
                return R.string.premium;
            case PremiumPlus:
                return R.string.premiumplus;

        }
    }
    public static String fileToString(String filePath) {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder.append(sCurrentLine).append("\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading file", e);
        }
        return contentBuilder.toString();
    }

    public static boolean isInternetAvailable(Activity activity) {
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

    public static void handleException(final Exception e, Activity mainActivity) {
        VpnStatus.logException(e);

        if (e instanceof UnknownHostException || e instanceof EOFException || e instanceof SocketTimeoutException || e instanceof SSLException) {
            mainActivity.runOnUiThread(new Runnable() {
                public void run() {
                    AlertDialog.Builder alert = new AlertDialog.Builder(mainActivity);
                    alert.setTitle(mainActivity.getString(R.string.network_problem_title));
                    alert.setMessage(mainActivity.getString(R.string.network_problem_message) + "\n\n" + e);
                    alert.setPositiveButton(mainActivity.getString(android.R.string.ok), null);
                    alert.show();
                }
            });
        }
    }

    public static String getRegisterInfo() {
        String localeLanguage = Locale.getDefault().getDisplayLanguage();
        VpnPreferences.Language myLang = VpnPreferences.Language.getLanguageForValue(localeLanguage);
        String res;
        if (myLang == VpnPreferences.Language.DEUTSCH) {
            res = "Ich akzeptiere die <a target='_agb' href='https://www.shellfire.de/agb/?&utm_nooverride=1'>AGB</a> und habe die <a target='_datenschutzerklaerung' href='https://www.shellfire.de/datenschutzerklaerung/?&utm_nooverride=1'>Datenschutzerklärung</a> sowie das <a target='_widerrufsrecht' href='https://www.shellfire.de/widerrufsrecht/?&utm_nooverride=1'>Widerrufsrecht</a> zur Kenntnis genommen.";
        } else if (myLang == VpnPreferences.Language.FRENCH) {
            res = "J'accepte les <a target='_agb' href='https://www.shellfire.fr/agb/?&utm_nooverride=1'>CGV</a> et j'ai pris connaissance de la <a target='_datenschutzerklaerung' href='https://www.shellfire.fr/datenschutzerklaerung/?&utm_nooverride=1'>déclaration de protection de données</a> et des <a target='_widerrufsrecht' href='https://www.shellfire.fr/widerrufsrecht/?&utm_nooverride=1'>avis de rétractationa</a>.";
        } else if (myLang == VpnPreferences.Language.SPANISH) {
            res = "Acepto los <a target='_agb' href='https://www.shellfire.net/agb/?&utm_nooverride=1'>Términos y Condiciones</a> y he leído <a target='_datenschutzerklaerung' href='https://www.shellfire.net/datenschutzerklaerung/?&utm_nooverride=1'>la política de privacidad</a> y <a target='_widerrufsrecht' href='https://www.shellfire.net/widerrufsrecht/?&utm_nooverride=1'>el aviso de retiro</a>.";
        } /*else if (myLang == VpnPreferences.Language.TURKISH) {
                res = "Ben <a target='_agb' href='https://www.shellfire.net/agb/?&utm_nooverride=1'>Şartlar ve Koşullar</a> kabul ediyorum ve <a target='_datenschutzerklaerung' href='https://www.shellfire.net/datenschutzerklaerung/?&utm_nooverride=1'>Gizlilik Politikası</a> ve <a target='_widerrufsrecht' href='https://www.shellfire.net/widerrufsrecht/?&utm_nooverride=1'>Çekilme</a> haber okudum.";
            } else if (myLang == VpnPreferences.Language.ARABIC) {
                res = "أوافق على شروط وأحكام و قرأت سياسة الخصوصية وإشعار الانسحاب: <a target='_agb' href='https://www.shellfire.net/agb/?&utm_nooverride=1'>الشروط والأحكام</AGB>, <a target='_datenschutzerklaerung' href='https://www.shellfire.net/datenschutzerklaerung/?&utm_nooverride=1'>بيان الخصوصية</a>, <a target='_widerrufsrecht' href='https://www.shellfire.net/widerrufsrecht/?&utm_nooverride=1'>انسحاب</a>";
            } */ else {
            // default to english
            res = "I accept the <a target='_agb' href='https://www.shellfire.net/agb/?&utm_nooverride=1'>Terms & Conditions</a> and take note of the <a target='_datenschutzerklaerung' href='https://www.shellfire.net/datenschutzerklaerung/?&utm_nooverride=1'>Privacy Statement</a> and the <a target='_widerrufsrecht' href='https://www.shellfire.net/widerrufsrecht/?&utm_nooverride=1'>Right of Withdrawal</a>.";
        }

        return res;
    }

    public static String escapeJson(String str) {
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < ' ' || c > '~') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    public static void logCurrentStackTrace(String tag) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder();
        sb.append("Current stack trace:\n");
        for (int i = 3; i < stackTrace.length; i++) { // Starting at index 3 to skip the getStackTrace() method and its call in logCurrentStackTrace()
            sb.append(stackTrace[i].toString());
            sb.append("\n");
        }
        Log.d(tag, sb.toString());
    }

    public static String formatPrice(double amount, String currencyCode) {
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault());
        currencyFormatter.setCurrency(Currency.getInstance(currencyCode));
        return currencyFormatter.format(amount);
    }
}
