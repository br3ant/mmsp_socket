package com.br3ant.mmsp_socket

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer


/**
 * @author houqiqi on 2025/5/30
 */


class WsClient(
    hostname: String,
    port: Int = 9092,
    protocol: String = "ws"
) : WebSocketClient(URI("$protocol://${hostname}:$port")), MMSPChannel {

    private var channelListener: ChannelListener? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val queue = Channel<ByteArray>(10, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun launch(): Boolean {
        scope.launch {
            while (true) {
                if (isOpen.not()) {
                    connect()
                }
                delay(5000)
            }
        }
        scope.launch {
            for (data in queue) {
                if (isOpen.not()) break
                while (hasBufferedData() == true) delay(10)
                send(data)
            }
        }
        return true
    }

    override fun sendData(data: ByteArray): Boolean {
        if (isOpen.not()) return false
        return queue.trySend(data).isSuccess
    }

    override fun terminate(): Boolean {
        close()
        scope.cancel()
        return true
    }

    override fun setChannelListener(listener: ChannelListener) {
        this.channelListener = listener
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        channelListener?.onNewConnected()
    }

    override fun onMessage(message: String?) {
    }

    override fun onMessage(bytes: ByteBuffer) {
        super.onMessage(bytes)
        channelListener?.onReceiver(bytes)
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
    }

    override fun onError(ex: java.lang.Exception?) {
        Log.e(MMSPServer.TAG, "Error occurred", ex)
    }
}