package de.shellfire.vpn.android;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HelpItemDao {
    @Query("SELECT * FROM help_items")
    LiveData<List<HelpItem>> getAllHelpItems();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertHelpItems(List<HelpItem> helpItems);

    @Query("DELETE FROM help_items")
    void clearAllHelpItems();
}
