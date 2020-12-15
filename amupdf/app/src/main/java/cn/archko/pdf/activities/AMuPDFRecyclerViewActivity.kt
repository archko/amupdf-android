package cn.archko.pdf.activities

import android.content.Context
import android.content.Intent
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.mupdf.R
import cn.archko.pdf.common.APageSizeLoader
import cn.archko.pdf.common.BitmapCache
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.MenuHelper
import cn.archko.pdf.common.OutlineHelper
import cn.archko.pdf.common.PDFBookmarkManager
import cn.archko.pdf.entity.APage
import cn.archko.pdf.entity.MenuBean
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.MenuListener
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.presenter.PageViewPresenter
import cn.archko.pdf.utils.FileUtils
import cn.archko.pdf.widgets.APageSeekBarControls
import cn.archko.pdf.widgets.ViewerDividerItemDecoration

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
        if (mReflow) {
            viewMode = ViewMode.REFLOW
        } else if (mCrop) {
            viewMode = ViewMode.CROP
        } else {
            viewMode = ViewMode.NORMAL
        }

        //checkout bookmark
        changeViewMode()

        cropModeSet(mCrop)
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

                if (e.y.toInt() < top) {
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

    private fun changeViewMode() {
        val aViewController = ViewControllerFactory.getOrCreateViewController(
            viewControllerCache,
            viewMode,
            this@AMuPDFRecyclerViewActivity,
            mContentView,
            mControllerLayout, pdfBookmarkManager!!, mPath!!,
            mPageSeekBarControls!!, gestureDetector
        )
        viewController = aViewController
        Logcat.d("changeViewMode:$viewMode,controller:$viewController")
        addDocumentView()
        val pos = pdfBookmarkManager?.bookmark!!
        viewController?.init(mPageSizes, mMupdfDocument, pos)
        viewController?.notifyDataSetChanged()
    }

    override fun doLoadDoc() {
        try {
            progressDialog.setMessage("Loading menu")

            Logcat.d("doLoadDoc:mCrop:$mCrop,mReflow:$mReflow")
            setCropButton(mCrop)

            val pos = pdfBookmarkManager?.restoreBookmark(mMupdfDocument!!.countPages())!!
            viewController?.doLoadDoc(mPageSizes, mMupdfDocument!!, pos)

            mPageSeekBarControls?.showReflow(true)

            outlineHelper = OutlineHelper(mMupdfDocument, this)

            mMenuHelper = MenuHelper(mLeftDrawer, outlineHelper, supportFragmentManager)
            mMenuHelper?.setupMenu(mPath, this@AMuPDFRecyclerViewActivity, menuListener)
            mMenuHelper?.setupOutline(pos)

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
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        } finally {
            progressDialog.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mPageSizes.let {
            if (it.size() < 0 || it.size() < APageSizeLoader.PAGE_COUNT) {
                return
            }
            APageSizeLoader.savePageSizeToFile(
                mCrop,
                pdfBookmarkManager!!.bookmarkToRestore!!.size,
                mPageSizes,
                FileUtils.getDiskCacheDir(
                    this@AMuPDFRecyclerViewActivity,
                    pdfBookmarkManager?.bookmarkToRestore?.name
                )
            )
        }
    }

    override fun preparePageSize(cp: Int) {
        val mRecyclerView = viewController?.getDocumentView()!!
        val width = mRecyclerView.width
        var start = SystemClock.uptimeMillis()
        var pageSizeBean: APageSizeLoader.PageSizeBean? = null
        if (pdfBookmarkManager != null && pdfBookmarkManager!!.bookmarkToRestore != null) {
            pageSizeBean = APageSizeLoader.loadPageSizeFromFile(
                width,
                pdfBookmarkManager!!.bookmarkToRestore!!.pageCount,
                pdfBookmarkManager!!.bookmarkToRestore!!.size,
                FileUtils.getDiskCacheDir(
                    this@AMuPDFRecyclerViewActivity,
                    pdfBookmarkManager?.bookmarkToRestore?.name
                )
            )
        }
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
            super.preparePageSize(cp)
            Logcat.d("open2:" + (SystemClock.uptimeMillis() - start))
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
                    super.preparePageSize(cp)
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
        changeViewMode()

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
            viewController?.scrollToPosition(pos)
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
                return mMupdfDocument!!.countPages()
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
        })
        return mPageSeekBarControls!!
    }

    private fun showOutline() {
        outlineHelper?.let {
            if (it.hasOutline()) {
                val frameLayout = mPageSeekBarControls?.getLayoutOutline()

                if (frameLayout?.visibility == View.GONE) {
                    frameLayout.visibility = View.VISIBLE
                    mMenuHelper?.updateSelection(getCurrentPos())
                } else {
                    frameLayout?.visibility = View.GONE
                }
            } else {
                mPageSeekBarControls?.getLayoutOutline()?.visibility = View.GONE
            }
        }
    }

    private fun toggleCrop() {
        var flag = cropModeSet(!mCrop)
        if (flag) {
            BitmapCache.getInstance().clear()
            viewController?.notifyDataSetChanged()
            mCrop = !mCrop
            if (mCrop) {
                viewMode = ViewMode.CROP
            } else {
                viewMode = ViewMode.NORMAL
            }
            changeViewMode()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            OUTLINE_REQUEST -> {
                onSelectedOutline(resultCode)
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
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
        if (mCrop) {
            pdfBookmarkManager?.bookmarkToRestore?.autoCrop = 0
        } else {
            pdfBookmarkManager?.bookmarkToRestore?.autoCrop = 1
        }
        if (mReflow) {
            pdfBookmarkManager?.bookmarkToRestore?.reflow = 1
        } else {
            pdfBookmarkManager?.bookmarkToRestore?.reflow = 0
        }
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
            context: Context,
            contentView: View,
            controllerLayout: RelativeLayout,
            pdfBookmarkManager: PDFBookmarkManager,
            path: String,
            pageSeekBarControls: APageSeekBarControls,
            gestureDetector: GestureDetector?
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
                pdfBookmarkManager,
                path,
                pageSeekBarControls,
                gestureDetector
            )
        }

        fun createViewController(
            viewMode: ViewMode, context: Context,
            contentView: View,
            controllerLayout: RelativeLayout,
            pdfBookmarkManager: PDFBookmarkManager,
            path: String,
            pageSeekBarControls: APageSeekBarControls,
            gestureDetector: GestureDetector?
        ): AViewController {
            if (viewMode == ViewMode.CROP) {
                return ACropViewController(
                    context,
                    contentView,
                    controllerLayout,
                    pdfBookmarkManager,
                    path,
                    pageSeekBarControls,
                    gestureDetector
                )
            } else if (viewMode == ViewMode.REFLOW) {
                return AReflowViewController(
                    context,
                    contentView,
                    controllerLayout,
                    pdfBookmarkManager,
                    path,
                    pageSeekBarControls,
                    gestureDetector
                )
            } else {
                return ANormalViewController(
                    context,
                    contentView,
                    controllerLayout,
                    pdfBookmarkManager,
                    path,
                    pageSeekBarControls,
                    gestureDetector
                )
            }
        }
    }

    enum class ViewMode {
        NORMAL, CROP, REFLOW
    }
}
