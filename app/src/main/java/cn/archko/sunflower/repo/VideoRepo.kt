package cn.archko.sunflower.repo

import GankBean
import GankResponse
import cn.archko.sunflower.http.OkHttpSingleton
import cn.archko.sunflower.http.WebConfig
import cn.archko.sunflower.model.ACategory
import cn.archko.sunflower.ui.utils.JsonUtils
import cn.archko.sunflower.ui.utils.VLog
import com.google.gson.reflect.TypeToken
import okhttp3.Request
import okhttp3.Response

/**
 * A   repo
 */
object VideoRepo {

    fun getACategory(): List<ACategory>? {
        val request: Request = Request
            .Builder()
            .url(WebConfig.categoriesUrl)
            .build()
        var res: Response? = null
        try {
            res = OkHttpSingleton.getInstance().execute(request)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val content = res?.body?.string()
        VLog.d("res,content:$content")
        return JsonUtils.fromJson(content, object : TypeToken<List<ACategory?>?>() {}.type)
    }

    fun getGankGirls(pageIndex: Int): GankResponse<MutableList<GankBean>> {
        val pn = pageIndex
        val category = "Girl"
        val type = "Girl"

        val url =
            "https://gank.io/api/v2/data/category/$category/type/$type/page/$pn/count/15";
        val request: Request = Request
            .Builder()
            .url(url)
            .build()
        var res: Response? = null
        try {
            res = OkHttpSingleton.getInstance().execute(request)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val content = res?.body?.string()
        VLog.d("repo,url:$url")
        VLog.d("repo,content:$content")
        return JsonUtils.parseGankResponse(content)
    }
}