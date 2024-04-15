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
import android.widget.EditText
import android.widget.ImageButton


class AddOutfitFragment : Fragment() {
    private lateinit var imageView: ImageView
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var editText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize the ActivityResultLauncher
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap?
                imageBitmap?.let {
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
        editText = view.findViewById(R.id.editTextClothingDescription)

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
                        VisionModel.Feature(type = "LABEL_DETECTION", maxResults = 10),
                        VisionModel.Feature(type = "LOGO_DETECTION", maxResults = 5),
                        VisionModel.Feature(type = "IMAGE_PROPERTIES", maxResults = 1),
                        VisionModel.Feature(type = "TEXT_DETECTION", maxResults = 10)
                    )
                )
            )
        )

        visionService.annotateImage(request).enqueue(object : retrofit2.Callback<VisionModel.VisionResponse> {
            override fun onResponse(call: retrofit2.Call<VisionModel.VisionResponse>, response: retrofit2.Response<VisionModel.VisionResponse>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    val labels = responseBody?.responses?.firstOrNull()?.labelAnnotations
                    val logos = responseBody?.responses?.firstOrNull()?.logoAnnotations
                    val colors = responseBody?.responses?.firstOrNull()?.imagePropertiesAnnotation?.dominantColors?.colors
                    val texts = responseBody?.responses?.firstOrNull()?.textAnnotations

                    activity?.runOnUiThread {
                        val descriptions = labels?.joinToString(separator = ", ") { it.description }
                        val brandNames = logos?.joinToString(separator = ", ") { it.description }
                        val textDescriptions = texts?.joinToString(separator = " ") { it.description } // Concatenate all detected text
                        val dominantColor = colors?.maxByOrNull { it.pixelFraction }?.color?.let { color ->
                            "R: ${color.red}, G: ${color.green}, B: ${color.blue}"
                        }

                        editText.setText("Labels: $descriptions\nBrands: $brandNames\nText: $textDescriptions\nDominant Color: $dominantColor")
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
