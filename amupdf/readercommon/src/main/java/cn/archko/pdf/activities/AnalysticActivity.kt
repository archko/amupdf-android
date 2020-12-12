package cn.archko.pdf.activities

import androidx.appcompat.app.AppCompatActivity
import com.umeng.analytics.MobclickAgent

/**
 * @author: archko 2018/12/16 :9:43
 */
open class AnalysticActivity : AppCompatActivity() {
    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this) // 基础指标统计，不能遗漏
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this) // 基础指标统计，不能遗漏
    }
}