package com.example.notepadl;

import static com.example.notepadl.MainActivity.NOTES_PREFS_NAME;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

import javax.crypto.Cipher;

public class SharedActivity extends AppCompatActivity {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_RESULT = "result";
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_TITLE)) {
            sharedPreferences = getSharedPreferences(NOTES_PREFS_NAME, MODE_PRIVATE);
            String title = intent.getStringExtra(EXTRA_TITLE);
            getNoteContent(title);
        } else {
            finishWithError("Note title was not provided");
        }
    }

    private void getNoteContent(String title) {
        try {
            String encryptedNote = sharedPreferences.getString(title, null);
            if (encryptedNote == null) {
                finishWithError("Cannot find note with title: " + title);
                return;
            }

            Cipher cipher = Crypto.getCipherForDecryption(encryptedNote);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                BiometricPrompt.AuthenticationCallback callback =
                        new BiometricPrompt.AuthenticationCallback() {
                            @Override
                            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                                super.onAuthenticationSucceeded(result);
                                try {
                                    String decryptedNote = Crypto.decryptString(cipher, encryptedNote);
                                    finishWithSuccess(decryptedNote);
                                } catch (Exception ex) {
                                    finishWithError("Failed to decrypt note");
                                }
                            }
                        };
                Executor executor = ContextCompat.getMainExecutor(this);
                BiometricPrompt prompt = new BiometricPrompt(this, executor, callback);
                Common.setupBiometricPrompt(prompt, cipher);
            } else {
                //TODO
                finishWithError("Unsupported Android SDK");
            }
        } catch (Exception e) {
            finishWithError("Failed to decrypt note");
        }
    }

    private void finishWithSuccess(String result) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_RESULT, result);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void finishWithError(String error) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_RESULT, error);
        setResult(RESULT_CANCELED, resultIntent);
        finish();
    }

}
