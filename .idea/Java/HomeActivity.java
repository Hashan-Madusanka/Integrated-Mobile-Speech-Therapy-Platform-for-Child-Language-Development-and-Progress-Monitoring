package com.example.speechtherapy;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class HomeActivity extends AppCompatActivity {
Button button1,button3,button2,button4;
ImageView logout,profile,profileImage;
DatabaseReference userRef;
    FirebaseStorage storage;
    StorageReference storageReference;
    private Switch premiumSwitch;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        button1 = findViewById(R.id.button1);
        button2= findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);
        button4 = findViewById(R.id.button4);
        logout = findViewById(R.id.logout);
        profile= findViewById(R.id.imageView4);
        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profileActivity();
            }
        });
        profileImage= findViewById(R.id.imageView4);
        // Initialize Firebase Storage
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        String currentUserUid = getCurrentUserUid();
        loadImageFromFirebaseStorage(currentUserUid);
        // Initialize Firebase reference
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            userRef = FirebaseDatabase.getInstance().getReference("user").child(userId);
            // Check the premium status and set the switch state
            checkPremiumStatus();
        }
        // Setup the switch listener for premium status toggle
        setupPremiumSwitchListener();

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                opencleftActivity();
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                opentherapyplanActivity();
            }
        });
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                opengameActivity();
            }
        });
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openreportActivity();
            }
        });
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Show confirmation dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                builder.setTitle("Logout");
                builder.setMessage("Are you sure you want to logout?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Logout user
                        FirebaseAuth.getInstance().signOut();
                        // If the user is signed in with email/password
                        openloginActivity();
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Dismiss dialog, do nothing
                        dialogInterface.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                // Change the button text color to white
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.GREEN);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.GREEN);
            }
        });

        premiumSwitch = findViewById(R.id.premium_switch);
        mAuth = FirebaseAuth.getInstance();

        // Reference to Firebase User node
        String currentUserId = mAuth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("user").child(currentUserId);

        // Set switch listener
        premiumSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Update Firebase with the new status
                userRef.child("isPremium").setValue(isChecked)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(HomeActivity.this,
                                    isChecked ? "Premium Activated" : "Premium Deactivated",
                                    Toast.LENGTH_SHORT).show();

                            // Change the switch color dynamically
                            if (isChecked) {
                                premiumSwitch.setThumbTintList(getResources().getColorStateList(R.color.green));
                                premiumSwitch.setTrackTintList(getResources().getColorStateList(R.color.green));
                            } else {
                                premiumSwitch.setThumbTintList(getResources().getColorStateList(R.color.red));
                                premiumSwitch.setTrackTintList(getResources().getColorStateList(R.color.red));
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(HomeActivity.this,
                                    "Error updating status. Try again.",
                                    Toast.LENGTH_SHORT).show();
                        });
            }
        });
        setupPremiumSwitchListener();
    }



    public void opencleftActivity() {
        Intent intent = new Intent(this, Deseases1Activity.class);
        startActivity(intent);
    }
    public void opentherapyplanActivity() {
        Intent intent = new Intent(this, TherapyQuestion.class);
        startActivity(intent);
    }
    public void opengameActivity() {
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
    }
    public void openreportActivity() {
        Intent intent = new Intent(this, GenerateReportActivity.class);
        startActivity(intent);
    }
    public void openloginActivity() {
        Intent intent = new Intent(this,LoginActivity.class);
        startActivity(intent);
    }
    public void profileActivity() {
        Intent intent = new Intent(this,ProfileActivity.class);
        startActivity(intent);
    }
    private void loadImageFromFirebaseStorage(String userUid) {
        // Check if the activity is still alive
        if (!isFinishing() && !isDestroyed()) {
            StorageReference imageRef = storageReference
                    .child("profile_images/user_" + userUid + ".jpg");
            // Use Glide to load and display the image
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                if (!isFinishing() && !isDestroyed()) {
                    String imageUrl = uri.toString();
                    Glide.with(this)
                            .load(imageUrl)
                            .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                            .into(profileImage);
                }
            });
        }
    }
    private String getCurrentUserUid() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            return user.getUid();
        } else {
            // Handle the case where the user is not authenticated
            return "";
        }
    }
    private void checkPremiumStatus() {
        userRef.child("isPremium").get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                boolean isPremium = snapshot.getValue(Boolean.class);
                premiumSwitch.setChecked(isPremium);

                // Set switch colors based on the status
                updateSwitchColors(isPremium);
            }
        }).addOnFailureListener(e -> Log.e("Firebase", "Error fetching isPremium status", e));
    }

    private void updateSwitchColors(boolean isPremium) {
        if (isPremium) {
            premiumSwitch.setThumbTintList(getResources().getColorStateList(R.color.green));
            premiumSwitch.setTrackTintList(getResources().getColorStateList(R.color.green));
        } else {
            premiumSwitch.setThumbTintList(getResources().getColorStateList(R.color.red));
            premiumSwitch.setTrackTintList(getResources().getColorStateList(R.color.red));
        }
    }
    private void setupPremiumSwitchListener() {
        premiumSwitch = findViewById(R.id.premium_switch);
        premiumSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            userRef.child("isPremium").setValue(isChecked)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(HomeActivity.this,
                                isChecked ? "Premium Activated" : "Premium Deactivated",
                                Toast.LENGTH_SHORT).show();
                        updateSwitchColors(isChecked);
                    })
                    .addOnFailureListener(e -> Toast.makeText(HomeActivity.this,
                            "Error updating status. Try again.", Toast.LENGTH_SHORT).show());
        });
    }
}