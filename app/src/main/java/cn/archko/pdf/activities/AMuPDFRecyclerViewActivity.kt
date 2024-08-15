package cn.archko.pdf.activities

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.text.TextUtils
import android.util.Log
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import cn.archko.pdf.R
import cn.archko.pdf.common.OutlineHelper
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.core.cache.BitmapCache
import cn.archko.pdf.core.common.Event
import cn.archko.pdf.core.common.GlobalEvent
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.common.SensorHelper
import cn.archko.pdf.core.common.StatusBarHelper
import cn.archko.pdf.core.entity.Bookmark
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.fragments.OutlineFragment
import cn.archko.pdf.fragments.SleepTimerDialog
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.tts.TTSActivity
import cn.archko.pdf.tts.TTSEngine
import cn.archko.pdf.tts.TTSEngine.ProgressListener
import cn.archko.pdf.viewmodel.PDFViewModel
import cn.archko.pdf.widgets.PageControls
import com.baidu.ai.edge.ui.activity.OcrActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.chungha.flowbus.busEvent

/**
 * @author: archko 2019/8/25 :12:43
 */
class AMuPDFRecyclerViewActivity : AnalysticActivity(), OutlineListener {
    private var mPath: String? = null

    private var controllerListener: ControllerListener? = null

    private var sensorHelper: SensorHelper? = null

    private var isDocLoaded: Boolean = false

    private var documentLayout: FrameLayout? = null
    private var viewController: AViewController? = null
    private val pdfViewModel: PDFViewModel = PDFViewModel()

    private var pageControls: PageControls? = null
    private var outlineHelper: OutlineHelper? = null
    private var outlineFragment: OutlineFragment? = null
    private lateinit var mReflowLayout: RelativeLayout
    private lateinit var mContentView: View
    private lateinit var ttsLayout: View
    private lateinit var ttsPlay: View
    private lateinit var ttsClose: View
    private lateinit var ttsSleep: View
    private val viewControllerCache: SparseArray<AViewController> = SparseArray<AViewController>()
    private var viewMode: ViewMode = ViewMode.CROP
    private var ttsMode = false
    private val handler = Handler(Looper.getMainLooper())

    /**
     * 用AMupdf打开,传入强制切边参数,如果是-1,是没有设置,如果设置1表示强制切边,如果是0不切边,让切边按钮失效
     */
    private var forceCropParam = -1

