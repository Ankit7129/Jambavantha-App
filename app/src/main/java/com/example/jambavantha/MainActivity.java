package com.example.jambavantha;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String WEATHER_API_KEY = "da552626aaca0bf507c70ed7c260d724"; // OpenWeatherMap API Key
    private static final String ESP_IP = "http://192.168.23.99"; // Example ESP IP for fetching sensor data

    private Button logoutButton, modeSwitchButton, rainForecastButton, profileButton, scheduleIntegrationButton, motorOnButton, motorOffButton;
    private FirebaseAuth mAuth;
    private TextView usernameTextView, currentLocationTextView, soilMoistureDataTextView, temperatureDataTextView, humidityDataTextView, rainForecastDataTextView, motorFeedbackTextView, timerTextView, weatherDataTextView;

    private boolean isAutoMode = true; // Default to Auto Mode
    private LocationManager locationManager;
    private Calendar scheduledTime;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI Elements
        initializeUIElements();

        // Get username from Firebase Auth
        String username = mAuth.getCurrentUser().getDisplayName();
        usernameTextView.setText("Hello " + username);

        // Handle UI Button Clicks
        setupButtonListeners();

        // Initialize Location Manager and fetch location
        initializeLocationManager();

        // Fetch live data from ESP Module
        fetchLiveSensorData();
    }

    private void initializeUIElements() {
        logoutButton = findViewById(R.id.logout_button);
        modeSwitchButton = findViewById(R.id.mode_switch_button);
        rainForecastButton = findViewById(R.id.rain_forecast_button);
        profileButton = findViewById(R.id.profile_button);
        scheduleIntegrationButton = findViewById(R.id.schedule_integration_button);
        motorOnButton = findViewById(R.id.manual_motor_on_button);
        motorOffButton = findViewById(R.id.manual_motor_off_button);
        usernameTextView = findViewById(R.id.username_text_view);
        currentLocationTextView = findViewById(R.id.current_location_text_view);
        soilMoistureDataTextView = findViewById(R.id.soil_moisture_data);
        temperatureDataTextView = findViewById(R.id.temperature_data);
        humidityDataTextView = findViewById(R.id.humidity_data);
        rainForecastDataTextView = findViewById(R.id.rain_forecast_data);
        motorFeedbackTextView = findViewById(R.id.motor_feedback_text_view);
        timerTextView = findViewById(R.id.timer_text_view);
        weatherDataTextView = findViewById(R.id.weather_data_text_view);
    }

    private void setupButtonListeners() {
        // Logout Button
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        // Mode Switch Button
        modeSwitchButton.setOnClickListener(v -> toggleMode());

        // Rain Forecast Button
        rainForecastButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ForecastActivity.class)));

        // Profile Button
        profileButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ProfileActivity.class)));

        // Schedule Integration Button
        scheduleIntegrationButton.setOnClickListener(v -> handleScheduleButton());

        // Motor Control Buttons
        motorOnButton.setOnClickListener(v -> controlMotor(true));
        motorOffButton.setOnClickListener(v -> controlMotor(false));

        // Update UI based on the current mode
        updateUIForMode();
    }

    private void initializeLocationManager() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        fetchCityName(latitude, longitude);
        fetchWeatherData(latitude, longitude);
    }

    private void fetchCityName(double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String cityName = address.getLocality();
                currentLocationTextView.setText("Location: " + (cityName != null ? cityName : "Unknown") + " (" + latitude + ", " + longitude + ")");
            } else {
                currentLocationTextView.setText("Location: Unknown (" + latitude + ", " + longitude + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to fetch city name.", Toast.LENGTH_SHORT).show();
            currentLocationTextView.setText("Location: Unknown (" + latitude + ", " + longitude + ")");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                }
            } else {
                Toast.makeText(this, "Location permission is required to fetch weather data.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void toggleMode() {
        isAutoMode = !isAutoMode;
        updateUIForMode();

        if (isAutoMode) {
            Toast.makeText(this, "Switched to Auto Mode", Toast.LENGTH_SHORT).show();
            handleAutoMode();
        } else {
            Toast.makeText(this, "Switched to Manual Mode", Toast.LENGTH_SHORT).show();
            handleManualMode();
        }
    }

    private void updateUIForMode() {
        if (isAutoMode) {
            modeSwitchButton.setText("Switch to Manual Mode");
            motorFeedbackTextView.setText("Auto Mode: Sensor-based irrigation");
            disableManualControls();
        } else {
            modeSwitchButton.setText("Switch to Auto Mode");
            motorFeedbackTextView.setText("Manual Mode: Set timer or control motor");
            enableManualControls();
        }
    }


    private void handleScheduleButton() {
        if (isAutoMode) {
            Toast.makeText(this, "Please switch to Manual Mode to schedule the motor.", Toast.LENGTH_SHORT).show();
        } else {
            showSchedulerDialog();
        }
    }

    private void disableManualControls() {
        scheduleIntegrationButton.setEnabled(false);
        motorOnButton.setEnabled(false);
        motorOffButton.setEnabled(false);
    }

    private void enableManualControls() {
        scheduleIntegrationButton.setEnabled(true);
        motorOnButton.setEnabled(true);
        motorOffButton.setEnabled(true);
    }

    private void fetchWeatherData(double latitude, double longitude) {
        new Thread(() -> {
            try {
                String apiUrl = "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&appid=" + WEATHER_API_KEY + "&units=metric";
                HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                connection.disconnect();

                runOnUiThread(() -> updateWeatherUI(response.toString()));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to fetch weather data.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void updateWeatherUI(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONObject main = jsonObject.getJSONObject("main");
            double temperature = main.getDouble("temp");
            int humidity = main.getInt("humidity");
            weatherDataTextView.setText("Temperature: " + temperature + "°C\nHumidity: " + humidity + "%");

            // Extract rain forecast
            if (jsonObject.has("rain")) {
                JSONObject rain = jsonObject.getJSONObject("rain");
                String rainInfo = "Rain volume: " + rain.optDouble("1h", 0) + " mm";
                rainForecastDataTextView.setText(rainInfo);
            } else {
                rainForecastDataTextView.setText("No rain forecast available.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            weatherDataTextView.setText("Error in weather data.");
        }
    }

    private void controlMotor(boolean turnOn) {
        String action = turnOn ? "on" : "off";
        new Thread(() -> {
            try {
                URL url = new URL(ESP_IP + "/motor?state=" + action);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> motorFeedbackTextView.setText("Motor turned " + (turnOn ? "on" : "off")));
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to control motor.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error controlling motor.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void fetchLiveSensorData() {
        new Thread(() -> {
            try {
                URL url = new URL(ESP_IP + "/sensors");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();
                connection.disconnect();

                runOnUiThread(() -> updateSensorUI(response.toString()));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to fetch sensor data.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void updateSensorUI(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            double soilMoisture = jsonObject.getDouble("soil_moisture");
            double temperature = jsonObject.getDouble("temperature");
            double humidity = jsonObject.getDouble("humidity");

            soilMoistureDataTextView.setText("Soil Moisture: " + soilMoisture + "%");
            temperatureDataTextView.setText("Temperature: " + temperature + "°C");
            humidityDataTextView.setText("Humidity: " + humidity + "%");
        } catch (Exception e) {
            e.printStackTrace();
            soilMoistureDataTextView.setText("Error fetching data.");
        }
    }

    private void showSchedulerDialog() {
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            scheduledTime.set(Calendar.YEAR, year);
            scheduledTime.set(Calendar.MONTH, month);
            scheduledTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this, (timePicker, hourOfDay, minute) -> {
                scheduledTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                scheduledTime.set(Calendar.MINUTE, minute);
                schedulePumpTask();
            }, currentHour, currentMinute, true);
            timePickerDialog.show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void schedulePumpTask() {
        // Implement scheduling logic here
        Toast.makeText(this, "Pump scheduled for " + scheduledTime.getTime().toString(), Toast.LENGTH_LONG).show();
    }

    private void handleAutoMode() {
        // Implement Auto Mode logic here
    }

    private void handleManualMode() {
        // Implement Manual Mode logic here
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
}
