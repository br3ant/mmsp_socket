package com.br3ant.mmsp_socket

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


/**
 * @author houqiqi on 2025/5/28
 */
class SendDemoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SendDemoActivity"
        private const val width = 540
        private const val height = 1200
    }

    private var mmspWsServer: MMSPServer =
        MMSPServer(
            ChannelMode.WS,
            port = 9095,
            onNewConnected = { onMMSPWsServerNewConnect() }
        ) { cmd, data ->

        }

    private val hostname by lazy { MMSPClient.hostnameFormLocal(APP.context)!! }

    private var audioTransClient: MMSPClient =
        MMSPClient(ChannelMode.WS, hostname, 9096) { cmd, data ->

        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)

        val iv = findViewById<ImageView>(R.id.iv)
        findViewById<Button>(R.id.btn_connect).setOnClickListener {
            connectMMSP()
        }

        findViewById<Button>(R.id.btn_jpg).setOnClickListener {
            sendJpg(iv)

        }
        findViewById<Button>(R.id.btn_rgb).setOnClickListener {
            sendRgb(iv)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
//        mmspWsServer.stop()
        audioTransClient.stop()
//        mmspClient.stop()
    }

    fun connectMMSP() {
//        mmspWsServer.start()
        audioTransClient.start()
//        mmspClient.start()
    }

    fun onMMSPWsServerNewConnect() {
        lifecycleScope.launch(Dispatchers.IO) {
            mmspWsServer.server.sendFormat(CmdType.CAMERA_FORMAT, width, height)
        }
    }

    var jpgJob: Job? = null
    fun sendJpg(iv: ImageView) {
        if (jpgJob?.isActive == true) return

        jpgJob = lifecycleScope.launch(Dispatchers.IO) {
            val jpgs = File(getExternalFilesDir(null), "br3ant/jpgs")
            jpgs.list()?.sorted()?.forEach {
                val data = File(jpgs, it).readBytes()
                mmspWsServer.server.send(CmdType.CAMERA_IMG, data)
                val options = BitmapFactory.Options()
                options.inMutable = true
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)
                withContext(Dispatchers.Main) {
                    iv.setImageBitmap(bitmap)
                }
//                    delay(10)
            } ?: run {
                Log.i(TAG, "jpgs is null")
            }

        }
    }


    var rgbJob: Job? = null

    fun sendRgb(iv: ImageView) {
        if (rgbJob?.isActive == true) return
        rgbJob = lifecycleScope.launch(Dispatchers.IO) {
            val rgbs = File(getExternalFilesDir(null), "br3ant/rgb_files")
            rgbs.list()?.sorted()?.forEach {
                val data = File(rgbs, it).readBytes()
                audioTransClient.client.sendRgbLikeData(CmdType.HUMAN_IMG, data, FORMAT.RGB, width, height)

//                val rgbs = RGBUtils.rgbLikeToJpg(data, FORMAT.RGB, width, height)
//                val options = BitmapFactory.Options()
//                options.inMutable = true
//                val bitmap = BitmapFactory.decodeByteArray(rgbs, 0, rgbs.size, options)
//                withContext(Dispatchers.Main) {
//                    iv.setImageBitmap(bitmap)
//                }
//                    delay(10)
            } ?: run {
                Log.i(TAG, "rgbs is null")
            }

        }
    }
}