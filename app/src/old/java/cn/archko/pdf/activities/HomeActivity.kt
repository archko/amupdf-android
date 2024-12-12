package cn.archko.pdf.activities

//import com.umeng.analytics.MobclickAgent
import android.Manifest
import android.app.AlertDialog
import android.app.ComponentCaller
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.util.SparseArray
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import cn.archko.mupdf.R
import cn.archko.pdf.common.PDFViewerHelper
import cn.archko.pdf.core.common.Event
import cn.archko.pdf.core.common.GlobalEvent
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.filesystem.FileExtensionFilter
import cn.archko.pdf.core.filesystem.FileSystemScanner
import cn.archko.pdf.fragments.BrowserFragment
import cn.archko.pdf.fragments.FavoriteFragment
import cn.archko.pdf.fragments.HistoryFragment
import cn.archko.pdf.fragments.SearchFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import vn.chungha.flowbus.collectFlowBus
import java.io.File
import java.lang.ref.WeakReference
import java.util.Locale

/**
 * @author archko
 */
open class HomeActivity : AnalysticActivity(), OnPermissionGranted,
    PopupMenu.OnMenuItemClickListener {

    private lateinit var mViewPager: ViewPager2
    private lateinit var mPagerAdapter: TabsAdapter

    private lateinit var searchBtn: ImageButton
    private lateinit var settingBtn: ImageButton
    private lateinit var menuBtn: ImageButton
    private val titles = arrayOfNulls<String>(3)

    private lateinit var tabLayout: TabLayout
    internal var mTabs: MutableList<SamplePagerItem> = ArrayList()
    private val permissionCallbacks = arrayOfNulls<OnPermissionGranted>(PERMISSION_LENGTH)
    private var permissionDialog: Dialog? = null
    protected var mPath: String? = null

    internal data class SamplePagerItem(
        var clss: Class<*>,
        var args: Bundle,
        var title: CharSequence
    )

    public override fun onCreate(savedInstanceState: Bundle?) {
        isLive = true
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, onBackPress)

        setContentView(R.layout.tabs_home)

        searchBtn = findViewById(R.id.search)
        settingBtn = findViewById(R.id.setting)
        menuBtn = findViewById(R.id.menu)
        searchBtn.setOnClickListener { showSearchDialog() }
        settingBtn.setOnClickListener { PdfOptionsActivity.start(this@HomeActivity) }
        menuBtn.setOnClickListener { prepareMenu(menuBtn) }

        checkForExternalPermission()

        // 设置为U-APP场景
        //MobclickAgent.setScenarioType(this, MobclickAgent.EScenarioType.E_UM_NORMAL);

        val filter = IntentFilter()
        filter.addAction(Event.ACTION_ISFIRST)

        collectFlowBus<GlobalEvent>(isSticky = true) {
            if (TextUtils.equals(it.name, Event.ACTION_ISFIRST) && it.obj as Boolean) {
                Logcat.d(TAG, "ACTION_ISFIRST:${it.name}")
                mViewPager.currentItem = 1
            }
        }

        if (null != savedInstanceState) {
            mPath = savedInstanceState.getString("path", null)
        }
        parseIntent()

        /*val defaultHome = Environment.getExternalStorageDirectory().absolutePath
        FileSystemScanner(this).startScan(object : FileExtensionFilter() {
            override fun accept(file: File?, name: String?): Boolean {
                if (null == file) {
                    return false
                }
                val fname = file.name.lowercase(Locale.ROOT)
                if (fname.startsWith(".")) {
                    return false
                }
                if (file.isDirectory) {
                    return true
                }

                return IntentFile.isMuPdf(fname)
                        || IntentFile.isImage(fname)
                        || IntentFile.isText(fname)
                        || IntentFile.isDjvu(fname)
                        || IntentFile.isMobi(fname)
            }
        }, defaultHome + "/book")*/
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
        parseIntent()
    }

    private fun parseIntent() {
        if (null == intent) {
            return
        }
        if (TextUtils.isEmpty(mPath)) {
            mPath = IntentFile.processIntentAction(intent, this)
        }
        if (!TextUtils.isEmpty(mPath)) {
            PDFViewerHelper.openImage(mPath, this)
        }
        intent = null
    }

    private val onBackPress = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (onBackEvent()) {
                return
            }
            finish()
        }
    }

    private fun onBackEvent(): Boolean {
        if (null == mPagerAdapter) {
            return false
        }
        val itemFragment = mPagerAdapter.getItemFragment(mViewPager.currentItem)
        if (itemFragment is BrowserFragment) {
            if (itemFragment.onBackPressed()) {
                return true
            }
        }
        return false
    }

    public override fun onResume() {
        super.onResume()
        //MobclickAgent.onPageStart(TAG)
        //MobclickAgent.onResume(mContext); // BaseActivity中已经统一调用，此处无需再调用
    }

    public override fun onPause() {
        super.onPause()
        //MobclickAgent.onPageEnd(TAG)
        //MobclickAgent.onPause(mContext); // BaseActivity中已经统一调用，此处无需再调用
    }

    private fun prepareMenu(anchorView: View) {
        val popupMenu = PopupMenu(this, anchorView)

        onPrepareCustomMenu(popupMenu)
        popupMenu.setOnMenuItemClickListener(this)
        popupMenu.show()
    }

    private fun onPrepareCustomMenu(menuBuilder: PopupMenu) {
        menuBuilder.inflate(R.menu.menu_history)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_about -> startActivity(
                Intent(
                    this@HomeActivity,
                    AboutActivity::class.java
                )
            )

            else -> {
                val fragment: Fragment? = mPagerAdapter.getItemFragment(mViewPager.currentItem)
                Logcat.d("menu:" + item.itemId + " fragment:" + fragment + " index:" + mViewPager.currentItem)
                if (fragment is HistoryFragment) {
                    fragment.onOptionSelected(item)
                } else if (fragment is BrowserFragment) {
                    fragment.onOptionSelected(item)
                }
                return true
            }
        }
        return false
    }

    private fun loadView() {
        tabLayout = findViewById(R.id.tabs)
        mViewPager = findViewById(R.id.pager)

        addTab()
        mPagerAdapter = TabsAdapter(this)
        mViewPager.adapter = mPagerAdapter

        TabLayoutMediator(tabLayout, mViewPager) { tab, position ->
            tab.text = mTabs[position].title
        }.attach()

        /*val primaryColor = ThemeStore.primaryColor(this)
        val normalColor: Int =
            ToolbarContentTintHelper.toolbarSubtitleColor(this, primaryColor)
        val selectedColor: Int =
            ToolbarContentTintHelper.toolbarTitleColor(this, primaryColor)
        //TabLayoutUtil.setTabIconColors(tabs, normalColor, selectedColor)
        tabLayout.setTabTextColors(normalColor, selectedColor)
        tabLayout.setSelectedTabIndicatorColor(ThemeStore.accentColor(this))*/
    }

    private fun addTab() {
        titles[0] = getString(cn.archko.pdf.R.string.tab_history)
        titles[1] = getString(cn.archko.pdf.R.string.tab_browser)
        titles[2] = getString(cn.archko.pdf.R.string.tab_favorite)

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

    private fun showSearchDialog() {
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

    //========================================

    private fun checkForExternalPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkStoragePermission()) {
                requestStoragePermission(this, true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requestAllFilesAccess(this)
            }
        } else {
            loadView()
        }
    }

    private fun checkStoragePermission(): Boolean {
        return (ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
                == PackageManager.PERMISSION_GRANTED)
    }

    open fun requestStoragePermission(
        onPermissionGranted: OnPermissionGranted, isInitialStart: Boolean
    ) {
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        permissionCallbacks[STORAGE_PERMISSION] = onPermissionGranted
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.grant_files_permission)
                .setMessage(R.string.grant_files_permission)
                .setPositiveButton(R.string.grant_cancel) { _, _ ->
                    finish()
                }
                .setNegativeButton(R.string.grant_ok) { _, _ ->
                    ActivityCompat.requestPermissions(
                        this, arrayOf(permission), STORAGE_PERMISSION
                    )
                    permissionDialog?.run {
                        permissionDialog!!.dismiss()
                    }
                }
            builder.setCancelable(false)
            builder.create().show()
        } else if (isInitialStart) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), STORAGE_PERMISSION)
        }
    }

    open fun requestAllFilesAccess(onPermissionGranted: OnPermissionGranted) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.grant_all_files_permission)
                .setMessage(R.string.grant_all_files_permission)
                .setPositiveButton(R.string.grant_cancel) { _, _ ->
                    finish()
                }
                .setNegativeButton(R.string.grant_ok) { _, _ ->
                    permissionCallbacks[ALL_FILES_PERMISSION] = onPermissionGranted
                    try {
                        val intent =
                            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                .setData(Uri.parse("package:$packageName"))
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Failed to initial activity to grant all files access",
                            e
                        )
                        Toast.makeText(this, "没有获取sdcard的读取权限", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            builder.setCancelable(false)
            builder.create().show()
        } else {
            loadView()
        }
    }

    private fun isGranted(grantResults: IntArray): Boolean {
        return grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION) {
            if (isGranted(grantResults)) {
                permissionCallbacks[STORAGE_PERMISSION]!!.onPermissionGranted()
                permissionCallbacks[STORAGE_PERMISSION] = null
            } else {
                Toast.makeText(this, R.string.grantfailed, Toast.LENGTH_SHORT).show()
                permissionCallbacks[STORAGE_PERMISSION]?.let {
                    requestStoragePermission(it, false)
                }
            }
        }
    }

    override fun onPermissionGranted() {
        loadView()
    }

    //========================================

    inner class TabsAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

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

        private val TAG = "ChooseFile"

        @JvmField
        val PREF_TAG = "ChooseFileActivity"

        @JvmField
        val PREF_HOME = "Home"

        const val PERMISSION_LENGTH = 2
        var STORAGE_PERMISSION = 0
        const val ALL_FILES_PERMISSION = 1
    }
}
