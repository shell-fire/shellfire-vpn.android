package de.shellfire.vpn.android.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;

@Entity(tableName = "aliases")
public class Alias {
    @PrimaryKey
    @NonNull
    @SerializedName("aliasId")
    private String aliasId;
    @SerializedName("host")
    private String host;
    @SerializedName("port")
    private String port;

    public Alias(String aliasId, String host, String port) {
        this.aliasId = aliasId;
        this.host = host;
        this.port = port;
    }

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

    @NotNull
    @Override
    public String toString() {
        return "https://" +
                getHost() +
                ":" +
                getPort() +
                "/webservice/sf_soap.php";
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof Alias)) {
            return false;
        }
        Alias that = (Alias) other;

        return that.getAliasId().equalsIgnoreCase(this.getAliasId());
    }

}
