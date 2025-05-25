package com.example.speechtherapy;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class EnglishSong2 extends AppCompatActivity {
    Button button, next;
    ImageView home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.english_song2);
        VideoView videoView = findViewById(R.id.videoView);
        button = findViewById(R.id.button);
        next = findViewById(R.id.next);
        home = findViewById(R.id.home);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {opensongreadActivity();}
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {Song1Activity();}
        });
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homeActivity();
            }
        });

        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.teapot);
        videoView.setVideoURI(videoUri);
        MediaController mediaController = new MediaController(this);
        videoView.setMediaController(mediaController);
        mediaController.setAnchorView(videoView);
        videoView.start();
    }
    public void opensongreadActivity(){
        Intent intent = new Intent(this, EnglishSong2_reading.class);
        startActivity(intent);
    }
    public void homeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
    }
    public void Song1Activity() {
        Intent intent = new Intent(this, EnglishSong1.class);
        startActivity(intent);
    }
}