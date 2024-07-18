package cn.archko.pdf.core.entity

import android.graphics.Rect
import org.json.JSONException
import org.json.JSONObject

/**
 *
 * @author: archko 2024/3/9 :19:29
 */
class APage {
    /**
     * pageindex
     */
    @JvmField
    var index = 0

    var width: Float = 0f
    var height: Float = 0f

    /**
     * view zoom
     */
    var zoom = 1f

    /**
     * pdf.page.width/viewWidth,page scale to view
     */
    //var scale = 1f
    //    private set

    /**
     * after crop page, scale to view
     */
    var cropScale = 1.0f
        private set

    var cropBounds: Rect? = null
        private set

    constructor() {}
    constructor(pageNumber: Int, width: Float, height: Float, zoom: Float) {
        index = pageNumber
        this.width = width
        this.height = height
        this.zoom = zoom
    }

    fun getWidth(crop: Boolean): Float {
        if (crop && cropBounds != null) {
            return cropBounds!!.width().toFloat()
        }
        return width
    }

    fun getHeight(crop: Boolean): Float {
        if (crop && cropBounds != null) {
            return cropBounds!!.height().toFloat()
    }
        return height
    }

    fun setCropBounds(cropBounds: Rect) {
        this.cropBounds = cropBounds
        this.cropScale = cropBounds.width() / width
    }

    fun getCropWidth(): Int {
        if (cropBounds == null) {
            return width.toInt()
        }
        return cropBounds!!.width()
    }

    fun getCropHeight(): Int {
        if (cropBounds == null) {
            return height.toInt()
        }
        return cropBounds!!.height()
    }

    override fun toString(): String {
        return "APage(index=$index, width=$width, height=$height, zoom=$zoom, cropScale=$cropScale, cropBounds=$cropBounds)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as APage

        if (index != other.index) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (zoom != other.zoom) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + width.hashCode()
        result = 31 * result + height.hashCode()
        result = 31 * result + zoom.hashCode()
        return result
    }

    companion object {

        fun calculateScale(viewWidth: Int, width: Float): Float {
            return 1.0f * viewWidth / width
    }

        @JvmStatic
        fun toJson(aPage: APage): JSONObject {
        val jo = JSONObject()
        try {
                jo.put("index", aPage.index)
                jo.put("width", aPage.width.toDouble())
                jo.put("height", aPage.height.toDouble())
                jo.put("zoom", aPage.zoom.toDouble())
                //jo.put("scale", aPage.scale.toDouble())
                if (aPage.cropBounds != null) {
                    jo.put("cbleft", aPage.cropBounds!!.left)
                    jo.put("cbtop", aPage.cropBounds!!.top)
                    jo.put("cbright", aPage.cropBounds!!.right)
                    jo.put("cbbottom", aPage.cropBounds!!.bottom)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return jo
    }

        @JvmStatic
        fun fromJson(jo: JSONObject): APage {
            val aPage = APage()
            aPage.index = jo.optInt("index")
            aPage.width = jo.optDouble("width").toFloat()
            aPage.height = jo.optDouble("height").toFloat()
            aPage.zoom = jo.optDouble("zoom").toFloat()
            //aPage.scale = jo.optDouble("scale").toFloat()
            val cbleft = jo.optInt("cbleft")
            val cbtop = jo.optInt("cbtop")
            val cbright = jo.optInt("cbright")
            val cbbottom = jo.optInt("cbbottom")
            if (cbright > 0 && cbbottom > 0) {
                aPage.cropBounds = Rect(cbleft, cbtop, cbright, cbbottom)
            }
            return aPage
        }
    }

}