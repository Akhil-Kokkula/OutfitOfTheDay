package com.example.outfitoftheday
import com.google.firebase.database.Exclude

// for database
data class ClothingItem(
    @get:Exclude var id: String? = null,
    var name: String = "",
    var imageUrl: String = ""
)

