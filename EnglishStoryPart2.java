package com.example.speechtherapy;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class EnglishStoryPart2 extends AppCompatActivity {
    private ImageView speaker, correctImage;
    private CheckBox checkboxAnswer1, checkboxAnswer2, checkboxAnswer3, checkboxAnswer4;
    private Button button,next;
    private DatabaseReference databaseReference;
    private TextToSpeech textToSpeech;
    ImageView back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.english_story_part2);
        speaker = findViewById(R.id.speaker);
        correctImage = findViewById(R.id.correct);
        correctImage.setVisibility(View.INVISIBLE); // Initially hide correct image
        checkboxAnswer1 = findViewById(R.id.checkbox_answer1);
        checkboxAnswer2 = findViewById(R.id.checkbox_answer2);
        checkboxAnswer3 = findViewById(R.id.checkbox_answer3);
        checkboxAnswer4 = findViewById(R.id.checkbox_answer4);
        button = findViewById(R.id.button);
        next = findViewById(R.id.next);
        next.setOnClickListener(v -> openstory2Activity());
        back= findViewById(R.id.home);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openhomeActivity();
            }
        });
        VideoView videoView = findViewById(R.id.videoView);
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.engstory2);
        videoView.setVideoURI(videoUri);
        MediaController mediaController = new MediaController(this);
        videoView.setMediaController(mediaController);
        mediaController.setAnchorView(videoView);
        videoView.start();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("enStory").child(currentUserId);
        databaseReference.child("part2").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isCorrect = snapshot.getValue(Boolean.class);
                if (isCorrect != null && isCorrect) {
                    correctImage.setVisibility(View.VISIBLE); // Show correct icon if part2 is true
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EnglishStoryPart2.this, "Failed to load data!", Toast.LENGTH_SHORT).show();
            }
        });

        setupSingleSelection(checkboxAnswer1, checkboxAnswer2, checkboxAnswer3, checkboxAnswer4);
        button.setOnClickListener(v -> {
            if (checkboxAnswer2.isChecked()) {  // Correct answer
                correctImage.setVisibility(View.VISIBLE);
                saveToFirebase("part2", true);
                Toast.makeText(this, "Correct! Moving to next part...", Toast.LENGTH_SHORT).show();
            } else {  // Wrong answer
                correctImage.setVisibility(View.INVISIBLE);
                saveToFirebase("part2", false);
                showAlertDialog();
            }
        });
        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Set the language to US English
                textToSpeech.setLanguage(Locale.US);
            }
        });
        // Set speaker button to read Sinhala text
        speaker.setOnClickListener(v -> {
            String text = "What places did the dog run passed?  Bus Stop and Hospital,Train Station and School, Library and School, Car and Bike";
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        });
    }
    private void setupSingleSelection(CheckBox... checkBoxes) {
        for (CheckBox checkBox : checkBoxes) {
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    for (CheckBox cb : checkBoxes) {
                        if (cb != buttonView) {
                            cb.setChecked(false);
                        }
                    }
                }
            });
        }
    }
    private void saveToFirebase(String part, boolean isCorrect) {
        databaseReference.child(part).setValue(isCorrect)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Saved to Firebase!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save!", Toast.LENGTH_SHORT).show());
    }
    private void showAlertDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Incorrect Answer")
                .setMessage("Your answer is incorrect, please listen carefully to the dialogue and provide the correct answer!")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }
    public void openhomeActivity() {
        Intent intent = new Intent(this,HomeActivity.class);
        startActivity(intent);
    }
    public void openstory2Activity() {
        Intent intent = new Intent(this, EnglishStoryPart3.class);
        startActivity(intent);
    }
}