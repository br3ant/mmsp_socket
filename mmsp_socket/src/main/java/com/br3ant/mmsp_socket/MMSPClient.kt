package com.br3ant.mmsp_socket

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

/**
 * @author houqiqi on 2025/6/24
 */
class MMSPClient(
    private val mode: ChannelMode,
    hostname: String = "",
    port: Int = 9092,
    socketName: String = "local_socket_mmsp",
    val listener: MessageReceiver? = null
) : ChannelListener {

    companion object {
        const val TAG = "MMSPClient"

        fun hostnameFormLocal(context: Context): String? {
            val cfg = listOf(
                File(context.getExternalFilesDir(null), "/huisheng/ws.config"),
                File(Environment.getExternalStorageDirectory(), "/huisheng/ws.config")
            )

            return cfg.find { it.exists() && it.isFile && it.canRead() }?.readText()?.trim().apply {
                Log.i(TAG, "localIP:$this")
            }
        }
    }

    val client = when(mode){
        ChannelMode.SOCKET -> TODO()
        ChannelMode.WS -> WsClient(hostname,port)
        ChannelMode.LOCAL_SOCKET -> LocalSocketClient(socketName)
    }

    @Volatile
    private var running = false

    fun start() {
        if (running) {
            Log.e(TAG, "MMSPClient Client 已经启动")
        } else {
            client.setChannelListener(this)
            client.launch()
            running = true
        }
    }

    fun stop() {
        if (running) {
            client.terminate()
            running = false
        }
    }

    override fun onReceiver(data: ByteBuffer) {
        listener?.dispatch(data)
    }

    override fun onNewConnected() {
    }
}