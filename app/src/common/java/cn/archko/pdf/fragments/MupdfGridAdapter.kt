package cn.archko.pdf.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.awidget.ARecyclerView
import androidx.recyclerview.awidget.GridLayoutManager
import cn.archko.pdf.core.cache.BitmapCache
import cn.archko.pdf.core.common.AppExecutors
import cn.archko.pdf.core.listeners.ClickListener
import cn.archko.pdf.decode.DocDecodeService
import org.vudroid.core.DecodeService

/**
 * @author: archko 2023/3/8 :14:34
 */
class MupdfGridAdapter(
    var decodeService: DocDecodeService,
    var context: Context,
    var recyclerView: ARecyclerView,
    private var crop: Boolean,
    var clickListener: ClickListener<View>
) :
    ARecyclerView.Adapter<ARecyclerView.ViewHolder>() {

    override fun getItemCount(): Int {
        return decodeService.getPageCount()
    }

    fun viewWidth(): Int {
        val lm = recyclerView.layoutManager
        if (lm is GridLayoutManager) {
            return recyclerView.width / lm.spanCount
        }
        return recyclerView.width
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ARecyclerView.ViewHolder {
        val width: Int = viewWidth()
        val view = ImageView(context)
            .apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, width)
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
        (holder as PdfHolder).view.setImageBitmap(null)
    }

    fun setCrop(crop: Boolean) {
        if (this.crop != crop) {
            BitmapCache.getInstance().clear()
            this.crop = crop
            notifyDataSetChanged()
        }
    }

    inner class PdfHolder(internal var view: ImageView) : ARecyclerView.ViewHolder(view) {

        private var index = -1
        private fun updateImage(bmp: Bitmap?) {
            if (null != bmp) {
                var width: Int = viewWidth()
                if (width == 0) {
                    width = 1080
                }
                val height: Int = (1f * width * bmp.height / bmp.width).toInt()
                //Log.d(
                //    "TAG",
                //    String.format("updateImage:%s-%s, %s-%s", width, height, bmp.width, bmp.height)
                //)
                var lp = view.layoutParams
                if (null == lp) {
                    lp = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
                    view.layoutParams = lp
                } else {
                    lp.width = width
                    lp.height = height
                    view.layoutParams = lp
                }
            }
            view.setImageBitmap(bmp)
        }

        fun onBind(position: Int) {
            index = position
            val aPage = decodeService.getAPage(position)

            val cacheKey = "crop-$crop-$position-${aPage}"

            view.setOnClickListener { clickListener.click(view, position) }
            view.setOnLongClickListener {
                clickListener.longClick(it, position, it)
                return@setOnLongClickListener true
            }

            val bmp = BitmapCache.getInstance().getBitmap(cacheKey)
            if (null != bmp) {
                Log.d("TAG", String.format("bind.hit cache:%s", aPage.index))
                updateImage(bmp)
                return
            }

            val callback = object : DecodeService.DecodeCallback {
                override fun decodeComplete(bitmap: Bitmap?, param: Boolean, args: Any?) {
                    Log.d(
                        "TAG",
                        String.format(
                            "decode callback:index:%s-%s, w-h:%s-%s, key:%s",
                            index,
                            crop,
                            bitmap?.width,
                            bitmap?.height,
                            cacheKey,
                        )
                    )
                    AppExecutors.instance.mainThread().execute {
                        updateImage(bitmap)
                    }
                }

                override fun shouldRender(index: Int, param: Boolean): Boolean {
                    return this@PdfHolder.index == index
                }
            }

            decodeService.decodePage(
                cacheKey,
                null,
                crop,
                index,
                callback,
                1f,
                RectF(0f, 0f, 1f, 1f),
                0,
                1f
            )
        }
    }
}
