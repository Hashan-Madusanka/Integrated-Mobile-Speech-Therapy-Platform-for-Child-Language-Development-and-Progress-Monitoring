package com.example.speechtherapy;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class VolumeActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private DatabaseReference databaseReference;
    private String currentUserId;  // Replace with actual user ID
    ImageView back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.volume);
        back= findViewById(R.id.home);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openhomeActivity();
            }
        });
        // Initialize Firebase
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this);
        }
        // Initialize Firebase reference
        databaseReference = FirebaseDatabase.getInstance().getReference("volume");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Fetch saved checkbox results
        fetchSavedCheckboxResults();
        // Play buttons
        ImageView play1 = findViewById(R.id.play1);
        ImageView play2 = findViewById(R.id.play2);
        ImageView play3 = findViewById(R.id.play3);
        ImageView play4 = findViewById(R.id.play4);
        ImageView play5 = findViewById(R.id.play5);
        ImageView play6 = findViewById(R.id.play6);

        // Stop buttons
        ImageView stop1 = findViewById(R.id.stop1);
        ImageView stop2 = findViewById(R.id.stop2);
        ImageView stop3 = findViewById(R.id.stop3);
        ImageView stop4 = findViewById(R.id.stop4);
        ImageView stop5 = findViewById(R.id.stop5);
        ImageView stop6 = findViewById(R.id.stop6);

        // Play button listeners
        play1.setOnClickListener(v -> playAudio(R.raw.audio1, findViewById(R.id.progressBar1)));
        play2.setOnClickListener(v -> playAudio(R.raw.audio2, findViewById(R.id.progressBar2)));
        play3.setOnClickListener(v -> playAudio(R.raw.audio3, findViewById(R.id.progressBar3)));
        play4.setOnClickListener(v -> playAudio(R.raw.audio4, findViewById(R.id.progressBar4)));
        play5.setOnClickListener(v -> playAudio(R.raw.audio5, findViewById(R.id.progressBar5)));
        play6.setOnClickListener(v -> playAudio(R.raw.audio6, findViewById(R.id.progressBar6)));


        // Stop button listeners
        stop1.setOnClickListener(v -> stopAudio(findViewById(R.id.progressBar1)));
        stop2.setOnClickListener(v -> stopAudio(findViewById(R.id.progressBar2)));
        stop3.setOnClickListener(v -> stopAudio(findViewById(R.id.progressBar3)));
        stop4.setOnClickListener(v -> stopAudio(findViewById(R.id.progressBar4)));
        stop5.setOnClickListener(v -> stopAudio(findViewById(R.id.progressBar5)));
        stop6.setOnClickListener(v -> stopAudio(findViewById(R.id.progressBar6)));


        // Save button
        Button saveButton = findViewById(R.id.button);
        saveButton.setOnClickListener(v -> saveCheckboxResults());
    }

    private void playAudio(int audioResId, final ProgressBar progressBar) {
        // Hide all progress bars initially
        hideAllProgressBars();

        // Show the selected progress bar
        progressBar.setVisibility(View.VISIBLE);

        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        mediaPlayer = MediaPlayer.create(this, audioResId);
        mediaPlayer.start();

        // Set up a handler to update the progress bar while the audio plays
        final Handler handler = new Handler();
        final int duration = mediaPlayer.getDuration();

        // Update progress bar every 100 ms
        Runnable updateProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {  // Null check for mediaPlayer
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    int progress = (int) (((float) currentPosition / duration) * 100);
                    progressBar.setProgress(progress);

                    // Keep updating until the audio finishes
                    if (mediaPlayer.isPlaying()) {
                        handler.postDelayed(this, 100);
                    }
                }
            }
        };


        handler.post(updateProgressRunnable);

        mediaPlayer.setOnCompletionListener(mp -> {
            // Reset progress bar when audio is completed
            progressBar.setProgress(0);
            progressBar.setVisibility(View.GONE);  // Hide progress bar
        });
    }

    private void stopAudio(ProgressBar progressBar) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            progressBar.setVisibility(View.GONE); // Hide the progress bar
        }
    }
    public void openhomeActivity() {
        Intent intent = new Intent(this,HomeActivity.class);
        startActivity(intent);
    }

    private void saveCheckboxResults() {
        // Collect checkbox data
        CheckBox checkBox1 = findViewById(R.id.checkBox1);
        CheckBox checkBox2 = findViewById(R.id.checkBox2);
        CheckBox checkBox3 = findViewById(R.id.checkBox3);
        CheckBox checkBox4 = findViewById(R.id.checkBox4);
        CheckBox checkBox5 = findViewById(R.id.checkBox5);
        CheckBox checkBox6 = findViewById(R.id.checkBox6);

        // Create a volume object to save checkbox results
        VolumeResult volumeResult = new VolumeResult(
                checkBox1.isChecked(),
                checkBox2.isChecked(),
                checkBox3.isChecked(),
                checkBox4.isChecked(),
                checkBox5.isChecked(),
                checkBox6.isChecked()
        );
// Save to Firebase under the current user's ID
        databaseReference.child(currentUserId).setValue(volumeResult)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Display Snackbar on success
                        Snackbar.make(findViewById(android.R.id.content), "Data saved successfully", Snackbar.LENGTH_SHORT).show();
                        // Navigate to GameActivity after showing results
                        navigateToGameActivity();
                    } else {
                        // Display Snackbar on failure
                        Snackbar.make(findViewById(android.R.id.content), "Failed to save data", Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    // Class to represent the volume result
    public static class VolumeResult {
        public boolean hz300, hz500, hz600, hz700, hz800, hz900;

        // No-argument constructor required by Firebase
        public VolumeResult() {
        }

        // Constructor with arguments
        public VolumeResult(boolean hz300, boolean hz500, boolean hz600, boolean hz700, boolean hz800, boolean hz900) {
            this.hz300 = hz300;
            this.hz500 = hz500;
            this.hz600 = hz600;
            this.hz700 = hz700;
            this.hz800 = hz800;
            this.hz900 = hz900;
        }
    }

    // Helper method to hide all progress bars
    private void hideAllProgressBars() {
        ProgressBar progressBar1 = findViewById(R.id.progressBar1);
        ProgressBar progressBar2 = findViewById(R.id.progressBar2);
        ProgressBar progressBar3 = findViewById(R.id.progressBar3);
        ProgressBar progressBar4 = findViewById(R.id.progressBar4);
        ProgressBar progressBar5 = findViewById(R.id.progressBar5);
        ProgressBar progressBar6 = findViewById(R.id.progressBar6);

        progressBar1.setVisibility(View.GONE);
        progressBar2.setVisibility(View.GONE);
        progressBar3.setVisibility(View.GONE);
        progressBar4.setVisibility(View.GONE);
        progressBar5.setVisibility(View.GONE);
        progressBar6.setVisibility(View.GONE);
    }

    private void fetchSavedCheckboxResults() {
        databaseReference.child(currentUserId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                VolumeResult volumeResult = task.getResult().getValue(VolumeResult.class);
                if (volumeResult != null) {
                    // Set the checkbox states based on the saved data
                    CheckBox checkBox1 = findViewById(R.id.checkBox1);
                    CheckBox checkBox2 = findViewById(R.id.checkBox2);
                    CheckBox checkBox3 = findViewById(R.id.checkBox3);
                    CheckBox checkBox4 = findViewById(R.id.checkBox4);
                    CheckBox checkBox5 = findViewById(R.id.checkBox5);
                    CheckBox checkBox6 = findViewById(R.id.checkBox6);

                    checkBox1.setChecked(volumeResult.hz300);
                    checkBox2.setChecked(volumeResult.hz500);
                    checkBox3.setChecked(volumeResult.hz600);
                    checkBox4.setChecked(volumeResult.hz700);
                    checkBox5.setChecked(volumeResult.hz800);
                    checkBox6.setChecked(volumeResult.hz900);
                }
            } else {
                // Handle failed Firebase read operation
                Snackbar.make(findViewById(android.R.id.content), "Failed to load saved data", Snackbar.LENGTH_SHORT).show();
            }
        });
    }
    private void navigateToGameActivity() {
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
        finish(); // Optionally call finish() to close the current activity
    }

}
