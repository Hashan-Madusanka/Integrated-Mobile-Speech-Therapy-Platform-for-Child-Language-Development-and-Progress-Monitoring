package com.example.speechtherapy;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class GameActivity extends AppCompatActivity {
    CardView card1, card2,card3,card4,card5,card6;
    ProgressBar bar1,bar1en,bar2,bar2en,bar3,bar4,bar4en,bar5,bar6;
    TextView text1,text1en,text2,text2en,text3,text4,text4en,text5,text6;
    ImageView back;

    private DatabaseReference databaseReference;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);

        card1 = findViewById(R.id.card1);
        card2 = findViewById(R.id.card2);
        card3 = findViewById(R.id.card3);
        card4 = findViewById(R.id.card4);
        card5 = findViewById(R.id.card5);
        card6 = findViewById(R.id.card6);
        text1= findViewById(R.id.scoreTextView1);
        text1en= findViewById(R.id.scoreTextView1En);
        text2 = findViewById(R.id.scoreTextView2);
        text2en = findViewById(R.id.scoreTextView2En);
        text3 = findViewById(R.id.scoreTextView3);
        text4 = findViewById(R.id.scoreTextView4);
        text4en = findViewById(R.id.scoreTextView4En);
        text5 = findViewById(R.id.scoreTextView5);
        text6 = findViewById(R.id.scoreTextView6);
        bar1 = findViewById(R.id.progressBar1);
        bar1en = findViewById(R.id.progressBar1En);
        bar2 = findViewById(R.id.progressBar2);
        bar2en = findViewById(R.id.progressBar2En);
        bar3 = findViewById(R.id.progressBar3);
        bar4= findViewById(R.id.progressBar4);
        bar4en= findViewById(R.id.progressBar4En);
        bar5= findViewById(R.id.progressBar5);
        bar6= findViewById(R.id.progressBar6);
        back= findViewById(R.id.home);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openhomeActivity();
            }
        });
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        card1.setOnClickListener(v -> showLanguageSelectionDialog());
        card2.setOnClickListener(v -> showSongDialogBox());
        card3.setOnClickListener(v -> openvolumeActivity());
        card4.setOnClickListener(v -> showStoryDialogBox());
        card5.setOnClickListener(v -> openspellActivity());
        card6.setOnClickListener(v -> opensoundActivity());

        // Retrieve and calculate progress
        calculateProgress();
        calculateSpellProgress();
        calculateSpellEnglishProgress();
        calculateVolumeProgress();
        calculateProgressEn();
        // Calculate story progress
        calculateStoryProgress();
        calculateStoryProgressEn();
        calculateSpellDelayProgress();
        calculateSoundProgress();

    }
    public void showStoryDialogBox(){

        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.language_select_popup,null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(true);
        dialog.show();

        Button englishButton = dialogView.findViewById(R.id.englishButton);
        Button sinhalaButton = dialogView.findViewById(R.id.sinhalaButton);

        englishButton.setOnClickListener(v->{
            openenglishStoryActivity();
            dialog.dismiss();
        });
        sinhalaButton.setOnClickListener(v->{
            openstoryActivity();
            dialog.dismiss();
        });

    }
    public void openspeech1Activity() {
        Intent intent = new Intent(this, Speech1Activity.class);
        startActivity(intent);
    }
    public void openenglishStoryActivity(){
        Intent intent = new Intent(this,EnglishStoryPart1.class);
        startActivity(intent);
    }
    public void showLanguageSelectionDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.language_select_popup,null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(true);
        dialog.show();

        Button englishButton = dialogView.findViewById(R.id.englishButton);
        Button sinhalaButton = dialogView.findViewById(R.id.sinhalaButton);

        englishButton.setOnClickListener(v->{
            openenglishspeech1Activity();
            dialog.dismiss();
        });
        sinhalaButton.setOnClickListener(v->{
            openspeech1Activity();
            dialog.dismiss();
        });

    }


    public void opensongActivity() {
        Intent intent = new Intent(this, Song1Activity.class);
        startActivity(intent);
    }
    public void openenglishspeech1Activity() {
        Intent intent = new Intent(this, EnglishSpeech1.class);
        startActivity(intent);
    }
    public void openvolumeActivity() {
        Intent intent = new Intent(this, VolumeActivity.class);
        startActivity(intent);
    }
    public void openstoryActivity() {
        Intent intent = new Intent(this, StoryPart1Activity.class);
        startActivity(intent);
    }
    public void openspellActivity() {
        Intent intent = new Intent(this, Spell1Activity.class);
        startActivity(intent);
    }
    public void opensoundActivity() {
        Intent intent = new Intent(this, Sound1Activity.class);
        startActivity(intent);
    }

    private void calculateProgress() {
        databaseReference.child("song1").child(currentUserId).child("result").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot song1Snapshot) {
                int song1Result = song1Snapshot.exists() ? song1Snapshot.getValue(Integer.class) : 0;

                databaseReference.child("song2").child(currentUserId).child("result").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot song2Snapshot) {
                        int song2Result = song2Snapshot.exists() ? song2Snapshot.getValue(Integer.class) : 0;

                        int combinedResult = song1Result + song2Result;
                        int progress = (combinedResult * 100) / 12;

                        bar2.setProgress(progress);
                        text2.setText(progress + "% Sinhala");
                        // Store progress in Firebase
                        databaseReference.child("user").child(currentUserId).child("sing_song").setValue(progress);

                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(GameActivity.this, "Failed to load song2 result.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(GameActivity.this, "Failed to load song1 result.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void openhomeActivity() {
        Intent intent = new Intent(this,HomeActivity.class);
        startActivity(intent);
    }
    public void showSongDialogBox(){
        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.language_select_popup,null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(true);
        dialog.show();

        Button englishButton = dialogView.findViewById(R.id.englishButton);
        Button sinhalaButton = dialogView.findViewById(R.id.sinhalaButton);

        englishButton.setOnClickListener(v->{
            openenglishsongActivity();
            dialog.dismiss();
        });
        sinhalaButton.setOnClickListener(v->{
            opensongActivity();
            dialog.dismiss();
        });
    }
    public void openenglishsongActivity(){
        Intent intent = new Intent(this,EnglishSong1.class);
        startActivity(intent);
    }

    private void calculateProgressEn() {
        databaseReference.child("engSong1").child(currentUserId).child("result").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot song1Snapshot) {
                int song1Result = song1Snapshot.exists() ? song1Snapshot.getValue(Integer.class) : 0;

                databaseReference.child("engSong2").child(currentUserId).child("result").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot song2Snapshot) {
                        int song2Result = song2Snapshot.exists() ? song2Snapshot.getValue(Integer.class) : 0;

                        int combinedResult = song1Result + song2Result;
                        int progress = (combinedResult * 100) / 12;

                        bar2en.setProgress(progress);
                        text2en.setText(progress + "% English");
                        // Store progress in Firebase
                        databaseReference.child("user").child(currentUserId).child("sing_songEn").setValue(progress);

                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(GameActivity.this, "Failed to load song2 result.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(GameActivity.this, "Failed to load song1 result.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void calculateSpellProgress() {
        databaseReference.child("spell").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int spellCount = 0;

                // Check spell_1 to spell_6 nodes
                for (int i = 1; i <= 6; i++) {
                    if (dataSnapshot.hasChild("spell_" + i)) {
                        spellCount++;
                    }
                }

                // Calculate progress as a percentage
                int progress = (spellCount * 100) / 6;

                // Update UI
                bar1.setProgress(progress);
                text1.setText(progress + "% Sinhala");
                // Store progress in Firebase
                databaseReference.child("user").child(currentUserId).child("spell_word").setValue(progress);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(GameActivity.this, "Failed to load spell data.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void calculateSpellEnglishProgress() {
        databaseReference.child("engSpell").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int spellCount = 0;

                // Check spell_1 to spell_6 nodes
                for (int i = 1; i <= 6; i++) {
                    if (dataSnapshot.hasChild("spell_" + i)) {
                        spellCount++;
                    }
                }

                // Calculate progress as a percentage
                int progress = (spellCount * 100) / 6;

                // Update UI
                bar1en.setProgress(progress);
                text1en.setText(progress + "% English");
                // Store progress in Firebase
                databaseReference.child("user").child(currentUserId).child("spell_wordEn").setValue(progress);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(GameActivity.this, "Failed to load spell data.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void calculateVolumeProgress() {
        databaseReference.child("volume").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int trueCount = 0;

                // List of expected volume result fields
                String[] volumeFields = {"hz300", "hz500", "hz600", "hz700", "hz800", "hz900"};

                // Check each field for a true value
                for (String field : volumeFields) {
                    Boolean value = dataSnapshot.child(field).getValue(Boolean.class);
                    if (value != null && value) {
                        trueCount++;
                    }
                }

                // Calculate progress as a percentage
                int progress = (trueCount * 100) / 6;

                // Update UI
                bar3.setProgress(progress);
                text3.setText(progress + "%");
                // Store progress in Firebase
                databaseReference.child("user").child(currentUserId).child("hear_volume").setValue(progress);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(GameActivity.this, "Failed to load volume data.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void calculateStoryProgress() {
        databaseReference.child("story").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int correctCount = 0;

                // Check each part for a true value
                for (int i = 1; i <= 5; i++) {
                    Boolean isPartCorrect = dataSnapshot.child("part" + i).getValue(Boolean.class);
                    if (isPartCorrect != null && isPartCorrect) {
                        correctCount++;
                    }
                }

                // Calculate the percentage based on the number of correct answers (divide by 5)
                int progress = (correctCount * 100) / 5;

                // Update UI with the calculated progress
                bar4.setProgress(progress);
                text4.setText(progress + "% Sinhala");
                // Store progress in Firebase
                databaseReference.child("user").child(currentUserId).child("listen_story").setValue(progress);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(GameActivity.this, "Failed to load story data.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void calculateStoryProgressEn() {
        databaseReference.child("engstory").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int correctCount = 0;

                // Check each part for a true value
                for (int i = 1; i <= 5; i++) {
                    Boolean isPartCorrect = dataSnapshot.child("part" + i).getValue(Boolean.class);
                    if (isPartCorrect != null && isPartCorrect) {
                        correctCount++;
                    }
                }

                // Calculate the percentage based on the number of correct answers (divide by 5)
                int progress = (correctCount * 100) / 5;

                // Update UI with the calculated progress
                bar4en.setProgress(progress);
                text4en.setText(progress + "% English");
                // Store progress in Firebase
                databaseReference.child("user").child(currentUserId).child("listen_storyEn").setValue(progress);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(GameActivity.this, "Failed to load story data.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void calculateSpellDelayProgress() {
        databaseReference.child("spell_dely").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int trueCount = 0;

                // Iterate through mic1 to mic15 and count true values
                for (int i = 1; i <= 15; i++) {
                    Boolean isTrue = dataSnapshot.child("mic" + i).getValue(Boolean.class);
                    if (isTrue != null && isTrue) {
                        trueCount++;
                    }
                }

                // Calculate the percentage (divide by 15)
                int progress = (trueCount * 100) / 15;

                // Update the UI elements
                bar5.setProgress(progress);
                text5.setText(progress + "%");
                // Store progress in Firebase
                databaseReference.child("user").child(currentUserId).child("try_sound").setValue(progress);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(GameActivity.this, "Failed to load spell delay data.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void calculateSoundProgress() {
        // Reference the "sound" node for the current user
        databaseReference.child("sound").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int trueCount = 0;

                // Iterate through "animal1" to "animal5" and count true values
                for (int i = 1; i <= 5; i++) {
                    Boolean value = dataSnapshot.child("animal" + i).getValue(Boolean.class);
                    if (value != null && value) {
                        trueCount++;
                    }
                }

                // Calculate the percentage (divide by 5)
                int progress = (trueCount * 100) / 5;

                // Update the UI elements
                bar6.setProgress(progress);
                text6.setText(progress + "%");
                // Store progress in Firebase
                databaseReference.child("user").child(currentUserId).child("catch_sound").setValue(progress);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(GameActivity.this, "Failed to load sound data.", Toast.LENGTH_SHORT).show();
            }
        });
    }


}
