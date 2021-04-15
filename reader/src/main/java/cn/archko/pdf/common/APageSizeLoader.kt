package cn.archko.pdf.common

import android.text.TextUtils
import android.util.SparseArray
import cn.archko.pdf.common.Logcat.d
import cn.archko.pdf.entity.APage
import cn.archko.pdf.entity.APage.Companion.fromJson
import cn.archko.pdf.utils.StreamUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * @author: archko 2020/1/1 :9:04 下午
 */
object APageSizeLoader {
    const val PAGE_COUNT = 250
    fun loadPageSizeFromFile(
        targetWidth: Int,
        pageCount: Int,
        fileSize: Long,
        file: File?
    ): PageSizeBean? {
        var pageSizeBean: PageSizeBean? = null
        try {
            val content = StreamUtils.readStringFromFile(file)
            if (!TextUtils.isEmpty(content)) {
                pageSizeBean = fromJson(targetWidth, pageCount, fileSize, JSONObject(content))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return pageSizeBean
    }

    fun savePageSizeToFile(
        crop: Boolean,
        fileSize: Long,
        sparseArray: SparseArray<APage>,
        file: File?
    ) {
        val content = toJson(crop, fileSize, sparseArray)
        StreamUtils.saveStringToFile(content, file)
    }

    fun fromJson(targetWidth: Int, pageCount: Int, fileSize: Long, jo: JSONObject): PageSizeBean? {
        val ja = jo.optJSONArray("pagesize")
        if (ja.length() != pageCount) {
            d("new pagecount:$pageCount")
            return null
        }
        if (fileSize != jo.optLong("filesize")) {
            d("new filesize:$fileSize")
            return null
        }
        val pageSizeBean = PageSizeBean()
        val sparseArray = fromJson(targetWidth, ja)
        pageSizeBean.sparseArray = sparseArray
        pageSizeBean.crop = jo.optBoolean("crop")
        return pageSizeBean
    }

    fun fromJson(targetWidth: Int, ja: JSONArray): SparseArray<APage> {
        val sparseArray = SparseArray<APage>()
        for (i in 0 until ja.length()) {
            sparseArray.put(i, fromJson(targetWidth, ja.optJSONObject(i)))
        }
        return sparseArray
    }

    fun toJson(crop: Boolean, fileSize: Long, sparseArray: SparseArray<APage>): String {
        val jo = JSONObject()
        try {
            jo.put("crop", crop)
            jo.put("filesize", fileSize)
            jo.put("pagesize", toJson(sparseArray))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return jo.toString()
    }

    fun toJson(sparseArray: SparseArray<APage>): JSONArray {
        val jsonArray = JSONArray()
        var aPage: APage
        for (i in 0 until sparseArray.size()) {
            aPage = sparseArray.valueAt(i)
            jsonArray.put(aPage.toJson())
        }
        return jsonArray
    }

    class PageSizeBean {
        var sparseArray: SparseArray<APage>? = null
        var crop = false
        var fileSize = 0
    }
}