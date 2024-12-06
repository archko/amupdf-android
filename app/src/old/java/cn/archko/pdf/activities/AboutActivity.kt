package cn.archko.pdf.activities

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.webkit.WebView
import android.widget.TabHost
import cn.archko.mupdf.R
import cn.archko.pdf.core.utils.FileUtils
import cn.archko.pdf.core.utils.LengthUtils
import com.google.android.material.appbar.MaterialToolbar

//import com.umeng.analytics.MobclickAgent

/**
 * @author: archko 2018/12/16 :9:43
 */
class AboutActivity : AnalysticActivity() {

    private lateinit var browserTabHost: TabHost

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.about)

        val appView = initAppView()
        val updateView = initUpdateView()
        val thirdPatyView = initThirdpatyView()

        browserTabHost = findViewById(R.id.browserTabHost)
        browserTabHost.setup()

        browserTabHost.addTab(
            browserTabHost
                .newTabSpec("App")
                .setIndicator(getString(cn.archko.pdf.R.string.tab_about_app))
                .setContent { appView }
        )
        browserTabHost.addTab(
            browserTabHost
                .newTabSpec("Changelog")
                .setIndicator(getString(cn.archko.pdf.R.string.tab_about_changelog))
                .setContent { updateView }
        )
        browserTabHost.addTab(
            browserTabHost
                .newTabSpec("Thirdpaty")
                .setIndicator(getString(cn.archko.pdf.R.string.tab_about_thirdpaty))
                .setContent { thirdPatyView }
        )

        var name = "Dragon"
        var version = ""
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            version = packageInfo.versionName.toString()
            name = resources.getString(packageInfo.applicationInfo!!.labelRes)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        val text = name + if (LengthUtils.isNotEmpty(version)) " v$version" else ""
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.title = text
    }

    private fun initAppView(): View {
        val content = PARTS[0].getContent(this@AboutActivity)
        return initView(content)
    }

    private fun initView(content: CharSequence?): WebView {
        val view = WebView(this@AboutActivity)

        view.loadData(content.toString(), "text/html", "UTF-8")

        return view
    }

    private fun initThirdpatyView(): View {
        val content = PARTS[1].getContent(this@AboutActivity)
        return initView(content)
    }

    private fun initUpdateView(): View {
        val content = PARTS[2].getContent(this@AboutActivity)
        return initView(content)
    }

    override fun onPause() {
        super.onPause()
        //MobclickAgent.onPageEnd("about")
    }

    override fun onResume() {
        super.onResume()
        //MobclickAgent.onPageStart("about")
    }

    class Part(val labelId: Int, val format: Format, val fileName: String) {
        var content: CharSequence? = null
        fun getContent(context: Context?): CharSequence? {
            if (TextUtils.isEmpty(content)) {
                content = try {
                    val text = FileUtils.readAssetAsString(fileName)
                    format.format(text)
                } catch (e: Exception) {
                    e.printStackTrace()
                    ""
                }
            }
            return content
        }
    }

    enum class Format {
        /**
         *
         */
        TEXT,

        /**
         *
         */
        HTML;

        /**
         *
         */
        /*WIKI {
            @Override
            public CharSequence format(final String text) {
                return Wiki.fromWiki(text);
            }
        };*/
        fun format(text: String): CharSequence {
            return text
        }
    }

    companion object {
        private val PARTS = arrayOf( // Start
            Part(
                R.string.about_commmon_title,
                Format.HTML,
                "about_common.html"
            ),  //new Part(R.string.about_license_title, Format.HTML, "about_license.html"),
            Part(R.string.about_3dparty_title, Format.HTML, "about_3rdparty.html"),
            Part(R.string.about_changelog_title, Format.HTML, "about_changelog.html")
        )
    }
}