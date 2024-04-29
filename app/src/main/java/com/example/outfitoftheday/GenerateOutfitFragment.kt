package com.example.outfitoftheday

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.outfitoftheday.Constants.PREF_ACCOUNT_NAME
import com.example.outfitoftheday.Constants.REQUEST_ACCOUNT_PICKER
import com.example.outfitoftheday.Constants.REQUEST_AUTHORIZATION
import com.example.outfitoftheday.Constants.REQUEST_GOOGLE_PLAY_SERVICES
import com.example.outfitoftheday.Constants.REQUEST_PERMISSION_GET_ACCOUNTS
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import pub.devrel.easypermissions.EasyPermissions
import java.io.IOException
import java.util.concurrent.TimeUnit



class GenerateOutfitFragment : Fragment() {

    private lateinit var locationManager: LocationManager
    private lateinit var outfitPhotoGallery : RecyclerView
    private lateinit var outfitGalleryAdapter : OutfitGalleryAdapter
    private lateinit var occasionInputText : EditText
    private lateinit var durationInputText : EditText
    private lateinit var weatherJSONObject : JSONObject
    private lateinit var hourlyWeatherJSONString : String
    private lateinit var wardrobeList : MutableList<ClothingItem>
    private lateinit var loadingIndicator: CircularProgressIndicator
    private var latitude = 0.0
    private var longitude = 0.0
    private lateinit var weatherTextView: TextView
    private lateinit var generateOutfitButton: Button
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    var weatherDataForHomePage = ""
    private val weatherDataCallbacks = mutableListOf<(String) -> Unit>()
    val weatherDataForHomePageLiveData = MutableLiveData<String>()



    //Weather Variables
    private val client = OkHttpClient()
    private val locationAPIKEY = "O4YfE6O0SCM3xZvGwTbrlqaT0xyGm6ZU"
    private var weatherTemp = 0.0
    private var weatherPrecipitationProbability = 0.0
    private var weatherHumidity = 0.0

