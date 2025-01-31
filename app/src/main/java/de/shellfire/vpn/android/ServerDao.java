package de.shellfire.vpn.android;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ServerDao {
    @Query("SELECT * FROM servers")
    LiveData<List<Server>> getAllServers();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertServers(List<Server> servers);

    @Query("DELETE FROM servers")
    void clearAll();
}
