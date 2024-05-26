package com.example.notepadl;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class Crypto {
    public static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String SECRET_KEY_ALIAS = "note_encryption_key";
    private static final byte[] IV  = new byte[]{1,2,3,4,5,6,7,8,9,10,11,12};

    public static SecretKey getSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        if (!keyStore.containsAlias(Crypto.SECRET_KEY_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            KeyGenParameterSpec keyGenParameterSpec = getKeyGenParameterSpec();

            keyGenerator.init(keyGenParameterSpec);
            keyGenerator.generateKey();
        }

        var secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry(Crypto.SECRET_KEY_ALIAS, null);
        return secretKeyEntry.getSecretKey();
    }

    @NonNull
    private static KeyGenParameterSpec getKeyGenParameterSpec() {
        KeyGenParameterSpec.Builder specBuilder = new KeyGenParameterSpec.Builder(
                Crypto.SECRET_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(false)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            specBuilder.setUserAuthenticationParameters(0, KeyProperties.AUTH_DEVICE_CREDENTIAL);
        }

        return specBuilder.build();
    }

    //public static Cipher getCipherForDecryption(String encryptedData) throws Exception {
    //    byte[] decodedData = Base64.decode(encryptedData, Base64.DEFAULT);
    //    int ivLength = decodedData[0];
    //    byte[] iv = new byte[ivLength];
    //    System.arraycopy(decodedData, 1, iv, 0, ivLength);

    //    SecretKey secretKey = getSecretKey();
    //    GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_SIZE, iv);
    //    Cipher cipher = Cipher.getInstance(Crypto.TRANSFORMATION);
    //    cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
    //    return cipher;
    //}

    public static String decryptString(Cipher cipher, String encryptedData) throws Exception {
        byte[] decodedData = Base64.decode(encryptedData, Base64.DEFAULT);
        byte[] decryptedData = cipher.doFinal(decodedData);
        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    public static void deleteKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        if (keyStore.containsAlias(SECRET_KEY_ALIAS)) {
            keyStore.deleteEntry(SECRET_KEY_ALIAS);
        }
    }

    public String encryptString(Cipher cipher, String plaintext) throws Exception {
        byte[] encryption = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(encryption, Base64.DEFAULT);
    }

    public static Cipher getCipher(int mode) throws Exception {
        SecretKey secretKey = getSecretKey();
        Cipher cipher = Cipher.getInstance(Crypto.TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(128, IV);
        cipher.init(mode, secretKey, spec);
        return cipher;
    }
}
