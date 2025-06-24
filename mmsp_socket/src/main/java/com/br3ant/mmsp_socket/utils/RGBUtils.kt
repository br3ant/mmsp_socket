package com.br3ant.mmsp_socket.utils

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import com.br3ant.mmsp_socket.FORMAT
import java.io.ByteArrayOutputStream

/**
 * @author houqiqi on 2025/6/24
 */
object RGBUtils {

    fun rgbLikeToJpg(bytes: ByteArray, format: FORMAT, width: Int, height: Int): ByteArray {
        val pixels = IntArray(width * height)

        when (format) {
            FORMAT.RGB -> for (i in 0 until width * height) {
                val r = bytes[i * 3].toInt() and 0xFF
                val g = bytes[i * 3 + 1].toInt() and 0xFF
                val b = bytes[i * 3 + 2].toInt() and 0xFF
                pixels[i] = Color.rgb(r, g, b)
            }

            FORMAT.BGR -> for (i in 0 until width * height) {
                val b = bytes[i * 3].toInt() and 0xFF
                val g = bytes[i * 3 + 1].toInt() and 0xFF
                val r = bytes[i * 3 + 2].toInt() and 0xFF
                pixels[i] = Color.rgb(r, g, b)
            }

            FORMAT.RGBA -> for (i in 0 until width * height) {
                val r = bytes[i * 4].toInt() and 0xFF
                val g = bytes[i * 4 + 1].toInt() and 0xFF
                val b = bytes[i * 4 + 2].toInt() and 0xFF
                val a = bytes[i * 4 + 3].toInt() and 0xFF

                pixels[i] = Color.argb(a, r, g, b)
            }

            FORMAT.BGRA -> for (i in 0 until width * height) {
                val b = bytes[i * 4].toInt() and 0xFF
                val g = bytes[i * 4 + 1].toInt() and 0xFF
                val r = bytes[i * 4 + 2].toInt() and 0xFF
                val a = bytes[i * 4 + 3].toInt() and 0xFF

                pixels[i] = Color.argb(a, r, g, b)
            }

            FORMAT.ARGB -> for (i in 0 until width * height) {
                val a = bytes[i * 4].toInt() and 0xFF
                val r = bytes[i * 4 + 1].toInt() and 0xFF
                val g = bytes[i * 4 + 2].toInt() and 0xFF
                val b = bytes[i * 4 + 3].toInt() and 0xFF

                pixels[i] = Color.argb(a, r, g, b)
            }
        }

        return intArrayToJpg(pixels, width, height)
    }

    fun intArrayToJpg(intArray: IntArray, width: Int, height: Int): ByteArray {
        val bitmap = createBitmap(width, height)
        bitmap.setPixels(intArray, 0, width, 0, 0, width, height)

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        return outputStream.toByteArray()
    }
}