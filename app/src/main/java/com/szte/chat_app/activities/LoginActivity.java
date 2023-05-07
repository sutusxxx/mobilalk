package com.szte.chat_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.szte.chat_app.databinding.ActivityLoginBinding;
import com.szte.chat_app.utils.Constants;
import com.szte.chat_app.utils.PreferenceManager;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private PreferenceManager preferenceManager;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        addListeners();
    }

    private void addListeners() {
        binding.registretionText.setOnClickListener(e ->
                startActivity(new Intent(getApplicationContext(), RegistrationActivity.class))
        );
        binding.loginButton.setOnClickListener(l -> {
            if (isValidDetails()) {
                login();
            }
        });
    }

    private void login() {
        loading(true);
        String email = binding.emailText.getText().toString();
        String password = binding.passwordText.getText().toString();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    mDatabase.collection(Constants.COLLECTION_USERS).document(uid).get()
                            .addOnSuccessListener(documentReference -> {
                                preferenceManager.putBoolean(Constants.IS_SIGNED_IN, true);
                                preferenceManager.putString(Constants.USER_ID, documentReference.getString(Constants.USER_ID));
                                preferenceManager.putString(Constants.DISPLAY_NAME, documentReference.getString(Constants.DISPLAY_NAME));
                                preferenceManager.putString(Constants.IMAGE, documentReference.getString(Constants.IMAGE));
                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            });
                })
                .addOnFailureListener(exception -> {
                    loading(false);
                    showMessage("Login failed.");
                });
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.loginButton.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.loginButton.setVisibility(View.VISIBLE);
        }
    }

    private void showMessage(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private Boolean isValidDetails() {
        if (binding.emailText.getText().toString().trim().isEmpty()) {
            showMessage("Enter email address");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(binding.emailText.getText().toString()).matches()) {
            showMessage("Email format is not valid");
            return false;
        }
        if (binding.passwordText.getText().toString().isEmpty()) {
            showMessage("Enter password");
            return false;
        }
        return true;
    }
}