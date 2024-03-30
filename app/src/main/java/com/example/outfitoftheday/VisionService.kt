package com.example.outfitoftheday

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface VisionService {
    @POST("v1/images:annotate?key=AIzaSyAJQCsD5Iq9b_t2Ecv0q3VMrPqUFNg7xzc")
    fun annotateImage(@Body request: VisionModel.VisionRequest): Call<VisionModel.VisionResponse>
}
