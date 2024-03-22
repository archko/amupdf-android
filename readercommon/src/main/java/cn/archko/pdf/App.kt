package cn.archko.pdf

import android.app.ActivityManager
import android.app.Application
import cn.archko.pdf.common.BitmapCache
import cn.archko.pdf.common.CrashHandler
import cn.archko.pdf.common.Graph
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.jeremyliao.liveeventbus.LiveEventBus
import com.tencent.mmkv.MMKV


class App : Application(), ImageLoaderFactory {
    //private val appkey = "5c15f639f1f556978b0009c8"
    var screenHeight = 720
    var screenWidth = 1080
    override fun onCreate() {
        super.onCreate()
        instance = this
        Graph.provide(this)
        MMKV.initialize(this)

        Thread.setDefaultUncaughtExceptionHandler(CrashHandler())
        val displayMetrics = resources.displayMetrics
        screenHeight = displayMetrics.heightPixels
        screenWidth = displayMetrics.widthPixels
        //UMConfigure.init(this, appkey, "archko", UMConfigure.DEVICE_TYPE_PHONE, null)
        LiveEventBus
            .config()
            .lifecycleObserverAlwaysActive(true)
            .autoClear(false)

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
        BitmapCache.setMaxMemory(totalMemory * 1024 * 1024 / 2)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .build()
    }

    companion object {
        var instance: App? = null
            private set

        @JvmStatic
        val PDF_PREFERENCES_NAME = "amupdf_preferences"
    }
}