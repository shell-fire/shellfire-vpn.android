package de.shellfire.vpn.android.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Inna Krokhmal on 18/04/17.
 */

public class Datum {
    @SerializedName("aliasId")
    @Expose
    private String aliasId;
    @SerializedName("host")
    @Expose
    private String host;
    @SerializedName("port")
    @Expose
    private String port;

    public String getAliasId() {
        return aliasId;
    }

    public void setAliasId(String aliasId) {
        this.aliasId = aliasId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}
