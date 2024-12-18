package com.example.weather;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView cityNameText, temperatureText, humidityText, descriptionText, windText;
    private ImageView weatherIcon;
    private Button refreshButton;
    private AutoCompleteTextView cityNameInput;
    private Spinner allCitiesSpinner;
    private static final String API_KEY = "6002cdee5fc55ef40b5203f466b55b58";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        cityNameText = findViewById(R.id.cityNameText);
        temperatureText = findViewById(R.id.temperatureText);
        humidityText = findViewById(R.id.humidityText);
        windText = findViewById(R.id.windText);
        descriptionText = findViewById(R.id.descriptionText);
        weatherIcon = findViewById(R.id.weatherIcon);
        refreshButton = findViewById(R.id.fetchWeatherButton);
        cityNameInput = findViewById(R.id.cityNameInput);
        allCitiesSpinner = findViewById(R.id.allCitiesSpinner);

        // Populate city suggestions and spinner
        List<String> citySuggestions = getCommonCities();
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, citySuggestions);
        cityNameInput.setAdapter(cityAdapter);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, citySuggestions);
        allCitiesSpinner.setAdapter(spinnerAdapter);

        // Fetch weather data for the selected city in spinner
        allCitiesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String selectedCity = adapterView.getItemAtPosition(position).toString();
                FetchWeatherData(selectedCity);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        // Set click listener for the refresh button
        refreshButton.setOnClickListener(view -> {
            String cityName = cityNameInput.getText().toString();
            if (!cityName.isEmpty()) {
                FetchWeatherData(cityName);
            } else {
                cityNameInput.setError("Please enter a city name");
            }
        });

        // Fetch weather data for the default city
        FetchWeatherData("Colombo");
    }

    private List<String> getCommonCities() {
        // Add a list of common cities or fetch dynamically
        List<String> cities = new ArrayList<>();
        cities.add("New York");
        cities.add("London");
        cities.add("Tokyo");
        cities.add("Colombo");
        cities.add("Dubai");
        // You can fetch and add more cities from an API or database
        return cities;
    }

    private void FetchWeatherData(String cityName) {
        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + cityName + "&appid=" + API_KEY + "&units=metric";

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();

            try {
                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String result = response.body().string();
                    runOnUiThread(() -> updateUI(result));
                } else {
                    runOnUiThread(() -> cityNameInput.setError("Invalid city name or server error"));
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> cityNameInput.setError("Network error. Please try again."));
            }
        });
    }

    private void updateUI(String result) {
        if (result != null) {
            try {
                JSONObject jsonObject = new JSONObject(result);

                // Parse JSON response
                JSONObject main = jsonObject.getJSONObject("main");
                double temperature = main.optDouble("temp", 0);
                double humidity = main.optDouble("humidity", 0);
                double windSpeed = jsonObject.getJSONObject("wind").optDouble("speed", 0);
                String description = jsonObject.getJSONArray("weather").getJSONObject(0).optString("description", "N/A");
                String iconCode = jsonObject.getJSONArray("weather").getJSONObject(0).optString("icon", "");

                // Update UI
                cityNameText.setText(jsonObject.optString("name", "N/A"));
                temperatureText.setText(String.format("%.0f°C", temperature));
                humidityText.setText(String.format("%.0f%%", humidity));
                windText.setText(String.format("%.0f km/h", windSpeed));
                descriptionText.setText(description);

                // Update weather icon
                String resourceName = "ic_" + iconCode;
                int resId = getResources().getIdentifier(resourceName, "drawable", getPackageName());
                if (resId != 0) {
                    weatherIcon.setImageResource(resId);
                } else {
                    weatherIcon.setImageResource(R.drawable.ic_01n); // Fallback icon
                }
            } catch (JSONException e) {
                e.printStackTrace();
                cityNameInput.setError("Error parsing weather data");
            }
        }
    }
}
