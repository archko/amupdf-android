package cn.archko.pdf.activities

import android.annotation.SuppressLint
import android.app.Activity.RESULT_FIRST_USER
import android.app.ProgressDialog
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.awidget.ARecyclerView
import androidx.recyclerview.awidget.LinearLayoutManager
import cn.archko.pdf.R
import cn.archko.pdf.adapters.MuPDFReflowAdapter
import cn.archko.pdf.common.StyleHelper
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.entity.State
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.core.widgets.ExtraSpaceLinearLayoutManager
import cn.archko.pdf.core.widgets.ViewerDividerItemDecoration
import cn.archko.pdf.entity.FontBean
import cn.archko.pdf.fragments.FontsFragment
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.viewmodel.DocViewModel
import cn.archko.pdf.viewmodel.PDFViewModel
import cn.archko.pdf.widgets.PageControls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.jfenn.colorpickerdialog.dialogs.ColorPickerDialog

/**
 * @author: archko 2020/5/15 :12:43
 */
class AReflowViewController(
    private var context: FragmentActivity,
    private var scope: CoroutineScope,
    private val mControllerLayout: RelativeLayout,
    private var docViewModel: DocViewModel,
    private var mPath: String,
    private var pageControls: PageControls?,
    private var controllerListener: ControllerListener?,
) :
    OutlineListener, AViewController {

    private var mStyleControls: View? = null

    private lateinit var mRecyclerView: ARecyclerView
    private var mStyleHelper: StyleHelper? = null
    private val START_PROGRESS = 15
    private var mPageSizes = mutableListOf<APage>()
    private var mGestureDetector: GestureDetector? = null
    protected var progressDialog: ProgressDialog? = null
    private var pdfViewModel = PDFViewModel()

    init {
        initView()
    }

    private inner class MySimpleOnGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            var margin = mRecyclerView.height
            margin = if (margin <= 0) {
                ViewConfiguration.get(context).scaledTouchSlop * 2
            } else {
                (margin * 0.03).toInt()
            }
            if (onSingleTap(e, margin)) {
                return true
            }
            controllerListener?.onSingleTapConfirmed(e, 0)
            return true
        }
    }

    private fun initView() {
        mGestureDetector = GestureDetector(context, MySimpleOnGestureListener())
        mRecyclerView = ARecyclerView(context)//contentView.findViewById(R.id.recycler_view)
        with(mRecyclerView) {
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            isNestedScrollingEnabled = false
            layoutManager = ExtraSpaceLinearLayoutManager(context, LinearLayoutManager.VERTICAL)
            setItemViewCacheSize(0)

            addItemDecoration(ViewerDividerItemDecoration(LinearLayoutManager.VERTICAL))
            addOnScrollListener(object : ARecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: ARecyclerView, newState: Int) {
                    if (newState == ARecyclerView.SCROLL_STATE_IDLE) {
                        updateProgress(getCurrentPos())
                    }
                }

                override fun onScrolled(recyclerView: ARecyclerView, dx: Int, dy: Int) {
                }
            })
        }

        initStyleControls()

        addGesture()
    }

    private fun showPasswordDialog() {
        /*PasswordDialog.show(this@AMuPDFRecyclerViewActivity,
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
            })*/
    }

    private fun loadDocument() {
        progressDialog = ProgressDialog(context)
        progressDialog!!.setMessage("Loading")
        progressDialog!!.show()

        scope.launch {
            val start = SystemClock.uptimeMillis()
            pdfViewModel.loadPdfDoc(context, mPath, null)
            pdfViewModel.pageFlow
                .collectLatest {
                    progressDialog!!.dismiss()
                    if (it.state == State.PASS) {
                        showPasswordDialog()
                        return@collectLatest
                    }
                    val cp = pdfViewModel.countPages()
                    if (cp > 0) {
                        Logcat.d(
                            TAG,
                            "open:" + (SystemClock.uptimeMillis() - start) + " cp:" + cp
                        )

                        postLoadDoc(cp)
                    } else {
                        context.finish()
                    }
                }
        }
    }

    override fun init() {
        loadDocument()
    }

    private fun postLoadDoc(cp: Int) {
        val width = mRecyclerView.width
        var start = SystemClock.uptimeMillis()

        scope.launch {
            docViewModel.preparePageSize(width).collectLatest { pageSizeBean ->
                Logcat.d("open3:" + (SystemClock.uptimeMillis() - start))
                mPageSizes.clear()
                var pageSizes: List<APage>? = null
                if (pageSizeBean != null) {
                    pageSizes = pageSizeBean.List
                }
                if (pageSizes.isNullOrEmpty()) {
                    start = SystemClock.uptimeMillis()
                    preparePageSize(cp)
                    Logcat.d("open2:" + (SystemClock.uptimeMillis() - start))
                } else {
                    Logcat.d("open3:pageSizes>0:" + pageSizes.size)
                    mPageSizes.addAll(pageSizes)
                    //checkPageSize(cp)
                }
                doLoadDoc()
            }
        }
    }

    private fun getPageSize(pageNum: Int): APage? {
        val p = pdfViewModel.loadPage(pageNum) ?: return null

        //Logcat.d(TAG, "open:getPageSize.$pageNum page:$p")
        val b = p.bounds
        val w = b.x1 - b.x0
        val h = b.y1 - b.y0
        p.destroy()
        return APage(pageNum, w, h, 1.0f/*zoomModel!!.zoom*/)
    }

    private fun preparePageSize(cp: Int) {
        for (i in 0 until cp) {
            val pointF = getPageSize(i)
            if (pointF != null) {
                mPageSizes.add(pointF)
            }
        }
    }

    private fun doLoadDoc() {
        initReflowMode(docViewModel.getCurrentPage())

        controllerListener?.doLoadedDoc(
            pdfViewModel.countPages(),
            docViewModel.getCurrentPage(),
            pdfViewModel.links
        )
    }

    override fun getDocumentView(): View {
        return mRecyclerView
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addGesture() {
        mRecyclerView.setOnTouchListener { v, event ->
            mGestureDetector?.onTouchEvent(event) == true
        }
    }

    private fun initReflowMode(pos: Int) {
        if (null == mRecyclerView.adapter) {
            mRecyclerView.adapter = MuPDFReflowAdapter(
                context,
                pdfViewModel.mupdfDocument,
                mStyleHelper,
                scope,
                pdfViewModel
            )
        } else {
            (mRecyclerView.adapter as MuPDFReflowAdapter).setScope(scope)
        }

        if (pos > 0) {
            val layoutManager = mRecyclerView.layoutManager
            layoutManager!!.scrollToPosition(pos)
            val vto: ViewTreeObserver = mRecyclerView.viewTreeObserver
            vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mRecyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    Logcat.d("onGlobalLayout:$this,pos:$pos")
                    layoutManager.scrollToPosition(pos)
                }
            })
        }
    }

    override fun getCurrentBitmap(): Bitmap? {
        return null
    }

    override fun getCurrentPos(): Int {
        if (null == mRecyclerView.layoutManager) {
            return 0
        }
        var position =
            (mRecyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        if (position < 0) {
            position = 0
        }
        return position
    }

    fun getLastPos(): Int {
        if (null == mRecyclerView.layoutManager) {
            return 0
        }
        var position =
            (mRecyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
        if (position < 0) {
            position = 0
        }
        return position
    }

    override fun getCount(): Int {
        return mPageSizes.size
    }

    override fun setOrientation(ori: Int) {
    }

    override fun setCrop(crop: Boolean) {
    }

    override fun getCrop(): Boolean {
        return false
    }

    override fun scrollToPosition(page: Int) {
        mRecyclerView.layoutManager?.run {
            val layoutManager: LinearLayoutManager = this as LinearLayoutManager
            layoutManager.scrollToPositionWithOffset(page, 0)
        }
    }

    override fun scrollPage(y: Int, top: Int, bottom: Int, margin: Int): Boolean {
        if (y < top) {
            var scrollY = mRecyclerView.scrollY
            scrollY -= mRecyclerView.height
            mRecyclerView.scrollBy(0, scrollY + margin)
            return true
        } else if (y > bottom) {
            var scrollY = mRecyclerView.scrollY
            scrollY += mRecyclerView.height
            mRecyclerView.scrollBy(0, scrollY - margin)
            return true
        }
        return false
    }

    override fun tryHyperlink(ev: MotionEvent): Boolean {
        return false
    }

    override fun onSingleTap(ev: MotionEvent?, margin: Int): Boolean {
        if (ev == null) {
            return false
        }
        val documentView = getDocumentView()
        val height = documentView.height
        val top = height / 4
        val bottom = height * 3 / 4
        if (scrollPage(ev.y.toInt(), top, bottom, margin)) {
            return true
        }

        return false
    }

    override fun onDoubleTap() {
    }

    override fun onSelectedOutline(index: Int) {
        mRecyclerView.layoutManager?.scrollToPosition(index - RESULT_FIRST_USER)
        updateProgress(index - RESULT_FIRST_USER)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        mRecyclerView.stopScroll()
        mRecyclerView.adapter?.notifyDataSetChanged()
    }

    fun updateProgress(index: Int) {
        if (pdfViewModel.mupdfDocument != null && pageControls?.visibility() == View.VISIBLE) {
            pageControls?.updatePageProgress(index)
        }
    }

    override fun notifyDataSetChanged() {
        mRecyclerView.adapter?.notifyDataSetChanged()
    }

    override fun notifyItemChanged(pos: Int) {
        mRecyclerView.adapter?.notifyItemChanged(pos)
    }

    override fun setFilter(colorMode: Int) {
    }

    override fun decodePageForTts(currentPos: Int) {
        //pdfViewModel.decodeTextForTts(currentPos)
    }

    //--------------------------------------

    override fun onResume() {
        mRecyclerView.postDelayed({ mRecyclerView.adapter?.notifyDataSetChanged() }, 250L)
    }

    override fun onPause() {
        if (null != pdfViewModel.mupdfDocument && null != docViewModel.bookProgress) {
            docViewModel.bookProgress!!.reflow = 1
            var savePos = getCurrentPos() + 1
            val lastPos = getLastPos()
            if (lastPos == mPageSizes.size - 1) {
                savePos = lastPos
            }
            docViewModel.saveBookProgress(
                mPath,
                mPageSizes.size,
                savePos,
                docViewModel.bookProgress!!.zoomLevel,
                -1,
                0
            )
        }
        if (null != mRecyclerView.adapter && mRecyclerView.adapter is MuPDFReflowAdapter) {
            (mRecyclerView.adapter as MuPDFReflowAdapter).clearCacheViews()
        }
    }

    override fun onDestroy() {
        pdfViewModel.destroy()
    }

    //===========================================
    override fun showController() {
    }

    private var fontSeekBar: SeekBar? = null
    private var fontSizeLabel: TextView? = null
    private var fontFaceSelected: TextView? = null
    private var lineSpaceLabel: TextView? = null
    private var colorLabel: TextView? = null
    private var fontFaceChange: View? = null
    private var linespaceMinus: View? = null
    private var linespacePlus: View? = null
    private var bgSetting: View? = null
    private var fgSetting: View? = null

    private fun initStyleControls() {
        if (null == mStyleHelper) {
            mStyleHelper = StyleHelper(context)
        }
        pageControls?.hide()
        if (null == mStyleControls) {
            mStyleControls = LayoutInflater.from(context).inflate(R.layout.text_style, null, false)
            fontSeekBar = mStyleControls!!.findViewById(R.id.font_seek_bar)
            fontSizeLabel = mStyleControls!!.findViewById(R.id.font_size_label)
            fontFaceSelected = mStyleControls!!.findViewById(R.id.font_face_selected)
            lineSpaceLabel = mStyleControls!!.findViewById(R.id.line_space_label)
            colorLabel = mStyleControls!!.findViewById(R.id.color_label)
            fontFaceChange = mStyleControls!!.findViewById(R.id.font_face_change)
            linespaceMinus = mStyleControls!!.findViewById(R.id.linespace_minus)
            linespacePlus = mStyleControls!!.findViewById(R.id.linespace_plus)
            bgSetting = mStyleControls!!.findViewById(R.id.bg_setting)
            fgSetting = mStyleControls!!.findViewById(R.id.fg_setting)

            val lp = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            mControllerLayout.addView(mStyleControls, lp)
        }

        mStyleHelper?.let {
            val progress = (it.styleBean?.textSize!! - START_PROGRESS).toInt()
            fontSeekBar?.progress = progress
            fontSizeLabel?.text = String.format("%s", progress + START_PROGRESS)
            fontSeekBar?.max = 10
            fontSeekBar?.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val index = (progress + START_PROGRESS)
                    fontSizeLabel?.text = String.format("%s", index)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    it.styleBean?.textSize = (seekBar?.progress!! + START_PROGRESS).toFloat()
                    it.saveStyleToSP(it.styleBean)
                    updateReflowAdapter()
                }
            })
            fontFaceSelected?.text = it.fontHelper?.fontBean?.fontName

            lineSpaceLabel?.text = String.format("%s倍", it.styleBean?.lineSpacingMult)
            colorLabel?.setBackgroundColor(it.styleBean?.bgColor!!)
            colorLabel?.setTextColor(it.styleBean?.fgColor!!)
        }

        fontFaceChange?.setOnClickListener {
            FontsFragment.showFontsDialog(
                context, mStyleHelper,
                object : DataListener {
                    override fun onSuccess(vararg args: Any?) {
                        updateReflowAdapter()
                        val fBean = args[0] as FontBean
                        fontFaceSelected?.text = fBean.fontName
                    }

                    override fun onFailed(vararg args: Any?) {
                    }
                })
        }

        linespaceMinus?.setOnClickListener {
            var old = mStyleHelper?.styleBean?.lineSpacingMult
            if (old!! < 0.8f) {
                return@setOnClickListener
            }
            old = old.minus(0.1f)
            applyLineSpace(old)
        }
        linespacePlus?.setOnClickListener {
            var old = mStyleHelper?.styleBean?.lineSpacingMult
            if (old!! > 2.2f) {
                return@setOnClickListener
            }
            old = old?.plus(0.1f)
            applyLineSpace(old)
        }
        bgSetting?.setOnClickListener {
            ColorPickerDialog()
                .withColor(mStyleHelper?.styleBean?.bgColor!!)
                .withListener { _, color ->
                    colorLabel?.setBackgroundColor(color)
                    mStyleHelper?.styleBean?.bgColor = color
                    mStyleHelper?.saveStyleToSP(mStyleHelper?.styleBean)
                    updateReflowAdapter()
                }
                .show(context.supportFragmentManager, "colorPicker")
        }
        fgSetting?.setOnClickListener {
            ColorPickerDialog()
                .withColor(mStyleHelper?.styleBean?.fgColor!!)
                .withListener { _, color ->
                    colorLabel?.setTextColor(color)
                    mStyleHelper?.styleBean?.fgColor = color
                    mStyleHelper?.saveStyleToSP(mStyleHelper?.styleBean)
                    updateReflowAdapter()
                }
                .show(context.supportFragmentManager, "colorPicker")
        }
    }

    private fun updateReflowAdapter() {
        mRecyclerView.adapter?.run {
            this.notifyDataSetChanged()
        }
    }

    private fun applyLineSpace(old: Float?) {
        lineSpaceLabel?.text = String.format("%s倍", old)
        mStyleHelper?.styleBean?.lineSpacingMult = old!!
        mStyleHelper?.saveStyleToSP(mStyleHelper?.styleBean)
        updateReflowAdapter()
    }

    companion object {

        private const val TAG = "ReflowView"
    }
}
