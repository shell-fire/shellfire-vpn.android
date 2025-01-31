package de.shellfire.vpn.android.webservice.model;

import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import de.shellfire.vpn.android.ServerType;

public class UpgradeVpnResponse  {
    @SerializedName("eAccountType")
    private String eAccountTypeString;

    @SerializedName("iUpgradeUntil")
    private int iUpgradeUntil;

    public UpgradeVpnResponse() {
    }

    public UpgradeVpnResponse(String eAccountTypeString, int iUpgradeUntil) {
        this.eAccountTypeString = eAccountTypeString;
        this.iUpgradeUntil = iUpgradeUntil;
    }

    public String geteAccountTypeString() {
        return eAccountTypeString;
    }

    public void seteAccountTypeString(String eAccountTypeString) {
        this.eAccountTypeString = eAccountTypeString;
    }

    public ServerType geteAccountType() {
        return parseServerType(eAccountTypeString);
    }

    public int getiUpgradeUntil() {
        return iUpgradeUntil;
    }

    public void setiUpgradeUntil(int iUpgradeUntil) {
        this.iUpgradeUntil = iUpgradeUntil;
    }

    private ServerType parseServerType(String serverTypeString) {
        try {
            return ServerType.valueOf(serverTypeString);
        } catch (IllegalArgumentException e) {
            return null; // or handle the case where the server type is not recognized
        }
    }

    @NotNull
    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "UpgradeVpnResponse { eAccountType='%s', iUpgradeUntil=%d }", geteAccountType(), iUpgradeUntil);
    }
}
