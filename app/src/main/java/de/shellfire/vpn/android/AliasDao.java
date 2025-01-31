package de.shellfire.vpn.android;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import de.shellfire.vpn.android.model.Alias;

@Dao
public interface AliasDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAliases(Alias[] aliases);

    @Query("SELECT * FROM aliases")
    LiveData<Alias[]> getAllAliases();

    @Query("DELETE FROM aliases")
    void clearAll();
}
