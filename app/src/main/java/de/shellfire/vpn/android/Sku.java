package de.shellfire.vpn.android;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

@Entity(tableName = "skus")
public class Sku {
    @PrimaryKey
    @NonNull
    @SerializedName(("sSku"))
    private String sku;

    @SerializedName("iBillingPeriod")
    private int billingPeriod;

    @SerializedName("eAccountType")
    private String serverTypeString;

    // Constructor
    public Sku(String sku, int billingPeriod, String serverTypeString) {
        this.sku = sku;
        this.billingPeriod = billingPeriod;
        this.serverTypeString = serverTypeString;
    }

    // Getters and setters
    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getBillingPeriod() {
        return billingPeriod;
    }

    public void setBillingPeriod(int billingPeriod) {
        this.billingPeriod = billingPeriod;
    }

    public String getServerTypeString() {
        return serverTypeString;
    }

    public ServerType geteAccountType() {
        return parseServerType(serverTypeString);
    }

    public void setServerTypeString(String serverTypeString) {
        this.serverTypeString = serverTypeString;
    }

    public boolean isSubscription() {
        return sku != null && sku.startsWith("sub_");
    }


    @Override
    public String toString() {
        return "Sku{" +
                "sku='" + sku + '\'' +
                ", billingPeriod=" + billingPeriod +
                ", serverTypeString='" + serverTypeString + '\'' +
                ", geteAccountType=" + geteAccountType() +
                '}';
    }

    private ServerType parseServerType(String serverTypeString) {
        try {
            return ServerType.valueOf(serverTypeString);
        } catch (IllegalArgumentException e) {
            return null; // or handle the case where the server type is not recognized
        }
    }
}
