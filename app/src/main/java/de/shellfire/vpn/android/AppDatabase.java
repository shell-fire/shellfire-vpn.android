package de.shellfire.vpn.android;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import de.shellfire.vpn.android.model.Alias;

@Database(entities = {Alias.class, Server.class, Vpn.class, VpnAttributeListEntity.class, Sku.class, HelpItem.class, ObfuscatedAccountId.class}, version = 3)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract ServerDao serverDao();
    public abstract AliasDao aliasDao();
    public abstract VpnDao vpnDao();
    public abstract VpnAttributeListDao vpnAttributeListDao();
    public abstract SkuDao skuDao();
    public abstract HelpItemDao helpItemDao();
    public abstract ObfuscatedAccountIdDao obfuscatedAccountIdDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "shellfire_vpn_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }

        return INSTANCE;
    }

}
