package com.example.outfitoftheday

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlin.properties.Delegates
import android.widget.Toast
import androidx.core.app.ActivityCompat
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class GenerateOutfitFragment : Fragment() {

    private lateinit var locationManager: LocationManager
    private var latitude = 0.0
    private var longitude = 0.0
    private lateinit var weatherTextView: TextView
    private lateinit var generateOutfitButton: Button
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    //Weather Variables
    private val client = OkHttpClient()
    private val locationAPIKEY = "O4YfE6O0SCM3xZvGwTbrlqaT0xyGm6ZU"
    private var weatherTemp = 0.0
    private var weatherPrecipitationProbability = 0.0
    private var weatherHumidity = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_generate_outfit, container, false)

        weatherTextView = view.findViewById(R.id.weatherTextView)
        generateOutfitButton = view.findViewById(R.id.generateOutfitButton)
        // Initialize LocationManager
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Check for location permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Request location updates
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
        } else {
            //Ask user for permission and then load location
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
        return view
    }

    // Define a LocationListener
    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Handle location updates
             latitude = location.latitude
             longitude = location.longitude

            // Update weatherTextView with latitude
            locationManager.removeUpdates(this) //Only get location Data Once

            //At this point, Call Weather API
            val url = "https://api.tomorrow.io/v4/weather/forecast?location=$latitude,$longitude&apikey=$locationAPIKEY"
            run(url)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove location updates when the fragment is destroyed to prevent memory leaks
        locationManager.removeUpdates(locationListener)
    }

    //This Function is used to handle the event in which a user accepts or declines allowing location to be shared with the application
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                // Check if the permission is granted
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted, proceed with location-related tasks
                    // Request location updates, initialize LocationManager, etc.
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
                    }
                } else {
                    // Permission is denied, handle accordingly (e.g., display a message to the user)
                    Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    //Run the Weather API Here:
    private fun run(url: String) {
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                // Handle API call failure
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val responseBody = response.body?.string()
                    responseBody?.let {
                        // Parse the JSON response
                        var jsonResponse = JSONObject(it)
                        jsonResponse = jsonResponse.get("timelines") as JSONObject
                        val jsonArray = jsonResponse.getJSONArray("minutely")
                        jsonResponse = jsonArray.getJSONObject(0)
                        val valuesObject = jsonResponse.getJSONObject("values")

                        //Now, get our desired values.
                        weatherPrecipitationProbability = valuesObject.optDouble("precipitationProbability", Double.NaN)
                        weatherTemp = valuesObject.optDouble("temperature", Double.NaN)
                        weatherHumidity = valuesObject.optDouble("humidity", Double.NaN)

                        //Turn WeatherTemp from C --> F
                        weatherTemp = (weatherTemp * 9/5) + 32

                        //Need to use this to update UI in a run()
                        requireActivity().runOnUiThread {
                            // Your UI update code here
                            weatherTextView.text= "Today's Weather Information:" + "\n" + "Temperature (F): " + weatherTemp.toString() + "\n" + "Precipitation Probability: " + weatherPrecipitationProbability.toString() + "%\n" + "Humidity Percentage: " + weatherHumidity.toString() + "%"
                        }
                    }
                }
            }
        })
    }
}