    public override fun onCreate(savedInstanceState: Bundle?) {
        isLive = true
        super.onCreate(savedInstanceState)

        if (null != savedInstanceState) {
            mPath = savedInstanceState.getString("path", null)
        }

        parseIntent()

        Logcat.d("path:" + mPath!!)

        if (TextUtils.isEmpty(mPath)) {
            Toast.makeText(
                this@AMuPDFRecyclerViewActivity,
                "error file path:$mPath",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        sensorHelper = SensorHelper(this)

        loadBookmark()
        initView()

        initTouchParams()

        initViewController()

        viewController?.init()
    }

    private fun parseIntent() {
        if (null == intent) {
            finish()
            return
        }
        if (TextUtils.isEmpty(mPath)) {
            mPath = intent.getStringExtra("path")
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
    }

    private fun loadBookmark() {
        lifecycleScope.launch {
            mPath!!.run { pdfViewModel.loadBookProgressByPath(this) }
        }
    }

    private fun initView() {
        StatusBarHelper.hideSystemUI(this)
        StatusBarHelper.setStatusBarImmerse(window)
        setFullScreen()

        setContentView(R.layout.reader)

        forceCropParam = intent.getIntExtra("forceCropParam", -1)

        mContentView = findViewById(R.id.content)
        ttsLayout = findViewById(R.id.tts_layout)
        ttsPlay = findViewById(R.id.ttsPlay)
        ttsClose = findViewById(R.id.ttsClose)
        ttsSleep = findViewById(R.id.ttsSleep)
        mReflowLayout = findViewById(R.id.reflow_layout)

        pageControls = createControls()
        pageControls?.apply {
            updateTitle(mPath)
            showReflow(pdfViewModel.checkReflow())
            orientation = pdfViewModel.bookProgress?.scrollOrientation ?: 1
            if (IntentFile.isDjvu(mPath!!)) {
                reflowButton.visibility = View.GONE
                ttsButton.visibility = View.GONE
            }
        }

        documentLayout = findViewById(R.id.document_layout)
    }

    private fun setFullScreen() {
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }

    private fun initTouchParams() {
        val view = documentLayout!!
        var margin = view.height
        margin = if (margin <= 0) {
            ViewConfiguration.get(this).scaledTouchSlop * 2
        } else {
            (margin * 0.03).toInt()
        }
        controllerListener = object : ControllerListener {
            override fun onSingleTapConfirmed(ev: MotionEvent?, currentPage: Int) {
                if (!isDocLoaded) {
                    return
                }
                if (viewMode == ViewMode.REFLOW) {
                    var showFlag = false
                    if (pageControls?.visibility() == View.VISIBLE) {
                        pageControls?.hide()
                        showFlag = true
                    }
                    if (mReflowLayout.visibility == View.VISIBLE) {
                        mReflowLayout.visibility = View.GONE
                        showFlag = true
                    }
                    if (!showFlag) {
                        pageControls?.show()
                        viewController?.getCurrentPos()
                            ?.let { pageControls?.updatePageProgress(it) }
                        mReflowLayout.visibility = View.VISIBLE
                    }
                } else {
                    if (mReflowLayout.visibility == View.VISIBLE) {
                        mReflowLayout.visibility = View.GONE
                    }
                    if (pageControls?.visibility() == View.VISIBLE) {
                        pageControls?.hide()
                        viewController?.getCurrentPos()
                            ?.let { pageControls?.updatePageProgress(it) }
                        return
                    } else {
                        if (viewController?.onSingleTap(ev, margin) != true) {
                            pageControls?.show()
                        }
                    }
                }
            }

            override fun onDoubleTap(ev: MotionEvent?, currentPage: Int) {
                if (!isDocLoaded) {
                    return
                }
                if (pageControls?.visibility() == View.VISIBLE) {
                    pageControls?.hide()
                }
                if (mReflowLayout.visibility == View.VISIBLE) {
                    mReflowLayout.visibility = View.GONE
                }
            }

            override fun doLoadedDoc(count: Int, pos: Int) {
                this@AMuPDFRecyclerViewActivity.doLoadDoc(count, pos)
            }
        }
    }

    fun getDocumentView(): View? {
        return viewController?.getDocumentView()!!
    }

    private fun addDocumentView() {
        documentLayout?.removeAllViews()
        val lap: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        documentLayout?.addView(viewController?.getDocumentView(), lap)
    }

    private fun initViewController(): Boolean {
        val oldMode = viewMode
        if (pdfViewModel.checkReflow()) {
            viewMode = ViewMode.REFLOW
        } else if (pdfViewModel.checkCrop()) {
            viewMode = ViewMode.CROP
        } else {
            viewMode = ViewMode.NORMAL
        }

        if (viewMode != ViewMode.REFLOW) {
            if (forceCropParam > -1) {
                pdfViewModel.storeCrop(forceCropParam == 1)
            }
        }

        viewController?.onDestroy()

        val aViewController = ViewControllerFactory.getOrCreateViewController(
            viewControllerCache,
            lifecycleScope,
            viewMode,
            this@AMuPDFRecyclerViewActivity,
            mReflowLayout, pdfViewModel, mPath!!,
            pageControls!!,
            controllerListener
        )
        viewController = aViewController
        Logcat.d("initViewController:$viewMode, forceCropParam: $forceCropParam, controller:$viewController")

        addDocumentView()

        cropModeSet(pdfViewModel.checkCrop())
        //setCropButton(pdfViewModel.checkCrop())

        return true
    }

    //===========================================

    private fun ocr() {
        if (pdfViewModel.checkReflow()) {
            Toast.makeText(
                this@AMuPDFRecyclerViewActivity,
                "已经是文本",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val pos = viewController?.getCurrentPos()
        if (pos != null) {
            startOcrActivity(
                this@AMuPDFRecyclerViewActivity,
                viewController?.getCurrentBitmap(),
                null,
                pos
            )
        }
    }

    private fun applyViewMode(pos: Int) {
        if (!initViewController()) {
            return
        }

        pdfViewModel.setCurrentPage(pos)
        viewController?.init()
    }

    private fun doLoadDoc(count: Int, pos: Int) {
        try {
            pageControls?.apply {
                update(count, pos)
            }

            outlineHelper = pdfViewModel.outlineHelper
            setupOutline(pos)

            isDocLoaded = true

            val sp = getSharedPreferences(PREF_READER, Context.MODE_PRIVATE)
            val isFirst = sp.getBoolean(PREF_READER_KEY_FIRST, true)
            if (isFirst) {
                showOutline()

                sp.edit()
                    .putBoolean(PREF_READER_KEY_FIRST, false)
                    .apply()
            }

            //checkout bookmark
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        } finally {
        }
    }

    private fun setupOutline(currentPos: Int?) {
        if (null == outlineFragment && null != outlineHelper) {
            outlineFragment = OutlineFragment()
            val bundle = Bundle()
            if (outlineHelper!!.hasOutline()) {
                outlineFragment!!.outlineItems = outlineHelper!!.getOutlineItems()
                bundle.putSerializable("out", outlineHelper?.getOutlineItems())
            }
            bundle.putSerializable("POSITION", currentPos)
            outlineFragment?.arguments = bundle
        }
    }

    private fun deleteBookmark(bookmark: Bookmark) {
        /*lifecycleScope.launch {
            pdfViewModel.deleteBookmark(bookmark).collectLatest {
                mMenuHelper?.updateBookmark(getCurrentPos(), it)
                viewController?.notifyDataSetChanged()
            }
        }*/
    }

    private fun addBookmark(page: Int) {
        lifecycleScope.launch {
            val currentPos = getCurrentPos()
            pdfViewModel.addBookmark(currentPos).collectLatest {
                viewController?.notifyDataSetChanged()
            }
        }
    }

    private fun toggleReflow() {
        val reflow = !pdfViewModel.checkReflow()
        if (!reflow) {  //如果原来是文本重排模式,则切换为自动切边或普通模式
            if (pdfViewModel.checkCrop()) {
                viewMode = ViewMode.CROP
            } else {
                viewMode = ViewMode.NORMAL
            }
            if (mReflowLayout.visibility == View.VISIBLE) {
                mReflowLayout.visibility = View.GONE
            }
        } else {
            viewMode = ViewMode.REFLOW
        }
        pdfViewModel.storeReflow(reflow)

        applyViewMode(getCurrentPos())

        setReflowButton(reflow)
    }

    private fun setReflowButton(reflow: Boolean) {
        val crop: Boolean
        if (reflow) {
            crop = false
            pageControls?.reflowButton!!.setColorFilter(Color.argb(0xFF, 172, 114, 37))
        } else {
            pageControls?.reflowButton!!.setColorFilter(Color.argb(0xFF, 255, 255, 255))
            crop = pdfViewModel.checkCrop()
        }

        setCropButton(crop)
        val pos = getCurrentPos()
        if (pos > 0) {
            viewController?.scrollToPosition(pos + 1)
        }
    }

    private fun createControls(): PageControls {
        pageControls = PageControls(
            mContentView,
            object : PageControls.ControlListener {
                override fun toggleReflow() {
                    this@AMuPDFRecyclerViewActivity.toggleReflow()
                }

                override fun toggleCrop() {
                    this@AMuPDFRecyclerViewActivity.toggleCrop()
                }

                override fun showOutline() {
                    this@AMuPDFRecyclerViewActivity.showOutline()
                }

                override fun gotoPage(page: Int) {
                    viewController?.scrollToPosition(page)
                }

                override fun back() {
                    this@AMuPDFRecyclerViewActivity.finish()
                }

                override fun changeOrientation(ori: Int) {
                    changeOri(ori)
                }

                override fun toggleTts() {
                    if (ttsMode) {
                    } else {
                        TTSEngine.get().getTTS { status: Int ->
                            if (status == TextToSpeech.SUCCESS) {
                                ttsLayout.visibility = View.VISIBLE
                                ttsMode = true
                                startTts()
                            } else {
                                Log.e(TTSActivity.TAG, "初始化失败")
                                Toast.makeText(
                                    this@AMuPDFRecyclerViewActivity,
                                    getString(R.string.tts_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                override fun ocr() {
                    this@AMuPDFRecyclerViewActivity.ocr()
                }
            }
        )
        return pageControls!!
    }

    private fun startTts() {
        TTSEngine.get().setSpeakListener(object : ProgressListener {
            override fun onStart(utteranceId: String) {
            }

            override fun onDone(key: String) {
                try {
                    //Logcat.d("onDone:$key")
                    val arr = key.split("-")
                    val page = Utils.parseInt(arr[0])
                    val current = getCurrentPos()
                    if (current != page) {
                        handler.post { onSelectedOutline(page + 1) }
                    }
                } catch (e: Exception) {
                    Logcat.e(e)
                }
            }
        })
        ttsPlay.setOnClickListener {
            if (TTSEngine.get().isSpeaking()) {
                TTSEngine.get().stop()
            } else {
                TTSEngine.get().resume()
            }
        }
        ttsClose.setOnClickListener {
            closeTts()
        }
        ttsSleep.setOnClickListener {
            SleepTimerDialog(object : SleepTimerDialog.TimeListener {
                override fun onTime(minute: Int) {
                    Logcat.d("TTSEngine.get().stop()")
                    handler.postDelayed({
                        closeTts()
                    }, (minute * 60000).toLong())
                }
            }).showDialog(this)
        }
        lifecycleScope.launch {
            pdfViewModel.decodePageForTts(getCurrentPos())
        }
    }

    private fun closeTts() {
        ttsLayout.visibility = View.GONE
        ttsMode = false
        TTSEngine.get().shutdown()
    }

    private fun changeOri(ori: Int) {
        pdfViewModel.bookProgress?.scrollOrientation = ori
        viewController?.setOrientation(ori)
    }

    private fun showOutline() {
        outlineHelper?.let {
            if (it.hasOutline()) {
                outlineFragment?.updateSelection(getCurrentPos())
                outlineFragment?.showDialog(this)
            } else {
                Toast.makeText(this, "no outline", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showBookmark() {
        outlineHelper?.let {
            //val frameLayout = pageControls?.layoutOutline

            /*val bookmarks = arrayListOf<Bookmark>()
            var element = Bookmark()
            element.page = 3
            bookmarks.add(element)
            element = Bookmark()
            element.page = 30
            bookmarks.add(element)*/
            /*if (frameLayout?.visibility == View.GONE) {
                frameLayout.visibility = View.VISIBLE
                mMenuHelper?.showBookmark(getCurrentPos(), pdfViewModel.bookmarks)
            } else {
                if (mMenuHelper!!.isOutline()) {
                    mMenuHelper?.showBookmark(getCurrentPos(), pdfViewModel.bookmarks)
                } else {
                    frameLayout?.visibility = View.GONE
                }
            }*/
        }
    }

    private fun toggleCrop() {
        var prefCrop = pdfViewModel.checkCrop();
        val flag = cropModeSet(!prefCrop)
        if (flag) {
            BitmapCache.getInstance().clear()
            viewController?.notifyDataSetChanged()
            prefCrop = !prefCrop
            if (prefCrop) {
                viewMode = ViewMode.CROP
            } else {
                viewMode = ViewMode.NORMAL
            }
            pdfViewModel.storeCrop(prefCrop)
            applyViewMode(getCurrentPos())
        }
    }

    private fun getCurrentPos(): Int {
        if (null == viewController) {
            return 0
        }
        return viewController!!.getCurrentPos()
    }

    private fun cropModeSet(crop: Boolean): Boolean {
        if (pdfViewModel.checkReflow()) {
            Toast.makeText(
                this,
                getString(R.string.in_reflow_mode),
                Toast.LENGTH_SHORT
            ).show()
            pageControls?.reflowButton!!.setColorFilter(Color.argb(0xFF, 172, 114, 37))
            pageControls?.autoCropButton!!.setImageResource(R.drawable.ic_no_crop)
            return false
        }
        setCropButton(crop)
        return true
    }

    private fun setCropButton(crop: Boolean) {
        if (crop) {
            pageControls?.autoCropButton!!.setImageResource(R.drawable.ic_crop)
        } else {
            pageControls?.autoCropButton!!.setImageResource(R.drawable.ic_no_crop)

        }
    }

    override fun onSelectedOutline(index: Int) {
        viewController?.onSelectedOutline(index)
        updateProgress(index - RESULT_FIRST_USER)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        viewController?.onConfigurationChanged(newConfig)
    }

    private fun updateProgress(index: Int) {
        if (isDocLoaded && pageControls?.visibility() == View.VISIBLE) {
            pageControls?.updatePageProgress(index)
        }
    }

    //--------------------------------------

    override fun onDestroy() {
        super.onDestroy()
        isDocLoaded = false
        busEvent(GlobalEvent(Event.ACTION_STOPPED, mPath))
        pdfViewModel.destroy()
        BitmapCache.getInstance().clear()

        viewController?.onDestroy()

        TTSEngine.get().shutdown()
    }

    override fun onResume() {
        super.onResume()
        sensorHelper?.onResume()
        viewController?.onResume()
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

    override fun onPause() {
        super.onPause()
        sensorHelper?.onPause()
        viewController?.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("path", mPath)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mPath = savedInstanceState.getString("path", null)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            StatusBarHelper.hideSystemUI(this)
            StatusBarHelper.setStatusBarImmerse(window)
        }
    }

    //===========================================

    companion object {

        private const val TAG = "AMuPDFRecyclerViewActivity"
        const val PREF_READER = "pref_reader_amupdf"
        const val PREF_READER_KEY_FIRST = "pref_reader_key_first"

        fun startOcrActivity(context: Context, bitmap: Bitmap?, path: String?, pos: Int) {
            OcrActivity.start(context, bitmap, path, pos.toString())
        }
    }

    //===========================================

    internal object ViewControllerFactory {
        fun getOrCreateViewController(
            viewControllerCache: SparseArray<AViewController>,
            scope: CoroutineScope,
            viewMode: ViewMode,
            context: FragmentActivity,
            controllerLayout: RelativeLayout,
            pdfViewModel: PDFViewModel,
            path: String,
            pageSeekBarControls: PageControls,
            controllerListener: ControllerListener?,
        ): AViewController {
            //val aViewController = viewControllerCache.get(viewMode.ordinal)
            //if (null != aViewController) {
            //    return aViewController
            //}
            return createViewController(
                scope,
                viewMode,
                context,
                controllerLayout,
                pdfViewModel,
                path,
                pageSeekBarControls,
                controllerListener,
            )
        }

        fun createViewController(
            scope: CoroutineScope,
            viewMode: ViewMode,
            context: FragmentActivity,
            controllerLayout: RelativeLayout,
            pdfViewModel: PDFViewModel,
            path: String,
            pageSeekBarControls: PageControls,
            controllerListener: ControllerListener?,
        ): AViewController {
            if (viewMode == ViewMode.CROP) {
                return ANormalViewController(
                    context,
                    scope,
                    controllerLayout,
                    pdfViewModel,
                    path,
                    pageSeekBarControls,
                    controllerListener,
                )
            } else if (viewMode == ViewMode.REFLOW) {
                return AReflowViewController(
                    context,
                    scope,
                    controllerLayout,
                    pdfViewModel,
                    path,
                    pageSeekBarControls,
                    controllerListener,
                )
            } else {
                return ANormalViewController(
                    context,
                    scope,
                    controllerLayout,
                    pdfViewModel,
                    path,
                    pageSeekBarControls,
                    controllerListener,
                )
            }
        }
    }

    enum class ViewMode {
        NORMAL, CROP, REFLOW
    }
}
