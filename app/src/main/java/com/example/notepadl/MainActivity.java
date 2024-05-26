package com.example.notepadl;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.notepadl.model.AesKeySpec;
import com.example.notepadl.utils.BiometricUtil;
import com.example.notepadl.utils.CryptoUtil;
import com.example.notepadl.utils.FridaDetectorUtil;
import com.example.notepadl.utils.KeyGeneratorUtil;
import com.example.notepadl.utils.KeyStoreUtil;
import com.example.notepadl.utils.NativeCryptoUtil;
import com.example.notepadl.utils.RootDetectorUtil;
import com.example.notepadl.utils.SharedPrefsUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class MainActivity extends AppCompatActivity {

    public static final String NOTES_PREFS_NAME = "com.example.notepadl.NOTE_PREFS";
    private static final String ENCRYPTED_NOTE = "EncryptedNote";
    private static final String SDCARD_APP_DIR = "Notepadl";
    private static final String DEFAULT_FILE_NAME = "notes.bak";
    private static final int STORAGE_PERMISSION_CODE = 23;
    private static final String TAG = "MainActivity";
    private final ActivityResultLauncher<Intent> storageActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    o -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            if (!Environment.isExternalStorageManager()) {
                                Toast.makeText(this, "Storage Permissions Denied", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
    private EditText contentInputField;
    private EditText titleInputField;
    private Button addButton;
    private ListView notesList;
    private List<String> titleList;
    private ArrayAdapter<String> adapter;
    private SharedPreferences sharedPreferences;
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        Uri uri = data.getData();
                        if (uri != null) {
                            importNotesFromFile(uri);
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isLockscreenEnabled()) {
            closeApp("Cannot use this app without setting system password/pin/pattern");
            return;
        }

        if (RootDetectorUtil.isDeviceRooted(this)) {
            closeApp("Cannot use this app with rooted device");
            return;
        }

        if (FridaDetectorUtil.isFridaInstalled()) {
            closeApp("Cannot use this app when frida-server is installed");
            return;
        }

        try {
            KeyStoreUtil.generateKeyIfNotExists();
        } catch (Exception e) {
            closeApp("Cannot generate application key");
            return;
        }

        setContentView(R.layout.activity_main);

        contentInputField = findViewById(R.id.contentInputField);
        titleInputField = findViewById(R.id.titleInputField);
        addButton = findViewById(R.id.addButton);
        notesList = findViewById(R.id.notesList);

        sharedPreferences = getSharedPreferences(NOTES_PREFS_NAME, MODE_PRIVATE);
        titleList = new ArrayList<>(loadTitlesFromPreferences());
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titleList);
        notesList.setAdapter(adapter);

        addButton.setOnClickListener(v -> {
            String content = contentInputField.getText().toString().trim();
            String title = titleInputField.getText().toString().trim();
            if (content.isEmpty() || title.isEmpty()) {
                Toast.makeText(this, "Note title or content is empty", Toast.LENGTH_SHORT).show();
            } else if (titleList.contains(title)) {
                String message = "Note with title: " + title + " already exists";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            } else {
                BiometricUtil biometricUtil = new BiometricUtil(this, saveNoteCallback(title, content));
                biometricUtil.authenticate();
                contentInputField.setText("");
                titleInputField.setText("");
            }
        });

        notesList.setOnItemClickListener((parent, view, position, id) -> {
            String title = titleList.get(position);
            Optional<String> content = loadContentFromPreferences(title);
            if (content.isPresent()) {
                Intent intent = new Intent(this, DisplayNoteActivity.class);
                intent.putExtra(ENCRYPTED_NOTE, content.get());
                startActivity(intent);
            } else {
                String message = "Cannot retrieve note content";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
            case R.id.import_notes: {
                openFilePickerAndImportNotes();
            }
            break;
            case R.id.export_notes: {
                if (titleList.isEmpty()) {
                    Toast.makeText(this, "There are no notes to export", Toast.LENGTH_LONG).show();
                    return true;
                }
                BiometricUtil biometricUtil = new BiometricUtil(this, exportNotesCallback());
                biometricUtil.authenticate();
            }
            break;
            case R.id.clear_data:
                showClearDataDialog();
                break;
        }
        return true;
    }

    private void requestForStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", this.getPackageName(), null);
                intent.setData(uri);
                storageActivityResultLauncher.launch(intent);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                storageActivityResultLauncher.launch(intent);
            }
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    STORAGE_PERMISSION_CODE
            );
        }
    }

    public boolean checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

            return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0) {
                boolean write = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean read = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                if (read && write) {
                    Toast.makeText(this, "Storage Permissions Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Storage Permissions Denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void saveToFile(byte[] data) throws IOException {
        if (!checkStoragePermissions()) {
            requestForStoragePermissions();
        }
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + File.separator + SDCARD_APP_DIR);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.d(TAG, "Cannot create app sdcard directory");
            }
        }
        File file = new File(dir, DEFAULT_FILE_NAME);
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data);
        fos.close();
        Toast.makeText(this, "File saved: " + file.getPath(), Toast.LENGTH_LONG).show();
    }

    private void showCustomDialog(String title, Consumer<String> consumer) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_custom, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setView(dialogView);

        builder.setPositiveButton("OK", (dialog, which) -> {
            EditText editText = dialogView.findViewById(R.id.editText);
            consumer.accept(editText.getText().toString());
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private boolean isLockscreenEnabled() {
        KeyguardManager keyguardManager = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
        return keyguardManager.isDeviceSecure();
    }

    private Set<String> loadTitlesFromPreferences() {
        return sharedPreferences.getAll().keySet();
    }

    private BiometricPrompt.AuthenticationCallback saveNoteCallback(String title, String content) {
        return new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                try {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    String encryptedContent = CryptoUtil.encryptString(content);
                    editor.putString(title, encryptedContent);
                    editor.apply();
                    titleList.add(title);
                    adapter.notifyDataSetChanged();
                } catch (Exception ex) {
                    Log.e(TAG, "Failed to encrypt and save note", ex);
                }
            }
        };
    }

    private BiometricPrompt.AuthenticationCallback exportNotesCallback() {
        return new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                byte[] serializedNotes;
                try {
                    serializedNotes = SharedPrefsUtil.decryptAndSerialize(sharedPreferences);
                } catch (Exception e) {
                    Log.d(TAG, "Cannot export notes", e);
                    Toast.makeText(MainActivity.this, "Cannot export notes", Toast.LENGTH_SHORT).show();
                    return;
                }
                showCustomDialog("Enter new file password", password -> {
                    try {
                        AesKeySpec keySpec = KeyGeneratorUtil.generateAesKeySpec(password);
                        byte[] encryptedNotes = NativeCryptoUtil.encrypt(serializedNotes, keySpec.getKey(), keySpec.getIv());
                        saveToFile(encryptedNotes);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Cannot export notes", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
    }

    private Optional<String> loadContentFromPreferences(String title) {
        return Optional.ofNullable(sharedPreferences.getString(title, null));
    }

    private void showClearDataDialog() {
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
                        KeyStoreUtil.deleteKey();
                        KeyStoreUtil.generateKeyIfNotExists();
                        Toast.makeText(this, "Data cleared successfully", Toast.LENGTH_LONG).show();
                    } catch (Exception ex) {
                        Toast.makeText(this, "Failed to delete key from Keystore", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void openFilePickerAndImportNotes() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        filePickerLauncher.launch(intent);
    }

    private void importNotesFromFile(Uri uri) {
        showCustomDialog("Enter file password", password -> {
            try {
                byte[] fileContent = readBytesFromUri(uri);
                AesKeySpec keySpec = KeyGeneratorUtil.generateAesKeySpec(password);
                byte[] decryptedBytes = NativeCryptoUtil.decrypt(fileContent, keySpec.getKey(), keySpec.getIv());
                Map<String, String> notes = SharedPrefsUtil.deserialize(decryptedBytes);
                BiometricUtil biometricUtil = new BiometricUtil(this, importNotesCallback(notes));
                biometricUtil.authenticate();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to decrypt data. Wrong password was provided.", Toast.LENGTH_LONG).show();
            }
        });
    }

    public byte[] readBytesFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }
        inputStream.close();
        return byteArrayOutputStream.toByteArray();
    }

    private BiometricPrompt.AuthenticationCallback importNotesCallback(Map<String, String> notes) {
        return new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                try {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    for (Map.Entry<String, String> entry : notes.entrySet()) {
                        String title = entry.getKey();
                        String content = entry.getValue();
                        String encryptedContent = CryptoUtil.encryptString(content);
                        editor.putString(title, encryptedContent);
                        editor.apply();
                        if (!titleList.contains(title)) {
                            titleList.add(title);
                            adapter.notifyDataSetChanged();
                        }
                    }
                    Toast.makeText(MainActivity.this, "Successfully imported notes", Toast.LENGTH_SHORT).show();
                } catch (Exception ex) {
                    Log.e(TAG, "Failed to encrypt and import note", ex);
                }
            }
        };
    }

    private void closeApp(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finishAndRemoveTask();
    }

}
