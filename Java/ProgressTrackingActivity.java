package com.example.speechtherapy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.speechtherapy.utils.ProgressDataManager;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

public class ProgressTrackingActivity extends AppCompatActivity {
    
    private static final String TAG = "ProgressTrackingActivity";
    
    private ImageView homeButton;
    private LineChart accuracyLineChart;
    private LineChart gameScoresBarChart;
    private LineChart communicationBarChart;
    private Spinner timeframeSpinner;
    private Button exportButton;
    
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    
    // Data collections for charts
    private TreeMap<String, Integer> speechAccuracyData = new TreeMap<>();
    private TreeMap<String, Integer> speechAccuracySinhalaData = new TreeMap<>();
    private TreeMap<String, Integer> speechAccuracyEnglishData = new TreeMap<>();
    private TreeMap<String, Map<String, Integer>> gameScoresData = new TreeMap<>();
    private TreeMap<String, Integer> communicationData = new TreeMap<>();
    
    // Time periods for filtering
    private static final int PERIOD_DAILY = 7;    // Show 7 days for daily view
    private static final int PERIOD_WEEKLY = 4;   // Show 4 weeks for weekly view
    private static final int PERIOD_MONTHLY = 6;  // Show 6 months for monthly view
    
    private static final int STORAGE_PERMISSION_CODE = 100;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress_tracking);
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("user");
        
        // Initialize views
        homeButton = findViewById(R.id.home_button);
        accuracyLineChart = findViewById(R.id.speech_accuracy_chart);
        gameScoresBarChart = findViewById(R.id.game_scores_chart);
        communicationBarChart = findViewById(R.id.communication_chart);
        timeframeSpinner = findViewById(R.id.timeframe_spinner);
        exportButton = findViewById(R.id.export_button);
        
        // Home button click listener
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        });
        
        // Export button click listener
        exportButton.setOnClickListener(v -> {
            exportProgressReport();
        });
        
        // Setup timeframe spinner
        setupTimeframeSpinner();
        
        // Initialize today's data if it doesn't exist
        ProgressDataManager.initializeDailyData();
        
        // Load user data
        loadUserData();
    }
    
    private void setupTimeframeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.timeframe_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeframeSpinner.setAdapter(adapter);
        
        timeframeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateCharts();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    
    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            
            DatabaseReference historyRef = databaseReference.child(userId).child("progress_history");
            historyRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    processProgressHistoryData(dataSnapshot);
                    // Update charts after data is loaded
                    updateCharts();
                }
                
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(ProgressTrackingActivity.this, 
                            "Failed to load data: " + databaseError.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Database error: " + databaseError.getMessage());
                }
            });
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            
            // Create sample data for demonstration when user is not logged in
            createSampleData();
            updateCharts();
        }
    }
    
    private void processProgressHistoryData(DataSnapshot historySnapshot) {
        // Clear existing data
        speechAccuracyData.clear();
        speechAccuracySinhalaData.clear();
        speechAccuracyEnglishData.clear();
        gameScoresData.clear();
        communicationData.clear();
        
        // Process each date entry
        for (DataSnapshot dateSnapshot : historySnapshot.getChildren()) {
            String date = dateSnapshot.getKey();
            if (date == null) continue;
            
            // Extract speech accuracy for both languages
            if (dateSnapshot.hasChild("speech_accuracy_sinhala")) {
                Integer accuracySinhala = dateSnapshot.child("speech_accuracy_sinhala").getValue(Integer.class);
                if (accuracySinhala != null) {
                    speechAccuracySinhalaData.put(date, accuracySinhala);
                }
            }
            
            if (dateSnapshot.hasChild("speech_accuracy_english")) {
                Integer accuracyEnglish = dateSnapshot.child("speech_accuracy_english").getValue(Integer.class);
                if (accuracyEnglish != null) {
                    speechAccuracyEnglishData.put(date, accuracyEnglish);
                }
            }
            
            // Extract general speech accuracy (for backward compatibility)
            if (dateSnapshot.hasChild("speech_accuracy")) {
                Integer accuracy = dateSnapshot.child("speech_accuracy").getValue(Integer.class);
                if (accuracy != null) {
                    speechAccuracyData.put(date, accuracy);
                    
                    // If language-specific data is missing, use general data for Sinhala
                    if (!speechAccuracySinhalaData.containsKey(date)) {
                        speechAccuracySinhalaData.put(date, accuracy);
                    }
                }
            }
            
            // Extract game scores - try both methods
            Map<String, Integer> dailyScores = new HashMap<>();
            
            // Method 1: Check for game_scores node
            if (dateSnapshot.hasChild("game_scores")) {
                DataSnapshot gameScoresSnapshot = dateSnapshot.child("game_scores");
                
                for (DataSnapshot gameTypeSnapshot : gameScoresSnapshot.getChildren()) {
                    String gameType = gameTypeSnapshot.getKey();
                    Integer score = gameTypeSnapshot.getValue(Integer.class);
                    if (gameType != null && score != null) {
                        dailyScores.put(gameType, score);
                    }
                }
                
                Log.d(TAG, "Found game scores under game_scores for date " + date + ": " + dailyScores);
            }
            
            // Method 2: Check for direct game score entries (sing_song, spell_word, listen_story)
            if (dateSnapshot.hasChild("sing_song")) {
                Integer score = dateSnapshot.child("sing_song").getValue(Integer.class);
                if (score != null) {
                    dailyScores.put("sing_song", score);
                }
            }
            
            if (dateSnapshot.hasChild("spell_word")) {
                Integer score = dateSnapshot.child("spell_word").getValue(Integer.class);
                if (score != null) {
                    dailyScores.put("spell_word", score);
                }
            }
            
            if (dateSnapshot.hasChild("listen_story")) {
                Integer score = dateSnapshot.child("listen_story").getValue(Integer.class);
                if (score != null) {
                    dailyScores.put("listen_story", score);
                }
            }
            
            // Also check for language-specific game scores
            if (dateSnapshot.hasChild("sing_song_sinhala")) {
                Integer score = dateSnapshot.child("sing_song_sinhala").getValue(Integer.class);
                if (score != null) {
                    dailyScores.put("sing_song_sinhala", score);
                }
            }
            
            if (dateSnapshot.hasChild("spell_word_sinhala")) {
                Integer score = dateSnapshot.child("spell_word_sinhala").getValue(Integer.class);
                if (score != null) {
                    dailyScores.put("spell_word_sinhala", score);
                }
            }
            
            if (dateSnapshot.hasChild("listen_story_sinhala")) {
                Integer score = dateSnapshot.child("listen_story_sinhala").getValue(Integer.class);
                if (score != null) {
                    dailyScores.put("listen_story_sinhala", score);
                }
            }
            
            if (dateSnapshot.hasChild("sing_song_english")) {
                Integer score = dateSnapshot.child("sing_song_english").getValue(Integer.class);
                if (score != null) {
                    dailyScores.put("sing_song_english", score);
                }
            }
            
            if (dateSnapshot.hasChild("spell_word_english")) {
                Integer score = dateSnapshot.child("spell_word_english").getValue(Integer.class);
                if (score != null) {
                    dailyScores.put("spell_word_english", score);
                }
            }
            
            if (dateSnapshot.hasChild("listen_story_english")) {
                Integer score = dateSnapshot.child("listen_story_english").getValue(Integer.class);
                if (score != null) {
                    dailyScores.put("listen_story_english", score);
                }
            }
            
            if (!dailyScores.isEmpty()) {
                gameScoresData.put(date, dailyScores);
                Log.d(TAG, "Added game scores for date " + date + ": " + dailyScores);
            }
            
            // Extract communication count
            if (dateSnapshot.hasChild("communication_count")) {
                Integer count = dateSnapshot.child("communication_count").getValue(Integer.class);
                if (count != null) {
                    communicationData.put(date, count);
                }
            }
        }
        
        // If no data found, create sample data for demonstration
        if (speechAccuracySinhalaData.isEmpty() && speechAccuracyEnglishData.isEmpty() && 
            gameScoresData.isEmpty() && communicationData.isEmpty()) {
            createSampleData();
            Log.d(TAG, "No data found, created sample data");
        } else {
            Log.d(TAG, "Found data - Sinhala accuracy points: " + speechAccuracySinhalaData.size() + 
                    ", English accuracy points: " + speechAccuracyEnglishData.size() +
                    ", Game score entries: " + gameScoresData.size() + 
                    ", Communication entries: " + communicationData.size());
        }
    }
    
    private void createSampleData() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar calendar = Calendar.getInstance();
        
        // Generate sample data for the past 7 days
        for (int i = 6; i >= 0; i--) {
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH, -i);
            String date = sdf.format(calendar.getTime());
            
            // Sample speech accuracy for Sinhala (gradually improving)
            int baseSinhalaAccuracy = 65;
            speechAccuracySinhalaData.put(date, baseSinhalaAccuracy + (i * 2) + (int)(Math.random() * 5));
            
            // Sample speech accuracy for English (lower starting point, improving faster)
            int baseEnglishAccuracy = 55;
            speechAccuracyEnglishData.put(date, baseEnglishAccuracy + (i * 3) + (int)(Math.random() * 5));
            
            // Also put in general speech accuracy for backward compatibility
            speechAccuracyData.put(date, baseSinhalaAccuracy + (i * 2) + (int)(Math.random() * 5));
            
            // Sample game scores for this date
            Map<String, Integer> dailyScores = new HashMap<>();
            
            // Sinhala game scores
            dailyScores.put("sing_song", 60 + (i * 3) + (int)(Math.random() * 5));
            dailyScores.put("spell_word", 50 + (i * 4) + (int)(Math.random() * 5));
            dailyScores.put("listen_story", 70 + (i * 2) + (int)(Math.random() * 5));
            
            // English game scores
            dailyScores.put("sing_song_english", 50 + (i * 4) + (int)(Math.random() * 5));
            dailyScores.put("spell_word_english", 45 + (i * 5) + (int)(Math.random() * 5));
            dailyScores.put("listen_story_english", 65 + (i * 3) + (int)(Math.random() * 5));
            
            gameScoresData.put(date, dailyScores);
            
            // Sample communication count (varies randomly between 0-4)
            communicationData.put(date, (int)(Math.random() * 5));
        }
    }
    
    private void updateCharts() {
        String timeframe = timeframeSpinner.getSelectedItem().toString();
        
        // Filter data based on selected timeframe
        filterDataByTimeframe(timeframe);
        
        // Update charts based on selected timeframe
        updateSpeechAccuracyChart();
        updateGameScoresChart();
        updateCommunicationChart();
    }
    
    private void filterDataByTimeframe(String timeframe) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cutoffDate = Calendar.getInstance();
        
        int period = PERIOD_DAILY;
        
        // Determine cutoff date based on timeframe
        switch (timeframe) {
            case "Daily":
                cutoffDate.add(Calendar.DAY_OF_MONTH, -PERIOD_DAILY);
                period = PERIOD_DAILY;
                break;
            case "Weekly":
                cutoffDate.add(Calendar.WEEK_OF_YEAR, -PERIOD_WEEKLY);
                period = PERIOD_WEEKLY;
                break;
            case "Monthly":
                cutoffDate.add(Calendar.MONTH, -PERIOD_MONTHLY);
                period = PERIOD_MONTHLY;
                break;
        }
        
        String cutoffDateString = sdf.format(cutoffDate.getTime());
        
        // Filter data to keep only entries after the cutoff date
        filterMapByDate(speechAccuracyData, cutoffDateString);
        filterMapByDate(speechAccuracySinhalaData, cutoffDateString);
        filterMapByDate(speechAccuracyEnglishData, cutoffDateString);
        filterMapByDate(gameScoresData, cutoffDateString);
        filterMapByDate(communicationData, cutoffDateString);
    }
    
    private <T> void filterMapByDate(TreeMap<String, T> map, String cutoffDate) {
        List<String> keysToRemove = new ArrayList<>();
        
        for (String date : map.keySet()) {
            if (date.compareTo(cutoffDate) < 0) {
                keysToRemove.add(date);
            }
        }
        
        for (String key : keysToRemove) {
            map.remove(key);
        }
    }
    
    private void updateSpeechAccuracyChart() {
        // Check if we have any data to display
        if (speechAccuracySinhalaData.isEmpty() && speechAccuracyEnglishData.isEmpty()) {
            accuracyLineChart.setNoDataText("No speech accuracy data available");
            accuracyLineChart.invalidate();
            return;
        }
        
        List<Entry> sinhalaEntries = new ArrayList<>();
        List<Entry> englishEntries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();
        
        // Combine all dates from both languages
        Set<String> allDates = new TreeSet<>();
        allDates.addAll(speechAccuracySinhalaData.keySet());
        allDates.addAll(speechAccuracyEnglishData.keySet());
        
        // Create entries for both languages
        int index = 0;
        for (String date : allDates) {
            if (speechAccuracySinhalaData.containsKey(date)) {
                sinhalaEntries.add(new Entry(index, speechAccuracySinhalaData.get(date)));
            }
            
            if (speechAccuracyEnglishData.containsKey(date)) {
                englishEntries.add(new Entry(index, speechAccuracyEnglishData.get(date)));
            }
            
            // Format date for display
            xLabels.add(formatDateForDisplay(date));
            index++;
        }
        
        // Create datasets for each language
        LineDataSet sinhalaDataSet = null;
        if (!sinhalaEntries.isEmpty()) {
            sinhalaDataSet = new LineDataSet(sinhalaEntries, "Sinhala Accuracy (%)");
            sinhalaDataSet.setColor(ContextCompat.getColor(this, R.color.teal_700));
            sinhalaDataSet.setLineWidth(2f);
            sinhalaDataSet.setCircleColor(ContextCompat.getColor(this, R.color.teal_700));
            sinhalaDataSet.setCircleRadius(4f);
            sinhalaDataSet.setDrawCircleHole(false);
            sinhalaDataSet.setValueTextSize(10f);
            sinhalaDataSet.setDrawFilled(true);
            sinhalaDataSet.setFillColor(ContextCompat.getColor(this, R.color.teal_200));
        }
        
        LineDataSet englishDataSet = null;
        if (!englishEntries.isEmpty()) {
            englishDataSet = new LineDataSet(englishEntries, "English Accuracy (%)");
            englishDataSet.setColor(ContextCompat.getColor(this, R.color.purple_500));
            englishDataSet.setLineWidth(2f);
            englishDataSet.setCircleColor(ContextCompat.getColor(this, R.color.purple_500));
            englishDataSet.setCircleRadius(4f);
            englishDataSet.setDrawCircleHole(false);
            englishDataSet.setValueTextSize(10f);
            englishDataSet.setDrawFilled(true);
            englishDataSet.setFillAlpha(60); // More transparent fill for better visibility
            englishDataSet.setFillColor(ContextCompat.getColor(this, R.color.purple_200));
        }
        
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        if (sinhalaDataSet != null) dataSets.add(sinhalaDataSet);
        if (englishDataSet != null) dataSets.add(englishDataSet);
        
        LineData lineData = new LineData(dataSets);
        
        // Configure X axis
        XAxis xAxis = accuracyLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        
        // Configure Y axis
        YAxis leftAxis = accuracyLineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);
        
        accuracyLineChart.getAxisRight().setEnabled(false);
        accuracyLineChart.getDescription().setEnabled(false);
        accuracyLineChart.setData(lineData);
        accuracyLineChart.getLegend().setEnabled(true); // Enable legend to show language differentiation
        accuracyLineChart.animateY(1000);
        accuracyLineChart.invalidate();
    }
    
    private void updateGameScoresChart() {
        if (gameScoresData.isEmpty()) {
            // Also check if there's any game data directly under the date node for backward compatibility
            Log.d(TAG, "No game scores data found. Checking for direct score entries.");
            updateGameScoresFromDirectEntries();
            return;
        }
        Log.d(TAG, "Game scores data found: " + gameScoresData.size() + " entries");

        // Arrays for line chart entries, grouped by language and game type
        List<Entry> singSongSinhalaEntries = new ArrayList<>();
        List<Entry> spellWordSinhalaEntries = new ArrayList<>();
        List<Entry> storyScoreSinhalaEntries = new ArrayList<>();
        
        List<Entry> singSongEnglishEntries = new ArrayList<>();
        List<Entry> spellWordEnglishEntries = new ArrayList<>();
        List<Entry> storyScoreEnglishEntries = new ArrayList<>();
        
        List<String> xLabels = new ArrayList<>();
        
        // Process each date's scores
        int index = 0;
        for (Map.Entry<String, Map<String, Integer>> dateEntry : gameScoresData.entrySet()) {
            Map<String, Integer> scores = dateEntry.getValue();
            
            // Extract Sinhala scores
            int singSongSinhalaScore = scores.getOrDefault("sing_song_sinhala", scores.getOrDefault("sing_song", 0));
            int spellWordSinhalaScore = scores.getOrDefault("spell_word_sinhala", scores.getOrDefault("spell_word", 0));
            int storyScoreSinhalaScore = scores.getOrDefault("listen_story_sinhala", scores.getOrDefault("listen_story", 0));
            
            // Extract English scores
            int singSongEnglishScore = scores.getOrDefault("sing_song_english", 0);
            int spellWordEnglishScore = scores.getOrDefault("spell_word_english", 0);
            int storyScoreEnglishScore = scores.getOrDefault("listen_story_english", 0);
            
            // Add entries for each game type
            singSongSinhalaEntries.add(new Entry(index, singSongSinhalaScore));
            spellWordSinhalaEntries.add(new Entry(index, spellWordSinhalaScore));
            storyScoreSinhalaEntries.add(new Entry(index, storyScoreSinhalaScore));
            
            singSongEnglishEntries.add(new Entry(index, singSongEnglishScore));
            spellWordEnglishEntries.add(new Entry(index, spellWordEnglishScore));
            storyScoreEnglishEntries.add(new Entry(index, storyScoreEnglishScore));
            
            // Add date label
            xLabels.add(formatDateForDisplay(dateEntry.getKey()));
            index++;
        }
        
        // Create line datasets for each game type
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        
        // Sinhala datasets
        LineDataSet singSongSinhalaDataSet = new LineDataSet(singSongSinhalaEntries, "Singing (Sinhala)");
        singSongSinhalaDataSet.setColor(ContextCompat.getColor(this, R.color.teal_700));
        singSongSinhalaDataSet.setLineWidth(2f);
        singSongSinhalaDataSet.setCircleColor(ContextCompat.getColor(this, R.color.teal_700));
        singSongSinhalaDataSet.setCircleRadius(4f);
        dataSets.add(singSongSinhalaDataSet);
        
        LineDataSet spellWordSinhalaDataSet = new LineDataSet(spellWordSinhalaEntries, "Spelling (Sinhala)");
        spellWordSinhalaDataSet.setColor(ContextCompat.getColor(this, R.color.purple_500));
        spellWordSinhalaDataSet.setLineWidth(2f);
        spellWordSinhalaDataSet.setCircleColor(ContextCompat.getColor(this, R.color.purple_500));
        spellWordSinhalaDataSet.setCircleRadius(4f);
        dataSets.add(spellWordSinhalaDataSet);
        
        LineDataSet storyScoreSinhalaDataSet = new LineDataSet(storyScoreSinhalaEntries, "Story (Sinhala)");
        storyScoreSinhalaDataSet.setColor(ContextCompat.getColor(this, R.color.orange));
        storyScoreSinhalaDataSet.setLineWidth(2f);
        storyScoreSinhalaDataSet.setCircleColor(ContextCompat.getColor(this, R.color.orange));
        storyScoreSinhalaDataSet.setCircleRadius(4f);
        dataSets.add(storyScoreSinhalaDataSet);
        
        // English datasets
        LineDataSet singSongEnglishDataSet = new LineDataSet(singSongEnglishEntries, "Singing (English)");
        singSongEnglishDataSet.setColor(ContextCompat.getColor(this, R.color.teal_200));
        singSongEnglishDataSet.setLineWidth(2f);
        singSongEnglishDataSet.setCircleColor(ContextCompat.getColor(this, R.color.teal_200));
        singSongEnglishDataSet.setCircleRadius(4f);
        dataSets.add(singSongEnglishDataSet);
        
        LineDataSet spellWordEnglishDataSet = new LineDataSet(spellWordEnglishEntries, "Spelling (English)");
        spellWordEnglishDataSet.setColor(ContextCompat.getColor(this, R.color.purple_200));
        spellWordEnglishDataSet.setLineWidth(2f);
        spellWordEnglishDataSet.setCircleColor(ContextCompat.getColor(this, R.color.purple_200));
        spellWordEnglishDataSet.setCircleRadius(4f);
        dataSets.add(spellWordEnglishDataSet);
        
        LineDataSet storyScoreEnglishDataSet = new LineDataSet(storyScoreEnglishEntries, "Story (English)");
        storyScoreEnglishDataSet.setColor(ContextCompat.getColor(this, R.color.yellow));
        storyScoreEnglishDataSet.setLineWidth(2f);
        storyScoreEnglishDataSet.setCircleColor(ContextCompat.getColor(this, R.color.yellow));
        storyScoreEnglishDataSet.setCircleRadius(4f);
        dataSets.add(storyScoreEnglishDataSet);
        
        LineData lineData = new LineData(dataSets);
        
        // Configure X axis
        XAxis xAxis = gameScoresBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setLabelRotationAngle(45f); // Rotate labels for better readability
        
        // Configure Y axis
        YAxis leftAxis = gameScoresBarChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);
        
        gameScoresBarChart.getAxisRight().setEnabled(false);
        gameScoresBarChart.getDescription().setEnabled(false);
        gameScoresBarChart.getLegend().setEnabled(true);
        gameScoresBarChart.setData(lineData);
        gameScoresBarChart.animateY(1000);
        gameScoresBarChart.invalidate();
    }
    
    private void updateGameScoresFromDirectEntries() {
        // Create an empty map to hold scores from direct entries
        Map<String, Integer> directScores = new HashMap<>();
        boolean hasAnyScores = false;
        
        // Check for direct entries in speechAccuracyData's dates
        for (String date : speechAccuracyData.keySet()) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) continue;
            
            String userId = currentUser.getUid();
            DatabaseReference userRef = databaseReference.child(userId);
            
            // Check for sing_song, spell_word, and listen_story in the main user data
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    Map<String, Integer> scores = new HashMap<>();
                    
                    // Check main user data for game scores
                    Integer singSong = snapshot.child("sing_song").getValue(Integer.class);
                    Integer spellWord = snapshot.child("spell_word").getValue(Integer.class);
                    Integer listenStory = snapshot.child("listen_story").getValue(Integer.class);
                    
                    if (singSong != null) scores.put("sing_song", singSong);
                    if (spellWord != null) scores.put("spell_word", spellWord);
                    if (listenStory != null) scores.put("listen_story", listenStory);
                    
                    Log.d(TAG, "Found direct scores: " + scores);
                    
                    if (!scores.isEmpty()) {
                        // We have found some scores, update the game chart
                        gameScoresData.put(date, scores);
                        updateGameScoresChart();
                    } else {
                        gameScoresBarChart.setNoDataText("No game score data available");
                        gameScoresBarChart.invalidate();
                    }
                }
                
                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "Error accessing user data: " + error.getMessage());
                    gameScoresBarChart.setNoDataText("Error loading game data");
                    gameScoresBarChart.invalidate();
                }
            });
            
            // Only need to check once
            return;
        }
        
        // If we reach here, we didn't find any date entries
        if (!hasAnyScores) {
            gameScoresBarChart.setNoDataText("No game score data available");
            gameScoresBarChart.invalidate();
        }
    }
    
    private void updateCommunicationChart() {
        if (communicationData.isEmpty()) {
            communicationBarChart.setNoDataText("No communication data available");
            communicationBarChart.invalidate();
            return;
        }
        
        List<Entry> entries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();
        
        int index = 0;
        for (Map.Entry<String, Integer> entry : communicationData.entrySet()) {
            entries.add(new Entry(index, entry.getValue()));
            // Format date for display
            xLabels.add(formatDateForDisplay(entry.getKey()));
            index++;
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "Communications with Therapist");
        dataSet.setColor(ContextCompat.getColor(this, R.color.purple_500));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.purple_500));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.purple_200));
        
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);
        
        LineData lineData = new LineData(dataSets);
        
        // Configure X axis
        XAxis xAxis = communicationBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        
        // Configure Y axis
        YAxis leftAxis = communicationBarChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        
        communicationBarChart.getAxisRight().setEnabled(false);
        communicationBarChart.getDescription().setEnabled(false);
        communicationBarChart.setData(lineData);
        communicationBarChart.animateY(1000);
        communicationBarChart.invalidate();
    }
    
    private String formatDateForDisplay(String dbDate) {
        try {
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd", Locale.US);
            Date date = dbFormat.parse(dbDate);
            return displayFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + e.getMessage());
            return dbDate;
        }
    }
    
    private void exportProgressReport() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 and above, we don't need storage permission for app-specific files
            generateReport();
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            // For Android 12 and below, check and request storage permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                generateReport();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generateReport();
            } else {
                Toast.makeText(this, "Storage permission is required to save the report", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void generateReport() {
        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Generating Report...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        try {
            // Create file name with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String fileName = "SpeechProgress_" + timestamp + ".pdf";
            File pdfFile = new File(getFilesDir(), fileName);

            // Generate PDF
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
            document.open();

            // Define fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Font contentFont = FontFactory.getFont(FontFactory.HELVETICA, 18);

            // Add title
            document.add(new Paragraph("Speech Therapy Progress Report", titleFont));
            document.add(new Paragraph("Generated on: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()), contentFont));
            document.add(new Paragraph("\n"));

            // Add speech accuracy data
            document.add(new Paragraph("Speech Accuracy", subtitleFont));
            document.add(new Paragraph("Sinhala Speech Accuracy:", contentFont));
            for (Map.Entry<String, Integer> entry : speechAccuracySinhalaData.entrySet()) {
                document.add(new Paragraph(String.format("  %s: %d%%", formatDateForDisplay(entry.getKey()), entry.getValue()), contentFont));
            }
            document.add(new Paragraph("\nEnglish Speech Accuracy:", contentFont));
            for (Map.Entry<String, Integer> entry : speechAccuracyEnglishData.entrySet()) {
                document.add(new Paragraph(String.format("  %s: %d%%", formatDateForDisplay(entry.getKey()), entry.getValue()), contentFont));
            }
            document.add(new Paragraph("\n"));

            // Add game scores data
            document.add(new Paragraph("Game Performance", subtitleFont));
            for (Map.Entry<String, Map<String, Integer>> dateEntry : gameScoresData.entrySet()) {
                document.add(new Paragraph("\nDate: " + formatDateForDisplay(dateEntry.getKey()), contentFont));
                Map<String, Integer> scores = dateEntry.getValue();
                
                // Sinhala games
                document.add(new Paragraph("Sinhala Games:", contentFont));
                document.add(new Paragraph(String.format("  Singing: %d%%", scores.getOrDefault("sing_song_sinhala", scores.getOrDefault("sing_song", 0))), contentFont));
                document.add(new Paragraph(String.format("  Spelling: %d%%", scores.getOrDefault("spell_word_sinhala", scores.getOrDefault("spell_word", 0))), contentFont));
                document.add(new Paragraph(String.format("  Story: %d%%", scores.getOrDefault("listen_story_sinhala", scores.getOrDefault("listen_story", 0))), contentFont));
                
                // English games
                document.add(new Paragraph("English Games:", contentFont));
                document.add(new Paragraph(String.format("  Singing: %d%%", scores.getOrDefault("sing_song_english", 0)), contentFont));
                document.add(new Paragraph(String.format("  Spelling: %d%%", scores.getOrDefault("spell_word_english", 0)), contentFont));
                document.add(new Paragraph(String.format("  Story: %d%%", scores.getOrDefault("listen_story_english", 0)), contentFont));
            }
            document.add(new Paragraph("\n"));

            // Add communication data
            document.add(new Paragraph("Communication Progress", subtitleFont));
            for (Map.Entry<String, Integer> entry : communicationData.entrySet()) {
                document.add(new Paragraph(String.format("%s: %d interactions", formatDateForDisplay(entry.getKey()), entry.getValue()), contentFont));
            }

            document.close();

            // Copy file to Downloads folder
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File downloadFile = new File(downloadsDir, fileName);
            FileOutputStream outputStream = new FileOutputStream(downloadFile);
            outputStream.write(java.nio.file.Files.readAllBytes(pdfFile.toPath()));
            outputStream.close();

            progressDialog.dismiss();
            Toast.makeText(this, "Report saved to Downloads folder: " + fileName, Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Error generating report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
} 