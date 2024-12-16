package cn.archko.pdf.activities

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
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
import cn.archko.pdf.controller.AEpubViewController
import cn.archko.pdf.controller.ANormalViewController
import cn.archko.pdf.controller.AScanReflowViewController
import cn.archko.pdf.controller.ATextReflowViewController
import cn.archko.pdf.controller.ATextViewController
import cn.archko.pdf.controller.ControllerListener
import cn.archko.pdf.controller.EpubPageController
import cn.archko.pdf.controller.IPageController
import cn.archko.pdf.controller.PageControllerListener
import cn.archko.pdf.controller.PdfPageController
import cn.archko.pdf.controller.TextPageController
import cn.archko.pdf.controller.ViewMode
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
import cn.archko.pdf.core.entity.ReflowBean
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.fragments.OutlineFragment
import cn.archko.pdf.fragments.SleepTimerDialog
import cn.archko.pdf.fragments.TtsTextFragment
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.tts.TTSActivity
import cn.archko.pdf.tts.TTSEngine
import cn.archko.pdf.viewmodel.DocViewModel
import com.tencent.mmkv.MMKV
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
    private val docViewModel: DocViewModel = DocViewModel()

    private var pageController: IPageController? = null
    private var outlineLinks = mutableListOf<OutlineLink>()
    private var outlineFragment: OutlineFragment? = null
    private lateinit var mReflowLayout: RelativeLayout
    private lateinit var mContentView: View
    private lateinit var ttsLayout: View
    private lateinit var ttsPlay: View
    private lateinit var ttsClose: View
    private lateinit var ttsCircle: View
    private lateinit var ttsSleep: View
    private lateinit var ttsText: View
    private val viewControllerCache: SparseArray<AViewController> = SparseArray<AViewController>()
    private var viewMode: ViewMode = ViewMode.CROP
    private var ttsMode = false
    private val handler = Handler(Looper.getMainLooper())
    private var pendingPos = -1

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

        clean()

        loadBookmark()
        initView()

        createControls()

        initTouchParams()

        initViewController()

        viewController?.init()
        //updateControls()
    }

    private fun clean() {
        val mmkv = MMKV.mmkvWithID("seekArc")
        mmkv.remove("progress")
        BitmapPool.getInstance().clear()
        BitmapCache.getInstance().clear()
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
            val result = withContext(Dispatchers.IO) {
                mPath!!.run { docViewModel.loadBookProgressByPath(this) }
            }
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
        ttsCircle = findViewById(R.id.ttsCircle)
        ttsSleep = findViewById(R.id.ttsSleep)
        ttsText = findViewById(R.id.ttsText)
        mReflowLayout = findViewById(R.id.reflow_layout)

        documentLayout = findViewById(R.id.document_layout)
    }

    private fun updateControls() {
        pageController?.apply {
            updateTitle(mPath)
            update(docViewModel.getPageCount(), getCurrentPos())
            /*showReflow(docViewModel.getReflow())
            showReflowImage(docViewModel.getReflow())
            orientation = docViewModel.bookProgress?.scrollOrientation ?: 1
            if (IntentFile.isText(mPath!!)) {
                reflowButton.visibility = View.GONE
                autoCropButton.visibility = View.GONE
                outlineButton.visibility = View.GONE
                oriButton.visibility = View.VISIBLE
                imageButton.visibility = View.GONE
            } else if (IntentFile.isMuPdf(mPath!!)) {
                reflowButton.visibility = View.VISIBLE
                imageButton.visibility = View.VISIBLE
                autoCropButton.visibility = View.VISIBLE
                outlineButton.visibility = View.VISIBLE
            } else {
                reflowButton.visibility = View.VISIBLE
                imageButton.visibility = View.VISIBLE
                autoCropButton.visibility = View.VISIBLE
                outlineButton.visibility = View.VISIBLE
            }
            if (IntentFile.isReflowable(mPath!!)) {
                imageButton.visibility = View.GONE
            }
            if (viewMode == ViewMode.REFLOW_SCAN) {
                oriButton.visibility = View.GONE
            }
            updatePageProgress(getCurrentPos())*/
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
                if (pageController?.visibility() == View.VISIBLE) {
                    pageController?.hide()
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
                    if (pageController?.visibility() == View.VISIBLE) {
                        pageController?.hide()
                        showFlag = true
                    }
                    if (mReflowLayout.visibility == View.VISIBLE) {
                        mReflowLayout.visibility = View.GONE
                        showFlag = true
                    }
                    if (!showFlag) {
                        pageController?.apply {
                            updateControls()
                            show()
                        }
                        viewController?.getCurrentPos()
                            ?.let { pageController?.updatePageProgress(it) }
                        mReflowLayout.visibility = View.VISIBLE
                    }
                } else {
                    if (mReflowLayout.visibility == View.VISIBLE) {
                        mReflowLayout.visibility = View.GONE
                    }
                    if (pageController?.visibility() == View.VISIBLE) {
                        pageController?.hide()
                        viewController?.getCurrentPos()
                            ?.let { pageController?.updatePageProgress(it) }
                    } else {
                        if (viewController?.onSingleTap(ev, margin) != true) {
                            pageController?.apply {
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

            override fun reloadDoc() {
                applyViewMode(getCurrentPos())
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
            val reflow = docViewModel.getReflow()
            viewMode = if (reflow == BookProgress.REFLOW_TXT) {
                ViewMode.REFLOW
            } else if (reflow == BookProgress.REFLOW_SCAN) {
                ViewMode.REFLOW_SCAN
            } else if (docViewModel.checkCrop()) {
                ViewMode.CROP
            } else {
                ViewMode.NORMAL
            }
        }

        if (viewMode != ViewMode.REFLOW && viewMode != ViewMode.REFLOW_SCAN) {
            if (forceCropParam > -1) {
                docViewModel.storeCrop(forceCropParam == 1)
            }
        }

        val aViewController = ViewControllerFactory.getOrCreateViewController(
            viewControllerCache,
            lifecycleScope,
            viewMode,
            this@AMuPDFRecyclerViewActivity,
            mReflowLayout,
            docViewModel,
            mPath!!,
            pageController!!,
            controllerListener
        )

        viewController?.onDestroy()
        val old = viewController
        viewController = aViewController
        Logcat.d("initViewController:$old, $viewController, controller:$viewController")

        addDocumentView()

        setReflowButton(docViewModel.getReflow())

        return true
    }

    //===========================================

    private fun ocr() {
        if (docViewModel.getReflow() != BookProgress.REFLOW_NO) {
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

        docViewModel.setCurrentPage(pos)

        createControls()
        updateControls()

        if (!initViewController()) {
            viewController?.notifyDataSetChanged()
            return
        }

        viewController?.init()
    }

    private fun doLoadDoc(count: Int, pos: Int, outlineLinks: List<OutlineLink>?) {
        try {
            pageController?.apply {
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
        val reflow = docViewModel.getReflow()
        if (reflow == BookProgress.REFLOW_TXT) {
            docViewModel.storeReflow(BookProgress.REFLOW_NO)
            viewMode = if (docViewModel.checkCrop()) {
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
            docViewModel.storeReflow(BookProgress.REFLOW_TXT)
        }

        applyViewMode(getCurrentPos())
    }

    private fun toggleReflowImage() {
        val reflow = docViewModel.getReflow()
        if (reflow == BookProgress.REFLOW_SCAN) {
            docViewModel.storeReflow(BookProgress.REFLOW_NO)
            viewMode = if (docViewModel.checkCrop()) {
                ViewMode.CROP
            } else {
                ViewMode.NORMAL
            }
        } else {
            viewMode = ViewMode.REFLOW_SCAN
            docViewModel.storeReflow(BookProgress.REFLOW_SCAN)
        }
        if (mReflowLayout.visibility == View.VISIBLE) {
            mReflowLayout.visibility = View.GONE
        }

        applyViewMode(getCurrentPos())
    }

    private fun setReflowButton(reflow: Int) {
        pageController?.setReflowButton(reflow)
        val pos = getCurrentPos()
        if (pos > 0) {
            viewController?.scrollToPosition(pos + 1)
        }
    }

    private val pageListener = object : PageControllerListener {
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
            if (!ttsMode) {
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

        override fun prev(string: String?) {
            viewController?.prev(string)
        }

        override fun next(string: String?) {
            viewController?.next(string)
        }

        override fun clearSearch() {
            viewController?.clearSearch()
        }

        override fun showSearch() {
            viewController?.showSearch();
        }
    }

    private fun createControls() {
        if (IntentFile.isText(mPath!!)) {
            if (pageController is TextPageController) {
            } else {
                pageController = TextPageController(
                    mContentView,
                    docViewModel,
                    pageListener
                )
            }
        } else if (IntentFile.isReflowable(mPath!!) || IntentFile.isMobi(mPath!!)) {
            if (pageController is EpubPageController) {
            } else {
                pageController = EpubPageController(
                    mContentView,
                    docViewModel,
                    pageListener
                )
            }
        } else {
            if (pageController is PdfPageController) {
            } else {
                pageController = PdfPageController(
                    mContentView,
                    docViewModel,
                    pageListener
                )
            }
        }
    }

    private fun startTts() {
        TTSEngine.get().setSpeakListener(object :
            TTSEngine.TtsProgressListener {
            override fun onStart(key: ReflowBean) {
                try {
                    val arr = key.page!!.split("-")
                    val page = Utils.parseInt(arr[0])
                    Logcat.d("onStart:$key, page:$page")
                    if (window.decorView.visibility != View.VISIBLE) {
                        pendingPos = page
                        docViewModel.bookProgress?.progress = pendingPos
                        docViewModel.saveBookProgress(page)
                        return
                    }
                    val current = getCurrentPos()
                    if (current != page) {
                        onSelectedOutline(page)
                    }
                } catch (e: Exception) {
                    Logcat.e(e)
                }
            }

            override fun onDone(key: ReflowBean) {
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
        ttsCircle.setOnClickListener {
            locatePage()
        }
        ttsSleep.setOnClickListener {
            SleepTimerDialog(object : SleepTimerDialog.TimeListener {
                override fun onTime(minute: Int) {
                    Logcat.d("TTSEngine.sleep.onTime()")
                    handler.removeCallbacks(closeRunnable)
                    handler.postDelayed(closeRunnable, (minute * 60000).toLong())
                }
            }).showDialog(this)
        }
        ttsText.setOnClickListener {
            /*if (TTSEngine.get().isSpeaking()) {
                TTSEngine.get().stop()
            }*/

            TtsTextFragment.showCreateDialog(
                this,
                object : DataListener {
                    override fun onSuccess(vararg args: Any?) {
                        val position = args[0] as Int
                        val keys = args[1] as MutableList<ReflowBean>
                        TTSEngine.get().resumeFromKeys(keys, position)
                    }

                    override fun onFailed(vararg args: Any?) {
                    }
                })
        }
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                viewController?.decodePageForTts(getCurrentPos())
            }
        }
    }

    private fun locatePage() {
        val first = TTSEngine.get().first
        if (first >= 0) {
            pendingPos = first
            locatePageForTTS()
        }
    }

    private val closeRunnable = Runnable { closeTts() }

    private fun closeTts() {
        ttsLayout.visibility = View.GONE
        ttsMode = false
        val first = TTSEngine.get().first
        if (first >= 0) {
            pendingPos = first
        }
        if (window.decorView.visibility == View.VISIBLE) {
            locatePageForTTS()
        }
        TTSEngine.get().shutdown()
    }

    private fun changeOri(ori: Int) {
        docViewModel.bookProgress?.scrollOrientation = ori
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
        val prefCrop = !docViewModel.checkCrop()
        val flag = viewMode == ViewMode.REFLOW || viewMode == ViewMode.REFLOW_SCAN
        if (!flag) {
            viewController?.notifyDataSetChanged()
            if (prefCrop) {
                viewMode = ViewMode.CROP
            } else {
                viewMode = ViewMode.NORMAL
            }
            docViewModel.storeCrop(prefCrop)
            applyViewMode(getCurrentPos())
            setReflowButton(docViewModel.getReflow())
        }
    }

    private fun getCurrentPos(): Int {
        if (null == viewController) {
            return 0
        }
        return viewController!!.getCurrentPos()
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
        if (isDocLoaded && pageController?.visibility() == View.VISIBLE) {
            pageController?.updatePageProgress(index)
        }
    }

    //--------------------------------------

    override fun onDestroy() {
        super.onDestroy()

        viewController?.onDestroy()

        isDocLoaded = false
        busEvent(GlobalEvent(Event.ACTION_STOPPED, mPath))

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
        updateControls()
        Logcat.d("onResume $pendingPos")
        if (pendingPos >= 0) {
            locatePageForTTS()
        }
    }

    private fun locatePageForTTS() {
        handler.post {
            onSelectedOutline(pendingPos)
            pendingPos = -1
        }
    }

    override fun onPause() {
        super.onPause()
        if (ttsMode) {
            val first = TTSEngine.get().first
            if (first >= 0) {
                pendingPos = first
            }
        }
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
            if (null == bitmap && TextUtils.isEmpty(path)) {
                return
            }
            //OcrActivity.start(context, bitmap, path, pos.toString())
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.setClassName(context, "com.baidu.ai.edge.ui.activity.OcrActivity")
            intent.putExtra("path", path)
            val key = System.currentTimeMillis().toString()
            BitmapCache.getInstance().addBitmap(key, bitmap!!)
            intent.putExtra("key", key)
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
            docViewModel: DocViewModel,
            path: String,
            pageSeekBarControls: IPageController,
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
                docViewModel,
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
            docViewModel: DocViewModel,
            path: String,
            pageController: IPageController,
            controllerListener: ControllerListener?,
        ): AViewController {
            if (viewMode == ViewMode.CROP) {
                if (IntentFile.isEpub(path) || IntentFile.isMobi(path)) {
                    return AEpubViewController(
                        context,
                        scope,
                        controllerLayout,
                        docViewModel,
                        path,
                        pageController,
                        controllerListener,
                    )
                }
                return ANormalViewController(
                    context,
                    scope,
                    controllerLayout,
                    docViewModel,
                    path,
                    pageController,
                    controllerListener,
                )
            } else if (viewMode == ViewMode.REFLOW) {
                return ATextReflowViewController(
                    context,
                    scope,
                    controllerLayout,
                    docViewModel,
                    path,
                    pageController,
                    controllerListener,
                )
            } else if (viewMode == ViewMode.REFLOW_SCAN) {
                return AScanReflowViewController(
                    context,
                    scope,
                    controllerLayout,
                    docViewModel,
                    path,
                    pageController,
                    controllerListener,
                )
            } else if (viewMode == ViewMode.TEXT) {
                return ATextViewController(
                    context,
                    scope,
                    controllerLayout,
                    docViewModel,
                    path,
                    pageController,
                    controllerListener,
                )
            } else {
                if (IntentFile.isEpub(path) || IntentFile.isMobi(path)) {
                    return AEpubViewController(
                        context,
                        scope,
                        controllerLayout,
                        docViewModel,
                        path,
                        pageController,
                        controllerListener,
                    )
                }
                return ANormalViewController(
                    context,
                    scope,
                    controllerLayout,
                    docViewModel,
                    path,
                    pageController,
                    controllerListener,
                )
            }
        }
    }
}