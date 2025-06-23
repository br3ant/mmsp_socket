package com.br3ant.mmsp_socket

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Environment
import android.util.Log
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * @author houqiqi on 2025/5/22
 */
object MMSPClientHelper {
    internal const val TAG = "MMSPClientHelper"

    @Volatile
    private var running = false

    private var client: WsClient? = null
    private val sendScope = CoroutineScope(Dispatchers.IO)
    private val channel = Channel<ByteArray>(Channel.UNLIMITED)

    fun hostnameFormLocal(context: Context): String? {
        val cfg = listOf(
            File(context.getExternalFilesDir(null), "/huisheng/ws.config"),
            File(Environment.getExternalStorageDirectory(), "/huisheng/ws.config")
        )

        return cfg.find { it.exists() && it.isFile && it.canRead() }?.readText()?.trim().apply {
            Log.i(TAG, "MMSPClientHelper localIP:$this")
        }
    }

    fun start(
        hostname: String,
        port: Int = 9092,
        onServerMessageListener: ServerMessageListener? = null
    ) {
        if (running) {
            Log.e(TAG, "MMSPSender Server 已经启动")
        } else {
            client = WsClient(hostname, port, onServerMessageListener)
            client?.connect()
            running = true
        }
    }

    fun stop() {
        if (running) {
            client?.disconnect()
            running = false
        }
    }

    private fun buildBytes(cmd: CmdType, data: ByteArray): ByteArray {

        val size = data.size
        val indexBytes = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(0).array()

        val sizeBytes =
            ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(size).array()

        val header =
            byteArrayOf(0xA5.toByte(), 0x01, cmd.type) + sizeBytes + indexBytes

        return header + data + byteArrayOf(0x00)
    }

    fun sendToAll(type: CmdType, data: ByteArray) {
        client?.send(buildBytes(type, data))
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