package de.shellfire.vpn.android;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

@Entity(tableName = "vpn_attribute_list")
@TypeConverters(VpnAttributeListConverter.class)
public class VpnAttributeListEntity {
    @PrimaryKey
    private int id;
    private String dataJson; // Storing the serialized JSON data

    public VpnAttributeListEntity(int id, String dataJson) {
        this.id = id;
        this.dataJson = dataJson;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDataJson() {
        return dataJson;
    }

    public void setDataJson(String dataJson) {
        this.dataJson = dataJson;
    }
}
