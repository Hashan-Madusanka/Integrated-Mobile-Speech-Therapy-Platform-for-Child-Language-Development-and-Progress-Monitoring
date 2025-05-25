package com.example.speechtherapy;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

public class Song2Activity extends AppCompatActivity {
    Button button,next;
    ImageView home;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.song_2);
        VideoView videoView = findViewById(R.id.videoView);
        button = findViewById(R.id.button);
        next = findViewById(R.id.next);
        home = findViewById(R.id.home);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                opensongonereadingActivity();
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Song1Activity();
            }
        });
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homeActivity();
            }
        });
        //video
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.song2);
        // Set the video URI
        videoView.setVideoURI(videoUri);
        // Set MediaController for video controls (optional)
        MediaController mediaController = new MediaController(this);
        videoView.setMediaController(mediaController);
        mediaController.setAnchorView(videoView);
        // Start the video automatically
        videoView.start();
    }
    public void opensongonereadingActivity() {
        Intent intent = new Intent(this, Song2_readingActivity.class);
        startActivity(intent);
    }
    public void homeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }
    public void Song1Activity() {
        Intent intent = new Intent(this, Song1Activity.class);
        startActivity(intent);
    }
}