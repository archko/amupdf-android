package cn.archko.pdf.core

import android.app.ActivityManager
import android.app.Application
import cn.archko.pdf.core.cache.BitmapCache
import cn.archko.pdf.core.common.CrashHandler
import cn.archko.pdf.core.common.Graph
import coil.ComponentRegistry
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.tencent.mmkv.MMKV
import vn.chungha.flowbus.FlowBusInitApplication

open class App : Application(), ImageLoaderFactory {
    //private val appkey = "5c15f639f1f556978b0009c8"
    var screenHeight = 720
    var screenWidth = 1080
    override fun onCreate() {
        super.onCreate()
        instance = this
        Graph.provide(this)
        MMKV.initialize(this)
        FlowBusInitApplication.initializer(this)

        Thread.setDefaultUncaughtExceptionHandler(CrashHandler())
        val displayMetrics = resources.displayMetrics
        screenHeight = displayMetrics.heightPixels
        screenWidth = displayMetrics.widthPixels
        //UMConfigure.init(this, appkey, "archko", UMConfigure.DEVICE_TYPE_PHONE, null)
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        //获取整个手机内存
        //获取整个手机内存
        val memoryInfo = ActivityManager.MemoryInfo()
        //最大分配内存
        //最大分配内存
        activityManager.getMemoryInfo(memoryInfo)
        val memory = memoryInfo.totalMem
        val availMem = memoryInfo.availMem
        val threshold = memoryInfo.threshold
        println(
            String.format(
                "memory:%s,availMem:%s,threshold:%s",
                memory * 1.0 / (1024 * 1024),
                availMem * 1.0 / (1024 * 1024),
                threshold * 1.0 / (1024 * 1024)
            )
        )
        //最大分配内存获取方法2
        //最大分配内存获取方法2
        val maxMemory = (Runtime.getRuntime().maxMemory() * 1.0 / (1024 * 1024)).toFloat()
        //当前分配的总内存
        //当前分配的总内存
        val totalMemory = (Runtime.getRuntime().totalMemory() * 1.0 / (1024 * 1024)).toFloat()
        //剩余内存
        //剩余内存
        val freeMemory = (Runtime.getRuntime().freeMemory() * 1.0 / (1024 * 1024)).toFloat()
        println(
            String.format(
                "maxMemory:%s, totalMemory:%s, freeMemory:%s",
                maxMemory,
                totalMemory,
                freeMemory
            )
        )
        BitmapCache.setMaxMemory(totalMemory * 1024 * 1024 * 4 / 5)
    }

    companion object {
        private const val MAX_MEMORY_CACHE_SIZE_PERCENTAGE = 0.3
        private const val MAX_DISK_CACHE_SIZE_PERCENTAGE = 0.2
        var instance: App? = null
            private set
    }

    override fun newImageLoader(): ImageLoader {
        val imageLoader = ImageLoader.Builder(this)
            .components(fun ComponentRegistry.Builder.() {
                //add(PdfiumFetcher.Factory())
                //add(MupdfFetcher.Factory())
            })
            .allowRgb565(true)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(MAX_MEMORY_CACHE_SIZE_PERCENTAGE)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(MAX_DISK_CACHE_SIZE_PERCENTAGE)
                    .build()
            }
            .build()
        return imageLoader
    }
}