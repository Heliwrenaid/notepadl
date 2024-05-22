package com.example.notepadl;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

import javax.crypto.Cipher;

public class Common {

    public static void setupBiometricPrompt(BiometricPrompt prompt, Cipher cipher) {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unblock crypto operations with PIN")
                .setSubtitle("Provide your PIN")
                .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
        prompt.authenticate(promptInfo, new BiometricPrompt.CryptoObject(cipher));
    }
}
