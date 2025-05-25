package com.example.speechtherapy;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.net.Uri;
import android.widget.VideoView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Spell1Activity extends AppCompatActivity {

    ImageView mic1, mic2, mic3, correct1, correct2, correct3;
    ImageView back;
    Button next;
    private static final int SPEECH_REQUEST_CODE = 1;
    private List<String> validWords = Arrays.asList("ruff","rough","rough rough", "ruff ruff", "ruff ruff ruff");
    private String currentMic = "";
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.spell1);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        String userId = mAuth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("spell_dely").child(userId);

        // Initialize views
        next = findViewById(R.id.next);
        mic1 = findViewById(R.id.mic1);
        mic2 = findViewById(R.id.mic2);
        mic3 = findViewById(R.id.mic3);
        correct1 = findViewById(R.id.correct1);
        correct2 = findViewById(R.id.correct2);
        correct3 = findViewById(R.id.correct3);

        // Hide correct indicators initially
        correct1.setVisibility(View.INVISIBLE);
        correct2.setVisibility(View.INVISIBLE);
        correct3.setVisibility(View.INVISIBLE);
// Set correct indicators based on Firebase data
        updateCorrectIndicatorsFromFirebase();
        // Video playback setup
        VideoView videoView = findViewById(R.id.videoView);
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.dog);
        videoView.setVideoURI(videoUri);
        MediaController mediaController = new MediaController(this);
        videoView.setMediaController(mediaController);
        mediaController.setAnchorView(videoView);
        videoView.start();
        back= findViewById(R.id.home);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openhomeActivity();
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                opennextActivity();
            }
        });
        // Set click listeners
        mic1.setOnClickListener(v -> startSpeechRecognition("mic1"));
        mic2.setOnClickListener(v -> startSpeechRecognition("mic2"));
        mic3.setOnClickListener(v -> startSpeechRecognition("mic3"));
    }

    private void startSpeechRecognition(String mic) {
        currentMic = mic; // Set the current mic button
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Lets try ruff ruff");

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition is not supported on your device.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0).toLowerCase().trim();

            if (validWords.contains(spokenText)) {
                updateFirebaseAndUI(true);
            } else {
                updateFirebaseAndUI(false);
                showRetryDialog();
            }
        }
    }

    private void updateFirebaseAndUI(boolean isCorrect) {
        if (currentMic.equals("mic1")) {
            userRef.child("mic1").setValue(isCorrect);
            correct1.setVisibility(isCorrect ? View.VISIBLE : View.INVISIBLE);
        } else if (currentMic.equals("mic2")) {
            userRef.child("mic2").setValue(isCorrect);
            correct2.setVisibility(isCorrect ? View.VISIBLE : View.INVISIBLE);
        } else if (currentMic.equals("mic3")) {
            userRef.child("mic3").setValue(isCorrect);
            correct3.setVisibility(isCorrect ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void showRetryDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Try Again")
                .setMessage("Incorrect pronunciation. Please try again.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }
    private void updateCorrectIndicatorsFromFirebase() {
        userRef.child("mic1").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().getValue() != null) {
                boolean mic1Correct = (Boolean) task.getResult().getValue();
                correct1.setVisibility(mic1Correct ? View.VISIBLE : View.INVISIBLE);
            }
        });

        userRef.child("mic2").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().getValue() != null) {
                boolean mic2Correct = (Boolean) task.getResult().getValue();
                correct2.setVisibility(mic2Correct ? View.VISIBLE : View.INVISIBLE);
            }
        });

        userRef.child("mic3").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().getValue() != null) {
                boolean mic3Correct = (Boolean) task.getResult().getValue();
                correct3.setVisibility(mic3Correct ? View.VISIBLE : View.INVISIBLE);
            }
        });
    }
    public void openhomeActivity() {
        Intent intent = new Intent(this,HomeActivity.class);
        startActivity(intent);
    }
    public void opennextActivity() {
        Intent intent = new Intent(this,Spell2Activity.class);
        startActivity(intent);
    }
}
