package cn.archko.pdf.activities

import android.annotation.SuppressLint
import android.app.Activity.RESULT_FIRST_USER
import android.content.res.Configuration
import android.graphics.Bitmap
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
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
import cn.archko.pdf.core.common.AppExecutors
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.core.widgets.ExtraSpaceLinearLayoutManager
import cn.archko.pdf.core.widgets.ViewerDividerItemDecoration
import cn.archko.pdf.entity.FontBean
import cn.archko.pdf.fragments.FontsFragment
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.viewmodel.PDFViewModel
import cn.archko.pdf.widgets.APageSeekBarControls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import me.jfenn.colorpickerdialog.dialogs.ColorPickerDialog

/**
 * @author: archko 2020/5/15 :12:43
 */
class AReflowViewController(
    private var context: FragmentActivity,
    private val mControllerLayout: RelativeLayout,
    private var pdfViewModel: PDFViewModel,
    private var mPath: String,
    private var mPageSeekBarControls: APageSeekBarControls?,
    private var gestureDetector: GestureDetector?,
) :
    OutlineListener, AViewController {

    private var mStyleControls: View? = null

    private lateinit var mRecyclerView: ARecyclerView
    private var mStyleHelper: StyleHelper? = null
    private val START_PROGRESS = 15
    private lateinit var mPageSizes: List<APage>
    private var scope: CoroutineScope? = null

    init {
        initView()
    }

    private fun initView() {
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
    }

    override fun init(pageSizes: List<APage>, pos: Int, scrollOrientation: Int) {
        try {
            if (scope == null || !scope!!.isActive) {
                scope =
                    CoroutineScope(Job() + AppExecutors.instance.diskIO().asCoroutineDispatcher())
            }
            Logcat.d("init:$this")
            if (null != pdfViewModel.mupdfDocument) {
                this.mPageSizes = pageSizes

                initReflowMode(pos)
            }
            addGesture()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
        }
    }

    override fun doLoadDoc(pageSizes: List<APage>, pos: Int) {
        try {
            Logcat.d("doLoadDoc:$this")
            this.mPageSizes = pageSizes

            initReflowMode(pos)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
        }
    }

    override fun getDocumentView(): View {
        return mRecyclerView
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addGesture() {
        mRecyclerView.setOnTouchListener { v, event ->
            gestureDetector?.onTouchEvent(event)
            false
        }
    }

    private fun initReflowMode(pos: Int) {
        if (null == mStyleHelper) {
            mStyleHelper = StyleHelper(context)
        }
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
            val vto: ViewTreeObserver = mRecyclerView.viewTreeObserver
            vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mRecyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    Logcat.d("onGlobalLayout:$this,pos:$pos")
                    layoutManager!!.scrollToPosition(pos)
                    mRecyclerView.smoothScrollToPosition(pos)
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
            layoutManager.scrollToPositionWithOffset(page - 1, 0)
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

    override fun onSingleTap(e: MotionEvent, margin: Int): Boolean {
        if (tryHyperlink(e)) {
            return true
        }
        val documentView = getDocumentView()
        val height = documentView.height
        val top = height / 4
        val bottom = height * 3 / 4
        if (scrollPage(e.y.toInt(), top, bottom, margin)) {
            return true
        }
        showReflowConfigMenu()
        return true
    }

    override fun onDoubleTap() {
        //if (mMupdfDocument == null) {
        //    return
        //}
        //mPageSeekBarControls?.hide()
        mStyleControls?.visibility = View.GONE
        //if (!mDrawerLayout.isDrawerOpen(mLeftDrawer)) {
        //    mDrawerLayout.openDrawer(mLeftDrawer)
        //} else {
        //    mDrawerLayout.closeDrawer(mLeftDrawer)
        //}
        //showOutline()
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
        if (pdfViewModel.mupdfDocument != null && mPageSeekBarControls?.visibility == View.VISIBLE) {
            mPageSeekBarControls?.updatePageProgress(index)
        }
    }

    override fun notifyDataSetChanged() {
        mRecyclerView.adapter?.notifyDataSetChanged()
    }

    override fun notifyItemChanged(pos: Int) {
        mRecyclerView.adapter?.notifyItemChanged(pos)
    }

    //--------------------------------------

    override fun onResume() {
        mStyleControls?.visibility = View.GONE

        mRecyclerView.postDelayed({ mRecyclerView.adapter?.notifyDataSetChanged() }, 250L)
    }

    override fun onPause() {
        if (null != pdfViewModel.mupdfDocument) {
            pdfViewModel.bookProgress?.run {
                reflow = 1
                val position = getCurrentPos()
                pdfViewModel.saveBookProgress(
                    mPath,
                    pdfViewModel.countPages(),
                    position + 1,
                    pdfViewModel.bookProgress!!.zoomLevel,
                    -1,
                    0
                )
            }
        }
        if (null != mRecyclerView.adapter && mRecyclerView.adapter is MuPDFReflowAdapter) {
            (mRecyclerView.adapter as MuPDFReflowAdapter).clearCacheViews()
        }
    }

    override fun onDestroy() {
        scope?.let {
            scope!!.cancel()
        }
    }

    //===========================================
    override fun showController() {
        mStyleControls?.visibility = View.VISIBLE
    }

    private fun showReflowConfigMenu() {
        if (null == mStyleControls) {
            initStyleControls()
        } else {
            if (mStyleControls?.visibility == View.VISIBLE) {
                mStyleControls?.visibility = View.GONE
            } else {
                showStyleFragment()
            }
        }
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
        mPageSeekBarControls?.hide()
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
        mStyleControls?.visibility = View.VISIBLE

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

    private fun showStyleFragment() {
        mStyleControls?.visibility = View.VISIBLE
    }

}
