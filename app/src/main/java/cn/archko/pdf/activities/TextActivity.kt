package cn.archko.pdf.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.R
import cn.archko.pdf.adapters.MuPDFTextAdapter
import cn.archko.pdf.common.Event
import cn.archko.pdf.common.Graph
import cn.archko.pdf.common.IntentFile
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.common.SensorHelper
import cn.archko.pdf.common.StyleHelper
import cn.archko.pdf.common.TextHelper
import cn.archko.pdf.entity.FontBean
import cn.archko.pdf.fragments.FontsFragment
import cn.archko.pdf.listeners.DataListener
import cn.archko.pdf.utils.StatusBarHelper
import cn.archko.pdf.utils.Utils
import cn.archko.pdf.viewmodel.PDFViewModel
import cn.archko.pdf.widgets.ViewerDividerItemDecoration
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.launch
import me.jfenn.colorpickerdialog.dialogs.ColorPickerDialog


/**
 * @author: archko 2022/7/11 :9:49 上午
 */
class TextActivity : AppCompatActivity() {

    private var path: String? = null
    private var mUri: Uri? = null
    private var sensorHelper: SensorHelper? = null
    private val preferencesRepository = PdfOptionRepository(Graph.dataStore)
    protected val pdfViewModel: PDFViewModel = PDFViewModel()

    private var mStyleControls: View? = null
    private var mControllerLayout: RelativeLayout? = null
    private var recyclerView: RecyclerView? = null

    private var mStyleHelper: StyleHelper? = null
    private var adapter: MuPDFTextAdapter? = null
    protected var pageNumberToast: Toast? = null

    private var header: View? = null
    private var footer: View? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (null != savedInstanceState) {
            path = savedInstanceState.getString("path", null)
        }
        sensorHelper = SensorHelper(this)
        initIntent()

        if (TextUtils.isEmpty(path)) {
            return
        }

        StatusBarHelper.hideSystemUI(this)

        mStyleHelper = StyleHelper(this, preferencesRepository)

        var margin = window.decorView.height
        if (margin <= 0) {
            margin = ViewConfiguration.get(this).scaledTouchSlop * 2
        } else {
            margin = (margin * 0.03).toInt()
        }
        val finalMargin = margin
        val gestureDetector = GestureDetector(this, object :
            GestureDetector.SimpleOnGestureListener() {

            override fun onDoubleTap(e: MotionEvent): Boolean {
                showReflowConfigMenu()
                return super.onDoubleTap(e)
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val documentView = recyclerView!!
                val top = documentView.height / 4
                val bottom = documentView.height * 3 / 4

                val rs: Boolean = scrollPage(e.y.toInt(), top, bottom, finalMargin)
                if (!rs) {
                    onSingleTap()
                }
                return true
            }

            private fun onSingleTap() {
                val pos = getCurrentPos()
                val pageText = (pos + 1).toString() + "/" + (adapter?.itemCount)
                if (pageNumberToast != null) {
                    pageNumberToast!!.setText(pageText)
                } else {
                    pageNumberToast =
                        Toast.makeText(this@TextActivity, pageText, Toast.LENGTH_SHORT)
                }
                pageNumberToast!!.setGravity(
                    Gravity.BOTTOM or Gravity.START,
                    Utils.dipToPixel(15f),
                    0
                )
                pageNumberToast!!.show()
            }
        })

        recyclerView = RecyclerView(this)
        mControllerLayout = RelativeLayout(this)
        mControllerLayout!!.addView(recyclerView)
        setContentView(mControllerLayout)

