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
        val type: String,
        val maxResults: Int
    )

    data class VisionResponse(
        val responses: List<ImageResponse>
    )

    data class ImageResponse(
        val labelAnnotations: List<LabelAnnotation>? = null,
        val logoAnnotations: List<LogoAnnotation>? = null,
        val imagePropertiesAnnotation: ImagePropertiesAnnotation? = null,
        val textAnnotations: List<TextAnnotation>? = null // Add textAnnotations to handle text detection
    )

    data class LabelAnnotation(
        val description: String,
        val score: Float
    )

    data class LogoAnnotation(
        val description: String,
        val score: Float
    )

    data class TextAnnotation(
        val description: String, // Contains the entire text detected
        val boundingPoly: BoundingPoly // The polygon around the detected text
    )

    data class BoundingPoly(
        val vertices: List<Vertex> // Coordinates of the bounding polygon for text
    )

    data class Vertex(
        val x: Int,
        val y: Int
    )

    data class ImagePropertiesAnnotation(
        val dominantColors: DominantColors
    )

    data class DominantColors(
        val colors: List<ColorInfo>
    )

    data class ColorInfo(
        val color: Color,
        val pixelFraction: Float,
        val score: Float
    )

    data class Color(
        val red: Int,
        val green: Int,
        val blue: Int,
        val alpha: Int
    )
}
