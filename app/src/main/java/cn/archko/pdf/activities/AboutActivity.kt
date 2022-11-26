package cn.archko.pdf.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import cn.archko.pdf.LocalBackPressedDispatcher
import cn.archko.pdf.ui.home.AboutScreen
import com.google.samples.apps.nowinandroid.core.ui.component.NiaBackground
import com.google.samples.apps.nowinandroid.core.ui.theme.NiaTheme
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

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            NiaTheme {
                CompositionLocalProvider(
                    LocalBackPressedDispatcher provides this.onBackPressedDispatcher
                ) {
                    NiaBackground {
                        Scaffold(
                            modifier = Modifier,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onBackground,
                        ) {
                            Row(
                                Modifier
                                    .fillMaxSize()
                                    .windowInsetsPadding(
                                        WindowInsets.safeDrawing.only(
                                            WindowInsetsSides.Horizontal
                                        )
                                    )
                            ) {
                                AboutScreen() {
                                    onBackPressed()
                                }
                            }
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