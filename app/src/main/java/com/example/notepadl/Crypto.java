package com.example.notepadl;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class Crypto {
    public static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int TAG_SIZE = 128;
    private static final String SECRET_KEY_ALIAS = "note_encryption_key";

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
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            specBuilder.setUserAuthenticationParameters(0, KeyProperties.AUTH_DEVICE_CREDENTIAL);
        }

        return specBuilder.build();
    }

    public static Cipher getCipherForDecryption(String encryptedData) throws Exception {
        byte[] decodedData = Base64.decode(encryptedData, Base64.DEFAULT);
        int ivLength = decodedData[0];
        byte[] iv = new byte[ivLength];
        System.arraycopy(decodedData, 1, iv, 0, ivLength);

        SecretKey secretKey = getSecretKey();
        GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_SIZE, iv);
        Cipher cipher = Cipher.getInstance(Crypto.TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
        return cipher;
    }

    public static String decryptString(Cipher cipher, String encryptedData) throws Exception {
        byte[] decodedData = Base64.decode(encryptedData, Base64.DEFAULT);
        int ivLength = decodedData[0];

        byte[] encryption = new byte[decodedData.length - ivLength - 1];
        System.arraycopy(decodedData, ivLength + 1, encryption, 0, encryption.length);

        byte[] decryptedData = cipher.doFinal(encryption);

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
        byte[] iv = cipher.getIV();
        byte[] encryption = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] encryptedData = new byte[1 + iv.length + encryption.length];

        encryptedData[0] = (byte) iv.length;
        System.arraycopy(iv, 0, encryptedData, 1, iv.length);
        System.arraycopy(encryption, 0, encryptedData, 1 + iv.length, encryption.length);

        return Base64.encodeToString(encryptedData, Base64.DEFAULT);
    }
}
