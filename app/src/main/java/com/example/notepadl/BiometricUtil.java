package com.example.notepadl;

import android.util.Log;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

public class BiometricUtil {

    private final FragmentActivity activity;
    private final BiometricPrompt.AuthenticationCallback authenticationCallback;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    public BiometricUtil(FragmentActivity activity, BiometricPrompt.AuthenticationCallback authenticationCallback) {
        this.activity = activity;
        this.authenticationCallback = authenticationCallback;
        initBiometricPrompt();
    }

    private void initBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(activity);
        biometricPrompt = new BiometricPrompt(activity, executor, authenticationCallback);

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unblock crypto operations with PIN")
                .setSubtitle("Provide your PIN")
                .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
    }

    public void authenticate() {
        BiometricManager biometricManager = BiometricManager.from(activity);
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricPrompt.authenticate(promptInfo);
        } else {
            Log.d("Biometric", "Biometric authentication is not available or not set up on this device.");
        }
    }
}
