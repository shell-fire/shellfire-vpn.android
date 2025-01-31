package de.shellfire.vpn.android.webservice.model;

import com.google.gson.annotations.SerializedName;

public class VpnAttributeElement implements java.io.Serializable {
    @SerializedName("name")
    private String name;

    @SerializedName("free")
    private Entry free;

    @SerializedName("premium")
    private Entry premium;

    @SerializedName("pp")
    private Entry pp;

    public VpnAttributeElement() {
    }

    public VpnAttributeElement(String name, Entry free, Entry premium, Entry pp) {
        this.name = name;
        this.free = free;
        this.premium = premium;
        this.pp = pp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Entry getFree() {
        return free;
    }

    public void setFree(Entry free) {
        this.free = free;
    }

    public Entry getPremium() {
        return premium;
    }

    public void setPremium(Entry premium) {
        this.premium = premium;
    }

    public Entry getPp() {
        return pp;
    }

    public void setPp(Entry pp) {
        this.pp = pp;
    }
}
