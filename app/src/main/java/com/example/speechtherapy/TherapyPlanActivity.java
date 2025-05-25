package com.example.speechtherapy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TherapyPlanActivity extends AppCompatActivity {
    ImageView home;
    TextView duration, progressText;
    ProgressBar progressBar, CircleBar;
    PieChart circleChart;
    LinearLayout activitiesLayout;
    Button saveProgressButton;
    Map<String, CheckBox> checkBoxMap = new HashMap<>();
    Map<String, List<CheckBox>> subCheckBoxesMap = new HashMap<>();
    Map<String, TextView> subCheckBoxRateMap = new HashMap<>();
    Map<String, LinearLayout> subCheckBoxLayoutMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.therapy_plan);

        home = findViewById(R.id.home);
        duration = findViewById(R.id.duration);
        activitiesLayout = findViewById(R.id.activitiesLayout2);
        saveProgressButton = findViewById(R.id.saveProgressButton2);
        progressBar = findViewById(R.id.progressBar); // Initialize progress bar
        progressText = findViewById(R.id.progressText);
        circleChart = findViewById(R.id.circleChart);

        // Initialize Pie Chart
        circleChart.setUsePercentValues(true);
        circleChart.getDescription().setEnabled(false);
        circleChart.setDrawHoleEnabled(true);  // Donut style
        circleChart.setHoleRadius(60f);
        circleChart.setTransparentCircleRadius(65f);

        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openhomeActivity();
            }
        });

        fetchTherapyPlan();

        saveProgressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProgressToFirebase();
            }
        });
    }

    public void openhomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    private void fetchTherapyPlan() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                .getReference("therapy_plan")
                .child(userId)
                .child("plan");

        // Fetch the therapy plan data (for displaying activities and duration)
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String therapyPlanData = dataSnapshot.getValue(String.class);

                    try {
                        JSONObject therapyPlanJson = new JSONObject(therapyPlanData);
                        String therapyPlan = therapyPlanJson.getString("Therapy Plan Prediction");

                        // Extract duration and activities
                        String[] splitPlan = therapyPlan.split("Activities-");
                        String durationText = splitPlan[0].replace("Duration-", "").trim();
                        String[] activityParts = splitPlan[1].trim().split(",");

                        // Set duration text
                        duration.setText("Duration: " + durationText);
                        duration.setTextSize(20);

                        // Define colors
                        ColorStateList orangeColor; // Orange
                        orangeColor = ColorStateList.valueOf(Color.parseColor("#FFA500"));
                        ColorStateList blackColor = ColorStateList.valueOf(Color.BLACK); // Default black

                        // Display checkboxes for each activity
                        activitiesLayout.removeAllViews();
                        for (String activity : activityParts) {
                            final String activityName = activity.trim();

                            // Main item layout
                            LinearLayout itemLayout = new LinearLayout(TherapyPlanActivity.this);
                            itemLayout.setOrientation(LinearLayout.VERTICAL);
                            itemLayout.setLayoutParams(new LayoutParams(
                                    LayoutParams.MATCH_PARENT,
                                    LayoutParams.WRAP_CONTENT
                            ));

                            // Main activity checkbox layout (horizontal)
                            LinearLayout mainCheckboxLayout = new LinearLayout(TherapyPlanActivity.this);
                            mainCheckboxLayout.setOrientation(LinearLayout.HORIZONTAL);
                            mainCheckboxLayout.setLayoutParams(new LayoutParams(
                                    LayoutParams.MATCH_PARENT,
                                    LayoutParams.WRAP_CONTENT
                            ));

                            CheckBox checkBox = new CheckBox(TherapyPlanActivity.this);
                            checkBox.setText(activityName);
                            checkBox.setTextColor(Color.parseColor("#000000"));
                            checkBox.setTextSize(18);
                            checkBox.setLayoutParams(new LayoutParams(
                                    LayoutParams.WRAP_CONTENT,
                                    LayoutParams.WRAP_CONTENT
                            ));

                            checkBox.setButtonTintList(blackColor);

                            ImageView checkIcon = new ImageView(TherapyPlanActivity.this);
                            checkIcon.setImageResource(R.drawable.one);
                            checkIcon.setVisibility(View.GONE);
                            checkIcon.setLayoutParams(new LayoutParams(60, 60));

                            // Add sub-checkboxes layout (initially hidden)
                            LinearLayout subCheckboxesLayout = new LinearLayout(TherapyPlanActivity.this);
                            subCheckboxesLayout.setOrientation(LinearLayout.VERTICAL);
                            subCheckboxesLayout.setLayoutParams(new LayoutParams(
                                    LayoutParams.MATCH_PARENT,
                                    LayoutParams.WRAP_CONTENT
                            ));
                            subCheckboxesLayout.setPadding(50, 0, 0, 0); // Indent sub-checkboxes
                            subCheckboxesLayout.setVisibility(View.GONE);

                            // Create sub-checkboxes
                            List<CheckBox> subCheckboxes = new ArrayList<>();
                            String[] subItems = {"1st time", "2nd time", "3rd time"};

                            for (String subItem : subItems) {
                                CheckBox subCheckBox = new CheckBox(TherapyPlanActivity.this);
                                subCheckBox.setText(subItem);
                                subCheckBox.setTextColor(Color.parseColor("#000000"));
                                subCheckBox.setTextSize(16);
                                subCheckBox.setButtonTintList(blackColor);
                                subCheckBox.setLayoutParams(new LayoutParams(
                                        LayoutParams.WRAP_CONTENT,
                                        LayoutParams.WRAP_CONTENT
                                ));

                                subCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                    updateSubCheckboxRate(activityName);
                                });

                                subCheckboxes.add(subCheckBox);
                                subCheckboxesLayout.addView(subCheckBox);
                            }

                            // Add rate display
                            LinearLayout rateLayout = new LinearLayout(TherapyPlanActivity.this);
                            rateLayout.setOrientation(LinearLayout.HORIZONTAL);
                            rateLayout.setLayoutParams(new LayoutParams(
                                    LayoutParams.MATCH_PARENT,
                                    LayoutParams.WRAP_CONTENT
                            ));
                            rateLayout.setPadding(50, 8, 0, 8);

                            TextView rateLabel = new TextView(TherapyPlanActivity.this);
                            rateLabel.setText("Rate: ");
                            rateLabel.setTextColor(Color.BLACK);
                            rateLabel.setTextSize(16);

                            TextView rateValue = new TextView(TherapyPlanActivity.this);
                            rateValue.setText("0%");
                            rateValue.setTextColor(Color.BLACK);
                            rateValue.setTextSize(16);

                            rateLayout.addView(rateLabel);
                            rateLayout.addView(rateValue);
                            subCheckboxesLayout.addView(rateLayout);

                            // Store in maps for later reference
                            subCheckBoxesMap.put(activityName, subCheckboxes);
                            subCheckBoxRateMap.put(activityName, rateValue);
                            subCheckBoxLayoutMap.put(activityName, subCheckboxesLayout);

                            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                if (isChecked) {
                                    checkBox.setButtonTintList(orangeColor);
                                    checkIcon.setVisibility(View.VISIBLE);
                                    subCheckboxesLayout.setVisibility(View.VISIBLE);

                                    // Find the sub-checkboxes associated with this main checkbox
                                    List<CheckBox> subCheckboxesList = subCheckBoxesMap.get(activityName);
                                    if (subCheckboxesList != null && !subCheckboxesList.isEmpty()) {
                                        // Check the first sub-checkbox
                                        subCheckboxesList.get(0).setChecked(true);
                                    }
                                } else {
                                    checkBox.setButtonTintList(blackColor);
                                    checkIcon.setVisibility(View.GONE);
                                    subCheckboxesLayout.setVisibility(View.GONE);

                                    // Find the sub-checkboxes associated with this main checkbox
                                    List<CheckBox> subCheckboxesList = subCheckBoxesMap.get(activityName);
                                    if (subCheckboxesList != null) {
                                        for (CheckBox subCheckbox : subCheckboxesList) {
                                            subCheckbox.setChecked(false);
                                        }
                                    }
                                }
                                updateSubCheckboxRate(activityName);
                            });

                            mainCheckboxLayout.addView(checkBox);
                            mainCheckboxLayout.addView(checkIcon);

                            itemLayout.addView(mainCheckboxLayout);
                            itemLayout.addView(subCheckboxesLayout);

                            activitiesLayout.addView(itemLayout);

                            // Store the main checkbox in checkBoxMap
                            checkBoxMap.put(activityName, checkBox);
                        }

                        // Fetch saved checkbox states from Firebase
                        DatabaseReference checkboxStateRef = FirebaseDatabase.getInstance()
                                .getReference("therapy_plan")
                                .child(userId)
                                .child("checkbox");

                        checkboxStateRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    // Loop through each activity in the database
                                    for (DataSnapshot activitySnapshot : dataSnapshot.getChildren()) {
                                        String key = activitySnapshot.getKey();

                                        // Skip the checkedCount entry
                                        if (key.equals("checkedCount")) {
                                            continue;
                                        }

                                        // Convert database key back to activity name
                                        String activityName = key.replace("_", ".");

                                        // Get the main checkbox for this activity
                                        CheckBox mainCheckBox = checkBoxMap.get(activityName);
                                        if (mainCheckBox != null) {
                                            // Check if this is a map (new structure) or boolean (old structure)
                                            if (activitySnapshot.hasChild("checked")) {
                                                // New structure
                                                Boolean isChecked = activitySnapshot.child("checked").getValue(Boolean.class);
                                                if (isChecked != null) {
                                                    mainCheckBox.setChecked(isChecked);
                                                }

                                                // If checked and has subItems, process them
                                                if (isChecked && activitySnapshot.hasChild("subItems")) {
                                                    List<CheckBox> subCheckBoxes = subCheckBoxesMap.get(activityName);
                                                    if (subCheckBoxes != null) {
                                                        DataSnapshot subItemsSnapshot = activitySnapshot.child("subItems");

                                                        for (int i = 0; i < subCheckBoxes.size(); i++) {
                                                            String subKey = "time_" + (i + 1);
                                                            if (subItemsSnapshot.hasChild(subKey)) {
                                                                Boolean subChecked = subItemsSnapshot.child(subKey).getValue(Boolean.class);
                                                                if (subChecked != null) {
                                                                    subCheckBoxes.get(i).setChecked(subChecked);
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                // Old structure - direct boolean value
                                                Object value = activitySnapshot.getValue();
                                                boolean isChecked = false;
                                                if (value instanceof Boolean) {
                                                    isChecked = (Boolean) value;
                                                } else if (value instanceof Long) {
                                                    isChecked = ((Long) value) == 1L;
                                                }
                                                mainCheckBox.setChecked(isChecked);
                                            }
                                        }
                                    }

                                    // Process any sub-checkboxes from the old structure if they exist
                                    for (DataSnapshot activitySnapshot : dataSnapshot.getChildren()) {
                                        String key = activitySnapshot.getKey();
                                        if (key.contains("_subcheckbox_")) {
                                            String[] parts = key.split("_subcheckbox_");
                                            if (parts.length == 2) {
                                                String activityName = parts[0].replace("_", ".");
                                                int subIndex = Integer.parseInt(parts[1]);

                                                List<CheckBox> subCheckBoxes = subCheckBoxesMap.get(activityName);
                                                if (subCheckBoxes != null && subIndex < subCheckBoxes.size()) {
                                                    Object value = activitySnapshot.getValue();
                                                    boolean isChecked = false;
                                                    if (value instanceof Boolean) {
                                                        isChecked = (Boolean) value;
                                                    } else if (value instanceof Long) {
                                                        isChecked = ((Long) value) == 1L;
                                                    }
                                                    subCheckBoxes.get(subIndex).setChecked(isChecked);
                                                }
                                            }
                                        }
                                    }

                                    // Update all sub-checkbox rates
                                    for (String activity : checkBoxMap.keySet()) {
                                        updateSubCheckboxRate(activity);
                                    }

                                    // Update the main progress bar
                                    updateProgressBar();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.e("TherapyPlanActivity", "Error fetching checkbox states: " + databaseError.getMessage());
                            }
                        });

                    } catch (Exception e) {
                        Log.e("TherapyPlanActivity", "Error parsing therapy plan JSON: " + e.getMessage());
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("TherapyPlanActivity", "Error fetching therapy plan data: " + databaseError.getMessage());
            }
        });
    }

    private void updateSubCheckboxRate(String activityName) {
        List<CheckBox> subCheckboxes = subCheckBoxesMap.get(activityName);
        TextView rateText = subCheckBoxRateMap.get(activityName);

        if (subCheckboxes != null && rateText != null) {
            int totalChecked = 0;
            for (CheckBox cb : subCheckboxes) {
                if (cb.isChecked()) {
                    totalChecked++;
                }
            }

            int percentage = (int) ((totalChecked / (float) subCheckboxes.size()) * 100);
            rateText.setText(percentage + "%");
        }
    }

    private void saveProgressToFirebase() {
        progressBar.setVisibility(View.VISIBLE);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userTherapyPlanRef = FirebaseDatabase.getInstance()
                .getReference("therapy_plan")
                .child(userId)
                .child("checkbox"); // Main checkbox node

        Map<String, Object> allCheckboxData = new HashMap<>();
        int checkedCount = 0;  // Initialize counter for checked checkboxes

        // Process each main checkbox
        for (Map.Entry<String, CheckBox> entry : checkBoxMap.entrySet()) {
            String activityName = entry.getKey();
            String sanitizedKey = activityName
                    .replace(".", "_")
                    .replace("#", "_")
                    .replace("$", "_")
                    .replace("[", "_")
                    .replace("]", "_")
                    .replace("/", "_");

            boolean isChecked = entry.getValue().isChecked();

            // Create a map for this specific activity's data
            Map<String, Object> activityData = new HashMap<>();
            activityData.put("checked", isChecked);

            // Increment the count if the checkbox is checked
            if (isChecked) {
                checkedCount++;

                // Process sub-checkboxes for this activity
                List<CheckBox> subCheckboxes = subCheckBoxesMap.get(activityName);
                if (subCheckboxes != null) {
                    // Create a map for sub-checkboxes
                    Map<String, Boolean> subCheckboxesData = new HashMap<>();

                    for (int i = 0; i < subCheckboxes.size(); i++) {
                        String subItemName = "time_" + (i + 1);
                        subCheckboxesData.put(subItemName, subCheckboxes.get(i).isChecked());
                    }

                    // Add sub-checkboxes data to this activity
                    activityData.put("subItems", subCheckboxesData);

                    // Calculate and store the rating for this activity
                    int totalChecked = 0;
                    for (CheckBox cb : subCheckboxes) {
                        if (cb.isChecked()) {
                            totalChecked++;
                        }
                    }
                    int percentage = (int) ((totalChecked / (float) subCheckboxes.size()) * 100);
                    activityData.put("rate", percentage);
                }
            }

            // Add this activity's data to the overall data
            allCheckboxData.put(sanitizedKey, activityData);
        }

        // Store the checked count separately
        allCheckboxData.put("checkedCount", checkedCount);

        // Save all data at once
        userTherapyPlanRef.setValue(allCheckboxData).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);

            if (task.isSuccessful()) {
                updateProgressBar();
                Snackbar.make(findViewById(android.R.id.content),
                        "Progress saved successfully", Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(findViewById(android.R.id.content),
                        "Failed to save data. Please try again later", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProgressBar() {
        int totalCheckboxes = checkBoxMap.size();
        if (totalCheckboxes == 0) {
            setChartProgress(0);
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference checkedCountRef = FirebaseDatabase.getInstance()
                .getReference("therapy_plan")
                .child(userId)
                .child("checkbox")
                .child("checkedCount");

        checkedCountRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int checkedCount = 0;
                if (dataSnapshot.exists()) {
                    checkedCount = dataSnapshot.getValue(Integer.class);
                }

                int progressPercentage = (int) ((checkedCount / (float) totalCheckboxes) * 100);
                setChartProgress(progressPercentage);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("TherapyPlanActivity", "Error fetching checked count: " + databaseError.getMessage());
            }
        });
    }

    private void setChartProgress(int progressPercentage) {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(progressPercentage, "Completed"));
        entries.add(new PieEntry(100 - progressPercentage, "Remaining"));

        PieDataSet dataSet = new PieDataSet(entries, "Progress");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(14f);

        PieData data = new PieData(dataSet);
        circleChart.setData(data);
        progressText.setText(progressPercentage + "%");
        circleChart.invalidate(); // Refresh Chart
    }
}