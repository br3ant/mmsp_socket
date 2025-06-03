package com.br3ant.mmsp_socket

/**
 * @author houqiqi on 2025/5/30
 */
interface ChannelServer {

    fun launch()

    fun send(type: CmdType, data: ByteArray)

    fun stop()

    fun setMessageReceiver(messageReceiver: MessageReceiver)
}