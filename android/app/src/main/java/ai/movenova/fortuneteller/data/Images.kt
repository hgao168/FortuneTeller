package ai.movenova.fortuneteller.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** Image plumbing: EXIF-correct decode, JPEG compression to match iOS (0.85), base64 thumbnails. */
object Images {

    private const val FULL_MAX_DIM = 1600
    private const val FULL_QUALITY = 85
    private const val THUMB_MAX_DIM = 360
    private const val THUMB_QUALITY = 70

    fun readBytes(context: Context, uri: Uri): ByteArray? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull()

    fun compressForUpload(raw: ByteArray): ByteArray? = compress(raw, FULL_MAX_DIM, FULL_QUALITY)

    fun thumbnailBase64(raw: ByteArray): String? {
        val thumb = compress(raw, THUMB_MAX_DIM, THUMB_QUALITY) ?: return null
        return Base64.encodeToString(thumb, Base64.NO_WRAP)
    }

    fun bitmapFromBase64(value: String?): Bitmap? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            val bytes = Base64.decode(value, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    fun imageBitmapFromBase64(value: String?): ImageBitmap? =
        bitmapFromBase64(value)?.asImageBitmap()

    fun imageBitmapFromBytes(bytes: ByteArray?): ImageBitmap? {
        if (bytes == null || bytes.isEmpty()) return null
        return runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    fun compress(raw: ByteArray, maxDim: Int, quality: Int): ByteArray? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sample = sampleSize(bounds.outWidth, bounds.outHeight, maxDim)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeByteArray(raw, 0, raw.size, decodeOptions) ?: return null
        val oriented = applyExifRotation(decoded, raw)
        val scaled = scaleDown(oriented, maxDim)

        ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            if (scaled !== oriented) scaled.recycle()
            if (oriented !== decoded) oriented.recycle()
            decoded.recycle()
            out.toByteArray()
        }
    }.getOrNull()

    private fun sampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (w / 2 >= maxDim && h / 2 >= maxDim) {
            w /= 2
            h /= 2
            sample *= 2
        }
        return sample
    }

    private fun scaleDown(bitmap: Bitmap, maxDim: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / longest
        val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private fun applyExifRotation(bitmap: Bitmap, raw: ByteArray): Bitmap {
        val orientation = runCatching {
            ExifInterface(ByteArrayInputStream(raw)).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

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
