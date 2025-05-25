package com.example.speechtherapy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class RegisterKidActivity extends AppCompatActivity {
    private Button register;
    private TextView text;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private TextInputEditText editTextEmail, editTextPassword, uname,cont;

    @Override
    public void onStart() {
        super.onStart();

        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userRoleRef = FirebaseDatabase.getInstance().getReference("user")
                    .child(currentUser.getUid()).child("userRole");

            userRoleRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String role = snapshot.getValue(String.class); // Get the user's role

                        if ("kid".equals(role)) {
                            // If the role is Kid, open HomeActivity
                            Intent intent = new Intent(RegisterKidActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish(); // Optional: Finish the current activity so user can't go back to login
                        } else if ("therapist".equals(role)) {
                            // If the role is Therapist, open UserChatListActivity
                            Intent intent = new Intent(RegisterKidActivity.this, UserChatListActivity.class);
                            startActivity(intent);
                            finish(); // Optional: Finish the current activity so user can't go back to login
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle database errors here if needed
                    Toast.makeText(RegisterKidActivity.this, "Failed to retrieve role.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_kid);
        mAuth = FirebaseAuth.getInstance();
        text = findViewById(R.id.textView2);
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        uname = findViewById(R.id.uname);
        progressBar = findViewById(R.id.progressBar);
        cont= findViewById(R.id.contact);
        text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLoginActivity();
            }
        });

        register = findViewById(R.id.buttonsignup);

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                String email, password, username,contact;

                email = String.valueOf(editTextEmail.getText());
                password = String.valueOf(editTextPassword.getText());
                username = String.valueOf(uname.getText());
                contact = String.valueOf(cont.getText());
                // Validate email
                if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(RegisterKidActivity.this, "Enter a valid email address", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                // Validate password
                if (TextUtils.isEmpty(password) || password.length() < 6) {
                    Toast.makeText(RegisterKidActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                // Validate username
                if (TextUtils.isEmpty(username)) {
                    Toast.makeText(RegisterKidActivity.this, "Please enter a username", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                // Validate username
                if (TextUtils.isEmpty(contact)) {
                    Toast.makeText(RegisterKidActivity.this, "Please enter a contact number", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                // Create user with email and password
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    // Get the current user
                                    FirebaseUser user = mAuth.getCurrentUser();

                                    // Get the user's UID
                                    String userId = user.getUid();
                                    storeUserInfo(userId, email, username,contact); // Store the user's information

                                    Toast.makeText(RegisterKidActivity.this, "Account created", Toast.LENGTH_SHORT).show();
                                    openHomeActivity();
                                } else {
                                    // If sign in fails, display a message to the user.
                                    Toast.makeText(RegisterKidActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }

    public void openHomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    public void openLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    private void storeUserInfo(String userId, String email, String username, String contact) {
        // Create a reference to the "user" node in the database
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("user");

        // Create a HashMap to store user information
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("id", userId);
        userMap.put("email", email);
        userMap.put("uname", username);
        userMap.put("contact", contact);
        userMap.put("userRole", "kid");

        // Store the user information in the database under the user's UID
        userRef.child(userId).setValue(userMap);

        DatabaseReference kidRef = FirebaseDatabase.getInstance().getReference().child("kid");

        // Create a HashMap to store kid specific information
        HashMap<String, Object> kidMap = new HashMap<>();
        kidMap.put("id", userId);
        kidMap.put("email", email);
        kidMap.put("uname", username);  // Save the username from the input
        kidMap.put("contact", contact);
        // Store the kid information under the "kid" node
        kidRef.child(userId).setValue(kidMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(RegisterKidActivity.this, "Kid information saved.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(RegisterKidActivity.this, "Failed to save kid information.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
