package com.br3ant.mmsp_socket

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.huisheng.IMMSPService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
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

    private var mmspService: IMMSPService? = null


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
        MMSPClientHelper.stop()
        mmspService?.stop()
    }

    fun connectMMSP() {
//        startMMSPService()

        MMSPClientHelper.start("192.168.2.1", 9095, object : ServerMessageListener {
            override fun onConnected() {
                MMSPClientHelper.sendToAll(CmdType.CAMERA_FORMAT, JSONObject().apply {
                    put("format", 1)
                    put("width", width)
                    put("height", height)
                }.toString().toByteArray(Charsets.UTF_8))
            }

            override fun onMessage(type: CmdType, data: ByteArray) {
                Log.i(TAG, "MMSP Service WsClient cmd $type")
            }
        })
    }

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val remoteService = IMMSPService.Stub.asInterface(service)
            mmspService = remoteService

            try {
                val result = remoteService.start(9095)
                Log.i(TAG, "MMSP Service Connect result $result")

            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "MMSP Service onServiceDisconnected")
        }
    }

    fun startMMSPService() {
        val intent = Intent("com.huisheng.REMOTE_SERVICE")
        intent.setPackage("com.iflytek.aiui.demo")
        this.bindService(intent, connection, Service.BIND_AUTO_CREATE)
    }

    var jpgJob: Job? = null
    fun sendJpg(iv: ImageView) {
        if (jpgJob?.isActive == true) return

        jpgJob = lifecycleScope.launch(Dispatchers.IO) {
            val jpgs = File(getExternalFilesDir(null), "br3ant/jpgs")
            jpgs.list()?.sorted()?.forEach {
                val data = File(jpgs, it).readBytes()
                MMSPClientHelper.sendToAll(CmdType.CAMERA_IMG, data)
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
                MMSPClientHelper.sendRgbLikeData(CmdType.HUMAN_IMG, data, FORMAT.RGB, width, height)

                val rgbs = MMSPClientHelper.rgbLikeToJpg(data, FORMAT.RGB, width, height)
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
}