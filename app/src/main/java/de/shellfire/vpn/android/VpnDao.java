package de.shellfire.vpn.android;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface VpnDao {
    @Query("SELECT * FROM vpns")
    LiveData<List<Vpn>> getAllVpns();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertVpns(List<Vpn> vpns);

    @Query("DELETE FROM vpns")
    void clearAll();
}
