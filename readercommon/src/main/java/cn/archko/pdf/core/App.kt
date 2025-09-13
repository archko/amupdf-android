package cn.archko.pdf.core

import android.app.ActivityManager
import android.app.Application
import cn.archko.pdf.core.cache.BitmapCache
import cn.archko.pdf.core.common.CrashHandler
import cn.archko.pdf.core.common.Graph
import com.tencent.bugly.library.Bugly
import com.tencent.bugly.library.BuglyBuilder
import com.tencent.mmkv.MMKV
import vn.chungha.flowbus.FlowBusInitApplication

open class App : Application() {
    var screenHeight = 2160
    var screenWidth = 1080
    override fun onCreate() {
        super.onCreate()
        instance = this
        Graph.provide(this)
        MMKV.initialize(this)
        FlowBusInitApplication.initializer(this)
        val appId = "aaeff47e9b"
        val appKey = "b7958f11-2a32-4ba8-ae82-f73f89108b75"
        val builder = BuglyBuilder(appId, appKey)
        Bugly.init(applicationContext, builder, false)

        Thread.setDefaultUncaughtExceptionHandler(CrashHandler())
        val displayMetrics = resources.displayMetrics
        screenHeight = displayMetrics.heightPixels
        screenWidth = displayMetrics.widthPixels
        //UMConfigure.init(this, appkey, "archko", UMConfigure.DEVICE_TYPE_PHONE, null)
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        //获取整个手机内存
        val memoryInfo = ActivityManager.MemoryInfo()
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
        val maxMemory = (Runtime.getRuntime().maxMemory() * 1.0 / (1024 * 1024)).toFloat()
        //当前分配的总内存
        val totalMemory = (Runtime.getRuntime().totalMemory() * 1.0 / (1024 * 1024)).toFloat()
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
        var instance: App? = null
            private set
    }
}