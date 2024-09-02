package cn.archko.pdf.activities

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.awidget.ARecyclerView
import androidx.recyclerview.awidget.LinearLayoutManager
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.decode.DocDecodeService
import cn.archko.pdf.widgets.APDFView

/**
 * @author: archko 2024/3/16 :21:28
 */
class PDFRecyclerAdapter(
    var context: Context,
    val decodeService: DocDecodeService,
    val mPageSizes: List<APage>,
    val recyclerView: ARecyclerView
) : ARecyclerView.Adapter<ARecyclerView.ViewHolder>()/*, FastScrollRecyclerView.SectionedAdapter*/ {

    private var orientation: Int = LinearLayoutManager.VERTICAL
    private var crop = true

    var defaultWidth = 1080
    var defaultHeight = 1080

    init {
        initWidthAndHeight()
    }

    private fun initWidthAndHeight() {
        if (mPageSizes != null && mPageSizes.size > 0 && viewWidth() > 0) {
            val aPage = mPageSizes[0]
            defaultWidth = viewWidth()
            defaultHeight = defaultWidth
            if (orientation == LinearLayoutManager.VERTICAL) {//垂直方向,以宽为准
                defaultHeight = (defaultWidth * aPage.height / aPage.width).toInt()
            } else {//水平滚动,以高为准
                defaultHeight = defaultWidth
                defaultWidth = (defaultHeight * aPage.width / aPage.height).toInt()
            }
        }
    }

    fun setOriention(ori: Int) {
        orientation = ori
        initWidthAndHeight()
    }

    fun setCrop(crop: Boolean) {
        this.crop = crop
    }

    fun viewWidth(): Int {
        return if (orientation == LinearLayoutManager.VERTICAL) {
            recyclerView.width
        } else {
            recyclerView.height
        }
    }

    //override fun getSectionName(position: Int): String {
    //    return String.format("%d", position + 1)
    //}

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ARecyclerView.ViewHolder {
        val view = APDFView(context, decodeService)
            .apply {
                layoutParams = ARecyclerView.LayoutParams(defaultWidth, defaultHeight)
                adjustViewBounds = true
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
        return mPageSizes.size
    }

    inner class PdfHolder(internal var view: APDFView) : ARecyclerView.ViewHolder(view) {
        fun onBind(position: Int) {
            val aPage = mPageSizes.get(position)
            var screenWidth = defaultWidth
            if (screenWidth == 0) {
                Log.d("TAG", String.format("onBind.size is 0 %s--%s", position, defaultWidth))
                screenWidth = defaultWidth
                return
            }

            view.updatePage(aPage, orientation, crop, screenWidth, defaultHeight)
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