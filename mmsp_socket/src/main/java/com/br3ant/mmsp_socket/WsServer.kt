package com.br3ant.mmsp_socket

import android.util.Log
import com.br3ant.mmsp_socket.MMSPSender.config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.json.JSONObject
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.experimental.and


/**
 * @author houqiqi on 2025/5/30
 */


internal class WsServer(
    private val port: Int = 9092,
) :
    WebSocketServer(InetSocketAddress(port)), ChannelServer {

    private val socketList = CopyOnWriteArrayList<WebSocket>()
    private val indexMap = ConcurrentHashMap<Int, Int>()
    private val channel = Channel<Message>(Channel.UNLIMITED)
    private val scopeRead = CoroutineScope(Dispatchers.IO)
    private val recvChannels = ConcurrentHashMap<Int, Channel<ByteBuffer>>()

    private var messageReceiver: MessageReceiver? = null

    override fun launch() {
        start()

        scopeRead.launch {
            for (message in channel) {
                send(message)
            }
        }
    }

    override fun stop() {
        super.stop()
        scopeRead.cancel()
        channel.close()
        socketList.forEach { it.close() }
        socketList.clear()
    }

    override fun setMessageReceiver(messageReceiver: MessageReceiver) {
        this.messageReceiver = messageReceiver
    }

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        socketList.add(conn)

        sendWSDefConfig(conn)
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        Log.d(MMSPSender.TAG, "Connection closed: ${conn.remoteSocketAddress}")
        socketList.remove(conn)
    }

    override fun onMessage(conn: WebSocket, message: String) {
        Log.d(MMSPSender.TAG, "onMessage txt :${message}")
    }

    override fun onMessage(conn: WebSocket, message: ByteBuffer) {
        super.onMessage(conn, message)
        if (config.debug) {
            Log.d(MMSPSender.TAG, "onMessage buffer :${message.limit()}")
        }
//        val channel = recvChannels.getOrPut(conn.hashCode()) {
//            Channel(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
//        }
//        channel.trySend(message)
        runCatching {
            val limit = message.limit()
            if (limit < 4) {
                //web 发送过来的无效
                return
            }
            val data = message.order(ByteOrder.LITTLE_ENDIAN)

            val cmd = data.get(2)
            val dataLen = data.getInt(3)
            if (dataLen + 10 > limit) {
                Log.d(MMSPSender.TAG, "onMessage buffer 过短 dataLen:${dataLen} limit:${limit}")
                return
            }
            val dataArray = ByteArray(dataLen)
            data.position(9)
            data.get(dataArray, 0, dataLen)
            when (cmd) {
                CmdType.PLAY_TTS.type -> {
                    messageReceiver?.onTTS(dataArray.decodeToString())
                }

                CmdType.SET_PARAM.type -> {
                    val param = Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                    }.decodeFromString(
                        ServerParam.serializer(),
                        dataArray.decodeToString()
                    )
                    if (config.debug) {
                        Log.i(MMSPSender.TAG, "onMessage SET_PARAM:${param}")
                    }
                    messageReceiver?.onServerParamUpdate(param)
                }
            }
        }.onFailure {
            Log.d(MMSPSender.TAG, "onMessage error:${it.message}")
        }
    }

    override fun onError(conn: WebSocket?, ex: Exception) {
        Log.e(MMSPSender.TAG, "Error occurred", ex)
    }

    override fun onStart() {
        Log.i(MMSPSender.TAG, "WebSocket 服务端已启动，监听:$port ...")
    }

    override fun send(type: CmdType, data: ByteArray) {
//        println("客户端连接数 ${socketList.size}")
        Statistics.framePoint(type)
        if (socketList.isNotEmpty()) {
            socketList.forEach { channel.trySend(Message(it, type, data)) }
        }
    }

    private fun sendMessage(message: Message) {
        channel.trySend(message)
    }

    private fun sendWSDefConfig(webSocket: WebSocket) {
        sendMessage(Message(webSocket, CmdType.CAMERA_FORMAT, JSONObject().apply {
            put("format", 1)
            put("width", config.cameraWidth)
            put("height", config.cameraHeight)
        }.toString().toByteArray(Charsets.UTF_8)))

        sendMessage(Message(webSocket, CmdType.HUMAN_FORMAT, JSONObject().apply {
            put("format", 1)
            put("width", config.humanWidth)
            put("height", config.humanHeight)
        }.toString().toByteArray(Charsets.UTF_8)))
    }

    private fun WebSocket.indexBytes(): ByteArray {
        val code = hashCode()
        val index = indexMap.getOrDefault(code, 1)
        val nextIndex = (index % 256) + 1
        indexMap.put(code, nextIndex)
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(nextIndex.toShort())
            .array()
    }


    private suspend fun send(message: Message) {
        val socket = message.socket
        try {
            if (socket !in socketList) {
                return
            }
            val data = message.data

            val size = data.size
            val indexBytes = socket.indexBytes()
            val index = ByteBuffer.wrap(indexBytes).order(ByteOrder.LITTLE_ENDIAN)
                .getShort() and 0xFFFF.toShort()

//            Log.i(MMSPSender.TAG, "sendData type=${message.cmd}, index=$index, len: $size 字节")

            val sizeBytes =
                ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(size).array()

            val header =
                byteArrayOf(0xA5.toByte(), 0x01, message.cmd.type) + sizeBytes + indexBytes
//            val bufferSize = 1024 * 8
//            var offset = bufferSize - header.size

            socket.send(header + data + byteArrayOf(0x00))

            // 循环读取文件内容并发送
//            while (offset < data.size) {
//                val chunkSize = minOf(bufferSize, data.size - offset)
//                socket.send(data.copyOfRange(offset, offset + chunkSize))
//                offset += chunkSize
//            }

            // 发送结束标记
//            socket.send(byteArrayOf(0x00))
//            Log.i(MMSPSender.TAG, "sendData index=$index 发送完成")

            // 等待客户端确认接收
//            val confirmBytesRead = withTimeoutOrNull(100) {
//                recvChannels.get(socket.hashCode())?.receive()?.limit()
//            } ?: -1
//            if (confirmBytesRead == -1) {
//                socket.close()
//                socketList.remove(socket)
//                Log.i(MMSPSender.TAG, "未收到客户端回复")
//                return
//            }

            if (config.debug) {
//                Log.i(
//                    MMSPSender.TAG,
//                    "客户端确认,传输用时: ${System.currentTimeMillis() - message.time} ms"
//                )
            }

        } catch (e: Exception) {
            Log.i(MMSPSender.TAG, "sendData error")
            e.printStackTrace()
        }
    }

    class Message(
        val socket: WebSocket,
        val cmd: CmdType,
        val data: ByteArray,
        val time: Long = System.currentTimeMillis()
    )
}