/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.shellfire.vpn.android.openvpn;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.security.KeyChain;
import android.security.KeyChainException;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.spongycastle.util.io.pem.PemObject;
import org.spongycastle.util.io.pem.PemWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.Vector;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import de.blinkt.openvpn.core.NativeUtils;
import de.shellfire.vpn.android.R;
import de.shellfire.vpn.android.webservice.model.WsFile;

public class VpnProfile implements Serializable, Cloneable {
    // Note that this class cannot be moved to core where it belongs since
    // the profile loading depends on it being here
    // The Serializable documentation mentions that class name change are possible
    // but the how is unclear
    //
    public static final long MAX_EMBED_FILE_SIZE = 2048 * 1024; // 2048kB
    // Don't change this, not all parts of the program use this constant
    public static final String EXTRA_PROFILEUUID = "de.shellfire.vpn.android.profileUUID";
    public static final String INLINE_TAG = "[[INLINE]]";
    public static final String DISPLAYNAME_TAG = "[[NAME]]";
    public static final int MAXLOGLEVEL = 4;
    public static final int CURRENT_PROFILE_VERSION = 9;
    public static final int DEFAULT_MSSFIX_SIZE = 1280;
    public static final int TYPE_CERTIFICATES = 0;
    public static final int TYPE_PKCS12 = 1;
    public static final int TYPE_KEYSTORE = 2;
    public static final int TYPE_USERPASS = 3;
    public static final int TYPE_STATICKEYS = 4;
    public static final int TYPE_USERPASS_CERTIFICATES = 5;
    public static final int TYPE_USERPASS_PKCS12 = 6;
    public static final int TYPE_USERPASS_KEYSTORE = 7;
    public static final int TYPE_EXTERNAL_APP = 8;
    public static final int X509_VERIFY_TLSREMOTE = 0;
    public static final int X509_VERIFY_TLSREMOTE_COMPAT_NOREMAPPING = 1;
    public static final int X509_VERIFY_TLSREMOTE_DN = 2;
    public static final int X509_VERIFY_TLSREMOTE_RDN = 3;
    public static final int X509_VERIFY_TLSREMOTE_RDN_PREFIX = 4;
    public static final int AUTH_RETRY_NONE_FORGET = 0;
    public static final int AUTH_RETRY_NOINTERACT = 2;
    public static final boolean mIsOpenVPN22 = false;
    private static final long serialVersionUID = 7085688938959334563L;
    private static final int AUTH_RETRY_NONE_KEEP = 1;
    private static final int AUTH_RETRY_INTERACT = 3;
    private static final String EXTRA_RSA_PADDING_TYPE = "de.shellfire.vpn.android.api.RSA_PADDING_TYPE";
    private static final String TAG = "VpnProfile";
    public static final String DEFAULT_DNS1 = "8.8.8.8";
    public static final String DEFAULT_DNS2 = "8.8.4.4";
    private final String mParams;
    private final List<WsFile> mCertList;
    // variable named wrong and should haven beeen transient
    // but needs to keep wrong name to guarante loading of old
    // profiles
    public final transient boolean profileDeleted = false;
    public final int mAuthenticationType = TYPE_KEYSTORE;
    public String mName;
    public String mAlias;
    public String mClientCertFilename;
    public String mTLSAuthDirection = "";
    public String mTLSAuthFilename;
    public String mClientKeyFilename;
    public String mCaFilename;
    public boolean mUseLzo = false;
    public String mPKCS12Filename;
    public String mPKCS12Password;
    public final boolean mUseTLSAuth = false;
    public String mDNS1 = DEFAULT_DNS1;
    public String mDNS2 = DEFAULT_DNS2;
    public String mIPv4Address;
    public String mIPv6Address;
    public boolean mOverrideDNS = false;
    public String mSearchDomain = "shellfire.de";
    public boolean mUseDefaultRoute = true;
    public boolean mUsePull = true;
    public String mCustomRoutes;
    public boolean mCheckRemoteCN = true;
    public boolean mExpectTLSCert = false;
    public String mRemoteCN = "";
    public String mPassword = "";
    public String mUsername = "";
    public boolean mRoutenopull = false;
    public boolean mUseRandomHostname = false;
    public boolean mUseFloat = false;
    public boolean mUseCustomConfig = false;
    public String mCustomConfigOptions = "";
    public String mVerb = "1";  //ignored
    public final String mCipher = "";
    public boolean mNobind = true;
    public boolean mUseDefaultRoutev6 = true;
    public String mCustomRoutesv6 = "";
    public final String mKeyPassword = "";
    public boolean mPersistTun = false;
    public String mConnectRetryMax = "-1";
    public String mConnectRetry = "2";
    public String mConnectRetryMaxTime = "300";
    public boolean mUserEditable = true;
    public String mAuth = "";
    public final int mX509AuthType = X509_VERIFY_TLSREMOTE_RDN;
    public String mx509UsernameField = null;
    public boolean mAllowLocalLAN;
    public String mExcludedRoutes;
    public String mExcludedRoutesv6;
    public int mMssFix = 0; // -1 is default,
    public Connection[] mConnections = new Connection[0];
    public boolean mRemoteRandom = false;
    public HashSet<String> mAllowedAppsVpn = new HashSet<>();
    public boolean mAllowedAppsVpnAreDisallowed = true;
    public final boolean mAllowAppVpnBypass = false;
    public String mCrlFilename;
    public String mProfileCreator;
    public String mExternalAuthenticator;
    public int mAuthRetry = AUTH_RETRY_NONE_FORGET;
    public int mTunMtu;
    public boolean mPushPeerInfo = false;
    public int mVersion = 0;
    // timestamp when the profile was last used
    public long mLastUsed;
    public String importedProfileHash;
    /* Options no longer used in new profiles */
    public String mServerName = "openvpn.example.com";
    public final String mServerPort = "1194";
    public final boolean mUseUdp = true;
    public boolean mTemporaryProfile = false;
    public String mDataCiphers = "";
    public boolean mBlockUnusedAddressFamilies = true;
    public final boolean mCheckPeerFingerprint = false;
    public String mPeerFingerPrints = "";
    private transient PrivateKey mPrivateKey;
    // Public attributes, since I got mad with getter/setter
    // set members to default values
    private UUID mUuid;
    private int mProfileVersion;

