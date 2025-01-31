/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.shellfire.vpn.android.openvpn;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

//! Openvpn Config FIle Parser, probably not 100% accurate but close enough

// And remember, this is valid :)
// --<foo>
// bar
// </foo>
public class ConfigParser {


    public static final String CONVERTED_PROFILE = "converted Profile";
    private static final String TAG = "ConfigParser";
    final String[] unsupportedOptions = {"config",
            "tls-server"

    };
    // Ignore all scripts
    // in most cases these won't work and user who wish to execute scripts will
    // figure out themselves
    private final String[] ignoreOptions = {"tls-client",
            "allow-recursive-routing",
            "askpass",
            "auth-nocache",
            "up",
            "down",
            "route-up",
            "ipchange",
            "route-pre-down",
            "auth-user-pass-verify",
            "block-outside-dns",
            "client-cert-not-required",
            "dhcp-release",
            "dhcp-renew",
            "dh",
            "group",
            "ip-win32",
            "ifconfig-nowarn",
            "management-hold",
            "management",
            "management-client",
            "management-query-remote",
            "management-query-passwords",
            "management-query-proxy",
            "management-external-key",
            "management-forget-disconnect",
            "management-signal",
            "management-log-cache",
            "management-up-down",
            "management-client-user",
            "management-client-group",
            "pause-exit",
            "preresolve",
            "plugin",
            "machine-readable-output",
            "persist-key",
            "push",
            "register-dns",
            "route-delay",
            "route-gateway",
            "route-metric",
            "route-method",
            "status",
            "script-security",
            "show-net-up",
            "suppress-timestamps",
            "tap-sleep",
            "tmp-dir",
            "tun-ipv6",
            "topology",
            "user",
            "win-sys",
    };
    private final String[][] ignoreOptionsWithArg =
            {
                    {"setenv", "IV_GUI_VER"},
                    {"setenv", "IV_SSO"},
                    {"setenv", "IV_PLAT_VER"},
                    {"setenv", "IV_OPENVPN_GUI_VERSION"},
                    {"engine", "dynamic"},
                    {"setenv", "CLIENT_CERT"},
                    {"resolv-retry", "60"}
            };
    private final String[] connectionOptions = {
            "local",
            "remote",
            "float",
            "port",
            "connect-retry",
            "connect-timeout",
            "connect-retry-max",
            "link-mtu",
            "tun-mtu",
            "tun-mtu-extra",
            "fragment",
            "mtu-disc",
            "local-port",
            "remote-port",
            "bind",
            "nobind",
            "proto",
            "http-proxy",
            "http-proxy-retry",
            "http-proxy-timeout",
            "http-proxy-option",
            "socks-proxy",
            "socks-proxy-retry",
            "http-proxy-user-pass",
            "explicit-exit-notify",
    };
    private final HashSet<String> connectionOptionsSet = new HashSet<>(Arrays.asList(connectionOptions));

    private final HashMap<String, Vector<Vector<String>>> options = new HashMap<>();
    private final HashMap<String, Vector<String>> meta = new HashMap<String, Vector<String>>();
    private String auth_user_pass_file;

    static public void useEmbbedUserAuth(VpnProfile np, String inlinedata) {
        String data = VpnProfile.getEmbeddedContent(inlinedata);
        String[] parts = data.split("\n");
        if (parts.length >= 2) {
            np.mUsername = parts[0];
            np.mPassword = parts[1];
        }
    }

    static public void useEmbbedHttpAuth(Connection c, String inlinedata) {
        String data = VpnProfile.getEmbeddedContent(inlinedata);
        String[] parts = data.split("\n");
        if (parts.length >= 2) {
            c.mProxyAuthUser = parts[0];
            c.mProxyAuthPassword = parts[1];
            c.mUseProxyAuth = true;
        }
    }

