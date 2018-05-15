package com.library.example.cifar10;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;


public class HomeActivity extends Activity {

    private static final String sRoot = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final String TAG = "HomeActivity";
    private ProgressDialog progressDialog;

    public void requestAllPower() {
        if (checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestAllPower();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PERMISSION_GRANTED) {
                    progressDialog = new ProgressDialog(this);
                    progressDialog.setMessage("正在复制模型文件，请稍等......");//3.设置显示内容
                    progressDialog.setCancelable(true);//4.设置可否用back键关闭对话框
                    progressDialog.show();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            initEnv();
                            Log.d(TAG, "copy completed");
                            progressDialog.dismiss();
                        }
                    }).start();
                } else {
                    finish();
                }
            }
        }
    }

    private void initEnv() {
        Utils.copyDBToSD(this, sRoot + "/Cifar10", "Cifar10_def.txt");
        Utils.copyDBToSD(this, sRoot + "/Cifar10", "labels.txt");
        Utils.copyDBToSD(this, sRoot + "/Cifar10", "mean.msg");
        Utils.copyDBToSD(this, sRoot + "/Cifar10", "model_param_conv1.msg");
        Utils.copyDBToSD(this, sRoot + "/Cifar10", "model_param_conv2.msg");
        Utils.copyDBToSD(this, sRoot + "/Cifar10", "model_param_conv3.msg");
        Utils.copyDBToSD(this, sRoot + "/Cifar10", "model_param_ip1.msg");
        Utils.copyDBToSD(this, sRoot + "/Cifar10", "model_param_ip2.msg");
    }

    public void enter_camera(View view) {
        startActivity(new Intent(this, MainActivity.class));
    }

    public void enter_image(View view) {
        startActivity(new Intent(this, ImageActivity.class));
    }
}
