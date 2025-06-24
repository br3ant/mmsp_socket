package com.br3ant.mmsp_socket

enum class CmdType(val type: Byte) {
    /**
     * 摄像头格式
     */
    CAMERA_FORMAT(0x07),

    /**
     * 摄像头数据
     */
    CAMERA_IMG(0x08),

    /**
     * 原始音频数据
     */
    AUDIO(0x1A),

    /**
     * AIUI event
     */
    AIUI_EVENT(0x1B),

    /**
     * 虚拟人格式
     */
    HUMAN_FORMAT(0x1C),

    /**
     * 虚拟人数据
     */
    HUMAN_IMG(0x1D),

    /**
     * IAT
     */
    AIUI_IAT(0x1E),

    /**
     * NLP
     */
    AIUI_NLP(0x1F),

    /**
     * 客户端请求TTS
     */
    PLAY_TTS(0x50),

    /**
     * 配置参数
     */
    SET_PARAM(0x51),

    /**
     * 未知
     */
    UN_KNOW(-1);

    companion object {
        fun from(type: Byte): CmdType {
            return CmdType.entries.find { it.type == type } ?: UN_KNOW
        }
    }
}

enum class FORMAT {
    RGB, BGR, RGBA, BGRA, ARGB
}

enum class ChannelMode {
    SOCKET, WS, LOCAL_SOCKET
}