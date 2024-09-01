package cn.archko.pdf.activities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.awidget.ARecyclerView
import cn.archko.pdf.adapters.ReflowTextViewHolder
import cn.archko.pdf.common.ReflowHelper
import cn.archko.pdf.core.cache.BitmapCache
import cn.archko.pdf.core.cache.ReflowViewCache
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.entity.BookProgress
import cn.archko.pdf.core.entity.State
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.viewmodel.DocViewModel
import cn.archko.pdf.viewmodel.PDFViewModel
import cn.archko.pdf.widgets.PageControls
import com.github.axet.k2pdfopt.K2PdfOpt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    context: FragmentActivity,
    scope: CoroutineScope,
    mControllerLayout: RelativeLayout,
    docViewModel: DocViewModel,
    mPath: String,
    pageControls: PageControls?,
    controllerListener: ControllerListener?,
) : ABaseViewController(
    context,
    scope,
    mControllerLayout,
    docViewModel,
    mPath,
    pageControls,
    controllerListener
) {

    val opt = K2PdfOpt()
    private var mPageSizes = mutableListOf<APage>()
    private var pdfViewModel = PDFViewModel()
    private var screenWidth = 1080
    private var screenHeight = 1920
    private val reflowCache = ReflowViewCache()

    init {
        screenWidth = Utils.getScreenWidthPixelWithOrientation(context)
        screenHeight = Utils.getScreenHeightPixelWithOrientation(context)
    }

    private var adapter = object : ARecyclerView.Adapter<ReflowViewHolder>() {
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
            scope.launch {
                val dm = context.resources.displayMetrics
                val key = "reflow:$position"
                var bitmap = BitmapCache.getInstance().getBitmap(key)
                if (bitmap == null) {
                    bitmap = ReflowHelper.loadBitmapByPage(
                        pdfViewModel.mupdfDocument,
                        (screenWidth * 2.4).toInt(),
                        position,
                    )
                    BitmapCache.getInstance().addBitmap(key, bitmap)
                }
                var bitmaps: MutableList<Bitmap>? = null
                if (null != bitmap) {
                    bitmaps = ReflowHelper.k2pdf2bitmap(
                        opt,
                        1f,
                        bitmap,
                        screenWidth,
                        screenHeight,
                        dm.densityDpi
                    )
                }
                withContext(Dispatchers.Main) {
                    holder.bindReflow(
                        bitmaps,
                        screenWidth,
                        reflowCache
                    )
                }
            }
        }

        override fun onViewRecycled(holder: ReflowViewHolder) {
            super.onViewRecycled(holder)
            if (holder is ReflowViewHolder) {
                holder.recycleViews(reflowCache)
            }
        }
    }

    override fun showPasswordDialog() {
    }

    override fun loadDocument() {
        scope.launch {
            pdfViewModel.loadPdfDoc(context, mPath, null)
            pdfViewModel.pageFlow.collectLatest {
                if (it.state == State.PASS) {
                    showPasswordDialog()
                    return@collectLatest
                }
                val cp = pdfViewModel.countPages()
                if (cp > 0) {
                    postLoadDoc(cp)
                } else {
                    context.finish()
                }
            }
        }
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
                }
                doLoadDoc()
            }
        }
    }

    private fun getPageSize(pageNum: Int): APage? {
        val p = pdfViewModel.loadPage(pageNum) ?: return null

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

    override fun doLoadDoc() {
        initReflowMode(docViewModel.getCurrentPage())

        controllerListener?.doLoadedDoc(
            pdfViewModel.countPages(),
            docViewModel.getCurrentPage(),
            pdfViewModel.links
        )
    }

    override fun initReflowMode(pos: Int) {
        mRecyclerView.adapter = adapter

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

    override fun getCount(): Int {
        return mPageSizes.size
    }

    override fun updateProgress(index: Int) {
        if (pdfViewModel.mupdfDocument != null && pageControls?.visibility() == View.VISIBLE) {
            pageControls?.updatePageProgress(index)
        }
    }

    override fun decodePageForTts(currentPos: Int) {
    }

    //--------------------------------------

    override fun onPause() {
        if (null != pdfViewModel.mupdfDocument && null != docViewModel.bookProgress) {
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
        pdfViewModel.destroy()
    }

    inner class ReflowViewHolder(private var pageView: ReflowView) :
        ARecyclerView.ViewHolder(pageView) {

        fun bindReflow(
            bitmaps: List<Bitmap>?,
            screenWidth: Int,
            reflowViewCache: ReflowViewCache?
        ) {
            recycleViews(reflowViewCache)
            if (bitmaps != null) {
                for (bitmap in bitmaps) {
                    pageView.addImageView(
                        bitmap,
                        screenWidth,
                        reflowViewCache,
                    )
                }
            }
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

    override fun reflow(): Int {
        return BookProgress.REFLOW_SCAN
    }
}
