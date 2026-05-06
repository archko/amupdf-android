package com.baidu.ai.edge.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import com.baidu.ai.edge.core.base.Consts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;

/**
 * Created by linyiran on 6/16/22.
 */
public abstract class BaseActivity extends AppCompatActivity {
    private static final int REQUEST_PERMISSION = 1;

    protected boolean allPermissionsGranted;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkAndRequestPermissions();
    }

    /**
     * 检查并请求所有需要的权限
     */
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 需要 MANAGE_EXTERNAL_STORAGE 权限
            if (Environment.isExternalStorageManager()) {
                checkAndRequestRuntimePermissions();
            } else {
                requestManageExternalStorage();
            }
        } else {
            checkAndRequestRuntimePermissions();
        }
    }

    /**
     * 检查并请求运行时权限（WRITE_EXTERNAL_STORAGE, CAMERA 等）
     */
    private void checkAndRequestRuntimePermissions() {
        String[] permissions = getRequiredPermissions();

        boolean allGranted = true;
        for (String perm : permissions) {
            if (PermissionChecker.checkSelfPermission(this, perm) != PermissionChecker.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            allPermissionsGranted = true;
            onPermissionsGranted();
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION);
        }
    }

    /**
     * 获取需要的权限列表
     */
    private String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 从技术上讲不需要 WRITE_EXTERNAL_STORAGE，
            return new String[]{
                    Manifest.permission.CAMERA,
                    // 以下权限根据实际需要保留
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.READ_PHONE_STATE
            };
        } else {
            // Android 6-10 需要 WRITE_EXTERNAL_STORAGE
            return new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.READ_PHONE_STATE
            };
        }
    }

    /**
     * 请求 MANAGE_EXTERNAL_STORAGE 权限（Android 11+）
     */
    private void requestManageExternalStorage() {
        if (!Consts.AUTH_REQUIRE_SDCARD) {
            // 不需要SD卡权限，直接检查其他权限
            checkAndRequestRuntimePermissions();
            return;
        }

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(
                this,
                androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert);
        alertBuilder.setMessage("需授权访问SD卡文件");
        alertBuilder.setCancelable(false);
        alertBuilder.setPositiveButton("去设置", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
        alertBuilder.setNegativeButton("取消", (dialog, which) -> showPermissionDeniedMessage());
        alertBuilder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION) {
            boolean allGranted = true;
            for (int grantRes : grantResults) {
                if (grantRes != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                allPermissionsGranted = true;
                onPermissionsGranted();
            } else {
                showPermissionDeniedMessage();
            }
        }
    }

    private void showPermissionDeniedMessage() {
        runOnUiThread(() -> new AlertDialog.Builder(this)
                .setTitle("权限不足")
                .setMessage("需要相机和存储权限才能正常使用此功能")
                .setPositiveButton("重新申请", (dialog, which) -> checkAndRequestPermissions())
                .setNegativeButton("退出", (dialog, which) -> finish())
                .setCancelable(false)
                .show());
    }

    /**
     * 权限全部授予后回调，子类实现此方法进行初始化
     */
    protected abstract void onPermissionsGranted();
}