package com.yashraj.cadence.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/** A photo the user chose to sit behind everything, and how bright it is, so the scrim over it can be chosen. */
@Immutable
class LoadedBackground(val image: ImageBitmap, val luminance: Float)

/** The chosen background, or null for the flowing gradient. */
val LocalBackground = compositionLocalOf<LoadedBackground?> { null }

object BackgroundMath {
    /** The part of an [imgW] x [imgH] picture that fills a [dstW] x [dstH] screen when centre-cropped. */
    data class Crop(val x: Int, val y: Int, val width: Int, val height: Int)

    fun centreCrop(imgW: Int, imgH: Int, dstW: Float, dstH: Float): Crop {
        if (imgW <= 0 || imgH <= 0 || dstW <= 0f || dstH <= 0f) return Crop(0, 0, imgW.coerceAtLeast(1), imgH.coerceAtLeast(1))
        val scale = max(dstW / imgW, dstH / imgH)
        val w = (dstW / scale).roundToInt().coerceIn(1, imgW)
        val h = (dstH / scale).roundToInt().coerceIn(1, imgH)
        return Crop(((imgW - w) / 2f).roundToInt(), ((imgH - h) / 2f).roundToInt(), w, h)
    }

    /**
     * How much of the ground colour is laid over the photo so the interface stays legible: a bright photo needs little
     * under dark text but a lot under light text, and the other way round. Never zero, so no photo can fight the text.
     */
    fun scrimAlpha(luminance: Float, themeIsDark: Boolean): Float = if (themeIsDark) {
        (0.30f + 0.50f * luminance).coerceIn(0.30f, 0.80f)
    } else {
        (0.30f + 0.45f * (1f - luminance)).coerceIn(0.30f, 0.75f)
    }
}

/** The chosen photo, kept inside the app so it keeps working after the original is moved or deleted. */
object BackgroundStore {
    private const val FileName = "background.jpg"
    private const val MaxEdge = 1600

    private fun file(context: Context) = File(context.filesDir, FileName)

    /** Copies [uri] into the app at screen size. Returns false when it cannot be read as an image. */
    suspend fun save(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            // ImageDecoder applies the photo's rotation itself; software memory so it can be written back out.
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = max(info.size.width, info.size.height)
                if (longest > MaxEdge) {
                    val k = MaxEdge.toFloat() / longest
                    decoder.setTargetSize((info.size.width * k).roundToInt().coerceAtLeast(1), (info.size.height * k).roundToInt().coerceAtLeast(1))
                }
            }
            file(context).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun clear(context: Context) {
        file(context).delete()
    }

    suspend fun load(context: Context): LoadedBackground? = withContext(Dispatchers.IO) {
        val f = file(context)
        if (!f.exists()) return@withContext null
        try {
            val bitmap = BitmapFactory.decodeFile(f.path) ?: return@withContext null
            val tiny = Bitmap.createScaledBitmap(bitmap, 24, 24, true)
            val pixels = IntArray(24 * 24)
            tiny.getPixels(pixels, 0, 24, 0, 0, 24, 24)
            LoadedBackground(bitmap.asImageBitmap(), GlassAdaptMath.read(pixels)?.luminance ?: 0.5f)
        } catch (e: Exception) {
            null
        }
    }
}
