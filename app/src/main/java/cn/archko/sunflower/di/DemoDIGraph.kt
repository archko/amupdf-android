package cn.archko.sunflower.di

import cn.archko.sunflower.repo.GankRepositoryImpl
import cn.archko.sunflower.App
import cn.archko.sunflower.api.GankApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object DemoDIGraph {

    private const val timeOut = 20L
    private const val BASE_URL = "https://gank.io/api/v2/data/category/"
    private val context = App.applicationContext()

    // create retrofit
    private val okHttpClient by lazy {
        OkHttpClient.Builder().apply {
            readTimeout(timeOut, TimeUnit.SECONDS)
            connectTimeout(timeOut, TimeUnit.SECONDS)
        }.build()
    }

    private val retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val gankApi = retrofit.create(GankApi::class.java)

    //pass dependencies to repository
    val gankRepository by lazy {
        GankRepositoryImpl(gankApi)
    }

    val mainDispatcher: CoroutineDispatcher
        get() = Dispatchers.Main

    val ioDispatcher: CoroutineDispatcher
        get() = Dispatchers.IO

}