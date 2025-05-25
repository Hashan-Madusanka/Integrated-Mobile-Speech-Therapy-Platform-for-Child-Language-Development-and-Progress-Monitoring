package com.example.speechtherapy;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

public class EnglishSong1_reading extends AppCompatActivity {

    private static final int SPEECH_REQUEST_CODE = 100;
    private CardView cardView1,cardView2,cardView3,cardView4,cardView5,cardView6;
    ImageView mic1, mic2, mic3, mic4, mic5, mic6, correct1, correct2, correct3, correct4, correct5, correct6,spek1,spek2,spek3,spek4,spek5,spek6;
    private TextToSpeech textToSpeech;

    private String[] expectedTexts = {
            "Twinkle Twinkle",
            "Little Star",
            "How I wonder",
            "What You are",
            "Like a Diamond",
            "In the Sky"
    };

    private List<List<String>> acceptableVariations = Arrays.asList(
            Arrays.asList("Twinkle Twinkle","twinkle twinkle"),
            Arrays.asList("Little star","little star"),
            Arrays.asList("How I wonder","how i wonder"),
            Arrays.asList("What You are", "what you are"),
            Arrays.asList("Like a Diamond", "like a diamond"),
            Arrays.asList("In the Sky", "in the sky")
    );

    private boolean[] isCorrect = new boolean[6];
    ImageView back;
    Button next;
//    Firebase Ref
    private DatabaseReference databaseReference;
    private String currentUserId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.english_song1_reading);
        back= findViewById(R.id.home);
        next = findViewById(R.id.next);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openHomeActivity();
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Song2Activity();
            }
        });

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("engSong1").child(currentUserId);


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

        mic1.setOnClickListener(v->promptSpeechInput(0));
        mic2.setOnClickListener(v->promptSpeechInput(1));
        mic3.setOnClickListener(v->promptSpeechInput(2));
        mic4.setOnClickListener(v->promptSpeechInput(3));
        mic5.setOnClickListener(v->promptSpeechInput(4));
        mic6.setOnClickListener(v->promptSpeechInput(5));

        findViewById(R.id.button).setOnClickListener(v->showResults());

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
            textToSpeech.speak(expectedTexts[index], TextToSpeech.QUEUE_FLUSH, null, null);
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
    public void openHomeActivity(){
        Intent intent = new Intent(this,HomeActivity.class);
        startActivity(intent);
    }

    private void promptSpeechInput(int micIndex){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, Locale.US.toString());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Please Read the Line");

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE + micIndex);
        }catch (Exception e){
            Toast.makeText(this,"Speech Recognition is not Supported on this device",Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);

        if(resultCode == RESULT_OK && data != null){
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String recognizedText = result.get(0).trim().replaceAll("\\s+"," ").toLowerCase();

            int micIndex = requestCode - SPEECH_REQUEST_CODE;

            if(micIndex >=0 && micIndex < expectedTexts.length){
                boolean correct = checkTextAgainstVariations(recognizedText, micIndex);
                isCorrect[micIndex] = correct;

                Toast.makeText(this, correct ? "Correct!" : "Incorrect. Please Try Again!",Toast.LENGTH_SHORT).show();

                if(correct){
                    showCorrectIcon(micIndex);
                    storeCorrectnessState(micIndex, true);
                }
            }
        }
    }

    private boolean checkTextAgainstVariations(String recognizedText, int micIndex){
        List<String> variations = acceptableVariations.get(micIndex);
        for(String variation : variations){
            if(recognizedText.equals(variation.trim().replaceAll("\\s+"," ").toLowerCase())){
                return true;
            }
        }
        return false;
    }

    private void showCorrectIcon(int micIndex){
        switch (micIndex){
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

    private void storeCorrectnessState(int micIndex, boolean isCorrect){
        String micKey = "mic" + (micIndex + 1); // e.g., mic1, mic2, etc.
        databaseReference.child(micKey).setValue(isCorrect);
    }
    private void loadCorrectnessState(){
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(int i = 0; i < 6; i++){
                    String micKey = "mic" + (i + 1);
                    Boolean isCorrectValue = dataSnapshot.child(micKey).getValue(Boolean.class);

                    if(isCorrectValue != null && isCorrectValue){
                        isCorrect[i] = true;
                        switch (i){
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
                Toast.makeText(EnglishSong1_reading.this, "Failed to load data.", Toast.LENGTH_SHORT).show();

            }
        });
    }
    private void showResults(){
        int correctCount = 0;
        for(boolean correct : isCorrect){
            if(correct) correctCount++;
        }

        String resultMessage = "You got " + correctCount + " out of " + expectedTexts.length;
        storeResultInFirebase(correctCount);
        showAlert("Answer",resultMessage, R.drawable.happy);
        navigateToGameActivity();
    }

    private void storeResultInFirebase(int correctCount){
//        Database Ref
        DatabaseReference resultReference = databaseReference.child("result");
//        Store in Firebase
        resultReference.setValue(correctCount).addOnCompleteListener(task ->{
            if(task.isSuccessful()){
                Toast.makeText(EnglishSong1_reading.this, "Results Saved Successfully",Toast.LENGTH_SHORT).show();
                }else{
                Toast.makeText(EnglishSong1_reading.this, "Failed to save results",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAlert(String title, String message, int imageResourceId){
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

        builder.setPositiveButton("Correct", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public  void Song2Activity(){
        Intent intent = new Intent(this, EnglishSong2.class);
        startActivity(intent);
    }
    private void navigateToGameActivity(){
        Intent intent = new Intent(this,GameActivity.class);
        startActivity(intent);
        finish();
    }
}