    //Get Calendar Information
    private var mCredential: GoogleAccountCredential? = null //to access our account
    private var mService: Calendar? = null //To access the calendar
    var mProgress: ProgressDialog? = null
    private lateinit var calendarButton: Button
    private var stringOfEvents = ""

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_generate_outfit, container, false)

        weatherTextView = view.findViewById(R.id.weatherTextView)
        generateOutfitButton = view.findViewById(R.id.generateOutfitButton)
        calendarButton = view.findViewById(R.id.loadCalendarButton)
        occasionInputText = view.findViewById<TextInputLayout>(R.id.tilOccasion).editText!!
        durationInputText = view.findViewById<TextInputLayout>(R.id.tilDuration).editText!!
        loadingIndicator = view.findViewById(R.id.loadingIndicator)
        loadingIndicator.visibility = View.GONE

        // Initialize LocationManager
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        outfitPhotoGallery = view.findViewById(R.id.rvOutfitGallery)
        outfitPhotoGallery.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)



        auth = FirebaseAuth.getInstance()
        setupFirebaseDatabase()

        outfitGalleryAdapter = OutfitGalleryAdapter(mutableListOf())
        outfitPhotoGallery.adapter = outfitGalleryAdapter

        generateOutfitButton.setOnClickListener {
            generateOutfitAction()
        }

        //Find Calendar Information
        calendarButton.setOnClickListener {
            getResultsFromApi()
        }


        // Check for location permission
        //startLocationGathering()

        //Calendar Information Below:
        mProgress = ProgressDialog(requireContext())
        mProgress!!.setMessage("Loading...")
        initCredentials()

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        startLocationGathering()
    }


    private fun startLocationGathering() {
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Request location updates
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
        } else {
            //Ask user for permission and then load location
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    fun startLocationGatheringForHome() {
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // Simulate gathering weather data asynchronously
        CoroutineScope(Dispatchers.Main).launch {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Request location updates
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
            } else {
                //Ask user for permission and then load location
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun setupFirebaseDatabase() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("GenerateOutfitFragment", "User is not logged in.")
            return
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("users/$userId/outfits")
    }

    private fun generateOutfitAction() {
        if (occasionInputText.text.toString() == "") {
            Toast.makeText(context, "Please enter your preferred event for the day", Toast.LENGTH_SHORT).show()
        } else if (durationInputText.text.toString() == "" || durationInputText.text.toString().toInt() == 0) {
            Toast.makeText(context, "Please enter how long you will wear the outfit for in hours", Toast.LENGTH_SHORT).show()
        } else if (weatherJSONObject == null) {
            Toast.makeText(context, "Please wait until weather information is loaded and try again", Toast.LENGTH_SHORT).show()
        } else {
            loadingIndicator.visibility = View.VISIBLE
            getHourlyWeatherData()
            val aiTextStrBuilder = StringBuilder()
            aiTextStrBuilder.append("You are a fashion stylist and you must give the user a full outfit for the day. ")
            aiTextStrBuilder.append("Here is the expected output format you need to provide and please only answer in this format:\n" +
                    "item -NvcyBYui5a1z-UH8Yfh: T-shirt, White, Casual\n" +
                    "item -NvcyBYui5a1z-UH7Ghy: Jeans, Light Blue, Casual\n" +
                    "item -NvcyBYui5a1z-UH6Ynh: Sneakers, White, Casual\n" +
                    "item -NvcyBYui5a1z-UH5Gfl: Aviator Sunglasses, Black, Casual\n\n")
            aiTextStrBuilder.append("Weather Information:\n")
            aiTextStrBuilder.append(hourlyWeatherJSONString)
            aiTextStrBuilder.append("\n\n")
            aiTextStrBuilder.append("The most important event of the user's day:\n")
            aiTextStrBuilder.append(occasionInputText.text.toString())
            aiTextStrBuilder.append("\n\n")

            //Calendar Information:
            aiTextStrBuilder.append("The itinerary for the user today:\n")
            aiTextStrBuilder.append(stringOfEvents)
            aiTextStrBuilder.append("\n\n")

            aiTextStrBuilder.append("User wardrobe:\n")
            //hardcoded now
//            aiTextStrBuilder.append("item1: Crop Top, Pink, Casual\n" +
//                    "item2: High-Waisted Jeans, Medium Wash, Casual\n" +
//                    "item3: Off-Shoulder Blouse, Floral Print, Casual\n" +
//                    "item4: Midi Skirt, Beige, Casual\n" +
//                    "item5: Leather Moto Jacket, Black, Casual\n" +
//                    "item6: Suede Jacket, Camel, Casual\n" +
//                    "item7: Knit Sweater Dress, Burgundy, Casual\n" +
//                    "item8: Pullover Hoodie, Gray, Casual\n" +
//                    "item9: Blazer, Navy Blue, Formal/Business\n" +
//                    "item10: Slip-on Sneakers, Pastel Pink, Casual\n" +
//                    "item11: Pointed Toe Flats, Nude, Formal\n" +
//                    "item12: Ankle Boots, Black, Casual/Formal\n" +
//                    "item13: Skinny Belt, Red, Casual/Formal\n" +
//                    "item14: Chain Belt, Gold, Casual/Formal\n" +
//                    "item15: Rose Gold Watch, Rose Gold, Casual/Formal\n" +
//                    "item16: Leather Cuff Bracelet, Brown, Casual/Formal\n" +
//                    "item17: Cat-eye Sunglasses, Tortoise Shell, Casual\n" +
//                    "item18: Round Sunglasses, Gold Frame, Casual\n" +
//                    "item19: Yoga Leggings, Black, Sportswear\n" +
//                    "item20: Sports Bra, Purple, Sportswear\n" +
//                    "item21: Rain Jacket, Yellow, Casual\n" +
//                    "item22: Puffer Jacket, Navy Blue, Casual/Formal\n")

            fetchDataFromDatabase { items ->
                //print(items)
                wardrobeList = items
                for (clothingItem in items) {
                    //println("item ${clothingItem.id}: ${clothingItem.label}, ${clothingItem.color}, ${clothingItem.brand}")
                    aiTextStrBuilder.append("item ${clothingItem.id}: ${clothingItem.label}, ${clothingItem.color}, ${clothingItem.brand}\n")
                }

                aiTextStrBuilder.append("\n\n")
                aiTextStrBuilder.append("Please generate an outfit for me")



                val aiTextStr = aiTextStrBuilder.toString()
                println("will send to ai")
                println(aiTextStr)

                sendAndReceiveMessageFromClaude(aiTextStr) { outfitAIResponseStr ->
                    val regex = Regex("""item ([\w-]+):""")
                    val clothingIds = outfitAIResponseStr
                        .split("\n")
                        .filter { it.startsWith("item") }
                        .mapNotNull { regex.find(it)?.groupValues?.get(1) }
                    println(clothingIds)

                    val outfitImages = mutableListOf<ClothingItem>()
                    wardrobeList.forEach { item ->
                        if (item.id in clothingIds) {
                            outfitImages.add(item)
                        }
                    }
                    //println(outfitImages)

                    GlobalScope.launch(Dispatchers.Main) {
                        loadingIndicator.visibility = View.GONE
                        outfitGalleryAdapter.updatingOutfitList(outfitImages)
                        outfitPhotoGallery.scrollToPosition(0)
                    }


                }



//                outfitAIResponse = "item -Nw66nMmYtivfPwHCT7Y: Sports shirt, Gray, Gym\n" +
//                "item -NvcyBYui5a1z-UH8Yfh: Shorts, Gray, Gym\n" +
//                "item -NvcydVXq6oJmBGjECkR: Sneakers, Gray, Gym"







            }



//            print("received ai response")
//            print(outfitAIResponse)
//            Log.d("AI Request",  aiTextStr)

        }
    }

    private fun fetchDataFromDatabase(callback: (MutableList<ClothingItem>) -> Unit) {
        val items = mutableListOf<ClothingItem>()
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val item = snapshot.getValue(ClothingItem::class.java)
                    item?.let {
                        it.id = snapshot.key
                        items.add(it)
                    }
                }
                // Call the callback function with the fetched items
                //println(items)
                callback(items)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("GenerateOutfitFragment", "Failed to read wardrobe data", databaseError.toException())
                callback(mutableListOf<ClothingItem>())
            }
        })
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
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
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
                        weatherTextView.post {
                            if (isAdded) {
                                weatherDataForHomePageLiveData.value = weatherDataForHomePage
                                Log.d("Weather", weatherDataForHomePage)
                                weatherTextView.text = "Weather Information Added!"
                            }
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

    private fun sendAndReceiveMessageFromClaude(userMsg: String, callback: (String) -> Unit){
        val ANTHROPIC_API_KEY = ""
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
                            callback(contentText)
                        } else {
                            Log.e("Claude API", "No 'text' field found in content array")
                            loadingIndicator.visibility = View.GONE
                            Toast.makeText(context, "Failed to generate outfit. Please try again!", Toast.LENGTH_LONG)
                        }
                    } else {
                        Log.e("Claude API", "Empty or null content array")
                        loadingIndicator.visibility = View.GONE
                        Toast.makeText(context, "Failed to generate outfit. Please try again!", Toast.LENGTH_LONG)
                    }

                } else {
                    Log.e("Claude API", "Error using Claude API: ${response.code} - ${response.message}")
                    val errorResponseBody = response.body?.string()
                    Log.e("Claude API", "Error Response: $errorResponseBody")
                    loadingIndicator.visibility = View.GONE
                    Toast.makeText(context, "Failed to generate outfit. Please try again!", Toast.LENGTH_LONG)
                }
            } catch (e: IOException) {
                Log.e("Claude API", "Error making API request: ${e.message}")
                e.printStackTrace()
                loadingIndicator.visibility = View.GONE
                Toast.makeText(context, "Failed to generate outfit. Please try again!", Toast.LENGTH_LONG)
            }
        }

    }

    //BELOW IS CALENDAR INFORMATION/FUNCTIONS
    //Citation: ChatGPT and Tutorial (https://medium.com/@eneskocerr/get-events-to-your-android-app-using-google-calendar-api-4411119cd586)
    //Tutorial had missing code segments, had to develop those myself.

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_GOOGLE_PLAY_SERVICES -> if (resultCode != Activity.RESULT_OK) {

            } else {
                getResultsFromApi()
            }
            REQUEST_ACCOUNT_PICKER -> if (resultCode == Activity.RESULT_OK && data != null &&
                data.extras != null
            ) {
                val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                if (accountName != null) {
                    val settings = this.activity?.getPreferences(Context.MODE_PRIVATE)
                    val editor = settings?.edit()
                    editor?.putString(PREF_ACCOUNT_NAME, accountName)
                    editor?.apply()
                    mCredential!!.selectedAccountName = accountName
                    getResultsFromApi()
                }
            }
            REQUEST_AUTHORIZATION -> if (resultCode == Activity.RESULT_OK) {
                getResultsFromApi()
            }
        }
    }

    private fun initCredentials() {
        mCredential = GoogleAccountCredential.usingOAuth2(
            requireContext(),
            arrayListOf(CalendarScopes.CALENDAR)
        )
            .setBackOff(ExponentialBackOff())
        Log.d("Google", mCredential.toString())
        initCalendarBuild(mCredential)
    }

    private fun initCalendarBuild(credential: GoogleAccountCredential?) {
        val transport = AndroidHttp.newCompatibleTransport()
        val jsonFactory = JacksonFactory.getDefaultInstance()
        mService = Calendar.Builder(
            transport, jsonFactory, credential
        )
            .setApplicationName("GetEventCalendar")
            .build()
    }

    private fun getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices()
        } else if (mCredential!!.selectedAccountName == null) {
            chooseAccount()
            Log.d("Google", "Here")
        } else if (!isDeviceOnline()) {
            //binding.txtOut.text = "No network connection available."
        } else {
            makeRequestTask()
        }
    }

    private fun chooseAccount() {
        if (EasyPermissions.hasPermissions(
                requireContext(), Manifest.permission.GET_ACCOUNTS
            )
        ) {
            val accountName = this.activity?.getPreferences(Context.MODE_PRIVATE)
                ?.getString(PREF_ACCOUNT_NAME, null)
            if (accountName != null) {
                mCredential!!.selectedAccountName = accountName
                Log.d("Google", accountName)
                getResultsFromApi()
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                    mCredential!!.newChooseAccountIntent(),
                    REQUEST_ACCOUNT_PICKER
                )
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            Log.d("Test", "We enter this")
            EasyPermissions.requestPermissions(
                this,
                "This app needs to access your Google account (via Contacts).",
                REQUEST_PERMISSION_GET_ACCOUNTS,
                Manifest.permission.GET_ACCOUNTS
            )
            calendarButton.text = "Click again to load calendar!"
        }
    }


    private fun isGooglePlayServicesAvailable(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(requireContext())
        return connectionStatusCode == ConnectionResult.SUCCESS
    }

    private fun acquireGooglePlayServices() {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(requireContext())
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode)
        }
    }

    fun showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode: Int) {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val dialog = apiAvailability.getErrorDialog(
            this,
            connectionStatusCode,
            REQUEST_GOOGLE_PLAY_SERVICES
        )
        dialog?.show()
    }

    private fun isDeviceOnline(): Boolean {
        val connMgr =
            this.activity?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun makeRequestTask() {
        var mLastError: Exception? = null

        lifecycleScope.executeAsyncTask(
            onStart = {
                mProgress!!.show()
            },
            doInBackground = {
                try {
                    getDataFromCalendar()
                } catch (e: Exception) {
                    mLastError = e
                    lifecycleScope.cancel()
                    null
                }
            },
            onPostExecute = { output ->
                mProgress!!.hide()
                if (output == null || output.size == 0) {
                    Log.d("Google", "No Calendar Information")
                } else {
                    for (index in 0 until output.size) {
                        stringOfEvents = stringOfEvents +  output[index].summary + "\n"
                    }
                    Log.d("Google", "These are your events for the day: \n$stringOfEvents")
                    calendarButton.text = "Calendar Added!"
                    calendarButton.isEnabled = false
                }
            },
            onCancelled = {
                mProgress!!.hide()
                if (mLastError != null) {
                    if (mLastError is GooglePlayServicesAvailabilityIOException) {
                        showGooglePlayServicesAvailabilityErrorDialog(
                            (mLastError as GooglePlayServicesAvailabilityIOException)
                                .connectionStatusCode
                        )
                    } else if (mLastError is UserRecoverableAuthIOException) {
                        this.startActivityForResult(
                            (mLastError as UserRecoverableAuthIOException).intent,
                            REQUEST_AUTHORIZATION
                        )
                    } else {
                        //binding.txtOut.text =
                        //  "The following error occurred:\n" + mLastError!!.message
                    }
                } else {
                    //binding.txtOut.text = "Request cancelled."
                }
            }
        )
    }

    fun getDataFromCalendar(): MutableList<GetEventModel> {
        val now = DateTime(System.currentTimeMillis())
        val end = DateTime(System.currentTimeMillis() + 86400000)
        val eventStrings = ArrayList<GetEventModel>()


        try {
            val events = mService!!.events().list("primary")
                .setMaxResults(10)
                .setTimeMin(now)
                .setTimeMax(end)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute()
            val items = events.items

            for (event in items) {
                var start = event.start.dateTime
                if (start == null) {
                    start = event.start.date
                }

                eventStrings.add(
                    GetEventModel(
                        summary = event.summary,
                        startDate = start.toString()
                    )
                )
            }
            return eventStrings

        } catch (e: IOException) {
            if (e is UserRecoverableAuthIOException) {
                startActivityForResult(e.intent, REQUEST_AUTHORIZATION);
            } else {
                // other cases
            }
        }
        return eventStrings
    }


}