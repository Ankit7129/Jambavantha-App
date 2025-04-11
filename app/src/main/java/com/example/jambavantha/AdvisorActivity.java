package com.example.jambavantha;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AdvisorActivity extends AppCompatActivity {

    private EditText soilType, crop, stage, moisture, n, p, k;
    private LinearLayout forecastContainer;
    private Button sendButton, addForecastBtn;
    private TextView resultText;
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advisor);

        initializeViews();
        addForecastBlock(); // Add initial forecast block

        addForecastBtn.setOnClickListener(view -> addForecastBlock());
        sendButton.setOnClickListener(view -> validateAndSendData());
    }

    private void initializeViews() {
        soilType = findViewById(R.id.soilType);
        crop = findViewById(R.id.crop);
        stage = findViewById(R.id.stage);
        moisture = findViewById(R.id.moisture);
        n = findViewById(R.id.n);
        p = findViewById(R.id.p);
        k = findViewById(R.id.k);
        forecastContainer = findViewById(R.id.forecastContainer);
        sendButton = findViewById(R.id.sendButton);
        addForecastBtn = findViewById(R.id.addForecastBtn);
        resultText = findViewById(R.id.resultText);

        // Autocomplete arrays
        String[] soilTypes = {"Loamy/Clayey", "Alluvial", "Loamy"};
        String[] crops = {"Paddy", "Wheat", "Maize"};
        String[] stages = {
                "Nursery", "Transplanting", "Tillering", "Panicle Initiation", "Flowering",
                "Grain Filling", "Maturity", "Germination", "Stem Extension",
                "Heading", "Vegetative", "Tasseling", "Silking", "Grain Development"
        };

        // Set adapters
        ArrayAdapter<String> soilAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, soilTypes);
        ArrayAdapter<String> cropAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, crops);
        ArrayAdapter<String> stageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, stages);

        ((AutoCompleteTextView) soilType).setAdapter(soilAdapter);
        ((AutoCompleteTextView) crop).setAdapter(cropAdapter);
        ((AutoCompleteTextView) stage).setAdapter(stageAdapter);
    }


    private void addForecastBlock() {
        View forecastView = getLayoutInflater().inflate(R.layout.forecast_input_block, null);

        // Set remove button functionality
        Button removeBtn = forecastView.findViewById(R.id.removeForecastBtn);
        removeBtn.setOnClickListener(v -> {
            if (forecastContainer.getChildCount() > 1) {
                forecastContainer.removeView(forecastView);
            } else {
                Toast.makeText(this, "At least one forecast is required", Toast.LENGTH_SHORT).show();
            }
        });

        forecastContainer.addView(forecastView);
    }

    private void validateAndSendData() {
        // Basic validation
        if (soilType.getText().toString().isEmpty() ||
                crop.getText().toString().isEmpty() ||
                stage.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please fill all field data", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < forecastContainer.getChildCount(); i++) {
            View forecastView = forecastContainer.getChildAt(i);
            EditText dateTime = forecastView.findViewById(R.id.dateTime);
            EditText temp = forecastView.findViewById(R.id.temperature);
            EditText rain = forecastView.findViewById(R.id.rainAmount);

            if (dateTime.getText().toString().isEmpty() ||
                    temp.getText().toString().isEmpty() ||
                    rain.getText().toString().isEmpty()) {
                Toast.makeText(this, "Please fill all forecast fields", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        sendDataToApi();
    }

    private void sendDataToApi() {
        executor.execute(() -> {
            try {
                JSONObject requestBody = buildRequestJson();
                String response = makeApiRequest(requestBody);

                runOnUiThread(() -> {
                    resultText.setText(formatApiResponse(response));
                    Toast.makeText(AdvisorActivity.this, "Recommendations received!", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    resultText.setText("Error: " + e.getMessage());
                    Toast.makeText(AdvisorActivity.this, "API Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
                e.printStackTrace();
            }
        });
    }

    private JSONObject buildRequestJson() throws JSONException {
        // Build field data
        JSONObject fieldData = new JSONObject();
        fieldData.put("Soil_Type", soilType.getText().toString());
        fieldData.put("Crop", crop.getText().toString());
        fieldData.put("Stage", stage.getText().toString());
        fieldData.put("Current_Moisture", Double.parseDouble(moisture.getText().toString()));
        fieldData.put("Current_N", Double.parseDouble(n.getText().toString()));
        fieldData.put("Current_P", Double.parseDouble(p.getText().toString()));
        fieldData.put("Current_K", Double.parseDouble(k.getText().toString()));

        // Build forecast array
        JSONArray forecastArray = new JSONArray();
        for (int i = 0; i < forecastContainer.getChildCount(); i++) {
            View forecastView = forecastContainer.getChildAt(i);
            EditText dateTime = forecastView.findViewById(R.id.dateTime);
            EditText temperature = forecastView.findViewById(R.id.temperature);
            EditText rainAmount = forecastView.findViewById(R.id.rainAmount);

            JSONObject forecast = new JSONObject();
            forecast.put("Date_Time", dateTime.getText().toString());
            forecast.put("Temperature_C", Double.parseDouble(temperature.getText().toString()));
            forecast.put("Rain_Amount_mm", Double.parseDouble(rainAmount.getText().toString()));
            forecastArray.put(forecast);
        }

        // Combine into final request
        JSONObject requestBody = new JSONObject();
        requestBody.put("field_data", fieldData);
        requestBody.put("weather_forecast", forecastArray);

        return requestBody;
    }

    private String makeApiRequest(JSONObject requestBody) throws IOException {
        URL url = new URL("https://farmers-friend-jambu-804603944436.asia-south1.run.app/get-recommendations");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        // Write request body
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Read response
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        } finally {
            conn.disconnect();
        }
    }

    private String formatApiResponse(String jsonResponse) {
        try {
            JSONObject response = new JSONObject(jsonResponse);
            StringBuilder formatted = new StringBuilder();

            // Format irrigation recommendations
            if (response.has("irrigation")) {
                JSONObject irrigation = response.getJSONObject("irrigation");
                formatted.append("💧 Irrigation Recommendations\n\n");
                formatted.append("• Current Moisture: ").append(irrigation.getDouble("current_moisture")).append("%\n");
                formatted.append("• Ideal Moisture: ").append(irrigation.getInt("ideal_moisture")).append("%\n");

                if (irrigation.getBoolean("needed")) {
                    formatted.append("• Recommended Amount: ").append(irrigation.getDouble("recommended_amount")).append("mm\n");
                }

                formatted.append("\nℹ️ ").append(irrigation.getString("final_recommendation")).append("\n\n");
            }

            // Format fertilizer recommendations
            if (response.has("fertilizer")) {
                JSONObject fertilizer = response.getJSONObject("fertilizer");
                formatted.append("🌱 Fertilizer Recommendations\n\n");

                // Nitrogen (N) recommendation
                JSONObject nitrogen = fertilizer.getJSONObject("N");
                formatted.append("Nitrogen (N):\n");
                formatted.append("- Current: ").append(nitrogen.getDouble("current")).append(" kg/ha\n");
                formatted.append("- Ideal: ").append(nitrogen.getInt("ideal")).append(" kg/ha\n");
                formatted.append("- Status: ").append(nitrogen.getString("status")).append("\n");
                formatted.append("- Recommendation: ").append(nitrogen.getString("recommendations")).append("\n\n");

                // Phosphorus (P) recommendation
                JSONObject phosphorus = fertilizer.getJSONObject("P");
                formatted.append("Phosphorus (P):\n");
                formatted.append("- Current: ").append(phosphorus.getDouble("current")).append(" kg/ha\n");
                formatted.append("- Ideal: ").append(phosphorus.getInt("ideal")).append(" kg/ha\n");
                formatted.append("- Status: ").append(phosphorus.getString("status")).append("\n");
                formatted.append("- Recommendation: ").append(phosphorus.getString("recommendations")).append("\n\n");

                // Potassium (K) recommendation
                JSONObject potassium = fertilizer.getJSONObject("K");
                formatted.append("Potassium (K):\n");
                formatted.append("- Current: ").append(potassium.getDouble("current")).append(" kg/ha\n");
                formatted.append("- Ideal: ").append(potassium.getInt("ideal")).append(" kg/ha\n");
                formatted.append("- Status: ").append(potassium.getString("status")).append("\n");
                formatted.append("- Recommendation: ").append(potassium.getString("recommendations")).append("\n");
            }

            return formatted.toString();
        } catch (JSONException e) {
            return "⚠️ Couldn't parse server response:\n\n" + jsonResponse;
        }
    }
}