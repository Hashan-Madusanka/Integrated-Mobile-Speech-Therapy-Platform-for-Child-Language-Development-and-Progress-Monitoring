package com.example.speechtherapy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
public class UserChatListActivity extends AppCompatActivity {
    ImageView logout,videocall;
    private RecyclerView recyclerView;
    private UserChatListAdapter adapter;
    private ArrayList<UserChat> userChatList;
    private DatabaseReference chatListRef;
    private String therapistId;
    TextView videocalltext;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_chat_list);
        logout = findViewById(R.id.logout);
        videocall = findViewById(R.id.voideocall);
        videocalltext = findViewById(R.id.videocalltext);
        progressBar = findViewById(R.id.progressBar);

        // Initially hide the videocall ImageView
        videocall.setVisibility(View.GONE);
        videocalltext.setVisibility(View.GONE);
        // Initially hide ProgressBar
        progressBar.setVisibility(View.GONE);
        // Get the current user's ID
        therapistId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        MediaPlayer mediaPlayer = MediaPlayer.create(UserChatListActivity.this, R.raw.ring); // Ensure calling_tone.mp3
        // Reference to the "videoCall" node in Firebase
        DatabaseReference videoCallRef = FirebaseDatabase.getInstance().getReference("videoCall");
        DatabaseReference kidRef = FirebaseDatabase.getInstance().getReference("kid");

        // Check if the therapistId in Firebase matches the current user's ID
        videoCallRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String firebaseTherapistId = snapshot.child("therapistId").getValue(String.class);
                    String userId = snapshot.child("userId").getValue(String.class);
                    if (firebaseTherapistId != null && firebaseTherapistId.equals(therapistId)) {
                        if (mediaPlayer != null) {
                            mediaPlayer.start(); // Start playing the MP3
                        }
                        // Fetch user details from "kid"
                        kidRef.orderByChild("id").equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot kidSnapshot) {
                                if (kidSnapshot.exists()) {
                                    for (DataSnapshot child : kidSnapshot.getChildren()) {
                                        String uname = child.child("uname").getValue(String.class);
                                        if (uname != null) {
                                            videocalltext.setText("Calling " + uname);
                                            videocall.setVisibility(View.VISIBLE); // Show videocall button
                                            videocalltext.setVisibility(View.VISIBLE); // Show text
                                            break;
                                        }
                                    }
                                } else {
                                    videocalltext.setText(""); // Clear text
                                    videocall.setVisibility(View.GONE); // Hide videocall button
                                    videocalltext.setVisibility(View.GONE); // Hide text
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(UserChatListActivity.this, "Failed to fetch user data", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        videocall.setVisibility(View.GONE); // Hide videocall button
                        videocalltext.setText(""); // Clear text
                        videocalltext.setVisibility(View.GONE); // Hide text
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserChatListActivity.this, "Failed to fetch video call data", Toast.LENGTH_SHORT).show();
            }
        });

        videocall.setOnClickListener(v -> {
            // Stop the MediaPlayer if it's playing
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }

            // Start the video call activity
            startVideoCall();
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Show confirmation dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(UserChatListActivity.this);
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
        recyclerView = findViewById(R.id.user_chat_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userChatList = new ArrayList<>();
        adapter = new UserChatListAdapter(userChatList, this::openChat);
        recyclerView.setAdapter(adapter);
        therapistId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatListRef = FirebaseDatabase.getInstance().getReference("chats").child(therapistId);
        loadChatList();
    }
    public void openloginActivity() {
        Intent intent = new Intent(this,LoginActivity.class);
        startActivity(intent);
    }
    private void loadChatList() {
        chatListRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userChatList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String userId = dataSnapshot.getKey();
                    // Fetch user details from the "kid" node
                    FirebaseDatabase.getInstance().getReference("kid").child(userId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                    String username = userSnapshot.child("uname").getValue(String.class);
                                    String email = userSnapshot.child("email").getValue(String.class);
                                    userChatList.add(new UserChat(userId, username, email));
                                    adapter.notifyDataSetChanged();
                                }@Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Toast.makeText(UserChatListActivity.this, "Failed to load user details", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }@Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserChatListActivity.this, "Failed to load chat list", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void openChat(UserChat userChat) {
        Intent intent = new Intent(this, UserChatActivity.class);
        intent.putExtra("therapistId", therapistId);
        intent.putExtra("userId", userChat.getUserId());
        startActivity(intent);
    }
    private void startVideoCall() {
        Intent intent = new Intent(UserChatListActivity.this, VideoCallActivity.class);
        startActivity(intent);
    }
}