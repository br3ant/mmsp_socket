package com.br3ant.mmsp_socket

/**
 * @author houqiqi on 2025/6/24
 */
fun interface MessageReceiver {
    fun onMessage(cmdType: CmdType, data: ByteArray)
}