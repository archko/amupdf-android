package cn.archko.pdf.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import cn.archko.pdf.LocalBackPressedDispatcher
import cn.archko.pdf.theme.*
import cn.archko.pdf.ui.home.AboutScreen
import cn.archko.pdf.utils.LocalSysUiController
import cn.archko.pdf.utils.SystemUiController
import com.google.accompanist.insets.ProvideWindowInsets
import com.umeng.analytics.MobclickAgent

/**
 * @author: archko 2018/12/16 :9:43
 */
class AboutActivity : ComponentActivity() {

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            val systemUiController = remember { SystemUiController(window) }
            val appTheme = remember { mutableStateOf(AppThemeState()) }
            val color = when (appTheme.value.pallet) {
                ColorPallet.GREEN -> green700
                ColorPallet.BLUE -> blue700
                ColorPallet.ORANGE -> orange700
                ColorPallet.PURPLE -> purple700
            }
            systemUiController.setStatusBarColor(
                color = color,
                darkIcons = appTheme.value.darkTheme
            )
            CompositionLocalProvider(
                LocalSysUiController provides systemUiController,
                LocalBackPressedDispatcher provides this.onBackPressedDispatcher
            ) {
                ProvideWindowInsets {
                    ComposeCookBookTheme(
                        darkTheme = appTheme.value.darkTheme,
                        colorPallet = appTheme.value.pallet
                    ) {
                        AboutScreen() {
                            onBackPressed()
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPageEnd("about")
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onPageStart("about")
    }

    companion object {

        fun start(context: Context) {
            context.startActivity(Intent(context, AboutActivity::class.java))
        }
    }
}