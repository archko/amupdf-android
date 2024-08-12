package cn.archko.pdf.activities

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import cn.archko.pdf.R
import cn.archko.pdf.common.OutlineHelper
import cn.archko.pdf.core.cache.BitmapCache
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.entity.Bookmark
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.fragments.OutlineFragment
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.viewmodel.PDFViewModel
import cn.archko.pdf.widgets.PageControls
import com.baidu.ai.edge.ui.activity.OcrActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @author: archko 2019/8/25 :12:43
 */
class AMuPDFRecyclerViewActivity : MuPDFRecyclerViewActivity(), OutlineListener {

    private var pageControls: PageControls? = null
    private var outlineHelper: OutlineHelper? = null
    private var outlineFragment: OutlineFragment? = null
    private lateinit var mReflowLayout: RelativeLayout
    private lateinit var mContentView: View
    private val viewControllerCache: SparseArray<AViewController> = SparseArray<AViewController>()
    private var viewMode: ViewMode = ViewMode.CROP

    /**
     * 用AMupdf打开,传入强制切边参数,如果是-1,是没有设置,如果设置1表示强制切边,如果是0不切边,让切边按钮失效
     */
    private var forceCropParam = -1

    override fun initView() {
        super.initView()
        forceCropParam = intent.getIntExtra("forceCropParam", -1)

        mContentView = findViewById(R.id.content)
        mReflowLayout = findViewById(R.id.reflow_layout)

        pageControls = createControls()
        pageControls?.apply {
            updateTitle(mPath)
            autoCropButton!!.visibility = View.VISIBLE
        }

        documentView = findViewById(R.id.document_view)

        initTouchParams()
    }

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

    override fun getDocumentView(): View? {
        return viewController?.getDocumentView()!!
    }

    private fun addDocumentView() {
        documentView?.removeAllViews()
        val lap: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        documentView?.addView(viewController?.getDocumentView(), lap)
    }

