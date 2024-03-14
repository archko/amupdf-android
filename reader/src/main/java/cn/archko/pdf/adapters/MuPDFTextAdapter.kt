package cn.archko.pdf.adapters

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.awidget.ARecyclerView
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
) : ARecyclerView.Adapter<ReflowTextViewHolder>() {

    private var screenHeight = 720
    private var screenWidth = 1080
    private val systemScale = Utils.getScale()
    private val reflowCache = ReflowViewCache()
    var data: List<ReflowBean>? = null

    init {
        screenHeight = App.instance!!.screenHeight
        screenWidth = App.instance!!.screenWidth
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReflowTextViewHolder {
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

    override fun getItemCount(): Int {
        return data?.size ?: 0
    }

    override fun onBindViewHolder(holder: ReflowTextViewHolder, position: Int) {
        val result = data?.get(position)
        result.run {
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
        if (holder is ReflowTextViewHolder) {
            holder.recycleViews(reflowCache)
        }
    }

    fun clearCacheViews() {
        reflowCache.clear()
    }

}
