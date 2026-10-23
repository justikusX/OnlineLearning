package com.example.onlinelearning.model.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * Turns a picked photo into a small square JPEG stored as Base64 in the user's Firestore document
 * (about 20–40 KB), so the avatar follows the account without Firebase Storage.
 */
object AvatarCodec {

    private const val SIZE = 256
    private const val QUALITY = 85

    suspend fun encode(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        // 1. Read only the dimensions to pick a sample size, so huge camera photos are not fully decoded.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Не удалось прочитать изображение" }
        var sample = 1
        while (min(bounds.outWidth, bounds.outHeight) / (sample * 2) >= SIZE) sample *= 2

        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Не удалось прочитать изображение")

        // 2. Respect the camera's EXIF rotation.
        val rotation = resolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        // 3. Center-crop to a square and scale to SIZE x SIZE.
        val side = min(decoded.width, decoded.height)
        val scale = SIZE.toFloat() / side
        val matrix = Matrix().apply {
            postScale(scale, scale)
            if (rotation != 0f) postRotate(rotation)
        }
        val square = Bitmap.createBitmap(
            decoded,
            (decoded.width - side) / 2,
            (decoded.height - side) / 2,
            side,
            side,
            matrix,
            true,
        )

        val out = ByteArrayOutputStream()
        square.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
        if (square !== decoded) square.recycle()
        decoded.recycle()
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    fun decode(base64: String): Bitmap? = runCatching {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}
