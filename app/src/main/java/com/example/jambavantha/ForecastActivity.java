package com.example.jambavantha;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ForecastActivity extends AppCompatActivity {

    // TextViews for displaying hourly data for each day
    private TextView[] day1Hours = new TextView[6];
    private TextView[] day2Hours = new TextView[6];
    private TextView[] day3Hours = new TextView[6];
    private TextView[] day4Hours = new TextView[6];
    private TextView[] day5Hours = new TextView[6];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast);

        // Initialize TextViews for each day and hour
        initializeTextViews();

        // Fetch forecast data
        new GetWeatherForecast().execute();
    }

    private void initializeTextViews() {
        // Day 1
        day1Hours[0] = findViewById(R.id.day1_hour1);
        day1Hours[1] = findViewById(R.id.day1_hour2);
        day1Hours[2] = findViewById(R.id.day1_hour3);
        day1Hours[3] = findViewById(R.id.day1_hour4);
        day1Hours[4] = findViewById(R.id.day1_hour5);
        day1Hours[5] = findViewById(R.id.day1_hour6);

        // Day 2
        day2Hours[0] = findViewById(R.id.day2_hour1);
        day2Hours[1] = findViewById(R.id.day2_hour2);
        day2Hours[2] = findViewById(R.id.day2_hour3);
        day2Hours[3] = findViewById(R.id.day2_hour4);
        day2Hours[4] = findViewById(R.id.day2_hour5);
        day2Hours[5] = findViewById(R.id.day2_hour6);

        // Day 3
        day3Hours[0] = findViewById(R.id.day3_hour1);
        day3Hours[1] = findViewById(R.id.day3_hour2);
        day3Hours[2] = findViewById(R.id.day3_hour3);
        day3Hours[3] = findViewById(R.id.day3_hour4);
        day3Hours[4] = findViewById(R.id.day3_hour5);
        day3Hours[5] = findViewById(R.id.day3_hour6);

        // Day 4
        day4Hours[0] = findViewById(R.id.day4_hour1);
        day4Hours[1] = findViewById(R.id.day4_hour2);
        day4Hours[2] = findViewById(R.id.day4_hour3);
        day4Hours[3] = findViewById(R.id.day4_hour4);
        day4Hours[4] = findViewById(R.id.day4_hour5);
        day4Hours[5] = findViewById(R.id.day4_hour6);

        // Day 5
        day5Hours[0] = findViewById(R.id.day5_hour1);
        day5Hours[1] = findViewById(R.id.day5_hour2);
        day5Hours[2] = findViewById(R.id.day5_hour3);
        day5Hours[3] = findViewById(R.id.day5_hour4);
        day5Hours[4] = findViewById(R.id.day5_hour5);
        day5Hours[5] = findViewById(R.id.day5_hour6);
    }

    private class GetWeatherForecast extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                String API_KEY = "da552626aaca0bf507c70ed7c260d724";  // Use your OpenWeatherMap API key here
                String location = "Kolkata"; // Use location data from GPS or user input
                String urlString = "https://api.openweathermap.org/data/2.5/forecast?q=" + location + "&appid=" + API_KEY + "&units=metric";
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return response.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    JSONArray list = jsonObject.getJSONArray("list");

                    // Extract and display the forecast for 5 days (every 3 hours in the forecast data)
                    for (int i = 0; i < 5; i++) {
                        int startIdx = i * 8; // 8 * 3-hour periods = 1 day

                        for (int j = 0; j < 6; j++) {
                            JSONObject hourData = list.getJSONObject(startIdx + j);
                            String dateTime = hourData.getString("dt_txt");
                            JSONObject main = hourData.getJSONObject("main");
                            double temp = main.getDouble("temp");
                            JSONArray weatherArray = hourData.getJSONArray("weather");
                            JSONObject weather = weatherArray.getJSONObject(0);
                            String description = weather.getString("description");
                            JSONObject clouds = hourData.getJSONObject("clouds");
                            int cloudiness = clouds.getInt("all");
                            JSONObject rain = hourData.optJSONObject("rain");
                            String rainAmount = rain != null ? rain.optString("3h", "0") + " mm" : "0 mm";

                            // Display the data in the corresponding TextView
                            String forecastText = dateTime + ": Temp: " + temp + "°C, Cloudiness: " + cloudiness + "%, Rain: " + rainAmount + ", " + description;

                            if (i == 0) day1Hours[j].setText(forecastText);
                            else if (i == 1) day2Hours[j].setText(forecastText);
                            else if (i == 2) day3Hours[j].setText(forecastText);
                            else if (i == 3) day4Hours[j].setText(forecastText);
                            else if (i == 4) day5Hours[j].setText(forecastText);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
