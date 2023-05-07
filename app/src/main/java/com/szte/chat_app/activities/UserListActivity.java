package com.szte.chat_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.szte.chat_app.adapters.UsersAdapter;
import com.szte.chat_app.databinding.ActivityUserListBinding;
import com.szte.chat_app.listeners.UserListener;
import com.szte.chat_app.models.User;
import com.szte.chat_app.utils.Constants;
import com.szte.chat_app.utils.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class UserListActivity extends BaseActivity implements UserListener {
    private ActivityUserListBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        database = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        addListeners();
        getUsers();
    }

    private void addListeners() {
        binding.imageBack.setOnClickListener(e -> onBackPressed());
    }

    private void getUsers() {
        loading(true);
        database.collection(Constants.COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    String currentUserId = mAuth.getCurrentUser().getUid();

                    if (task.isSuccessful() && task.getResult() != null) {
                        List<User> users = new ArrayList<>();

                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            if (currentUserId.equals(queryDocumentSnapshot.getId())) {
                                continue;
                            }
                            User user = new User();
                            user.displayName = queryDocumentSnapshot.getString(Constants.DISPLAY_NAME);
                            user.email = queryDocumentSnapshot.getString(Constants.EMAIL);
                            user.image = queryDocumentSnapshot.getString(Constants.IMAGE);
                            user.token = queryDocumentSnapshot.getString(Constants.FCM_TOKEN);
                            user.id = queryDocumentSnapshot.getId();
                            users.add(user);
                        }

                        if (users.size() > 0) {
                            UsersAdapter adapter = new UsersAdapter(users, this);
                            binding.usersRecyclerView.setAdapter(adapter);
                            binding.usersRecyclerView.setVisibility(View.VISIBLE);
                        } else {
                            showErrorMessage();
                        }
                    } else {
                        showErrorMessage();
                    }
                });
    }

    private void showErrorMessage() {
        binding.errorMessageText.setText(String.format("%s", "No user available"));
        binding.errorMessageText.setVisibility(View.VISIBLE);
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUserClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.USER, user);
        startActivity(intent);
        finish();
    }
}