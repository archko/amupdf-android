package cn.archko.pdf.core.cache

import android.widget.ImageView
import android.widget.TextView

/**
 * @author: archko 2019/11/17 :09:35
 */
class ReflowViewCache(
    private val txtCount: Int = TEXT_THRESHOLD,
    private val imgCount: Int = IMAGE_THRESHOLD
) {

    private val cacheTextViews = ArrayList<TextView>(txtCount)
    private val cacheImageViews = ArrayList<ImageView>(imgCount)

    fun textViewCount(): Int {
        return cacheTextViews.size
    }

    fun imageViewCount(): Int {
        return cacheImageViews.size
    }

    fun addTextView(child: TextView) {
        if (cacheTextViews.size > TEXT_THRESHOLD) {
            cacheTextViews.clear()
        }
        child.text = null
        //(child.parent as ViewGroup).removeView(child)
        cacheTextViews.add(child)
    }

    fun addImageView(child: ImageView) {
        if (cacheImageViews.size > IMAGE_THRESHOLD) {
            cacheImageViews.clear()
        }
        child.setImageBitmap(null)
        //(child.parent as ViewGroup).removeView(child)
        cacheImageViews.add(child)
    }

    fun getAndRemoveTextView(i: Int): TextView {
        return cacheTextViews.removeAt(i)
    }

    fun getAndRemoveImageView(i: Int): ImageView {
        return cacheImageViews.removeAt(i)
    }

    fun clear() {
        cacheTextViews.clear()
        cacheImageViews.clear()
    }

    companion object {

        const val TEXT_THRESHOLD = 20
        const val IMAGE_THRESHOLD = 8
    }
}