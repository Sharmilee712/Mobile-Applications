package com.example.habitsnap.utils

import android.content.Context
import android.graphics.*
import android.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object CameraUtils {

    fun createPhotoFile(context: Context, habitId: Long): File {
        val dir = File(context.filesDir, "habit_photos").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(dir, "HABIT_${habitId}_$timestamp.jpg")
    }

    fun stampTimestampOnImage(filePath: String) {
        try {
            val original = BitmapFactory.decodeFile(filePath) ?: return
            val mutable = original.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutable)
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            val stripPaint = Paint().apply {
                color = Color.argb(160, 0, 0, 0)
            }
            val stripHeight = (mutable.height * 0.08f).coerceAtLeast(60f)
            canvas.drawRect(
                0f,
                mutable.height - stripHeight,
                mutable.width.toFloat(),
                mutable.height.toFloat(),
                stripPaint
            )

            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = stripHeight * 0.45f
                isAntiAlias = true
                typeface = Typeface.MONOSPACE
            }
            canvas.drawText(
                "HabitSnap  $now",
                16f,
                mutable.height - stripHeight * 0.25f,
                textPaint
            )

            FileOutputStream(filePath).use { out ->
                mutable.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            mutable.recycle()
            original.recycle()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun rotateBitmapIfRequired(filePath: String, bitmap: Bitmap): Bitmap {
        val exif = ExifInterface(filePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}