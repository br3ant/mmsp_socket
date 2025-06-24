package com.br3ant.mmsp_socket

import android.app.Application

/**
 * @author houqiqi on 2025/6/24
 */
class APP : Application() {

    override fun onCreate() {
        super.onCreate()
        context = this
    }

    companion object {
        lateinit var context: Application
    }
}