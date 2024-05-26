package com.example.notepadl;

import android.content.Context;
import android.content.pm.PackageManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public final class RootDetectorUtil {
    private RootDetectorUtil() {
    }

    public static boolean isDeviceRooted(Context context) {
        return isSuperuserPresent() || isRootManagementAppPresent(context) ||
                hasDangerousProps() || isSELinuxPermissive() || canCreateFileInSystem();
    }

    private static boolean isSuperuserPresent() {
        String[] paths = {
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su"
        };
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    private static boolean isRootManagementAppPresent(Context context) {
        String[] packages = {
                "com.topjohnwu.magisk",
                "eu.chainfire.supersu",
                "com.noshufou.android.su",
                "com.koushikdutta.superuser",
                "com.thirdparty.superuser",
                "com.yellowes.su",
        };
        PackageManager pm = context.getPackageManager();
        for (String pkg : packages) {
            try {
                pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
                return true;
            } catch (PackageManager.NameNotFoundException e) {
                // Package not found
            }
        }
        return false;
    }

    private static boolean isSELinuxPermissive() {
        String selinuxStatus = getSELinuxStatus();
        return "Permissive".equals(selinuxStatus);
    }

    private static String getSELinuxStatus() {
        String line;
        try {
            Process process = Runtime.getRuntime().exec("getenforce");
            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            line = input.readLine();
            input.close();
        } catch (IOException ex) {
            return null;
        }
        return line;
    }

    private static String getSystemProperty(String propName) {
        String line;
        BufferedReader input = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop " + propName);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException ex) {
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return line;
    }

    private static boolean hasDangerousProps() {
        String[] lines = {
                "ro.debuggable=1",
                "ro.secure=0"
        };
        for (String line : lines) {
            String prop = getSystemProperty(line.split("=")[0]);
            if (prop != null && prop.equals(line.split("=")[1])) {
                return true;
            }
        }
        return false;
    }

    private static boolean canCreateFileInSystem() {
        File testFile = new File("/system/test.txt");
        boolean canWrite = false;
        try {
            if (testFile.createNewFile()) {
                canWrite = true;
                testFile.delete();
            }
        } catch (IOException e) {
            // Ignore as this is expected on non-rooted devices
        }
        return canWrite;
    }

}
