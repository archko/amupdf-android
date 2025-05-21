package cn.archko.pdf.controller

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.awidget.ARecyclerView
import androidx.recyclerview.awidget.GridLayoutManager
import cn.archko.pdf.core.cache.BitmapCache
import cn.archko.pdf.core.common.AppExecutors
import cn.archko.pdf.core.listeners.ClickListener
import cn.archko.pdf.decode.DocDecodeService
import cn.archko.pdf.decode.DocDecodeService.IView
import org.vudroid.core.DecodeService
import org.vudroid.core.DecodeServiceBase
import org.vudroid.core.codec.CodecDocument

/**
 * @author: archko 2025/5/8 :14:34
 */
class ThumbnailView(
    private val context: Context,
    private val recyclerView: ARecyclerView,
    private val clickListener: ClickListener<View>
) {

    private var decodeService: DocDecodeService? = null
    private var pdfAdapter: MupdfAdapter? = null
    private var path: String? = null
    private var crop: Boolean = false
    var document: CodecDocument? = null
    private var selectedPosition: Int = 0

    private fun loadDoc() {
        val iView = object : IView {
            override fun getWidth(): Int {
                return recyclerView.width
            }

            override fun getHeight(): Int {
                return recyclerView.height
            }
        }
        path?.let {
            val codecContext = DecodeServiceBase.openContext(it)
            if (null == codecContext) {
                Toast.makeText(context, "open file error", Toast.LENGTH_SHORT).show()
                return@let
            }
            decodeService = DocDecodeService(codecContext)
            decodeService!!.setContainerView(iView)
            pdfAdapter = MupdfAdapter(
                decodeService!!,
                context,
                recyclerView,
                crop,
                clickListener
            )
            recyclerView.layoutManager = GridLayoutManager(context, 1)
            recyclerView.adapter = pdfAdapter
            recyclerView.setHasFixedSize(true)

            document = decodeService!!.open(it, true)
            pdfAdapter?.notifyDataSetChanged()
        }
    }

    fun gotoPage(position: Int) {
        if (null != document) {
            scrollToPosition(position)
        } else {
            loadDoc()
            scrollToPosition(position)
        }
    }

    private fun scrollToPosition(position: Int){
        if (selectedPosition == position){
            return
        }
        this.selectedPosition = position
        pdfAdapter?.setPosition(position)
        recyclerView.scrollToPosition(position)
        pdfAdapter?.notifyDataSetChanged()
    }

    fun setPath(path: String) {
        this.path = path
    }

    private class MupdfAdapter(
        var decodeService: DocDecodeService,
        var context: Context,
        var recyclerView: ARecyclerView,
        private var crop: Boolean,
        var clickListener: ClickListener<View>
    ) :
        ARecyclerView.Adapter<ARecyclerView.ViewHolder>() {

        private var selectedPosition = 0

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

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ARecyclerView.ViewHolder {
            val width: Int = viewWidth()
            val view = AImageView(context)
                .apply {
                    layoutParams =
                        ViewGroup.LayoutParams(width, width)
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

        private val selectedPaint = selectedPaint()

        private fun selectedPaint(): Paint {
            val paint = Paint()
            paint.setColor(Color.RED)
            paint.setStyle(Paint.Style.STROKE)
            paint.strokeWidth = 6f
            return paint
        }

        fun setPosition(i: Int) {
            selectedPosition = i
        }

        inner class AImageView(context: Context) :
            androidx.appcompat.widget.AppCompatImageView(context) {
            var isSelectedPos = false
            val rect = Rect(3, 3, width - 3, height - 3)
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                if (isSelectedPos) {
                    rect.set(3, 3, width - 3, height - 3)
                    canvas.drawRect(rect, selectedPaint)
                }
            }
        }

        inner class PdfHolder(var view: AImageView) : ARecyclerView.ViewHolder(view) {

            private var index = -1
            private fun updateImage(bmp: Bitmap?) {
                if (null != bmp) {
                    var width: Int = viewWidth()
                    if (width == 0) {
                        width = thumbnail_width
                    }
                    val height: Int = (1f * width * bmp.height / bmp.width).toInt()
                    var lp = view.layoutParams
                    if (null == lp) {
                        lp = ViewGroup.LayoutParams(width, height)
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
                view.isSelectedPos = (selectedPosition == position)
                val aPage = decodeService.getAPage(position)

                val cacheKey = "crop-$crop-$position-${aPage}"

                view.setOnClickListener { clickListener.click(view, position) }

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

    companion object {

        const val TAG = "ThumbnailView"
        private const val thumbnail_width = 256
    }
}
