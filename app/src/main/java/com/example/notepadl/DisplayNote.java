package com.example.notepadl;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DisplayNote extends AppCompatActivity {
    private static final String ENCRYPTED_NOTE = "EncryptedNote";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_note);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(ENCRYPTED_NOTE)) {
            String encryptedNote = intent.getStringExtra(ENCRYPTED_NOTE);

            try {
                String decryptedNote = Crypto.decryptString(encryptedNote);
                TextView textView = findViewById(R.id.textView);
                textView.setText(decryptedNote);
            } catch (Exception e) {
                Log.e("DisplayNoteActivity", "Failed to decrypt note", e);
            }
        } else {
            Log.e("DisplayNoteActivity", "No encrypted note provided");
        }
    }
}