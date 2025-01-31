package de.shellfire.vpn.android;

import androidx.room.TypeConverter;

import com.google.gson.Gson;

import de.shellfire.vpn.android.webservice.model.VpnAttributeList;

public class VpnAttributeListConverter {
    @TypeConverter
    public static String fromVpnAttributeList(VpnAttributeList vpnAttributeList) {
        return new Gson().toJson(vpnAttributeList);
    }

    @TypeConverter
    public static VpnAttributeList toVpnAttributeList(String data) {
        return new Gson().fromJson(data, VpnAttributeList.class);
    }
}
