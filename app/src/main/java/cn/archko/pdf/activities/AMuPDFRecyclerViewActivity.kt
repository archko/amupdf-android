package cn.archko.pdf.activities

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.util.SparseArray
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.mupdf.R
import cn.archko.pdf.common.APageSizeLoader
import cn.archko.pdf.common.BitmapCache
import cn.archko.pdf.common.IntentFile
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.MenuHelper
import cn.archko.pdf.common.OutlineHelper
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.entity.APage
import cn.archko.pdf.entity.Bookmark
import cn.archko.pdf.entity.MenuBean
import cn.archko.pdf.fragments.BookmarkFragment
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.MenuListener
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.presenter.PageViewPresenter
import cn.archko.pdf.utils.Utils
import cn.archko.pdf.viewmodel.PDFViewModel
import cn.archko.pdf.widgets.APageSeekBarControls
import cn.archko.pdf.widgets.ViewerDividerItemDecoration
import com.baidu.ai.edge.ui.activity.OcrActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * @author: archko 2019/8/25 :12:43
 */
class AMuPDFRecyclerViewActivity : MuPDFRecyclerViewActivity(), OutlineListener {

    private lateinit var mLeftDrawer: RecyclerView
    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var mControllerLayout: RelativeLayout

    private var mPageSeekBarControls: APageSeekBarControls? = null
    private var outlineHelper: OutlineHelper? = null

    private var mMenuHelper: MenuHelper? = null
    private lateinit var mContentView: View
    private val viewControllerCache: SparseArray<AViewController> = SparseArray<AViewController>()
    private var viewMode: ViewMode = ViewMode.CROP

    override fun onCreate(savedInstanceState: Bundle?) {
        BitmapCache.getInstance().resize(BitmapCache.CAPACITY_FOR_AMUPDF)
        super.onCreate(savedInstanceState)
    }

    override fun initView() {
        super.initView()

        mPageSeekBarControls?.updateTitle(mPath)
        mLeftDrawer = findViewById(R.id.left_drawer)
        mDrawerLayout = findViewById(R.id.drawerLayout)

        mControllerLayout = findViewById(R.id.layout)

        mPageSeekBarControls = createSeekControls()

        val lp = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        mControllerLayout.addView(mPageSeekBarControls, lp)

        mPageSeekBarControls?.autoCropButton!!.visibility = View.VISIBLE

        with(mLeftDrawer) {
            layoutManager = LinearLayoutManager(
                this@AMuPDFRecyclerViewActivity,
                LinearLayoutManager.VERTICAL,
                false
            )
            addItemDecoration(
                ViewerDividerItemDecoration(
                    this@AMuPDFRecyclerViewActivity,
                    LinearLayoutManager.VERTICAL
                )
            )
        }

        mContentView = findViewById(R.id.content)
        mDocumentView = findViewById(R.id.document_view)

        initTouchParams()
    }

    override fun getDocumentView(): View? {
        return viewController?.getDocumentView()!!
    }

    private fun addDocumentView() {
        mDocumentView?.removeAllViews()
        val lap: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        mDocumentView?.addView(viewController?.getDocumentView(), lap)
    }

    private fun initTouchParams() {
        val view = mDocumentView!!
        var margin = view.height
        if (margin <= 0) {
            margin = ViewConfiguration.get(this).scaledTouchSlop * 2
        } else {
            margin = (margin * 0.03).toInt()
        }
        gestureDetector = GestureDetector(this, object : GestureDetector.OnGestureListener {

            override fun onDown(e: MotionEvent): Boolean {
                return false
            }

            override fun onShowPress(e: MotionEvent) {

            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                return false
            }

            override fun onScroll(
                e1: MotionEvent,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                return false
            }

            override fun onLongPress(e: MotionEvent) {

            }

            override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                return false
            }
        })

