package cn.archko.pdf.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceFragmentCompat
import cn.archko.pdf.App
import cn.archko.pdf.R
import cn.archko.pdf.common.Graph
import com.google.android.material.appbar.MaterialToolbar
import com.umeng.analytics.MobclickAgent

/**
 * @author: archko 2018/12/12 :15:43
 */
class PdfOptionsActivity : FragmentActivity() {

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        val mFragmentManager = supportFragmentManager
        val mFragmentTransaction = mFragmentManager.beginTransaction()
        val mPrefsFragment = PrefsFragment()
        mFragmentTransaction.replace(android.R.id.content, mPrefsFragment)
        mFragmentTransaction.commit()
    }


    class PrefsFragment : PreferenceFragmentCompat() {

        //lateinit var dataStore: DataStore<Preferences>
        private var mDelegate: AppCompatDelegate? = null
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            preferenceManager.preferenceDataStore =
                DataStorePreferenceAdapter(Graph.dataStore, lifecycleScope)
        }

        public override fun onResume() {
            super.onResume()

            MobclickAgent.onResume(activity) // 基础指标统计，不能遗漏
            MobclickAgent.onPageStart(TAG)
            //setSummaries()
        }

        override fun onPause() {
            super.onPause()
            MobclickAgent.onPause(activity) // 基础指标统计，不能遗漏
            MobclickAgent.onPageEnd(TAG)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.options, rootKey)
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view = super.onCreateView(inflater, container, savedInstanceState)
            if (view is LinearLayout) {
                // AppBar
                val toolbar = activity?.let {
                    MaterialToolbar(it)
                }

                toolbar?.apply {
                    setNavigationIcon(R.drawable.ic_nav)
                    popupTheme = R.style.ThemeOverlay_MaterialComponents_ActionBar
                    title = getString(R.string.options)
                    setNavigationOnClickListener { activity?.finish() }
                }
                mDelegate?.apply {
                    mDelegate = activity?.let { AppCompatDelegate.create(it, null) }
                    setSupportActionBar(toolbar)
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                    supportActionBar?.setDisplayShowHomeEnabled(true)
                    supportActionBar?.setTitle(R.string.options)
                }

                view.addView(toolbar, 0)
            }
            return view
        }


        /*fun setSummaries() {
            for (i in summaryKeys.indices) {
                setSummary(i);
            }
        }

        fun setSummary(key: String) {
            for (i in summaryKeys.indices) {
                if (summaryKeys[i] == key) {
                    setSummary(i);
                    return;
                }
            }
        }*/

        fun setSummary(i: Int) {
            /*val pref: androidx.preference.Preference? = findPreference(summaryKeys[i])
            val value = options.getString(
                summaryKeys[i], resources.getString(
                    summaryDefaults[i]
                )
            )
            val valueArray: Array<String> = resources.getStringArray(summaryEntryValues[i])
            val entryArray: Array<String> = resources.getStringArray(summaryEntries[i])
            for (j in valueArray.indices) if (valueArray[j] == value) {
                pref?.summary = entryArray[j]
                return
            }*/
        }
    }

    companion object {
        const val TAG = "PdfOptionsActivity"
        /*const val PREF_SHOW_EXTENSION = "showExtension"

        //public final static String PREF_ORIENTATION = "orientation";
        const val PREF_FULLSCREEN = "fullscreen"
        const val PREF_AUTOCROP = "autocrop"
        const val PREF_VERTICAL_SCROLL_LOCK = "verticalScrollLock"
        const val PREF_SIDE_MARGINS = "sideMargins2" // sideMargins was boolean
        const val PREF_TOP_MARGIN = "topMargin"
        const val PREF_KEEP_ON = "keepOn"
        const val PREF_LIST_STYLE = "list_style"
        const val PREF_DART_THEME = "pref_dart_theme"
        private val summaryKeys = arrayOf(
            SensorHelper.PREF_ORIENTATION, PREF_SIDE_MARGINS,
            PREF_TOP_MARGIN, PREF_LIST_STYLE
        )
        private val summaryEntryValues = intArrayOf(
            R.array.opts_orientations, R.array.opts_margins,
            R.array.opts_margins, R.array.opts_list_styles
        )
        private val summaryEntries = intArrayOf(
            R.array.opts_orientation_labels, R.array.opts_margin_labels,
            R.array.opts_margin_labels, R.array.opts_list_style_labels
        )
        private val summaryDefaults = intArrayOf(
            R.string.opts_default_orientation, R.string.opts_default_side_margin,
            R.string.opts_default_top_margin, R.string.opts_default_list_style
        )*/

        /*fun getString(resources: Resources?, options: SharedPreferences, key: String): String? {
            for (i in summaryKeys.indices) if (summaryKeys[i] == key) return options.getString(
                key, resources!!.getString(
                    summaryDefaults[i]
                )
            )
            return options.getString(key, "")
        }*/

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, PdfOptionsActivity::class.java))
        }
    }
}