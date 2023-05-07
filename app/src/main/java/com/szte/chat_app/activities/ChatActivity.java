package com.szte.chat_app.activities;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.szte.chat_app.adapters.ChatAdapter;
import com.szte.chat_app.databinding.ActivityChatBinding;
import com.szte.chat_app.models.ChatMessage;
import com.szte.chat_app.models.User;
import com.szte.chat_app.utils.Constants;
import com.szte.chat_app.utils.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ChatActivity extends BaseActivity {
    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter adapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String conversationId = null;
    private Boolean isReceiverAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        addListeners();
        loadReceiverDetails();
        init();
        listenMessages();
    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        adapter = new ChatAdapter(
                chatMessages,
                getBitmapFromEncodedString(receiverUser.image),
                preferenceManager.getString(Constants.USER_ID)
        );
        binding.chatRecyclerView.setAdapter(adapter);
        database = FirebaseFirestore.getInstance();
    }

    private void sendMessage() {
        Map<String, Object> message = new HashMap<>();
        message.put(Constants.SENDER_ID, preferenceManager.getString(Constants.USER_ID));
        message.put(Constants.RECEIVER_ID, receiverUser.id);
        message.put(Constants.MESSAGE, binding.inputMessage.getText().toString());
        message.put(Constants.TIMESTAMP, new Date());
        database.collection(Constants.COLLECTION_CHAT).add(message);

        if (conversationId != null) {
            updateConversation(binding.inputMessage.getText().toString());
        } else {
            Map<String, Object> conversation = new HashMap<>();
            conversation.put(Constants.SENDER_ID, preferenceManager.getString(Constants.USER_ID));
            conversation.put(Constants.SENDER_NAME, preferenceManager.getString(Constants.DISPLAY_NAME));
            conversation.put(Constants.SENDER_IMAGE, preferenceManager.getString(Constants.IMAGE));
            conversation.put(Constants.RECEIVER_ID, receiverUser.id);
            conversation.put(Constants.RECEIVER_NAME, receiverUser.displayName);
            conversation.put(Constants.RECEIVER_IMAGE, receiverUser.image);
            conversation.put(Constants.LAST_MESSAGE, binding.inputMessage.getText().toString());
            conversation.put(Constants.TIMESTAMP, new Date());
            addConversation(conversation);
        }
        binding.inputMessage.setText(null);
    }

    private void listenAvailabilityOfReceiver() {
        database.collection(Constants.COLLECTION_USERS).document(
                receiverUser.id
        ).addSnapshotListener(ChatActivity.this, (value, error) -> {
            if (error != null) return;
            if (value != null) {
                if (value.getLong(Constants.AVAILABILITY) != null) {
                    int availability = Objects.requireNonNull(value.getLong(Constants.AVAILABILITY)).intValue();
                    isReceiverAvailable = availability == 1;
                }
            }
            if (isReceiverAvailable) {
                binding.textAvailability.setVisibility(View.VISIBLE);
            } else {
                binding.textAvailability.setVisibility(View.GONE);
            }
        });
    }

    private void listenMessages() {
        database.collection(Constants.COLLECTION_CHAT)
                .whereEqualTo(Constants.SENDER_ID, preferenceManager.getString(Constants.USER_ID))
                .whereEqualTo(Constants.RECEIVER_ID, receiverUser.id)
                .addSnapshotListener(eventListener);
        database.collection(Constants.COLLECTION_CHAT)
                .whereEqualTo(Constants.SENDER_ID, receiverUser.id)
                .whereEqualTo(Constants.RECEIVER_ID, preferenceManager.getString(Constants.USER_ID))
                .addSnapshotListener(eventListener);
    }

    @SuppressLint("NotifyDataSetChanged")
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) return;

        if (value != null) {
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.MESSAGE);
                    chatMessage.date = getReadableDateTime(documentChange.getDocument().getDate(Constants.TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }
            chatMessages.sort((obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if (count == 0) {
                adapter.notifyDataSetChanged();
            } else {
                adapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);

        if (conversationId == null) {
            checkForConversation();
        }
    };

    private Bitmap getBitmapFromEncodedString(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private void loadReceiverDetails() {
        receiverUser = (User) getIntent().getSerializableExtra(Constants.USER);
        binding.textName.setText(receiverUser.displayName);
    }

    private void addListeners() {
        binding.imageBack.setOnClickListener(e -> onBackPressed());
        binding.layoutSend.setOnClickListener(e -> sendMessage());
    }

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("yyyy/MMMM/dd - hh:mm a", Locale.getDefault()).format(date);
    }

    private void checkForConversation() {
        if (chatMessages.size() != 0) {
            checkForConversationRemotely(preferenceManager.getString(Constants.USER_ID), receiverUser.id);
            checkForConversationRemotely(receiverUser.id, preferenceManager.getString(Constants.USER_ID));
        }
    }

    private void addConversation(Map<String, Object> conversation) {
        database.collection(Constants.COLLECTION_CONVERSATIONS)
                .add(conversation)
                .addOnSuccessListener(documentReference -> conversationId = documentReference.getId());
    }

    private void updateConversation(String message) {
        DocumentReference documentReference = database.collection(Constants.COLLECTION_CONVERSATIONS).document(conversationId);
        documentReference.update(
                Constants.LAST_MESSAGE, message,
                Constants.TIMESTAMP, new Date()
        );
    }

    private void checkForConversationRemotely(String senderId, String receiverId) {
        database.collection(Constants.COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.SENDER_ID, senderId)
                .whereEqualTo(Constants.RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversationOnCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversationOnCompleteListener = task -> {
        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversationId = documentSnapshot.getId();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }
}