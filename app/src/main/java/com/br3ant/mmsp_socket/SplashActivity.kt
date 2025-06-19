package com.br3ant.mmsp_socket

import android.content.Intent
import android.os.Bundle

class SplashActivity : BaseActivity(), BaseActivity.PermissionsListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.setListener(this)
        super.onCreate(savedInstanceState)

    }

    override fun init() {
        // 权限获取成功后，显示可以操作的界面
        startActivity(Intent(this, SendDemoActivity::class.java))
        finish()
    }

    companion object {
        private val TAG = SplashActivity::class.java.simpleName
    }
}
