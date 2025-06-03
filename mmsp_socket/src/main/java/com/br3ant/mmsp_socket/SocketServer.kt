package com.br3ant.mmsp_socket


import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread
import kotlin.experimental.and

/**
 * @author houqiqi on 2025/5/21
 */

internal class SocketServer(private val port: Int = 9091) : ChannelServer {


    private val socketList = CopyOnWriteArrayList<Socket>()
    private val serverSocket = ServerSocket(port)
    private val indexMap = ConcurrentHashMap<Int, Int>()
    private val channel = Channel<Message>(Channel.UNLIMITED)
    private val scopeRead = CoroutineScope(Dispatchers.IO)

    override fun launch() {
        thread {
            Log.i(MMSPSender.TAG, "Socket服务端已启动，监听:$port ...")

            try {
                while (true) {
                    val clientSocket = serverSocket.accept()
                    Log.i(
                        MMSPSender.TAG,
                        "接收到连接：${clientSocket.inetAddress}:${clientSocket.port}"
                    )
                    socketList.add(clientSocket)

                    sendSocketDefConfig(clientSocket)
                }
            } finally {
                stop()
            }
        }
        scopeRead.launch {
            for (message in channel) {
                send(message)
            }
        }
    }

    override fun send(type: CmdType, data: ByteArray) {
//        println("客户端连接数 ${socketList.size}")
        Statistics.framePoint(type)
        if (socketList.isNotEmpty()) {
            socketList.forEach { channel.trySend(Message(it, type, data)) }
        }
    }

    override fun stop() {
        serverSocket.close()
        socketList.forEach { it.close() }
        serverSocket.close()
        channel.close()
        scopeRead.cancel()
    }

    override fun setMessageReceiver(messageReceiver: MessageReceiver) {
    }

    private fun sendMessage(message: Message) {
        channel.trySend(message)
    }

    private fun sendSocketDefConfig(socket: Socket) {
        sendMessage(
            Message(
                socket,
                CmdType.CAMERA_FORMAT,
                JSONObject().apply {
                    put("format", 1)
                    put("width", MMSPSender.config.cameraWidth)
                    put("height", MMSPSender.config.cameraHeight)
                }.toString().toByteArray(Charsets.UTF_8)
            )
        )

        sendMessage(
            Message(
                socket,
                CmdType.HUMAN_FORMAT,
                JSONObject().apply {
                    put("format", 1)
                    put("width", MMSPSender.config.humanWidth)
                    put("height", MMSPSender.config.humanHeight)
                }.toString().toByteArray(Charsets.UTF_8)
            )
        )
    }

    private fun Socket.indexBytes(): ByteArray {
        val code = hashCode()
        val index = indexMap.getOrDefault(code, 1)
        val nextIndex = (index % 256) + 1
        indexMap.put(code, nextIndex)
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(nextIndex.toShort())
            .array()
    }


    private fun send(message: Message) {
        val socket = message.socket
        try {
            if (socket !in socketList) {
                return
            }
            val data = message.data

            val outputStream = socket.outputStream
            val inputStream = socket.inputStream
            val size = data.size
            val indexBytes = socket.indexBytes()
            val index = ByteBuffer.wrap(indexBytes).order(ByteOrder.LITTLE_ENDIAN)
                .getShort() and 0xFFFF.toShort()

//            println("sendData type=$type, index=$index, len: $size 字节")

            val sizeBytes =
                ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(size).array()

            val header =
                byteArrayOf(0xA5.toByte(), 0x01, message.cmd.type) + sizeBytes + indexBytes
            val bufferSize = 1024
            var offset = bufferSize - header.size

            outputStream.write(header + data.take(offset))

            // 循环读取文件内容并发送
            while (offset < data.size) {
                val chunkSize = minOf(bufferSize, data.size - offset)
                outputStream.write(data, offset, chunkSize)
                offset += chunkSize
            }

            // 发送结束标记
            outputStream.write(byteArrayOf(0x00))
            outputStream.flush()
//            println("sendData index=$index 发送完成")

            // 等待客户端确认接收
            val confirmBuffer = ByteArray(1024)
            val confirmBytesRead = inputStream.read(confirmBuffer)
            if (confirmBytesRead == -1) {
                socket.close()
                socketList.remove(socket)
                println("客户端断开")
                return
            }

            if (MMSPSender.config.debug) {
                Log.i(
                    MMSPSender.TAG,
                    "客户端确认,传输用时: ${System.currentTimeMillis() - message.time} ms"
                )
            }

        } catch (e: SocketException) {
            socket.close()
            socketList.remove(socket)
            println("sendData error,socket is closed")
        } catch (e: Exception) {
            println("sendData error")
            e.printStackTrace()
        }
    }

    class Message(
        val socket: Socket,
        val cmd: CmdType,
        val data: ByteArray,
        val time: Long = System.currentTimeMillis()
    )
}

