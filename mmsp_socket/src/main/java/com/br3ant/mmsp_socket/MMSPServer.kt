package com.br3ant.mmsp_socket

import android.util.Log
import java.nio.ByteBuffer

/**
 * @author houqiqi on 2025/6/24
 */

class MMSPServer(
    private val mode: ChannelMode,
    val port: Int = 9092,
    socketName: String = "local_socket_mmsp",
    val onNewConnected: () -> Unit = {},
    val listener: MessageReceiver? = null
) : ChannelListener {

    companion object {
        const val TAG = "MMSPServer"
    }

    @Volatile
    private var running = false

    val server: MMSPChannel = when (mode) {
        ChannelMode.WS -> WsServer(port)
        ChannelMode.SOCKET -> SocketServer(port)
        ChannelMode.LOCAL_SOCKET -> LocalSocketServer(socketName)
    }

    fun start() {
        if (running) {
            Log.e(TAG, "MMSPServer Server 已经启动")
        } else {
            server.setChannelListener(this)
            server.launch()
            running = true
        }
    }

    fun stop() {
        if (running) {
            server.terminate()
            running = false
        }
    }


    override fun onReceiver(message: ByteBuffer) {
        listener?.dispatch(message)
    }

    override fun onNewConnected() {
        onNewConnected.invoke()
    }
}