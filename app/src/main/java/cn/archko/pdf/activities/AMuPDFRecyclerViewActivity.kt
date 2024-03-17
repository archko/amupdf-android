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
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import cn.archko.pdf.R
import cn.archko.pdf.common.APageSizeLoader
import cn.archko.pdf.common.BitmapCache
import cn.archko.pdf.common.IntentFile
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.OutlineHelper
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.entity.APage
import cn.archko.pdf.entity.Bookmark
import cn.archko.pdf.fragments.OutlineFragment
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.presenter.PageViewPresenter
import cn.archko.pdf.utils.Utils
import cn.archko.pdf.viewmodel.PDFViewModel
import cn.archko.pdf.widgets.APageSeekBarControls
import cn.archko.pdf.widgets.BaseMenu
import cn.archko.pdf.widgets.CakeView
import cn.archko.pdf.widgets.type_crop
import cn.archko.pdf.widgets.type_exit
import cn.archko.pdf.widgets.type_ocr
import cn.archko.pdf.widgets.type_outline
import cn.archko.pdf.widgets.type_reflow
import cn.archko.pdf.widgets.type_scroll_ori
import cn.archko.pdf.widgets.type_seek
import com.baidu.ai.edge.ui.activity.OcrActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * @author: archko 2019/8/25 :12:43
 */
class AMuPDFRecyclerViewActivity : MuPDFRecyclerViewActivity(), OutlineListener {

    private lateinit var mControllerLayout: RelativeLayout

    private var mPageSeekBarControls: APageSeekBarControls? = null
    private var outlineHelper: OutlineHelper? = null

    private var outlineFragment: OutlineFragment? = null
    private lateinit var mContentView: View
    private val viewControllerCache: SparseArray<AViewController> = SparseArray<AViewController>()
    private var viewMode: ViewMode = ViewMode.CROP

    /**
     * 用AMupdf打开,传入强制切边参数,如果是-1,是没有设置,如果设置1表示强制切边,如果是0不切边,让切边按钮失效
     */
    private var forceCropParam = -1

    private val menus: MutableList<BaseMenu> = mutableListOf()
    private var color = Color.parseColor("#3783f6") //圆形菜单颜色
    private val selectedColor = Color.parseColor("#AC7225")
    private lateinit var reflowStr: String
    private lateinit var ocrStr: String
    private lateinit var autoCropStr: String
    private lateinit var outlineStr: String
    private lateinit var seekStr: String
    private lateinit var scrollOriStr: String
    private lateinit var exitStr: String

    private fun initMenus() {
        menus.clear()
        var menu: BaseMenu
        if (pdfViewModel.bookProgress?.reflow == 0) {   //不重排
            menu = BaseMenu(color, 1f, reflowStr, -1, type_reflow)
            menus.add(menu)
            menu = BaseMenu(color, 1f, ocrStr, -1, type_ocr)
            menus.add(menu)

            if (pdfViewModel.bookProgress?.autoCrop == 0) {
                menu = BaseMenu(selectedColor, 1f, autoCropStr, -1, type_crop)
                menus.add(menu)
            } else {
                menu = BaseMenu(color, 1f, autoCropStr, -1, type_crop)
                menus.add(menu)
            }
        } else {    //文本重排时,自动切边不生效
            menu = BaseMenu(selectedColor, 1f, reflowStr, -1, type_reflow)
            menus.add(menu)
            menu = BaseMenu(color, 1f, autoCropStr, -1, type_crop)
            menus.add(menu)
        }
        //addMenu("字体", color, menus)

        menu = BaseMenu(color, 1f, outlineStr, -1, type_outline)
        menus.add(menu)
        menu = BaseMenu(color, 1f, seekStr, -1, type_seek)
        menus.add(menu)
        menu = BaseMenu(color, 1f, scrollOriStr, -1, type_scroll_ori)
        menus.add(menu)
        menu = BaseMenu(color, 1f, exitStr, -1, type_exit)
        menus.add(menu)
    }

