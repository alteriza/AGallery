package com.agallery.app.util

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream

object BitmapUtils {

    fun decode(context: Context, uri: Uri): Bitmap? {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: return null

        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input: InputStream ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }
        return if (matrix.isIdentity) bitmap
        else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun applyBrightness(src: Bitmap, brightness: Float): Bitmap {
        if (brightness == 1f) return src
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(
                ColorMatrix().apply { setScale(brightness, brightness, brightness, 1f) }
            )
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    fun rotateAndFlip(src: Bitmap, degrees: Float, flipH: Boolean): Bitmap {
        val m = Matrix().apply {
            postRotate(degrees)
            if (flipH) postScale(-1f, 1f)
        }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    fun crop(src: Bitmap, left: Float, top: Float, right: Float, bottom: Float): Bitmap {
        val x = (left * src.width).toInt().coerceIn(0, src.width - 1)
        val y = (top * src.height).toInt().coerceIn(0, src.height - 1)
        val w = ((right - left) * src.width).toInt().coerceIn(1, src.width - x)
        val h = ((bottom - top) * src.height).toInt().coerceIn(1, src.height - y)
        return Bitmap.createBitmap(src, x, y, w, h)
    }
}