package cn.archko.pdf

import android.app.Application
import cn.archko.pdf.common.CrashHandler
import cn.archko.pdf.common.RecentManager
import com.jeremyliao.liveeventbus.LiveEventBus
import com.umeng.commonsdk.UMConfigure

class App : Application() {
    private val appkey = "5c15f639f1f556978b0009c8"
    var screenHeight = 720
    var screenWidth = 1080
    override fun onCreate() {
        super.onCreate()
        instance = this
        uiThread = Thread.currentThread()
        RecentManager.instance.recentTableManager.open()
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

    companion object {
        var instance: App? = null
            private set

        @JvmField
        var uiThread: Thread? = null
    }
}