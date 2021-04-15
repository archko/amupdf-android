package cn.archko.sunflower

import android.app.Application
import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.util.DebugLogger
import okhttp3.Cache
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.File

/**
 * @author: archko 2021/3/5 :15:48
 */
class App : Application(), ImageLoaderFactory {

    init {
        instance = requireNotNull(this)
    }

    companion object {
        private lateinit var instance: App

        fun applicationContext(): Context {
            return instance
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .availableMemoryPercentage(0.25) // Use 25% of the application's available memory.
            .crossfade(true) // Show a short crossfade when loading images from network or disk.
            .componentRegistry {
                // GIFs
                //if (SDK_INT >= 28) {
                //    add(ImageDecoderDecoder(this@Application))
                //} else {
                //    add(GifDecoder())
                //}

                // SVGs
                //add(SvgDecoder(this@Application))

                // Video frames
                //add(VideoFrameFileFetcher(this@Application))
                //add(VideoFrameUriFetcher(this@Application))
                //add(VideoFrameDecoder(this@Application))
            }
            .okHttpClient {
                // Create a disk cache with "unlimited" size. Don't do this in production.
                // To create the an optimized Coil disk cache, use CoilUtils.createDefaultCache(context).
                val cacheDirectory = File(filesDir, "image_cache").apply { mkdirs() }
                val cache = Cache(cacheDirectory, 100 * 1024 * 1024)

                // Rewrite the Cache-Control header to cache all responses for a year.
                val cacheControlInterceptor =
                    ResponseHeaderInterceptor("Cache-Control", "max-age=31536000,public")

                // Don't limit concurrent network requests by host.
                val dispatcher = Dispatcher().apply { maxRequestsPerHost = maxRequests }

                // Lazily create the OkHttpClient that is used for network operations.
                OkHttpClient.Builder()
                    .cache(cache)
                    .dispatcher(dispatcher)
                    //.forceTls12() // The Unsplash API requires TLS 1.2, which isn't enabled by default before API 21.
                    .addNetworkInterceptor(cacheControlInterceptor)
                    .build()
            }
            .apply {
                // Enable logging to the standard Android log if this is a debug build.
                logger(DebugLogger(Log.WARN))
            }
            .build()
    }
}

class ResponseHeaderInterceptor(
    private val name: String,
    private val value: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        return response.newBuilder().header(name, value).build()
    }
}