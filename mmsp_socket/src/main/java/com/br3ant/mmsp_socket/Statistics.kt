package com.br3ant.mmsp_socket

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * @author houqiqi on 2025/5/29
 */
object Statistics {
    private var time: Long = System.currentTimeMillis()
    private val countMap = ConcurrentHashMap<CmdType, Int>()
    private const val WINDOW = 5000

    fun framePoint(cmdType: CmdType) {
        if (MMSPSender.config.debug.not()) return

        val count = countMap.getOrDefault(cmdType, 0)
        countMap[cmdType] = count + 1

        val duration = System.currentTimeMillis() - time
        if (duration > WINDOW) {
            countMap.clear()

            val fms = (count / (duration / 1000)).toInt()
            if (cmdType == CmdType.HUMAN_IMG) {
                Log.i(MMSPSender.TAG, "虚拟人帧率：${fms}")
            } else if (cmdType == CmdType.CAMERA_IMG) {
                Log.i(MMSPSender.TAG, "摄像头帧率：${fms}")
            }

            time = System.currentTimeMillis()
        }
    }
}