    private lateinit var cakeView: CakeView

    private fun initStr() {
        reflowStr = resources.getString(R.string.cake_menu_reflow)
        ocrStr = resources.getString(R.string.cake_menu_ocr)
        autoCropStr = resources.getString(R.string.cake_menu_auto_crop)
        outlineStr = resources.getString(R.string.cake_menu_ourline)
        seekStr = resources.getString(R.string.cake_menu_seek)
        scrollOriStr = resources.getString(R.string.cake_menu_scroll_ori)
        exitStr = resources.getString(R.string.cake_menu_exit)
    }

    override fun initView() {
        super.initView()
        initStr()
        MupdfDocument.useNewCropper = PdfOptionRepository.getCropper()

        forceCropParam = intent.getIntExtra("forceCropParam", -1)

        mPageSeekBarControls?.updateTitle(mPath)
        mControllerLayout = findViewById(R.id.layout)

        mPageSeekBarControls = createSeekControls()

        val lp = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        mControllerLayout.addView(mPageSeekBarControls, lp)

        mPageSeekBarControls?.autoCropButton!!.visibility = View.VISIBLE

        cakeView = findViewById(R.id.cakeView)
        cakeView.setCakeData(menus)
        cakeView.viewOnclickListener = object : CakeView.ViewOnclickListener {
            override fun onViewClick(v: View?, position: Int) {
                Logcat.d(TAG, "click:pos:$position, ${menus[position]}")
                cakeClick(position)
            }

            override fun onViewCenterClick() {
            }

        }

        mContentView = findViewById(R.id.content)
        mDocumentView = findViewById(R.id.document_view)

        initTouchParams()
    }

    private fun cakeClick(position: Int) {
        val menu = menus[position]
        when (menu.type) {
            type_reflow -> toggleReflow()
            type_ocr -> ocr()
            type_outline -> {
                mPageSeekBarControls?.show()
                showOutline()
            }

            type_seek -> mPageSeekBarControls?.show()
            type_scroll_ori -> {
                val ori = pdfViewModel.bookProgress?.scrollOrientation ?: 1
                val result = if (ori == LinearLayout.VERTICAL) {
                    LinearLayout.HORIZONTAL
                } else {
                    LinearLayout.VERTICAL
                }
                changeOri(result)
            }

            type_crop -> toggleCrop()
            type_exit -> finish()
        }
        cakeView.visibility = View.GONE
    }

