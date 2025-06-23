package com.br3ant.mmsp_socket

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.nio.ByteOrder


/**
 * @author houqiqi on 2025/5/30
 */

internal class WsClient(
    hostname: String,
    port: Int = 9092,
    private val onServerMessageListener: ServerMessageListener? = null
) : WebSocketListener() {

    companion object {
        private const val TAG = "WsClient"
    }

    val client = OkHttpClient()

    val request = Request.Builder()
        .url("ws://${hostname}:${port}")
        .build()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var connected = false
    private var socket: WebSocket? = null

    fun connect() {
        scope.launch {
            while (true) {
                if (connected.not()) {
                    socket = client.newWebSocket(request, this@WsClient)
                }
                delay(5000)
            }
        }
    }

    fun disconnect() {
        scope.cancel()
    }

    fun send(data: ByteArray) {
        socket?.send(data.toByteString())
    }


    override fun onOpen(webSocket: okhttp3.WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        Log.i(TAG, "连接已建立")
        connected = true
        socket = webSocket
        onServerMessageListener?.onConnected()
    }

    override fun onMessage(webSocket: okhttp3.WebSocket, bytes: ByteString) {
        super.onMessage(webSocket, bytes)

        runCatching {
            val message = bytes.asByteBuffer()
            val limit = message.limit()
            if (limit < 4) {
                //web 发送过来的无效
                return
            }
            val data = message.order(ByteOrder.LITTLE_ENDIAN)

            val cmd = data.get(2)
            val dataLen = data.getInt(3)
            if (dataLen + 10 > limit) {
                Log.d(TAG, "onMessage buffer 过短 dataLen:${dataLen} limit:${limit}")
                return
            }
            val dataArray = ByteArray(dataLen)
            data.position(9)
            data.get(dataArray, 0, dataLen)

            onServerMessageListener?.onMessage(CmdType.from(cmd), dataArray)
        }.onFailure {
            Log.d(TAG, "onMessage error:${it.message}")
        }
    }


    override fun onClosed(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)
        socket = null
        Log.e(TAG, "连接已断开 reason${reason}")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        Log.e(TAG, "连接错误 error${t.message} rep:${response?.message}")
    }
}

interface ServerMessageListener {
    fun onConnected()

    fun onMessage(type: CmdType, data: ByteArray)
}