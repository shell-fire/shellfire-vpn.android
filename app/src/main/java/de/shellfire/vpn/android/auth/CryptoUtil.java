package de.shellfire.vpn.android.auth;

import android.util.Base64;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtil {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";
    private static final int KEY_LENGTH = 256; // bits
    private static final int ITERATION_COUNT = 10000;
    private static final int SALT_LENGTH = 16; // bytes

    // Generate a random salt
    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        return salt;
    }

    // Generate the secret key
    private static SecretKey generateKey(char[] passphrase, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(passphrase, salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    public static String encrypt(String input, String passphrase) throws Exception {
        byte[] salt = generateSalt();
        SecretKey key = generateKey(passphrase.toCharArray(), salt);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(input.getBytes());

        // Combine salt and encrypted bytes
        byte[] combined = new byte[salt.length + encryptedBytes.length];
        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(encryptedBytes, 0, combined, salt.length, encryptedBytes.length);

        return Base64.encodeToString(combined, Base64.DEFAULT);
    }

    public static String decrypt(String input, String passphrase) throws Exception {
        byte[] combined = Base64.decode(input, Base64.DEFAULT);

        // Extract salt and encrypted bytes
        byte[] salt = new byte[SALT_LENGTH];
        byte[] encryptedBytes = new byte[combined.length - SALT_LENGTH];
        System.arraycopy(combined, 0, salt, 0, salt.length);
        System.arraycopy(combined, salt.length, encryptedBytes, 0, encryptedBytes.length);

        SecretKey key = generateKey(passphrase.toCharArray(), salt);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes);
    }
}
