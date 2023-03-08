package cn.archko.pdf

import android.app.Application
import cn.archko.pdf.common.CrashHandler
import cn.archko.pdf.common.Graph
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.jeremyliao.liveeventbus.LiveEventBus
import com.umeng.commonsdk.UMConfigure

class App : Application(), ImageLoaderFactory {
    private val appkey = "5c15f639f1f556978b0009c8"
    var screenHeight = 720
    var screenWidth = 1080
    override fun onCreate() {
        super.onCreate()
        instance = this
        Graph.provide(this)

        Thread.setDefaultUncaughtExceptionHandler(CrashHandler())
        val displayMetrics = resources.displayMetrics
        screenHeight = displayMetrics.heightPixels
        screenWidth = displayMetrics.widthPixels
        UMConfigure.init(this, appkey, "archko", UMConfigure.DEVICE_TYPE_PHONE, null)
        LiveEventBus
            .config()
            .supportBroadcast(this)
            .lifecycleObserverAlwaysActive(true)
            .autoClear(false)
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