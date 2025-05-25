package com.example.speechtherapy;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Deseases1Activity extends AppCompatActivity {
    Button btn1, btn2, btn3;
    ImageView back;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.deseases1);

        btn1 = findViewById(R.id.bt1);
        btn2 = findViewById(R.id.bt2);
        btn3 = findViewById(R.id.bt3);
        back= findViewById(R.id.home);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openhomeActivity();
            }
        });

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                opencleftActivity();
            }
        });

        // Handle button 2 click for Language Delay
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLanguageDelayDialog();
            }
        });

        // Handle button 3 click for Hearing Loss
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHearingLossDialog();
            }
        });
    }

    // Open Cleft Image Upload Activity
    public void opencleftActivity() {
        Intent intent = new Intent(this, CleftImageUploadActivity.class);
        startActivity(intent);
    }

    // Show dialog for Language Delay
    private void showLanguageDelayDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Deseases1Activity.this);
        builder.setTitle("Does the kid have a language delay problem?");

        // Set up the radio buttons for "Yes" and "No"
        final RadioGroup radioGroup = new RadioGroup(Deseases1Activity.this);
        RadioButton yesButton = new RadioButton(Deseases1Activity.this);
        yesButton.setText("Yes");
        RadioButton noButton = new RadioButton(Deseases1Activity.this);
        noButton.setText("No");

        radioGroup.addView(yesButton);
        radioGroup.addView(noButton);

        builder.setView(radioGroup);

        builder.setPositiveButton("Save", (dialog, which) -> {
            boolean isLanguageDelay = radioGroup.getCheckedRadioButtonId() == yesButton.getId();
            saveLanguageDelayToFirebase(isLanguageDelay);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    // Show dialog for Hearing Loss
    private void showHearingLossDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Deseases1Activity.this);
        builder.setTitle("Does the kid have a hearing loss issue?");

        // Set up the radio buttons for "Yes" and "No"
        final RadioGroup radioGroup = new RadioGroup(Deseases1Activity.this);
        RadioButton yesButton = new RadioButton(Deseases1Activity.this);
        yesButton.setText("Yes");
        RadioButton noButton = new RadioButton(Deseases1Activity.this);
        noButton.setText("No");

        radioGroup.addView(yesButton);
        radioGroup.addView(noButton);

        builder.setView(radioGroup);

        builder.setPositiveButton("Save", (dialog, which) -> {
            boolean isHearingLoss = radioGroup.getCheckedRadioButtonId() == yesButton.getId();
            saveHearingLossToFirebase(isHearingLoss);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    // Save language delay status to Firebase
    private void saveLanguageDelayToFirebase(boolean isLanguageDelay) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("user").child(userId);

        databaseReference.child("languagedelay").setValue(isLanguageDelay)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(Deseases1Activity.this, "Language Delay status saved successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(Deseases1Activity.this, "Failed to save Language Delay status", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Save hearing loss status to Firebase
    private void saveHearingLossToFirebase(boolean isHearingLoss) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("user").child(userId);

        databaseReference.child("hearingloss").setValue(isHearingLoss)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(Deseases1Activity.this, "Hearing Loss status saved successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(Deseases1Activity.this, "Failed to save Hearing Loss status", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    public void openhomeActivity() {
        Intent intent = new Intent(this,HomeActivity.class);
        startActivity(intent);
    }
}
