package cn.archko.pdf.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.ImageView
import com.artifex.mupdf.fitz.Link

/**
 * @author: archko 2018/12/16 :9:43
 */
class AImageView : ImageView {

    internal val searchPaint = Paint()
    internal val linkPaint = Paint()
    private var mSearchBoxes: Array<RectF>? = null
    protected var mLinks: Array<Link>? = null
    private val mHighlightLinks: Boolean = false
    internal var scale = 1.0f// mSourceScale * (float) getWidth() / (float) mSize.x;

    fun setScale(scale: Float) {
        this.scale = scale
    }

    fun setSearchBoxes(mSearchBoxes: Array<RectF>) {
        this.mSearchBoxes = mSearchBoxes
    }

    fun setLinks(mLinks: Array<Link>) {
        this.mLinks = mLinks
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    internal fun init() {
        searchPaint.color = HIGHLIGHT_COLOR
        linkPaint.color = LINK_COLOR
    }

    internal fun drawSearchBox(canvas: Canvas) {
        if (drawable != null && mSearchBoxes != null) {
            for (rect in mSearchBoxes!!)
                canvas.drawRect(
                    rect.left * scale, rect.top * scale,
                    rect.right * scale, rect.bottom * scale,
                    searchPaint
                )
        }
    }

    internal fun drawLink(canvas: Canvas) {
        if (drawable != null && mLinks != null && mHighlightLinks) {
            for (link in mLinks!!)
                canvas.drawRect(
                    link.bounds.x0 * scale, link.bounds.y0 * scale,
                    link.bounds.x1 * scale, link.bounds.y1 * scale,
                    linkPaint
                )
        }
    }

    companion object {

        private val HIGHLIGHT_COLOR = -0x7f339a00
        private val LINK_COLOR = -0x7fff9934
    }
}
