package com.example.speechtherapy;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private ArrayList<ChatMessage> chatMessages;


    public ChatAdapter(ArrayList<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
    }
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);
        // Assuming ChatMessage has a method getSenderId()
        String senderId = message.getSenderId();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Customize CardView based on sender
        if (senderId.equals(currentUserId)) {
            // Message from the current user
            holder.chatCard.setCardBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.green));
            // Align to the right
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) holder.chatCard.getLayoutParams();
            params.horizontalBias = 1.0f; // Align right
            holder.chatCard.setLayoutParams(params);
        } else {
            // Message from another user
            holder.chatCard.setCardBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.blue));
            // Align to the left
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) holder.chatCard.getLayoutParams();
            params.horizontalBias = 0.0f; // Align left
            holder.chatCard.setLayoutParams(params);
        }
        String messageText = message.getMessage();
        holder.messageTextView.setText(messageText);
        holder.messageTextView.setText(message.getMessage());
        // If the message contains a file link
        if (message.getFileLink() != null) {
            // Show the original file name as a clickable link
            holder.messageTextView.setText(message.getOriginalFileName());
            holder.messageTextView.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(message.getFileLink()));
                v.getContext().startActivity(intent);

            });
        } else {
            holder.messageTextView.setText(message.getMessage());
        }
    }
    @Override
    public int getItemCount() {
        return chatMessages.size();
    }
    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        CardView chatCard;
        public ChatViewHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.user_name);
            chatCard = itemView.findViewById(R.id.chat_card);
        }
    }
}
