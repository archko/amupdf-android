package cn.archko.pdf.controller

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.awidget.ARecyclerView
import androidx.recyclerview.awidget.LinearLayoutManager
import cn.archko.pdf.R
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.core.cache.BitmapCache
import cn.archko.pdf.core.cache.ReflowViewCache
import cn.archko.pdf.core.common.APageSizeLoader
import cn.archko.pdf.core.common.AppExecutors.Companion.instance
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.entity.BookProgress
import cn.archko.pdf.core.utils.ColorUtil.getColorMode
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.core.widgets.ExtraSpaceLinearLayoutManager
import cn.archko.pdf.core.widgets.ViewerDividerItemDecoration
import cn.archko.pdf.decode.DocDecodeService
import cn.archko.pdf.decode.DocDecodeService.IView
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.viewmodel.DocViewModel
import cn.archko.pdf.widgets.PdfRecyclerView
import com.google.android.material.slider.Slider
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import org.vudroid.core.DecodeService
import org.vudroid.core.DecodeServiceBase
import org.vudroid.core.codec.CodecDocument
import org.vudroid.core.models.ZoomModel

/**
 * 扫描版的重排,用k2pdfopt库
 * 重排比较麻烦,一是要调整合适的高宽,二是页码进度不好控制.
 * 一个页面重排后会分为几个页面,需要重新计算页码与大纲对应关系,这个对应关系比较糟糕
 * 重排需要先对图片解码,然后再重新分割组合,所以暂时只支持pdf,其它格式按原来的重排规则.
 * 重排模式,不支持手势放大缩小,大纲也不支持或者大纲只支持原始页面,不支持重排后的页面
 *
 * @author: archko 2024/8/25 :12:43
 */
