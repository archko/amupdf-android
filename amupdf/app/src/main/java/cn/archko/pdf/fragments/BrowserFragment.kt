package cn.archko.pdf.fragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import cn.archko.mupdf.R
import cn.archko.pdf.App
import cn.archko.pdf.activities.ChooseFileFragmentActivity
import cn.archko.pdf.activities.PdfOptionsActivity
import cn.archko.pdf.adapters.BookAdapter
import cn.archko.pdf.common.AnalysticsHelper
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.PDFViewerHelper
import cn.archko.pdf.common.RecentManager
import cn.archko.pdf.entity.BookProgress
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.listeners.DataListener
import cn.archko.pdf.listeners.OnItemClickListener
import cn.archko.pdf.utils.FileUtils
import com.umeng.analytics.MobclickAgent
import java.io.File
import java.util.*

/**
 * @description:file browser
 *
 * @author: archko 11-11-17
 */
open class BrowserFragment : RefreshableFragment(), SwipeRefreshLayout.OnRefreshListener,
    PopupMenu.OnMenuItemClickListener {

    protected val mHandler = Handler()
    private var mCurrentPath: String? = null

    protected lateinit var mSwipeRefreshWidget: SwipeRefreshLayout
    protected lateinit var pathTextView: TextView
    protected lateinit var filesListView: RecyclerView
    protected lateinit var fileListAdapter: BookAdapter

    private val dirsFirst = true
    protected var showExtension: Boolean = true

    private var mPathMap: MutableMap<String, Int> = HashMap()
    private var mSelectedPos = -1
    protected var currentBean: FileBean? = null
    protected lateinit var bookViewModel: BookViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mCurrentPath = getHome()
        fileListAdapter = BookAdapter(activity as Context, itemClickListener)
        bookViewModel = BookViewModel()
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_set_as_home -> setAsHome()
        }
        return super.onOptionsItemSelected(menuItem)
    }

    private fun setAsHome() {
        val edit = activity?.getSharedPreferences(ChooseFileFragmentActivity.PREF_TAG, 0)?.edit()
        edit?.putString(ChooseFileFragmentActivity.PREF_HOME, mCurrentPath)
        edit?.apply()
    }

    open fun onBackPressed(): Boolean {
        val path = Environment.getExternalStorageDirectory().absolutePath
        if (this.mCurrentPath != path && this.mCurrentPath != "/") {
            val upFolder = File(this.mCurrentPath!!).parentFile
            if (null != upFolder && upFolder.isDirectory) {
                this.mCurrentPath = upFolder.absolutePath
                loadData()
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        Logcat.d(TAG, ".onResume.$this")
        MobclickAgent.onPageStart("BrowserFragment")
        val options = PreferenceManager.getDefaultSharedPreferences(App.instance)
        showExtension = options.getBoolean(PdfOptionsActivity.PREF_SHOW_EXTENSION, true)
        currentBean?.let {
            mHandler.postDelayed({ updateItem() }, 50L)
        }
    }

    override fun onPause() {
        super.onPause()
        Logcat.i(TAG, ".onPause.$this")
        MobclickAgent.onPageEnd("BrowserFragment")
    }

    override fun onDestroy() {
        super.onDestroy()
        Logcat.i(TAG, ".onDestroy.$this")
    }

    override fun onDetach() {
        super.onDetach()
        Logcat.i(TAG, ".onDetach.$this")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.list_book_choose, container, false)

        this.pathTextView = view.findViewById(R.id.path)
        this.filesListView = view.findViewById(R.id.files)
        filesListView.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)

        mSwipeRefreshWidget = view.findViewById(R.id.swipe_refresh_widget) as SwipeRefreshLayout
        mSwipeRefreshWidget.apply {
            setColorSchemeResources(
                R.color.text_border_pressed, R.color.text_border_pressed,
                R.color.text_border_pressed, R.color.text_border_pressed
            )
            setOnRefreshListener(this@BrowserFragment)
        }

        addObserver()
        return view
    }

    override fun onRefresh() {
        loadData()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        this.filesListView.adapter = this.fileListAdapter
        mHandler.postDelayed({ loadData() }, 80L)
    }

    private fun addObserver() {
        bookViewModel.uiFileModel.observe(viewLifecycleOwner,
            { fileList -> emitFileBeans(fileList) })

        bookViewModel.uiItemModel.observe(viewLifecycleOwner) { flag ->
            run {
                fileListAdapter.notifyDataSetChanged()
                currentBean = null
            }
        }

        bookViewModel.uiScannerModel.observe(viewLifecycleOwner, { args ->
            emitScannerBean(args)
        })
    }

    override fun update() {
        if (!isResumed) {
            return
        }

        loadData()
    }

    open fun updateItem() {
        val file = currentBean!!.file

        file?.let {
            bookViewModel.updateItem(it, fileListAdapter.data)
        }
    }

    open fun loadData() {
        bookViewModel.loadFiles(
            resources.getString(R.string.go_home),
            mCurrentPath,
            dirsFirst,
            showExtension
        )
    }

    open fun emitFileBeans(fileList: List<FileBean>) {
        fileListAdapter.data = fileList
        fileListAdapter.notifyDataSetChanged()
        if (null != mPathMap[mCurrentPath!!]) {
            val pos = mPathMap[mCurrentPath!!]
            if (pos!! < fileList.size) {
                (filesListView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                    pos,
                    0
                )
            }
        } else {
            (filesListView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
        }
        mSwipeRefreshWidget.isRefreshing = false

        bookViewModel.startGetProgress(fileList, mCurrentPath!!)
    }

    private fun emitScannerBean(args: Array<Any?>) {
        val path = args[0] as String
        if (mCurrentPath.equals(path)) {
            fileListAdapter.data = args[1] as ArrayList<FileBean>
            fileListAdapter.notifyDataSetChanged()
        }
    }

    private fun getHome(): String {
        val defaultHome = Environment.getExternalStorageDirectory().absolutePath
        var path: String? = activity?.getSharedPreferences(ChooseFileFragmentActivity.PREF_TAG, 0)!!
            .getString(ChooseFileFragmentActivity.PREF_HOME, null)
        if (null == path) {
            Toast.makeText(
                activity,
                resources.getString(R.string.toast_set_as_home),
                Toast.LENGTH_SHORT
            )
            path = defaultHome
        }
        if (path!!.length > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length - 2)
        }

        val pathFile = File(path)

        if (pathFile.exists() && pathFile.isDirectory)
            return path
        else
            return defaultHome
    }

    private val itemClickListener: OnItemClickListener<FileBean> =
        object : OnItemClickListener<FileBean> {
            override fun onItemClick(view: View?, data: FileBean?, position: Int) {
                clickItem(position)
            }

            override fun onItemClick2(view: View?, data: FileBean?, position: Int) {
                clickItem2(position, view!!)
            }
        }

    private fun clickItem(position: Int) {
        val clickedEntry = fileListAdapter.data[position]
        val clickedFile: File?

        if (clickedEntry.type == FileBean.HOME) {
            clickedFile = File(getHome())
        } else {
            clickedFile = clickedEntry.file
        }

        if (null == clickedFile || !clickedFile.exists())
            return

        if (clickedFile.isDirectory) {
            var pos: Int =
                (filesListView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            if (pos < 0) {
                pos = 0
            }
            mPathMap.put(mCurrentPath!!, pos)
            this@BrowserFragment.mCurrentPath = clickedFile.absolutePath
            loadData()

            val map = HashMap<String, String>()
            map.put("type", "dir")
            map.put("name", clickedFile.name)
            MobclickAgent.onEvent(activity, AnalysticsHelper.A_FILE, map)
        } else {
            val map = HashMap<String, String>()
            map.put("type", "file")
            map.put("name", clickedFile.name)
            MobclickAgent.onEvent(activity, AnalysticsHelper.A_FILE, map)

            currentBean = clickedEntry
            PDFViewerHelper.openWithDefaultViewer(Uri.fromFile(clickedFile), activity!!)
        }
    }

    private fun clickItem2(position: Int, view: View) {
        val entry = this.fileListAdapter.data.get(position) as FileBean
        if (!entry.isDirectory && entry.type != FileBean.HOME) {
            mSelectedPos = position
            prepareMenu(view, entry)
            return
        }
        mSelectedPos = -1
    }

    //--------------------- popupMenu ---------------------

    /**
     * 初始化自定义菜单

     * @param anchorView 菜单显示的锚点View。
     */
    fun prepareMenu(anchorView: View, entry: FileBean) {
        val popupMenu = PopupMenu(activity, anchorView)

        onCreateCustomMenu(popupMenu)
        onPrepareCustomMenu(popupMenu, entry)
        //return showCustomMenu(anchorView);
        popupMenu.setOnMenuItemClickListener(this)
        popupMenu.show()
    }

    /**
     * 创建菜单项，供子类覆盖，以便动态地添加菜单项。

     * @param menuBuilder
     */
    private fun onCreateCustomMenu(menuBuilder: PopupMenu) {
        /*menuBuilder.add(0, 1, 0, "title1");*/
        menuBuilder.menu.clear()
    }

    /**
     * 创建菜单项，供子类覆盖，以便动态地添加菜单项。

     * @param menuBuilder
     */
    private fun onPrepareCustomMenu(menuBuilder: PopupMenu, entry: FileBean) {
        /*menuBuilder.add(0, 1, 0, "title1");*/
        if (entry.type == FileBean.HOME) {
            //menuBuilder.getMenu().add(R.string.set_as_home);
            return
        }

        menuBuilder.menu.add(0, mupdfContextMenuItem, 0, getString(R.string.menu_mupdf))
        //menuBuilder.menu.add(0, bartekscViewContextMenuItem, 0, "barteksc Viewer")
        //menuBuilder.menu.add(0, vudroidContextMenuItem, 0, getString(R.string.menu_vudroid))
        menuBuilder.menu.add(0, documentContextMenuItem, 0, "Mupdf new Viewer")
        menuBuilder.menu.add(0, otherContextMenuItem, 0, getString(R.string.menu_other))
        menuBuilder.menu.add(0, infoContextMenuItem, 0, getString(R.string.menu_info))

        if (entry.type == FileBean.RECENT) {
            menuBuilder.menu.add(
                0,
                removeContextMenuItem,
                0,
                getString(R.string.menu_remove_from_recent)
            )
        } else if (!entry.isDirectory && entry.type != FileBean.HOME) {
            if (entry.bookProgress?.isFavorited == 0) {
                menuBuilder.menu.add(0, deleteContextMenuItem, 0, getString(R.string.menu_delete))
            }
        }
        setFavoriteMenu(menuBuilder, entry)
    }

    private fun setFavoriteMenu(menuBuilder: PopupMenu, entry: FileBean) {
        if (null == entry.bookProgress) {
            if (null != entry.file) {
                entry.bookProgress = BookProgress(FileUtils.getRealPath(entry.file!!.absolutePath))
            } else {
                return
            }
        }
        if (entry.bookProgress!!.isFavorited == 0) {
            menuBuilder.menu.add(
                0,
                addToFavoriteContextMenuItem,
                0,
                getString(R.string.menu_add_to_fav)
            )
        } else {
            menuBuilder.menu.add(
                0,
                removeFromFavoriteContextMenuItem,
                0,
                getString(R.string.menu_remove_from_fav)
            )
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        if (fileListAdapter.itemCount <= 0 || mSelectedPos == -1) {
            return true
        }
        val position = mSelectedPos
        val entry = fileListAdapter.data[position]
        if (item.itemId == deleteContextMenuItem) {
            Logcat.d(TAG, "delete:$entry")
            MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, "delete")
            if (entry.type == FileBean.NORMAL && !entry.isDirectory) {
                entry.file?.delete()
                update()
            }
            return true
        } else if (item.itemId == removeContextMenuItem) {
            if (entry.type == FileBean.RECENT) {
                MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, "remove")
                RecentManager.instance.removeRecentFromDb(entry.file!!.absolutePath)
                update()
            }
        } else {
            val clickedFile: File = entry.file!!

            if (clickedFile.exists()) {
                if (item.itemId == infoContextMenuItem) {
                    val map = HashMap<String, String>()
                    map.put("type", "info")
                    map.put("name", clickedFile.name)
                    MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                    showFileInfoDiaLog(entry)
                    return true
                }
                if (item.itemId == addToFavoriteContextMenuItem) {
                    val map = HashMap<String, String>()
                    map.put("type", "addToFavorite")
                    map.put("name", clickedFile.name)
                    MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                    bookViewModel.favorite(entry, 1)
                    return true
                }
                if (item.itemId == removeFromFavoriteContextMenuItem) {
                    val map = HashMap<String, String>()
                    map.put("type", "removeFromFavorite")
                    map.put("name", clickedFile.name)
                    MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                    bookViewModel.favorite(entry, 0)
                    return true
                }

                currentBean = entry
                PDFViewerHelper.openViewer(clickedFile, item, activity!!)
            }
        }
        return false
    }

    private fun showFileInfoDiaLog(entry: FileBean) {
        FileInfoFragment.showInfoDialog(activity, entry, object : DataListener {
            override fun onSuccess(vararg args: Any?) {
                val fileEntry = args[0] as FileBean
                filesListView.let { prepareMenu(it, fileEntry) }
            }

            override fun onFailed(vararg args: Any?) {
            }
        })
    }

    companion object {

        const val TAG = "BrowserFragment"

        const val deleteContextMenuItem = Menu.FIRST + 100
        const val removeContextMenuItem = Menu.FIRST + 101

        const val mupdfContextMenuItem = Menu.FIRST + 110

        //protected const val apvContextMenuItem = Menu.FIRST + 111
        const val vudroidContextMenuItem = Menu.FIRST + 112
        const val otherContextMenuItem = Menu.FIRST + 113
        const val infoContextMenuItem = Menu.FIRST + 114
        const val documentContextMenuItem = Menu.FIRST + 115
        const val addToFavoriteContextMenuItem = Menu.FIRST + 116
        const val removeFromFavoriteContextMenuItem = Menu.FIRST + 117
        //protected const val bartekscViewContextMenuItem = Menu.FIRST + 118
    }
}
