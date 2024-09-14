package com.example.jambavantha;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    TextView welcomeText, subText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        welcomeText = findViewById(R.id.welcome_text);
        subText = findViewById(R.id.subtext);

        // Load the preferred language
        SharedPreferences preferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        String language = preferences.getString("Language", "en");

        // Update UI text based on the selected language
        if (language.equals("hi")) {
            welcomeText.setText("जम्बवान्था में आपका स्वागत है");
            subText.setText("नमस्ते, शुरू करते हैं!");
        } else if (language.equals("bn")) {
            welcomeText.setText("জম্ববান্থায় স্বাগতম");
            subText.setText("নমস্কার, চলুন শুরু করি!");
        } else if (language.equals("ta")) {
            welcomeText.setText("ஜம்பவந்தா வரவேற்கிறது");
            subText.setText("வணக்கம், தொடரலாம்!");
        } else {
            welcomeText.setText("Welcome to JAMBAVANTHA");
            subText.setText("Hello, let’s get started!");
        }
    }

    public void goToLogin(View view) {
        // Redirect to Login page
        startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
    }
}
