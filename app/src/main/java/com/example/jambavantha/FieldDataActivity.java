package com.example.jambavantha;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;


public class FieldDataActivity extends AppCompatActivity {

    private static final String TAG = "FieldDataActivity";
    private static final String ESP_IP = "http://192.168.84.99"; // Replace with your ESP device IP
    private Handler handler = new Handler();

    private TextView soilMoistureTextView;
    private TextView receivedSoilMoistureTextView;
    private TextView motorStatusTextView;
    private TextView lowerThresholdTextView;
    private TextView upperThresholdTextView;
    private TextView modeTextView;
    private TextView remainingTimeTextView;
    private Runnable fetchTask;

    private boolean isManualMode = false; // Flag to track manual mode

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field_data);

        soilMoistureTextView = findViewById(R.id.soilMoistureTextView);
        receivedSoilMoistureTextView = findViewById(R.id.receivedSoilMoistureTextView);
        motorStatusTextView = findViewById(R.id.motorStatusTextView);
        lowerThresholdTextView = findViewById(R.id.lowerThresholdTextView);
        upperThresholdTextView = findViewById(R.id.upperThresholdTextView);
        modeTextView = findViewById(R.id.modeTextView);
        remainingTimeTextView = findViewById(R.id.remainingTimeTextView);

        EditText timerInput = findViewById(R.id.timerInput);

        Button switchToManualButton = findViewById(R.id.switchToManualButton);
        Button switchToAutoButton = findViewById(R.id.switchToAutoButton);
        Button turnPumpOnButton = findViewById(R.id.turnPumpOnButton);
        Button turnPumpOffButton = findViewById(R.id.turnPumpOffButton);
        Button setTimerButton = findViewById(R.id.setTimerButton);

        // Disable pump control and timer buttons by default
        turnPumpOnButton.setEnabled(false);
        turnPumpOffButton.setEnabled(false);
        setTimerButton.setEnabled(false);

        switchToManualButton.setOnClickListener(v -> {
            isManualMode = true;
            turnPumpOnButton.setEnabled(true);
            turnPumpOffButton.setEnabled(true);
            setTimerButton.setEnabled(true);
            controlPump("manual");
        });

        switchToAutoButton.setOnClickListener(v -> {
            isManualMode = false;
            turnPumpOnButton.setEnabled(false);
            turnPumpOffButton.setEnabled(false);
            setTimerButton.setEnabled(false);
            controlPump("auto");
        });

        turnPumpOnButton.setOnClickListener(v -> {
            if (isManualMode) {
                controlPump("on");
            } else {
                showManualModeToast();
            }
        });

        turnPumpOffButton.setOnClickListener(v -> {
            if (isManualMode) {
                controlPump("off");
            } else {
                showManualModeToast();
            }
        });

        setTimerButton.setOnClickListener(v -> {
            if (isManualMode) {
                setTimer(timerInput.getText().toString());
            } else {
                showManualModeToast();
            }
        });

        // Start fetching data from the ESP device at intervals
        fetchTask = new Runnable() {
            @Override
            public void run() {
                fetchFieldData();
                handler.postDelayed(this, 500); // Fetch data every 5 seconds
            }
        };
        handler.post(fetchTask);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(fetchTask); // Stop the repeated task when the activity is destroyed
    }

    private void fetchFieldData() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                URL url = new URL(ESP_IP + "/status");
                Log.d(TAG, "Fetching data from: " + url);

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000); // Connection timeout (5 sec)
                connection.setReadTimeout(5000);    // Read timeout (5 sec)

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    Log.d(TAG, "Response: " + response.toString());

                    runOnUiThread(() -> updateFieldDataUI(response.toString()));
                } else {
                    String errorMsg = "Failed to fetch field data. Response Code: " + responseCode;
                    Log.e(TAG, errorMsg);
                    runOnUiThread(() -> Toast.makeText(FieldDataActivity.this, errorMsg, Toast.LENGTH_LONG).show());
                }

            } catch (SocketTimeoutException e) {
                Log.e(TAG, "Timeout error: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(FieldDataActivity.this, "Connection timed out. Check your network or ESP device.", Toast.LENGTH_LONG).show());

            } catch (UnknownHostException e) {
                Log.e(TAG, "Network error: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(FieldDataActivity.this, "No internet connection or ESP device unreachable.", Toast.LENGTH_LONG).show());

            } catch (IOException e) {
                Log.e(TAG, "I/O error: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(FieldDataActivity.this, "Error reading data. Please try again.", Toast.LENGTH_LONG).show());

            } catch (Exception e) {
                Log.e(TAG, "Unexpected error: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(FieldDataActivity.this, "An unexpected error occurred.", Toast.LENGTH_LONG).show());

            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing reader: " + e.getMessage(), e);
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }


    private void updateFieldDataUI(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);

            soilMoistureTextView.setText("\uD83D\uDCA7 Soil Moisture (Central Node): " + jsonObject.getInt("soilMoisture"));
            receivedSoilMoistureTextView.setText("\uD83D\uDCA7 Soil Moisture (Node 2): " + jsonObject.getInt("receivedSoilMoisture"));
            motorStatusTextView.setText("⚙\uFE0F Motor Status: " + jsonObject.getString("motorStatus"));
            lowerThresholdTextView.setText("\uD83D\uDCC9 Lower Threshold: " + jsonObject.getInt("lowerThreshold"));
            upperThresholdTextView.setText("\uD83D\uDCC8 Upper Threshold: " + jsonObject.getInt("upperThreshold"));
            modeTextView.setText("\uD83D\uDD04 Mode: " + jsonObject.getString("mode"));
            remainingTimeTextView.setText("⏳Remaining Time: " + jsonObject.getInt("remainingTime") + " seconds");
        } catch (Exception e) {
            Log.e(TAG, "Error parsing field data", e);
            Toast.makeText(this, "Error parsing field data.", Toast.LENGTH_SHORT).show();
        }
    }

    private void controlPump(String action) {
        new Thread(() -> {
            try {
                URL url = new URL(ESP_IP + "/control?action=" + action);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(5000); // Set connection timeout
                connection.setReadTimeout(5000);    // Set read timeout

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Pump action: " + action + " succeeded.");
                    runOnUiThread(() -> Toast.makeText(FieldDataActivity.this, "Pump action: " + action + " succeeded.", Toast.LENGTH_SHORT).show());
                } else {
                    Log.e(TAG, "Failed to control pump. Response Code: " + responseCode);
                    runOnUiThread(() -> Toast.makeText(FieldDataActivity.this, "Failed to control pump. Response Code: " + responseCode, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error controlling pump", e);
                runOnUiThread(() -> Toast.makeText(FieldDataActivity.this, "Failed to control pump.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void setTimer(String timer) {
        new Thread(() -> {
            try {
                URL url = new URL(ESP_IP + "/set_timer?timer=" + timer);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(5000); // Set connection timeout
                connection.setReadTimeout(5000);    // Set read timeout

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Timer set to: " + timer + " minutes.");
                    runOnUiThread(() -> Toast.makeText(FieldDataActivity.this, "Timer set to: " + timer + " minutes.", Toast.LENGTH_SHORT).show());
                } else {
                    Log.e(TAG, "Failed to set timer. Response Code: " + responseCode);
                    runOnUiThread(() -> Toast.makeText(FieldDataActivity.this, "Failed to set timer. Response Code: " + responseCode, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting timer", e);
                runOnUiThread(() -> Toast.makeText(FieldDataActivity.this, "Failed to set timer.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void showManualModeToast() {
        Toast.makeText(this, "Please switch to Manual Mode to control the pump.", Toast.LENGTH_SHORT).show();
    }
}
