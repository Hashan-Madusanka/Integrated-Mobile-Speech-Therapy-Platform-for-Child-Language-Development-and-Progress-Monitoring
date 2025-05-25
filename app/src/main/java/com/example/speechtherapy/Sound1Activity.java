package com.example.speechtherapy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Sound1Activity extends AppCompatActivity {
    CardView card1, card2, card3;
    ImageView correct1, correct2, correct3, back;
    DatabaseReference databaseReference;
    String userId;
    Button next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sound1);
        back= findViewById(R.id.home);
        next= findViewById(R.id.next);
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
        // Initialize Firebase
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("sound").child(userId);

        // Initialize UI components
        card1 = findViewById(R.id.cardView1);
        card2 = findViewById(R.id.cardView2);
        card3 = findViewById(R.id.cardView3);
        correct1 = findViewById(R.id.correct1);
        correct2 = findViewById(R.id.correct2);
        correct3 = findViewById(R.id.correct3);

        // Video playback setup
        VideoView videoView = findViewById(R.id.videoView);
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.dog2);
        videoView.setVideoURI(videoUri);
        MediaController mediaController = new MediaController(this);
        videoView.setMediaController(mediaController);
        mediaController.setAnchorView(videoView);
        videoView.start();

        // Set initial visibility of correct images
        correct1.setVisibility(View.INVISIBLE);
        correct2.setVisibility(View.INVISIBLE);
        correct3.setVisibility(View.INVISIBLE);

        // Check if the user has already answered correctly
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.child("animal1").getValue(Boolean.class) != null) {
                    boolean isCorrect = snapshot.child("animal1").getValue(Boolean.class);
                    if (isCorrect) {
                        correct1.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Sound1Activity.this, "Error loading data", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle card clicks
        card1.setOnClickListener(v -> handleCardClick(true, correct1));
        card2.setOnClickListener(v -> handleCardClick(false, null));
        card3.setOnClickListener(v -> handleCardClick(false, null));
    }

    private void handleCardClick(boolean isCorrect, ImageView correctImageView) {
        if (isCorrect) {
            // Display "You are correct" alert and show the correct image
            Toast.makeText(this, "You are correct!", Toast.LENGTH_SHORT).show();
            if (correctImageView != null) {
                correctImageView.setVisibility(View.VISIBLE);
            }
            showRetryDialog2();

            // Save to Firebase as correct answer
            databaseReference.child("animal1").setValue(true);
        } else {
            // Update Firebase to indicate incorrect answer
            databaseReference.child("animal1").setValue(false);

            // Hide the correct image if shown
            correct1.setVisibility(View.INVISIBLE);

            // Show retry dialog
            showRetryDialog();
            Toast.makeText(this, "Your answer is wrong. Try again!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRetryDialog() {
        // Create an ImageView programmatically
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(R.drawable.oop); // Replace with your image resource
        imageView.setAdjustViewBounds(true); // Allow scaling
        imageView.setMaxHeight(400); // Set maximum height (adjust as needed)
        imageView.setMaxWidth(300); // Set maximum width (adjust as needed)

        // Build the dialog
        new AlertDialog.Builder(this)
                .setTitle("Try Again")
                .setView(imageView) // Add the ImageView to the dialog
                .setMessage("It's okay, let's try again.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showRetryDialog2() {
        // Create an ImageView programmatically
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(R.drawable.happy); // Replace with your image resource
        imageView.setAdjustViewBounds(true); // Allow scaling
        imageView.setMaxHeight(400); // Set maximum height (adjust as needed)
        imageView.setMaxWidth(300); // Set maximum width (adjust as needed)

        // Build the dialog
        new AlertDialog.Builder(this)
                .setTitle("Good job")
                .setView(imageView) // Add the ImageView to the dialog
                .setMessage("your answer is correct")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }
    public void openhomeActivity() {
        Intent intent = new Intent(this,HomeActivity.class);
        startActivity(intent);
    }
    public void opennextActivity() {
        Intent intent = new Intent(this,Sound2Activity.class);
        startActivity(intent);
    }
}
