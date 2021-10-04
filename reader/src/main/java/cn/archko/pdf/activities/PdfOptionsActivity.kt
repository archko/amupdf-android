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
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cn.archko.pdf.R
import cn.archko.pdf.common.Graph
import com.google.android.material.appbar.MaterialToolbar
import com.umeng.analytics.MobclickAgent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        protected val preferencesRepository = PdfPreferencesRepository(Graph.dataStore)
        private var mDelegate: AppCompatDelegate? = null
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
        }

        override fun onResume() {
            super.onResume()

            MobclickAgent.onResume(activity) // 基础指标统计，不能遗漏
            MobclickAgent.onPageStart(TAG)
        }

        override fun onPause() {
            super.onPause()
            MobclickAgent.onPause(activity) // 基础指标统计，不能遗漏
            MobclickAgent.onPageEnd(TAG)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore =
                DataStorePreferenceAdapter(Graph.dataStore, lifecycleScope)
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

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            setSummaries()
        }

        private fun setSummaries() {
            /*lifecycleScope.launch {
                val pdfPreference = withContext(Dispatchers.IO) {
                    preferencesRepository.pdfPreferencesFlow.first()
                }
                findPreference<Preference>(PreferencesKeys.PREF_ORIENTATION.name)?.summary =
                    resources.getStringArray(R.array.opts_orientation_labels)[pdfPreference.orientation]

                findPreference<Preference>(PreferencesKeys.PREF_LIST_STYLE.name)?.summary =
                    resources.getStringArray(R.array.opts_orientation_labels)[pdfPreference.listStyle]
            }*/
        }
    }

    companion object {
        const val TAG = "PdfOptionsActivity"

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, PdfOptionsActivity::class.java))
        }
    }
}