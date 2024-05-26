package com.example.notepadl.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

public final class FridaDetectorUtil {

    private FridaDetectorUtil() {
    }

    public static boolean isFridaInstalled() {
        return isFridaPortOpen() || isFridaProcessRunning();
    }

    private static boolean isFridaProcessRunning() {
        String[] commands = {
                "ps -A",
                "ps"
        };
        for (String command : commands) {
            try {
                Process process = Runtime.getRuntime().exec(command);
                BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.contains("frida-server")) {
                        return true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static boolean isFridaPortOpen() {
        int[] ports = {27042, 27043}; // Default ports used by Frida
        for (int port : ports) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", port), 200);
                return true;
            } catch (IOException e) {
                // Port is not open
            }
        }
        return false;
    }
}
