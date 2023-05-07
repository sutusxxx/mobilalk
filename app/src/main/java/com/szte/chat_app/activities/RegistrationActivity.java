package com.szte.chat_app.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.szte.chat_app.databinding.ActivityRegistrationBinding;
import com.szte.chat_app.utils.Constants;
import com.szte.chat_app.utils.PreferenceManager;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

public class RegistrationActivity extends AppCompatActivity {
    private ActivityRegistrationBinding binding;
    private String encodedImage;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mDatabase;

    private PreferenceManager mPreferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseFirestore.getInstance();
        mPreferenceManager = new PreferenceManager(getApplicationContext());
        addListeners();
    }

    private void addListeners() {
        binding.loginText.setOnClickListener(e -> onBackPressed());
        binding.registerButton.setOnClickListener(e -> {
            if (this.isValidDetails()) {
                register();
            }
        });
        binding.layoutImage.setOnClickListener(e -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    private void showMessage(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void register() {
        loading(true);
        String email = binding.emailText.getText().toString();
        String password = binding.passwordText.getText().toString();
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(task -> {
                    HashMap<String, Object>  user = new HashMap<>();
                    user.put(Constants.USER_ID, task.getUser().getUid());
                    user.put(Constants.DISPLAY_NAME, binding.nameText.getText().toString());
                    user.put(Constants.EMAIL, binding.emailText.getText().toString());
                    user.put(Constants.IMAGE, encodedImage);
                   mDatabase.collection(Constants.COLLECTION_USERS).document(task.getUser().getUid())
                           .set(user)
                           .addOnSuccessListener(documentReference -> {
                                loading(false);
                                mPreferenceManager.putBoolean(Constants.IS_SIGNED_IN, true);
                                mPreferenceManager.putString(Constants.USER_ID, task.getUser().getUid());
                                mPreferenceManager.putString(Constants.DISPLAY_NAME, binding.nameText.getText().toString());
                                mPreferenceManager.putString(Constants.IMAGE, encodedImage);
                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                           })
                           .addOnFailureListener(exception -> {
                                loading(false);
                                showMessage(exception.getMessage());
                           });
                })
                .addOnFailureListener(exception -> {
                    loading(false);
                    showMessage(exception.getMessage());
                });
    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 0, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();

                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            binding.addImageText.setVisibility(View.GONE);
                            encodedImage = this.encodeImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.registerButton.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.registerButton.setVisibility(View.VISIBLE);
        }
    }

    private Boolean isValidDetails() {
        if (encodedImage == null) {
            showMessage("Select profile image");
            return false;
        }
        if (binding.nameText.getText().toString().trim().isEmpty()) {
            showMessage("Enter name");
            return false;
        }
        if (binding.emailText.getText().toString().trim().isEmpty()) {
            showMessage("Enter email address");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(binding.emailText.getText().toString()).matches()) {
            showMessage("Enter valid email address");
            return false;
        }
        if (binding.passwordText.getText().toString().trim().isEmpty()) {
            showMessage("Enter password");
            return false;
        }
        if (binding.confirmPasswordText.getText().toString().trim().isEmpty()) {
            showMessage("Enter confirm password");
            return false;
        }
        if (!binding.passwordText.getText().toString().equals(binding.confirmPasswordText.getText().toString())) {
            showMessage("Password and confirm password are not the same");
            return false;
        }
        return true;
    }
}