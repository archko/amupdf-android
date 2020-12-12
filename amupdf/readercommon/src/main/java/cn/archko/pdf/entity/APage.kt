package cn.archko.pdf.entity

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
    var pageSize // pagesize of real page
            : PointF? = null
        private set

    /**
     * view zoom
     */
    var zoom = 0f
    private var targetWidth = 0

    /**
     * viewwidth/pagewidth
     */
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
        private set
    var sourceBounds: RectF? = null
        private set
    var cropBounds: RectF? = null
        private set
    private var cropWidth = 0
    private var cropHeight = 0

    constructor() {}
    constructor(pageNumber: Int, pageSize: PointF?, zoom: Float, targetWidth: Int) {
        index = pageNumber
        this.pageSize = pageSize
        this.zoom = zoom
        setTargetWidth(targetWidth)
        initSourceBounds(1.0f)
    }

    private fun initSourceBounds(cropScale: Float) {
        sourceBounds = RectF()
        sourceBounds!!.right = effectivePagesWidth * cropScale * zoom
        sourceBounds!!.bottom = effectivePagesHeight * cropScale * zoom
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

    fun setCropBounds(cropBounds: RectF, cropScale: Float) {
        this.cropBounds = cropBounds
        this.cropScale = cropScale
        initSourceBounds(cropScale)
        setCropWidth(cropBounds.width().toInt())
        setCropHeight(cropBounds.height().toInt())
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

    val cropScaleWidth: Int
        get() {
            if (cropBounds != null) {
                return cropBounds!!.width().toInt()
            }
            if (cropWidth == 0) {
                cropWidth = effectivePagesWidth
            }
            return cropWidth
        }
    val cropScaleHeight: Int
        get() {
            if (cropBounds != null) {
                return cropBounds!!.height().toInt()
            }
            if (cropHeight == 0) {
                cropHeight = effectivePagesHeight
            }
            return cropHeight
        }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val aPage = o as APage
        if (index != aPage.index) return false
        if (java.lang.Float.compare(aPage.zoom, zoom) != 0) return false
        if (targetWidth != aPage.targetWidth) return false
        if (java.lang.Float.compare(aPage.scale, scale) != 0) return false
        if (java.lang.Float.compare(aPage.cropScale, cropScale) != 0) return false
        if (if (pageSize != null) pageSize != aPage.pageSize else aPage.pageSize != null) return false
        if (if (sourceBounds != null) sourceBounds != aPage.sourceBounds else aPage.sourceBounds != null) return false
        return if (cropBounds != null) cropBounds == aPage.cropBounds else aPage.cropBounds == null
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + if (pageSize != null) pageSize.hashCode() else 0
        result = 31 * result + if (zoom != +0.0f) java.lang.Float.floatToIntBits(zoom) else 0
        result = 31 * result + targetWidth
        result = 31 * result + if (scale != +0.0f) java.lang.Float.floatToIntBits(scale) else 0
        result =
            31 * result + if (cropScale != +0.0f) java.lang.Float.floatToIntBits(cropScale) else 0
        result = 31 * result + if (sourceBounds != null) sourceBounds.hashCode() else 0
        result = 31 * result + if (cropBounds != null) cropBounds.hashCode() else 0
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
                ", sourceBounds=" + sourceBounds +
                ", cropBounds=" + cropBounds +
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