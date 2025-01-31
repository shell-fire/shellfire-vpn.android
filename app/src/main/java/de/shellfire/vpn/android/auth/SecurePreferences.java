package de.shellfire.vpn.android.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

public class SecurePreferences {
    private static final String FILE_NAME = "secure_prefs";
    private static final String TOKEN_KEY = "auth_token";
    private static SecurePreferences instance;
    private SharedPreferences sharedPreferences;
    private final Context context;

    private SecurePreferences(Context context) throws GeneralSecurityException, IOException {
        this.context = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                MasterKey masterKey = new MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();

                sharedPreferences = EncryptedSharedPreferences.create(
                        context,
                        FILE_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
            } catch (GeneralSecurityException | IOException e) {
                Log.e("SecurePreferences", "KeyStore error, clearing data.", e);
                // Clear corrupted KeyStore and SharedPreferences
                clearKeyStore();
                sharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
            }
        } else {
            sharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        }
    }

    private void clearKeyStore() {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS); // Clear the MasterKey
        } catch (Exception e) {
            Log.e("SecurePreferences", "Error clearing KeyStore", e);
        }
    }


    public static synchronized SecurePreferences getInstance(Context context) throws GeneralSecurityException, IOException {
        if (instance == null) {
            instance = new SecurePreferences(context);
        }
        return instance;
    }

    public void saveToken(String token) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                sharedPreferences.edit().putString(TOKEN_KEY, token).apply();
            } catch (Exception e) {
                Log.e("SecurePreferences", "Error saving token. Resetting preferences.", e);
                clearToken(); // Reset the corrupted data
            }
        } else {
            try {
                String encryptionKey = getEncryptionKey(context);
                String encryptedToken = CryptoUtil.encrypt(token, encryptionKey);
                sharedPreferences.edit().putString(TOKEN_KEY, encryptedToken).apply();
            } catch (Exception e) {
                Log.e("SecurePreferences", "Error encrypting token", e);
            }
        }
    }



    public String getToken() {
        try {
            String token = sharedPreferences.getString(TOKEN_KEY, null);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M && token != null) {
                String encryptionKey = getEncryptionKey(context);
                return CryptoUtil.decrypt(token, encryptionKey);
            }
            return token;
        } catch (Exception e) {
            Log.e("SecurePreferences", "Error decrypting token. Clearing corrupted data.", e);
            clearToken(); // Clear corrupted data
            return null;
        }
    }


    public void clearToken() {
        sharedPreferences.edit().remove(TOKEN_KEY).apply();
    }

    private String getEncryptionKey(Context context) {
        // Get device-specific ID
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return androidId;
    }

}
