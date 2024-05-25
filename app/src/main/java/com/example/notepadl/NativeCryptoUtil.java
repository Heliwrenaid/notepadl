package com.example.notepadl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class NativeCryptoUtil {
    private final static int INT_SIZE = 4;
    private final static int AES_BLOCK_SIZE = 16;

    static {
        System.loadLibrary("aeslib");
    }

    private NativeCryptoUtil() {
    }

    private static native byte[] encryptInternal(byte[] input, byte[] key, byte[] iv);

    private static native byte[] decryptInternal(byte[] input, byte[] key, byte[] iv);

    public static byte[] encrypt(byte[] input, byte[] key, byte[] iv) throws IOException {
        return encryptInternal(addPadding(input), key, iv);
    }

    public static byte[] decrypt(byte[] input, byte[] key, byte[] iv) {
        return removePadding(decryptInternal(input, key, iv));
    }

    private static byte[] addPadding(byte[] data) throws IOException {
        int outputWIthPaddingInfoLength = data.length + INT_SIZE;
        int paddingLength = 16 - (outputWIthPaddingInfoLength % AES_BLOCK_SIZE);
        byte[] paddingLengthBytes = ByteBuffer.allocate(INT_SIZE).putInt(paddingLength).array();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(paddingLengthBytes);
        output.write(data);

        byte[] padding = new byte[paddingLength];
        Arrays.fill(padding, (byte) 0);
        output.write(padding);

        return output.toByteArray();
    }

    //TODO: reduce number of array copies
    private static byte[] removePadding(byte[] data) {
        int paddingLength = ByteBuffer.wrap(data, 0, INT_SIZE).getInt();
        if (paddingLength < 0 | paddingLength > data.length) {
            throw new RuntimeException("Cannot decrypt data");
        }
        byte[] outputWithoutPaddingInfo = Arrays.copyOfRange(data, INT_SIZE, data.length);
        if (paddingLength == 0) {
            return outputWithoutPaddingInfo;
        } else {
            int outputLength = outputWithoutPaddingInfo.length - paddingLength;
            if (outputLength < 0 || outputLength > data.length) {
                throw new RuntimeException("Cannot decrypt data");
            }
            return Arrays.copyOfRange(outputWithoutPaddingInfo, 0, outputLength);
        }
    }
}
