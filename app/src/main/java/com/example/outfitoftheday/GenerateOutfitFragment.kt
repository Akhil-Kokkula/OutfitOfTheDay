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
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlin.properties.Delegates
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GenerateOutfitFragment : Fragment() {

    private lateinit var locationManager: LocationManager
    private lateinit var outfitPhotoGallery : RecyclerView
    private lateinit var outfitPhotoList : MutableList<OutfitPhoto>
    private lateinit var occasionInputText : EditText
    private lateinit var durationInputText : EditText
    private lateinit var weatherJSONObject : JSONObject
    private lateinit var hourlyWeatherJSONString : String
    private lateinit var outfitAIResponse : String
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
        occasionInputText = view.findViewById<TextInputLayout>(R.id.tilOccasion).editText!!
        durationInputText = view.findViewById<TextInputLayout>(R.id.tilDuration).editText!!
        // Initialize LocationManager
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        outfitPhotoGallery = view.findViewById(R.id.rvOutfitGallery)
        outfitPhotoGallery.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        outfitPhotoList = mutableListOf(
            OutfitPhoto("https://static.vecteezy.com/system/resources/previews/004/141/669/non_2x/no-photo-or-blank-image-icon-loading-images-or-missing-image-mark-image-not-available-or-image-coming-soon-sign-simple-nature-silhouette-in-frame-isolated-illustration-vector.jpg"),
            OutfitPhoto("https://static.vecteezy.com/system/resources/previews/004/141/669/non_2x/no-photo-or-blank-image-icon-loading-images-or-missing-image-mark-image-not-available-or-image-coming-soon-sign-simple-nature-silhouette-in-frame-isolated-illustration-vector.jpg"),
            OutfitPhoto("https://static.vecteezy.com/system/resources/previews/004/141/669/non_2x/no-photo-or-blank-image-icon-loading-images-or-missing-image-mark-image-not-available-or-image-coming-soon-sign-simple-nature-silhouette-in-frame-isolated-illustration-vector.jpg"),
            OutfitPhoto("https://static.vecteezy.com/system/resources/previews/004/141/669/non_2x/no-photo-or-blank-image-icon-loading-images-or-missing-image-mark-image-not-available-or-image-coming-soon-sign-simple-nature-silhouette-in-frame-isolated-illustration-vector.jpg")
        )

        val outfitGalleryAdapter = OutfitGalleryAdapter(outfitPhotoList)
        outfitPhotoGallery.adapter = outfitGalleryAdapter

        generateOutfitButton.setOnClickListener {
            generateOutfitAction()
        }

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

    private fun generateOutfitAction() {
        if (occasionInputText.text.toString() == "") {
            Toast.makeText(context, "Please enter your occasion", Toast.LENGTH_SHORT).show()
        } else if (durationInputText.text.toString() == "" || durationInputText.text.toString().toInt() == 0) {
            Toast.makeText(context, "Please enter how long you will wear the outfit for in hours", Toast.LENGTH_SHORT).show()
        } else {
            getHourlyWeatherData()
            val aiTextStrBuilder = StringBuilder()
            aiTextStrBuilder.append("You are a fashion stylist and you must give the user a full outfit for the day. ")
            aiTextStrBuilder.append("Here is the expected output format you need to provide and please only answer in this format:\n" +
                    "item1: T-shirt, White, Casual\n" +
                    "item2: Jeans, Light Blue, Casual\n" +
                    "item10: Sneakers, White, Casual\n" +
                    "item17: Aviator Sunglasses, Black, Casual\n\n")
            aiTextStrBuilder.append("Weather Information:\n")
            aiTextStrBuilder.append(hourlyWeatherJSONString)
            aiTextStrBuilder.append("\n\n")
            aiTextStrBuilder.append("Occasion:\n")
            aiTextStrBuilder.append(occasionInputText.text.toString())
            aiTextStrBuilder.append("\n\n")
            aiTextStrBuilder.append("User wardrobe:\n")
            //hardcoded now
            aiTextStrBuilder.append("item1: Crop Top, Pink, Casual\n" +
                    "item2: High-Waisted Jeans, Medium Wash, Casual\n" +
                    "item3: Off-Shoulder Blouse, Floral Print, Casual\n" +
                    "item4: Midi Skirt, Beige, Casual\n" +
                    "item5: Leather Moto Jacket, Black, Casual\n" +
                    "item6: Suede Jacket, Camel, Casual\n" +
                    "item7: Knit Sweater Dress, Burgundy, Casual\n" +
                    "item8: Pullover Hoodie, Gray, Casual\n" +
                    "item9: Blazer, Navy Blue, Formal/Business\n" +
                    "item10: Slip-on Sneakers, Pastel Pink, Casual\n" +
                    "item11: Pointed Toe Flats, Nude, Formal\n" +
                    "item12: Ankle Boots, Black, Casual/Formal\n" +
                    "item13: Skinny Belt, Red, Casual/Formal\n" +
                    "item14: Chain Belt, Gold, Casual/Formal\n" +
                    "item15: Rose Gold Watch, Rose Gold, Casual/Formal\n" +
                    "item16: Leather Cuff Bracelet, Brown, Casual/Formal\n" +
                    "item17: Cat-eye Sunglasses, Tortoise Shell, Casual\n" +
                    "item18: Round Sunglasses, Gold Frame, Casual\n" +
                    "item19: Yoga Leggings, Black, Sportswear\n" +
                    "item20: Sports Bra, Purple, Sportswear\n" +
                    "item21: Rain Jacket, Yellow, Casual\n" +
                    "item22: Puffer Jacket, Navy Blue, Casual/Formal\n")

            aiTextStrBuilder.append("\n\n")
            aiTextStrBuilder.append("Please generate an outfit for me")



            val aiTextStr = aiTextStrBuilder.toString()
            println("will send to ai")
            println(aiTextStr)

            outfitAIResponse = sendAndReceiveMessageFromClaude(aiTextStr)
            print("received ai response")
            print(outfitAIResponse)

        }
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
                        weatherJSONObject = jsonResponse
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
                            if (isAdded) {
                                weatherTextView.text= "Today's Weather Information:" + "\n" + "Temperature (F): " + "%.2f".format(weatherTemp) + "\n" + "Precipitation Probability: " + weatherPrecipitationProbability.toString() + "%\n" + "Humidity Percentage: " + weatherHumidity.toString() + "%"
                            }
                            // Your UI update code here
                        }
                    }
                }
            }
        })
    }

    private fun getHourlyWeatherData() {
        val timelinesObject = weatherJSONObject.getJSONObject("timelines")

        val hourlyArray = timelinesObject.getJSONArray("hourly")

        val filteredHourlyArray = JSONArray()

        for (i in 0 until minOf(hourlyArray.length(), durationInputText.text.toString().toInt())) {
            val hourlyObject = hourlyArray.getJSONObject(i)

            val valuesObject = hourlyObject.getJSONObject("values")
            val filteredValuesObject = JSONObject()
            filteredValuesObject.put("humidity", valuesObject.optInt("humidity"))
            filteredValuesObject.put("precipitationProbability", valuesObject.optInt("precipitationProbability"))
            filteredValuesObject.put("temperature", valuesObject.optDouble("temperature"))
            filteredValuesObject.put("uvIndex", valuesObject.optInt("uvIndex"))
            filteredValuesObject.put("windGust", valuesObject.optDouble("windGust"))
            filteredValuesObject.put("windSpeed", valuesObject.optDouble("windSpeed"))

            val filteredHourlyObject = JSONObject()
            filteredHourlyObject.put("time", hourlyObject.getString("time"))
            filteredHourlyObject.put("values", filteredValuesObject)

            filteredHourlyArray.put(filteredHourlyObject)
        }

        val filteredJsonObject = JSONObject()
        filteredJsonObject.put("timelines", JSONObject().put("hourly", filteredHourlyArray))
        filteredJsonObject.put("location", weatherJSONObject.getJSONObject("location"))

        Log.d("Filtered JSON", filteredJsonObject.toString())
        hourlyWeatherJSONString = filteredJsonObject.toString()
        println("printing hourly weather data")
        println(hourlyWeatherJSONString)
    }

    private fun sendAndReceiveMessageFromClaude(userMsg: String) : String {
        val ANTHROPIC_API_KEY = "YOUR_ANTHROPIC_KEY"
        val url = "https://api.anthropic.com/v1/messages"
        val headers = mapOf(
            "x-api-key" to ANTHROPIC_API_KEY,
            "anthropic-version" to "2023-06-01",
            "content-type" to "application/json"
        )

        val jsonBody = JSONObject().apply {
            put("model", "claude-3-opus-20240229")
            put("max_tokens", 500)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userMsg)
                })
            })
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())

        val client = OkHttpClient().newBuilder()
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()


        val request = Request.Builder()
            .url(url)
            .apply {
                headers.forEach { (key, value) ->
                    addHeader(key, value)
                }
            }
            .post(requestBody)
            .build()

        var resString = ""
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("Claude API", "Executing API request")
                Log.d("Claude API", "Request Headers: $headers")
                Log.d("Claude API", "Request Body: $jsonBody")
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d("Claude API", "API request successful")
                    val responseBody = response.body?.string()
                    val responseJSON = responseBody?.let { JSONObject(it) }
                    val contentArray = responseJSON?.getJSONArray("content")

                    if (contentArray != null && contentArray.length() > 0) {
                        val contentObject = contentArray.getJSONObject(0)
                        if (contentObject.has("text")) {
                            val contentText = contentObject.getString("text")
                            Log.d("Claude API", "Content Text: $contentText")
                            resString = contentText
                        } else {
                            Log.e("Claude API", "No 'text' field found in content array")
                        }
                    } else {
                        Log.e("Claude API", "Empty or null content array")
                    }

                } else {
                    Log.e("Claude API", "Error using Claude API: ${response.code} - ${response.message}")
                    val errorResponseBody = response.body?.string()
                    Log.e("Claude API", "Error Response: $errorResponseBody")
                }
            } catch (e: IOException) {
                Log.e("Claude API", "Error making API request: ${e.message}")
                e.printStackTrace()
            }
        }

        return resString
    }
}
