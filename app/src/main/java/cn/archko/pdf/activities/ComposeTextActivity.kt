package cn.archko.pdf.activities

import ImageViewer
import TextViewer
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewConfiguration
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import cn.archko.pdf.App
import cn.archko.pdf.AppExecutors
import cn.archko.pdf.LocalBackPressedDispatcher
import cn.archko.pdf.common.BitmapCache
import cn.archko.pdf.common.Event
import cn.archko.pdf.common.Graph
import cn.archko.pdf.common.ImageWorker
import cn.archko.pdf.common.IntentFile
import cn.archko.pdf.common.PdfImageDecoder
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.common.SensorHelper
import cn.archko.pdf.common.StyleHelper
import cn.archko.pdf.entity.APage
import cn.archko.pdf.entity.State
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.utils.BitmapUtils
import cn.archko.pdf.utils.FileUtils
import cn.archko.pdf.utils.StatusBarHelper
import cn.archko.pdf.utils.Utils
import cn.archko.pdf.viewmodel.PDFViewModel
import com.google.samples.apps.nowinandroid.core.ui.component.NiaBackground
import com.google.samples.apps.nowinandroid.core.ui.theme.NiaTheme
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.launch
import java.io.File

/**
 * @author: archko 2022/7/11 :9:49 上午
 */
class ComposeTextActivity : ComponentActivity() {

    private var path: String? = null
    private var sensorHelper: SensorHelper? = null
    private val preferencesRepository = PdfOptionRepository(Graph.dataStore)
    private val pdfViewModel: PDFViewModel = PDFViewModel()
    private var mStyleHelper: StyleHelper? = null
    protected var pageNumberToast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StatusBarHelper.hideSystemUI(this)

        initIntent()

        if (TextUtils.isEmpty(path)) {
            error()
            return
        }

        if (!IntentFile.isText(path) && !IntentFile.isPdf(path)) {
            error()
            return
        }

        mStyleHelper = StyleHelper(this, preferencesRepository)
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

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
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
                                if (IntentFile.isText(path)) {
                                    val showLoading = remember { mutableStateOf(true) }
                                    val result by pdfViewModel.textFlow.collectAsState()
                                    if (State.INIT == result.state) {
                                        lifecycleScope.launch {
                                            pdfViewModel.loadTextDoc(path!!)
                                        }
                                    }
                                    showLoading.value = (result.state != State.FINISHED)
                                    if (State.ERROR == result.state) {
                                        error()
                                    } else if (result.state == State.INIT || result.list == null) {
                                        LoadingView()
                                    } else {
                                        TextViewer(
                                            result = result,
                                            pdfViewModel = pdfViewModel,
                                            styleHelper = mStyleHelper,
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
                                            pdfViewModel.loadPdfDoc(
                                                this@ComposeTextActivity,
                                                path!!,
                                                null
                                            )
                                        }
                                    }
                                    showLoading.value = (result.state != State.FINISHED)
                                    if (State.PASS == result.state) {
                                        PasswordDialog.show(this@ComposeTextActivity,
                                            object : PasswordDialog.PasswordDialogListener {
                                                override fun onOK(password: String?) {
                                                    lifecycleScope.launch {
                                                        pdfViewModel.loadPdfDoc(
                                                            this@ComposeTextActivity,
                                                            path!!,
                                                            password
                                                        )
                                                    }
                                                }

                                                override fun onCancel() {
                                                    error()
                                                }
                                            })
                                    } else if (State.ERROR == result.state) {
                                        error()
                                    } else if (result.state == State.INIT || result.list == null || pdfViewModel.mupdfDocument == null) {
                                        LoadingView()
                                    } else {
                                        ImageViewer(
                                            result = result,
                                            pdfViewModel = pdfViewModel,
                                            styleHelper = mStyleHelper,
                                            mupdfDocument = pdfViewModel.mupdfDocument!!,
                                            onClick = { pos -> showToast(pos, result.list!!.size) },
                                            width = window.decorView.width,
                                            height = window.decorView.height,
                                            margin = margin,
                                            finish = { finish() },
                                            ocr = { pos: Int, mupdfDocument: MupdfDocument, aPage: APage ->
                                                ocr(
                                                    pos,
                                                    mupdfDocument,
                                                    aPage
                                                )
                                            }
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

    @Composable
    private fun LoadingView(
        text: String = "Loading"
    ) {
        Column(
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text,
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(12.dp)
            )
            /*CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(20.dp)
            )*/
            Spacer(modifier = Modifier.height(20.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .height(8.dp)
                    .align(alignment = Alignment.CenterHorizontally)
            )
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

    private fun ocr(pos: Int, mupdfDocument: MupdfDocument, aPage: APage) {
        AppExecutors.instance.networkIO().execute {
            val decodeParam = ImageWorker.DecodeParam(
                aPage.toString(),
                true,
                0,
                aPage,
                mupdfDocument.document,
            )
            val bitmap = ImageLoader.decodeFromPDF(
                decodeParam.key,
                decodeParam.pageNum,
                decodeParam.zoom,
                decodeParam.screenWidth
            )
                PdfImageDecoder.decode(decodeParam)
            //val file = FileUtils.getDiskCacheDir(App.instance, pos.toString())
            //BitmapUtils.saveBitmapToFile(bitmap, file)
            AppExecutors.instance.mainThread()
                .execute { startOcrActivity(this, bitmap, null, pos) }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorHelper?.onPause()
    }

    override fun onResume() {
        super.onResume()
        sensorHelper?.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        LiveEventBus
            .get(Event.ACTION_STOPPED)
            .post(path)
        pdfViewModel.destroy()
        BitmapCache.getInstance().clear()
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