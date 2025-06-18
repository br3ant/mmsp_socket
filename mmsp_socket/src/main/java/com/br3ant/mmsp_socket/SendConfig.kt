package com.br3ant.mmsp_socket

/**
 * @author houqiqi on 2025/5/29
 */
data class SendConfig(
    val debug: Boolean = false,
    val port: Int = 49999,
)



val defaultCfg = SendConfig()
