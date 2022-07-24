package cn.archko.pdf.activities

import ImageViewer
import TextViewer
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewConfiguration
import android.widget.Toast
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import cn.archko.pdf.LocalBackPressedDispatcher
import cn.archko.pdf.common.Graph
import cn.archko.pdf.common.IntentFile
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.common.SensorHelper
import cn.archko.pdf.entity.State
import cn.archko.pdf.ui.home.LoadingView
import cn.archko.pdf.utils.Utils
import cn.archko.pdf.viewmodel.PDFViewModel
import com.google.samples.apps.nowinandroid.core.ui.component.NiaBackground
import com.google.samples.apps.nowinandroid.core.ui.theme.NiaTheme
import kotlinx.coroutines.launch

/**
 * @author: archko 2022/7/11 :9:49 上午
 */
class ComposeTextActivity : ComponentActivity() {

    private var path: String? = null
    private var mUri: Uri? = null
    private var sensorHelper: SensorHelper? = null
    private val preferencesRepository = PdfOptionRepository(Graph.dataStore)
    private val pdfViewModel: PDFViewModel = PDFViewModel()
    protected var pageNumberToast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)

        initIntent()

        if (TextUtils.isEmpty(path)) {
            error()
            return
        }

        sensorHelper = SensorHelper(this@ComposeTextActivity)
        lifecycleScope.launch {
            pdfViewModel.loadBookProgressByPath(path!!, preferencesRepository)
        }
        setView()
    }

    private fun error() {
        Toast.makeText(
            this@ComposeTextActivity,
            "error file path:$path",
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun setView() {
        var margin = window.decorView.height
        if (margin <= 0) {
            margin = ViewConfiguration.get(this).scaledTouchSlop * 2
        } else {
            margin = (margin * 0.03).toInt()
        }
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
                                if (path!!.endsWith("txt", true)) {
                                    val showLoading = remember { mutableStateOf(true) }
                                    val result by pdfViewModel.textFlow.collectAsState()
                                    if (State.INIT == result.state) {
                                        lifecycleScope.launch {
                                            pdfViewModel.loadTextDoc(path!!)
                                        }
                                    }
                                    showLoading.value = (result.state != State.FINISHED)
                                    if (result.state == State.INIT || result.list == null) {
                                        LoadingView(showLoading)
                                    } else if (State.ERROR == result.state) {
                                        error()
                                    } else {
                                        TextViewer(
                                            result = result,
                                            pdfViewModel = pdfViewModel,
                                            onClick = { pos -> showToast(pos, result.list!!.size) },
                                            height = window.decorView.height,
                                            margin = margin,
                                        )
                                    }
                                } else {
                                    val showLoading = remember { mutableStateOf(true) }
                                    val result by pdfViewModel.pageFlow.collectAsState()
                                    if (result.state == State.INIT) {
                                        lifecycleScope.launch {
                                            pdfViewModel.loadPdfDoc2(
                                                this@ComposeTextActivity,
                                                path!!,
                                                null
                                            )
                                        }
                                    }
                                    showLoading.value = (result.state != State.FINISHED)
                                    if (result.state == State.INIT || result.list == null || pdfViewModel.mupdfDocument == null) {
                                        LoadingView(showLoading)
                                    } else if (State.ERROR == result.state) {
                                        error()
                                    } else {
                                        ImageViewer(
                                            result = result,
                                            pdfViewModel = pdfViewModel,
                                            mupdfDocument = pdfViewModel.mupdfDocument!!,
                                            onClick = { pos -> showToast(pos, result.list!!.size) },
                                            width = window.decorView.width,
                                            height = window.decorView.height,
                                            margin = margin,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showToast(pos: Int, size: Int) {
        val pageText = (pos + 1).toString() + "/" + size
        if (pageNumberToast != null) {
            pageNumberToast!!.setText(pageText)
        } else {
            pageNumberToast =
                Toast.makeText(this@ComposeTextActivity, pageText, Toast.LENGTH_SHORT)
        }
        pageNumberToast!!.setGravity(
            Gravity.BOTTOM or Gravity.START,
            Utils.dipToPixel(15f),
            0
        )
        pageNumberToast!!.show()
    }

    override fun onPause() {
        super.onPause()
        sensorHelper?.onPause()
    }

    override fun onResume() {
        super.onResume()
        sensorHelper?.onResume()
    }

    private fun initIntent() {
        if (!TextUtils.isEmpty(path)) {
            return
        }

        path = IntentFile.processIntentAction(intent, this@ComposeTextActivity)
    }

    companion object {

        fun start(context: Context, path: String) {
            val intent = Intent(context, ComposeTextActivity::class.java)
            intent.putExtra("path", path)
            context.startActivity(intent)
        }
    }
}