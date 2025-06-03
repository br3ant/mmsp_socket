package com.br3ant.mmsp_socket

/**
 * @author houqiqi on 2025/5/29
 */
data class SendConfig(
    val debug: Boolean = false,
    val mode: Mode = Mode.SOCKET,
    val cameraWidth: Int = 612,
    val cameraHeight: Int = 612,
    val humanWidth: Int = 720,
    val humanHeight: Int = 1280
)

enum class Mode {
    SOCKET, WS
}

val defaultCfg = SendConfig()
