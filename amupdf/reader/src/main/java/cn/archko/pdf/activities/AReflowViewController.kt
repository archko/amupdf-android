package cn.archko.pdf.activities

import android.annotation.SuppressLint
import android.app.Activity.RESULT_FIRST_USER
import android.content.Context
import android.content.res.Configuration
import android.util.SparseArray
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.R
import cn.archko.pdf.adapters.MuPDFReflowAdapter
import cn.archko.pdf.colorpicker.ColorPickerDialog
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.PDFBookmarkManager
import cn.archko.pdf.common.StyleHelper
import cn.archko.pdf.entity.APage
import cn.archko.pdf.entity.FontBean
import cn.archko.pdf.fragments.FontsFragment
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.DataListener
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.widgets.APageSeekBarControls
import cn.archko.pdf.widgets.ViewerDividerItemDecoration

/**
 * @author: archko 2020/5/15 :12:43
 */
class AReflowViewController(
    private var context: Context,
    private var contentView: View,
    private val mControllerLayout: RelativeLayout,
    private var pdfBookmarkManager: PDFBookmarkManager,
    private var mPath: String,
    private var mPageSeekBarControls: APageSeekBarControls?,
    private var gestureDetector: GestureDetector?
) :
    OutlineListener, AViewController {


    private var mStyleControls: View? = null

    private var mFontSeekBar: SeekBar? = null
    private var mFontSizeLabel: TextView? = null
    private var mFontFaceSelected: TextView? = null
    private var mFontFaceChange: TextView? = null
    private var mLineSpaceLabel: TextView? = null
    private var mLinespaceMinus: View? = null
    private var mLinespacePlus: View? = null
    private var mColorLabel: TextView? = null
    private var mBgSetting: View? = null
    private var mFgSetting: View? = null
    private var colorPickerDialog: ColorPickerDialog? = null

    private lateinit var mRecyclerView: RecyclerView
    private var mStyleHelper: StyleHelper? = null
    private var mMupdfDocument: MupdfDocument? = null
    private val START_PROGRESS = 15
    private lateinit var mPageSizes: SparseArray<APage>

    init {
        initView()
    }

    private fun initView() {
        mRecyclerView = RecyclerView(context)//contentView.findViewById(R.id.recycler_view)
        with(mRecyclerView) {
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            setItemViewCacheSize(0)

            addItemDecoration(ViewerDividerItemDecoration(context, LinearLayoutManager.VERTICAL))
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        updateProgress(getCurrentPos())
                    }
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                }
            })
        }
    }

    override fun init(pageSizes: SparseArray<APage>, mupdfDocument: MupdfDocument?, pos: Int) {
        try {
            Logcat.d("init:$this")
            if (null != mupdfDocument) {
                this.mPageSizes = pageSizes
                this.mMupdfDocument = mupdfDocument

                setReflowMode(pos)
            }
            addGesture()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
        }
    }

    override fun doLoadDoc(pageSizes: SparseArray<APage>, mupdfDocument: MupdfDocument, pos: Int) {
        try {
            Logcat.d("doLoadDoc:$this")
            this.mPageSizes = pageSizes
            this.mMupdfDocument = mupdfDocument

            setReflowMode(pos)
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

    private fun setReflowMode(pos: Int) {
        if (null == mStyleHelper) {
            mStyleHelper = StyleHelper()
        }
        if (null == mRecyclerView.adapter) {
            mRecyclerView.adapter = MuPDFReflowAdapter(context, mMupdfDocument, mStyleHelper)
        }

        if (pos > 0) {
            mRecyclerView.scrollToPosition(pos)
        }
    }

    override fun getCurrentPos(): Int {
        var position =
            (mRecyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        if (position < 0) {
            position = 0
        }
        return position
    }

    override fun scrollToPosition(page: Int) {
        mRecyclerView.layoutManager?.scrollToPosition(page)
    }

    override fun onSingleTap() {
        //if (mPageSeekBarControls?.visibility == View.VISIBLE) {
        //    mPageSeekBarControls?.hide()
        //    return
        //}
        showReflowConfigMenu()
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
        if (mMupdfDocument != null && mPageSeekBarControls?.visibility == View.VISIBLE) {
            mPageSeekBarControls?.updatePageProgress(index)
        }
    }

    override fun notifyDataSetChanged() {
        mRecyclerView.adapter?.notifyDataSetChanged()
    }

    //--------------------------------------

    override fun onResume() {
        //mPageSeekBarControls?.hide()
        mStyleControls?.visibility = View.GONE

        mRecyclerView.postDelayed(object : Runnable {
            override fun run() {
                mRecyclerView.adapter?.notifyDataSetChanged()
            }
        }, 250L)
    }

    override fun onPause() {
        pdfBookmarkManager.bookmarkToRestore?.reflow = 1
        val position = getCurrentPos()
        val zoomLevel = pdfBookmarkManager.bookmarkToRestore!!.zoomLevel;
        pdfBookmarkManager.saveCurrentPage(
            mPath,
            mMupdfDocument!!.countPages(),
            position,
            zoomLevel,
            -1,
            0
        )
        if (null != mRecyclerView.adapter && mRecyclerView.adapter is MuPDFReflowAdapter) {
            (mRecyclerView.adapter as MuPDFReflowAdapter).clearCacheViews()
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

    private fun initStyleControls() {
        mPageSeekBarControls?.hide()
        if (null == mStyleControls) {
            mStyleControls = LayoutInflater.from(context).inflate(R.layout.text_style, null, false)

            val lp = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            mControllerLayout.addView(mStyleControls, lp)
        }
        mStyleControls?.visibility = View.VISIBLE

        mFontSeekBar = mStyleControls?.findViewById(R.id.font_seek_bar)
        mFontSizeLabel = mStyleControls?.findViewById(R.id.font_size_label)
        mFontFaceSelected = mStyleControls?.findViewById(R.id.font_face_selected)
        mFontFaceChange = mStyleControls?.findViewById(R.id.font_face_change)
        mLineSpaceLabel = mStyleControls?.findViewById(R.id.line_space_label)
        mLinespaceMinus = mStyleControls?.findViewById(R.id.linespace_minus)
        mLinespacePlus = mStyleControls?.findViewById(R.id.linespace_plus)
        mColorLabel = mStyleControls?.findViewById(R.id.color_label)
        mBgSetting = mStyleControls?.findViewById(R.id.bg_setting)
        mFgSetting = mStyleControls?.findViewById(R.id.fg_setting)

        mStyleHelper?.let {
            val progress = (it.styleBean?.textSize!! - START_PROGRESS).toInt()
            mFontSeekBar?.progress = progress
            mFontSizeLabel?.text = String.format("%s", progress + START_PROGRESS)
            mFontSeekBar?.max = 10
            mFontSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val index = (progress + START_PROGRESS)
                    mFontSizeLabel?.text = String.format("%s", index)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    it.styleBean?.textSize = (seekBar?.progress!! + START_PROGRESS).toFloat()
                    it.saveStyleToSP(it.styleBean)
                    updateReflowAdapter()
                }
            })
            mFontFaceSelected?.text = it.fontHelper?.fontBean?.fontName

            mLineSpaceLabel?.text = String.format("%s倍", it.styleBean?.lineSpacingMult)
            mColorLabel?.setBackgroundColor(it.styleBean?.bgColor!!)
            mColorLabel?.setTextColor(it.styleBean?.fgColor!!)
        }

        mFontFaceChange?.setOnClickListener {
            FontsFragment.showFontsDialog(context as FragmentActivity, mStyleHelper,
                object : DataListener {
                    override fun onSuccess(vararg args: Any?) {
                        updateReflowAdapter()
                        val fBean = args[0] as FontBean
                        mFontFaceSelected?.text = fBean.fontName
                    }

                    override fun onFailed(vararg args: Any?) {
                    }
                })
        }

        mLinespaceMinus?.setOnClickListener {
            var old = mStyleHelper?.styleBean?.lineSpacingMult
            if (old!! < 0.8f) {
                return@setOnClickListener
            }
            old = old.minus(0.1f)
            applyLineSpace(old)
        }
        mLinespacePlus?.setOnClickListener {
            var old = mStyleHelper?.styleBean?.lineSpacingMult
            if (old!! > 2.2f) {
                return@setOnClickListener
            }
            old = old?.plus(0.1f)
            applyLineSpace(old)
        }
        mBgSetting?.setOnClickListener {
            pickerColor(
                mStyleHelper?.styleBean?.bgColor!!,
                ColorPickerDialog.OnColorSelectedListener { color ->
                    mColorLabel?.setBackgroundColor(color)
                    mStyleHelper?.styleBean?.bgColor = color
                    mStyleHelper?.saveStyleToSP(mStyleHelper?.styleBean)
                    updateReflowAdapter()
                })
        }
        mFgSetting?.setOnClickListener {
            pickerColor(
                mStyleHelper?.styleBean?.fgColor!!,
                ColorPickerDialog.OnColorSelectedListener { color ->
                    mColorLabel?.setTextColor(color)
                    mStyleHelper?.styleBean?.fgColor = color
                    mStyleHelper?.saveStyleToSP(mStyleHelper?.styleBean)
                    updateReflowAdapter()
                })
        }
    }

    private fun updateReflowAdapter() {
        mRecyclerView.adapter?.run {
            this.notifyDataSetChanged()
        }
    }

    private fun applyLineSpace(old: Float?) {
        mLineSpaceLabel?.text = String.format("%s倍", old)
        mStyleHelper?.styleBean?.lineSpacingMult = old!!
        mStyleHelper?.saveStyleToSP(mStyleHelper?.styleBean)
        updateReflowAdapter()
    }

    private fun pickerColor(
        initialColor: Int,
        selectedListener: ColorPickerDialog.OnColorSelectedListener
    ) {
        if (null == colorPickerDialog) {
            colorPickerDialog = ColorPickerDialog(context, initialColor, selectedListener)
        } else {
            colorPickerDialog?.updateColor(initialColor)
            colorPickerDialog?.setOnColorSelectedListener(selectedListener)
        }
        colorPickerDialog?.show()
    }

    private fun showStyleFragment() {
        mStyleControls?.visibility = View.VISIBLE
    }

}
