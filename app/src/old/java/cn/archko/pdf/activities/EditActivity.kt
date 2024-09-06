package cn.archko.pdf.activities

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.TextUtils
import android.view.Window
import android.view.WindowManager
import cn.archko.pdf.core.common.StatusBarHelper
import cn.archko.pdf.fragments.PdfEditFragment

//import com.umeng.analytics.MobclickAgent

/**
 * @author: archko 2024/9/16 :9:43
 */
class EditActivity : AnalysticActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StatusBarHelper.hideSystemUI(this)
        StatusBarHelper.setImmerseBarAppearance(window, true)
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val path = intent.getStringExtra("path")
        if (TextUtils.isEmpty(path)) {
            finish()
            return
        }
        val pdfEditFragment = PdfEditFragment()
        pdfEditFragment.setPath(path!!)
        supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, pdfEditFragment)
            .commitAllowingStateLoss()
        /*intent.getStringExtra("path")?.let {
            PdfEditFragment.showCreateDialog(it, this, null)
        }*/
    }

    override fun onPause() {
        super.onPause()
        //MobclickAgent.onPageEnd("about")
    }

    override fun onResume() {
        super.onResume()
        //MobclickAgent.onPageStart("about")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    companion object {
        fun start(path: String, context: Context) {
            val intent = Intent(context, EditActivity::class.java)
            intent.putExtra("path", path)
            context.startActivity(intent)
        }
    }
}