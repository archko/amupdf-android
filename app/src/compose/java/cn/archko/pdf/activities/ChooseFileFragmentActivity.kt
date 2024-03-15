package cn.archko.pdf.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import cn.archko.mupdf.R
import cn.archko.pdf.LocalBackPressedDispatcher
import cn.archko.pdf.NavGraph
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.PdfOptionRepository
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.samples.apps.nowinandroid.core.ui.component.NiaBackground
import com.google.samples.apps.nowinandroid.core.ui.theme.NiaTheme

/**
 * @author archko
 */
open class ChooseFileFragmentActivity : AnalysticActivity(), OnPermissionGranted {

    private val permissionCallbacks = arrayOfNulls<OnPermissionGranted>(PERMISSION_LENGTH)
    private var permissionDialog: Dialog? = null

    @OptIn(
        ExperimentalMaterialApi::class, ExperimentalPagerApi::class,
        ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        isLive = true
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)

        if (!isTaskRoot) { //如果是进入了二级页面,不处理.
            Logcat.d(TAG, "!isTaskRoot")
            return
        }
        //val windowSizeClass = calculateWindowSizeClass(this)
        setContent {
            val sysDark = isSystemInDarkTheme()
            val darkTheme = remember {
                mutableStateOf(sysDark)
            }
            val changeTheme: (Boolean) -> Unit = { it ->
                PdfOptionRepository.setDartTheme(it)
                darkTheme.value = !darkTheme.value
            }
            NiaTheme(darkTheme = darkTheme.value) {
                CompositionLocalProvider(
                    LocalBackPressedDispatcher provides this.onBackPressedDispatcher
                ) {
                    NiaBackground {
                        Scaffold(
                            modifier = Modifier,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onBackground,
                        ) { padding ->
                            Row(
                                Modifier
                                    .fillMaxSize()
                                    .windowInsetsPadding(
                                        WindowInsets.safeDrawing.only(
                                            WindowInsetsSides.Horizontal
                                        )
                                    )
                            ) {
                                NavGraph(
                                    changeTheme,
                                    darkTheme.value,
                                    up = { finish() },
                                    modifier = Modifier
                                        .padding(padding)
                                )
                            }
                        }
                    }
                }
            }
        }

        checkForExternalPermission()

        // 设置为U-APP场景
        //MobclickAgent.setScenarioType(this, MobclickAgent.EScenarioType.E_UM_NORMAL)
    }

    public override fun onResume() {
        super.onResume()
    }

    public override fun onPause() {
        super.onPause()
    }

    private fun checkForExternalPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkStoragePermission()) {
                requestStoragePermission(this, true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requestAllFilesAccess(this)
            }
        }
    }

    private fun checkStoragePermission(): Boolean {
        return (ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
                == PackageManager.PERMISSION_GRANTED)
    }

    open fun requestStoragePermission(
        onPermissionGranted: OnPermissionGranted, isInitialStart: Boolean
    ) {
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        permissionCallbacks[STORAGE_PERMISSION] = onPermissionGranted
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.grant_files_permission)
                .setMessage(R.string.grant_files_permission)
                .setPositiveButton(R.string.grant_cancel) { _, _ ->
                    finish()
                }
                .setNegativeButton(R.string.grant_ok) { _, _ ->
                    ActivityCompat.requestPermissions(
                        this, arrayOf(permission), STORAGE_PERMISSION
                    )
                    permissionDialog?.run {
                        permissionDialog!!.dismiss()
                    }
                }
            builder.setCancelable(false)
            builder.create().show()
        } else if (isInitialStart) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), STORAGE_PERMISSION)
        }
    }

    open fun requestAllFilesAccess(onPermissionGranted: OnPermissionGranted) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.grant_all_files_permission)
                .setMessage(R.string.grant_all_files_permission)
                .setPositiveButton(R.string.grant_cancel) { _, _ ->
                    finish()
                }
                .setNegativeButton(R.string.grant_ok) { _, _ ->
                    permissionCallbacks[ALL_FILES_PERMISSION] = onPermissionGranted
                    try {
                        val intent =
                            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                .setData(Uri.parse("package:$packageName"))
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Failed to initial activity to grant all files access",
                            e
                        )
                        Toast.makeText(this, "没有获取sdcard的读取权限", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            builder.setCancelable(false)
            builder.create().show()
        }
    }

    private fun isGranted(grantResults: IntArray): Boolean {
        return grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION) {
            if (isGranted(grantResults)) {
                permissionCallbacks[STORAGE_PERMISSION]!!
                    .onPermissionGranted()
                permissionCallbacks[STORAGE_PERMISSION] =
                    null
            } else {
                Toast.makeText(this, R.string.grantfailed, Toast.LENGTH_SHORT).show()
                permissionCallbacks[STORAGE_PERMISSION]?.let {
                    requestStoragePermission(
                        it,
                        false
                    )
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    override fun onPermissionGranted() {
        loadView()
    }

    //========================================

    private fun loadView() {

    }

    companion object {

        /**
         * Logging tag.
         */
        private val TAG = "ChooseFile"

        @JvmField
        val PREF_TAG = "ChooseFileActivity"

        @JvmField
        val PREF_HOME = "Home"

        const val PERMISSION_LENGTH = 2
        var STORAGE_PERMISSION = 0
        const val ALL_FILES_PERMISSION = 1
    }
}