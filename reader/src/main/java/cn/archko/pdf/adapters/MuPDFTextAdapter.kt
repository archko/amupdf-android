package cn.archko.pdf.adapters

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.awidget.ARecyclerView
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.common.StyleHelper
import cn.archko.pdf.core.App
import cn.archko.pdf.core.cache.ReflowViewCache
import cn.archko.pdf.core.entity.ReflowBean
import cn.archko.pdf.core.utils.Utils

/**
 * @author: archko 2022/7/13 :11:03
 */
class MuPDFTextAdapter(
    private val context: Context,
    private var styleHelper: StyleHelper?,
) : ARecyclerView.Adapter<ReflowTextViewHolder>() {

    private var screenHeight = 1080
    private var screenWidth = 1920
    private val systemScale = Utils.getScale()
    private val reflowCache = ReflowViewCache()
    var data: List<ReflowBean>? = null

    init {
        screenWidth = App.instance!!.screenWidth
        screenHeight = App.instance!!.screenHeight
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReflowTextViewHolder {
        val pdfView: ReflowTextViewHolder.PDFTextView =
            ReflowTextViewHolder.PDFTextView(context, styleHelper)
        val holder = ReflowTextViewHolder(pdfView)
        val lp: RecyclerView.LayoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            screenHeight
        )
        pdfView.layoutParams = lp

        return holder
    }

    override fun getItemCount(): Int {
        return data?.size ?: 0
    }

    override fun onBindViewHolder(holder: ReflowTextViewHolder, position: Int) {
        val result = data?.get(position)
        result?.run {
            holder.bindAsReflowBean(
                result,
                screenHeight,
                screenWidth,
                systemScale,
                reflowCache,
                showBookmark(position)
            )
        }
    }

    private fun showBookmark(position: Int): Boolean {
        return false
    }

    override fun onViewRecycled(holder: ReflowTextViewHolder) {
        super.onViewRecycled(holder)
        holder.recycleViews(reflowCache)
    }

    fun clearCacheViews() {
        reflowCache.clear()
    }

}
