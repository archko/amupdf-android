package cn.archko.pdf.activities

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.TextView
import cn.archko.mupdf.R
import cn.archko.pdf.activities.AboutActivity
import cn.archko.pdf.utils.FileUtils
import cn.archko.pdf.utils.LengthUtils
import com.google.android.material.appbar.MaterialToolbar
import com.umeng.analytics.MobclickAgent

/**
 * @author: archko 2018/12/16 :9:43
 */
class AboutActivity : AnalysticActivity() {
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about)
        var name = "AMuPDF"
        var version = ""
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            version = packageInfo.versionName
            name = resources.getString(packageInfo.applicationInfo.labelRes)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        val text = name + if (LengthUtils.isNotEmpty(version)) " v$version" else ""
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.title = text
        setSupportActionBar(toolbar)
        val view = findViewById<View>(R.id.about_parts) as ExpandableListView
        view.setAdapter(PartsAdapter())
        view.expandGroup(0)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPageEnd("about")
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onPageStart("about")
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

    inner class PartsAdapter : BaseExpandableListAdapter() {
        override fun getGroupCount(): Int {
            return PARTS.size
        }

        override fun getChildrenCount(groupPosition: Int): Int {
            return 1
        }

        override fun getGroup(groupPosition: Int): Part {
            return PARTS[groupPosition]
        }

        override fun getChild(groupPosition: Int, childPosition: Int): Part {
            return PARTS[groupPosition]
        }

        override fun getGroupId(groupPosition: Int): Long {
            return groupPosition.toLong()
        }

        override fun getChildId(groupPosition: Int, childPosition: Int): Long {
            return childPosition.toLong()
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        override fun getGroupView(
            groupPosition: Int, isExpanded: Boolean, convertView: View,
            parent: ViewGroup
        ): View {
            var container: View? = null
            var view: TextView? = null
            container = convertView
                ?: LayoutInflater.from(this@AboutActivity)
                    .inflate(R.layout.about_part, parent, false)
            view = container!!.findViewById<View>(R.id.about_partText) as TextView
            view.setText(getGroup(groupPosition).labelId)
            return container
        }

        override fun getChildView(
            groupPosition: Int, childPosition: Int, isLastChild: Boolean,
            convertView: View, parent: ViewGroup
        ): View {
            var view: WebView? = null
            view = if (convertView !is WebView) {
                WebView(this@AboutActivity)
            } else {
                convertView
            }
            val content = getChild(groupPosition, childPosition).getContent(this@AboutActivity)
            view.loadData(content.toString(), "text/html", "UTF-8")
            //view.setBackgroundColor(Color.GRAY);
            return view
        }

        override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
            return false
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