package com.br3ant.mmsp_socket

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * @author houqiqi on 2025/5/22
 */
object MMSPSender {
    internal const val TAG = "MMSPSender"

    @Volatile
    private var running = false

    internal var config = defaultCfg

    private val server by lazy {
        if (config.mode == Mode.SOCKET)
            SocketServer(config.port)
        else WsServer(config.port)
    }
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

            //声音特殊，按照chuck发送
            readScope.launch {
                for (audio in channel) {
                    sendToAll(CmdType.AUDIO, audio)
                }
            }

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

    fun sendToAll(type: CmdType, data: ByteArray) {
        server.send(type, data)
    }

    fun sendToAll(type: CmdType, string: String) {
        sendToAll(type, string.toByteArray())
    }

    fun sendToAllFormat(type: CmdType, width: Int, height: Int, format: Int = 1) {
        sendToAll(type, JSONObject().apply {
            put("format", format)
            put("width", width)
            put("height", height)
        }.toString().toByteArray(Charsets.UTF_8))
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

    fun sendRgbLikeData(type: CmdType, data: ByteArray, format: FORMAT, width: Int, height: Int) {
        val jpgBytes = rgbLikeToJpg(data, format, width, height)
        sendToAll(type, jpgBytes)
    }

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

enum class FORMAT {
    RGB, BGR, RGBA, BGRA, ARGB
}