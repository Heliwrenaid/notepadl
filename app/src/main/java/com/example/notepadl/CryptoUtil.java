package com.example.notepadl;

import android.util.Base64;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class CryptoUtil {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    private CryptoUtil() {}

    public static String decryptString(String encryptedText) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKey key = KeyStoreUtil.getKey();

        byte[] encryptedBytes = Base64.decode(encryptedText, Base64.DEFAULT);

        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedBytes, 0, iv, 0, iv.length);

        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] original = cipher.doFinal(encryptedBytes, iv.length, encryptedBytes.length - iv.length);
        return new String(original, StandardCharsets.UTF_8);
    }

    public static String encryptString(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKey key = KeyStoreUtil.getKey();

        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] iv = cipher.getIV();
        byte[] encryption = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryption.length);
        byteBuffer.put(iv);
        byteBuffer.put(encryption);

        return Base64.encodeToString(byteBuffer.array(), Base64.DEFAULT);
    }
}
