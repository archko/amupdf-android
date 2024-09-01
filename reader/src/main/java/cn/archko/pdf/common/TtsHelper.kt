package cn.archko.pdf.common

import android.text.TextUtils
import android.util.Log
import cn.archko.pdf.core.entity.ReflowBean
import cn.archko.pdf.core.utils.FileUtils
import cn.archko.pdf.core.utils.StreamUtils
import cn.archko.pdf.entity.TtsBean
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * @author: archko 2024/9/1 :19:40
 */
object TtsHelper {

    fun loadFromFile(
        pageCount: Int,
        path: String,
    ): TtsBean? {
        var bean: TtsBean? = null
        try {
            val saveFile = getSaveFile(File(path))
            val content = StreamUtils.readStringFromFile(saveFile)
            if (!TextUtils.isEmpty(content)) {
                bean = fromJson(pageCount, path, JSONObject(content))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bean
    }

    fun saveToFile(
        pageCount: Int,
        path: String,
        list: List<ReflowBean>,
    ) {
        val file = File(path)
        val saveFile = getSaveFile(file)
        val content = toJson(pageCount, path, list)
        StreamUtils.saveStringToFile(content, saveFile)
    }

    private fun fromJson(pageCount: Int, path: String, jo: JSONObject): TtsBean? {
        val ja = jo.optJSONArray("reflow")
        val pc = jo.optInt("pc")
        if (null == ja || pc != pageCount) {
            Log.d("TAG", "new page count:$pageCount")
            return null
        }
        if (path != jo.optString("path")) {
            Log.d("TAG", "new path:$path")
            return null
        }
        val list = fromJson(ja)
        val bean = TtsBean(path, pageCount, list)
        return bean
    }

    private fun fromJson(ja: JSONArray): List<ReflowBean> {
        val list = mutableListOf<ReflowBean>()
        for (i in 0 until ja.length()) {
            val obj = ja.optJSONObject(i)
            val reflowBean = ReflowBean(obj.optString("data"), page = obj.optString("page"))
            list.add(reflowBean)
        }
        return list
    }

    private fun toJson(pc: Int, path: String, list: List<ReflowBean>): String {
        val jo = JSONObject()
        try {
            jo.put("pc", pc)
            jo.put("path", path)
            jo.put("reflow", toJsons(list))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return jo.toString()
    }

    private fun toJsons(list: List<ReflowBean>): JSONArray {
        val jsonArray = JSONArray()
        for (bean in list) {
            val jo=JSONObject()
            jo.put("page", bean.page)
            jo.put("data", bean.data)
            jsonArray.put(jo)
        }
        return jsonArray
    }

    fun deleteFromFile(path: String) {
        val file = File(path)
        val saveFile = getSaveFile(file)
        if (saveFile.exists()) {
            saveFile.delete()
        }
    }

    private fun getSaveFile(file: File) = File(
        FileUtils.getStorageDirPath() + "/amupdf"
                + File.separator + "page" + File.separator + file.nameWithoutExtension + "_tts.json"
    )
}