    public VpnProfile(String params, List<WsFile> certs) {
        mUuid = UUID.randomUUID();

        String fileName = certs.get(1).getName();
        mName = fileName.substring(0, fileName.indexOf("."));
        //mName = certs.get(0).getName();
        mParams = params;
        this.mCertList = certs;

    }

    public static String openVpnEscape(String unescaped) {
        if (unescaped == null)
            return null;
        String escapedString = unescaped.replace("\\", "\\\\");
        escapedString = escapedString.replace("\"", "\\\"");
        escapedString = escapedString.replace("\n", "\\n");

        if (escapedString.equals(unescaped) && !escapedString.contains(" ") &&
                !escapedString.contains("#") && !escapedString.contains(";")
                && !escapedString.equals(""))
            return unescaped;
        else
            return '"' + escapedString + '"';
    }

    public static boolean doUseOpenVPN3(Context c) {
        return false;
    }

    //! Put inline data inline and other data as normal escaped filename
    public static String insertFileData(String cfgentry, String filedata) {
        if (filedata == null) {
            return String.format("%s %s\n", cfgentry, "file missing in config profile");
        } else if (isEmbedded(filedata)) {
            String dataWithOutHeader = getEmbeddedContent(filedata);
            return String.format(Locale.ENGLISH, "<%s>\n%s\n</%s>\n", cfgentry, dataWithOutHeader, cfgentry);
        } else {
            return String.format(Locale.ENGLISH, "%s %s\n", cfgentry, openVpnEscape(filedata));
        }
    }

    public static String getDisplayName(String embeddedFile) {
        int start = DISPLAYNAME_TAG.length();
        int end = embeddedFile.indexOf(INLINE_TAG);
        return embeddedFile.substring(start, end);
    }

