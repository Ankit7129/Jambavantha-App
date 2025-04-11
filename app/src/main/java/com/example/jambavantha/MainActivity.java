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
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.os.AsyncTask;
import org.json.JSONObject;

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
    private static final String ESP_IP = "http://192.168.84.99"; // Example ESP IP for fetching sensor data

    private Button logoutButton, rainForecastButton, profileButton, fieldControlButton, irrigationButton ;
    private FirebaseAuth mAuth;
    private TextView usernameTextView, currentLocationTextView, temperatureDataTextView, humidityDataTextView, windSpeedDataTextView, motorFeedbackTextView, timerTextView;

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
        rainForecastButton = findViewById(R.id.rain_forecast_button);
        profileButton = findViewById(R.id.profile_button);
        fieldControlButton = findViewById(R.id.field_control_button);
        irrigationButton = findViewById(R.id.irrigation_button);
        usernameTextView = findViewById(R.id.username_text_view);
        currentLocationTextView = findViewById(R.id.current_location_text_view);
        temperatureDataTextView = findViewById(R.id.temperature_data);
        humidityDataTextView = findViewById(R.id.humidity_data);
        windSpeedDataTextView = findViewById(R.id.wind_speed_data);
    }

    private void setupButtonListeners() {
        // Logout Button
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });


        // Rain Forecast Button
        rainForecastButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ForecastActivity.class)));


        irrigationButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AdvisorActivity.class)));

        // Profile Button
        profileButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ProfileActivity.class)));


        // Field Control Button
        fieldControlButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FieldDataActivity.class);
            startActivity(intent);
        });



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
                currentLocationTextView.setText("Location: " + (cityName != null ? cityName : "Unknown") );
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
            temperatureDataTextView.setText("Temperature: " + temperature + "°C");
            humidityDataTextView.setText("Humidity: " + humidity + "%");

            // Extract wind speed
            if (jsonObject.has("wind")) {
                JSONObject wind = jsonObject.getJSONObject("wind");
                double windSpeed = wind.getDouble("speed");
                windSpeedDataTextView.setText("Wind Speed: " + windSpeed + " m/s");
            } else {
                windSpeedDataTextView.setText("Wind Speed: Data not available.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            temperatureDataTextView.setText("Error in weather data.");
            humidityDataTextView.setText("");
            windSpeedDataTextView.setText("");
        }
    }





    private void fetchLiveSensorData() {
        // Code to fetch live sensor data from ESP module and update UI
    }



    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(@NonNull String provider) {}

    @Override
    public void onProviderDisabled(@NonNull String provider) {}
}
