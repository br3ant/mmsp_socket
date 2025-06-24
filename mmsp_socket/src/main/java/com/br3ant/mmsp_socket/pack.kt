package com.br3ant.mmsp_socket

import android.util.Log
import com.br3ant.mmsp_socket.MMSPServer.Companion.TAG
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * @author houqiqi on 2025/6/24
 */
fun MessageReceiver?.dispatch(message: ByteBuffer) {
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
            Log.d(TAG, "onMessage buffer 过短 dataLen:${dataLen} limit:${limit}")
            return
        }
        val dataArray = ByteArray(dataLen)
        data.position(9)
        data.get(dataArray, 0, dataLen)
        this?.onMessage(CmdType.from(cmd), dataArray)
    }
}