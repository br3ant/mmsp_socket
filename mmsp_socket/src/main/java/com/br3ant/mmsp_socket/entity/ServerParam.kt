package com.br3ant.mmsp_socket.entity

import kotlinx.serialization.Serializable

/**
 * @author houqiqi on 2025/6/9
 */
@Serializable
data class ServerParam(

    /**
     * 传输tts
     */
    val transTTS: Boolean = false,

    /**
     * 播放cae
     */
    val playCAE: Boolean = false,
)