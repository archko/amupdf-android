package cn.archko.pdf.activities

import android.app.ProgressDialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
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
import cn.archko.pdf.core.cache.BitmapCache
import cn.archko.pdf.core.cache.ReflowViewCache
import cn.archko.pdf.core.common.APageSizeLoader
import cn.archko.pdf.core.common.AppExecutors
import cn.archko.pdf.core.common.AppExecutors.Companion.instance
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.entity.BookProgress
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.core.widgets.ExtraSpaceLinearLayoutManager
import cn.archko.pdf.decode.DocDecodeService
import cn.archko.pdf.decode.DocDecodeService.IView
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.viewmodel.DocViewModel
import cn.archko.pdf.widgets.PageControls
import kotlinx.coroutines.CoroutineScope
import org.vudroid.core.DecodeService
import org.vudroid.core.DecodeServiceBase
import org.vudroid.core.codec.CodecDocument
import org.vudroid.core.models.CurrentPageModel
import org.vudroid.core.models.DecodingProgressModel
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
    private var pdfViewModel: DocViewModel,
    private var mPath: String,
    private var pageControls: PageControls?,
    private var controllerListener: ControllerListener?,
) :
    OutlineListener, AViewController {

    private lateinit var mRecyclerView: ARecyclerView
    private lateinit var mPageSizes: List<APage>

    private var crop: Boolean = false
    private var scrollOrientation = LinearLayoutManager.VERTICAL

    private var decodeService: DocDecodeService? = null
    private lateinit var currentPageModel: CurrentPageModel

    protected var progressDialog: ProgressDialog? = null
    protected var isDocLoaded: Boolean = false
    private var document: CodecDocument? = null

    private inner class MySimpleOnGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            controllerListener?.onSingleTapConfirmed(e, 0)
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            return controllerListener?.onDoubleTap(e, 0) ?: super.onDoubleTap(e)
        }
    }

    var defaultWidth = 1080
    var defaultHeight = 1080

    private val reflowCache = ReflowViewCache()
    private var pdfAdapter: ARecyclerView.Adapter<ReflowViewHolder>? = null

    init {
        defaultWidth = Utils.getScreenWidthPixelWithOrientation(context)
        defaultHeight = Utils.getScreenHeightPixelWithOrientation(context)
        initView()
    }

    private fun initView() {
        val zoomModel = ZoomModel()

        pdfViewModel.bookProgress?.run {
            zoomModel.zoom = this.zoomLevel / 1000
            scrollOrientation = pdfViewModel.bookProgress?.scrollOrientation ?: 1
        }

        val progressModel = DecodingProgressModel()
        progressModel.addEventListener(this)
        currentPageModel = CurrentPageModel()
        currentPageModel.addEventListener(this)
        initDecodeService()

        val gestureDetector = GestureDetector(context, MySimpleOnGestureListener())
        val view = LayoutInflater.from(context)
            .inflate(cn.archko.pdf.R.layout.reader_crop, mControllerLayout, false)
        mRecyclerView = view.findViewById(cn.archko.pdf.R.id.recycler)
        (mRecyclerView.parent as ViewGroup).removeView(mRecyclerView)

        val iView= object : IView {
            override fun getWidth(): Int {
                return defaultWidth
            }

            override fun getHeight(): Int {
                return defaultHeight
            }
        }
        decodeService?.setContainerView(iView)

        with(mRecyclerView) {
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            isNestedScrollingEnabled = false
            layoutManager = ExtraSpaceLinearLayoutManager(context, LinearLayoutManager.VERTICAL)
            setItemViewCacheSize(0)

            //addItemDecoration(ViewerDividerItemDecoration(context, LinearLayoutManager.VERTICAL))
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
                    val changed = defaultWidth != mRecyclerView.width
                    defaultWidth = mRecyclerView.width
                    defaultHeight = mRecyclerView.height
                    if (Logcat.loggable) {
                        Logcat.d(
                            "TAG", String.format(
                                "onGlobalLayout : w-h:%s-%s",
                                defaultWidth, defaultHeight
                            )
                        )
                    }
                    if (changed) {
                        mRecyclerView.adapter?.notifyDataSetChanged()
                    }
                }
            })

        mRecyclerView.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event) == true
        }
    }

    private fun loadDocument() {
        progressDialog = ProgressDialog(context)
        progressDialog!!.setMessage("Loading")
        progressDialog!!.show()

        instance.diskIO().execute {
            try {
                document = decodeService!!.open(mPath, crop, true)
            } catch (e: Exception) {
            }
            instance.mainThread().execute {
                progressDialog!!.dismiss()
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
        Logcat.d("init:")
        crop = pdfViewModel.checkCrop()

        loadDocument()
    }

    private fun doLoadDoc(pageSizeBean: APageSizeLoader.PageSizeBean, document: CodecDocument) {
        Logcat.d("doLoadDoc:${pageSizeBean.crop}, ${pageSizeBean.List!!.size}")
        this.mPageSizes = pageSizeBean.List!!
        if (null == mPageSizes) {
            return
        }

        controllerListener?.doLoadedDoc(
            mPageSizes!!.size,
            pdfViewModel.getCurrentPage(),
            document.loadOutline()
        )

        gotoPage(pdfViewModel.getCurrentPage())
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
        }
        return DocDecodeService(codecContext)
    }

    override fun getDocumentView(): View {
        return mRecyclerView
    }

    private fun gotoPage(pos: Int) {
        setOrientation(scrollOrientation)
        val dm = context.resources.displayMetrics
        pdfAdapter = object : ARecyclerView.Adapter<ReflowViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReflowViewHolder {
                val pdfView = ReflowView(context)
                val holder = ReflowViewHolder(pdfView)
                val lp: ARecyclerView.LayoutParams = ARecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
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
                if (holder is ReflowViewHolder) {
                    holder.recycleViews(reflowCache)
                }
            }
        }
        mRecyclerView.adapter = pdfAdapter

        if (pos > 0) {
            val layoutManager = mRecyclerView.layoutManager

            val vto: ViewTreeObserver = mRecyclerView.viewTreeObserver
            vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mRecyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    Logcat.d("onGlobalLayout:$this,pos:$pos")
                    mRecyclerView.postDelayed({
                        layoutManager!!.scrollToPosition(pos)
                        mRecyclerView.requestLayout()
                    }, 10L)
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

    override fun tryHyperlink(ev: MotionEvent): Boolean {
        return false
    }

    override fun onSingleTap(e: MotionEvent?, margin: Int): Boolean {
        if (e == null) {
            return false
        }
        if (tryHyperlink(e)) {
            return true
        }
        val documentView = getDocumentView()
        val height =
            if (scrollOrientation == LinearLayoutManager.VERTICAL) documentView.height else documentView.width
        val top = height / 4
        val bottom = height * 3 / 4
        if (scrollPage(e.y.toInt(), top, bottom, margin)) {
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
        BitmapCache.getInstance().clear()

        defaultWidth = Utils.dipToPixel(newConfig.screenWidthDp.toFloat())
        defaultHeight = Utils.dipToPixel(newConfig.screenHeightDp.toFloat())
        if (Logcat.loggable) {
            Logcat.d(
                "TAG", String.format(
                    "newConfig:w-h:%s-%s, config:%s-%s, %s",
                    defaultWidth,
                    defaultHeight,
                    newConfig.screenWidthDp,
                    newConfig.screenHeightDp,
                    newConfig.orientation
                )
            )
        }

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
        if (pageControls?.visibility() == View.VISIBLE) {
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
    }

    override fun reflow(): Int {
        return BookProgress.REFLOW_NO
    }

    //--------------------------------------

    override fun onResume() {
        mRecyclerView.postDelayed({ mRecyclerView.adapter?.notifyDataSetChanged() }, 250L)
    }

    override fun onPause() {
        if (null != pdfViewModel.bookProgress) {
            pdfViewModel.bookProgress!!.reflow = BookProgress.REFLOW_SCAN
            var savePos = getCurrentPos() + 1
            val lastPos = getLastPos()
            if (lastPos == mPageSizes.size - 1) {
                savePos = lastPos
            }
            pdfViewModel.saveBookProgress(
                mPath,
                mPageSizes.size,
                savePos,
                pdfViewModel.bookProgress!!.zoomLevel,
                -1,
                0
            )
        }
    }

    override fun onDestroy() {
        Logcat.d("crop.onDestroy")
        decodeService?.recycle()
    }

    //===========================================
    override fun showController() {
    }


    inner class ReflowViewHolder(private var pageView: ReflowView) :
        ARecyclerView.ViewHolder(pageView) {

        private var index: Int = 0

        private fun generateCacheKey(index: Int, w: Int, h: Int, crop: Boolean): String {
            return String.format("%s-%s-%s-%s", index, w, h, crop)
        }

        private fun updateImage(bmp: Bitmap?, args: Any?, reflowViewCache: ReflowViewCache?) {
            if (args is List<*>) {
                val bitmaps = args as List<Bitmap>
                for (bitmap in bitmaps) {
                    pageView.addImageView(
                        bitmap,
                        defaultWidth,
                        reflowViewCache,
                    )
                }
            }
        }

        fun bindReflow(
            dpi: Int,
            index: Int,
            reflowViewCache: ReflowViewCache?
        ) {
            this.index = index
            recycleViews(reflowViewCache)

            val key = generateCacheKey(index, defaultWidth, defaultHeight, crop)
            /*val bmp = BitmapCache.getInstance().getBitmap(key)

            if (null != bmp) {
                Log.d("TAG", String.format("hit the cache:%s", index))
                updateImage(this, bmp, resultWidth, resultHeight)
                return
            }*/

            val callback = object : DecodeService.DecodeCallback {
                override fun decodeComplete(bitmap: Bitmap?, param: Boolean, args: Any?) {
                    AppExecutors.instance.mainThread().execute {
                        updateImage(bitmap, args, reflowViewCache)
                    }
                }

                override fun shouldRender(index: Int, param: Boolean): Boolean {
                    return this@ReflowViewHolder.index == index
                }
            }

            decodeService?.decodePage(
                key,
                null,
                false,
                index,
                callback,
                2.4f,
                RectF(0f, 0f, 1f, 1f),
                dpi
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

    inner class ReflowView(context: Context?) :
        LinearLayout(context) {
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
        ) {
            val imageView: ImageView?
            if (null != reflowViewCache && reflowViewCache.imageViewCount() > 0) {
                imageView = reflowViewCache.getImageView(0)
            } else {
                imageView = ImageView(context)
                imageView.adjustViewBounds = true
                imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            }
            val ratio = 1f * width / bitmap.width
            val rw = width - leftPadding - rightPadding
            val height = (bitmap.height * ratio).toInt()
            //Logcat.d("reflow", String.format("w-h:%s-%s,%s", rw, height, ratio))
            val lp = LayoutParams(rw, height)
            lp.leftMargin = leftPadding
            lp.rightMargin = rightPadding
            addView(imageView, lp)
            imageView.setImageBitmap(bitmap)
        }
    }
}