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

import com.example.notepadl.utils.BiometricUtil;
import com.example.notepadl.utils.CryptoUtil;

public class DisplayNoteActivity extends AppCompatActivity {
    private static final String TAG = "DisplayNoteActivity";
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
            BiometricUtil biometricUtil = new BiometricUtil(this, displayNoteContent(encryptedNote));
            biometricUtil.authenticate();
        } else {
            Log.e(TAG, "No encrypted note provided");
        }
    }

    private BiometricPrompt.AuthenticationCallback displayNoteContent(String encryptedNote) {
        return new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                try {
                    String decryptedNote = CryptoUtil.decryptString(encryptedNote);
                    TextView textView = findViewById(R.id.sensitiveTextView);
                    textView.setText(decryptedNote);
                } catch (Exception ex) {
                    Log.e(TAG, "Failed to decrypt note", ex);
                }
            }
        };
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