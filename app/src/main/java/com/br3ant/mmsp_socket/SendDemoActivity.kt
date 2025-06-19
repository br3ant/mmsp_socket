package com.br3ant.mmsp_socket

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.br3ant.mmsp_socket.MMSPSender.rgbLikeToJpg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


/**
 * @author houqiqi on 2025/5/28
 */
class SendDemoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SendDemoActivity"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)

        val iv = findViewById<ImageView>(R.id.iv)

        findViewById<Button>(R.id.btn_ws).setOnClickListener {
            MMSPSender.start(defaultCfg.copy(debug = true, port = 49999))
        }

        findViewById<Button>(R.id.btn_jpg).setOnClickListener {
            sendJpg(iv)

        }
        findViewById<Button>(R.id.btn_rgb).setOnClickListener {
            sendRgb(iv)
        }

        findViewById<Button>(R.id.btn_bgr).setOnClickListener {
            sendBgr(iv)
        }

        MMSPSender.setMessageReceiver(object : MessageReceiver {
            override fun onTTS(tts: String) {
                Log.i(TAG, "MessageReceiver onTTS :$tts")
            }

            override fun onServerParamUpdate(param: ServerParam) {
                Log.i(TAG, "MessageReceiver onServerParamUpdate :$param")
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        MMSPSender.stop()
    }

    private val width = 540
    private val height = 1200

    var jpgJob: Job? = null
    fun sendJpg(iv: ImageView) {
        if (jpgJob?.isActive == true) return

        jpgJob = lifecycleScope.launch(Dispatchers.IO) {
            val jpgs = File(getExternalFilesDir(null), "br3ant/jpgs")
            jpgs.list()?.sorted()?.forEach {
                val data = File(jpgs, it).readBytes()
                MMSPSender.syncSendJpg(data, width, height)
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
                MMSPSender.sendBgr(data, width, height)

                val rgbs = rgbLikeToJpg(data, FORMAT.RGB, width, height)
                val options = BitmapFactory.Options()
                options.inMutable = true
                val bitmap = BitmapFactory.decodeByteArray(rgbs, 0, rgbs.size, options)
                withContext(Dispatchers.Main) {
                    iv.setImageBitmap(bitmap)
                }
//                    delay(10)
            } ?: run {
                Log.i(TAG, "rgbs is null")
            }

        }
    }

    var bgrJob: Job? = null
    fun sendBgr(iv: ImageView) {
        if (bgrJob?.isActive == true) return
        bgrJob = lifecycleScope.launch(Dispatchers.IO) {
            val bgrs = File(getExternalFilesDir(null), "br3ant/bgr_files")
            bgrs.list()?.sorted()?.forEach {
                val data = File(bgrs, it).readBytes()
                Log.i(TAG, "bgr size:${data.size}")

                MMSPSender.sendBgr(data, width, height)
                delay(500)
//                val bgr = rgbLikeToJpg(data, FORMAT.BGR, width, height)
//                val options = BitmapFactory.Options()
//                options.inMutable = true
//                val bitmap = BitmapFactory.decodeByteArray(bgr, 0, bgr.size, options)
//                withContext(Dispatchers.Main) {
//                    iv.setImageBitmap(bitmap)
//                }
            } ?: run {
                Log.i(TAG, "bgrs is null")
            }
        }
    }
}