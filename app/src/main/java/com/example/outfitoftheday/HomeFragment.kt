package com.example.outfitoftheday

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone


class HomeFragment : Fragment() {
    private lateinit var pieChart: PieChart
    private lateinit var welcomeText: TextView
    private lateinit var weatherIcon: ImageView
    private lateinit var locationManager: LocationManager
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private val LOCATION_PERMISSION_REQUEST_CODE = 100

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        pieChart = view.findViewById(R.id.pieChart)
        welcomeText = view.findViewById(R.id.welcomeTextView)
        weatherIcon = view.findViewById(R.id.weatherImageView)

        setupPieChart()
        loadPieChartData()
        displayUserGreeting()
        setupLocationManager()
        return view
    }

    private fun setupPieChart() {
        pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            legend.isEnabled = false
        }
    }

    private fun loadPieChartData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseReference = FirebaseDatabase.getInstance().getReference("users/$userId/outfits")
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val categoryMap = HashMap<String, Float>()

                for (outfitSnapshot in snapshot.children) {
                    val category = outfitSnapshot.child("category").getValue(String::class.java)
                    if (category != null) {
                        categoryMap[category] = categoryMap.getOrDefault(category, 0f) + 1
                    }
                }

                // Log each category and its count for debugging
                categoryMap.forEach { (category, count) ->
                    Log.d("PieChartData", "Category: $category, Count: $count")
                }

                val entries = ArrayList<PieEntry>()
                for (entry in categoryMap.entries) {
                    // Get localized category name
                    entries.add(PieEntry(entry.value, makeLocale(entry.key)))
                }

                if (entries.isNotEmpty()) {
                    val dataSet = PieDataSet(entries, "").apply {
                        colors = listOf(
                            Color.parseColor("#FFD700"),
                            Color.parseColor("#C5B358"),
                            Color.parseColor("#FFDF00"),
                            Color.parseColor("#D4AF37")
                        )
                        valueTextColor = Color.WHITE
                        valueTextSize = 16f
                        setDrawValues(true)
                    }

                    pieChart.data = PieData(dataSet).apply {
                        setValueFormatter(PercentFormatter(pieChart))
                    }
                    pieChart.invalidate()
                } else {
                    Log.d("PieChartData", "No categories to display.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("loadPieChartData", "Error loading data", error.toException())
            }
        })
    }

    // Function to get localized string based on current locale

    private fun  makeLocale(title: String) : String {
        return when (title) {
            "All" -> resources.getStringArray(R.array.clothing_categories)[0]
            "Hats" -> resources.getStringArray(R.array.clothing_categories)[1]
            "Tops" -> resources.getStringArray(R.array.clothing_categories)[2]
            "Bottoms" -> resources.getStringArray(R.array.clothing_categories)[3]
            "Footwear" -> resources.getStringArray(R.array.clothing_categories)[4]
            "Miscellaneous" -> resources.getStringArray(R.array.clothing_categories)[5]
            else -> title // Return the original title if it's not found
        }
    }

    private fun displayUserGreeting() {
        val currUser = FirebaseAuth.getInstance().currentUser
        welcomeText.text = getString(R.string.pieChartCategory_welcomeWord) + ", ${currUser?.email?.split('@')?.first()}!"
    }

    private fun setupLocationManager() {
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            requestLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission") // Permission is checked before calling this method
    private fun requestLocationUpdates() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10f, locationListener)
    }

    private var locationListener = LocationListener { location ->
        latitude = location.latitude
        longitude = location.longitude
        if (isAdded) {
            fetchWeatherData(latitude, longitude)
        }
    }

    private fun fetchWeatherData(latitude: Double, longitude: Double) {
        val apiKey = BuildConfig.WEATHER_API_KEY
        val client = OkHttpClient()
        val url = "https://api.tomorrow.io/v4/weather/realtime?location=${latitude},${longitude}&apikey=$apiKey"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("WeatherData", "Failed to fetch weather data", e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e("WeatherData", "Unexpected response code $response")
                        return
                    }

                    val responseBody = response.body?.string()
                    responseBody?.let {
                        val jsonResponse = JSONObject(it)
                        val dataObject = jsonResponse.optJSONObject("data")
                        dataObject?.let {
                            val valuesObject = dataObject.optJSONObject("values")
                            val weatherCode = valuesObject.optInt("weatherCode", -1)
                            val temperatureC = valuesObject.optDouble("temperature", Double.NaN)
                            val isDaytime = determineDayOrNight(dataObject.optString("time", ""))

                            if (isAdded) {
                                updateWeatherIcon(weatherCode, isDaytime, temperatureC)
                            }
                        }
                    }
                }
            }
        })
    }

    private fun determineDayOrNight(time: String): Boolean {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = dateFormat.parse(time) ?: return false
        val calendar = Calendar.getInstance()
        calendar.time = date
        return calendar.get(Calendar.HOUR_OF_DAY) in 7..19  // Consider daytime from 7 AM to 7 PM
    }

    @SuppressLint("SetTextI18n")
    private fun updateWeatherIcon(weatherCode: Int, isDaytime: Boolean, temperatureC: Double) {
        val iconRes = when (weatherCode) {
            1000 -> if (isDaytime) R.drawable.clear_day else R.drawable.clear_night
            1100 -> if (isDaytime) R.drawable.mostly_clear_day else R.drawable.mostly_clear_night
            1101 -> if (isDaytime) R.drawable.partly_cloudy_day else R.drawable.partly_cloudy_night
            1102 -> R.drawable.mostly_cloudy
            1001 -> R.drawable.cloudy
            2000, 2100 -> R.drawable.fog
            4000 -> R.drawable.drizzle
            4001, 4201 -> R.drawable.rain_heavy
            4200 -> R.drawable.rain
            5000, 5100, 5101, 5001 -> R.drawable.snow
            6000, 6200, 6201, 6001 -> R.drawable.freezing_rain
            7000, 7101, 7102 -> R.drawable.ice_pellets
            8000 -> R.drawable.thunderstorm
            else -> R.drawable.unknown_weather
        }

        val description = when (weatherCode) {
            1000 -> getString(R.string.weather_clear)
            1100 -> getString(R.string.weather_mostly_clear)
            1101 -> getString(R.string.weather_partly_cloudy)
            1102 -> getString(R.string.weather_mostly_cloudy)
            1001 -> getString(R.string.weather_cloudy)
            2000, 2100 -> getString(R.string.weather_fog)
            4000 -> getString(R.string.weather_drizzle)
            4001, 4201 -> getString(R.string.weather_heavy_rain)
            4200 -> getString(R.string.weather_rain)
            5000, 5100, 5101, 5001 -> getString(R.string.weather_snow)
            6000, 6200, 6201, 6001 -> getString(R.string.weather_freezing_rain)
            7000, 7101, 7102 -> getString(R.string.weather_ice_pellets)
            8000 -> getString(R.string.weather_thunderstorm)
            else -> getString(R.string.weather_unknown)
        }

        // Convert Celsius to Fahrenheit
        val temperatureF = temperatureC * 9/5 + 32

        activity?.runOnUiThread {
            weatherIcon.setImageResource(iconRes)
            // Update the weather information text
            val weatherInfoTextView: TextView = requireView().findViewById(R.id.weatherInfoTextView)
            weatherInfoTextView.text = "$description, ${String.format("%.1f", temperatureF)}°F"
        }
    }

    override fun onStop() {
        super.onStop()
        if (this::locationManager.isInitialized) {
            locationManager.removeUpdates(locationListener)
        }
    }
}
