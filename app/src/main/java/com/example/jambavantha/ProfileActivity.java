package com.example.jambavantha;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    private TextView nameTextView;
    private TextView emailTextView;
    private TextView phoneNumberTextView;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI Elements
        nameTextView = findViewById(R.id.name_text_view);
        emailTextView = findViewById(R.id.email_text_view);
        phoneNumberTextView = findViewById(R.id.phone_number_text_view);

        // Get current user
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Set user details to TextViews
            nameTextView.setText("\uD83D\uDC64 Name:  " + user.getDisplayName());
            emailTextView.setText("✉\uFE0F Email: " + user.getEmail());
            phoneNumberTextView.setText("\uD83D\uDCDE Phone: " + (user.getPhoneNumber() != null ? user.getPhoneNumber() : "N/A"));
        }
    }
}
