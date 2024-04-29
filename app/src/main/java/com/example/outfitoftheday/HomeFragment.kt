package com.example.outfitoftheday

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
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
        val entries = ArrayList<PieEntry>().apply {
            add(PieEntry(40f, getString(R.string.pieChartCategory_casual)))
            add(PieEntry(30f, getString(R.string.pieChartCategory_formal)))
            add(PieEntry(15f, getString(R.string.pieChartCategory_sport)))
            add(PieEntry(15f, getString(R.string.pieChartCategory_others)))
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#FFD700"),
                Color.parseColor("#C5B358"),
                Color.parseColor("#FFDF00"),
                Color.parseColor("#D4AF37")
            )
            valueTextColor = Color.WHITE
            valueTextSize = 12f
            setDrawValues(true)
        }

        pieChart.data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(pieChart))
        }
        pieChart.invalidate()
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
            locationListener = LocationListener { location ->
                latitude = location.latitude
                longitude = location.longitude
                fetchWeatherData(latitude, longitude)
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10f, locationListener)
        }
    }

    private var locationListener = LocationListener { location ->
        latitude = location.latitude
        longitude = location.longitude
        fetchWeatherData(latitude, longitude)
    }

    private fun fetchWeatherData(latitude: Double, longitude: Double) {
        val apiKey = "qIX6tr50cDZVt7xVQoJyNSZu17UzcEMZ"
        val client = OkHttpClient()
        val url = "https://api.tomorrow.io/v4/weather/realtime?location=${latitude},${longitude}&apikey=$apiKey"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val responseBody = response.body?.string()
                    responseBody?.let {
                        // Parse the JSON response
                        val jsonResponse = JSONObject(it)
                        val dataObject = jsonResponse.optJSONObject("data")
                        dataObject?.let {
                            val valuesObject = dataObject.optJSONObject("values")

                            //Now, get our desired values.
                            val weatherCode = valuesObject.optInt("weatherCode", -1)
                            val time = dataObject.optString("time", "")
                            val isDaytime = determineDayOrNight(time)

                            // Update weather icon based on weather code and daytime/nighttime
                            updateWeatherIcon(weatherCode, isDaytime)

                            // Handle UI update
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

    private fun updateWeatherIcon(weatherCode: Int, isDaytime: Boolean) {
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

        activity?.runOnUiThread {
            weatherIcon.setImageResource(iconRes)
        }
    }
}
