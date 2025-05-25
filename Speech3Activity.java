package com.example.speechtherapy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
public class Speech3Activity extends AppCompatActivity {
    ImageView home,correct;
    Button button1,record;
    private static final int SPEECH_REQUEST_CODE = 1;
    private ImageView micImageView, speakerImageView;
    private TextToSpeech textToSpeech;
    private String targetWord = "කුමාරයා";
    private MediaRecorder mediaRecorder;
    private String outputFilePath;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private android.app.AlertDialog alertDialog;
    private CountDownTimer countDownTimer;
    private TextView timerText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.speech3);
        micImageView = findViewById(R.id.mic);
        speakerImageView = findViewById(R.id.speaker);
        correct= findViewById(R.id.correct);
        home = findViewById(R.id.home);
        record= findViewById(R.id.record);
        button1 = findViewById(R.id.button1);
        VideoView videoView = findViewById(R.id.videoView);
        //video
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.kumaraya);
        // Set the video URI
        videoView.setVideoURI(videoUri);
        // Set MediaController for video controls (optional)
        MediaController mediaController = new MediaController(this);
        videoView.setMediaController(mediaController);
        mediaController.setAnchorView(videoView);
        // Start the video automatically
        videoView.start();
        record.setOnClickListener(view -> showRecordingDialog());
        // Check for permissions
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        }
        videoView.start();
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openspeechActivity();
            }
        });
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openhomeActivity();
            }
        });
        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int langResult = textToSpeech.setLanguage(new Locale("si", "LK"));
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "Sinhala language not supported on this device", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show();
            }
        });
        // Set up the Mic click listener for Speech-to-Text
        micImageView.setOnClickListener(view -> startSpeechToText());
        // Set up the Speaker click listener for Text-to-Speech
        speakerImageView.setOnClickListener(view -> speakText(targetWord));
        // Check Firebase for spell_1 value
        checkSpellStatus();
    }
    // Method to start Speech-to-Text
    private void startSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "si-LK"); // Set Sinhala language
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "කුමාරයා");
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Your device doesn't support Speech Recognition", Toast.LENGTH_SHORT).show();
        }
    }
    // Handling Speech-to-Text result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && results.size() > 0) {
                String spokenText = results.get(0);

                // Firebase reference to the current user
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("spell").child(currentUserId);

                // Retrieve existing spell_3 node value
                userRef.child("spell_3").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        long spell_3Value = snapshot.getValue(Long.class) != null ? snapshot.getValue(Long.class) : 0;

                        // If spokenText matches targetWord
                        if (spokenText.equals(targetWord)) {
                            correct.setVisibility(View.VISIBLE);
                            // Increment count node and set spell_1 to 1 if not already set
                            userRef.child("spell_3").setValue(1);
                            Snackbar.make(findViewById(R.id.profile_section), "You are correct!", Snackbar.LENGTH_LONG).show();
                        } else {
                            correct.setVisibility(View.GONE);
                            Snackbar.make(findViewById(R.id.profile_section), "Try again!", Snackbar.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Snackbar.make(findViewById(R.id.profile_section), "Error accessing database", Snackbar.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    // Method to handle Text-to-Speech
    private void speakText(String text) {
        if (textToSpeech != null && textToSpeech.isLanguageAvailable(new Locale("si", "LK")) >= TextToSpeech.LANG_AVAILABLE) {
            textToSpeech.setSpeechRate(0.3f);
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_UTTERANCE_ID");
        } else {
            Toast.makeText(this, "Sinhala TTS is not supported on this device", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
    public void openhomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }
    private void showRecordingDialog() {
        // Create the AlertDialog with custom layout (image, text, and buttons)
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_recording, null);
        ImageView imageView = dialogView.findViewById(R.id.recordingImage);
        Button startButton = dialogView.findViewById(R.id.startButton);
        Button stopButton = dialogView.findViewById(R.id.stopButton);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        ImageView closeButton = dialogView.findViewById(R.id.closeButton);
        TextView path =dialogView.findViewById(R.id.recordpath);
        timerText = dialogView.findViewById(R.id.Text);
        // Create the AlertDialog
        final android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        // Show the dialog
        alertDialog.show();
        startButton.setOnClickListener(v -> {
            startRecording();
            path.setText("Recording path: " + outputFilePath); // Set the recording path
            startTimer();
        });
        stopButton.setOnClickListener(view -> {
            stopRecording(alertDialog);
            stopTimer();
        });
        // Handle Save button click
        saveButton.setOnClickListener(view -> saveRecording(alertDialog, path));  // Pass alertDialog here
        closeButton.setOnClickListener(v -> alertDialog.dismiss());
    }
    private void saveRecording(android.app.AlertDialog alertDialog, TextView path) {
        File file = new File(outputFilePath);
        if (file.exists()) {
            path.setText("Recording saved to: " + outputFilePath);
            // Optionally, you can copy or move the file to a permanent location if needed
            Snackbar.make(findViewById(R.id.profile_section), "Recording saved: " + outputFilePath, Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(findViewById(R.id.profile_section), "Failed to save recording", Snackbar.LENGTH_LONG).show();
        }
    }
    private void stopRecording(android.app.AlertDialog alertDialog) {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            Snackbar.make(findViewById(R.id.profile_section), "Recording stopped", Snackbar.LENGTH_LONG).show();
        }
    }
    private void startRecording() {
        // Show the dialog to indicate recording is in progress
        AlertDialog.Builder builder = new AlertDialog.Builder(Speech3Activity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_recording, null);
        builder.setView(dialogView);
        // Set up recording
        outputFilePath = getExternalCacheDir().getAbsolutePath() + "/speech_audio3.3gp"; // Save to cache directory
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(outputFilePath);
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            Snackbar.make(findViewById(R.id.profile_section), "Recording started", Snackbar.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
        }
    }
    private void startTimer() {
        countDownTimer = new CountDownTimer(Long.MAX_VALUE, 1000) { // Timer set to maximum value and ticks every second
            private int seconds = 0;
            @Override
            public void onTick(long millisUntilFinished) {
                seconds++;
                int minutes = seconds / 60;
                int remainingSeconds = seconds % 60;
                timerText.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds));
            }
            @Override
            public void onFinish() {
                // Timer will not finish as we set it to Long.MAX_VALUE; you can ignore this.
            }
        };
        countDownTimer.start();
    }
    private void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            timerText.setText("00:00"); // Reset timer display
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with recording
            } else {
                Toast.makeText(this, "Permission to record audio denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void checkSpellStatus() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("spell").child(currentUserId);

        userRef.child("spell_3").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long spell_1Value = snapshot.getValue(Long.class) != null ? snapshot.getValue(Long.class) : 0;
                if (spell_1Value == 1) {
                    correct.setVisibility(View.VISIBLE);
                } else {
                    correct.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(), "Failed to fetch spell status", Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void openspeechActivity() {
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
    }
}