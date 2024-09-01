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
import androidx.core.util.forEach
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import cn.archko.pdf.R
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.core.cache.BitmapCache
import cn.archko.pdf.core.cache.BitmapPool
import cn.archko.pdf.core.common.Event
import cn.archko.pdf.core.common.GlobalEvent
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.common.SensorHelper
import cn.archko.pdf.core.common.StatusBarHelper
import cn.archko.pdf.core.entity.BookProgress
import cn.archko.pdf.core.entity.Bookmark
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.fragments.OutlineFragment
import cn.archko.pdf.fragments.SleepTimerDialog
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.tts.TTSActivity
import cn.archko.pdf.tts.TTSEngine
import cn.archko.pdf.viewmodel.DocViewModel
import cn.archko.pdf.widgets.PageControls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.vudroid.core.codec.OutlineLink
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
    private val pdfViewModel: DocViewModel = DocViewModel()

    private var pageControls: PageControls? = null
    private var outlineLinks = mutableListOf<OutlineLink>()
    private var outlineFragment: OutlineFragment? = null
    private lateinit var mReflowLayout: RelativeLayout
    private lateinit var mContentView: View
    private lateinit var ttsLayout: View
    private lateinit var ttsPlay: View
    private lateinit var ttsClose: View
    private lateinit var ttsSleep: View
    private lateinit var ttsText: View
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
        updateControls()
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
        ttsText = findViewById(R.id.ttsText)
        mReflowLayout = findViewById(R.id.reflow_layout)

        pageControls = createControls()

        documentLayout = findViewById(R.id.document_layout)
    }

    private fun updateControls() {
        pageControls?.apply {
            updateTitle(mPath)
            showReflow(pdfViewModel.getReflow())
            showReflowImage(pdfViewModel.getReflow())
            orientation = pdfViewModel.bookProgress?.scrollOrientation ?: 1
            if (IntentFile.isText(mPath!!)) {
                reflowButton.visibility = View.GONE
                autoCropButton.visibility = View.GONE
                outlineButton.visibility = View.GONE
                oriButton.visibility = View.GONE
                imageButton.visibility = View.GONE
            } else if (IntentFile.isMuPdf(mPath!!)) {
                reflowButton.visibility = View.VISIBLE
                imageButton.visibility = View.VISIBLE
                autoCropButton.visibility = View.VISIBLE
                outlineButton.visibility = View.VISIBLE
            } else {
                reflowButton.visibility = View.VISIBLE
                imageButton.visibility = View.GONE
                autoCropButton.visibility = View.VISIBLE
                outlineButton.visibility = View.VISIBLE
            }
            updatePageProgress(getCurrentPos())
        }
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
                    viewController?.onSingleTap(ev, margin)
                }
            }

            override fun onDoubleTap(ev: MotionEvent?, currentPage: Int): Boolean {
                if (!isDocLoaded) {
                    return false
                }
                if (viewMode == ViewMode.REFLOW || viewMode == ViewMode.REFLOW_SCAN) {
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
                        pageControls?.apply {
                            updateControls()
                            show()
                        }
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
                    } else {
                        if (viewController?.onSingleTap(ev, margin) != true) {
                            pageControls?.apply {
                                updateControls()
                                show()
                            }
                        }
                    }
                }
                return true
            }

            override fun doLoadedDoc(count: Int, pos: Int, outlineLinks: List<OutlineLink>?) {
                this@AMuPDFRecyclerViewActivity.doLoadDoc(count, pos, outlineLinks)
            }
        }
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
        if (IntentFile.isText(mPath!!)) {
            viewMode = ViewMode.TEXT
        } else {
            val reflow = pdfViewModel.getReflow()
            viewMode = if (reflow == BookProgress.REFLOW_TXT) {
                ViewMode.REFLOW
            } else if (reflow == BookProgress.REFLOW_SCAN) {
                ViewMode.REFLOW_SCAN
            } else if (pdfViewModel.checkCrop()) {
                ViewMode.CROP
            } else {
                ViewMode.NORMAL
            }
        }

        if (viewMode != ViewMode.REFLOW && viewMode != ViewMode.REFLOW_SCAN) {
            if (forceCropParam > -1) {
                pdfViewModel.storeCrop(forceCropParam == 1)
            }
        }

        closeTts()
        viewController?.onDestroy()
        val old = viewController

        val aViewController = ViewControllerFactory.getOrCreateViewController(
            viewControllerCache,
            lifecycleScope,
            viewMode,
            this@AMuPDFRecyclerViewActivity,
            mReflowLayout,
            pdfViewModel,
            mPath!!,
            pageControls!!,
            controllerListener
        )
        viewController = aViewController
        Logcat.d("initViewController:$old, $viewController, forceCropParam: $forceCropParam, controller:$viewController")

        addDocumentView()

        setReflowButton(pdfViewModel.getReflow())

        return true
    }

    //===========================================

    private fun ocr() {
        if (pdfViewModel.getReflow() != BookProgress.REFLOW_NO) {//TODO
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
        BitmapPool.getInstance().clear()
        BitmapCache.getInstance().clear()

        pdfViewModel.setCurrentPage(pos)
        updateControls()

        if (!initViewController()) {
            viewController?.notifyDataSetChanged()
            return
        }

        viewController?.init()
    }

    private fun doLoadDoc(count: Int, pos: Int, outlineLinks: List<OutlineLink>?) {
        try {
            pageControls?.apply {
                update(count, pos)
            }

            if (outlineLinks != null) {
                this.outlineLinks.addAll(outlineLinks)
            }
            setupOutline(pos)

            isDocLoaded = true

            val sp = getSharedPreferences(PREF_READER, MODE_PRIVATE)
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
        if (null == outlineFragment) {
            outlineFragment = OutlineFragment()
            val bundle = Bundle()
            if (outlineLinks.size > 0) {
                outlineFragment!!.outlineItems = ArrayList(outlineLinks)
                bundle.putSerializable("out", outlineFragment!!.outlineItems)
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
        /*lifecycleScope.launch {
            val currentPos = getCurrentPos()
            pdfViewModel.addBookmark(currentPos).collectLatest {
                viewController?.notifyDataSetChanged()
            }
        }*/
    }

    /**
     * 原来不是文本重排模式,可能是没有初始化过,不确定,切换为重排模式时需要先确定文档是否支持
     * 原来是重排模式,直接切换回来即可,切边或不切边
     */
    private fun toggleReflow() {
        val reflow = pdfViewModel.getReflow()
        if (reflow == BookProgress.REFLOW_TXT) {
            pdfViewModel.storeReflow(BookProgress.REFLOW_NO)
            viewMode = if (pdfViewModel.checkCrop()) {
                ViewMode.CROP
            } else {
                ViewMode.NORMAL
            }
            if (mReflowLayout.visibility == View.VISIBLE) {
                mReflowLayout.visibility = View.GONE
            }
        } else {
            /*val reflowMode = viewController?.reflow()
            viewMode = if (reflowMode == BookProgress.REFLOW_TXT) {
                ViewMode.REFLOW
            } else {
                ViewMode.REFLOW_SCAN
            }
            if (reflowMode != null) {
                pdfViewModel.storeReflow(reflowMode)
            }*/
            viewMode = ViewMode.REFLOW
            pdfViewModel.storeReflow(BookProgress.REFLOW_TXT)
        }

        applyViewMode(getCurrentPos())
    }

    private fun toggleReflowImage() {
        val reflow = pdfViewModel.getReflow()
        if (reflow == BookProgress.REFLOW_SCAN) {
            pdfViewModel.storeReflow(BookProgress.REFLOW_NO)
            viewMode = if (pdfViewModel.checkCrop()) {
                ViewMode.CROP
            } else {
                ViewMode.NORMAL
            }
        } else {
            viewMode = ViewMode.REFLOW_SCAN
            pdfViewModel.storeReflow(BookProgress.REFLOW_SCAN)
        }
        if (mReflowLayout.visibility == View.VISIBLE) {
            mReflowLayout.visibility = View.GONE
        }

        applyViewMode(getCurrentPos())
    }

    private fun setReflowButton(reflow: Int) {
        val crop: Boolean
        if (reflow == BookProgress.REFLOW_TXT) {
            crop = false
            pageControls?.reflowButton!!.setColorFilter(Color.argb(0xFF, 172, 114, 37))
            pageControls?.imageButton!!.setColorFilter(Color.argb(0xFF, 255, 255, 255))
        } else if (reflow == BookProgress.REFLOW_SCAN) {
            crop = false
            pageControls?.reflowButton!!.setColorFilter(Color.argb(0xFF, 255, 255, 255))
            pageControls?.imageButton!!.setColorFilter(Color.argb(0xFF, 172, 114, 37))
        } else {
            pageControls?.reflowButton!!.setColorFilter(Color.argb(0xFF, 255, 255, 255))
            pageControls?.imageButton!!.setColorFilter(Color.argb(0xFF, 255, 255, 255))
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

                override fun toggleReflowImage() {
                    this@AMuPDFRecyclerViewActivity.toggleReflowImage()
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
        TTSEngine.get().setSpeakListener(object : TTSEngine.ProgressListener {
            override fun onStart(key: String) {
                try {
                    val arr = key.split("-")
                    val page = Utils.parseInt(arr[0])
                    val current = getCurrentPos()
                    //Logcat.d("onStart:$key, current:$current")
                    if (current != page) {
                        onSelectedOutline(page)
                    }
                } catch (e: Exception) {
                    Logcat.e(e)
                }
            }

            override fun onDone(key: String) {
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
        ttsText.setOnClickListener {
            if (TTSEngine.get().isSpeaking()) {
                TTSEngine.get().stop()
            }

            TtsTextFragment.showCreateDialog(
                this,
                object : DataListener {
                    override fun onSuccess(vararg args: Any?) {
                        val key = args[0] as String
                        TTSEngine.get().resumeFromKey(content)
                    }

                    override fun onFailed(vararg args: Any?) {
                    }
                })
        }
        lifecycleScope.launch {
            viewController?.decodePageForTts(getCurrentPos())
        }
    }

    private val closeRunnable = Runnable { closeTts() }

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
        if (outlineLinks.size > 0) {
            outlineFragment?.updateSelection(getCurrentPos())
            outlineFragment?.showDialog(this)
        } else {
            Toast.makeText(this, "no outline", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBookmark() {
    }

    private fun toggleCrop() {
        val prefCrop = !pdfViewModel.checkCrop()
        val flag = viewMode == ViewMode.REFLOW || viewMode == ViewMode.REFLOW_SCAN
        if (!flag) {
            BitmapCache.getInstance().clear()
            viewController?.notifyDataSetChanged()
            if (prefCrop) {
                viewMode = ViewMode.CROP
            } else {
                viewMode = ViewMode.NORMAL
            }
            pdfViewModel.storeCrop(prefCrop)
            applyViewMode(getCurrentPos())
            setReflowButton(pdfViewModel.getReflow())
        }
    }

    private fun getCurrentPos(): Int {
        if (null == viewController) {
            return 0
        }
        return viewController!!.getCurrentPos()
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
        updateProgress(index)
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
        BitmapCache.getInstance().clear()

        viewControllerCache.forEach { key, value -> value.onDestroy() }

        TTSEngine.get().shutdown()
        BitmapPool.getInstance().clear()
        BitmapCache.getInstance().clear()
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
        updateControls()
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
            //OcrActivity.start(context, bitmap, path, pos.toString())
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.setClassName(context, "com.baidu.ai.edge.ui.activity.OcrActivity")
            intent.putExtra("path", path)
            if (null != bitmap) {
                val key = System.currentTimeMillis().toString()
                BitmapCache.getInstance().addBitmap(key, bitmap)
                intent.putExtra("key", key)
            }
            intent.putExtra("name", pos.toString())
            context.startActivity(intent)
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
            pdfViewModel: DocViewModel,
            path: String,
            pageSeekBarControls: PageControls,
            controllerListener: ControllerListener?,
        ): AViewController {
            //var aViewController = viewControllerCache.get(viewMode.ordinal)
            //if (null != aViewController) {
            //    return aViewController
            //}
            val aViewController = createViewController(
                scope,
                viewMode,
                context,
                controllerLayout,
                pdfViewModel,
                path,
                pageSeekBarControls,
                controllerListener,
            )
            //viewControllerCache.put(viewMode.ordinal, aViewController)

            return aViewController
        }

        fun createViewController(
            scope: CoroutineScope,
            viewMode: ViewMode,
            context: FragmentActivity,
            controllerLayout: RelativeLayout,
            pdfViewModel: DocViewModel,
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
                return ATextReflowViewController(
                    context,
                    scope,
                    controllerLayout,
                    pdfViewModel,
                    path,
                    pageSeekBarControls,
                    controllerListener,
                )
            } else if (viewMode == ViewMode.REFLOW_SCAN) {
                return AScanReflowViewController(
                    context,
                    scope,
                    controllerLayout,
                    pdfViewModel,
                    path,
                    pageSeekBarControls,
                    controllerListener,
                )
            } else if (viewMode == ViewMode.TEXT) {
                return ATextViewController(
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
        NORMAL, CROP, REFLOW, REFLOW_SCAN, TEXT
    }
}