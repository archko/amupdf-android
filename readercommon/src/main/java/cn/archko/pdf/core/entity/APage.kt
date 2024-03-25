package cn.archko.pdf.core.entity

import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import org.json.JSONException
import org.json.JSONObject

/**
 * 有两个对象,一个是com.artifex.mupdf.fitz.Page,包含了这个页的原始信息.
 * 另一个是缩放值,android.graphics.PointF对象
 * +++++++++++++++++++++++++++++++++++++++++++
 * +              ------------------               +
 * +              -                    -               +
 * +              -                    -               +
 * +              -                    -               +
 * +              -                    -               +
 * +  view       -      page         -     view     +
 * +              -                    -               +
 * +              -                    -               +
 * +              -                    -               +
 * +              -                    -               +
 * +              ------------------               +
 * +++++++++++++++++++++++++++++++++++++++++++
 *
 * @author: archko 2019/2/1 :19:29
 */
class APage {
    /**
     * pageindex
     */
    @JvmField
    var index = 0

    /**
     * width/height
     */
    var pageSize: PointF? = null
        private set

    var width: Float = 0f
    var height: Float = 0f

    /**
     * view zoom
     */
    var zoom = 0f
    private var targetWidth = 0

    var scale = 1f
        private set

    //===================== render a bitmap from mupdf,no crop=======================
    //val ctm = Matrix(pageSize.scale*zoom)
    //var width = pageSize.zoomPoint.x=scale*zoom*mPageSize.x
    //var height = pageSize.zoomPoint.y=scale*zoom*mPageSize.y
    //val bitmap = BitmapPool.getInstance().acquire(width, height)
    //MupdfDocument.render(page, ctm, bitmap, xOrigin, leftBound, topBound);
    //===================== render a bitmap from mupdf,crop white bounds =======================
    //1.get origin page:
    // val ctm = Matrix(pageSize.scale*zoom)
    // var width = pageSize.zoomPoint.x=scale*zoom*mPageSize.x
    // var height = pageSize.zoomPoint.y=scale*zoom*mPageSize.y
    //2.render as a thumb to get rectf.
    // var ratio=6;
    // val thumb = BitmapPool.getInstance().acquire(width / ratio, height / ratio)
    // Matrix m=ctm.scale(1/ratio)
    // MupdfDocument.render(page, m, thumb, 0, 0, 0)
    //3.caculate new width, height,and ctm. new width = viewwidth
    // val rectF = MupdfDocument.getCropRect(thumb)
    // var sscale = thumb.width / rectF.width()
    // val ctm = Matrix(pageSize.scaleZoom * sscale)
    //4.restore to a full bitmap,get bound,scale
    // leftBound = (rectF.left * sscale * ratio)
    // topBound = (rectF.top * sscale * ratio)
    // height = (rectF.height() * sscale * ratio)
    //5.render a crop page
    //val bitmap = BitmapPool.getInstance().acquire(width, height)
    // MupdfDocument.render(page, ctm, bitmap, xOrigin, leftBound, topBound)
    var cropScale = 1.0f
    var sourceBounds: RectF? = null
        private set
    var cropBounds: RectF? = null
        private set
    private var cropWidth = 0
    private var cropHeight = 0

    constructor()
    constructor(pageNumber: Int, pageSize: PointF, zoom: Float, targetWidth: Int) {
        index = pageNumber
        this.pageSize = pageSize
        this.width = pageSize.x
        this.height = pageSize.y

        this.zoom = zoom
        setTargetWidth(targetWidth)
    }
    
    fun getTargetWidth(): Int {
        return targetWidth
    }

    fun setTargetWidth(targetWidth: Int) {
        if (targetWidth > 0 && this.targetWidth != targetWidth) {
            scale = calculateScale(targetWidth)
        }
        this.targetWidth = targetWidth
    }

    private fun calculateScale(tw: Int): Float {
        return 1.0f * tw / pageSize!!.x
    }

    val effectivePagesWidth: Int
        get() = getScaledWidth(pageSize, scale)
    val effectivePagesHeight: Int
        get() = getScaledHeight(pageSize, scale)
    val scaleZoom: Float
        get() = scale * zoom
    val zoomPoint: Point
        get() = getZoomPoint(scaleZoom)

    fun getZoomPoint(scaleZoom: Float): Point {
        return Point((scaleZoom * pageSize!!.x).toInt(), (scaleZoom * pageSize!!.y).toInt())
    }

    fun getCropWidth(): Int {
        if (cropWidth == 0) {
            cropWidth = effectivePagesWidth
        }
        return cropWidth
    }

