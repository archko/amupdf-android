package cn.archko.pdf.activities

import android.annotation.TargetApi
import android.app.ProgressDialog
import android.content.Intent
import android.database.Cursor
import android.graphics.PointF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.TextUtils
import android.util.SparseArray
import android.view.GestureDetector
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import cn.archko.pdf.R
import cn.archko.pdf.common.BitmapCache
import cn.archko.pdf.common.Event
import cn.archko.pdf.common.Graph
import cn.archko.pdf.common.IntentFile
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.PDFBookmarkManager
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.common.SensorHelper
import cn.archko.pdf.entity.APage
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.utils.Utils
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author: archko 2016/5/9 :12:43
 */
abstract class MuPDFRecyclerViewActivity : AnalysticActivity() {

    protected val OUTLINE_REQUEST = 0
    protected var mPath: String? = null

    protected lateinit var progressDialog: ProgressDialog

    protected var gestureDetector: GestureDetector? = null
    protected var pageNumberToast: Toast? = null

    protected var pdfBookmarkManager: PDFBookmarkManager? = null
    protected var sensorHelper: SensorHelper? = null
    protected var mMupdfDocument: MupdfDocument? = null
    protected var mPageSizes = SparseArray<APage>()

    protected var mReflow = false
    protected var mCrop: Boolean = true
    protected var isDocLoaded: Boolean = false

    protected var mDocumentView: FrameLayout? = null
    protected var viewController: AViewController? = null
    protected val optionRepository = PdfOptionRepository(Graph.dataStore)

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        progressDialog = ProgressDialog(this)

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

        loadDoc()
    }

    open fun loadBookmark() {
        pdfBookmarkManager = PDFBookmarkManager()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                mCrop = optionRepository.pdfOptionFlow.first().autocrop

                var autoCrop = 0
                if (!mCrop) {
                    autoCrop = 1
                }
                pdfBookmarkManager!!.setStartBookmark(mPath, autoCrop)
                val bookmark = pdfBookmarkManager?.bookmarkToRestore
                bookmark?.let {
                    mCrop = it.autoCrop == 0
                    mReflow = it.reflow == 1
                }
            }
        }
    }

    open fun doLoadDoc() {
        try {
            progressDialog.setMessage("Loading menu")

            isDocLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        } finally {
            progressDialog.dismiss()
        }
    }

    private fun parseIntent() {
        if (TextUtils.isEmpty(mPath)) {
            val intent = intent

            if (Intent.ACTION_VIEW == intent.action) {
                var path: String? = IntentFile.getPath(this, intent.data)
                if (path == null && intent.data != null) {
                    path = intent.data.toString()
                }
                mPath = path
            } else {
                if (!TextUtils.isEmpty(getIntent().getStringExtra("path"))) {
                    mPath = getIntent().getStringExtra("path")
                }
            }
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
            .get(Event.ACTION_STOPPED)
            .post(mPath)
        mMupdfDocument?.destroy()
        progressDialog.dismiss()
        BitmapCache.getInstance().clear()
    }

    open fun initView() {
        window.requestFeature(Window.FEATURE_NO_TITLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lp: WindowManager.LayoutParams = window.getAttributes()
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_IMMERSIVE
        }
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        setContentView(R.layout.reader)
    }

    open fun getDocumentView(): View? {
        return null
    }

    open fun onSingleTap() {
        if (!isDocLoaded) {
            return
        }
        val pos = getCurrentPos()
        val pageText = (pos + 1).toString() + "/" + mMupdfDocument!!.countPages()
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
                val data = optionRepository.pdfOptionFlow.first()
                keepOn = data.keepOn
                fullscreen = data.fullscreen
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

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE
        }
    }

    override fun onPause() {
        super.onPause()

        sensorHelper?.onPause()
    }

    open fun getCurrentPos(): Int {
        return viewController?.getCurrentPos()!!
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

    open fun loadDoc() {
        progressDialog.setMessage(mPath)
        progressDialog.show()
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    var start = SystemClock.uptimeMillis()
                    mMupdfDocument = MupdfDocument(this@MuPDFRecyclerViewActivity)
                    mMupdfDocument?.newDocument(mPath, getPassword())
                    var res = true
                    mMupdfDocument?.let {
                        if (it.document.needsPassword()) {
                            res = it.document.authenticatePassword(getPassword())
                        }
                    }

                    val cp = mMupdfDocument!!.countPages()
                    Logcat.d(TAG, "open:" + (SystemClock.uptimeMillis() - start) + " cp:" + cp)

                    //val loc = mDocument!!.layout(mLayoutW, mLayoutH, mLayoutEM)

                    preparePageSize(cp)
                    Logcat.d(TAG, "open:end." + mPageSizes.size())
                    val mill = SystemClock.uptimeMillis() - start
                    if (mill < 500L) {
                        delay(500L - mill)
                    }
                    return@withContext true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return@withContext false
            }
            if (result) {
                doLoadDoc()
            } else {
                finish()
            }
        }
    }

    open fun getPageSize(pageNum: Int): APage? {
        val p = mMupdfDocument?.loadPage(pageNum) ?: return null

        //Logcat.d(TAG, "open:getPageSize.$pageNum page:$p")
        val b = p.bounds
        val w = b.x1 - b.x0
        val h = b.y1 - b.y0
        val pointf = PointF(w, h)
        p.destroy()
        return APage(pageNum, pointf, 1.0f/*zoomModel!!.zoom*/, 0)
    }

    open fun preparePageSize(cp: Int) {
        for (i in 0 until cp) {
            val pointF = getPageSize(i)
            mPageSizes.put(i, pointF)
        }
    }
}
