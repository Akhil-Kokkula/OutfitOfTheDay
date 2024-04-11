package com.example.outfitoftheday

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface VisionService {
    @POST("v1/images:annotate?key=KEY_HERE")
    fun annotateImage(@Body request: VisionModel.VisionRequest): Call<VisionModel.VisionResponse>
}