    public void parseConfig(Reader reader) throws IOException, ConfigParseError {

        HashMap<String, String> optionAliases = new HashMap<>();
        optionAliases.put("server-poll-timeout", "timeout-connect");

        BufferedReader br = new BufferedReader(reader);

        int lineno = 0;
        try {
            while (true) {
                String line = br.readLine();
                lineno++;
                if (line == null)
                    break;

                if (lineno == 1) {
                    if ((line.startsWith("PK\003\004")
                            || (line.startsWith("PK\007\008")))) {
                        throw new ConfigParseError("Input looks like a ZIP Archive. Import is only possible for OpenVPN config files (.ovpn/.conf)");
                    }
                    if (line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                }

                // Check for OpenVPN Access Server Meta information
                if (line.startsWith("# OVPN_ACCESS_SERVER_")) {
                    Vector<String> metaarg = parsemeta(line);
                    meta.put(metaarg.get(0), metaarg);
                    continue;
                }
                Vector<String> args = parseline(line);

                if (args.isEmpty())
                    continue;


                if (args.get(0).startsWith("--"))
                    args.set(0, args.get(0).substring(2));

                checkinlinefile(args, br);

                String optionname = args.get(0);
                if (optionAliases.get(optionname) != null)
                    optionname = optionAliases.get(optionname);

                if (!options.containsKey(optionname)) {
                    options.put(optionname, new Vector<Vector<String>>());
                }
                if (options != null) {
                    options.get(optionname).add(args);
                }

            }
        } catch (java.lang.OutOfMemoryError memoryError) {
            throw new ConfigParseError("File too large to parse: " + memoryError.getLocalizedMessage());
        }
    }

    private Vector<String> parsemeta(String line) {
        String meta = line.split("#\\sOVPN_ACCESS_SERVER_", 2)[1];
        String[] parts = meta.split("=", 2);
        Vector<String> rval = new Vector<String>();
        Collections.addAll(rval, parts);
        return rval;

    }

    private void checkinlinefile(Vector<String> args, BufferedReader br) throws IOException, ConfigParseError {
        String arg0 = args.get(0).trim();
        // CHeck for <foo>
        if (arg0.startsWith("<") && arg0.endsWith(">")) {
            String argname = arg0.substring(1, arg0.length() - 1);
            String inlinefile = VpnProfile.INLINE_TAG;

            String endtag = String.format("</%s>", argname);
            do {
                String line = br.readLine();
                if (line == null) {
                    throw new ConfigParseError(String.format("No endtag </%s> for starttag <%s> found", argname, argname));
                }
                if (line.trim().equals(endtag))
                    break;
                else {
                    inlinefile += line;
                    inlinefile += "\n";
                }
            } while (true);

            if (inlinefile.endsWith("\n"))
                inlinefile = inlinefile.substring(0, inlinefile.length() - 1);

            args.clear();
            args.add(argname);
            args.add(inlinefile);
        }

    }

    public String getAuthUserPassFile() {
        return auth_user_pass_file;
    }

    private boolean space(char c) {
        // I really hope nobody is using zero bytes inside his/her config file
        // to sperate parameter but here we go:
        return Character.isWhitespace(c) || c == '\0';

    }

    // adapted openvpn's parse function to java
    private Vector<String> parseline(String line) throws ConfigParseError {
        Vector<String> parameters = new Vector<String>();

        if (line.isEmpty())
            return parameters;


        linestate state = linestate.initial;
        boolean backslash = false;
        char out = 0;

        int pos = 0;
        String currentarg = "";

        do {
            // Emulate the c parsing ...
            char in;
            if (pos < line.length())
                in = line.charAt(pos);
            else
                in = '\0';

            if (!backslash && in == '\\' && state != linestate.readin_single_quote) {
                backslash = true;
            } else {
                if (state == linestate.initial) {
                    if (!space(in)) {
                        if (in == ';' || in == '#') /* comment */
                            break;
                        if (!backslash && in == '\"')
                            state = linestate.reading_quoted;
                        else if (!backslash && in == '\'')
                            state = linestate.readin_single_quote;
                        else {
                            out = in;
                            state = linestate.reading_unquoted;
                        }
                    }
                } else if (state == linestate.reading_unquoted) {
                    if (!backslash && space(in))
                        state = linestate.done;
                    else
                        out = in;
                } else if (state == linestate.reading_quoted) {
                    if (!backslash && in == '\"')
                        state = linestate.done;
                    else
                        out = in;
                } else if (state == linestate.readin_single_quote) {
                    if (in == '\'')
                        state = linestate.done;
                    else
                        out = in;
                }

                if (state == linestate.done) {
                    /* ASSERT (parm_len > 0); */
                    state = linestate.initial;
                    parameters.add(currentarg);
                    currentarg = "";
                    out = 0;
                }

                if (backslash && out != 0) {
                    if (!(out == '\\' || out == '\"' || space(out))) {
                        throw new ConfigParseError("Options warning: Bad backslash ('\\') usage");
                    }
                }
                backslash = false;
            }

            /* store parameter character */
            if (out != 0) {
                currentarg += out;
            }
        } while (pos++ < line.length());

        return parameters;
    }

    private String join(String s, Vector<String> str) {
        if (Build.VERSION.SDK_INT > 26)
            return String.join(s, str);
        else
            return TextUtils.join(s, str);
    }

    private Pair<Connection, Connection[]> parseConnection(String connection, Connection defaultValues) throws IOException, ConfigParseError {
        // Parse a connection Block as a new configuration file


        ConfigParser connectionParser = new ConfigParser();
        StringReader reader = new StringReader(connection.substring(VpnProfile.INLINE_TAG.length()));
        connectionParser.parseConfig(reader);

        Pair<Connection, Connection[]> conn = connectionParser.parseConnectionOptions(defaultValues);

        return conn;
    }

    private Pair<Connection, Connection[]> parseConnectionOptions(Connection connDefault) throws ConfigParseError {
        Connection conn;
        if (connDefault != null)
            try {
                conn = connDefault.clone();
            } catch (CloneNotSupportedException e) {
                Log.e(TAG, "Failed to clone connection", e);
                return null;
            }
        else
            conn = new Connection();

        Vector<String> port = getOption("port", 1, 1);
        if (port != null) {
            conn.mServerPort = port.get(1);
        }

        Vector<String> rport = getOption("rport", 1, 1);
        if (rport != null) {
            conn.mServerPort = rport.get(1);
        }

        Vector<String> proto = getOption("proto", 1, 1);
        if (proto != null) {
            conn.mUseUdp = isUdpProto(proto.get(1));
        }

        Vector<String> connectTimeout = getOption("connect-timeout", 1, 1);
        if (connectTimeout != null) {
            try {
                conn.mConnectTimeout = Integer.parseInt(connectTimeout.get(1));
            } catch (NumberFormatException nfe) {
                throw new ConfigParseError(String.format("Argument to connect-timeout (%s) must to be an integer: %s",
                        connectTimeout.get(1), nfe.getLocalizedMessage()));

            }
        }

        Vector<String> proxy = getOption("socks-proxy", 1, 2);
        if (proxy == null)
            proxy = getOption("http-proxy", 2, 2);

        if (proxy != null) {
            if (proxy.get(0).equals("socks-proxy")) {
                conn.mProxyType = Connection.ProxyType.SOCKS5;
                // socks defaults to 1080, http always sets port
                conn.mProxyPort = "1080";
            } else {
                conn.mProxyType = Connection.ProxyType.HTTP;
            }

            conn.mProxyName = proxy.get(1);
            if (proxy.size() >= 3)
                conn.mProxyPort = proxy.get(2);
        }

        Vector<String> httpproxyauthhttp = getOption("http-proxy-user-pass", 1, 1);
        if (httpproxyauthhttp != null)
            useEmbbedHttpAuth(conn, httpproxyauthhttp.get(1));


        // Parse remote config
        Vector<Vector<String>> remotes = getAllOption("remote", 1, 3);


        Vector<String> optionsToRemove = new Vector<>();
        // Assume that we need custom options if connectionDefault are set or in the connection specific set
        for (Map.Entry<String, Vector<Vector<String>>> option : options.entrySet()) {
            if (connDefault != null || connectionOptionsSet.contains(option.getKey())) {
                conn.mCustomConfiguration += getOptionStrings(option.getValue());
                optionsToRemove.add(option.getKey());
            }
        }
        for (String o : optionsToRemove)
            options.remove(o);

        if (!(conn.mCustomConfiguration == null || "".equals(conn.mCustomConfiguration.trim())))
            conn.mUseCustomConfig = true;

        // Make remotes empty to simplify code
        if (remotes == null)
            remotes = new Vector<Vector<String>>();

        Connection[] connections = new Connection[remotes.size()];


        int i = 0;
        for (Vector<String> remote : remotes) {
            try {
                connections[i] = conn.clone();
            } catch (CloneNotSupportedException e) {
                Log.e(TAG, "Failed to clone connection", e);
            }
            switch (remote.size()) {
                case 4:
                    connections[i].mUseUdp = isUdpProto(remote.get(3));
                case 3:
                    connections[i].mServerPort = remote.get(2);
                case 2:
                    connections[i].mServerName = remote.get(1);
            }
            i++;
        }

        return Pair.create(conn, connections);

    }

    private void checkRedirectParameters(VpnProfile np, Vector<Vector<String>> defgw, boolean defaultRoute) {

        boolean noIpv4 = false;
        if (defaultRoute)

            for (Vector<String> redirect : defgw)
                for (int i = 1; i < redirect.size(); i++) {
                    if (redirect.get(i).equals("block-local"))
                        np.mAllowLocalLAN = false;
                    else if (redirect.get(i).equals("unblock-local"))
                        np.mAllowLocalLAN = true;
                    else if (redirect.get(i).equals("!ipv4"))
                        noIpv4 = true;
                    else if (redirect.get(i).equals("ipv6"))
                        np.mUseDefaultRoutev6 = true;
                }
        if (defaultRoute && !noIpv4)
            np.mUseDefaultRoute = true;
    }

    private boolean isUdpProto(String proto) throws ConfigParseError {
        boolean isudp;
        if (proto.equals("udp") || proto.equals("udp4") || proto.equals("udp6"))
            isudp = true;
        else if (proto.equals("tcp-client") ||
                proto.equals("tcp") ||
                proto.equals("tcp4") ||
                proto.endsWith("tcp4-client") ||
                proto.equals("tcp6") ||
                proto.endsWith("tcp6-client"))
            isudp = false;
        else
            throw new ConfigParseError("Unsupported option to --proto " + proto);
        return isudp;
    }

    private void checkIgnoreAndInvalidOptions(VpnProfile np) throws ConfigParseError {
        for (String option : unsupportedOptions)
            if (options.containsKey(option))
                throw new ConfigParseError(String.format("Unsupported Option %s encountered in config file. Aborting", option));

        for (String option : ignoreOptions)
            // removing an item which is not in the map is no error
            options.remove(option);


        boolean customOptions = false;
        for (Vector<Vector<String>> option : options.values()) {
            for (Vector<String> optionsline : option) {
                if (!ignoreThisOption(optionsline)) {
                    customOptions = true;
                }
            }
        }
        if (customOptions) {
            np.mCustomConfigOptions = "# These options found in the config file do not map to config settings:\n"
                    + np.mCustomConfigOptions;

            for (Vector<Vector<String>> option : options.values()) {

                np.mCustomConfigOptions += getOptionStrings(option);

            }
            np.mUseCustomConfig = true;

        }
    }

    boolean ignoreThisOption(Vector<String> option) {
        for (String[] ignoreOption : ignoreOptionsWithArg) {

            if (option.size() < ignoreOption.length)
                continue;

            boolean ignore = true;
            for (int i = 0; i < ignoreOption.length; i++) {
                if (!ignoreOption[i].equals(option.get(i))) {
                    ignore = false;
                    break;
                }
            }
            if (ignore)
                return true;

        }
        return false;
    }

    //! Generate options for custom options
    private String getOptionStrings(Vector<Vector<String>> option) {
        String custom = "";
        for (Vector<String> optionsline : option) {
            if (!ignoreThisOption(optionsline)) {
                // Check if option had been inlined and inline again
                if (optionsline.size() == 2 &&
                        "extra-certs".equals(optionsline.get(0))) {
                    custom += VpnProfile.insertFileData(optionsline.get(0), optionsline.get(1));
                } else {
                    for (String arg : optionsline)
                        custom += VpnProfile.openVpnEscape(arg) + " ";
                    custom += "\n";
                }
            }
        }
        return custom;
    }

    private void fixup(VpnProfile np) {
        if (np.mRemoteCN.equals(np.mServerName)) {
            np.mRemoteCN = "";
        }
    }

    private Vector<String> getOption(String option, int minarg, int maxarg) throws ConfigParseError {
        Vector<Vector<String>> alloptions = getAllOption(option, minarg, maxarg);
        if (alloptions == null)
            return null;
        else
            return alloptions.lastElement();
    }

    private Vector<Vector<String>> getAllOption(String option, int minarg, int maxarg) throws ConfigParseError {
        Vector<Vector<String>> args = options.get(option);
        if (args == null)
            return null;

        for (Vector<String> optionline : args)

            if (optionline.size() < (minarg + 1) || optionline.size() > maxarg + 1) {
                String err = String.format(Locale.getDefault(), "Option %s has %d parameters, expected between %d and %d",
                        option, optionline.size() - 1, minarg, maxarg);
                throw new ConfigParseError(err);
            }
        options.remove(option);
        return args;
    }

    enum linestate {
        initial,
        readin_single_quote, reading_quoted, reading_unquoted, done
    }

    public static class ConfigParseError extends Exception {
        private static final long serialVersionUID = -60L;

        public ConfigParseError(String msg) {
            super(msg);
        }
    }

}



