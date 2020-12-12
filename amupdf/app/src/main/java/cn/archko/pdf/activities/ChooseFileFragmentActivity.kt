package cn.archko.pdf.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.SparseArray
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import cn.archko.mupdf.R
import cn.archko.pdf.common.Event
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.fragments.BrowserFragment
import cn.archko.pdf.fragments.FavoriteFragment
import cn.archko.pdf.fragments.HistoryFragment
import cn.archko.pdf.fragments.SearchFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.jeremyliao.liveeventbus.LiveEventBus
import com.umeng.analytics.MobclickAgent
import java.lang.ref.WeakReference
import java.util.*

/**
 * @author archko
 */
open class ChooseFileFragmentActivity : AnalysticActivity() {

    private lateinit var mViewPager: ViewPager2
    private lateinit var mPagerAdapter: TabsAdapter
    private lateinit var toolbar: MaterialToolbar
    internal val titles = arrayOfNulls<String>(3)

    private lateinit var tabLayout: TabLayout
    internal var mTabs: MutableList<SamplePagerItem> = ArrayList()

    internal data class SamplePagerItem(
        var clss: Class<*>,
        var args: Bundle,
        var title: CharSequence
    )

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.tabs_home)
        toolbar = findViewById(R.id.toolbar)

        toolbar.inflateMenu(R.menu.menu_history)
        toolbar.setOnMenuItemClickListener { item ->
            val id = item.itemId
            when (id) {
                R.id.action_about -> startActivity(
                    Intent(
                        this@ChooseFileFragmentActivity,
                        AboutActivity::class.java
                    )
                )
                R.id.action_options -> PdfOptionsActivity.start(this@ChooseFileFragmentActivity)
                R.id.action_search -> {
                    showSearchDialog()
                }
                else -> {
                    val fragment: Fragment? = mPagerAdapter.getItemFragment(mViewPager.currentItem)
                    Logcat.d("menu:" + id + " fragment:" + fragment + " index:" + mViewPager.currentItem)
                    fragment?.onOptionsItemSelected(item)
                }
            }
            false
        }

        checkSdcardPermission()

        // 设置为U-APP场景
        MobclickAgent.setScenarioType(this, MobclickAgent.EScenarioType.E_UM_NORMAL);

        val filter = IntentFilter()
        filter.addAction(Event.ACTION_ISFIRST)
        LiveEventBus
            .get(Event.ACTION_ISFIRST, Boolean::class.java)
            .observe(this, object : Observer<Boolean> {
                override fun onChanged(t: Boolean?) {
                    mViewPager.currentItem = 1
                }
            })
    }

    public override fun onResume() {
        super.onResume()
        MobclickAgent.onPageStart(TAG)
        //MobclickAgent.onResume(mContext); // BaseActivity中已经统一调用，此处无需再调用
    }

    public override fun onPause() {
        super.onPause()
        MobclickAgent.onPageEnd(TAG)
        //MobclickAgent.onPause(mContext); // BaseActivity中已经统一调用，此处无需再调用
    }


    private fun checkSdcardPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // WRITE_EXTERNAL_STORAGE permission has not been granted.

            requestSdcardPermission()
        } else {
            loadView()
        }
    }

    /**
     * Requests the sdcard permission.
     * If the permission has been denied previously, a SnackBar will prompt the user to grant the
     * permission, otherwise it is requested directly.
     */
    private fun requestSdcardPermission() {
        Logcat.d(TAG, "sdcard permission has NOT been granted. Requesting permission.")

        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example if the user has previously denied the permission.
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_CODE
            )
        } else {

            // sdcard permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //  权限通过
                //((RefreshableFragment) (mPagerAdapter.getItem(mViewPager.getCurrentItem()))).update();
                loadView()
            } else {
                // 权限拒绝
                Toast.makeText(this, "没有获取sdcard的读取权限", Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }

    //========================================

    private fun loadView() {
        tabLayout = findViewById(R.id.tabs)
        mViewPager = findViewById(R.id.pager)

        addTab()
        mPagerAdapter = TabsAdapter(this)
        mViewPager.adapter = mPagerAdapter

        TabLayoutMediator(tabLayout, mViewPager) { tab, position ->
            tab.text = mTabs[position].title
        }.attach()

        // 滑动监听
        mViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                toolbar.menu.clear()
                if (position == 0) {
                    toolbar.inflateMenu(R.menu.menu_history)
                } else if (position == 1) {
                    toolbar.inflateMenu(R.menu.menu_browser)
                } else if (position == 2) {
                    toolbar.inflateMenu(R.menu.menu_favorite)
                }
            }
        });
    }

    protected fun addTab() {
        titles[0] = getString(R.string.tab_history)
        titles[1] = getString(R.string.tab_browser)
        titles[2] = getString(R.string.tab_favorite)

        var title = titles[0]
        var bundle = Bundle()
        mTabs.add(SamplePagerItem(HistoryFragment::class.java, bundle, title!!))

        title = titles[1]
        bundle = Bundle()
        mTabs.add(SamplePagerItem(BrowserFragment::class.java, bundle, title!!))

        title = titles[2]
        bundle = Bundle()
        mTabs.add(SamplePagerItem(FavoriteFragment::class.java, bundle, title!!))
    }

    override fun onBackPressed() {
        val fragment = mPagerAdapter.getItemFragment(mViewPager.currentItem) as BrowserFragment
        if (fragment.onBackPressed()) {
            return
        }
        super.onBackPressed()
    }

    /*override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val flag = super.onCreateOptionsMenu(menu)
        this.searchMenuItem = menu.add(R.string.menu_search)
        MenuItemCompat.setShowAsAction(this.searchMenuItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM)
        return flag
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem === this.searchMenuItem) {
            showSearchDialog()
        }
        return super.onOptionsItemSelected(menuItem)
    }*/

    protected fun showSearchDialog() {
        val ft = supportFragmentManager.beginTransaction()
        val prev = supportFragmentManager.findFragmentByTag("dialog")
        if (prev != null) {
            ft.remove(prev)
        }
        ft.addToBackStack(null)

        // Create and show the dialog.
        val fileInfoFragment = SearchFragment()
        val bundle = Bundle()
        fileInfoFragment.arguments = bundle
        fileInfoFragment.show(ft, "dialog")
    }

    inner class TabsAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

        private val mContext: Context
        private val mFragmentArray = SparseArray<WeakReference<Fragment>>()

        init {
            mContext = activity
        }

        override fun getItemCount(): Int {
            return mTabs.size
        }

        fun getItemFragment(index: Int): Fragment? {
            return mFragmentArray.get(index)?.get()
        }

        override fun createFragment(position: Int): Fragment {
            val mWeakFragment = mFragmentArray.get(position)
            if (mWeakFragment?.get() != null) {
                return mWeakFragment.get()!!
            }

            val info = mTabs[position]
            val fragment = Fragment.instantiate(mContext, info.clss.name, info.args)
            mFragmentArray.put(position, WeakReference(fragment))
            return fragment
        }
    }

    companion object {

        /**
         * Logging tag.
         */
        private val TAG = "ChooseFile"

        @JvmField
        val PREF_TAG = "ChooseFileActivity"

        @JvmField
        val PREF_HOME = "Home"
        private val REQUEST_PERMISSION_CODE = 0x001
    }
}
