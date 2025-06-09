package com.br3ant.mmsp_socket

import kotlinx.serialization.Serializable

/**
 * @author houqiqi on 2025/6/9
 */
@Serializable
data class ServerParam(

    /**
     * 传输tts
     */
    val transTTS: Boolean = true,

    /**
     * 传输cae
     */
    val transCAE: Boolean = false,
)


