package com.example.outfitoftheday

object VisionModel {
    data class VisionRequest(val requests: List<AnnotateImageRequest>)

    data class AnnotateImageRequest(
        val image: Image,
        val features: List<Feature>
    )

    data class Image(
        val content: String
    )

    data class Feature(
        val type: String = "LABEL_DETECTION",
        val maxResults: Int = 10
    )

    data class VisionResponse(
        val responses: List<ImageResponse>
    )

    data class ImageResponse(
        val labelAnnotations: List<LabelAnnotation>
    )

    data class LabelAnnotation(
        val description: String,
        val score: Float
    )
}
