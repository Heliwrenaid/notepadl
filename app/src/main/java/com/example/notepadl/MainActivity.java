package com.example.notepadl;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "com.example.notepadl.PREFS";
    private static final String ENCRYPTED_NOTE = "EncryptedNote";
    private final Crypto crypto = new Crypto();

    private EditText contentInputField;
    private EditText titleInputField;
    private Button addButton;
    private ListView notesList;
    private List<String> titleList;
    private ArrayAdapter<String> adapter;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isLockscreenEnabled()) {
            String message = "App requires setting a system password/pin/pattern to work";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            finishAndRemoveTask();
        }

        setContentView(R.layout.activity_main);

        contentInputField = findViewById(R.id.contentInputField);
        titleInputField = findViewById(R.id.titleInputField);
        addButton = findViewById(R.id.addButton);
        notesList = findViewById(R.id.notesList);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        titleList = new ArrayList<>(loadTitlesFromPreferences());
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titleList);
        notesList.setAdapter(adapter);

        addButton.setOnClickListener(v -> {
            String content = contentInputField.getText().toString().trim();
            String title = titleInputField.getText().toString().trim();
            if (content.isEmpty() || title.isEmpty()) {
                Toast.makeText(MainActivity.this, "Note title or content is empty", Toast.LENGTH_SHORT).show();
            } else if (titleList.contains(title)) {
                String message = "Note with title: " + title + " already exists";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            } else {
                saveNote(title, content);
                contentInputField.setText("");
                titleInputField.setText("");
            }
        });

        notesList.setOnItemClickListener((parent, view, position, id) -> {
            String title = titleList.get(position);
            Optional<String> content = loadContentFromPreferences(title);
            if (content.isPresent()) {
                Intent intent = new Intent(this, DisplayNote.class);
                intent.putExtra(ENCRYPTED_NOTE, content.get());
                startActivity(intent);
            } else {
                String message = "Cannot retrieve note content";
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.import_notes:
                Toast.makeText(this, "Import selected", Toast.LENGTH_SHORT).show();
                //TODO: implement notes import
            case R.id.export_notes:
                Toast.makeText(this, "Export selected", Toast.LENGTH_SHORT).show();
                //TODO: implement notes export
            case R.id.clear_data:
                showConfirmationDialog();
        }
        return true;
    }

    private boolean isLockscreenEnabled() {
        KeyguardManager keyguardManager = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
        return keyguardManager.isDeviceSecure();
    }

    private Set<String> loadTitlesFromPreferences() {
        return sharedPreferences.getAll().keySet();
    }

    private void saveNote(String title, String content) {
        try {
            SecretKey secretKey = Crypto.getSecretKey();
            Cipher cipher = Cipher.getInstance(Crypto.TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                BiometricPrompt.AuthenticationCallback callback =
                        new BiometricPrompt.AuthenticationCallback() {
                            @Override
                            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                                super.onAuthenticationSucceeded(result);
                                try {
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    String encryptedContent = crypto.encryptString(cipher, content);
                                    editor.putString(title, encryptedContent);
                                    editor.apply();
                                    titleList.add(title);
                                    adapter.notifyDataSetChanged();
                                } catch (Exception ex) {
                                    Log.e("MainActivity", "Failed to encrypt and save note", ex);
                                }
                            }
                        };

                Executor executor = ContextCompat.getMainExecutor(this);
                BiometricPrompt prompt = new BiometricPrompt(this, executor, callback);
                Common.setupBiometricPrompt(prompt, cipher);
            }
        } catch (Exception ex) {
            Toast.makeText(this, "Failed to encrypt message", Toast.LENGTH_LONG).show();
            Log.e("MainActivity", "Failed invoke encrypt operation in secure context", ex);
        }
    }

    private Optional<String> loadContentFromPreferences(String title) {
        return Optional.ofNullable(sharedPreferences.getString(title, null));
    }

    private void showConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm")
                .setMessage("Are you sure you want to delete all notes?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.clear();
                    editor.apply();
                    titleList.clear();
                    adapter.notifyDataSetChanged();
                    try {
                        Crypto.deleteKey();
                        Toast.makeText(this, "Data cleared successfully", Toast.LENGTH_LONG).show();
                    } catch (Exception ex) {
                        Toast.makeText(this, "Failed to delete key from Keystore", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }
}
