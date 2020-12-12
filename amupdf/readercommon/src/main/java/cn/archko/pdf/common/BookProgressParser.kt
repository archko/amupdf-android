package cn.archko.pdf.common

import cn.archko.pdf.entity.BookProgress
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*

/**
 * 存储最近阅读的记录
 *
 * @author: archko 2014/4/17 :15:05
 */
object BookProgressParser {
    @JvmStatic
    fun addProgressToJson(progress: BookProgress, ja: JSONArray) {
        val tmp = JSONObject()
        try {
            tmp.put("index", progress.index)
            tmp.put("path", URLEncoder.encode(progress.path))
            tmp.put("name", progress.name)
            tmp.put("ext", progress.ext)
            tmp.put("md5", progress.md5)
            tmp.put("pageCount", progress.pageCount)
            tmp.put("size", progress.size)
            tmp.put("firstTimestampe", progress.firstTimestampe)
            tmp.put("lastTimestampe", progress.lastTimestampe)
            tmp.put("readTimes", progress.readTimes)
            tmp.put("progress", progress.progress)
            tmp.put("page", progress.page)
            tmp.put("zoomLevel", progress.zoomLevel)
            tmp.put("rotation", progress.rotation)
            tmp.put("offsetX", progress.offsetX)
            tmp.put("offsetY", progress.offsetY)
            tmp.put("autoCrop", progress.autoCrop)
            tmp.put("reflow", progress.reflow)
            tmp.put("isFavorited", progress.isFavorited)
            tmp.put("inRecent", progress.inRecent)
            ja.put(tmp)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    @Throws(Exception::class)
    fun parseProgress(jsonobject: JSONObject?): BookProgress? {
        if (null == jsonobject) {
            return null
        }
        val bean = BookProgress()
        try {
            bean.index = jsonobject.optInt("index")
            bean.path = URLDecoder.decode(jsonobject.optString("path"))
            bean.name = jsonobject.optString("name")
            bean.ext = jsonobject.optString("ext")
            bean.md5 = jsonobject.optString("md5")
            bean.pageCount = jsonobject.optInt("pageCount")
            bean.size = jsonobject.optLong("size")
            bean.firstTimestampe = jsonobject.optLong("firstTimestampe")
            bean.lastTimestampe = jsonobject.optLong("lastTimestampe")
            bean.readTimes = jsonobject.optInt("readTimes")
            bean.progress = jsonobject.optInt("progress")
            bean.page = jsonobject.optInt("page")
            bean.zoomLevel = jsonobject.optInt("zoomLevel").toFloat()
            bean.rotation = jsonobject.optInt("rotation")
            bean.offsetX = jsonobject.optInt("offsetX")
            bean.offsetY = jsonobject.optInt("offsetY")
            bean.autoCrop = jsonobject.optInt("autoCrop")
            bean.reflow = jsonobject.optInt("reflow")
            bean.isFavorited = jsonobject.optInt("isFavorited")
            bean.inRecent = jsonobject.optInt("inRecent")
        } catch (jsonexception: Exception) {
            throw Exception(jsonexception.message + ":" + jsonobject, jsonexception)
        }
        return bean
    }

    /**
     * @return
     * @throws WeiboException
     */
    @JvmStatic
    fun parseProgresses(jo: String): ArrayList<BookProgress> {
        val arraylist: ArrayList<BookProgress> = ArrayList<BookProgress>()
        var i = 0
        try {
            val json = JSONObject(jo)
            val jsonarray = json.optJSONArray("root")
            val len = jsonarray.length()
            var bean: BookProgress? = null
            while (i < len) {
                bean = parseProgress(jsonarray.optJSONObject(i))
                arraylist.add(bean!!)
                i++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return arraylist
    }
}