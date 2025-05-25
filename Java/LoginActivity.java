package com.example.speechtherapy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {
    Button login;
    FirebaseAuth mAuth;
    ProgressBar progressBar;
    TextInputEditText editTextEmail, editTextPassword;
    TextView kid,therapist;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        therapist = findViewById(R.id.therapist);
        kid = findViewById(R.id.kid);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // Initialize views
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        progressBar = findViewById(R.id.progressBar);
        login = findViewById(R.id.button);

        kid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openkidActivity();
            }
        });
        therapist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                opentheActivity();
            }
        });
        // Handle login button click
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                String email, password;
                email = String.valueOf(editTextEmail.getText());
                password = String.valueOf(editTextPassword.getText());
                // Validate email and password
                if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(LoginActivity.this, "Enter a valid email address", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(LoginActivity.this, "Enter a valid password", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                // Sign in the user
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // Sign in success, get the current user
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    checkUserRoleAndNavigate(user.getUid());
                                }
                            } else {
                                // If sign-in fails, display a message to the user
                                Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                            }
                        });
            }
        });
    }
    public void openhomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }
    public void openkidActivity() {
        Intent intent = new Intent(this, RegisterKidActivity.class);
        startActivity(intent);
    }
    public void opentheActivity() {
        Intent intent = new Intent(this, RegisterTherapistActivity.class);
        startActivity(intent);
    }
    // Method to check the user's role and navigate to the corresponding activity
    private void checkUserRoleAndNavigate(String uid) {
        // Assuming the user role is stored in Firebase Realtime Database
        DatabaseReference userRoleRef = FirebaseDatabase.getInstance().getReference("user").child(uid).child("userRole");

        userRoleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String role = snapshot.getValue(String.class); // Get the user's role
                    if ("kid".equals(role)) {
                        // Navigate to PlayerActivity
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish(); // Optionally finish the login activity
                    } else if ("therapist".equals(role)) {
                        // Navigate to ShopOwnerActivity
                        Intent intent = new Intent(LoginActivity.this, UserChatListActivity.class);
                        startActivity(intent);
                        finish(); // Optionally finish the login activity
                    }
                } else {
                    // Handle case where role is not found
                    Toast.makeText(LoginActivity.this, "User role not found.", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
            }@Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle database error
                Toast.makeText(LoginActivity.this, "Failed to retrieve role.", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}