package com.example.speechtherapy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
ImageView start;
    FirebaseAuth mAuth;
    @Override
    public void onStart() {
        super.onStart();

        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {

            // Example: Retrieve the role from Firebase Realtime Database
            DatabaseReference userRoleRef = FirebaseDatabase.getInstance().getReference("user").child(currentUser.getUid()).child("userRole");

            userRoleRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String role = snapshot.getValue(String.class); // Get the user's role

                        // Enable the button after role is retrieved
                        start.setEnabled(true);

                        if ("kid".equals(role)) {
                            // If the role is Player, open PlayerActivity
                            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish(); // Optional: Finish the current activity so user can't go back to login
                        } else if ("therapist".equals(role)) {
                            // If the role is Shop Owner, open ShopOwnerActivity
                            Intent intent = new Intent(MainActivity.this, UserChatListActivity.class);
                            startActivity(intent);
                            finish(); // Optional: Finish the current activity so user can't go back to login
                        }
                    } else {
                        // Enable the button even if the role doesn't exist to allow for other actions
                        start.setEnabled(true);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle database errors here if needed
                    Toast.makeText(MainActivity.this, "Failed to retrieve role.", Toast.LENGTH_SHORT).show();
                    // Enable button in case of an error as well
                    start.setEnabled(true);
                }
            });
        } else {
            // Enable the button if there's no current user (could direct to login screen)
            start.setEnabled(true);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialize FirebaseAuth
        mAuth = FirebaseAuth.getInstance();
        start = findViewById(R.id.start);
        start.setEnabled(false);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openloginActivity();
            }
        });
    }
    public void openloginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }
}