    fun getCropHeight(): Int {
        if (cropBounds != null) {
            return cropBounds!!.height().toInt()
        }
        if (cropHeight == 0) {
            cropHeight = effectivePagesHeight
        }
        return cropHeight
    }

    fun setCropWidth(cropWidth: Int) {
        this.cropWidth = cropWidth
    }

    fun setCropHeight(cropHeight: Int) {
        this.cropHeight = cropHeight
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as APage

        if (index != other.index) return false
        if (pageSize != other.pageSize) return false
        if (zoom != other.zoom) return false
        if (targetWidth != other.targetWidth) return false
        if (scale != other.scale) return false
        if (cropScale != other.cropScale) return false
        if (sourceBounds != other.sourceBounds) return false
        if (cropBounds != other.cropBounds) return false
        if (cropWidth != other.cropWidth) return false
        if (cropHeight != other.cropHeight) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + (pageSize?.hashCode() ?: 0)
        result = 31 * result + zoom.hashCode()
        result = 31 * result + targetWidth
        result = 31 * result + scale.hashCode()
        result = 31 * result + cropScale.hashCode()
        result = 31 * result + (sourceBounds?.hashCode() ?: 0)
        result = 31 * result + (cropBounds?.hashCode() ?: 0)
        result = 31 * result + cropWidth
        result = 31 * result + cropHeight
        return result
    }

    override fun toString(): String {
        return "APage{" +
                "index=" + index +
                ", mPageSize=" + pageSize +
                ", mZoom=" + zoom +
                ", targetWidth=" + targetWidth +
                ", scale=" + scale +
                ", cropScale=" + cropScale +
                ", cropWidth=" + cropWidth +
                ", cropHeight=" + cropHeight +
                '}'
    }

    fun toJson(): JSONObject {
        val jo = JSONObject()
        try {
            jo.put("index", index)
            jo.put("x", pageSize!!.x.toDouble())
            jo.put("y", pageSize!!.y.toDouble())
            jo.put("zoom", zoom.toDouble())
            jo.put("scale", scale.toDouble())
            jo.put("cropScale", cropScale.toDouble())
            if (sourceBounds != null) {
                jo.put("sbleft", sourceBounds!!.left.toDouble())
                jo.put("sbtop", sourceBounds!!.top.toDouble())
                jo.put("sbright", sourceBounds!!.right.toDouble())
                jo.put("sbbottom", sourceBounds!!.bottom.toDouble())
            }
            if (cropBounds != null) {
                jo.put("cbleft", cropBounds!!.left.toDouble())
                jo.put("cbtop", cropBounds!!.top.toDouble())
                jo.put("cbright", cropBounds!!.right.toDouble())
                jo.put("cbbottom", cropBounds!!.bottom.toDouble())
            }
            if (cropWidth > 0) {
                jo.put("cropWidth", cropWidth)
            }
            if (cropHeight > 0) {
                jo.put("cropHeight", cropHeight)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return jo
    }

    companion object {
        private fun getScaledHeight(page: PointF?, scale: Float): Int {
            return (scale * page!!.y).toInt()
        }

        private fun getScaledWidth(page: PointF?, scale: Float): Int {
            return (scale * page!!.x).toInt()
        }

        @JvmStatic
        fun fromJson(targetWidth: Int, jo: JSONObject): APage {
            val aPage = APage()
            aPage.targetWidth = targetWidth
            aPage.index = jo.optInt("index")
            val x = jo.optInt("x")
            val y = jo.optInt("y")
            aPage.pageSize = PointF(x.toFloat(), y.toFloat())
            aPage.zoom = jo.optDouble("zoom").toFloat()
            aPage.scale = jo.optDouble("scale").toFloat()
            aPage.cropScale = jo.optDouble("cropScale").toFloat()
            val sbleft = jo.optDouble("sbleft").toFloat()
            val sbtop = jo.optDouble("sbtop").toFloat()
            val sbright = jo.optDouble("sbright").toFloat()
            val sbbottom = jo.optDouble("sbbottom").toFloat()
            if (sbright > 0 && sbbottom > 0) {
                aPage.sourceBounds = RectF(sbleft, sbtop, sbright, sbbottom)
            }
            val cbleft = jo.optDouble("cbleft").toFloat()
            val cbtop = jo.optDouble("cbtop").toFloat()
            val cbright = jo.optDouble("cbright").toFloat()
            val cbbottom = jo.optDouble("cbbottom").toFloat()
            if (cbright > 0 && cbbottom > 0) {
                aPage.cropBounds = RectF(cbleft, cbtop, cbright, cbbottom)
            }
            aPage.cropWidth = jo.optInt("cropWidth")
            aPage.cropHeight = jo.optInt("cropHeight")
            return aPage
        }
    }
}