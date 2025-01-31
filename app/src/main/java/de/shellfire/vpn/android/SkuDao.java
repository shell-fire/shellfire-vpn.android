package de.shellfire.vpn.android;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SkuDao {
    @Query("SELECT * FROM skus WHERE SUBSTR(sku, 3) = :type or  SUBSTR(sku, 4) = :type")
    LiveData<List<Sku>> getSkusByType(String type);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSkus(List<Sku> skus);

    @Query("DELETE FROM skus WHERE SUBSTR(sku, 3) = :type or  SUBSTR(sku, 4) = :type")
    void clearSkusByType(String type);

    @Query("DELETE FROM skus")
    void clearAll();

}
