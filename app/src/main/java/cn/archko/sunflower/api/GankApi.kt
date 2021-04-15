package cn.archko.sunflower.api

import GankBean
import GankResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface GankApi {

    @GET("{category}/type/{type}/page/{pn}/count/{per_page}")
    suspend fun getGankGirls(
        @Path("category") category: String,
        @Path("type") type: String,
        @Path("pn") pn: Int = 1,
        @Path("per_page") pageSize: Int = 20
    ): Response<GankResponse<MutableList<GankBean>>>
}