    private fun initTouchParams() {
        val view = documentView!!
        var margin = view.height
        margin = if (margin <= 0) {
            ViewConfiguration.get(this).scaledTouchSlop * 2
        } else {
            (margin * 0.03).toInt()
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
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                return false
            }

            override fun onLongPress(e: MotionEvent) {

            }

            override fun onFling(
                e1: MotionEvent?,
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
                if (mReflowLayout.visibility == View.VISIBLE) {
                    mReflowLayout.visibility = View.GONE
                    return true
                }

                if (onSingleTap()) {
                    return true
                }

                //return viewController?.scrollPage(e.y.toInt(), top, bottom, finalMargin)!!
                if (viewController?.onSingleTap(e, finalMargin) == false) {
                    showPageToast()
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

    private fun applyViewMode(pos: Int) {
        if (!pdfViewModel.checkReflow() && pdfViewModel.checkCrop() == viewController?.getCrop()) {
            Logcat.d("applyViewMode:crop don't change, controller:$viewController")
            return
        }
        viewController?.onDestroy()

        val aViewController = ViewControllerFactory.getOrCreateViewController(
            viewControllerCache,
            viewMode,
            this@AMuPDFRecyclerViewActivity,
            mReflowLayout, pdfViewModel, mPath!!,
            pageControls!!,
            gestureDetector
        )
        viewController = aViewController
        Logcat.d("applyViewMode:$viewMode, pos:$pos,forceCropParam: $forceCropParam, controller:$viewController")

        addDocumentView()
        viewController?.init(pageSizes, pos, pdfViewModel.bookProgress?.scrollOrientation ?: 1)
        viewController?.notifyDataSetChanged()
    }

    /*override fun loadDoc(password: String?) {
        lifecycleScope.launch {
            val ocr = PdfOptionRepository.getImageOcr()
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
    }*/

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
            setCropButton(pdfViewModel.checkCrop())

            val pos = pdfViewModel.getCurrentPage(pdfViewModel.countPages())
            Logcat.d("doLoadDoc:pos:$pos")
            viewController?.doLoadDoc(pageSizes, pos)

            pageControls?.apply {
                update(pageSizes.size, pos)
                showReflow(pdfViewModel.checkReflow())
                orientation = pdfViewModel.bookProgress?.scrollOrientation ?: 1
            }

            //outlineHelper = OutlineHelper(pdfViewModel.mupdfDocument, this)
            outlineHelper = pdfViewModel.outlineHelper

            //mMenuHelper = MenuHelper(mLeftDrawer, outlineHelper, supportFragmentManager)
            //mMenuHelper?.setupMenu(mPath, this@AMuPDFRecyclerViewActivity, menuListener)
            //mMenuHelper?.setupOutline(pos)
            setupOutline(pos)

            isDocLoaded = true

            val sp = getSharedPreferences(PREF_READER, Context.MODE_PRIVATE)
            val isFirst = sp.getBoolean(PREF_READER_KEY_FIRST, true)
            if (isFirst) {
                //mDrawerLayout.openDrawer(mLeftDrawer)
                showOutline()

                sp.edit()
                    .putBoolean(PREF_READER_KEY_FIRST, false)
                    .apply()
            }

            //checkout bookmark
            applyViewMode(pos - 1)

            cropModeSet(pdfViewModel.checkCrop())
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        } finally {
            //progressDialog.dismiss()
        }
    }

    private fun setupOutline(currentPos: Int?) {
        if (null == outlineFragment) {
            outlineFragment = OutlineFragment()
            val bundle = Bundle()
            if (outlineHelper!!.hasOutline()) {
                outlineFragment!!.outlineItems = outlineHelper!!.getOutlineItems()
                bundle.putSerializable("out", outlineHelper?.getOutlineItems())
            }
            bundle.putSerializable("POSITION", currentPos)
            outlineFragment?.arguments = bundle
        }

        //supportFragmentManager.beginTransaction()
        //    .add(R.id.layout_outline, outlineFragment!!)
        //    .commit()
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

    override fun onDestroy() {
        super.onDestroy()
        viewController?.onDestroy()
        /*pageSizes.let {
            if (it.size < 0 || it.size < APageSizeLoader.PAGE_COUNT) {
                return
            }
            lifecycleScope.launch {
                pdfViewModel.savePageSize(pdfViewModel.checkCrop(), pageSizes).collectLatest { }
            }
        }*/
    }

    override fun postLoadDoc(cp: Int) {
        val mRecyclerView = viewController?.getDocumentView()
        val width =
            mRecyclerView?.width ?: Utils.getScreenWidthPixelWithOrientation(this)
        var start = SystemClock.uptimeMillis()

        lifecycleScope.launch {
            pdfViewModel.preparePageSize(width).collectLatest { pageSizeBean ->
                Logcat.d("open3:" + (SystemClock.uptimeMillis() - start))

                var pageSizes: List<APage>? = null
                if (pageSizeBean != null) {
                    pageSizes = pageSizeBean.List
                }
                if (!pageSizes.isNullOrEmpty()) {
                    Logcat.d("open3:pageSizes>0:" + pageSizes.size)
                    this@AMuPDFRecyclerViewActivity.pageSizes.clear()
                    this@AMuPDFRecyclerViewActivity.pageSizes.addAll(pageSizes)
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
        for (i in 0 until pageSizes.size) {
            val pointF = getPageSize(i)
            if (pointF != null) {
                pageSizes.add(pointF)
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
        } else {
            viewMode = ViewMode.REFLOW
        }
        pdfViewModel.storeReflow(reflow)

        applyViewMode(getCurrentPos())

        setReflowButton(reflow)

        Toast.makeText(
            this,
            if (reflow) getString(R.string.entering_reflow_mode) else getString(R.string.leaving_reflow_mode),
            Toast.LENGTH_SHORT
        ).show()
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

    override fun onSingleTap(): Boolean {
        if (pageControls?.visibility() == View.VISIBLE) {
            pageControls?.hide()
            return true
        }
        return false
    }

    override fun onDoubleTap() {
        super.onDoubleTap()
        if (!isDocLoaded) {
            return
        }

        pageControls?.toggleControls()
        viewController?.onDoubleTap()

        if (mReflowLayout.visibility == View.VISIBLE) {
            mReflowLayout.visibility = View.GONE
            return
        }
    }

    private fun createControls(): PageControls {
        pageControls = PageControls(
            mContentView,
            object : PageControls.ControlListener {
                override fun toggleReflow() {
                    toggleReflow()
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
                    //mPageSeekBarControls?.hide()
                }

                override fun changeOrientation(ori: Int) {
                    changeOri(ori)
                }
            }
        )
        return pageControls!!
    }

    private fun changeOri(ori: Int) {
        pdfViewModel.bookProgress?.scrollOrientation = ori
        viewController?.setOrientation(ori)
    }

    private fun showOutline() {
        outlineHelper?.let {
            if (it.hasOutline()) {
                outlineFragment?.updateSelection(getCurrentPos())
            }
        }
        outlineFragment?.showDialog(this)
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

    private fun cropModeSet(crop: Boolean): Boolean {
        if (pdfViewModel.checkReflow()) {
            Toast.makeText(
                this,
                getString(R.string.in_reflow_mode),
                Toast.LENGTH_SHORT
            ).show()
            pageControls?.reflowButton!!.setColorFilter(Color.argb(0xFF, 172, 114, 37))
            pageControls?.autoCropButton!!.setColorFilter(Color.argb(0xFF, 255, 255, 255))
            return false
        }
        setCropButton(crop)
        return true
    }

    private fun setCropButton(crop: Boolean) {
        if (crop) {
            pageControls?.autoCropButton!!.setColorFilter(Color.argb(0xFF, 172, 114, 37))
        } else {
            pageControls?.autoCropButton!!.setColorFilter(Color.argb(0xFF, 255, 255, 255))
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
        if (isDocLoaded && pageControls?.visibility() == View.VISIBLE) {
            pageControls?.updatePageProgress(index)
        }
    }
    //--------------------------------------

    override fun onResume() {
        super.onResume()

        pageControls?.hide()

        viewController?.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewController?.onPause()
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

    /*private var menuListener = object : MenuListener {

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
    }*/

    internal object ViewControllerFactory {
        fun getOrCreateViewController(
            viewControllerCache: SparseArray<AViewController>,
            viewMode: ViewMode,
            context: FragmentActivity,
            controllerLayout: RelativeLayout,
            pdfViewModel: PDFViewModel,
            path: String,
            pageSeekBarControls: PageControls,
            gestureDetector: GestureDetector?,
        ): AViewController {
            //val aViewController = viewControllerCache.get(viewMode.ordinal)
            //if (null != aViewController) {
            //    return aViewController
            //}
            return createViewController(
                viewMode,
                context,
                controllerLayout,
                pdfViewModel,
                path,
                pageSeekBarControls,
                gestureDetector,
            )
        }

        fun createViewController(
            viewMode: ViewMode,
            context: FragmentActivity,
            controllerLayout: RelativeLayout,
            pdfViewModel: PDFViewModel,
            path: String,
            pageSeekBarControls: PageControls,
            gestureDetector: GestureDetector?,
        ): AViewController {
            if (viewMode == ViewMode.CROP) {
                return ANormalViewController(
                    context,
                    controllerLayout,
                    pdfViewModel,
                    path,
                    pageSeekBarControls,
                    gestureDetector,
                    true,
                )
            } else if (viewMode == ViewMode.REFLOW) {
                return AReflowViewController(
                    context,
                    controllerLayout,
                    pdfViewModel,
                    path,
                    pageSeekBarControls,
                    gestureDetector,
                )
            } else {
                return ANormalViewController(
                    context,
                    controllerLayout,
                    pdfViewModel,
                    path,
                    pageSeekBarControls,
                    gestureDetector,
                    false,
                )
            }
        }
    }

    enum class ViewMode {
        NORMAL, CROP, REFLOW
    }
}
