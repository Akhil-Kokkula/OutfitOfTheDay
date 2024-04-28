package com.example.outfitoftheday

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ClothingItem(
    var id: String? = null,
    val label: String? = null,
    val color: String? = null,
    val brand: String? = null,
    val category: String? = null,
    val imageBase64: String? = null,
    val imageUrl: String? = null
)