    private fun ocr() {
        if (mReflow) {
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
                if (cakeView.visibility == View.VISIBLE) {
                    cakeView.visibility = View.GONE
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

    private fun changeViewMode(pos: Int) {
        viewController?.onDestroy()

        val aViewController = ViewControllerFactory.getOrCreateViewController(
            viewControllerCache,
            viewMode,
            this@AMuPDFRecyclerViewActivity,
            mControllerLayout, pdfViewModel, mPath!!,
            mPageSeekBarControls!!,
            gestureDetector
        )
        viewController = aViewController
        Logcat.d("changeViewMode:$viewMode, pos:$pos,forceCropParam: $forceCropParam, controller:$viewController")
        if (forceCropParam > -1) {
            viewController?.setCrop(forceCropParam == 1)
        }

        addDocumentView()
        viewController?.init(mPageSizes, pos, pdfViewModel.bookProgress?.scrollOrientation ?: 1)
        viewController?.notifyDataSetChanged()
    }

    override fun loadDoc(password: String?) {
        lifecycleScope.launch {
            val ocr = PdfOptionRepository.getImageOcr()
            if (IntentFile.isText(mPath)) {
                //TextActivity.start(this@AMuPDFRecyclerViewActivity, mPath!!)
                //finish()
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
            //progressDialog.setMessage("Loading menu")

            setCropButton(mCrop)

            val pos = pdfViewModel.getCurrentPage(pdfViewModel.countPages())
            Logcat.d("doLoadDoc:mCrop:$mCrop,mReflow:$mReflow, pos:$pos")
            viewController?.doLoadDoc(mPageSizes, pos)

            mPageSeekBarControls?.showReflow(true)
            mPageSeekBarControls?.orientation = pdfViewModel.bookProgress?.scrollOrientation ?: 1

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
            //progressDialog.dismiss()
        }
    }

    private fun setupOutline(currentPos: Int?) {
        if (null == outlineFragment) {
            outlineFragment = OutlineFragment()
            val bundle = Bundle()
            if (outlineHelper!!.hasOutline()) {
                //bundle.putSerializable("OUTLINE", outlineHelper?.getOutline())
                bundle.putSerializable("out", outlineHelper?.getOutlineItems())
            }
            bundle.putSerializable("POSITION", currentPos)
            outlineFragment?.arguments = bundle
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.layout_outline, outlineFragment!!)
            .commit()
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
        if (mReflow) {  //如果原来是文本重排模式,则切换为自动切边或普通模式
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
        pdfViewModel.bookProgress?.reflow = if (mReflow) 1 else 0

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

    override fun onSingleTap(): Boolean {
        if (mPageSeekBarControls?.visibility == View.VISIBLE) {
            mPageSeekBarControls?.hide()
            return true
        }
        return false
    }

    override fun onDoubleTap() {
        super.onDoubleTap()
        if (!isDocLoaded) {
            return
        }

        mPageSeekBarControls?.hide()
        //showOutline()
        viewController?.onDoubleTap()

        if (cakeView.visibility == View.VISIBLE) {
            cakeView.visibility = View.GONE
            return
        }
        initMenus()
        cakeView.setCakeData(menus)
        cakeView.visibility = View.VISIBLE
    }

    private fun createSeekControls(): APageSeekBarControls {
        mPageSeekBarControls = APageSeekBarControls(this, object : PageViewPresenter {
            override fun toggleReflow() {
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
                this@AMuPDFRecyclerViewActivity.finish()
                //mPageSeekBarControls?.hide()
            }

            override fun getTitle(): String {
                return mPath!!
            }

            override fun autoCrop() {
                toggleCrop()
            }

            override fun changeOrientation(ori: Int) {
                changeOri(ori)
            }
        })
        return mPageSeekBarControls!!
    }

    private fun changeOri(ori: Int) {
        pdfViewModel.bookProgress?.scrollOrientation = ori
        viewController?.setOrientation(ori)
    }

    private fun showOutline() {
        outlineHelper?.let {
            if (it.hasOutline()) {
                val frameLayout = mPageSeekBarControls?.getLayoutOutline()

                if (frameLayout?.visibility == View.GONE) {
                    frameLayout.visibility = View.VISIBLE
                    //mMenuHelper?.updateSelection(getCurrentPos())
                    outlineFragment?.updateSelection(getCurrentPos())
                } else {
                    frameLayout?.visibility = View.GONE
                }
            } else {
                mPageSeekBarControls?.getLayoutOutline()?.visibility = View.GONE
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
            pdfViewModel.bookProgress?.autoCrop = if (mCrop) 0 else 1
        }
    }

    private fun cropModeSet(crop: Boolean): Boolean {
        if (mReflow) {
            Toast.makeText(
                this,
                getString(R.string.in_reflow_mode),
                Toast.LENGTH_SHORT
            ).show()
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
            pageSeekBarControls: APageSeekBarControls,
            gestureDetector: GestureDetector?,
        ): AViewController {
            val aViewController = viewControllerCache.get(viewMode.ordinal)
            if (null != aViewController) {
                return aViewController
            }
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
            pageSeekBarControls: APageSeekBarControls,
            gestureDetector: GestureDetector?,
        ): AViewController {
            if (viewMode == ViewMode.CROP) {
                return ACropViewController(
                    context,
                    controllerLayout,
                    pdfViewModel,
                    path,
                    pageSeekBarControls,
                    gestureDetector,
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
                )
            }
        }
    }

    enum class ViewMode {
        NORMAL, CROP, REFLOW
    }
}
