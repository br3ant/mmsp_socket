package com.br3ant.mmsp_socket.utils

import java.util.LinkedList
import kotlin.math.ln
import kotlin.math.pow

/**
 * @author houqiqi on 2025/6/19
 */
data class ByteRecord(val timestamp: Long, val bytesSent: Int)

class ByteRateCounter(private val windowSeconds: Int = 10) {
    private val records = LinkedList<ByteRecord>()

    fun logBytes(bytesSent: Int) {
        val now = System.currentTimeMillis()
        records.add(ByteRecord(now, bytesSent))

        // 移除超过时间窗口的记录
        val windowStart = now - windowSeconds * 1000
        while (records.isNotEmpty() && records.first.timestamp < windowStart) {
            records.removeFirst()
        }

        val totalBytes = records.sumOf { it.bytesSent }
        val timeSpanMillis = if (records.size > 1)
            records.last.timestamp - records.first.timestamp
        else
            1L

        val ratePerSecond = totalBytes.toDouble() / (timeSpanMillis / 1000.0)

        // 使用人类友好单位格式化
        val readableRate = formatBytesPerSecond(ratePerSecond)
        println("过去 $windowSeconds 秒平均速率：$readableRate")
    }

    private fun formatBytesPerSecond(bytesPerSec: Double): String {
        if (bytesPerSec < 1) return "%.2f B/s".format(bytesPerSec)

        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s", "TB/s")
        val exp = ln(bytesPerSec) / ln(1024.0)
        val index = exp.toInt().coerceAtMost(units.lastIndex)
        val scaled = bytesPerSec / 1024.0.pow(index.toDouble())

        return "%.2f %s".format(scaled, units[index])
    }
}