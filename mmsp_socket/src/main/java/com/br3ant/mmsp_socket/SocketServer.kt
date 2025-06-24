package com.br3ant.mmsp_socket


import android.util.Log
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * @author houqiqi on 2025/5/21
 */

internal class SocketServer(private val port: Int) : MMSPChannel {

    private val serverSocket = ServerSocket(port)
    private var socket: Socket? = null

    override fun launch(): Boolean {
        thread {
            Log.i(MMSPServer.TAG, "Socket服务端已启动，监听:$port ...")

            try {
                while (true) {
                    val clientSocket = serverSocket.accept()
                    Log.i(
                        MMSPServer.TAG,
                        "接收到连接：${clientSocket.inetAddress}:${clientSocket.port}"
                    )
                    socket = clientSocket
                }
            } finally {
                terminate()
            }
        }
        return true
    }

    override fun sendData(data: ByteArray): Boolean {
        val socket = socket
        if (socket == null) return false
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
        socket?.close()
        socket = null
        serverSocket.close()
        return true
    }

    override fun setChannelListener(listener: ChannelListener) {
    }
}

