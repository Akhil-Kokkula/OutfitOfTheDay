package com.example.outfitoftheday

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ClothingItem(
    @Exclude var id: String? = "", // Exclude the id from Firebase as it's often used as a key
    var name: String? = "",
    var imageUrl: String? = ""
)
