package cn.archko.pdf.activities

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.appcompat.app.AppCompatActivity
import com.umeng.analytics.MobclickAgent

/**
 * @author: archko 2018/12/16 :9:43
 */
open class AnalysticActivity : AppCompatActivity() {

    companion object {

        /**
         * 应用是否已启动
         */
        @JvmStatic
        var isLive = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!isLive) {
            val componentName = ComponentName(
                "cn.archko.mupdf",
                "cn.archko.pdf.activities.ChooseFileFragmentActivity"
            )
            try {
                val intent = Intent()
                intent.component = componentName
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Process.killProcess(Process.myPid())
        } else {
            super.onCreate(savedInstanceState)
        }
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this) // 基础指标统计，不能遗漏
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this) // 基础指标统计，不能遗漏
    }
}