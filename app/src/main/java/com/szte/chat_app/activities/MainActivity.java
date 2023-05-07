package com.szte.chat_app.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.szte.chat_app.adapters.RecentConversationsAdapter;
import com.szte.chat_app.databinding.ActivityMainBinding;
import com.szte.chat_app.listeners.ConservationListener;
import com.szte.chat_app.models.ChatMessage;
import com.szte.chat_app.models.User;
import com.szte.chat_app.utils.Constants;
import com.szte.chat_app.utils.PreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends BaseActivity implements ConservationListener {
    private PreferenceManager preferenceManager;
    private ActivityMainBinding binding;
    private List<ChatMessage> conversations;
    private RecentConversationsAdapter adapter;

    private FirebaseAuth mAuth;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        mAuth = FirebaseAuth.getInstance();
        conversations = new ArrayList<>();
        adapter = new RecentConversationsAdapter(conversations, this);
        binding.conversationsRecyclerView.setAdapter(adapter);
        database = FirebaseFirestore.getInstance();
        loadUserDetails();
        getToken();
        addListeners();
        listenConversations();
    }

    private void addListeners() {
        binding.imageLogout.setOnClickListener(e -> logout());
        binding.fabNewChat.setOnClickListener(e -> startActivity(new Intent(getApplicationContext(), UserListActivity.class)));
    }

    private void listenConversations() {
        database.collection(Constants.COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.SENDER_ID, preferenceManager.getString(Constants.USER_ID))
                .addSnapshotListener(eventListener);
        database.collection(Constants.COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.RECEIVER_ID, preferenceManager.getString(Constants.USER_ID))
                .addSnapshotListener(eventListener);
    }

    @SuppressLint("NotifyDataSetChanged")
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) return;
        if (value != null) {
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    String senderId = documentChange.getDocument().getString(Constants.SENDER_ID);
                    String receiverId = documentChange.getDocument().getString(Constants.RECEIVER_ID);
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = senderId;
                    chatMessage.receiverId = receiverId;

                    if (preferenceManager.getString(Constants.USER_ID).equals(senderId)) {
                        chatMessage.conversationImage = documentChange.getDocument().getString(Constants.RECEIVER_IMAGE);
                        chatMessage.conversationName = documentChange.getDocument().getString(Constants.RECEIVER_NAME);
                        chatMessage.conversationId = documentChange.getDocument().getString(Constants.RECEIVER_ID);
                    } else {
                        chatMessage.conversationImage = documentChange.getDocument().getString(Constants.SENDER_IMAGE);
                        chatMessage.conversationName = documentChange.getDocument().getString(Constants.SENDER_NAME);
                        chatMessage.conversationId = documentChange.getDocument().getString(Constants.USER_ID);
                    }
                    chatMessage.message = documentChange.getDocument().getString(Constants.LAST_MESSAGE);
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.TIMESTAMP);
                    conversations.add(chatMessage);
                } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    for (int i = 0; i < conversations.size(); i++) {
                        String senderId = documentChange.getDocument().getString(Constants.SENDER_ID);
                        String receiverId = documentChange.getDocument().getString(Constants.RECEIVER_ID);

                        if (conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId)) {
                            conversations.get(i).message = documentChange.getDocument().getString(Constants.LAST_MESSAGE);
                            conversations.get(i).dateObject = documentChange.getDocument().getDate(Constants.TIMESTAMP);
                        }
                    }
                }
            }
            conversations.sort((obj1, obj2) -> obj2.dateObject.compareTo(obj1.dateObject));
            adapter.notifyDataSetChanged();
            binding.conversationsRecyclerView.smoothScrollToPosition(0);
            binding.conversationsRecyclerView.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
        }
    };

    private void loadUserDetails() {
        binding.nameText.setText(preferenceManager.getString(Constants.DISPLAY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }

    private void showMessage(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void getToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token) {
        DocumentReference documentReference = database.collection(Constants.COLLECTION_USERS).document(
                preferenceManager.getString(Constants.USER_ID)
        );
        documentReference.update(Constants.FCM_TOKEN, token)
                .addOnFailureListener(e -> showMessage("Unable to update token"));
    }

    private void logout() {
        showMessage("Signing out...");
        DocumentReference documentReference = database.collection(Constants.COLLECTION_USERS).document(
                preferenceManager.getString(Constants.USER_ID)
        );
        Map<String, Object> updates = new HashMap<>();
        updates.put(Constants.FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                })
                .addOnFailureListener(e -> showMessage("Unable to sign out!"));
        mAuth.signOut();
    }

    @Override
    public void onConservationClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.USER, user);
        startActivity(intent);
    }
}