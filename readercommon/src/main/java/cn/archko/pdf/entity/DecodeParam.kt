package cn.archko.pdf.entity

import android.text.TextUtils
import android.widget.ImageView
import cn.archko.pdf.common.MupdfDocument
import cn.archko.pdf.listeners.DecodeCallback

/**
 * @author: archko 2024/3/17 :19:41
 */
class DecodeParam {
    var key: String
    var pageNum: Int
    var zoom = 0f
    var screenWidth = 0
    var imageView: ImageView
    var crop = false
    var xOrigin = 0
    var pageSize: APage? = null
    var document: MupdfDocument? = null
    var width = 1080
    var height = 1080
    var decodeCallback: DecodeCallback? = null

    constructor(key: String, pageNum: Int, zoom: Float, screenWidth: Int, imageView: ImageView) {
        this.key = key
        this.pageNum = pageNum
        this.zoom = zoom
        this.screenWidth = screenWidth
        this.imageView = imageView
    }

    constructor(
        key: String, imageView: ImageView, crop: Boolean, xOrigin: Int,
        pageSize: APage, document: MupdfDocument?, callback: DecodeCallback?,
        width: Int, height: Int
    ) {
        this.key = key
        pageNum = pageSize.index
        if (TextUtils.isEmpty(key)) {
            this.key = String.format("%s,%s,%s,%s", imageView, crop, xOrigin, pageSize)
        }
        this.imageView = imageView
        this.crop = crop
        this.xOrigin = xOrigin
        this.pageSize = pageSize
        this.document = document
        decodeCallback = callback
        this.width = width
        this.height = height
    }

    override fun toString(): String {
        return "DecodeParam{" +
                "key='" + key + '\'' +
                ", pageNum=" + pageNum +
                ", zoom=" + zoom +
                ", screenWidth=" + screenWidth +
                ", crop=" + crop +
                ", xOrigin=" + xOrigin +
                ", width=" + width +
                ", height=" + height +
                ", pageSize=" + pageSize +
                ", decodeCallback=" + decodeCallback +
                ", imageView=" + imageView +
                ", document=" + document +
                '}'
    }
}