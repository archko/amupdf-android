/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.archko.pdf.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import cn.archko.pdf.LocalBackPressedDispatcher
import cn.archko.pdf.NavGraph
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.theme.JetsnackTheme
import cn.archko.pdf.utils.LocalSysUiController
import cn.archko.pdf.utils.SystemUiController
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.pager.ExperimentalPagerApi

class MainActivity : ComponentActivity() {

    @ExperimentalPagerApi
    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This app draws behind the system bars, so we want to handle fitting system windows
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            val systemUiController = remember { SystemUiController(window) }
            CompositionLocalProvider(
                LocalSysUiController provides systemUiController,
                LocalBackPressedDispatcher provides this.onBackPressedDispatcher
            ) {
                ProvideWindowInsets {
                    JetsnackTheme {
                        NavGraph()
                    }
                }
            }
        }
        checkSdcardPermission()
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    private fun checkSdcardPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // WRITE_EXTERNAL_STORAGE permission has not been granted.

            requestSdcardPermission()
        } else {
        }
    }

    private fun requestSdcardPermission() {
        Logcat.d("sdcard permission has NOT been granted. Requesting permission.")

        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example if the user has previously denied the permission.
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_CODE
            )
        } else {

            // sdcard permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //  权限通过
            } else {
                // 权限拒绝
                Toast.makeText(this, "没有获取sdcard的读取权限", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {

        @JvmField
        val PREF_HOME = "Home"
        private val REQUEST_PERMISSION_CODE = 0x001
    }
}
