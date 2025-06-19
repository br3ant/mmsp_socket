package com.br3ant.mmsp_socket

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * @author houqiqi on 2025/5/22
 */
object MMSPSender {
    internal const val TAG = "MMSPSender"

    @Volatile
    private var running = false

    internal var config = defaultCfg

    private val server by lazy { WsServer(config.port) }
    private val readScope = CoroutineScope(Dispatchers.IO)
    private val sendScope = CoroutineScope(Dispatchers.IO)
    private val channel = Channel<ByteArray>(Channel.UNLIMITED)

    fun start(config: SendConfig = defaultCfg) {
        if (running) {
            Log.e(TAG, "MMSPSender Server 已经启动")
        } else {
            this.config = config
            Log.i(TAG, this.config.toString())

            server.launch()

            running = true
        }
    }

    fun stop() {
        if (running) {
            server.stop()
            running = false
        }
    }

    fun setMessageReceiver(messageReceiver: MessageReceiver) {
        server.setMessageReceiver(messageReceiver)
    }

    fun send(data: MessageData) {
        server.send(data)
    }

    fun sendAudio(data: ByteArray, chuck: Int = 3200, delay: Long = 100, firstDelay: Long = 300) {
        sendScope.launch {
            delay(firstDelay)
//            var offset = 0
//
//            // 循环读取文件内容并发送
//            while (offset < data.size) {
//                val chunkSize = minOf(chuck, data.size - offset)
//                channel.send(data.copyOfRange(offset, offset + chunkSize))
//                offset += chunkSize
//                delay(delay)
//            }
            channel.send(data)
        }

    }

    fun sendJpg(data: ByteArray, width: Int, height: Int) {
        send(Video(width, height, jpegToBgr(data)))
    }

    suspend fun syncSendJpg(data: ByteArray, width: Int, height: Int) {
        server.syncSend(Video(width, height, jpegToBgr(data)))
    }


    fun sendBgr(data: ByteArray, width: Int, height: Int) {
        send(Video(width, height, data))
    }

//    fun sendRgbLikeData(data: ByteArray, format: FORMAT, width: Int, height: Int) {
//        val jpgBytes = rgbLikeToJpg(data, format, width, height)
//        send(Video(width, height, jpgBytes))
//    }

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

    fun jpegToBgr(jpegByteArray: ByteArray): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.size)
            ?: throw IllegalArgumentException("Decode jpegByteArray failed")

        val width = bitmap.width
        val height = bitmap.height
        val pixelCount = width * height
        val pixels = IntArray(pixelCount)

        // 一次性获取所有像素（ARGB格式）
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val bgrByteArray = ByteArray(pixelCount * 3)

        var index = 0
        for (pixel in pixels) {
            bgrByteArray[index++] = Color.blue(pixel).toByte()   // B
            bgrByteArray[index++] = Color.green(pixel).toByte()  // G
            bgrByteArray[index++] = Color.red(pixel).toByte()    // R
        }

        return bgrByteArray
    }

}

enum class FORMAT {
    RGB, BGR, RGBA, BGRA, ARGB
}