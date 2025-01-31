package de.shellfire.vpn.android;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface ObfuscatedAccountIdDao {
    @Query("SELECT * FROM obfuscated_account_id LIMIT 1")
    LiveData<ObfuscatedAccountId> getObfuscatedAccountId();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void inserObfuscatedAccountId(ObfuscatedAccountId obfuscatedAccountId);

    @Query("DELETE FROM obfuscated_account_id")
    void clearObfuscatedAccountId();
}
