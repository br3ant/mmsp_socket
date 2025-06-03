package com.br3ant.mmsp_socket;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

/**
 * @Date 2020.06.20 13:03
 * @Comment
 */
public class BaseActivity extends Activity {
    private PermissionsListener listener;
    private int REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 将activity设置为全屏显示
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initPermission();
        requestPermissions(99);
    }

    // 请求权限
    public void requestPermissions(int requestCode) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // 先判断有没有权限
                if (!Environment.isExternalStorageManager()) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + this.getPackageName()));
                    startActivityForResult(intent, REQUEST_CODE);
                    return;
                }
            }

            ArrayList<String> requestPerssionArr = new ArrayList<>();

            int hasSdcardRead = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (hasSdcardRead != PackageManager.PERMISSION_GRANTED) {
                requestPerssionArr.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }

            int hasSdcardWrite = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasSdcardWrite != PackageManager.PERMISSION_GRANTED) {
                requestPerssionArr.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }

            int readPhoneState = checkSelfPermission(Manifest.permission.READ_PHONE_STATE);
            if (readPhoneState != PackageManager.PERMISSION_GRANTED) {
                requestPerssionArr.add(Manifest.permission.READ_PHONE_STATE);
            }

            int recodeAudio = checkSelfPermission(Manifest.permission.RECORD_AUDIO);
            if (recodeAudio != PackageManager.PERMISSION_GRANTED) {
                requestPerssionArr.add(Manifest.permission.RECORD_AUDIO);
            }

            int camera = checkSelfPermission(Manifest.permission.CAMERA);
            if (camera != PackageManager.PERMISSION_GRANTED) {
                requestPerssionArr.add(Manifest.permission.CAMERA);
            }

            // 是否应该显示权限请求
            if (requestPerssionArr.size() >= 1) {
                String[] requestArray = new String[requestPerssionArr.size()];
                for (int i = 0; i < requestArray.length; i++) {
                    requestArray[i] = requestPerssionArr.get(i);
                }
                requestPermissions(requestArray, requestCode);
            } else {
                if (this.listener != null) {
                    this.listener.init();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String permissions[] = {
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CAMERA
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                //进入到这里代表没有权限.
            }
        }
        String tmpList[] = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        boolean flag = false;
        for (int i = 0; i < permissions.length; i++) {
            if (PackageManager.PERMISSION_GRANTED == grantResults[i]) {
                flag = true;
            } else {
                flag = false;
            }
        }

        if (flag && this.listener != null) {
            this.listener.init();
        }
    }

    protected void setListener(PermissionsListener listener) {
        this.listener = listener;
    }

    public interface PermissionsListener {

        void init();
    }
}
