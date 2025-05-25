package com.example.speechtherapy;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.example.speechtherapy.utils.ProgressDataManager;

public class Song1_readingActivity extends AppCompatActivity {
    private static final int SPEECH_REQUEST_CODE = 100;
    private CardView cardView1, cardView2, cardView3, cardView4, cardView5, cardView6;
    ImageView mic1, mic2, mic3, mic4, mic5, mic6, correct1, correct2, correct3, correct4, correct5, correct6,spek1,spek2,spek3,spek4,spek5,spek6;
    private TextToSpeech textToSpeech;
    private String[] expectedTexts2 = {
            "කිරි සුදු හාවා",
            "පැන පැන ආවා",
            "එළවලු කොටුවේ",
            "දළු කොළ කෑවා",
            "දැක මගේ පඹයා",
            "බිය වී වෙවුලා"
    };
    private String[] expectedTexts = {
            "කිරි සුදු හාවා",
            "පැන පැන ආවා",
            "පැණ පැණ ආවා",
            "එළවලු කොටුවේ",
            "එලවලු කොටුවේ",
            "එලවළු කොටුවේ",
            "දළු කොළ කෑවා",
            "දළු කොල කෑවා",
            "දලු කොල කෑවා",
            "දලු කොළ කෑවා",
            "දැක මගේ පඹයා",
            "බිය වී වෙවුලා"
    };
    private List<List<String>> acceptableVariations = Arrays.asList(
            Arrays.asList("කිරි සුදු හාවා"),
            Arrays.asList("පැන පැන ආවා", "පැණ පැණ ආවා"),
            Arrays.asList("එළවලු කොටුවේ", "එලවලු කොටුවේ", "එලවළු කොටුවේ"),
            Arrays.asList("දළු කොළ කෑවා", "දළු කොල කෑවා", "දලු කොල කෑවා", "දලු කොළ කෑවා"),
            Arrays.asList("දැක මගේ පඹයා"),
            Arrays.asList("බිය වී වෙවුලා")
    );
    private boolean[] isCorrect = new boolean[6];
    ImageView back;
    Button next;
    // Firebase references
    private DatabaseReference databaseReference;
    private String currentUserId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.song1_reading);
        back= findViewById(R.id.home);
        next = findViewById(R.id.next);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openhomeActivity();
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Song2Activity();
            }
        });
        // Initialize Firebase
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("song1").child(currentUserId);



        // Load the stored correctness state from Firebase
        loadCorrectnessState();

        mic1 = findViewById(R.id.mic1);
        mic2 = findViewById(R.id.mic2);
        mic3 = findViewById(R.id.mic3);
        mic4 = findViewById(R.id.mic4);
        mic5 = findViewById(R.id.mic5);
        mic6 = findViewById(R.id.mic6);
        spek1 = findViewById(R.id.speaker1);
        spek2 = findViewById(R.id.speaker2);
        spek3 = findViewById(R.id.speaker3);
        spek4 = findViewById(R.id.speaker4);
        spek5 = findViewById(R.id.speaker5);
        spek6 = findViewById(R.id.speaker6);
        correct1 = findViewById(R.id.correct1);
        correct2 = findViewById(R.id.correct2);
        correct3 = findViewById(R.id.correct3);
        correct4 = findViewById(R.id.correct4);
        correct5 = findViewById(R.id.correct5);
        correct6 = findViewById(R.id.correct6);

        correct1.setVisibility(View.INVISIBLE);
        correct2.setVisibility(View.INVISIBLE);
        correct3.setVisibility(View.INVISIBLE);
        correct4.setVisibility(View.INVISIBLE);
        correct5.setVisibility(View.INVISIBLE);
        correct6.setVisibility(View.INVISIBLE);

        mic1.setOnClickListener(v -> promptSpeechInput(0));
        mic2.setOnClickListener(v -> promptSpeechInput(1));
        mic3.setOnClickListener(v -> promptSpeechInput(2));
        mic4.setOnClickListener(v -> promptSpeechInput(3));
        mic5.setOnClickListener(v -> promptSpeechInput(4));
        mic6.setOnClickListener(v -> promptSpeechInput(5));

        findViewById(R.id.button).setOnClickListener(v -> showResults());
        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
            }
        });

        // Assign click listeners for speakers
        spek1.setOnClickListener(v -> speakText(0));
        spek2.setOnClickListener(v -> speakText(1));
        spek3.setOnClickListener(v -> speakText(2));
        spek4.setOnClickListener(v -> speakText(3));
        spek5.setOnClickListener(v -> speakText(4));
        spek6.setOnClickListener(v -> speakText(5));
    }
    private void speakText(int index) {
        if (textToSpeech != null) {
            textToSpeech.speak(expectedTexts2[index], TextToSpeech.QUEUE_FLUSH, null, null);
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
        Intent intent = new Intent(this,HomeActivity.class);
        startActivity(intent);
    }


    private void promptSpeechInput(int micIndex) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "si-LK"); // Sinhala language
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "කරුණාකර පේළිය කියවන්න"); // Sinhala prompt

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE + micIndex);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition not supported on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String recognizedText = result.get(0).trim().replaceAll("\\s+", " ").toLowerCase();

            int micIndex = requestCode - SPEECH_REQUEST_CODE;

            if (micIndex >= 0 && micIndex < expectedTexts.length) {
                boolean correct = checkTextAgainstVariations(recognizedText, micIndex);
                isCorrect[micIndex] = correct;

                Toast.makeText(this, correct ? "නිවැරදියි!" : "නැවත උත්සාහ කරන්න!", Toast.LENGTH_SHORT).show();

                if (correct) {
                    showCorrectIcon(micIndex);
                    storeCorrectnessState(micIndex, true);
                }
            }
        }
    }

    private boolean checkTextAgainstVariations(String recognizedText, int micIndex) {
        List<String> variations = acceptableVariations.get(micIndex);
        for (String variation : variations) {
            if (recognizedText.equals(variation.trim().replaceAll("\\s+", " ").toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private void showCorrectIcon(int micIndex) {
        switch (micIndex) {
            case 0:
                correct1.setVisibility(View.VISIBLE);
                break;
            case 1:
                correct2.setVisibility(View.VISIBLE);
                break;
            case 2:
                correct3.setVisibility(View.VISIBLE);
                break;
            case 3:
                correct4.setVisibility(View.VISIBLE);
                break;
            case 4:
                correct5.setVisibility(View.VISIBLE);
                break;
            case 5:
                correct6.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void storeCorrectnessState(int micIndex, boolean isCorrect) {
        String micKey = "mic" + (micIndex + 1); // e.g., mic1, mic2, etc.
        databaseReference.child(micKey).setValue(isCorrect);
    }
    // Load the correctness state from Firebase
    private void loadCorrectnessState() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (int i = 0; i < 6; i++) {
                    String micKey = "mic" + (i + 1);
                    Boolean isCorrectValue = dataSnapshot.child(micKey).getValue(Boolean.class);

                    if (isCorrectValue != null && isCorrectValue) {
                        isCorrect[i] = true;
                        switch (i) {
                            case 0:
                                correct1.setVisibility(View.VISIBLE);
                                break;
                            case 1:
                                correct2.setVisibility(View.VISIBLE);
                                break;
                            case 2:
                                correct3.setVisibility(View.VISIBLE);
                                break;
                            case 3:
                                correct4.setVisibility(View.VISIBLE);
                                break;
                            case 4:
                                correct5.setVisibility(View.VISIBLE);
                                break;
                            case 5:
                                correct6.setVisibility(View.VISIBLE);
                                break;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(Song1_readingActivity.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showResults() {
        int correctCount = 0;
        for (boolean correct : isCorrect) {
            if (correct) correctCount++;
        }

        String resultMessage = "ඔබ " + correctCount + " ක් නිවැරදි " + expectedTexts.length + " යෙන්";
        // Store the result in Firebase under the "result" node
        storeResultInFirebase(correctCount);
        // Show AlertDialog with image and result message
        showAlert("පිළිතුරු", resultMessage, R.drawable.happy);
        // Navigate to GameActivity after showing results
        navigateToGameActivity();
    }
    private void storeResultInFirebase(int correctCount) {
        // Calculate percentage score (0-100)
        int totalQuestions = expectedTexts.length;
        int percentageScore = (correctCount * 100) / totalQuestions;
        
        // Create a reference to the "result" node under the current user
        DatabaseReference resultReference = databaseReference.child("result");

        // Store the result in Firebase
        resultReference.setValue(correctCount).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Also store in main user data for overall score tracking
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("user")
                        .child(currentUserId);
                
                userRef.child("sing_song").setValue(percentageScore);
                
                // Store the score with date tracking using ProgressDataManager with Sinhala language
                ProgressDataManager.recordGameScore("sing_song", percentageScore, ProgressDataManager.LANGUAGE_SINHALA);
                
                Toast.makeText(Song1_readingActivity.this, "Result saved successfully.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(Song1_readingActivity.this, "Failed to save result.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAlert(String title, String message, int imageResourceId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_custom, null);

        builder.setView(dialogView);

        ImageView dialogImage = dialogView.findViewById(R.id.dialog_image);
        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);

        dialogImage.setImageResource(imageResourceId);
        dialogTitle.setText(title);
        dialogMessage.setText(message);

        builder.setPositiveButton("හරි", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    public void Song2Activity() {
        Intent intent = new Intent(this, Song2Activity.class);
        startActivity(intent);
    }
    private void navigateToGameActivity() {
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
        finish(); // Optionally call finish() to close the current activity
    }

}