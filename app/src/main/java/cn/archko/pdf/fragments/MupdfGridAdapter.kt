package cn.archko.pdf.fragments

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.awidget.ARecyclerView
import androidx.recyclerview.awidget.GridLayoutManager
import cn.archko.pdf.core.common.AppExecutors
import cn.archko.pdf.core.cache.BitmapCache
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.decode.MupdfDocument
import cn.archko.pdf.core.decode.DecodeParam
import cn.archko.pdf.core.listeners.ClickListener
import cn.archko.pdf.core.decode.DecodeCallback

/**
 * @author: archko 2023/3/8 :14:34
 */
class MupdfGridAdapter(
    var mupdfListener: MupdfListener,
    var context: Context,
    var recyclerView: ARecyclerView,
    var clickListener: ClickListener<View>
) :
    ARecyclerView.Adapter<ARecyclerView.ViewHolder>() {

    private var resultWidth: Int = 1080
    private var resultHeight: Int = 1080

    override fun getItemCount(): Int {
        return mupdfListener.getPageCount()
    }

    fun viewWidth(): Int {
        val lm = recyclerView.layoutManager
        if (lm is GridLayoutManager) {
            return recyclerView.width / lm.spanCount
        }
        return recyclerView.width
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ARecyclerView.ViewHolder {
        val width: Int = viewWidth()
        val view = ImageView(context)
            .apply {
                layoutParams = ViewGroup.LayoutParams(width, width)
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

        return PdfHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ARecyclerView.ViewHolder, position: Int) {
        val pdfHolder = viewHolder as PdfHolder

        pdfHolder.onBind(position)
    }

    override fun onViewRecycled(holder: ARecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        val pos = holder.absoluteAdapterPosition
        Log.d("TAG", String.format("decode.onViewRecycled:%s", pos))
        (holder as PdfHolder).view.setImageResource(android.R.color.transparent)
    }

    inner class PdfHolder(internal var view: ImageView) : ARecyclerView.ViewHolder(view) {

        private var index = -1

        fun onBind(position: Int) {
            index = position
            val aPage = mupdfListener.getPageList()[position]

            val cacheKey =
                "${mupdfListener.getDocument()!!}_page_$position-${aPage}"

            var width: Int = viewWidth() / 2
            if (width == 0) {
                width = 1080
            }
            val height: Int = (width * 4 / 3f).toInt()
            resultWidth = width
            resultHeight = height
            //if (aPage.getTargetWidth() != resultWidth) {
            //    aPage.setTargetWidth(resultWidth)
            //}

            view.setOnClickListener { clickListener.click(view, position) }
            view.setOnLongClickListener {
                clickListener.longClick(it, position, it)
                return@setOnLongClickListener true
            }

            val bmp = BitmapCache.getInstance().getBitmap(cacheKey)
            if (null != bmp) {
                Log.d("TAG", String.format("bind.hit cache:%s", aPage.index))
                view.setImageBitmap(bmp)
                return
            }

            val callback = object : DecodeCallback {
                override fun decodeComplete(bitmap: Bitmap?, param: DecodeParam) {
                    if (Logcat.loggable) {
                        Logcat.d(
                            String.format(
                                "decode callback:index:%s-%s, decode.page:%s, key:%s, param:%s",
                                param.pageNum, index, param.pageNum, cacheKey, param.key
                            )
                        )
                    }
                    if (param.pageNum == index) {
                        view.setImageBitmap(bitmap)
                    }
                }

                override fun shouldRender(index: Int, param: DecodeParam): Boolean {
                    return this@PdfHolder.index == index
                }
            }
            //aPage 这个如果当参数传递,由于复用机制,后面的页面更新后会把它覆盖,导致解码并不是原来那个
            //这里应该传递高宽值
            val decodeParam = DecodeParam(
                cacheKey,
                view,
                false,
                0,
                aPage,
                mupdfListener.getDocument(),
                callback,
                resultWidth,
                resultHeight
            )
            AppExecutors.instance.diskIO().execute {
                MupdfDocument.decode(aPage, decodeParam)
            }
        }

        private fun setLayoutSize(bitmap: Bitmap) {
            val ratio = bitmap.width * 1f / bitmap.height

            val viewWidth = resultWidth
            val viewHeight: Int = (resultWidth / ratio).toInt()

            var lp = view.layoutParams
            if (null == lp) {
                lp = ViewGroup.LayoutParams(viewWidth, viewHeight)
                view.layoutParams = lp
            } else {
                lp.width = viewWidth
                lp.height = viewHeight
            }
        }
    }

}
