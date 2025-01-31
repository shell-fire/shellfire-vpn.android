package de.shellfire.vpn.android;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface VpnAttributeListDao {
    @Query("SELECT * FROM vpn_attribute_list WHERE id = 1") // Assuming only one entry
    LiveData<VpnAttributeListEntity> getVpnAttributeList();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertVpnAttributeList(VpnAttributeListEntity vpnAttributeListEntity);

    @Query("DELETE FROM vpn_attribute_list")
    void clearAll();
}
