package cn.archko.pdf.activities

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.text.TextUtils
import android.view.GestureDetector
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import cn.archko.pdf.R
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.core.cache.BitmapCache
import cn.archko.pdf.core.common.Event
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.common.SensorHelper
import cn.archko.pdf.core.common.StatusBarHelper
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.entity.State
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.viewmodel.PDFViewModel
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author: archko 2016/5/9 :12:43
 */
abstract class MuPDFRecyclerViewActivity : AnalysticActivity() {

    protected val OUTLINE_REQUEST = 0
    protected var mPath: String? = null

    //protected lateinit var progressDialog: ProgressDialog

    protected var gestureDetector: GestureDetector? = null
    protected var pageNumberToast: Toast? = null

    protected var sensorHelper: SensorHelper? = null
    protected var mPageSizes = mutableListOf<APage>()

    protected var mReflow = false
    protected var mCrop: Boolean = true
    protected var isDocLoaded: Boolean = false

    protected var mDocumentView: FrameLayout? = null
    protected var viewController: AViewController? = null
    protected val pdfViewModel: PDFViewModel = PDFViewModel()

    public override fun onCreate(savedInstanceState: Bundle?) {
        isLive = true
        super.onCreate(savedInstanceState)

        //progressDialog = ProgressDialog(this)

        if (null != savedInstanceState) {
            mPath = savedInstanceState.getString("path", null)
        }

        parseIntent()

        Logcat.d("path:" + mPath!!)

        if (TextUtils.isEmpty(mPath)) {
            Toast.makeText(
                this@MuPDFRecyclerViewActivity,
                "error file path:$mPath",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        sensorHelper = SensorHelper(this)

        loadBookmark()
        initView()

        loadDoc(null)
    }

    open fun loadBookmark() {
        lifecycleScope.launch {
            mCrop = PdfOptionRepository.getAutocrop()
            mPath?.run {
                val bookProgress = pdfViewModel.loadBookProgressByPath(this)
                bookProgress?.let {
                    mCrop = it.autoCrop == 0
                    mReflow = it.reflow == 1
                }
            }
        }
    }

    open fun doLoadDoc() {
        //try {
        //progressDialog.setMessage("Loading menu")

        isDocLoaded = true
        //} catch (e: Exception) {
        //    e.printStackTrace()
        //    finish()
        //} finally {
        //    //progressDialog.dismiss()
        //}
    }

    private fun parseIntent() {
        if (null == intent) {
            finish()
            return
        }
        if (TextUtils.isEmpty(mPath)) {
            mPath = intent.getStringExtra("path")
            //pos = getIntent().getIntExtra("pos", 0)
            if (Intent.ACTION_VIEW == intent.action) {
                if (TextUtils.isEmpty(mPath)) {
                    val uri = getIntent().data
                    val path = IntentFile.getPath(this, uri)
                    mPath = path
                }
            } else {
                mPath = intent.getStringExtra("path")
            }
        }
        if (TextUtils.isEmpty(mPath)) {
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("path", mPath)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mPath = savedInstanceState.getString("path", null)
    }

    override fun onDestroy() {
        super.onDestroy()
        isDocLoaded = false
        LiveEventBus
            .get<String>(Event.ACTION_STOPPED)
            .post(mPath)
        pdfViewModel.destroy()
        //progressDialog.dismiss()
        BitmapCache.getInstance().clear()
    }

    open fun initView() {
        StatusBarHelper.hideSystemUI(this)
        StatusBarHelper.setStatusBarImmerse(window)
        setFullScreen()

        setContentView(R.layout.reader)
    }

    private fun setFullScreen() {
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }

    open fun getDocumentView(): View? {
        return null
    }

    open fun onSingleTap(): Boolean {
        return false
    }

    fun showPageToast() {
        val pos = getCurrentPos()
        val pageText = (pos + 1).toString() + "/" + pdfViewModel.countPages()
        if (pageNumberToast != null) {
            pageNumberToast!!.setText(pageText)
        } else {
            pageNumberToast =
                Toast.makeText(this@MuPDFRecyclerViewActivity, pageText, Toast.LENGTH_SHORT)
        }
        pageNumberToast!!.setGravity(Gravity.BOTTOM or Gravity.START, Utils.dipToPixel(15f), 0)
        pageNumberToast!!.show()
    }

    open fun onDoubleTap() {
        if (!isDocLoaded) {
            return
        }
    }

    open fun updateProgress(index: Int) {
    }
    //--------------------------------------

    override fun onResume() {
        super.onResume()

        sensorHelper?.onResume()

        lifecycleScope.launch {
            var keepOn = false
            var fullscreen = true
            withContext(Dispatchers.IO) {
                keepOn = PdfOptionRepository.getKeepOn()
                fullscreen = PdfOptionRepository.getFullscreen()
            }

            if (keepOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            if (fullscreen) {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        }
        Logcat.d("onResume ")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            StatusBarHelper.hideSystemUI(this)
            StatusBarHelper.setStatusBarImmerse(window)
        }
    }

    override fun onPause() {
        super.onPause()

        sensorHelper?.onPause()
    }

    open fun getCurrentPos(): Int {
        if (null == viewController) {
            return 0
        }
        return viewController!!.getCurrentPos()
    }

    open fun getPassword(): String? {
        return null
    }

    //===========================================

    companion object {

        const val TAG = "MuPDFRecyclerViewActivity"
        const val TYPE_TITLE = 0
        const val TYPE_PROGRESS = 1
        const val TYPE_ZOOM = 2
        const val TYPE_CLOSE = 3
        const val TYPE_FONT = 4
        const val TYPE_SETTINGS = 5
    }

    open fun loadDoc(password: String?) {
        //progressDialog.setMessage(mPath)
        //progressDialog.show()
        lifecycleScope.launch {
            val start = SystemClock.uptimeMillis()
            pdfViewModel.loadPdfDoc(this@MuPDFRecyclerViewActivity, mPath!!, password)
            pdfViewModel.pageFlow
                .collectLatest {
                    if (it.state == State.PASS) {
                        showPasswordDialog()
                        return@collectLatest
                    }
                    val cp = pdfViewModel.countPages()
                    if (cp > 0) {
                        Logcat.d(TAG, "open:" + (SystemClock.uptimeMillis() - start) + " cp:" + cp)

                        postLoadDoc(cp)
                    } else {
                        finish()
                    }
                }
        }
    }

    open fun postLoadDoc(cp: Int) {
        preparePageSize(cp)
        Logcat.d(TAG, "open:end." + mPageSizes.size)
        //val mill = SystemClock.uptimeMillis() - start
        //if (mill < 500L) {
        //    delay(500L - mill)
        //}

        doLoadDoc()
    }

    abstract fun showPasswordDialog()

    open fun getPageSize(pageNum: Int): APage? {
        val p = pdfViewModel.loadPage(pageNum) ?: return null

        //Logcat.d(TAG, "open:getPageSize.$pageNum page:$p")
        val b = p.bounds
        val w = b.x1 - b.x0
        val h = b.y1 - b.y0
        p.destroy()
        return APage(pageNum, w, h, 1.0f/*zoomModel!!.zoom*/)
    }

    open fun preparePageSize(cp: Int) {
        for (i in 0 until cp) {
            val pointF = getPageSize(i)
            if (pointF != null) {
                mPageSizes.add(pointF)
            }
        }
    }
}
