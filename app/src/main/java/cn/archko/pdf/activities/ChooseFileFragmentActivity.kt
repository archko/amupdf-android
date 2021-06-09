package cn.archko.pdf.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import cn.archko.pdf.LocalBackPressedDispatcher
import cn.archko.pdf.NavGraph
import cn.archko.pdf.common.Graph
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.theme.AppThemeState
import cn.archko.pdf.theme.ColorPallet
import cn.archko.pdf.theme.ComposeCookBookTheme
import cn.archko.pdf.theme.blue700
import cn.archko.pdf.theme.green700
import cn.archko.pdf.theme.orange700
import cn.archko.pdf.theme.purple700
import cn.archko.pdf.utils.LocalSysUiController
import cn.archko.pdf.utils.SystemUiController
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.pager.ExperimentalPagerApi
import com.umeng.analytics.MobclickAgent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author archko
 */
open class ChooseFileFragmentActivity : ComponentActivity() {

    @ExperimentalPagerApi
    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This app draws behind the system bars, so we want to handle fitting system windows
        WindowCompat.setDecorFitsSystemWindows(window, true)

        val options = PreferenceManager.getDefaultSharedPreferences(this)

        setContent {
            val systemUiController = remember { SystemUiController(window) }
            val appTheme = remember {
                mutableStateOf(
                    AppThemeState(
                        darkTheme = options.getBoolean(
                            PdfOptionsActivity.PREF_DART_THEME,
                            false
                        )
                    )
                )
            }
            val color = if (appTheme.value.darkTheme) {
                when (appTheme.value.pallet) {
                    ColorPallet.GREEN -> green700.copy(alpha = 0.3f)
                    ColorPallet.BLUE -> blue700.copy(alpha = 0.3f)
                    ColorPallet.ORANGE -> orange700.copy(alpha = 0.3f)
                    ColorPallet.PURPLE -> purple700.copy(alpha = 0.3f)
                }
            } else {
                when (appTheme.value.pallet) {
                    ColorPallet.GREEN -> green700
                    ColorPallet.BLUE -> blue700
                    ColorPallet.ORANGE -> orange700
                    ColorPallet.PURPLE -> purple700
                }
            }
            systemUiController.setStatusBarColor(
                color = color,
                darkIcons = appTheme.value.darkTheme
            )
            val changeTheme: (Boolean) -> Unit = { it ->
                options.edit().putBoolean(PdfOptionsActivity.PREF_DART_THEME, it).apply()
            }
            CompositionLocalProvider(
                LocalSysUiController provides systemUiController,
                LocalBackPressedDispatcher provides this.onBackPressedDispatcher
            ) {
                ProvideWindowInsets {
                    ComposeCookBookTheme(
                        darkTheme = appTheme.value.darkTheme,
                        colorPallet = appTheme.value.pallet
                    ) {
                        NavGraph(changeTheme, appTheme, up = { finish() })
                    }
                }
            }
        }

        checkSdcardPermission()

        // 设置为U-APP场景
        MobclickAgent.setScenarioType(this, MobclickAgent.EScenarioType.E_UM_NORMAL);
    }

    public override fun onResume() {
        super.onResume()
        MobclickAgent.onPageStart(TAG)
        //MobclickAgent.onResume(mContext); // BaseActivity中已经统一调用，此处无需再调用
    }

    public override fun onPause() {
        super.onPause()
        MobclickAgent.onPageEnd(TAG)
        //MobclickAgent.onPause(mContext); // BaseActivity中已经统一调用，此处无需再调用
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
            loadView()
        }
    }

    /**
     * Requests the sdcard permission.
     * If the permission has been denied previously, a SnackBar will prompt the user to grant the
     * permission, otherwise it is requested directly.
     */
    private fun requestSdcardPermission() {
        Logcat.d(TAG, "sdcard permission has NOT been granted. Requesting permission.")

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
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //  权限通过
                //((RefreshableFragment) (mPagerAdapter.getItem(mViewPager.getCurrentItem()))).update();
                loadView()
            } else {
                // 权限拒绝
                Toast.makeText(this, "没有获取sdcard的读取权限", Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

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
        private val REQUEST_PERMISSION_CODE = 0x001
    }
}
