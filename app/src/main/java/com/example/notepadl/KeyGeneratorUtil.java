package com.example.notepadl;

import com.example.notepadl.model.AesKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class KeyGeneratorUtil {
    private KeyGeneratorUtil() {}

    public static AesKeySpec generateAesKeySpec(String seed) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha256.digest(seed.getBytes(StandardCharsets.UTF_8));
        byte[] key = Arrays.copyOfRange(hash, 0, 16);
        byte[] iv = Arrays.copyOfRange(hash, 16, 32);
        return new AesKeySpec(key, iv);
    }
}
