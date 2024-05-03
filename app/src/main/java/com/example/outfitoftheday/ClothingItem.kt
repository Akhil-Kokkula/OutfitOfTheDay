package com.example.outfitoftheday

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ClothingItem(
    var id: String? = null,
    var label: String? = null,
    var color: String? = null,
    var brand: String? = null,
    var category: String? = null,
    var imageBase64: String? = null,
    var imageUrl: String? = null
)