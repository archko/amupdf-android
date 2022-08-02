package cn.archko.pdf.adapters

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.App
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.ReflowViewCache
import cn.archko.pdf.common.StyleHelper
import cn.archko.pdf.entity.ReflowBean
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.utils.Utils
import cn.archko.pdf.viewmodel.PDFViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author: archko 2016/5/13 :11:03
 */
class MuPDFReflowAdapter(
    private val mContext: Context,
    private val mupdfDocument: MupdfDocument?,
    private var styleHelper: StyleHelper?,
    private var scope: CoroutineScope?,
    private var pdfViewModel: PDFViewModel
) : BaseRecyclerAdapter<Any>(mContext) {

    private var screenHeight = 720
    private var screenWidth = 1080
    private val systemScale = Utils.getScale()
    private val reflowCache = ReflowViewCache()

    init {
        screenHeight = App.instance!!.screenHeight
        screenWidth = App.instance!!.screenWidth
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return mupdfDocument?.countPages()!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<*> {
        val pdfView: ReflowTextViewHolder.PDFTextView =
            ReflowTextViewHolder.PDFTextView(mContext, styleHelper)
        val holder = ReflowTextViewHolder(pdfView)
        val lp: RecyclerView.LayoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        pdfView.layoutParams = lp

        return holder
    }

    fun decode(pos: Int): List<ReflowBean>? {
        return mupdfDocument?.decodeReflow(pos);
    }

    override fun onBindViewHolder(holder: BaseViewHolder<Any>, pos: Int) {
        scope!!.launch {
            val result = decode(pos)
            withContext(Dispatchers.Main) {
                result?.run {
                    (holder as ReflowTextViewHolder).bindAsList(
                        result,
                        screenHeight,
                        screenWidth,
                        systemScale,
                        reflowCache,
                        showBookmark(pos)
                    )
                }
            }
        }
    }

    private fun showBookmark(position: Int): Boolean {
        val bookmarks = pdfViewModel.bookmarks
        if (null != bookmarks) {
            for (bookmark in bookmarks) {
                if (position == bookmark.page) {
                    return true
                }
            }
        }
        return false
    }

    override fun onViewRecycled(holder: BaseViewHolder<*>) {
        super.onViewRecycled(holder)
        val pdfHolder = holder as ReflowTextViewHolder?

        Logcat.d("onViewRecycled:$pdfHolder,exist count:${reflowCache.textViewCount()},${reflowCache.imageViewCount()}")
        pdfHolder?.recycleViews(reflowCache)
        Logcat.d("onViewRecycled end,exist count::${reflowCache.textViewCount()},${reflowCache.imageViewCount()}")
    }

    fun clearCacheViews() {
        reflowCache.clear()
    }

    fun setScope(scope: CoroutineScope?) {
        this.scope = scope
    }

    companion object {

        const val TYPE_TEXT = 0
    }

}
