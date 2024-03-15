package cn.archko.pdf.fragments

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.awidget.ARecyclerView
import androidx.recyclerview.awidget.GridLayoutManager
import cn.archko.pdf.AppExecutors
import cn.archko.pdf.common.BitmapCache
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.entity.APage
//import cn.archko.pdf.entity.DecodeTask
import cn.archko.pdf.listeners.ClickListener
import cn.archko.pdf.listeners.DecodeCallback

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

    inner class PdfHolder(internal var view: ImageView) : ARecyclerView.ViewHolder(view),
        DecodeCallback {

        private var aPage: APage? = null
        private var pageIndex = -1
        private var resultWidth: Int = 1080
        private var resultHeight: Int = 1080
        fun onBind(position: Int) {
            pageIndex = position
            aPage = mupdfListener.getPageList()[position]

            val key =
                "${mupdfListener.getDocument()!!}_page_$position-${aPage}"

            var width: Int = viewWidth()
            if (width == 0) {
                width = 1080
            }
            val height: Int = (width * 4 / 3f).toInt()
            resultWidth = width
            resultHeight = height

            view.setOnClickListener { clickListener.click(view, position) }
            view.setOnLongClickListener {
                clickListener.longClick(it, position, it)
                //showPopupMenu(it, position)
                return@setOnLongClickListener true
            }

            val bitmap = BitmapCache.getInstance().getBitmap(key)
            if (null != bitmap) {
                Log.d("TAG", String.format("bind.hit cache:%s", aPage?.index))
                view.setImageBitmap(bitmap)
                setLayoutSize(bitmap)
                return
            }
            /*val task =
                DecodeTask(
                    width, height, 1,
                    position, aPage!!,
                    false,
                    this,
                    mupdfListener.getDocument()
                )

            view.setImageDrawable(null)
            AppExecutors.instance.diskIO().execute { task.run() }*/
        }

        private fun setLayoutSize(bitmap: Bitmap) {
            val ratio = bitmap.width * 1f / bitmap.height

            val viewWidth = resultWidth
            val viewHeight: Int = (resultWidth / ratio).toInt()
            /*if (Logcat.loggable) {
                Logcat.d(
                    TAG, String.format(
                        "decode layout:index:%s, w-h:%s-%s, %s, %s",
                        pageIndex, viewWidth, viewHeight, resultHeight, ratio
                    )
                )
            }*/

            var lp = view.layoutParams
            if (null == lp) {
                lp = ViewGroup.LayoutParams(viewWidth, viewHeight)
                view.layoutParams = lp
            } else {
                lp.width = viewWidth
                lp.height = viewHeight
            }
        }

        /*override fun decodeComplete(bitmap: Bitmap?, position: Int, key: String) {
            if (null != bitmap) {
                BitmapCache.getInstance().addBitmap(key, bitmap)
                if (Logcat.loggable) {
                    Logcat.d(
                        "TAG", String.format(
                            "decode complete:index:%s,pageIndex:%s, %s, %s-%s",
                            position, pageIndex, key, bitmap.width, bitmap.height,
                        )
                    )
                }
                if (position == pageIndex) {
                    AppExecutors.instance.mainThread().execute {
                        view.setImageBitmap(bitmap)
                        setLayoutSize(bitmap!!)
                    }
                }
            }
        }

        override fun shouldRender(index: Int, key: String?): Boolean {
            return pageIndex != index
        }*/

        override fun decodeComplete(bitmap: Bitmap?) {
        }
    }

}
