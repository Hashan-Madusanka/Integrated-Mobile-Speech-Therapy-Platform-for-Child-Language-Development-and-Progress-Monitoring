package com.example.speechtherapy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

public class UserChatActivity extends AppCompatActivity {
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private ArrayList<ChatMessage> chatMessageList;
    private EditText messageInput;
    private ImageView sendButton,voicecall;
    private String userId;
    private String therapistId;
    private DatabaseReference chatRef;
    private static final int REQUEST_CALL_PHONE = 101;
    private static final int PERMISSION_REQUEST_CODE = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_chat);
        // Initialize UI elements
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        chatMessageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessageList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
        voicecall = findViewById(R.id.voicecall);

        // Retrieve the therapist ID from the intent
        userId = getIntent().getStringExtra("userId");
        // Get the current user ID from Firebase Authentication
        therapistId = getIntent().getStringExtra("therapistId");
        // Reference to the chat node in Firebase
        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(therapistId).child(userId);
        // Load chat messages
        // Request necessary permissions
        requestPermissions();
        loadChatMessages();

        // Set up the send button click listener
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
        // Fetch therapist phone number from Firebase
        DatabaseReference kidRef = FirebaseDatabase.getInstance().getReference("kid").child(userId);
        kidRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String phoneNumber = snapshot.child("contact").getValue(String.class);
                if (phoneNumber != null) {
                    // Set up the voice call button listener
                    voicecall.setOnClickListener(v -> makeVoiceCall(phoneNumber));
                } else {
                    Toast.makeText(UserChatActivity.this, "Therapist phone number not available", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserChatActivity.this, "Failed to fetch phone number", Toast.LENGTH_SHORT).show();
            }
        });

    }
    private void loadChatMessages() {
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatMessageList.clear(); // Clear the existing messages
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    ChatMessage message = dataSnapshot.getValue(ChatMessage.class);
                    if (message != null) {
                        chatMessageList.add(message);
                    }
                }
                chatAdapter.notifyDataSetChanged(); // Update the chat display
            }@Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(UserChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (!messageText.isEmpty()) {
            // Create a new chat message
            ChatMessage message = new ChatMessage(therapistId, messageText);
            chatRef.push().setValue(message).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    messageInput.setText(""); // Clear the input field
                } else {
                    Toast.makeText(UserChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Enter a message", Toast.LENGTH_SHORT).show();
        }
    }
    private void makeVoiceCall(String phoneNumber) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PHONE);
        } else {
            // Permission already granted, proceed with the call
            initiateCall(phoneNumber);
        }
    }

    private void initiateCall(String phoneNumber) {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + phoneNumber));
        try {
            startActivity(callIntent);
        } catch (SecurityException e) {
            Toast.makeText(this, "Failed to make the call. Permission denied.", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CALL_PHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted for phone calls
                Toast.makeText(this, "Permission granted. Try making the call again.", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied for phone calls
                Toast.makeText(this, "Permission denied. Cannot make calls.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted for video call
                Toast.makeText(this, "Permissions granted. You can now use the video call feature.", Toast.LENGTH_SHORT).show();
            } else {
                // Permissions denied for video call
                Toast.makeText(this, "Permissions denied. Video call feature will not work.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_CODE);
        }
    }
}