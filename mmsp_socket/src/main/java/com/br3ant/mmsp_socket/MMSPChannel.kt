package com.br3ant.mmsp_socket

import com.br3ant.mmsp_socket.MMSPChannel.Companion.indexMap
import com.br3ant.mmsp_socket.utils.RGBUtils
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.experimental.and

/**
 * @author houqiqi on 2025/5/30
 */
interface MMSPChannel {

    fun launch(): Boolean

    fun sendData(data: ByteArray): Boolean

    fun terminate(): Boolean

    fun setChannelListener(listener: ChannelListener)

    companion object {
        internal val indexMap = ConcurrentHashMap<Int, Int>()
    }
}

private fun MMSPChannel.indexBytes(): ByteArray {
    val code = hashCode()
    val index = indexMap.getOrDefault(code, 1)
    val nextIndex = (index % 256) + 1
    indexMap.put(code, nextIndex)
    return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(nextIndex.toShort())
        .array()
}

fun MMSPChannel.send(type: CmdType, data: ByteArray) {
    val size = data.size
    val indexBytes = indexBytes()
    val index = ByteBuffer.wrap(indexBytes).order(ByteOrder.LITTLE_ENDIAN)
        .getShort() and 0xFFFF.toShort()

    val sizeBytes =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(size).array()

    val header =
        byteArrayOf(0xA5.toByte(), 0x01, type.type) + sizeBytes + indexBytes

    sendData(header + data + byteArrayOf(0x00))
}

fun MMSPChannel.send(type: CmdType, string: String) {
    send(type, string.toByteArray())
}

fun MMSPChannel.sendFormat(type: CmdType, width: Int, height: Int, format: Int = 1) {
    send(type, JSONObject().apply {
        put("format", format)
        put("width", width)
        put("height", height)
    }.toString().toByteArray(Charsets.UTF_8))
}

fun MMSPChannel.sendRgbLikeData(
    type: CmdType,
    data: ByteArray,
    format: FORMAT,
    width: Int,
    height: Int
) {
    val jpgBytes = RGBUtils.rgbLikeToJpg(data, format, width, height)
    send(type, jpgBytes)
}


interface ChannelListener {
    fun onReceiver(data: ByteBuffer)

    fun onNewConnected()
}