        recyclerView?.run {
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            //setItemViewCacheSize(0)

            addItemDecoration(ViewerDividerItemDecoration(context, LinearLayoutManager.VERTICAL))
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    }
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                }
            })

            setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                false
            }
        }

        adapter = MuPDFTextAdapter(this, mStyleHelper)
        header = View(this)
        header!!.minimumHeight = Utils.dipToPixel(TextHelper.HEADER_HEIGHT)
        adapter!!.addHeaderView(header)
        footer = View(this)
        footer!!.minimumHeight = Utils.dipToPixel(TextHelper.HEADER_HEIGHT)
        adapter!!.addFootView(footer)

        updateHeaderFooterBg()

        recyclerView?.adapter = adapter

        lifecycleScope.launch {
            pdfViewModel.loadTextDoc(path!!)
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                pdfViewModel.textFlow.collect {
                    adapter?.data = it.list
                    adapter?.notifyDataSetChanged()
                    loadBookmark()
                }
            }
        }
    }

    fun scrollPage(y: Int, top: Int, bottom: Int, margin: Int): Boolean {
        if (y < top) {
            var scrollY = recyclerView!!.scrollY
            scrollY -= recyclerView!!.height
            recyclerView!!.scrollBy(0, scrollY + margin)
            return true
        } else if (y > bottom) {
            var scrollY = recyclerView!!.scrollY
            scrollY += recyclerView!!.height
            recyclerView!!.scrollBy(0, scrollY - margin)
            return true
        }
        return false
    }

    private fun loadBookmark() {
        lifecycleScope.launch {
            val bookProgress =
                path!!.run { pdfViewModel.loadBookProgressByPath(this, preferencesRepository) }
            bookProgress?.page?.let { scrollToPosition(it) }
        }
    }

    private fun scrollToPosition(page: Int) {
        recyclerView?.layoutManager?.run {
            val layoutManager: LinearLayoutManager = this as LinearLayoutManager
            layoutManager.scrollToPositionWithOffset(page, 0)
        }
    }

    override fun onPause() {
        super.onPause()
        //sensorHelper.onPause();
        pdfViewModel.bookProgress?.run {
            val position = getCurrentPos()
            pdfViewModel.saveBookProgress(
                path,
                adapter?.itemCount?.minus(2) ?: 1,
                position,
                pdfViewModel.bookProgress!!.zoomLevel,
                -1,
                0
            )
        }
    }

    fun getCurrentPos(): Int {
        if (null == recyclerView?.layoutManager) {
            return 0
        }
        var position =
            (recyclerView!!.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        if (position < 0) {
            position = 0
        }
        return position
    }

    override fun onResume() {
        super.onResume()
        //sensorHelper.onResume();
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter?.clearCacheViews()
        LiveEventBus
            .get(Event.ACTION_STOPPED)
            .post(path)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    private fun initIntent() {
        if (!TextUtils.isEmpty(path)) {
            return
        }

        path = IntentFile.processIntentAction(intent, this@TextActivity)
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

    private fun showStyleFragment() {
        mStyleControls?.visibility = View.VISIBLE
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
        if (null == mStyleControls) {
            mStyleControls = LayoutInflater.from(this).inflate(R.layout.text_style, null, false)
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
            mControllerLayout?.addView(mStyleControls, lp)
        }
        mStyleControls?.visibility = View.VISIBLE

        mStyleHelper?.let {
            val progress = (it.styleBean?.textSize!! - START_PROGRESS).toInt()
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
            updateHeaderFooterBg()
        }

        fontFaceChange?.setOnClickListener {
            FontsFragment.showFontsDialog(
                this as FragmentActivity, mStyleHelper,
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
                    updateHeaderFooterBg()
                }
                .show(supportFragmentManager, "colorPicker")
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
                .show(supportFragmentManager, "colorPicker")
        }
    }

    private fun updateHeaderFooterBg() {
        header?.setBackgroundColor(mStyleHelper?.styleBean?.bgColor!!)
        footer?.setBackgroundColor(mStyleHelper?.styleBean?.bgColor!!)
    }

    private fun updateReflowAdapter() {
        recyclerView?.adapter?.run {
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

        private const val START_PROGRESS = 15
        fun start(context: Context, path: String) {
            val intent = Intent(context, TextActivity::class.java)
            intent.putExtra("path", path)
            context.startActivity(intent)
        }
    }
}