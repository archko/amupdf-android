package cn.archko.pdf.adapters

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.awidget.ARecyclerView
import cn.archko.pdf.common.StyleHelper
import cn.archko.pdf.core.App
import cn.archko.pdf.core.cache.ReflowViewCache
import cn.archko.pdf.core.common.AppExecutors
import cn.archko.pdf.core.decode.MupdfDocument
import cn.archko.pdf.core.entity.ReflowBean
import cn.archko.pdf.core.utils.Utils

/**
 * @author: archko 2016/5/13 :11:03
 */
class MuPDFReflowAdapter(
    private val mContext: Context,
    private val mupdfDocument: MupdfDocument?,
    private var styleHelper: StyleHelper?,
) : ARecyclerView.Adapter<ReflowTextViewHolder>() {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReflowTextViewHolder {
        val pdfView: ReflowTextViewHolder.PDFTextView =
            ReflowTextViewHolder.PDFTextView(mContext, styleHelper)
        val holder = ReflowTextViewHolder(pdfView)
        val lp: ARecyclerView.LayoutParams = ARecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        pdfView.layoutParams = lp

        return holder
    }

    override fun onBindViewHolder(holder: ReflowTextViewHolder, position: Int) {
        //mupdf only single thread
        AppExecutors.instance.diskIO().execute {
            val result = decode(position)
            result?.run {
                AppExecutors.instance.mainThread().execute {
                    holder.bindAsList(
                        result,
                        screenHeight,
                        screenWidth,
                        systemScale,
                        reflowCache,
                        showBookmark(position)
                    )
                }
            }
        }
    }

    fun decode(pos: Int): List<ReflowBean>? {
        return mupdfDocument?.decodeReflow(pos)
    }

    private fun showBookmark(position: Int): Boolean {
        /*val bookmarks = pdfViewModel.bookmarks
        if (null != bookmarks) {
            for (bookmark in bookmarks) {
                if (position == bookmark.page) {
                    return true
                }
            }
        }*/
        return false
    }

    override fun onViewRecycled(holder: ReflowTextViewHolder) {
        super.onViewRecycled(holder)
        val pdfHolder = holder as ReflowTextViewHolder?
        pdfHolder?.recycleViews(reflowCache)
        //Logcat.d("onViewRecycled end,exist count::${reflowCache.textViewCount()},${reflowCache.imageViewCount()}")
    }

    fun clearCacheViews() {
        reflowCache.clear()
    }

}
