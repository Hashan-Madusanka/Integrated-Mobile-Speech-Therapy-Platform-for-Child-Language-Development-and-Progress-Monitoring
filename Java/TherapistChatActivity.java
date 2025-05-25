package com.example.speechtherapy;
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.agora.rtc2.Constants;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;

import com.example.speechtherapy.utils.ProgressDataManager;


public class TherapistChatActivity extends AppCompatActivity {
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private ArrayList<ChatMessage> chatMessageList;
    private EditText messageInput;
    private ImageView sendButton,voicecall,videocall;
    private String therapistId;
    private DatabaseReference chatRef;
    private StorageReference storageRef;
    private CardView shareReport;
    ImageView home;
    private String userId;
    // File picker launcher
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private static final int REQUEST_CALL_PHONE = 101;
    private static final int PERMISSION_REQUEST_CODE = 102;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.therapist_chat);
        // Initialize UI elements
        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        shareReport = findViewById(R.id.report_share);
        voicecall = findViewById(R.id.voicecall);
        videocall = findViewById(R.id.voideocall);
        chatMessageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessageList);
        home = findViewById(R.id.home);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
        // Retrieve the therapist ID from the intent
        therapistId = getIntent().getStringExtra("therapistId");
        // Get the current user ID from Firebase Authentication
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Reference to the chat node in Firebase
        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(therapistId).child(userId);
        storageRef = FirebaseStorage.getInstance().getReference("reports");
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homeActivity();
            }
        });
        // File picker initialization
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null) {
                            uploadPDF(fileUri);
                        }
                    }
                });
        // Load chat messages
        loadChatMessages();
        // Set up the send button click listener
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
        // Request necessary permissions
        requestPermissions();
        // Share report button click listener
        shareReport.setOnClickListener(v -> openFilePicker());
        // Initialize the Agora RTC Engine
//        initializeAgoraEngine();
        videocall.setOnClickListener(v -> {
            // Save voice call data to Firebase before starting the video call
            saveVideoCallData();
            startVideoCall();
        });
        // Fetch therapist phone number from Firebase
        DatabaseReference therapistRef = FirebaseDatabase.getInstance().getReference("therapist").child(therapistId);
        therapistRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String phoneNumber = snapshot.child("contact").getValue(String.class);
                if (phoneNumber != null) {
                    // Set up the voice call button listener
                    voicecall.setOnClickListener(v -> makeVoiceCall(phoneNumber));
                } else {
                    Toast.makeText(TherapistChatActivity.this, "Therapist phone number not available", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TherapistChatActivity.this, "Failed to fetch phone number", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(TherapistChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (!messageText.isEmpty()) {
            // Create a new chat message
            ChatMessage message = new ChatMessage(userId, messageText);
            chatRef.push().setValue(message).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    messageInput.setText(""); // Clear the input field
                    
                    // Record this message in progress tracking
                    ProgressDataManager.recordTherapistCommunication("message");
                } else {
                    Toast.makeText(TherapistChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "Enter a message", Toast.LENGTH_SHORT).show();
        }
    }
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(Intent.createChooser(intent, "Select PDF"));
    }

    private void uploadPDF(Uri fileUri) {
        String originalFileName = getFileName(fileUri); // Extract the original file name
        String fileName = System.currentTimeMillis() + ".pdf";
        StorageReference fileRef = storageRef.child(fileName);

        fileRef.putFile(fileUri).addOnSuccessListener(taskSnapshot ->
                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String fileLink = uri.toString();
                    sendFileLinkToChat(fileLink, originalFileName);
                })).addOnFailureListener(e ->
                Toast.makeText(TherapistChatActivity.this, "Failed to upload file", Toast.LENGTH_SHORT).show()
        );
    }
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
    private void sendFileLinkToChat(String fileLink, String originalFileName) {
        // Create a message with the file link
        String messageText = "📄 Report: " + originalFileName + "\n" + fileLink;
        ChatMessage message = new ChatMessage(userId, messageText);
        chatRef.push().setValue(message).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(TherapistChatActivity.this, "Report shared successfully", Toast.LENGTH_SHORT).show();
                
                // Record this file share in progress tracking
                ProgressDataManager.recordTherapistCommunication("file_share");
            } else {
                Toast.makeText(TherapistChatActivity.this, "Failed to share report", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void makeVoiceCall(String phoneNumber) {
        // Check if phone call permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PHONE);
        } else {
            // Record the voice call attempt in progress tracking
            ProgressDataManager.recordTherapistCommunication("voice_call");
            
            // Initiate the call
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
    private void startVideoCall() {
        // Start the VideoCallActivity and pass the therapist ID
        Intent intent = new Intent(this, VideoCallActivity.class);
        intent.putExtra("therapistId", therapistId);
        
        // Record the video call attempt in progress tracking
        ProgressDataManager.recordTherapistCommunication("video_call");
        
        startActivity(intent);
    }
    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_CODE);
        }
    }
    private void saveVideoCallData() {
        // Reference to the videoCall node in Firebase
        DatabaseReference videoCallRef = FirebaseDatabase.getInstance().getReference("videoCall");

        // Create a map with the call data
        Map<String, Object> videoCallData = new HashMap<>();
        videoCallData.put("therapistId", therapistId);
        videoCallData.put("userId", userId);
        videoCallData.put("timestamp", System.currentTimeMillis()); // Add timestamp if needed

        // Save data to the videoCall node, replacing any previous data
        videoCallRef.setValue(videoCallData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("VideoCall", "Video call data saved and replaced successfully");
            } else {
                Log.e("VideoCall", "Failed to save video call data", task.getException());
            }
        });
    }

    public void homeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

}
