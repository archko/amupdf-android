package cn.archko.pdf.common

import android.widget.ImageView
import android.widget.TextView

/**
 * @author: archko 2019/11/17 :09:35
 */
public class ReflowViewCache {

    private val cacheTextViews = ArrayList<TextView>(22)
    private val cacheImageViews = ArrayList<ImageView>(10)

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
        child.setText(null)
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

    fun getTextView(i: Int): TextView {
        return cacheTextViews.removeAt(i)
    }

    fun getImageView(i: Int): ImageView {
        return cacheImageViews.removeAt(i)
    }

    fun clear() {
        cacheTextViews.clear()
        cacheImageViews.clear()
    }

    companion object {

        const val TEXT_THRESHOLD = 20;
        const val IMAGE_THRESHOLD = 8;
    }
}