    public static String getEmbeddedContent(String data) {
        if (!data.contains(INLINE_TAG))
            return data;

        int start = data.indexOf(INLINE_TAG) + INLINE_TAG.length();
        return data.substring(start);
    }

    public static boolean isEmbedded(String data) {
        if (data == null)
            return false;
        return data.startsWith(INLINE_TAG) || data.startsWith(DISPLAYNAME_TAG);
    }

    static public String getVersionEnvString(Context c) {
        String version = "unknown";
        try {
            PackageInfo packageinfo = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            version = packageinfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            VpnStatus.logException(e);
        }
        return String.format(Locale.US, "%s %s", c.getPackageName(), version);

    }

    public static String implode(String separator, String... data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length - 1; i++) {
            //data.length - 1 => to not add separator at the end
            if (!data[i].matches(" *")) {//empty string are ""; " "; "  "; and so on
                sb.append(data[i]);
                sb.append(separator);
            }
        }
        sb.append(data[data.length - 1].trim());
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VpnProfile) {
            VpnProfile vpnProfile = (VpnProfile) obj;
            return mUuid.equals(vpnProfile.mUuid);
        } else {
            return false;
        }
    }

    public void clearDefaults() {
        mServerName = "unknown";
        mUsePull = false;
        mUseLzo = false;
        mUseDefaultRoute = false;
        mUseDefaultRoutev6 = false;
        mExpectTLSCert = false;
        mCheckRemoteCN = false;
        mPersistTun = false;
        mAllowLocalLAN = true;
        mPushPeerInfo = false;
        mMssFix = 0;
        mNobind = false;
    }

    public UUID getUUID() {
        return mUuid;

    }

    // Only used for the special case of managed profiles
    public void setUUID(UUID uuid) {
        mUuid = uuid;
    }

    public String getName() {
        if (TextUtils.isEmpty(mName))
            return "No profile name";
        return mName;
    }

    public void upgradeProfile() {

        /* Fallthrough is intended here */
        switch (mProfileVersion) {
            case 0:
            case 1:
                /* default to the behaviour the OS used */
                mAllowLocalLAN = Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT;
            case 2:
            case 3:
                moveOptionsToConnection();
                mAllowedAppsVpnAreDisallowed = true;

                if (mAllowedAppsVpn == null)
                    mAllowedAppsVpn = new HashSet<>();

                if (mConnections == null)
                    mConnections = new Connection[0];
            case 4:
            case 5:

                if (TextUtils.isEmpty(mProfileCreator))
                    mUserEditable = true;
            case 6:
                for (Connection c : mConnections)
                    if (c.mProxyType == null)
                        c.mProxyType = Connection.ProxyType.NONE;
            case 7:
                if (mAllowAppVpnBypass)
                    mBlockUnusedAddressFamilies = false;
            case 8:
                if (!TextUtils.isEmpty(mCipher) && !mCipher.equals("AES-256-GCM") && !mCipher.equals("AES-128-GCM")) {
                    mDataCiphers = "AES-256-GCM:AES-128-GCM:" + mCipher;
                }
            default:
        }

        mProfileVersion = CURRENT_PROFILE_VERSION;

    }

    private void moveOptionsToConnection() {
        mConnections = new Connection[1];
        Connection conn = new Connection();

        conn.mServerName = mServerName;
        conn.mServerPort = mServerPort;
        conn.mUseUdp = mUseUdp;
        conn.mCustomConfiguration = "";

        mConnections[0] = conn;

    }

    public String getConfigFile(Context context, boolean configForOvpn3) {
        return buildOpenvpnArgv(context);
    }

    public String getPlatformVersionEnvString() {
        return String.format(Locale.US, "%d %s %s %s %s %s", Build.VERSION.SDK_INT, Build.VERSION.RELEASE,
                NativeUtils.getNativeAPI(), Build.BRAND, Build.BOARD, Build.MODEL);
    }

    @NonNull
    private Collection<String> getCustomRoutes(String routes) {
        Vector<String> cidrRoutes = new Vector<>();
        if (routes == null) {
            // No routes set, return empty vector
            return cidrRoutes;
        }
        for (String route : routes.split("[\n \t]")) {
            if (!route.equals("")) {
                String cidrroute = cidrToIPAndNetmask(route);
                if (cidrroute == null)
                    return cidrRoutes;

                cidrRoutes.add(cidrroute);
            }
        }

        return cidrRoutes;
    }

    private Collection<String> getCustomRoutesv6(String routes) {
        Vector<String> cidrRoutes = new Vector<>();
        if (routes == null) {
            // No routes set, return empty vector
            return cidrRoutes;
        }
        for (String route : routes.split("[\n \t]")) {
            if (!route.equals("")) {
                cidrRoutes.add(route);
            }
        }

        return cidrRoutes;
    }

    private String cidrToIPAndNetmask(String route) {
        String[] parts = route.split("/");

        // No /xx, assume /32 as netmask
        if (parts.length == 1)
            parts = (route + "/32").split("/");

        if (parts.length != 2)
            return null;
        int len;
        try {
            len = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ne) {
            return null;
        }
        if (len < 0 || len > 32)
            return null;


        long nm = 0xffffffffL;
        nm = (nm << (32 - len)) & 0xffffffffL;

        String netmask = String.format(Locale.ENGLISH, "%d.%d.%d.%d", (nm & 0xff000000) >> 24, (nm & 0xff0000) >> 16, (nm & 0xff00) >> 8, nm & 0xff);
        return parts[0] + "  " + netmask;
    }

    public Intent prepareStartService(Context context) {
        Intent intent = getStartServiceIntent(context);

        return intent;
    }

    public void writeConfigFile(Context context) throws IOException {
        FileWriter cfg = new FileWriter(VPNLaunchHelper.getConfigFilePath(context));
        cfg.write(getConfigFile(context, false));
        cfg.flush();
        cfg.close();

    }

    public String getParams(Context context) {

        File cacheDir = context.getCacheDir();
        String params = "";

        // params += mParams.replace("%APPDATA%\\ShellfireVPN\\", filesDir.getAbsolutePath()+ "/");
        params += mParams.replace("%APPDATA%\\ShellfireVPN\\", cacheDir.getAbsolutePath() + "/").replace("\"", "");
        params = params.trim();
        params += " --management ";

        params += cacheDir.getAbsolutePath() + "/" + "mgmtsocket unix ";
        params += "--management-client --management-query-passwords --management-hold ";
        // --" + getVersionEnvString(context);

        //params += "--parsable-output true";



 /* flotodo: think about this
        if (mOverrideDNS || !mUsePull) {
            if (nonNull(mDNS1))
                cfg += "dhcp-option DNS " + mDNS1 + "\n";
            if (nonNull(mDNS2))
                cfg += "dhcp-option DNS " + mDNS2 + "\n";
            if (nonNull(mSearchDomain))
                cfg += "dhcp-option DOMAIN " + mSearchDomain + "\n";
        }
*/

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean usesystemproxy = prefs.getBoolean("usesystemproxy", true);
        if (usesystemproxy) {
            params += " --management-query-proxy";
        }

        //params = params.replace("--verb 3", "--verb 9");

        return params;
    }

    private String buildOpenvpnArgv(Context context) {
        File cacheDir = context.getCacheDir();
        Vector<String> args = new Vector<String>();

        String params = getParams(context);

        String[] allParams = params.split(" ");

        // Some parameters magic ahead.
        for (int i = 0; i < allParams.length; i++) {
            if (allParams[i].equals("--service")) {
                i += 2;
            } else if (allParams[i].equals("--redirect-gateway")) {
                i++;
            } else if (allParams[i].isEmpty()) {
                continue;
            } else {
                args.add(allParams[i]);
            }
        }
        String[] result = args.toArray(new String[args.size()]);

        String abspath = "";
        if (this.mCertList != null) {
            try {
                // write certificates
                for (WsFile file : this.mCertList) {
                    abspath = context.getCacheDir().getAbsolutePath() + "/" + file.getName();
                    VpnStatus.logInfo("writing file to: " + abspath);
                    FileWriter writer = new FileWriter(abspath);
                    writer.write(file.getContent());
                    writer.flush();
                    writer.close();

                    File f = new File(abspath);
                    if (!f.exists()) {
                        VpnStatus.logError("does not exist: " + abspath);
                    }
                }

            } catch (IOException e) {
                VpnStatus.logError("error while writing file to: " + abspath + " " + e.getMessage());
            }

        }

        return VpnProfile.implode("\n", result);
    }

    public Intent getStartServiceIntent(Context context) {
        String prefix = context.getPackageName();

        Intent intent = new Intent(context, OpenVPNService.class);
        intent.putExtra(prefix + ".profileUUID", mUuid.toString());
        intent.putExtra(prefix + ".profileVersion", mVersion);
        return intent;
    }

    public void checkForRestart(final Context context) {
        /* This method is called when OpenVPNService is restarted */

        if ((mAuthenticationType == VpnProfile.TYPE_KEYSTORE || mAuthenticationType == VpnProfile.TYPE_USERPASS_KEYSTORE)
                && mPrivateKey == null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    getExternalCertificates(context);

                }
            }).start();
        }
    }

    @NotNull
    @Override
    protected VpnProfile clone() throws CloneNotSupportedException {
        VpnProfile copy = (VpnProfile) super.clone();
        copy.mUuid = UUID.randomUUID();
        copy.mConnections = new Connection[mConnections.length];
        int i = 0;
        for (Connection conn : mConnections) {
            copy.mConnections[i++] = conn.clone();
        }
        copy.mAllowedAppsVpn = (HashSet<String>) mAllowedAppsVpn.clone();
        return copy;
    }

    public VpnProfile copy(String name) {
        try {
            VpnProfile copy = clone();
            copy.mName = name;
            return copy;

        } catch (CloneNotSupportedException e) {
            Log.e(TAG, "Could not copy profile", e);
            return null;
        }
    }

    private X509Certificate[] getKeyStoreCertificates(Context context) throws KeyChainException, InterruptedException {
        PrivateKey privateKey = KeyChain.getPrivateKey(context, mAlias);
        mPrivateKey = privateKey;


        X509Certificate[] caChain = KeyChain.getCertificateChain(context, mAlias);
        return caChain;
    }


    public String[] getExternalCertificates(Context context) {
        return getExternalCertificates(context, 5);
    }


    synchronized String[] getExternalCertificates(Context context, int tries) {
        // Force application context- KeyChain methods will block long enough that by the time they
        // are finished and try to unbind, the original activity context might have been destroyed.
        context = context.getApplicationContext();

        try {
            String keystoreChain = null;

            X509Certificate[] caChain;
            caChain = getKeyStoreCertificates(context);

            if (caChain == null)
                throw new NoCertReturnedException("No certificate returned from Keystore");

            if (caChain.length <= 1 && TextUtils.isEmpty(mCaFilename)) {
                VpnStatus.logMessage(VpnStatus.LogLevel.ERROR, "", context.getString(R.string.keychain_nocacert));
            } else {
                StringWriter ksStringWriter = new StringWriter();

                PemWriter pw = new PemWriter(ksStringWriter);
                for (int i = 1; i < caChain.length; i++) {
                    X509Certificate cert = caChain[i];
                    pw.writeObject(new PemObject("CERTIFICATE", cert.getEncoded()));
                }
                pw.close();
                keystoreChain = ksStringWriter.toString();
            }


            String caout = null;
            if (!TextUtils.isEmpty(mCaFilename)) {
                try {
                    Certificate[] cacerts = X509Utils.getCertificatesFromFile(mCaFilename);
                    StringWriter caoutWriter = new StringWriter();
                    PemWriter pw = new PemWriter(caoutWriter);

                    for (Certificate cert : cacerts)
                        pw.writeObject(new PemObject("CERTIFICATE", cert.getEncoded()));
                    pw.close();
                    caout = caoutWriter.toString();

                } catch (Exception e) {
                    VpnStatus.logError("Could not read CA certificate" + e.getLocalizedMessage());
                }
            }


            StringWriter certout = new StringWriter();


            if (caChain.length >= 1) {
                X509Certificate usercert = caChain[0];

                PemWriter upw = new PemWriter(certout);
                upw.writeObject(new PemObject("CERTIFICATE", usercert.getEncoded()));
                upw.close();

            }
            String user = certout.toString();


            String ca, extra;
            if (caout == null) {
                ca = keystoreChain;
                extra = null;
            } else {
                ca = caout;
                extra = keystoreChain;
            }

            return new String[]{ca, extra, user};
        } catch (InterruptedException | IOException | KeyChainException | NoCertReturnedException |
                 IllegalArgumentException
                 | CertificateException e) {

            VpnStatus.logError(R.string.keyChainAccessError, e.getLocalizedMessage());

            VpnStatus.logError(R.string.keychain_access);
            return null;

        } catch (AssertionError | NullPointerException e) {
            if (tries == 0)
                return null;
            VpnStatus.logError(String.format("Failure getting Keystore Keys (%s), retrying", e.getLocalizedMessage()));
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e1) {
                VpnStatus.logException(e1);
            }
            return getExternalCertificates(context, tries - 1);
        }

    }

    public int checkProfile(Context c) {
        return checkProfile(c, doUseOpenVPN3(c));
    }

    //! Return an error if something is wrong
    public int checkProfile(Context context, boolean useOpenVPN3) {
        if (mAuthenticationType == TYPE_KEYSTORE || mAuthenticationType == TYPE_USERPASS_KEYSTORE || mAuthenticationType == TYPE_EXTERNAL_APP) {
            if (mAlias == null)
                return R.string.no_keystore_cert_selected;
        } else if (mAuthenticationType == TYPE_CERTIFICATES || mAuthenticationType == TYPE_USERPASS_CERTIFICATES) {
            if (TextUtils.isEmpty(mCaFilename) && !mCheckPeerFingerprint)
                return R.string.no_ca_cert_selected;
        }

        if (mCheckRemoteCN && mX509AuthType == X509_VERIFY_TLSREMOTE)
            return R.string.deprecated_tls_remote;

        if (!mUsePull || mAuthenticationType == TYPE_STATICKEYS) {
            if (mIPv4Address == null || cidrToIPAndNetmask(mIPv4Address) == null)
                return R.string.ipv4_format_error;
        }
        if (!mUseDefaultRoute) {
            if (!TextUtils.isEmpty(mCustomRoutes) && getCustomRoutes(mCustomRoutes).isEmpty())
                return R.string.custom_route_format_error;

            if (!TextUtils.isEmpty(mExcludedRoutes) && getCustomRoutes(mExcludedRoutes).isEmpty())
                return R.string.custom_route_format_error;

        }

        if (mUseTLSAuth && TextUtils.isEmpty(mTLSAuthFilename))
            return R.string.missing_tlsauth;

        if ((mAuthenticationType == TYPE_USERPASS_CERTIFICATES || mAuthenticationType == TYPE_CERTIFICATES)
                && (TextUtils.isEmpty(mClientCertFilename) || TextUtils.isEmpty(mClientKeyFilename)))
            return R.string.missing_certificates;

        if ((mAuthenticationType == TYPE_CERTIFICATES || mAuthenticationType == TYPE_USERPASS_CERTIFICATES)
                && TextUtils.isEmpty(mCaFilename))
            return R.string.missing_ca_certificate;


        boolean noRemoteEnabled = true;
        for (Connection c : mConnections) {
            if (c.mEnabled) {
                noRemoteEnabled = false;
                break;
            }

        }
        if (noRemoteEnabled)
            return R.string.remote_no_server_selected;

        if (useOpenVPN3) {
            if (mAuthenticationType == TYPE_STATICKEYS) {
                return R.string.openvpn3_nostatickeys;
            }
            if (mAuthenticationType == TYPE_PKCS12 || mAuthenticationType == TYPE_USERPASS_PKCS12) {
                return R.string.openvpn3_pkcs12;
            }
            for (Connection conn : mConnections) {
                if (conn.mProxyType == Connection.ProxyType.ORBOT || conn.mProxyType == Connection.ProxyType.SOCKS5)
                    return R.string.openvpn3_socksproxy;
            }
        }
        for (Connection c : mConnections) {
            if (c.mProxyType == Connection.ProxyType.ORBOT) {
                if (usesExtraProxyOptions())
                    return R.string.error_orbot_and_proxy_options;
                if (!OrbotHelper.checkTorReceier(context))
                    return R.string.no_orbotfound;
            }
        }


        // Everything okay
        return R.string.no_error_found;

    }

    //! Openvpn asks for a "Private Key", this should be pkcs12 key
    //
    public String getPasswordPrivateKey() {
        String cachedPw = PasswordCache.getPKCS12orCertificatePassword(mUuid, true);
        if (cachedPw != null) {
            return cachedPw;
        }
        switch (mAuthenticationType) {
            case TYPE_PKCS12:
            case TYPE_USERPASS_PKCS12:
                return mPKCS12Password;

            case TYPE_CERTIFICATES:
            case TYPE_USERPASS_CERTIFICATES:
                return mKeyPassword;

            case TYPE_USERPASS:
            case TYPE_STATICKEYS:
            default:
                return null;
        }
    }

    public boolean isUserPWAuth() {
        switch (mAuthenticationType) {
            case TYPE_USERPASS:
            case TYPE_USERPASS_CERTIFICATES:
            case TYPE_USERPASS_KEYSTORE:
            case TYPE_USERPASS_PKCS12:
                return true;
            default:
                return false;

        }
    }

    public boolean requireTLSKeyPassword() {
        if (TextUtils.isEmpty(mClientKeyFilename))
            return false;

        String data = "";
        if (isEmbedded(mClientKeyFilename))
            data = mClientKeyFilename;
        else {
            char[] buf = new char[2048];
            FileReader fr;
            try {
                fr = new FileReader(mClientKeyFilename);
                int len = fr.read(buf);
                while (len > 0) {
                    data += new String(buf, 0, len);
                    len = fr.read(buf);
                }
                fr.close();
            } catch (FileNotFoundException e) {
                return false;
            } catch (IOException e) {
                return false;
            }

        }

        if (data.contains("Proc-Type: 4,ENCRYPTED"))
            return true;
        else return data.contains("-----BEGIN ENCRYPTED PRIVATE KEY-----");
    }

    public int needUserPWInput(String transientCertOrPkcs12PW, String mTransientAuthPW) {
        if ((mAuthenticationType == TYPE_PKCS12 || mAuthenticationType == TYPE_USERPASS_PKCS12) &&
                (mPKCS12Password == null || mPKCS12Password.equals(""))) {
            if (transientCertOrPkcs12PW == null)
                return R.string.pkcs12_file_encryption_key;
        }

        if (mAuthenticationType == TYPE_CERTIFICATES || mAuthenticationType == TYPE_USERPASS_CERTIFICATES) {
            if (requireTLSKeyPassword() && TextUtils.isEmpty(mKeyPassword))
                if (transientCertOrPkcs12PW == null) {
                    return R.string.private_key_password;
                }
        }

        if (isUserPWAuth() &&
                (TextUtils.isEmpty(mUsername) ||
                        (TextUtils.isEmpty(mPassword) && mTransientAuthPW == null))) {
            return R.string.password;
        }
        return 0;
    }

    public String getPasswordAuth() {
        String cachedPw = PasswordCache.getAuthPassword(mUuid, true);
        if (cachedPw != null) {
            return cachedPw;
        } else {
            return mPassword;
        }
    }

    // Used by the Array Adapter
    @NotNull
    @Override
    public String toString() {
        return mName;
    }

    public String getUUIDString() {
        return mUuid.toString().toLowerCase(Locale.ENGLISH);
    }

    public PrivateKey getKeystoreKey() {
        return mPrivateKey;
    }

    @Nullable
    public String getSignedData(Context c, String b64data, boolean pkcs1padding) {
        byte[] data = Base64.decode(b64data, Base64.DEFAULT);
        byte[] signed_bytes;
        signed_bytes = getKeyChainSignedData(data, pkcs1padding);

        if (signed_bytes != null)
            return Base64.encodeToString(signed_bytes, Base64.NO_WRAP);
        else
            return null;
    }

    private byte[] getKeyChainSignedData(byte[] data, boolean pkcs1padding) {

        PrivateKey privkey = getKeystoreKey();
        // The Jelly Bean *evil* Hack
        // 4.2 implements the RSA/ECB/PKCS1PADDING in the OpenSSLprovider
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN) {
            return processSignJellyBeans(privkey, data, pkcs1padding);
        }


        try {
            @SuppressLint("GetInstance")
            String keyalgorithm = privkey.getAlgorithm();

            byte[] signed_bytes;
            if (keyalgorithm.equals("EC")) {
                Signature signer = Signature.getInstance("NONEwithECDSA");

                signer.initSign(privkey);
                signer.update(data);
                signed_bytes = signer.sign();

            } else {
            /* ECB is perfectly fine in this special case, since we are using it for
               the public/private part in the TLS exchange
             */
                Cipher signer;
                if (pkcs1padding)
                    signer = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
                else
                    signer = Cipher.getInstance("RSA/ECB/NoPadding");


                signer.init(Cipher.ENCRYPT_MODE, privkey);

                signed_bytes = signer.doFinal(data);
            }
            return signed_bytes;
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException
                 | BadPaddingException | NoSuchPaddingException | SignatureException e) {
            VpnStatus.logError(R.string.error_rsa_sign, e.getClass().toString(), e.getLocalizedMessage());
            return null;
        }
    }

    private byte[] processSignJellyBeans(PrivateKey privkey, byte[] data, boolean pkcs1padding) {
        try {
            Class<?> superClass = privkey.getClass().getSuperclass();
            if (superClass != null) {
                Method getKey = superClass.getDeclaredMethod("getOpenSSLKey");
                getKey.setAccessible(true);

                // Real object type is OpenSSLKey
                Object opensslkey = getKey.invoke(privkey);
                getKey.setAccessible(false);

                if (opensslkey != null) {
                    Class<?> clazz = opensslkey.getClass();
                    if (clazz != null) {
                        Method getPkeyContext = clazz.getDeclaredMethod("getPkeyContext");
                        // integer pointer to EVP_pkey
                        getPkeyContext.setAccessible(true);
                        int pkey = (Integer) getPkeyContext.invoke(opensslkey);
                        getPkeyContext.setAccessible(false);
                        // 112 with TLS 1.2 (172 back with 4.3), 36 with TLS 1.0
                        return NativeUtils.rsasign(data, pkey, pkcs1padding);
                    }
                }
            }

            return null;

        } catch (NoSuchMethodException | InvalidKeyException | InvocationTargetException |
                 IllegalAccessException | IllegalArgumentException e) {
            VpnStatus.logError(R.string.error_rsa_sign, e.getClass().toString(), e.getLocalizedMessage());
            return null;
        }
    }

    private boolean usesExtraProxyOptions() {
        if (mUseCustomConfig && mCustomConfigOptions != null && mCustomConfigOptions.contains("http-proxy-option "))
            return true;
        for (Connection c : mConnections)
            if (c.usesExtraProxyOptions())
                return true;

        return false;
    }

    /**
     * The order of elements is important!
     */
    private enum RsaPaddingType {
        NO_PADDING,
        PKCS1_PADDING
    }

    static class NoCertReturnedException extends Exception {
        public NoCertReturnedException(String msg) {
            super(msg);
        }
    }
}



