package de.shellfire.vpn.android;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Entity(tableName = "vpns")
public class Vpn implements Serializable {

    @SerializedName("iProductTypeId")
    private final ProductType productType;

    @PrimaryKey
    @SerializedName("iVpnId")
    private int vpnId;

    @SerializedName("iServerId")
    private int serverId;

    @SerializedName("eAccountType")
    private ServerType accountType;

    @SerializedName("sListenHost")
    private String listenHost;

    @SerializedName("eProtocol")
    private Protocol protocol;

    @SerializedName("iPremiumUntil")
    @JsonAdapter(UnixTimestampDateAdapter.class)
    private Date premiumUntil;

    public Vpn(ProductType productType, int vpnId, int serverId, ServerType accountType, String listenHost, Protocol protocol, Date premiumUntil) {
        this.productType = productType;
        this.vpnId = vpnId;
        this.serverId = serverId;
        this.accountType = accountType;
        this.listenHost = listenHost;
        this.protocol = protocol;
        this.premiumUntil = premiumUntil;
    }

    public int getVpnId() {
        return vpnId;
    }

    public void setVpnId(int vpnId) {
        this.vpnId = vpnId;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public ServerType getAccountType() {
        return accountType;
    }

    public void setAccountType(ServerType accountType) {
        this.accountType = accountType;
    }

    public String getListenHost() {
        return listenHost;
    }

    public void setListenHost(String listenHost) {
        this.listenHost = listenHost;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public ProductType getProductType() {
        return this.productType;
    }

    public Date getPremiumUntil() {
        return this.premiumUntil;
    }

    public void setPremiumUntil(Date premiumUntil) {
        this.premiumUntil = premiumUntil;
    }

    public void setPremiumUntil(int premiumUntil) {
        setPremiumUntil(new Date(premiumUntil));
    }

    @NotNull
    @Override
    public String toString() {
        return "int vpnId: " + vpnId +
                " int serverId: " + serverId +
                " ServerType accountType: " + accountType +
                " String listenHost: " + listenHost +
                " Protocol protocol: " + protocol +
                " ProductType productType: " + productType +
                " final Date premiumUntil: " + premiumUntil;
    }

    public String getName() {
        return "sf" + vpnId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Vpn vpn = (Vpn) obj;

        return vpnId == vpn.vpnId &&
                serverId == vpn.serverId &&
                Objects.equals(accountType, vpn.accountType) &&
                Objects.equals(listenHost, vpn.listenHost) &&
                Objects.equals(protocol, vpn.protocol) &&
                Objects.equals(premiumUntil, vpn.premiumUntil) &&
                Objects.equals(productType, vpn.productType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vpnId, serverId, accountType, listenHost, protocol, premiumUntil, productType);
    }
}
