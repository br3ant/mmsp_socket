package com.br3ant.mmsp_socket

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.br3ant.mmsp_socket.CmdType
import com.br3ant.mmsp_socket.FORMAT
import com.br3ant.mmsp_socket.MMSPSender
import com.br3ant.mmsp_socket.MMSPSender.rgbLikeToJpg
import com.br3ant.mmsp_socket.MessageReceiver
import com.br3ant.mmsp_socket.Mode
import com.br3ant.mmsp_socket.defaultCfg
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
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)
        MMSPSender.start(defaultCfg.copy(debug = true, mode = Mode.WS))
        val iv = findViewById<ImageView>(R.id.iv)
        findViewById<Button>(R.id.btn_jpg).setOnClickListener {
            sendJpg(iv)

        }
        findViewById<Button>(R.id.btn_rgb).setOnClickListener {
            sendRgb(iv)
        }

        MMSPSender.setMessageReceiver(object : MessageReceiver {
            override fun onTTS(tts: String) {
                Log.i(TAG, "MessageReceiver onTTS :$tts")
            }
        })
    }

    var jpgJob: Job? = null
    fun sendJpg(iv: ImageView) {
        if (jpgJob?.isActive == true) return

        jpgJob = lifecycleScope.launch(Dispatchers.IO) {
            val jpgs = "/sdcard/br3ant/jpgs"
            File(jpgs).list().sorted().forEach {
                val data = File(jpgs, it).readBytes()
                MMSPSender.sendToAll(CmdType.CAMERA_IMG, data)
                val options = BitmapFactory.Options()
                options.inMutable = true
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)
                withContext(Dispatchers.Main) {
                    iv.setImageBitmap(bitmap)
                }
//                    delay(10)
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MMSPSender.stop()
    }


    var rgbJob: Job? = null
    private val width = 540
    private val height = 1200
    fun sendRgb(iv: ImageView) {
        if (rgbJob?.isActive == true) return
        rgbJob = lifecycleScope.launch(Dispatchers.IO) {
            val rgbs = "/sdcard/br3ant/rgb_files"
            File(rgbs).list().sorted().forEach {
                val data = File(rgbs, it).readBytes()
                MMSPSender.sendRgbLikeData(CmdType.HUMAN_IMG, data, FORMAT.RGB, width, height)

                val rgbs = rgbLikeToJpg(data, FORMAT.RGB, width, height)
                val options = BitmapFactory.Options()
                options.inMutable = true
                val bitmap = BitmapFactory.decodeByteArray(rgbs, 0, rgbs.size, options)
                withContext(Dispatchers.Main) {
                    iv.setImageBitmap(bitmap)
                }
//                    delay(10)
            }

        }
    }
}