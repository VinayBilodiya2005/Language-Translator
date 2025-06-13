package com.example.languatranslator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Spinner fromSpinner, toSpinner;
    private TextInputEditText sourceEdt;
    private ImageView micIV;
    private MaterialButton translateBtn;
    private TextView translatedTV;

    String[] fromLanguages = {"From", "English", "Afrikaans", "Arabic", "Bengali", "Catalan", "Czech", "Hindi", "Urdu"};
    String[] toLanguages = {"To", "English", "Afrikaans", "Arabic", "Bengali", "Catalan", "Czech", "Hindi", "Urdu"};

    private static final int REQUEST_PERMISSION_CODE = 1;
    private static final int MICROPHONE_PERMISSION_CODE = 2;
    private static final int DOWNLOAD_TIMEOUT_MS = 30000; // 30 seconds timeout

    int fromLanguageCode = 0, toLanguageCode = 0;
    private Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupSpinners();
        setupClickListeners();
    }

    private void initializeViews() {
        fromSpinner = findViewById(R.id.idFromSpinner);
        toSpinner = findViewById(R.id.idTopSpinner);
        sourceEdt = findViewById(R.id.idEdtSource);
        micIV = findViewById(R.id.idIVMic);
        translateBtn = findViewById(R.id.idBtnTranslate);
        translatedTV = findViewById(R.id.idTVTranslatedTV);
    }

    private void setupSpinners() {
        // Setup From Spinner
        ArrayAdapter<String> fromAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fromLanguages);
        fromAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fromSpinner.setAdapter(fromAdapter);

        fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fromLanguageCode = getLanguageCode(fromLanguages[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Setup To Spinner
        ArrayAdapter<String> toAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, toLanguages);
        toAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        toSpinner.setAdapter(toAdapter);

        toSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                toLanguageCode = getLanguageCode(toLanguages[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupClickListeners() {
        translateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                translateText();
            }
        });

        micIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestMicrophonePermission();
            }
        });
    }

    private void translateText() {
        translatedTV.setText("");
        String sourceText = sourceEdt.getText().toString().trim();

        if (sourceText.isEmpty()) {
            Toast.makeText(MainActivity.this, "Please enter your text to translate", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fromLanguageCode == 0) {
            Toast.makeText(MainActivity.this, "Please select the source language", Toast.LENGTH_SHORT).show();
            return;
        }

        if (toLanguageCode == 0) {
            Toast.makeText(MainActivity.this, "Please select the target language", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fromLanguageCode == toLanguageCode) {
            Toast.makeText(MainActivity.this, "Source and target languages cannot be the same", Toast.LENGTH_SHORT).show();
            return;
        }

        performTranslation(sourceText);
    }

    private void performTranslation(String sourceText) {
        translatedTV.setText("Preparing translation...");
        translateBtn.setEnabled(false); // Disable button during translation

        // Set up timeout
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                translatedTV.setText("Translation timeout");
                translateBtn.setEnabled(true);
                Toast.makeText(MainActivity.this, "Translation timed out. Please check your internet connection and try again.", Toast.LENGTH_LONG).show();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, DOWNLOAD_TIMEOUT_MS);

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(getMLKitLanguageCode(fromLanguageCode))
                .setTargetLanguage(getMLKitLanguageCode(toLanguageCode))
                .build();

        Translator translator = Translation.getClient(options);

        // Use more permissive download conditions
        DownloadConditions conditions = new DownloadConditions.Builder()
                .build(); // Remove WiFi requirement - allow mobile data

        // First check if model is already downloaded by trying translation
        translatedTV.setText("Checking models...");

        translator.translate(sourceText)
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String translatedText) {
                        // Cancel timeout
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        translatedTV.setText(translatedText);
                        translateBtn.setEnabled(true);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // If translation fails, try downloading the model
                        translatedTV.setText("Downloading language model... This may take a few minutes on first use.");

                        translator.downloadModelIfNeeded(conditions)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        translatedTV.setText("Models ready. Translating...");
                                        // Now try translation again
                                        translator.translate(sourceText)
                                                .addOnSuccessListener(new OnSuccessListener<String>() {
                                                    @Override
                                                    public void onSuccess(String translatedText) {
                                                        // Cancel timeout
                                                        timeoutHandler.removeCallbacks(timeoutRunnable);
                                                        translatedTV.setText(translatedText);
                                                        translateBtn.setEnabled(true);
                                                        Toast.makeText(MainActivity.this, "Translation completed!", Toast.LENGTH_SHORT).show();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        // Cancel timeout
                                                        timeoutHandler.removeCallbacks(timeoutRunnable);
                                                        translatedTV.setText("Translation failed");
                                                        translateBtn.setEnabled(true);
                                                        Toast.makeText(MainActivity.this, "Translation failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Cancel timeout
                                        timeoutHandler.removeCallbacks(timeoutRunnable);
                                        translatedTV.setText("Download failed");
                                        translateBtn.setEnabled(true);

                                        String errorMsg = "Model download failed: ";
                                        if (e.getMessage() != null) {
                                            if (e.getMessage().contains("network")) {
                                                errorMsg += "Check your internet connection";
                                            } else if (e.getMessage().contains("storage")) {
                                                errorMsg += "Not enough storage space";
                                            } else {
                                                errorMsg += e.getMessage();
                                            }
                                        } else {
                                            errorMsg += "Unknown error. Please check your internet connection.";
                                        }

                                        Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                });
    }

    private void requestMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MICROPHONE_PERMISSION_CODE);
        } else {
            startSpeechToText();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MICROPHONE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSpeechToText();
            } else {
                Toast.makeText(this, "Microphone permission is required for speech input", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to convert into text");

        try {
            startActivityForResult(intent, REQUEST_PERMISSION_CODE);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Speech recognition not supported: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null && !result.isEmpty()) {
                    sourceEdt.setText(result.get(0));
                }
            }
        }
    }

    private int getLanguageCode(String language) {
        switch (language) {
            case "English":
                return 1;
            case "Afrikaans":
                return 2;
            case "Arabic":
                return 3;
            case "Bengali":
                return 4;
            case "Catalan":
                return 5;
            case "Czech":
                return 6;
            case "Hindi":
                return 7;
            case "Urdu":
                return 8;
            default:
                return 0;
        }
    }

    private String getMLKitLanguageCode(int languageCode) {
        switch (languageCode) {
            case 1:
                return TranslateLanguage.ENGLISH;
            case 2:
                return TranslateLanguage.AFRIKAANS;
            case 3:
                return TranslateLanguage.ARABIC;
            case 4:
                return TranslateLanguage.BENGALI;
            case 5:
                return TranslateLanguage.CATALAN;
            case 6:
                return TranslateLanguage.CZECH;
            case 7:
                return TranslateLanguage.HINDI;
            case 8:
                return TranslateLanguage.URDU;
            default:
                return TranslateLanguage.ENGLISH;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel timeout handler to prevent memory leaks
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
    }
}