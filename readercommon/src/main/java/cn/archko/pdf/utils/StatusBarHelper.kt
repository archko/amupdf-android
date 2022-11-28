package cn.archko.pdf.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 *
 */
object StatusBarHelper {

    /**
     * 显示/隐藏 顶部状态栏
     *
     * @param window
     * @param fullScreen
     */
    fun setFullScreenFlag(window: Window, fullScreen: Boolean) {
        if (fullScreen) {
            hideStatusBar(window)
        } else {
            showStatusBar(window)
        }
    }

    fun hideStatusBar(window: Window) {
        val attrs = window.attributes
        attrs.flags = attrs.flags and WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN.inv()
        attrs.flags = attrs.flags or WindowManager.LayoutParams.FLAG_FULLSCREEN
        window.attributes = attrs
        // NavigatiobBar透明切盖在内容上面
//            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    fun showStatusBar(window: Window) {
        val attrs = window.attributes
        attrs.flags = attrs.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN.inv()
        attrs.flags = attrs.flags or WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN
        window.attributes = attrs
    }

    /**
     * 获取状态栏高度
     */
    fun getStatusBarHeight(ctx: Context): Int {
        var h = getStatusBarHeight1(ctx)
        println(String.format("h1:%s", h))
        if (h == 0) {
            h = getStatusBarHeight2(ctx)
            println(String.format("h2:%s", h))
        }
        if (h == 0) {
            h = getStatusBarHeight3(ctx)
            println(String.format("h3:%s", h))
        }
        if (h == 0) {
            h = getStatusBarHeight4(ctx)
            println(String.format("h4:%s", h))
        }
        return h
    }

    private fun getStatusBarHeight1(context: Context): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val windowMetrics = wm.currentWindowMetrics
            val windowInsets = windowMetrics.windowInsets
            val insets =
                windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout())
            return insets.top
        }
        return 0
    }

    private fun getStatusBarHeight2(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    @SuppressLint("PrivateApi")
    private fun getStatusBarHeight3(ctx: Context): Int {
        var statusBarHeight = 0
        try {
            /**
             * 通过反射机制获取StatusBar高度
             */
            val clazz = Class.forName("com.android.internal.R\$dimen")
            val obj = clazz.newInstance()
            val field = clazz.getField("status_bar_height")
            val height = field[obj].toString().toInt()
            /**
             * 设置StatusBar高度
             */
            statusBarHeight = ctx.resources.getDimensionPixelSize(height)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return statusBarHeight
    }

    private fun getStatusBarHeight4(context: Context): Int {
        val statusBarHeight = Math.ceil((38 * context.resources.displayMetrics.density).toDouble())
        return statusBarHeight.toInt()
    }

    fun hideSystemUI(activity: Activity) {
        val window = activity.window

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {   //不加这句,systemBars()调用会导致顶部有一块黑的
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    fun showSystemUI(activity: Activity) {
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
    }

    /**
     * @param activity
     * @param statusBarColor 状态栏的颜色
     */
    fun setBackgroundColor(activity: Activity, @ColorInt statusBarColor: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { //5.0及以上
            val option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            val decorView = activity.window.decorView
            decorView.systemUiVisibility = option
            activity.window.statusBarColor = statusBarColor
        }
    }

    fun setBackgroundColor(window: Window, @ColorInt statusBarColor: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { //5.0及以上
            val option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            val decorView = window.decorView
            decorView.systemUiVisibility = option
            window.statusBarColor = statusBarColor
        }
    }

    fun setColorRes(activity: Activity, @ColorRes statusBarColor: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.window.statusBarColor = activity.resources.getColor(statusBarColor)
        }
    }

    /**
     * 设置顶部状态栏的颜色
     *
     * @param activity    当前的页面
     * @param statusColor 状态栏的颜色值
     */
    fun setStatusBarColor(activity: Activity, statusColor: Int) {
        val window = activity.window
        //取消状态栏透明
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        //添加Flag把状态栏设为可绘制模式
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        //设置状态栏颜色
        window.statusBarColor = statusColor
        // 如果亮色，设置状态栏文字为黑色
        if (isLightColor(statusColor)) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            //设置系统状态栏处于可见状态
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
        //让view不根据系统窗口来调整自己的布局
        val mContentView = window.findViewById<View>(Window.ID_ANDROID_CONTENT) as ViewGroup
        val mChildView = mContentView.getChildAt(0)
        if (mChildView != null) {
            mChildView.fitsSystemWindows = false
            ViewCompat.requestApplyInsets(mChildView)
        }
    }

    /**
     * 判断当前的状态栏是不是亮色
     *
     * @param color 当前的颜色
     * @return
     */
    fun isLightColor(@ColorInt color: Int): Boolean {
        return ColorUtils.calculateLuminance(color) >= 0.5
    }

    val isSystemUiFullscreen: Unit
        get() {}

    fun setTextAndIconDark(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    fun setTextAndIconLight(activity: Activity) {
        activity.window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    /**
     * 设置沉浸式状态栏
     *
     * @param window
     */
    fun setStatusBarImmerse(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        //设置专栏栏和导航栏的底色，透明
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.navigationBarDividerColor = Color.TRANSPARENT
        }
    }

    /**
     * 设置沉浸后状态栏和导航字体的颜色
     *
     * @param window
     * @param isLight true,则字体是黑色的,false则白色
     */
    fun setImmerseBarAppearance(window: Window, isLight: Boolean) {
        val controller = ViewCompat.getWindowInsetsController(window.decorView)
        if (null != controller) {
            controller.isAppearanceLightStatusBars = isLight
            controller.isAppearanceLightNavigationBars = isLight
        }
    }
}