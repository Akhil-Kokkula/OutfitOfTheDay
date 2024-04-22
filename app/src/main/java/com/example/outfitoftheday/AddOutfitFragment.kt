package com.example.outfitoftheday

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID


class AddOutfitFragment : Fragment() {
    private lateinit var imageView: ImageView
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var editTextLabel: EditText
    private lateinit var editTextColor: EditText
    private lateinit var editTextBrand: EditText
    private val allowedClothingTypes = listOf(
        "T-shirt", "skirt", "shoes", "pants", "dress", "shirt", "jacket", "hat",
        "blouse", "sweater", "shorts", "socks", "scarf", "coat", "jeans",
        "boots", "sandals", "cap", "gloves", "belt", "tie", "swimsuit",
        "underwear", "leggings", "pullover"
    )
    private lateinit var buttonSubmit: Button
    private var capturedImageBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap?
                imageBitmap?.let {
                    capturedImageBitmap = it
                    imageView.setImageBitmap(it)
                    analyzeImage(imageBitmap)
                } ?: Toast.makeText(requireContext(), "Failed to capture image!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_add_outfit, container, false)
        imageView = view.findViewById(R.id.imageViewCaptured)
        val buttonCapture: ImageButton = view.findViewById(R.id.buttonCapture)
        editTextLabel = view.findViewById(R.id.editTextLabel)
        editTextColor = view.findViewById(R.id.editTextColor)
        editTextBrand = view.findViewById(R.id.editTextBrand)
        buttonSubmit = view.findViewById(R.id.buttonAddToWardrobe)

        buttonSubmit.setOnClickListener {
            submitOutfitData()
        }

        buttonCapture.setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                takePictureLauncher.launch(takePictureIntent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Camera not available.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun submitOutfitData() {
        val label = editTextLabel.text.toString().trim()
        val color = editTextColor.text.toString().trim()
        val brand = editTextBrand.text.toString().trim()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        if (label.isEmpty() || color.isEmpty() || brand.isEmpty() || capturedImageBitmap == null) {
            Toast.makeText(context, "Please fill in all fields and capture an image", Toast.LENGTH_SHORT).show()
            return
        }

        val imageBase64 = encodeImageToBase64(capturedImageBitmap!!)
        val outfit = hashMapOf(
            "label" to label,
            "color" to color,
            "brand" to brand,
            "imageBase64" to imageBase64
        )

        val databaseReference = FirebaseDatabase.getInstance().getReference("users/$userId/outfits")
        databaseReference.push().setValue(outfit).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Outfit added successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to add outfit", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun setupRetrofit(): VisionService {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://vision.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()

        return retrofit.create(VisionService::class.java)
    }
    fun analyzeImage(imageBitmap: Bitmap) {
        val visionService = setupRetrofit()
        val byteArrayOutputStream = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
        val base64Image = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)

        val request = VisionModel.VisionRequest(
            requests = listOf(
                VisionModel.AnnotateImageRequest(
                    image = VisionModel.Image(content = base64Image),
                    features = listOf(
                        VisionModel.Feature(type = "LABEL_DETECTION", maxResults = 20),
                        VisionModel.Feature(type = "LOGO_DETECTION", maxResults = 5),
                        VisionModel.Feature(type = "IMAGE_PROPERTIES", maxResults = 1),
                        VisionModel.Feature(type = "TEXT_DETECTION", maxResults = 5)
                    )
                )
            )
        )

        visionService.annotateImage(request).enqueue(object : retrofit2.Callback<VisionModel.VisionResponse> {
            override fun onResponse(call: retrofit2.Call<VisionModel.VisionResponse>, response: retrofit2.Response<VisionModel.VisionResponse>) {
                if (response.isSuccessful) {
                    response.body()?.responses?.firstOrNull()?.let {
                        val gson = GsonBuilder().setPrettyPrinting().create()
                        Log.d("AddOutfitFragment", "Vision API Response: ${gson.toJson(it)}")
                        val descriptions = it.labelAnnotations?.mapNotNull { label ->
                            label.description.toLowerCase(Locale.ROOT)
                        }?.filter { desc ->
                            allowedClothingTypes.any { type -> type.equals(desc, ignoreCase = true) }
                        }?.joinToString(", ")

                        val brandNames = it.logoAnnotations?.mapNotNull { logo -> logo.description }?.joinToString(", ")
                        val textDescriptions = it.textAnnotations?.mapNotNull { text -> text.description }?.joinToString(" ")

                        val dominantColor = it.imagePropertiesAnnotation?.dominantColors?.colors?.maxByOrNull { color -> color.pixelFraction }
                            ?.color?.let { color -> "R: ${color.red}, G: ${color.green}, B: ${color.blue}" }

                        activity?.runOnUiThread {
                            editTextLabel.setText(descriptions ?: "No label detected")
                            editTextColor.setText(dominantColor ?: "No color detected")

                            editTextBrand.setText(listOfNotNull(brandNames, textDescriptions).joinToString(" ").ifBlank { "No brand or text detected" })
                        }
                    }
                } else {
                    Log.e("AddOutfitFragment", "API Error Response: ${response.errorBody()?.string()}")
                    Toast.makeText(context, "API request failed with code: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<VisionModel.VisionResponse>, t: Throwable) {
                Toast.makeText(context, "API request failed: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }


}
