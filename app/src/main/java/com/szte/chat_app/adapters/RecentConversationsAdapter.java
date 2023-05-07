package com.szte.chat_app.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.szte.chat_app.databinding.UserContainerRecentConversationBinding;
import com.szte.chat_app.listeners.ConservationListener;
import com.szte.chat_app.models.ChatMessage;
import com.szte.chat_app.models.User;

import java.util.List;

public class RecentConversationsAdapter extends RecyclerView.Adapter<RecentConversationsAdapter.ConversationViewHolder> {
    private final List<ChatMessage> chatMessages;
    private final ConservationListener listener;

    public RecentConversationsAdapter(List<ChatMessage> chatMessages, ConservationListener listener) {
        this.chatMessages = chatMessages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversationViewHolder(
                UserContainerRecentConversationBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        holder.setData(chatMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder {
        UserContainerRecentConversationBinding binding;

        ConversationViewHolder(UserContainerRecentConversationBinding userContainerRecentConversationBinding) {
            super(userContainerRecentConversationBinding.getRoot());
            binding = userContainerRecentConversationBinding;

        }

        void setData(ChatMessage chatMessage) {
            binding.imageProfile.setImageBitmap(getConversationImage(chatMessage.conversationImage));
            binding.nameText.setText(chatMessage.conversationName);
            binding.textRecentMessage.setText(chatMessage.message);
            binding.getRoot().setOnClickListener(v -> {
                User user = new User();
                user.id = chatMessage.conversationId;
                user.displayName = chatMessage.conversationName;
                user.image = chatMessage.conversationImage;
                listener.onConservationClicked(user);
            });
        }
    }

    private Bitmap getConversationImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
