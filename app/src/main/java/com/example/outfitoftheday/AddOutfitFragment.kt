package com.example.outfitoftheday

import VisionModel
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.math.sqrt


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
        "underwear", "leggings", "pullover", "athletic", "formal",
        "cargo pants", "hoodie", "cardigan", "blazer", "bikini", "halter top",
        "high heels", "loafers", "suit", "tuxedo", "vest", "windbreaker",
        "yoga pants", "kimono", "jumpsuit", "sari", "kilt", "toga", "sarong"
    )
    private lateinit var buttonSubmit: Button
    private var capturedImageBitmap: Bitmap? = null
    data class NamedColor(val name: String, val r: Int, val g: Int, val b: Int)

    private val colorList = listOf(
        NamedColor("Red", 255, 0, 0),
        NamedColor("Green", 0, 255, 0),
        NamedColor("Blue", 0, 0, 255),
        NamedColor("Black", 0, 0, 0),
        NamedColor("White", 255, 255, 255),
        NamedColor("Orange", 255, 165, 0),
        NamedColor("Yellow", 255, 255, 0),
        NamedColor("Purple", 128, 0, 128),
        NamedColor("Grey", 128, 128, 128),
        NamedColor("Brown", 165, 42, 42),
        NamedColor("Magenta", 255, 0, 255),
        NamedColor("Tan", 210, 180, 140),
        NamedColor("Cyan", 0, 255, 255),
        NamedColor("Olive", 128, 128, 0),
        NamedColor("Maroon", 128, 0, 0),
        NamedColor("Navy", 0, 0, 128),
        NamedColor("Aquamarine", 127, 255, 212),
        NamedColor("Turquoise", 64, 224, 208),
        NamedColor("Silver", 192, 192, 192),
        NamedColor("Lime", 0, 255, 0),
        NamedColor("Coral", 255, 127, 80),
        NamedColor("Salmon", 250, 128, 114),
        NamedColor("Pink", 255, 192, 203)
    )

    private fun getClosestColorName(r: Int, g: Int, b: Int): String {
        return colorList.minByOrNull { color ->
            // Calculate Euclidean distance between the color parameter and each color in the list
            sqrt(((color.r - r) * (color.r - r) + (color.g - g) * (color.g - g) + (color.b - b) * (color.b - b)).toDouble())
        }?.name ?: "Unknown Color"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap?
                imageBitmap?.let {
                    capturedImageBitmap = it
                    imageView.setImageBitmap(it)
                    analyzeImage(imageBitmap)
                } ?: Toast.makeText(requireContext(), getString(R.string.addClothesFail), Toast.LENGTH_SHORT).show()
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

        arguments?.let {
            val itemId = it.getString("item_id")
            if (itemId != null) {
                // Set up the fragment in modify mode
                editTextLabel.setText(it.getString("label"))
                editTextColor.setText(it.getString("color"))
                editTextBrand.setText(it.getString("brand"))
                // Load the image if available
                it.getString("imageBase64")?.let { base64 ->
                    val imageBytes = Base64.decode(base64, Base64.DEFAULT)
                    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    imageView.setImageBitmap(decodedImage)
                    capturedImageBitmap = decodedImage
                } ?: it.getString("imageUrl")?.let { imageUrl ->
                    Glide.with(this).load(imageUrl).into(imageView)
                }

                buttonSubmit.text = "Modify Clothing Item"
                buttonSubmit.setOnClickListener {
                    modifyOutfitData(itemId)
                }
            } else {
                // Set up the fragment in add mode
                buttonSubmit.text = getString(R.string.addClothes_buttonText)
                buttonSubmit.setOnClickListener {
                    submitOutfitData()
                }
            }
        } ?: run {
            // Default to add mode if no arguments are passed
            buttonSubmit.text = getString(R.string.addClothes_buttonText)
            buttonSubmit.setOnClickListener {
                submitOutfitData()
            }
        }

        buttonCapture.setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                takePictureLauncher.launch(takePictureIntent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), getString(R.string.addClothesCameraUnavailable), Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun modifyOutfitData(itemId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Extract data from input fields
        val label = editTextLabel.text.toString().trim()
        val color = editTextColor.text.toString().trim()
        val brand = editTextBrand.text.toString().trim()

        if (label.isEmpty() || color.isEmpty() || brand.isEmpty() || capturedImageBitmap == null) {
            Toast.makeText(context, "Please fill all fields and capture an image.", Toast.LENGTH_SHORT).show()
            return
        }

        val imageBase64 = encodeImageToBase64(capturedImageBitmap!!)
        val category = getCategoryFromLabel(label)

        // Create a map of data to update
        val outfitUpdates = mapOf(
            "label" to label,
            "color" to color,
            "brand" to brand,
            "category" to category,
            "imageBase64" to imageBase64
        )

        // Reference to the specific outfit item in Firebase
        val outfitRef = FirebaseDatabase.getInstance().getReference("users/$userId/outfits/$itemId")

        // Update the data in Firebase
        outfitRef.updateChildren(outfitUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Outfit updated successfully!", Toast.LENGTH_SHORT).show()
                // Optionally navigate back or clear the form
            } else {
                Toast.makeText(context, "Failed to update outfit.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCategoryFromLabel(label: String): String {
        val normalizedLabel = label.toLowerCase(Locale.ROOT).trim()
        return when {
            "t-shirt" in normalizedLabel || "shirt" in normalizedLabel || "blouse" in normalizedLabel || "jacket" in normalizedLabel || "coat" in normalizedLabel || "hoodie" in normalizedLabel || "cardigan" in normalizedLabel || "blazer" in normalizedLabel -> "Tops"
            "skirt" in normalizedLabel || "shorts" in normalizedLabel || "leggings" in normalizedLabel -> "Bottoms"
            "pant" in normalizedLabel || "jeans" in normalizedLabel -> "Bottoms"
            "hat" in normalizedLabel || "cap" in normalizedLabel -> "Hats"
            "shoes" in normalizedLabel || "boots" in normalizedLabel || "sandals" in normalizedLabel || "high heels" in normalizedLabel || "loafers" in normalizedLabel-> "Footwear"
            // Add more conditions as necessary
            else -> "Miscellaneous"
        }
    }

    private fun submitOutfitData() {
        val label = editTextLabel.text.toString().trim()
        val color = editTextColor.text.toString().trim()
        val brand = editTextBrand.text.toString().trim()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        if (label.isEmpty() || color.isEmpty() || brand.isEmpty() || capturedImageBitmap == null) {
            Toast.makeText(context, getString(R.string.addClothesAllInputsNeeded), Toast.LENGTH_SHORT).show()
            return
        }

        val imageBase64 = encodeImageToBase64(capturedImageBitmap!!)
        val category = getCategoryFromLabel(label) // Get category based on label
        val outfit = hashMapOf(
            "label" to label,
            "color" to color,
            "brand" to brand,
            "category" to category,  // Include the automatically determined category
            "imageBase64" to imageBase64
        )

        val databaseReference = FirebaseDatabase.getInstance().getReference("users/$userId/outfits")
        databaseReference.push().setValue(outfit).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, getString(R.string.addClothesSuccess), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, getString(R.string.addClothesFailToAdd), Toast.LENGTH_SHORT).show()
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

                        val descriptions = it.labelAnnotations?.map { label ->
                            label.description.toLowerCase(Locale.ROOT)
                        }?.filter { desc ->
                            allowedClothingTypes.any { type -> type.equals(desc, ignoreCase = true) }
                        }?.joinToString(", ")

                        val brandNames = it.logoAnnotations?.map { logo -> logo.description }?.joinToString(", ")
                        val textDescriptions = it.textAnnotations?.map { text -> text.description }?.joinToString(" ")

                        // Extracting dominant color and converting it to a named color
                        val dominantColorInfo = it.imagePropertiesAnnotation?.dominantColors?.colors?.maxByOrNull { color -> color.pixelFraction }
                        val closestColorName = dominantColorInfo?.color?.let { color ->
                            getClosestColorName(color.red, color.green, color.blue) // Converting RGB to color name
                        } ?: "No color detected"

                        activity?.runOnUiThread {
                            editTextLabel.setText(descriptions ?: "No label detected")
                            editTextColor.setText(closestColorName) // Using the named color

                            val brandText = if (brandNames == textDescriptions) brandNames else listOfNotNull(brandNames, textDescriptions).joinToString(" ").ifBlank { "No brand or text detected" }
                            editTextBrand.setText(brandText)
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
