package com.example.notepadl;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

import javax.crypto.Cipher;

public class DisplayNote extends AppCompatActivity {
    private static final String ENCRYPTED_NOTE = "EncryptedNote";
    private View sensitiveView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );

        setContentView(R.layout.activity_display_note);

        sensitiveView = findViewById(R.id.sensitiveTextView);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(ENCRYPTED_NOTE)) {
            String encryptedNote = intent.getStringExtra(ENCRYPTED_NOTE);

            try {
                Cipher cipher = Crypto.getCipherForDecryption(encryptedNote);

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    BiometricPrompt.AuthenticationCallback callback =
                            new BiometricPrompt.AuthenticationCallback() {
                                @Override
                                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                                    super.onAuthenticationSucceeded(result);
                                    try {
                                        String decryptedNote = Crypto.decryptString(cipher, encryptedNote);
                                        TextView textView = findViewById(R.id.sensitiveTextView);
                                        textView.setText(decryptedNote);
                                    } catch (Exception ex) {
                                        Log.e("MainActivity", "Failed to encrypt and save note", ex);
                                    }
                                }
                            };

                    Executor executor = ContextCompat.getMainExecutor(this);
                    BiometricPrompt prompt = new BiometricPrompt(this, executor, callback);
                    Common.setupBiometricPrompt(prompt, cipher);
                } else {
                    //TODO
                }
            } catch (Exception e) {
                Log.e("DisplayNoteActivity", "Failed to decrypt note", e);
            }
        } else {
            Log.e("DisplayNoteActivity", "No encrypted note provided");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensitiveView.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensitiveView.setVisibility(View.VISIBLE);
    }
}