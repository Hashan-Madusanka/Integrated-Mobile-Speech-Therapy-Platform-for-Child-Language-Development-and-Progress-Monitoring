package com.example.speechtherapy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
public class UserChatListAdapter extends RecyclerView.Adapter<UserChatListAdapter.ViewHolder> {
    private List<UserChat> userChatList;
    private OnUserChatClickListener listener;
    public interface OnUserChatClickListener {
        void onUserChatClick(UserChat userChat);
    }
    public UserChatListAdapter(List<UserChat> userChatList, OnUserChatClickListener listener) {
        this.userChatList = userChatList;
        this.listener = listener;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_chat, parent, false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserChat userChat = userChatList.get(position);
        holder.userNameText.setText(userChat.getUsername());
        holder.userEmailText.setText(userChat.getEmail());
        holder.itemView.setOnClickListener(v -> listener.onUserChatClick(userChat));
    }
    @Override
    public int getItemCount() {
        return userChatList.size();
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView userIdText;
        TextView userNameText;
        TextView userEmailText;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userIdText = itemView.findViewById(R.id.user_name);
            userNameText = itemView.findViewById(R.id.user_name);
            userEmailText = itemView.findViewById(R.id.user_email);
        }
    }
}
