// IStatusCallbacks.aidl
package de.shellfire.vpn.android.openvpn;

// Declare any non-default types here with import statements
//import de.shellfire.vpn.android.openvpn.ConnectionStatus;
import de.shellfire.vpn.android.openvpn.LogItem;

interface IStatusCallbacks {
    /**
     * Called when the service has a new status for you.
     */
    oneway void newLogItem(in LogItem item);

    oneway void updateStateString(in String state, in String msg, in int resid, in de.shellfire.vpn.android.openvpn.ConnectionStatus level, in Intent intent);

    oneway void updateByteCount(long inBytes, long outBytes);

    oneway void connectedVPN(String uuid);

}