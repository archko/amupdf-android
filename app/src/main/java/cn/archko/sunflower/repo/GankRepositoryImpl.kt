package cn.archko.sunflower.repo

import GankBean
import GankResponse
import androidx.annotation.WorkerThread
import cn.archko.sunflower.api.GankApi
import cn.archko.sunflower.ui.utils.VLog

class GankRepositoryImpl(
    private val cryptoApi: GankApi,
) {

    @WorkerThread
    suspend fun getGankResponse(page: Int, pageSize: Int): GankResponse<MutableList<GankBean>>? {
        VLog.d("getGank:$page")
        val response = cryptoApi.getGankGirls("Girl", "Girl", page, pageSize)
        return if (response.isSuccessful && null != response.body()) {
            val cryptoApiResponseList = response.body()
            cryptoApiResponseList
        } else {
            GankResponse<MutableList<GankBean>>(0, 0, 0, 0, "", "", mutableListOf())
        }
    }
}