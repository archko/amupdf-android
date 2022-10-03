package cn.archko.pdf.adapters

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.App
import cn.archko.pdf.common.ReflowViewCache
import cn.archko.pdf.common.StyleHelper
import cn.archko.pdf.entity.ReflowBean
import cn.archko.pdf.utils.Utils

/**
 * @author: archko 2022/7/13 :11:03
 */
class MuPDFTextAdapter(
    private val context: Context,
    private var styleHelper: StyleHelper?,
) : HeaderAndFooterRecyclerAdapter<ReflowBean>(context) {

    private var screenHeight = 720
    private var screenWidth = 1080
    private val systemScale = Utils.getScale()
    private val reflowCache = ReflowViewCache()

    init {
        screenHeight = App.instance!!.screenHeight
        screenWidth = App.instance!!.screenWidth
    }

    override fun doCreateViewHolder(parent: ViewGroup?, viewType: Int): BaseViewHolder<ReflowBean> {
        val pdfView: ReflowTextViewHolder.PDFTextView =
            ReflowTextViewHolder.PDFTextView(context, styleHelper)
        val holder = ReflowTextViewHolder(pdfView)
        val lp: RecyclerView.LayoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        pdfView.layoutParams = lp

        return holder
    }

    override fun onBindNormalViewHolder(
        holder: BaseViewHolder<ReflowBean>,
        position: Int,
        realPos: Int
    ) {
        val result = data.get(realPos)
        result?.run {
            (holder as ReflowTextViewHolder).bindAsReflowBean(
                result,
                screenHeight,
                screenWidth,
                systemScale,
                reflowCache,
                showBookmark(realPos)
            )
        }
    }

    private fun showBookmark(position: Int): Boolean {
        return false
    }

    override fun onViewRecycled(holder: BaseViewHolder<ReflowBean>) {
        super.onViewRecycled(holder)
        if (holder is ReflowTextViewHolder) {
            holder.recycleViews(reflowCache)
        }
    }

    fun clearCacheViews() {
        reflowCache.clear()
    }

}
