package com.example.speechtherapy;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class ProfileActivity extends AppCompatActivity {
    ImageView back;
    Button button,save;
    ImageView profilePic;
    Button updateProfileBtn;
    ProgressBar progressBar;
    ActivityResultLauncher<Intent> imagePickLauncher;
    Uri imageUri; // To store the selected image URI
    FirebaseStorage storage;
    StorageReference storageReference;
    TextView mEmail;
    EditText name;
    EditText contact;
    private void setUserData() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user.getEmail() != null) {
            String email = user.getEmail();
            //set email
            mEmail.setText(email);
        }


        StorageReference imageRef = storageReference
                .child("profile_images/user_" + user.getUid() + ".jpg");
        // Use Glide to load and display the image
        imageRef.getDownloadUrl().
                addOnSuccessListener(uri ->
                {
                    String imageUrl = uri.toString();
                    Glide.with(this)
                            .load(imageUrl)
                            .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                            .into(profilePic);
                });
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);
        save= findViewById(R.id.save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the current user's UID
                String currentUserUid = getCurrentUserUid();

                // Create a reference to the "user" node in the database
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("user").child(currentUserUid);

                // Get the name and contact information from the EditText fields
                String userName = name.getText().toString();
                String userContact = contact.getText().toString();

                // Save the user's name and contact information to the database
                userRef.child("uname").setValue(userName);
                userRef.child("contact").setValue(userContact);

                // Disable the email EditText field
                mEmail.setEnabled(false);

                // Inform the user that the changes have been saved
                Toast.makeText(ProfileActivity.this, "Changes saved successfully", Toast.LENGTH_SHORT).show();
            }
        });

        back= findViewById(R.id.back);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openhomeActivity();
            }
        });
        profilePic = findViewById(R.id.profile_img_view);
        updateProfileBtn = findViewById(R.id.profile_update_btn);
        progressBar = findViewById(R.id.progressBar);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        mEmail = findViewById(R.id.profile_email);
        name = findViewById(R.id.username);
        retrieveAndSetUserName();
        contact = findViewById(R.id.contact);
        retrieveAndSetcontact();

        setUserData();
        // Get the current user UID
        String uid = getCurrentUserUid();
        imagePickLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // Handle the picked image URI
                        imageUri = result.getData().getData();
                        profilePic.setImageURI(imageUri);
                        // Save the image URI to SharedPreferences
                        saveImageUri(imageUri);
                        // Call the method to upload the image to Firebase
                        uploadImageToFirebase();
                        loadCircularImage(imageUri);
                    }
                });

        // Check if there is a previously uploaded image URI and load it using Glide
        imageUri = loadImageUri();
        if (imageUri != null) {
            loadCircularImage(imageUri);
        }

        updateProfileBtn.setOnClickListener(view -> pickImage());

    }
    public void openhomeActivity() {
        Intent intent = new Intent(this,HomeActivity.class);
        startActivity(intent);
    }
    private void loadCircularImage(Uri imageUri) {
        Glide.with(this)
                .load(imageUri)
                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                .into(profilePic);
    }
    private void uploadImageToFirebase() {
        if (imageUri != null) {
            // Get the UID of the current user
            String userUid = getCurrentUserUid();
            // Get the reference to the image file in Firebase Storage
            StorageReference imageRef = storageReference.child("profile_images/user_" + userUid + ".jpg");

            // Upload the image to Firebase Storage
            UploadTask uploadTask = imageRef.putFile(imageUri);

            // Register observers to listen for the upload process
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                Toast.makeText(ProfileActivity.this, "Profile picture updated", Toast.LENGTH_SHORT).show();
                // Get the download URL (if needed) and load the image using Glide
                loadCircularImage(imageUri);
            }).addOnFailureListener(e -> {
                // Handle unsuccessful uploads
                Toast.makeText(ProfileActivity.this, "Failed to update profile picture", Toast.LENGTH_SHORT).show();
            }).addOnProgressListener(snapshot -> {
                // Track the progress of the upload
                double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                progressBar.setProgress((int) progress);
            });
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                saveProfileImageUrl(userUid, imageUrl);
            });
        } else {
            // No image selected
            Toast.makeText(ProfileActivity.this, "Please select an image", Toast.LENGTH_SHORT).show();
        }
    }
    private void saveProfileImageUrl(String userUid, String imageUrl) {
        SharedPreferences prefs = getSharedPreferences("my_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("profileImageUrl_user_" + userUid, imageUrl);
        editor.apply();
    }

    private void saveImageUri(Uri uri) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("imageUri", uri.toString());
        editor.apply();
    }

    private Uri loadImageUri() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String uriString = prefs.getString("imageUri", null);
        if (uriString != null) {
            return Uri.parse(uriString);
        } else {
            return null;
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

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickLauncher.launch(intent);
    }
    public void openMainActivity2() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }
    private void retrieveAndSetUserName() {
        // Get the current user's UID
        String currentUserUid = getCurrentUserUid();

        // Create a reference to the "user" node in the database
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("user").child(currentUserUid);

        // Retrieve the user's name from the database
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Retrieve user data from the snapshot
                    String userName = dataSnapshot.child("uname").getValue(String.class);
                    // Set the user's name to UsrNameTextView
                    name.setText(userName);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle the error
                Log.e("Firebase", "Error retrieving user name: " + databaseError.getMessage());
            }
        });
    }
    private void retrieveAndSetcontact() {
        // Get the current user's UID
        String currentUserUid = getCurrentUserUid();

        // Create a reference to the "user" node in the database
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("user").child(currentUserUid);

        // Retrieve the user's name from the database
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Retrieve user data from the snapshot
                    String userName = dataSnapshot.child("contact").getValue(String.class);
                    // Set the user's name to UsrNameTextView
                    contact.setText(userName);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle the error
                Log.e("Firebase", "Error retrieving contact: " + databaseError.getMessage());
            }
        });
    }
}