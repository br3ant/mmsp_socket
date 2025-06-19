package com.br3ant.mmsp_socket

import android.util.Log
import com.br3ant.mmsp_socket.MMSPSender.config
import com.br3ant.mmsp_socket.utils.ByteRateCounter
import kotlinx.coroutines.delay
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList


/**
 * @author houqiqi on 2025/5/30
 */


internal class WsServer(
    private val port: Int = 49999,
) :
    WebSocketServer(InetSocketAddress(port)), ChannelServer {

    private val socketList = CopyOnWriteArrayList<WebSocket>()
    private var socket: WebSocket? = null
    private val byteRateCounter = ByteRateCounter()

    private var messageReceiver: MessageReceiver? = null

    override fun launch() {
        start()
    }

    override fun stop() {
        super.stop()
        socketList.forEach { it.close() }
        socketList.clear()
        socket?.close()
    }

    override fun setMessageReceiver(messageReceiver: MessageReceiver) {
        this.messageReceiver = messageReceiver
    }

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        socketList.add(conn)
        socket = conn
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        Log.d(MMSPSender.TAG, "Connection closed: ${conn.remoteSocketAddress}")
        socketList.remove(conn)
        socket = null
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
//            val data = message.order(ByteOrder.LITTLE_ENDIAN)
//
//            val cmd = data.get(2)
//            val dataLen = data.getInt(3)
//            if (dataLen + 10 > limit) {
//                Log.d(MMSPSender.TAG, "onMessage buffer 过短 dataLen:${dataLen} limit:${limit}")
//                return
//            }
//            val dataArray = ByteArray(dataLen)
//            data.position(9)
//            data.get(dataArray, 0, dataLen)

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

    override fun send(data: MessageData) {
//        println("客户端连接数 ${socketList.size}")

        if (socketList.isNotEmpty()) {
            socketList.forEach {
                it.send(data.bytes(it.nextIndex()))
            }
        }
    }

    override suspend fun syncSend(message: MessageData) {
        val socket = socket ?: return
        while (socket.hasBufferedData()) {
            delay(10)
        }
        socket.send(message.bytes(socket.nextIndex()).also { byteRateCounter.logBytes(it.size) })
    }

    private val indexMap = ConcurrentHashMap<Int, Long>()

    private fun WebSocket.nextIndex(): Long {
        val code = hashCode()
        val index = indexMap.getOrDefault(code, 1)
        val nextIndex = index + 1
        indexMap.put(code, nextIndex)
        return nextIndex
    }
}

