package com.example.speechtherapy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
public class TherapistAdapter extends RecyclerView.Adapter<TherapistAdapter.TherapistViewHolder> {
    private ArrayList<Therapist> therapists;
    private OnTherapistClickListener listener;
    public TherapistAdapter(ArrayList<Therapist> therapists, OnTherapistClickListener listener) {
        this.therapists = therapists;
        this.listener = listener;
    }
    @NonNull
    @Override
    public TherapistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_therapist, parent, false);
        return new TherapistViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull TherapistViewHolder holder, int position) {
        Therapist therapist = therapists.get(position);
        holder.usernameTextView.setText(therapist.getUsername());
        holder.emailTextView.setText(therapist.getEmail());
        holder.itemView.setOnClickListener(v -> listener.onTherapistClick(therapist));
    }
    @Override
    public int getItemCount() {
        return therapists.size();
    }
    public static class TherapistViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView;
        TextView emailTextView;
        public TherapistViewHolder(View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.user_name);
            emailTextView = itemView.findViewById(R.id.user_email);
        }
    }
    public interface OnTherapistClickListener {
        void onTherapistClick(Therapist therapist);
    }
}
