package com.br3ant.mmsp_socket

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.nio.ByteBuffer


/**
 * @author houqiqi on 2025/5/30
 */


class WsServer(
    private val port: Int = 9092,
) : WebSocketServer(InetSocketAddress(port)), MMSPChannel {

    private var socket: WebSocket? = null

    private var channelListener: ChannelListener? = null

    private val scope = CoroutineScope(Dispatchers.IO)
    private val queue = Channel<ByteArray>(10, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun launch(): Boolean {
        start()

        scope.launch {
            for (data in queue) {
                val socket = socket ?: continue
                if (socket.isOpen.not()) continue
                while (socket.hasBufferedData() == true) delay(10)
                socket.send(data)
            }
        }
        return true
    }

    override fun sendData(data: ByteArray): Boolean {
        return queue.trySend(data).isSuccess
    }

    override fun terminate(): Boolean {
        stop()
        scope.cancel()
        return true
    }

    override fun setChannelListener(listener: ChannelListener) {
        this.channelListener = listener
    }

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        socket = conn
        channelListener?.onNewConnected()
    }

    override fun onClose(
        conn: WebSocket?,
        code: Int,
        reason: String?,
        remote: Boolean
    ) {
        socket = null
    }

    override fun onMessage(conn: WebSocket?, message: String?) {
    }

    override fun onMessage(conn: WebSocket?, message: ByteBuffer) {
        super.onMessage(conn, message)
        channelListener?.onReceiver(message)
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        Log.e(MMSPServer.TAG, "Error occurred", ex)
    }

    override fun onStart() {
        Log.i(MMSPServer.TAG, "WebSocket 服务端已启动，监听:$port ...")
    }
}