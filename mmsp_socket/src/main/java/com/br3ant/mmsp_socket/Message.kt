package com.br3ant.mmsp_socket

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * @author houqiqi on 2025/6/18
 */
class Message(
    val data: MessageData,
    val time: Long = System.currentTimeMillis(),
)


sealed class MessageData() {
    abstract fun bytes(index: Long): ByteArray
}

class Video(
    val imgWidth: Int,
    val imgHeight: Int,
    val videoData: ByteArray,
    val hasFace: Boolean = false,
    val faceNum: Int = 0,
    val faceIndex: Long = 0,
    val w: Int = 0,
    val h: Int = 0,
    val x: Int = 0,
    val y: Int = 0,
    val mouthW: Int = 0,
    val mouthH: Int = 0,
    val mouthX: Int = 0,
    val mouthY: Int = 0
) : MessageData() {
    override fun bytes(index: Long): ByteArray {
        val dataLen = videoData.size

        // Create message buffer
        val message = ByteArray(72 + dataLen)
        val buffer = ByteBuffer.wrap(message)
        buffer.order(ByteOrder.BIG_ENDIAN)

        // Write header fields
        buffer.putInt(10003)                      // 0-4: video type
        buffer.putLong(index)                     // 4-12: index
        buffer.putInt(dataLen)                    // 12-16: data_len
        buffer.putInt(imgWidth)                   // 16-20: img_width
        buffer.putInt(imgHeight)                  // 20-24: img_height
        buffer.putInt(if (hasFace) 1 else 0)      // 24-28: hasFace
        buffer.putInt(faceNum)                    // 28-32: faceNum
        buffer.putLong(faceIndex)                 // 32-40: faceIndex
        buffer.putInt(w)                          // 40-44: w
        buffer.putInt(h)                          // 44-48: h
        buffer.putInt(x)                          // 48-52: x
        buffer.putInt(y)                          // 52-56: y
        buffer.putInt(mouthW)                     // 56-60: mouthW
        buffer.putInt(mouthH)                     // 60-64: mouthH
        buffer.putInt(mouthX)                     // 64-68: mouthX
        buffer.putInt(mouthY)                     // 68-72: mouthY

        // Add image data
        System.arraycopy(videoData, 0, message, 72, dataLen)

        return message
    }
}