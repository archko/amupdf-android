package cn.archko.pdf.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import cn.archko.pdf.LocalBackPressedDispatcher
import cn.archko.pdf.ui.home.SettingsScreen
import cn.archko.pdf.ui.settings.ProvidePreferenceLocals
import com.google.samples.apps.nowinandroid.core.ui.component.NiaBackground
import com.google.samples.apps.nowinandroid.core.ui.theme.NiaTheme

/**
 * @author: archko 2024/3/14 :16:14
 */
class SettingsActivity : AnalysticActivity() {
    companion object {
        const val TAG = "PdfOptionsActivity"

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, SettingsActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        isLive = true
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            val sysDark = isSystemInDarkTheme()
            val darkTheme = remember {
                mutableStateOf(sysDark)
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
                            ProvidePreferenceLocals { SettingsScreen() }
                        }
                    }
                }
            }
        }
    }
}