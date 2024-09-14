package com.example.jambavantha;import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class LanguageSelectionActivity extends AppCompatActivity {

    Button englishButton, hindiButton, bengaliButton, tamilButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_selection);

        englishButton = findViewById(R.id.button_english);
        hindiButton = findViewById(R.id.button_hindi);
        bengaliButton = findViewById(R.id.button_bengali);
        tamilButton = findViewById(R.id.button_tamil);

        // Setting up onClickListeners for each language button
        englishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLanguage("en");
            }
        });

        hindiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLanguage("hi");
            }
        });

        bengaliButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLanguage("bn");
            }
        });

        tamilButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLanguage("ta");
            }
        });
    }

    private void setLanguage(String langCode) {
        // Store the language preference in SharedPreferences
        SharedPreferences preferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("Language", langCode);
        editor.apply();

        // Redirect to Welcome page after setting the language
        Intent intent = new Intent(LanguageSelectionActivity.this, WelcomeActivity.class);
        startActivity(intent);
        finish();
    }
}
