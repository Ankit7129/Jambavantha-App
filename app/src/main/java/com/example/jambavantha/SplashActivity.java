package com.example.jambavantha;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            // User is logged in, navigate to MainActivity
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        } else {
            // User is not logged in, navigate to WelcomeActivity
            startActivity(new Intent(SplashActivity.this, LanguageSelectionActivity.class));
        }
        finish();
    }
}
