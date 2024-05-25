package com.example.notepadl;

import android.content.SharedPreferences;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

public class SharedPrefsUtil {
    private SharedPrefsUtil() {
    }

    public static byte[] decryptAndSerialize(SharedPreferences sharedPreferences) throws Exception {
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, ?> entry : sharedPreferences.getAll().entrySet()) {
            String encryptedNote = (String) entry.getValue();
            Cipher cipher = Crypto.getCipherForDecryption(encryptedNote);
            //TODO: fix error with no authenticated cipher and IV reusing
            String decrypted = Crypto.decryptString(cipher, encryptedNote);
            map.put(entry.getKey(), decrypted);
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(map);
        objectOutputStream.close();
        return byteArrayOutputStream.toByteArray();
    }

    public static Map<String, String> deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        Map<String, String> allEntries = (Map<String, String>) objectInputStream.readObject();
        objectInputStream.close();
        return allEntries;
    }
}