class AScanReflowViewController(
    private var context: FragmentActivity,
    private val scope: CoroutineScope,
    private val mControllerLayout: RelativeLayout,
    private var docViewModel: DocViewModel,
    private var mPath: String,
    private var pageController: IPageController?,
    private var controllerListener: ControllerListener?,
) :
    OutlineListener, AViewController {

    private lateinit var mRecyclerView: PdfRecyclerView
    private lateinit var mPageSizes: List<APage>
    protected var mStyleControls: View? = null
    private var fontSlider: Slider? = null
    private var scaleSlider: Slider? = null

    private fun getFontSize(): Float {
        val mmkv = MMKV.mmkvWithID("scan")
        return mmkv.decodeFloat("font", 1f)
    }

    private fun setFontSize(size: Float) {
        val mmkv = MMKV.mmkvWithID("scan")
        Logcat.d("font:$size")
        mmkv.encode("font", size)
    }

    private fun getPageScale(): Float {
        val mmkv = MMKV.mmkvWithID("scan")
        return mmkv.decodeFloat("scale", 1.5f)
    }

    private fun setPageScale(size: Float) {
        val mmkv = MMKV.mmkvWithID("scan")
        Logcat.d("scale:$size")
        mmkv.encode("scale", size)
    }

    private fun initStyleControls() {
        pageController?.hide()
        if (null == mStyleControls) {
            mStyleControls = LayoutInflater.from(context).inflate(R.layout.scan_style, null, false)
            fontSlider = mStyleControls!!.findViewById(R.id.font_slider)
            fontSlider?.apply {
                valueFrom = 0.5f
                valueTo = 2.4f
                value = getFontSize()
            }
            scaleSlider = mStyleControls!!.findViewById(R.id.scale_slider)
            scaleSlider?.apply {
                valueFrom = 1.0f
                valueTo = 2.5f
                value = getPageScale()
            }

            val lp = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            mControllerLayout.addView(mStyleControls, lp)
        }
        fontSlider?.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
            }

            override fun onStopTrackingTouch(slider: Slider) {
                applyFontSize(slider.value)
            }
        })
        scaleSlider?.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
            }

            override fun onStopTrackingTouch(slider: Slider) {
                applyPageScale(slider.value)
            }
        })
    }

    private fun applyFontSize(old: Float) {
        setFontSize(old)
        notifyDataSetChanged()
    }

    private fun applyPageScale(old: Float) {
        setPageScale(old)
        notifyDataSetChanged()
    }

    private var crop: Boolean = false
    private var scrollOrientation = LinearLayoutManager.VERTICAL

    private var decodeService: DocDecodeService? = null

    protected var isDocLoaded: Boolean = false
    private var document: CodecDocument? = null
    private var widthHeightMap = HashMap<String, Int?>()
    private var filter: ColorFilter? = null

    private inner class MySimpleOnGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            controllerListener?.onSingleTapConfirmed(e, 0)
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            return controllerListener?.onDoubleTap(e, 0) ?: super.onDoubleTap(e)
        }
    }

    //解码的高宽固定,避免横竖屏切换时,重新生成
    var screenWidth = 1080
    var screenHeight = 1080

    //显示的高宽,图片解码后,需要根据这个缩放
    var viewWidth = 1080
    var viewHeight = 1080

    private val reflowCache = ReflowViewCache()
    private var pdfAdapter: ARecyclerView.Adapter<ReflowViewHolder>? = null

    init {
        viewWidth = Utils.getScreenWidthPixelWithOrientation(context)
        viewHeight = Utils.getScreenHeightPixelWithOrientation(context)
        screenWidth = viewWidth
        screenHeight = viewHeight
        initView()
    }

    private fun initView() {
        val zoomModel = ZoomModel()

        docViewModel.bookProgress?.run {
            zoomModel.zoom = this.zoomLevel / 1000
            scrollOrientation = docViewModel.bookProgress?.scrollOrientation ?: 1
        }

        initDecodeService()

        val gestureDetector = GestureDetector(context, MySimpleOnGestureListener())
        mRecyclerView = PdfRecyclerView(context)

        val iView = object : IView {
            override fun getWidth(): Int {
                return screenWidth
            }

            override fun getHeight(): Int {
                return screenHeight
            }
        }
        decodeService?.setContainerView(iView)

        with(mRecyclerView) {
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            isNestedScrollingEnabled = false
            layoutManager = ExtraSpaceLinearLayoutManager(context, LinearLayoutManager.VERTICAL)
            this.gestureDetector = gestureDetector

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
        mRecyclerView.viewTreeObserver
            .addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mRecyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val changed = viewWidth != mRecyclerView.width
                    viewWidth = mRecyclerView.width
                    viewHeight = mRecyclerView.height
                    screenWidth = viewWidth
                    screenHeight = viewHeight
                    Logcat.d(
                        TAG, String.format(
                            "onGlobalLayout : w-h:%s-%s",
                            viewWidth, viewHeight
                        )
                    )
                    if (changed) {
                        mRecyclerView.adapter?.notifyDataSetChanged()
                    }
                }
            })

        setFilter(PdfOptionRepository.getColorMode())
        setOrientation(scrollOrientation)
    }

    private fun loadDocument() {
        instance.diskIO().execute {
            try {
                document = decodeService!!.open(mPath, true)
            } catch (_: Exception) {
            }
            instance.mainThread().execute {
                if (null == document) {
                    Toast.makeText(
                        context,
                        "Open Failed",
                        Toast.LENGTH_LONG
                    ).show()
                    context.finish()
                    return@execute
                }
                isDocLoaded = true
                doLoadDoc(decodeService!!.pageSizeBean, document!!)
                //documentView.showDocument(crop)
            }
        }
    }

    override fun init() {
        Logcat.d(TAG, "init:")
        crop = docViewModel.checkCrop()

        loadDocument()
    }

    private fun doLoadDoc(pageSizeBean: APageSizeLoader.PageSizeBean, document: CodecDocument) {
        Logcat.d(TAG, "doLoadDoc:${pageSizeBean.crop}, ${pageSizeBean.List!!.size}")
        this.mPageSizes = pageSizeBean.List!!
        if (null == mPageSizes) {
            return
        }

        docViewModel.setPageCount(mPageSizes.size)

        controllerListener?.doLoadedDoc(
            mPageSizes.size,
            docViewModel.getCurrentPage(),
            document.loadOutline()
        )

        initStyleControls()

        val dm = context.resources.displayMetrics
        pdfAdapter = object : ARecyclerView.Adapter<ReflowViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReflowViewHolder {
                val pdfView = ReflowView(context)
                val holder = ReflowViewHolder(pdfView)
                val lp: ARecyclerView.LayoutParams = ARecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    viewHeight
                )
                pdfView.layoutParams = lp

                return holder
            }

            override fun getItemCount(): Int {
                return mPageSizes.size
            }

            override fun onBindViewHolder(holder: ReflowViewHolder, position: Int) {
                holder.bindReflow(dm.densityDpi, position, reflowCache)
            }

            override fun onViewRecycled(holder: ReflowViewHolder) {
                super.onViewRecycled(holder)
                holder.recycleViews(reflowCache)
            }
        }
        mRecyclerView.adapter = pdfAdapter

        gotoPage(docViewModel.getCurrentPage())
    }

    private fun initDecodeService() {
        if (decodeService == null) {
            decodeService = createDecodeService()
        }
    }

    private fun createDecodeService(): DocDecodeService {
        val codecContext = DecodeServiceBase.openContext(mPath)
        if (null == codecContext) {
            Toast.makeText(context, "open file error", Toast.LENGTH_SHORT).show()
            context.finish()
        }
        return DocDecodeService(codecContext)
    }

    override fun getDocumentView(): View {
        return mRecyclerView
    }

    private fun gotoPage(pos: Int) {
        if (pos > 0) {
            val layoutManager = mRecyclerView.layoutManager

            val vto: ViewTreeObserver = mRecyclerView.viewTreeObserver
            vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mRecyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    Logcat.d(TAG, "onGlobalLayout:$this,pos:$pos")
                    mRecyclerView.postDelayed({
                        layoutManager!!.scrollToPosition(pos)
                        mRecyclerView.requestLayout()
                    }, 10L)
                }
            })
        }
    }

    override fun getCurrentBitmap(): Bitmap? {
        val bitmap = BitmapCache.getInstance()
            .getBitmap(generateCacheKey(getCurrentPos(), viewWidth, viewHeight, crop))
        return bitmap
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
        return crop
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

    override fun scrollPageHorizontal(x: Int, left: Int, right: Int, margin: Int): Boolean {
        if (x < left) {
            var scrollX = mRecyclerView.scrollX
            scrollX -= mRecyclerView.width
            mRecyclerView.scrollBy(scrollX + margin, 0)
            return true
        } else if (x > right) {
            var scrollX = mRecyclerView.scrollX
            scrollX += mRecyclerView.width
            mRecyclerView.scrollBy(scrollX + margin, 0)
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
        if (tryHyperlink(ev)) {
            return true
        }
        val documentView = getDocumentView()
        val height =
            if (scrollOrientation == LinearLayoutManager.VERTICAL) documentView.height
            else documentView.width
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
        mRecyclerView.layoutManager?.scrollToPosition(index)
        updateProgress(index)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        mRecyclerView.stopScroll()

        viewWidth = Utils.dipToPixel(newConfig.screenWidthDp.toFloat())
        viewHeight = Utils.dipToPixel(newConfig.screenHeightDp.toFloat())
        Logcat.d(
            TAG, String.format(
                "newConfig:w-h:%s-%s, config:%s-%s, %s",
                viewWidth,
                viewHeight,
                newConfig.screenWidthDp,
                newConfig.screenHeightDp,
                newConfig.orientation
            )
        )

        val lm = (mRecyclerView.layoutManager as LinearLayoutManager)
        var offset = 0
        val first = lm.findFirstVisibleItemPosition()
        if (first > 0) {
            val child = lm.findViewByPosition(first)
            child?.run {
                val r = Rect()
                child.getLocalVisibleRect(r)
                offset = r.top
            }
        }
        lm.scrollToPositionWithOffset(first, -offset)
        mRecyclerView.adapter?.notifyDataSetChanged()
    }

    private fun updateProgress(index: Int) {
        if (pageController?.visibility() == View.VISIBLE) {
            pageController?.updatePageProgress(index)
        }
    }

    override fun notifyDataSetChanged() {
        mRecyclerView.adapter?.notifyDataSetChanged()
    }

    override fun notifyItemChanged(pos: Int) {
        mRecyclerView.adapter?.notifyItemChanged(pos)
    }

    override fun setFilter(colorMode: Int) {
        val colorMatrix = getColorMode(colorMode)
        filter = if (null == colorMatrix) {
            null
        } else {
            ColorMatrixColorFilter(ColorMatrix(colorMatrix))
        }
    }

    override fun decodePageForTts(currentPos: Int) {
    }

    override fun reflow(): Int {
        return BookProgress.REFLOW_SCAN
    }

    override fun prev(string: String?) {
    }

    override fun next(string: String?) {
    }

    override fun clearSearch() {
    }

    override fun showSearch() {
    }

    override fun setSpeakingPage(page: Int) {
    }

    override fun toggleThumbnail() {
    }

    override fun selectFont() {
    }

    //--------------------------------------

    override fun onResume() {
        mRecyclerView.postDelayed({ mRecyclerView.adapter?.notifyDataSetChanged() }, 250L)
    }

    override fun onPause() {
        if (null != docViewModel.bookProgress) {
            docViewModel.bookProgress!!.reflow = BookProgress.REFLOW_SCAN
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
    }

    override fun onDestroy() {
        Logcat.d(TAG, "ScanReflow.onDestroy")
        decodeService?.recycle()
    }

    //===========================================
    override fun showController() {
    }

    companion object {
        const val TAG = "ScanReflow"

        private fun generateCacheKey(index: Int, w: Int, h: Int, crop: Boolean): String {
            return String.format("%s-%s-%s-%s", index, w, h, crop)
        }
    }

    inner class ReflowViewHolder(private var pageView: ReflowView) :
        ARecyclerView.ViewHolder(pageView) {

        private var index: Int = 0

        private fun updateImage(args: Any?, reflowViewCache: ReflowViewCache?) {
            if (args is Array<*>) {
                val bitmaps = args[0] as List<Bitmap>
                val pageNumber = args[1]
                if (pageNumber != index) {
                    Logcat.d(TAG, "updateImage.not same page:$pageNumber, $index")
                    return
                }
                var allHeight = 0
                for (bitmap in bitmaps) {
                    allHeight += pageView.addImageView(
                        bitmap,
                        viewWidth,
                        reflowViewCache,
                        filter
                    )
                }
                widthHeightMap["$index-$viewWidth"] = allHeight
                var lp = pageView.layoutParams as ARecyclerView.LayoutParams?
                if (null == lp) {
                    lp = ARecyclerView.LayoutParams(
                        ARecyclerView.LayoutParams.MATCH_PARENT,
                        allHeight
                    )
                    pageView.layoutParams = lp
                } else {
                    lp.width = ARecyclerView.LayoutParams.MATCH_PARENT
                    lp.height = allHeight
                    pageView.layoutParams = lp
                }
            }
        }

        fun bindReflow(
            dpi: Int,
            index: Int,
            reflowViewCache: ReflowViewCache?
        ) {
            val height: Int? = widthHeightMap["$index-$viewWidth"]
            if (height != null) {
                var lp = pageView.layoutParams as ARecyclerView.LayoutParams?
                if (null == lp) {
                    lp = ARecyclerView.LayoutParams(ARecyclerView.LayoutParams.MATCH_PARENT, height)
                    pageView.layoutParams = lp
                } else {
                    lp.width = ARecyclerView.LayoutParams.MATCH_PARENT
                    lp.height = height
                    pageView.layoutParams = lp
                }
            }

            val callback = object : DecodeService.DecodeCallback {
                override fun decodeComplete(bitmap: Bitmap?, param: Boolean, args: Any?) {
                    instance.mainThread().execute {
                        updateImage(args, reflowViewCache)
                    }
                }

                override fun shouldRender(index: Int, param: Boolean): Boolean {
                    return this@ReflowViewHolder.index == index
                }
            }

            this.index = index
            val key = generateCacheKey(index, viewWidth, viewHeight, crop)
            decodeService?.decodePage(
                key,
                null,
                false,
                index,
                callback,
                getPageScale(),
                RectF(0f, 0f, 1f, 1f),
                dpi,
                getFontSize()
            )
        }

        fun recycleViews(reflowViewCache: ReflowViewCache?) {
            if (null != reflowViewCache) {
                for (i in 0 until pageView.childCount) {
                    val child = pageView.getChildAt(i)
                    reflowViewCache.addImageView((child as ImageView))
                }
                pageView.removeAllViews()
            }
        }
    }

    inner class ReflowView(context: Context?) : LinearLayout(context) {
        private val leftPadding = 15
        private val rightPadding = 15

        init {
            orientation = VERTICAL
            setBackgroundColor(Color.WHITE)
            minimumHeight = Utils.dipToPixel(200f)
        }

        fun addImageView(
            bitmap: Bitmap,
            width: Int,
            reflowViewCache: ReflowViewCache?,
            filter: ColorFilter?
        ): Int {
            val imageView: ImageView?
            if (null != reflowViewCache && reflowViewCache.imageViewCount() > 0) {
                imageView = reflowViewCache.getAndRemoveImageView(0)
            } else {
                imageView = ImageView(context)
                imageView.adjustViewBounds = true
                imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            }
            val ratio = 1f * width / bitmap.width
            val rw = width - leftPadding - rightPadding
            val height = (bitmap.height * ratio).toInt()
            //Logcat.d(TAG, String.format("w-h:%s-%s,%s", rw, height, ratio))
            val lp = LayoutParams(rw, height)
            lp.leftMargin = leftPadding
            lp.rightMargin = rightPadding
            addView(imageView, lp)
            imageView.colorFilter = filter
            imageView.setImageBitmap(bitmap)

            return height
        }
    }
}