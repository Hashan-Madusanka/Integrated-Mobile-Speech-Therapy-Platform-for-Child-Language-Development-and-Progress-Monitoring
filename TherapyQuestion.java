package com.example.speechtherapy;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class TherapyQuestion extends AppCompatActivity {
    ImageView home;
    Button button2;
    private RadioGroup group1, group2, group4,group5,group6;
    private Button submitButton;
    private ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.therapy_question);
        home = findViewById(R.id.home);
        button2= findViewById(R.id.button2);
        progressBar = findViewById(R.id.progressBar);
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openhomeActivity();
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openplanActivity();
            }
        });
        // Initialize RadioGroups
        group1 = findViewById(R.id.group1);
        group2 = findViewById(R.id.group2);
        group4 = findViewById(R.id.group4);
        group5 = findViewById(R.id.group5);
        group6 = findViewById(R.id.group6);
        submitButton = findViewById(R.id.buttonupload);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String age = getSelectedOption(group1);
                String gender = getSelectedOption(group2);
                String disorder = getSelectedOption(group4);
                String symptoms = getSelectedOption(group5);
                String cause = getSelectedOption(group6);

                if (age == null || gender == null ||disorder == null|| symptoms == null || cause == null) {
                    Toast.makeText(TherapyQuestion.this, "Please answer all questions", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create JSON payload
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("Age", Integer.parseInt(age));
                    payload.put("Gender", gender);
                    payload.put("Disorders", disorder);
                    payload.put("Symptoms", symptoms);
                    payload.put("Cause", cause);

                    sendTherapyPlanData(payload);

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(TherapyQuestion.this, "Error creating payload", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    public void openhomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }
    public void openplanActivity() {
        Intent intent = new Intent(this, TherapyPlanActivity.class);
        startActivity(intent);
    }
    private String getSelectedOption(RadioGroup group) {
        int selectedId = group.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selectedRadioButton = findViewById(selectedId);
            return selectedRadioButton.getText().toString();
        }
        return null;
    }

    private void sendTherapyPlanData(JSONObject payload) {
        runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));
        new Thread(() -> {
            try {
                URL url = new URL("https://speechtherapy-389242265122.us-central1.run.app");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                Log.d("API_CALL", "Sending payload: " + payload);

                // Send payload
                OutputStream os = connection.getOutputStream();
                os.write(payload.toString().getBytes());
                os.flush();
                os.close();

                // Get response
                int responseCode = connection.getResponseCode();
                InputStream inputStream = (responseCode == HttpURLConnection.HTTP_OK) ?
                        connection.getInputStream() : connection.getErrorStream();

                Scanner scanner = new Scanner(inputStream);
                StringBuilder response = new StringBuilder();
                while (scanner.hasNextLine()) {
                    response.append(scanner.nextLine());
                }
                scanner.close();

                Log.d("API_RESPONSE", "Response code: " + responseCode + ", Response: " + response);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    saveToFirebase(response.toString());
                } else {
                    runOnUiThread(() -> Toast.makeText(TherapyQuestion.this, "Server Error: " + response, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("NETWORK_ERROR", "Error: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(TherapyQuestion.this, "Error connecting to server", Toast.LENGTH_SHORT).show());
                progressBar.setVisibility(View.GONE);
            }
        }).start();
    }

    private void saveToFirebase(String therapyPlan) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("therapy_plan");

        String userId = auth.getCurrentUser().getUid();

        // Remove the existing therapy plan for the current user
        database.child(userId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Once the old data is deleted, save the new therapy plan
                database.child(userId).child("plan").setValue(therapyPlan).addOnCompleteListener(saveTask -> {
                    runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                    if (saveTask.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(TherapyQuestion.this, "Therapy Plan Saved Successfully", Toast.LENGTH_SHORT).show();
                            navigateToTherapyPlanPage();
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(TherapyQuestion.this, "Failed to save Therapy Plan", Toast.LENGTH_SHORT).show());
                    }
                });
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(TherapyQuestion.this, "Failed to remove existing therapy plan", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }


    private void navigateToTherapyPlanPage() {
        Intent intent = new Intent(TherapyQuestion.this, TherapyPlanActivity.class);
        startActivity(intent);
        finish();
    }
}