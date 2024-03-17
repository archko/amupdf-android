package cn.archko.pdf.activities

import android.content.Context
import android.util.SparseArray
import android.view.ViewGroup
import androidx.recyclerview.awidget.ARecyclerView
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.entity.APage
import cn.archko.pdf.fastscroll.FastScrollRecyclerView
import cn.archko.pdf.viewmodel.PDFViewModel
import cn.archko.pdf.widgets.APDFView

/**
 * @author: archko 2024/3/16 :21:28
 */
class PDFRecyclerAdapter(
    var context: Context,
    val pdfViewModel: PDFViewModel,
    val mPageSizes: SparseArray<APage>
) : ARecyclerView.Adapter<ARecyclerView.ViewHolder>(), FastScrollRecyclerView.SectionedAdapter {

    var defaultWidth = 1080
    var defaultHeight = 1080
    private var crop: Boolean = true

    fun setCrop(crop: Boolean) {
        this.crop = crop
    }
    
    override fun getSectionName(position: Int): String {
        return String.format("%d", position + 1);
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ARecyclerView.ViewHolder {
        val view = APDFView(context)
            .apply {
                layoutParams = ViewGroup.LayoutParams(defaultWidth, defaultHeight)
            }
        return PdfHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ARecyclerView.ViewHolder, position: Int) {
        val pdfHolder = viewHolder as PdfHolder
        pdfHolder.onBind(position)
    }

    override fun onViewRecycled(holder: ARecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        val pdfHolder = holder as PdfHolder?

        pdfHolder?.view?.recycle()
    }

    override fun getItemCount(): Int {
        return mPageSizes.size()
    }

    inner class PdfHolder(internal var view: APDFView) : ARecyclerView.ViewHolder(view) {
        fun onBind(position: Int) {
            val pageSize = mPageSizes.get(position)
            if (pageSize.getTargetWidth() != defaultWidth) {
                pageSize.setTargetWidth(defaultWidth)
            }
            Logcat.d(
                String.format(
                    "bind:position:%s,targetWidth:%s, measuredWidth:%s-%s",
                    position,
                    pageSize.getTargetWidth(),
                    defaultWidth,
                    defaultHeight
                )
            )
            if (defaultWidth <= 0) {
                return
            }
            view.updatePage(pageSize, 1.0f, pdfViewModel.mupdfDocument, crop)
        }

        /*private fun showBookmark(position: Int): Boolean {
            val bookmarks = pdfViewModel.bookmarks
            if (null != bookmarks) {
                for (bookmark in bookmarks) {
                    if (position == bookmark.page) {
                        return true
                    }
                }
            }
            return false
        }*/
    }

}