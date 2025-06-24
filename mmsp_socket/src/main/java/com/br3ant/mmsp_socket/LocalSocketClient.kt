package com.br3ant.mmsp_socket


import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer


/**
 * @author houqiqi on 2025/5/21
 */

internal class LocalSocketClient(private val name: String) : MMSPChannel {

    private var socket: LocalSocket = LocalSocket()
    private val readScope = CoroutineScope(Dispatchers.IO)
    private var channelListener: ChannelListener? = null

    private val process = DataProcess(object : DataListener {
        override fun onReceive(data: ByteArray) {
            channelListener?.onReceiver(ByteBuffer.wrap(data))
        }
    })

    override fun launch(): Boolean {
        readScope.launch {
            while (isActive) {
                if (socket.isConnected.not()) {
                    runCatching {
                        socket.connect(
                            LocalSocketAddress(
                                name,
                                LocalSocketAddress.Namespace.ABSTRACT
                            )
                        )
                        Log.i(MMSPClient.TAG, "Socket客户端连接$name 成功")
                        val inputStream = socket.inputStream
                        while (socket.isConnected) {
                            val readBuffer = ByteArray(1024)
                            var size = inputStream.read(readBuffer)

                            if (size <= 0) continue

                            process.handle(readBuffer.take(size).toByteArray())
                        }
                    }.onFailure {
                        Log.e(MMSPClient.TAG, "Socket客户端连接$name 失败 ${it.message}")
                    }
                }
                delay(5000)
            }
        }
        return true
    }

    override suspend fun sendData(data: ByteArray): Boolean {
        val outputStream = socket.outputStream
        val inputStream = socket.inputStream

        var offset = 0
        val bufferSize = 1024

        // 循环读取文件内容并发送
        while (offset < data.size) {
            val chunkSize = minOf(bufferSize, data.size - offset)
            outputStream.write(data, offset, chunkSize)
            offset += chunkSize
        }

        // 发送结束标记
        outputStream.flush()
        return true
    }

    override fun terminate(): Boolean {
        socket.close()
        readScope.cancel()
        return true
    }

    override fun setChannelListener(listener: ChannelListener) {
        this.channelListener = listener
    }

    interface DataListener {
        fun onReceive(data: ByteArray)
    }

    class DataProcess(private val listener: DataListener) {
        private var buffer = mutableListOf<Byte>()

        fun handle(data: ByteArray) {
            buffer.addAll(data.toList())
//            Log.i(TAG, "ReadThread = " + GattUtils.bytesToHexString(buffer.toByteArray()))

            // 解析数据包
            while (buffer.size >= 10) {  // 至少要有头部+Length
                if (buffer[0] == 0xA5.toByte() && buffer[1] == 0x01.toByte()) {
                    val length = byteArrayToLittleEndianInt(buffer, 3)
                    if (buffer.size >= length + 10) { // 确保完整数据包
                        val packet = buffer.subList(0, length + 10).toByteArray()
                        listener.onReceive(packet)
                        buffer.subList(0, length + 10).clear() // 移除已解析数据
                    } else {
                        break // 等待更多数据
                    }
                } else {
                    buffer.removeAt(0) // 不是头部，丢弃一个字节
                }
            }
        }

        fun byteArrayToLittleEndianInt(bytes: List<Byte>, startIndex: Int): Int {
            require(startIndex >= 0 && startIndex + 4 <= bytes.size) {
                "Invalid index range for 4-byte Int"
            }

            return (bytes[startIndex].toInt() and 0xFF) or
                    ((bytes[startIndex + 1].toInt() and 0xFF) shl 8) or
                    ((bytes[startIndex + 2].toInt() and 0xFF) shl 16) or
                    ((bytes[startIndex + 3].toInt() and 0xFF) shl 24)
        }
    }
}

