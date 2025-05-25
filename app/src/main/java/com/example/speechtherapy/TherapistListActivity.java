package com.example.speechtherapy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

public class TherapistListActivity extends AppCompatActivity implements TherapistAdapter.OnTherapistClickListener {
    ImageView home;
    private RecyclerView recyclerView;
    private TherapistAdapter adapter;
    private ArrayList<Therapist> therapistList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.therapist_list);
        home = findViewById(R.id.home);
        recyclerView = findViewById(R.id.therapist_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Initialize therapist list and adapter
        therapistList = new ArrayList<>();
        adapter = new TherapistAdapter(therapistList, this); // Pass the current activity as the listener
        recyclerView.setAdapter(adapter);
        // Load therapists from Firebase
        loadTherapists();
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openHomeActivity();
            }
        });
    }
    public void openHomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }
    private void loadTherapists() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("therapist");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                therapistList.clear();  // Clear the existing list to avoid duplicates
                Log.d("TherapistListActivity", "Data snapshot received. Children count: " + snapshot.getChildrenCount());
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Therapist therapist = dataSnapshot.getValue(Therapist.class);
                    if (therapist != null) {
                        therapistList.add(therapist); // Add therapist to the list
                        Log.d("TherapistListActivity", "Therapist added: " + therapist.getUsername() + " (" + therapist.getEmail() + ")");
                    } else {
                        Log.w("TherapistListActivity", "Received a null therapist object.");
                    }
                }
                // Update the adapter directly after data retrieval
                adapter.notifyDataSetChanged(); // Notify the adapter that data has changed
                Log.d("TherapistListActivity", "Therapist list updated. Total therapists: " + therapistList.size());
            }@Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(TherapistListActivity.this, "Failed to load therapists", Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public void onTherapistClick(Therapist therapist) {
        Intent intent = new Intent(this, TherapistChatActivity.class);
        intent.putExtra("therapistId", therapist.getId());
        startActivity(intent);
    }
}