        val finalMargin = margin
        gestureDetector!!.setOnDoubleTapListener(object : GestureDetector.OnDoubleTapListener {

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val documentView = getDocumentView()!!
                val top = documentView.height / 4
                val bottom = documentView.height * 3 / 4

                /*if (e.y.toInt() < top) {
                    var scrollY = documentView.scrollY
                    scrollY -= documentView.height
                    documentView.scrollBy(0, scrollY + finalMargin)
                    return true
                } else if (e.y.toInt() > bottom) {
                    var scrollY = documentView.scrollY
                    scrollY += documentView.height
                    documentView.scrollBy(0, scrollY - finalMargin)
                    return true
                } else {
                    onSingleTap()
                }*/
                val rs: Boolean =
                    viewController?.scrollPage(e.y.toInt(), top, bottom, finalMargin)!!
                if (!rs) {
                    onSingleTap()
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                onDoubleTap()
                return true
            }

            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                return false
            }
        })
    }

    private fun changeViewMode(pos: Int) {
        viewController?.onDestroy()

        val aViewController = ViewControllerFactory.getOrCreateViewController(
            viewControllerCache,
            viewMode,
            this@AMuPDFRecyclerViewActivity,
            mContentView,
            mControllerLayout, pdfViewModel, mPath!!,
            mPageSeekBarControls!!,
            gestureDetector,
            optionRepository
        )
        viewController = aViewController
        Logcat.d("changeViewMode:$viewMode, pos:$pos, controller:$viewController")
        addDocumentView()
        viewController?.init(mPageSizes, pos)
        viewController?.notifyDataSetChanged()
    }

    override fun loadDoc(password: String?) {
        lifecycleScope.launch {
            val ocr = optionRepository.pdfOptionFlow.first().imageOcr
            if (IntentFile.isText(mPath)) {
                TextActivity.start(this@AMuPDFRecyclerViewActivity, mPath!!)
                finish()
            } else if (IntentFile.isImage(mPath) && ocr) {
                OcrActivity.start(
                    this@AMuPDFRecyclerViewActivity,
                    null,
                    mPath,
                    System.currentTimeMillis().toString()
                )
                finish()
            } else {
                super.loadDoc(password)
            }
        }
    }

    override fun showPasswordDialog() {
        PasswordDialog.show(this@AMuPDFRecyclerViewActivity,
            object : PasswordDialog.PasswordDialogListener {
                override fun onOK(content: String?) {
                    loadDoc(password = content)
                }

                override fun onCancel() {
                    Toast.makeText(
                        this@AMuPDFRecyclerViewActivity,
                        "error file path:$mPath",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    override fun doLoadDoc() {
        try {
            progressDialog.setMessage("Loading menu")

            setCropButton(mCrop)

            val pos = pdfViewModel.getCurrentPage(pdfViewModel.countPages())
            Logcat.d("doLoadDoc:mCrop:$mCrop,mReflow:$mReflow, pos:$pos")
            viewController?.doLoadDoc(mPageSizes, pos)

            mPageSeekBarControls?.showReflow(true)

            outlineHelper = OutlineHelper(pdfViewModel.mupdfDocument, this)

            mMenuHelper = MenuHelper(mLeftDrawer, outlineHelper, supportFragmentManager)
            mMenuHelper?.apply {
                setupMenu(mPath, this@AMuPDFRecyclerViewActivity, menuListener)
                mMenuHelper?.itemListener = object : BookmarkFragment.ItemListener {
                    override fun onClick(data: Bookmark, position: Int) {
                        viewController?.scrollToPosition(data.page)
                    }

                    override fun onDelete(data: Bookmark, position: Int) {
                        deleteBookmark(data)
                    }

                    override fun onAdd(page: Int) {
                        addBookmark(page)
                    }
                }
                mMenuHelper?.setupOutline(pos)
            }

            isDocLoaded = true

            val sp = getSharedPreferences(PREF_READER, Context.MODE_PRIVATE)
            val isFirst = sp.getBoolean(PREF_READER_KEY_FIRST, true)
            if (isFirst) {
                mDrawerLayout.openDrawer(mLeftDrawer)
                showOutline()

                sp.edit()
                    .putBoolean(PREF_READER_KEY_FIRST, false)
                    .apply()
            }

            if (mReflow) {
                viewMode = ViewMode.REFLOW
            } else if (mCrop) {
                viewMode = ViewMode.CROP
            } else {
                viewMode = ViewMode.NORMAL
            }

            //checkout bookmark
            changeViewMode(pos - 1)

            cropModeSet(mCrop)
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        } finally {
            progressDialog.dismiss()
        }
    }

    private fun deleteBookmark(bookmark: Bookmark) {
        lifecycleScope.launch {
            pdfViewModel.deleteBookmark(bookmark).collectLatest {
                mMenuHelper?.updateBookmark(getCurrentPos(), it)
                viewController?.notifyDataSetChanged()
            }
        }
    }

    private fun addBookmark(page: Int) {
        lifecycleScope.launch {
            val currentPos = getCurrentPos()
            pdfViewModel.addBookmark(currentPos).collectLatest {
                viewController?.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewController?.onDestroy()
        mPageSizes.let {
            if (it.size() < 0 || it.size() < APageSizeLoader.PAGE_COUNT) {
                return
            }
            lifecycleScope.launch {
                pdfViewModel.savePageSize(mCrop, mPageSizes).collectLatest { }
            }
        }
    }

    override fun postLoadDoc(cp: Int) {
        val mRecyclerView = viewController?.getDocumentView()
        val width =
            mRecyclerView?.width ?: Utils.getScreenWidthPixelWithOrientation(this)
        var start = SystemClock.uptimeMillis()

        lifecycleScope.launch {
            pdfViewModel.preparePageSize(width).collectLatest { pageSizeBean ->
                Logcat.d("open3:" + (SystemClock.uptimeMillis() - start))

                var pageSizes: SparseArray<APage>? = null
                if (pageSizeBean != null) {
                    pageSizes = pageSizeBean.sparseArray
                }
                if (pageSizes != null && pageSizes.size() > 0) {
                    Logcat.d("open3:pageSizes>0:" + pageSizes.size())
                    mPageSizes = pageSizes
                    checkPageSize(cp)
                } else {
                    start = SystemClock.uptimeMillis()
                    preparePageSize(cp)
                    Logcat.d("open2:" + (SystemClock.uptimeMillis() - start))
                }
                doLoadDoc()
            }
        }
    }

    /**
     * if scale=1.0f,reload it from mupdf
     */
    private fun checkPageSize(cp: Int) {
        for (i in 0 until mPageSizes.size()) {
            val point = mPageSizes.valueAt(i)
            if (point.scale == 1.0f) {
                val pointF = getPageSize(i)
                if (null == point) {
                    mPageSizes.clear()
                    preparePageSize(cp)
                    break
                }
                mPageSizes.put(i, pointF)
            }
        }
    }

    private fun toggleReflow() {
        if (mReflow) {  //如果原来是文本重排模式,则切换为切割或普通模式
            if (mCrop) {
                viewMode = ViewMode.CROP
            } else {
                viewMode = ViewMode.NORMAL
            }
        } else {
            viewMode = ViewMode.REFLOW
        }
        changeViewMode(getCurrentPos())

        mReflow = !mReflow
        setReflowButton(mReflow)

        Toast.makeText(
            this,
            if (mReflow) getString(R.string.entering_reflow_mode) else getString(R.string.leaving_reflow_mode),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun setReflowButton(reflow: Boolean) {
        if (reflow) {
            mPageSeekBarControls?.reflowButton!!.setColorFilter(Color.argb(0xFF, 172, 114, 37))
        } else {
            mPageSeekBarControls?.reflowButton!!.setColorFilter(Color.argb(0xFF, 255, 255, 255))
        }
        setCropButton(mCrop)
        val pos = getCurrentPos()
        if (pos > 0) {
            viewController?.scrollToPosition(pos + 1)
        }
    }

    override fun onSingleTap() {
        if (mPageSeekBarControls?.visibility == View.VISIBLE) {
            mPageSeekBarControls?.hide()
            return
        }
        super.onSingleTap()
        viewController?.onSingleTap()
    }

    override fun onDoubleTap() {
        super.onDoubleTap()
        if (!isDocLoaded) {
            return
        }
        mPageSeekBarControls?.hide()
        if (!mDrawerLayout.isDrawerOpen(mLeftDrawer)) {
            mDrawerLayout.openDrawer(mLeftDrawer)
        } else {
            mDrawerLayout.closeDrawer(mLeftDrawer)
        }
        showOutline()
        viewController?.onDoubleTap()
    }

    private fun createSeekControls(): APageSeekBarControls {
        mPageSeekBarControls = APageSeekBarControls(this, object : PageViewPresenter {
            override fun reflow() {
                toggleReflow()
            }

            override fun getPageCount(): Int {
                return pdfViewModel.countPages()
            }

            override fun getCurrentPageIndex(): Int {
                return getCurrentPos()
            }

            override fun goToPageIndex(page: Int) {
                viewController?.scrollToPosition(page)
            }

            override fun showOutline() {
                this@AMuPDFRecyclerViewActivity.showOutline()
            }

            override fun back() {
                //this@MuPDFRecyclerViewActivity.finish()
                mPageSeekBarControls?.hide()
            }

            override fun getTitle(): String {
                return mPath!!
            }

            override fun autoCrop() {
                toggleCrop()
            }

            override fun showBookmark() {
                this@AMuPDFRecyclerViewActivity.showBookmark()
            }
        })
        return mPageSeekBarControls!!
    }

    private fun showOutline() {
        outlineHelper?.let {
            if (it.hasOutline()) {
                val frameLayout = mPageSeekBarControls?.layoutOutline

                if (frameLayout?.visibility == View.GONE) {
                    frameLayout.visibility = View.VISIBLE
                    mMenuHelper?.updateSelection(getCurrentPos())
                } else {
                    if (mMenuHelper!!.isOutline()) {
                        frameLayout?.visibility = View.GONE
                    } else {
                        mMenuHelper?.updateSelection(getCurrentPos())
                    }
                }
            } else {
                mPageSeekBarControls?.layoutOutline?.visibility = View.GONE
            }
        }
    }

    private fun showBookmark() {
        outlineHelper?.let {
            val frameLayout = mPageSeekBarControls?.layoutOutline

            /*val bookmarks = arrayListOf<Bookmark>()
            var element = Bookmark()
            element.page = 3
            bookmarks.add(element)
            element = Bookmark()
            element.page = 30
            bookmarks.add(element)*/
            if (frameLayout?.visibility == View.GONE) {
                frameLayout.visibility = View.VISIBLE
                mMenuHelper?.showBookmark(getCurrentPos(), pdfViewModel.bookmarks)
            } else {
                if (mMenuHelper!!.isOutline()) {
                    mMenuHelper?.showBookmark(getCurrentPos(), pdfViewModel.bookmarks)
                } else {
                    frameLayout?.visibility = View.GONE
                }
            }
        }
    }

    private fun toggleCrop() {
        val flag = cropModeSet(!mCrop)
        if (flag) {
            BitmapCache.getInstance().clear()
            viewController?.notifyDataSetChanged()
            mCrop = !mCrop
            if (mCrop) {
                viewMode = ViewMode.CROP
            } else {
                viewMode = ViewMode.NORMAL
            }
            changeViewMode(getCurrentPos())
        }
    }

    private fun cropModeSet(crop: Boolean): Boolean {
        if (mReflow) {
            Toast.makeText(this, getString(R.string.in_reflow_mode), Toast.LENGTH_SHORT).show()
            mPageSeekBarControls?.reflowButton!!.setColorFilter(Color.argb(0xFF, 172, 114, 37))
            mPageSeekBarControls?.autoCropButton!!.setColorFilter(Color.argb(0xFF, 255, 255, 255))
            return false
        }
        setCropButton(crop)
        return true
    }

    private fun setCropButton(crop: Boolean) {
        if (crop) {
            mPageSeekBarControls?.autoCropButton!!.setColorFilter(Color.argb(0xFF, 172, 114, 37))
        } else {
            mPageSeekBarControls?.autoCropButton!!.setColorFilter(Color.argb(0xFF, 255, 255, 255))
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

    override fun updateProgress(index: Int) {
        if (isDocLoaded && mPageSeekBarControls?.visibility == View.VISIBLE) {
            mPageSeekBarControls?.updatePageProgress(index)
        }
    }
    //--------------------------------------

    override fun onResume() {
        super.onResume()

        mPageSeekBarControls?.hide()
        mDrawerLayout.closeDrawers()

        viewController?.onResume()
    }

    override fun onPause() {
        super.onPause()
        pdfViewModel.storeCropAndReflow(mCrop, mReflow)
        Logcat.d("onPause:mCrop:$mCrop,mReflow:$mReflow")
        viewController?.onPause()
    }

    //===========================================

    companion object {

        private const val TAG = "AMuPDFRecyclerViewActivity"
        const val PREF_READER = "pref_reader_amupdf"
        const val PREF_READER_KEY_FIRST = "pref_reader_key_first"
    }

    //===========================================

    private var menuListener = object : MenuListener {

        override fun onMenuSelected(data: MenuBean?, position: Int) {
            when (data?.type) {
                TYPE_PROGRESS -> {
                    mDrawerLayout.closeDrawer(mLeftDrawer)
                    mPageSeekBarControls?.show()
                }

                TYPE_ZOOM -> {
                    mDrawerLayout.closeDrawer(mLeftDrawer)
                    viewController?.showController()
                }

                TYPE_CLOSE -> {
                    this@AMuPDFRecyclerViewActivity.finish()
                }

                TYPE_SETTINGS -> {
                    PdfOptionsActivity.start(this@AMuPDFRecyclerViewActivity)
                }

                else -> {
                }
            }
        }

    }

    internal object ViewControllerFactory {
        fun getOrCreateViewController(
            viewControllerCache: SparseArray<AViewController>,
            viewMode: ViewMode,
            context: FragmentActivity,
            contentView: View,
            controllerLayout: RelativeLayout,
            pdfViewModel: PDFViewModel,
            path: String,
            pageSeekBarControls: APageSeekBarControls,
            gestureDetector: GestureDetector?,
            optionRepository: PdfOptionRepository,
        ): AViewController {
            val aViewController = viewControllerCache.get(viewMode.ordinal)
            if (null != aViewController) {
                return aViewController
            }
            return createViewController(
                viewMode,
                context,
                contentView,
                controllerLayout,
                pdfViewModel,
                path,
                pageSeekBarControls,
                gestureDetector,
                optionRepository
            )
        }

        fun createViewController(
            viewMode: ViewMode,
            context: FragmentActivity,
            contentView: View,
            controllerLayout: RelativeLayout,
            pdfViewModel: PDFViewModel,
            path: String,
            pageSeekBarControls: APageSeekBarControls,
            gestureDetector: GestureDetector?,
            optionRepository: PdfOptionRepository,
        ): AViewController {
            if (viewMode == ViewMode.CROP) {
                return ACropViewController(
                    context,
                    contentView,
                    controllerLayout,
                    pdfViewModel,
                    path,
                    pageSeekBarControls,
                    gestureDetector,
                    optionRepository
                )
            } else if (viewMode == ViewMode.REFLOW) {
                return AReflowViewController(
                    context,
                    contentView,
                    controllerLayout,
                    pdfViewModel,
                    path,
                    pageSeekBarControls,
                    gestureDetector,
                    optionRepository
                )
            } else {
                return ANormalViewController(
                    context,
                    contentView,
                    controllerLayout,
                    pdfViewModel,
                    path,
                    pageSeekBarControls,
                    gestureDetector,
                    optionRepository
                )
            }
        }
    }

    enum class ViewMode {
        NORMAL, CROP, REFLOW
    }
}
