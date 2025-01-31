package de.shellfire.vpn.android;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

@Entity(tableName = "servers")
public class Server implements Serializable, Comparable<Server> {

    @PrimaryKey
    private int vpnServerId;

    @SerializedName("country")
    private String countryString;


    public void setCountryEnum(Country countryEnum) {
        this.countryEnum = countryEnum;
    }

    private String countryPrint;

    private String city;
    private String name;
    private String host;

    @SerializedName("servertype")
    private String serverTypeString;

    private double longitude;
    private double latitude;
    private int loadPercentage;
    private String wireguardPublicKey;

    @Expose(serialize = false)
    @Ignore
    private Country countryEnum;

    // Default constructor
    public Server() {
    }

    // Getters and setters
    public int getVpnServerId() {
        return vpnServerId;
    }

    public void setVpnServerId(int vpnServerId) {
        this.vpnServerId = vpnServerId;
    }

    public Country getCountryEnum() {
        if (countryEnum == null) {
            countryEnum = this.parseCountry(this.countryString);
        }
        return countryEnum;
    }

    public String getServerTypeString() {
        return serverTypeString;
    }

    public void setServerTypeString(String serverTypeString) {
        this.serverTypeString = serverTypeString;
    }

    public String getCity() {
        return city != null ? city.trim() : null;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getName() {
        return name != null ? name.trim() : null;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host != null ? host.trim() : null;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public ServerType getServerType() {
        return parseServerType(serverTypeString);
    }

    private ServerType parseServerType(String serverTypeString) {
        try {
            return ServerType.valueOf(serverTypeString.trim());
        } catch (IllegalArgumentException e) {
            return null; // or handle the case where the server type is not recognized
        }
    }

    private Country parseCountry(String countryString) {
        for (Country country : Country.values()) {
            if (country.getName().equalsIgnoreCase(countryString.trim())) {
                return country;
            }
        }
        Log.e("Server", "Country not found: " + countryString + " serverId = " + vpnServerId);
        return null; // or handle the case where the country is not recognized
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public int getLoadPercentage() {
        return loadPercentage;
    }

    public void setLoadPercentage(int loadPercentage) {
        this.loadPercentage = loadPercentage;
    }

    public String getWireguardPublicKey() {
        return wireguardPublicKey;
    }

    public void setWireguardPublicKey(String wireguardPublicKey) {
        this.wireguardPublicKey = wireguardPublicKey;
    }

    public VpnStar getServerSpeed() {
        switch (this.getServerType()) {
            case PremiumPlus:
                return new VpnStar(5, R.string.unlimited);
            case Premium:
                return new VpnStar(3, R.string.upto10000);
            case Free:
            default:
                return new VpnStar(1, R.string.upto786);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Server server = (Server) obj;

        return vpnServerId == server.vpnServerId &&
                Double.compare(server.longitude, longitude) == 0 &&
                Double.compare(server.latitude, latitude) == 0 &&
                loadPercentage == server.loadPercentage &&
                Objects.equals(countryString, server.countryString) &&
                Objects.equals(city, server.city) &&
                Objects.equals(name, server.name) &&
                Objects.equals(host, server.host) &&
                Objects.equals(serverTypeString, server.serverTypeString) &&
                Objects.equals(wireguardPublicKey, server.wireguardPublicKey) &&
                Objects.equals(countryEnum, server.countryEnum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vpnServerId, countryString, city, name, host, serverTypeString, longitude, latitude, loadPercentage, wireguardPublicKey, countryEnum);
    }


    public String getCountryString() {
        return countryString;
    }

    public void setCountryString(String countryString) {
        this.countryString = countryString;
    }

    public VpnStar getSecurity() {
        switch (this.getServerType()) {
            case PremiumPlus:
                return new VpnStar(5, R.string._256bit);
            case Premium:
                return new VpnStar(3, R.string._192bit);
            case Free:
            default:
                return new VpnStar(2, R.string._128bit);
        }
    }

    @SuppressLint("DefaultLocale")
    @NotNull
    @Override
    public String toString() {
        return String.format(
                "Server { vpnServerId=%d, countryString=%s, country='%s', city='%s', name='%s', host='%s', serverType=%s, longitude=%f, latitude=%f, wireguardPublicKey='%s' }",
                vpnServerId, getCountryString(), getCountryEnum(), getCity(), getName(), getHost(), getServerType(), longitude, latitude, wireguardPublicKey
        );
    }

    @Override
    public int compareTo(@NonNull Server that) {
        return Integer.compare(this.getVpnServerId(), that.getVpnServerId());
    }

    public void setServerTypeString(ServerType serverType) {
        this.serverTypeString = serverType.toString();
    }

    public String getImageUrl() {
        return "https://www.shellfire.de/webservice/serverImage.php/?iServerId=" + vpnServerId;
    }

    public String getCountryPrint() {
        return countryPrint;
    }

    public void setCountryPrint(String countryPrint) {
        this.countryPrint = countryPrint;
    }

}
