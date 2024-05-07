package com.example.outfitoftheday

import VisionModel
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface VisionService {
    @POST("v1/images:annotate?key=${BuildConfig.VISION_API_KEY}")
    fun annotateImage(@Body request: VisionModel.VisionRequest): Call<VisionModel.VisionResponse>
}
