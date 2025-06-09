package com.br3ant.mmsp_socket

/**
 * @author houqiqi on 2025/6/3
 */
interface MessageReceiver {

    fun onTTS(tts: String)

    fun onServerParamUpdate(param: ServerParam)
}