package com.example.habitsnap.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

data class VerificationResult(
    val isValid: Boolean,
    val labels: List<String>,
    val confidence: Float,
    val message: String
)

object HabitImageVerifier {

    private const val CONFIDENCE_THRESHOLD = 0.30f

    private val categoryKeywords = mapOf(
        "workout" to listOf(
            "person", "exercise", "sport", "gym", "fitness",
            "weights", "dumbbell", "running", "yoga", "muscle",
            "arm", "leg", "body", "athletic", "training"
        ),
        "reading" to listOf(
            "book", "paper", "text", "reading", "magazine",
            "literature", "library", "notebook", "publication",
            "document", "page", "print"
        ),
        "coding" to listOf(
            "laptop", "keyboard", "computer", "monitor",
            "screen", "technology", "desk", "gadget",
            "electronic", "device", "display", "typing"
        ),
        "meditation" to listOf(
            "person", "calm", "sitting", "yoga", "mat",
            "room", "floor", "indoor", "peaceful"
        ),
        "cooking" to listOf(
            "food", "kitchen", "cooking", "meal", "plate",
            "vegetable", "fruit", "dish", "ingredient",
            "pot", "pan", "stove", "eat"
        ),
        "walking" to listOf(
            "person", "road", "path", "street", "outdoor",
            "park", "nature", "tree", "sky", "walking",
            "shoe", "foot", "ground", "pavement"
        ),
        "sleep" to listOf(
            "bed", "bedroom", "pillow", "sleeping", "room",
            "furniture", "blanket", "sheet", "mattress"
        ),
        "water" to listOf(
            "water", "bottle", "drink", "glass", "liquid",
            "beverage", "cup", "container", "pink", "plastic",
            "drinkware", "tableware", "person", "hand",
            "drinking", "fluid", "clear", "transparent",
            "product", "indoor", "table", "holding"
        ),
        "other" to listOf(
            "person", "object", "activity", "indoor",
            "outdoor", "hand", "product", "room"
        )
    )

    suspend fun verify(bitmap: Bitmap, habitCategory: String): VerificationResult =
        suspendCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            val options = ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.30f)
                .build()
            val labeler = ImageLabeling.getClient(options)

            labeler.process(image)
                .addOnSuccessListener { labels ->
                    val detectedLabels = labels.map { it.text.lowercase() }
                    val topConfidence = labels.maxOfOrNull { it.confidence } ?: 0f

                    android.util.Log.d("MLKit", "Detected labels: $detectedLabels")
                    android.util.Log.d("MLKit", "Category: $habitCategory")

                    val keywords = categoryKeywords[habitCategory.lowercase()]
                        ?: categoryKeywords["other"]!!

                    // More relaxed matching
                    val matched = detectedLabels.any { label ->
                        keywords.any { kw ->
                            label.contains(kw) ||
                                    kw.contains(label) ||
                                    label.length > 3 && kw.length > 3 &&
                                    (label.contains(kw.take(4)) || kw.contains(label.take(4)))
                        }
                    }

                    val result = if (matched) {
                        VerificationResult(
                            isValid = true,
                            labels = detectedLabels,
                            confidence = topConfidence,
                            message = "✅ Image verified!"
                        )
                    } else {
                        VerificationResult(
                            isValid = false,
                            labels = detectedLabels,
                            confidence = topConfidence,
                            message = "❌ Image does not match the habit. Please capture valid proof.\nDetected: ${detectedLabels.take(5)}"
                        )
                    }
                    cont.resume(result)
                }
                .addOnFailureListener { e ->
                    // If ML fails, allow check-in
                    cont.resume(VerificationResult(
                        isValid = true,
                        labels = emptyList(),
                        confidence = 0f,
                        message = "✅ Check-in accepted!"
                    ))
                }
        }
}
