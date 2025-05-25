package com.example.speechtherapy;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class GenerateReportActivity extends AppCompatActivity {
    ImageView home;
    Button button1,button2;
    private static final int PICK_AUDIO_REQUEST = 1;
    private Uri audioFileUri;
    private TextView textViewFileName, textViewResult;
    private Button submitButton;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference,databaseReference2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.generate_report);
        home = findViewById(R.id.home);
        button1= findViewById(R.id.button1);
        button2= findViewById(R.id.button2);
        progressBar = findViewById(R.id.progressBar);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("user");
        databaseReference2 = FirebaseDatabase.getInstance().getReference("Voice_analysis");
        updateChart();
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openhomeActivity();
            }
        });
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openreportActivity();
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPremiumStatus();
            }
        });

        // Initialize views
        CardView selectAudioCard = findViewById(R.id.selectaudio);
        textViewFileName = findViewById(R.id.text);
        textViewResult = findViewById(R.id.result);
        submitButton = findViewById(R.id.buttonupload);



        // Handle audio file selection
        selectAudioCard.setOnClickListener(v -> openAudioFile());

        // Handle submit button click
        submitButton.setOnClickListener(v -> {
            if (audioFileUri != null) {
                uploadAudioFile(audioFileUri);
            } else {
                Toast.makeText(this, "Please select an audio file first", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void checkPremiumStatus() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            databaseReference.child(userId).child("isPremium").get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            Boolean isPremium = snapshot.getValue(Boolean.class);
                            if (isPremium != null && isPremium) {
                                // User is a premium user, open chat activity
                                openchatActivity();
                            } else {
                                // User is not a premium user, show Snackbar
                                View rootView = findViewById(android.R.id.content);
                                Snackbar.make(rootView, "You are not a premium user, Activate premium status", Snackbar.LENGTH_LONG).show();
                            }
                        } else {
                            Log.e("Firebase", "isPremium field does not exist");
                            Toast.makeText(this, "Error: Unable to determine premium status", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Log.e("Firebase", "Error fetching isPremium status", e));
        } else {
            Log.e("Firebase", "User is not authenticated");
            Toast.makeText(this, "Please log in to access this feature", Toast.LENGTH_SHORT).show();
        }
    }

    public void openhomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }
    public void openreportActivity() {
        Intent intent = new Intent(this, ThreapyReportActivity.class);
        startActivity(intent);
    }
    public void openchatActivity() {
        Intent intent = new Intent(this, TherapistListActivity.class);
        startActivity(intent);
    }
    private void openAudioFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(intent, PICK_AUDIO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_AUDIO_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            audioFileUri = data.getData();
            String fileName = getFileName(audioFileUri);
            textViewFileName.setText(fileName);
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    private void uploadAudioFile(Uri fileUri) {
        String urlString = "https://voiceanalysis-389242265122.us-central1.run.app/";

        // Show the progress bar
        runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));

        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(fileUri);
                byte[] fileBytes = new byte[inputStream.available()];
                inputStream.read(fileBytes);
                inputStream.close();

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=*****");
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Charset", "UTF-8");

                DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
                String boundary = "*****";
                String lineEnd = "\r\n";
                String twoHyphens = "--";

                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + getFileName(fileUri) + "\"" + lineEnd);
                outputStream.writeBytes("Content-Type: audio/*" + lineEnd);
                outputStream.writeBytes(lineEnd);

                outputStream.write(fileBytes);
                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                outputStream.flush();
                outputStream.close();

                int responseCode = connection.getResponseCode();
                StringBuilder response = new StringBuilder();
                InputStream responseStream;

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    responseStream = connection.getInputStream();
                } else {
                    responseStream = connection.getErrorStream();
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                connection.disconnect();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE); // Hide progress bar
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response.toString());
                            String prediction = jsonResponse.optString("prediction", "");

                            if ("Correct".equals(prediction)) {
                                textViewResult.setText("Speaking Good");
                                saveToFirebase("Speaking Good");
                            } else {
                                textViewResult.setText("Speaking Bad");
                                saveToFirebase("Speaking Bad");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            textViewResult.setText("Error parsing response");
                        }
                    } else {
                        Toast.makeText(GenerateReportActivity.this, "Error in response", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE); // Hide progress bar on failure
                    Toast.makeText(GenerateReportActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

private void saveToFirebase(String predictionResult) {
    FirebaseUser currentUser = firebaseAuth.getCurrentUser();
    if (currentUser != null) {
        String userId = currentUser.getUid();
        DatabaseReference voiceAnalysisRef = databaseReference2.child(userId);

        // Generate a new unique key
        String key = voiceAnalysisRef.push().getKey();

        // Determine the value (1 for correct, 0 for incorrect)
        int resultValue = predictionResult.equals("Speaking Good") ? 1 : 0;

        if (key != null) {
            voiceAnalysisRef.child(key).setValue(resultValue)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(GenerateReportActivity.this, "Result saved successfully", Toast.LENGTH_SHORT).show();
                        updateChart(); // Update chart after saving result
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(GenerateReportActivity.this, "Failed to save result: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
    private void updateChart() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference voiceAnalysisRef = databaseReference2.child(userId);

            voiceAnalysisRef.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    int correctCount = 0;
                    int incorrectCount = 0;

                    for (DataSnapshot data : snapshot.getChildren()) {
                        Integer value = data.getValue(Integer.class);
                        if (value != null) {
                            if (value == 1) correctCount++;
                            else incorrectCount++;
                        }
                    }

                    int total = correctCount + incorrectCount;
                    float correctPercentage = total > 0 ? (correctCount * 100f) / total : 0;

                    updatePieChart(correctPercentage);
                    saveToFirebase2(correctPercentage);
                }
            }).addOnFailureListener(e -> {
                Log.e("Firebase", "Error fetching voice_analysis data", e);
            });
        }
    }
    private void saveToFirebase2(float correctPercentage) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            databaseReference.child(userId).child("voice_analysis").setValue(correctPercentage)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(GenerateReportActivity.this, "Result saved successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(GenerateReportActivity.this, "Failed to save result: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }
    private void updatePieChart(float correctPercentage) {
        PieChart pieChart = findViewById(R.id.circleChart);

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(correctPercentage, "Good"));
        entries.add(new PieEntry(100 - correctPercentage, "Bad"));

        PieDataSet dataSet = new PieDataSet(entries, "progress");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.invalidate(); // Refresh chart
    }

}