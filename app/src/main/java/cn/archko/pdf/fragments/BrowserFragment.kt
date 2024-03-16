package cn.archko.pdf.fragments

//import com.umeng.analytics.MobclickAgent
import android.annotation.SuppressLint
import android.app.backup.BackupAgent
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import cn.archko.mupdf.R
import cn.archko.pdf.adapters.BaseViewHolder
import cn.archko.pdf.adapters.BookAdapter
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.PDFViewerHelper
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.entity.BookProgress
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.listeners.DataListener
import cn.archko.pdf.listeners.OnItemClickListener
import cn.archko.pdf.utils.FileUtils
import cn.archko.pdf.widgets.ColorItemDecoration
import java.io.File

/**
 * @description:file browser
 *
 * @author: archko 11-11-17
 */
open class BrowserFragment : RefreshableFragment(), SwipeRefreshLayout.OnRefreshListener,
    PopupMenu.OnMenuItemClickListener {

    protected val mHandler = Handler(Looper.getMainLooper())
    private var mCurrentPath: String? = null

    protected lateinit var mSwipeRefreshWidget: SwipeRefreshLayout
    protected lateinit var pathTextView: TextView
    protected lateinit var filesListView: RecyclerView
    protected lateinit var fileListAdapter: ListAdapter<FileBean, BaseViewHolder<FileBean>>

    private val dirsFirst = true
    protected var showExtension: Boolean = true

    private var mPathMap: MutableMap<String, Int> = HashMap()
    private var mSelectedPos = -1
    protected var currentBean: FileBean? = null
    protected lateinit var bookViewModel: BookViewModel

    protected val beanItemCallback: DiffUtil.ItemCallback<FileBean> =
        object : DiffUtil.ItemCallback<FileBean>() {
            override fun areItemsTheSame(oldItem: FileBean, newItem: FileBean): Boolean {
                return oldItem == newItem
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: FileBean, newItem: FileBean): Boolean {
                return if (null == oldItem.bookProgress) {
                    false
                } else {
                    oldItem.bookProgress!!.equals(newItem.bookProgress)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mCurrentPath = getHome()
        fileListAdapter = initAdapter()
        bookViewModel = BookViewModel()
    }

    open fun initAdapter(): BookAdapter {
        return BookAdapter(
            activity as Context,
            beanItemCallback,
            BookAdapter.TYPE_FILE,
            itemClickListener
        )
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_set_as_home -> setAsHome()
            R.id.action_extract -> extractImage()
            R.id.action_create -> createPdf()
        }
        return super.onOptionsItemSelected(menuItem)
    }

    fun extractImage() {
        PdfOperationFragment.showCreateDialog(
            PdfOperationFragment.TYPE_MERGE,
            requireActivity(),
            null
        )
    }

    fun createPdf() {
        PdfCreationFragment.showCreateDialog(requireActivity(), null)
    }

    private fun setAsHome() {
        val edit = activity?.getSharedPreferences(PREF_TAG, 0)?.edit()
        edit?.putString(PREF_HOME, mCurrentPath)
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
        //MobclickAgent.onPageStart("BrowserFragment")
        showExtension = PdfOptionRepository.getShowExtension()
        currentBean?.let {
            mHandler.postDelayed({ updateItem() }, 50L)
        }
    }

    override fun onPause() {
        super.onPause()
        Logcat.i(TAG, ".onPause.$this")
        //MobclickAgent.onPageEnd("BrowserFragment")
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
        filesListView.addItemDecoration(ColorItemDecoration(requireContext()))

        mSwipeRefreshWidget = view.findViewById(R.id.swipe_refresh_widget) as SwipeRefreshLayout
        mSwipeRefreshWidget.apply {
            setColorSchemeResources(
                cn.archko.pdf.R.color.text_border_pressed,
                cn.archko.pdf.R.color.text_border_pressed,
                cn.archko.pdf.R.color.text_border_pressed,
                cn.archko.pdf.R.color.text_border_pressed
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
        mHandler.postDelayed({ onRefresh() }, 80L)
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

        bookViewModel.uiScannerModel.observe(viewLifecycleOwner) { args ->
            emitScannerBean(args)
        }
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
            bookViewModel.updateItem(it, fileListAdapter.currentList)
        }
    }

    open fun loadData() {
        bookViewModel.loadFiles(
            resources.getString(cn.archko.pdf.R.string.go_home),
            mCurrentPath,
            dirsFirst,
            showExtension
        )
    }

    open fun emitFileBeans(fileList: List<FileBean>) {
        fileListAdapter.submitList(fileList)
        //fileListAdapter.notifyDataSetChanged()
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
        pathTextView.text = mCurrentPath
    }

    private fun emitScannerBean(args: Array<Any?>) {
        val path = args[0] as String
        if (mCurrentPath.equals(path)) {
            fileListAdapter.submitList(args[1] as ArrayList<FileBean>)
            //fileListAdapter.notifyDataSetChanged()
        }
    }

    private fun getHome(): String {
        val defaultHome = Environment.getExternalStorageDirectory().absolutePath
        var path: String? = activity?.getSharedPreferences(PREF_TAG, 0)!!
            .getString(PREF_HOME, null)
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

    val itemClickListener: OnItemClickListener<FileBean> =
        object : OnItemClickListener<FileBean> {
            override fun onItemClick(view: View?, data: FileBean?, position: Int) {
                clickItem(position)
            }

            override fun onItemClick2(view: View?, data: FileBean?, position: Int) {
                clickItem2(position, view!!)
            }
        }

    private fun clickItem(position: Int) {
        val clickedEntry = fileListAdapter.currentList[position]
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

            //val map = HashMap<String, String>()
            //map.put("type", "dir")
            //map.put("name", clickedFile.name)
            //MobclickAgent.onEvent(activity, AnalysticsHelper.A_FILE, map)
        } else {
            //val map = HashMap<String, String>()
            //map.put("type", "file")
            //map.put("name", clickedFile.name)
            //MobclickAgent.onEvent(activity, AnalysticsHelper.A_FILE, map)

            currentBean = clickedEntry
            PDFViewerHelper.openWithDefaultViewer(clickedFile, requireActivity())
        }
    }

    private fun clickItem2(position: Int, view: View) {
        val entry = this.fileListAdapter.currentList.get(position) as FileBean
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
        //menuBuilder.menu.add(0, documentContextMenuItem, 0, "Mupdf new Viewer")
        menuBuilder.menu.add(0, infoContextMenuItem, 0, getString(R.string.menu_info))
        menuBuilder.menu.add(0, otherContextMenuItem, 0, getString(R.string.menu_other))

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
        val entry = fileListAdapter.currentList[position]
        if (item.itemId == deleteContextMenuItem) {
            Logcat.d(TAG, "delete:$entry")
            //MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, "delete")
            if (entry.type == FileBean.NORMAL && !entry.isDirectory) {
                entry.file?.delete()
                //update()
                val list :ArrayList<FileBean> = arrayListOf()
                list.addAll(fileListAdapter.currentList)
                list.remove(entry)
                fileListAdapter.submitList(list)
            }
            return true
        } else if (item.itemId == removeContextMenuItem) {
            remove(entry)
        } else {
            val clickedFile: File = entry.file!!

            if (clickedFile.exists()) {
                if (item.itemId == infoContextMenuItem) {
                    //val map = HashMap<String, String>()
                    //map.put("type", "info")
                    //map.put("name", clickedFile.name)
                    //MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                    showFileInfoDiaLog(entry)
                    return true
                }
                if (item.itemId == addToFavoriteContextMenuItem) {
                    //val map = HashMap<String, String>()
                    //map.put("type", "addToFavorite")
                    //map.put("name", clickedFile.name)
                    //MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                    bookViewModel.favorite(entry, 1)
                    return true
                }
                if (item.itemId == removeFromFavoriteContextMenuItem) {
                    //val map = HashMap<String, String>()
                    //map.put("type", "removeFromFavorite")
                    //map.put("name", clickedFile.name)
                    //MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                    bookViewModel.favorite(entry, 0)
                    return true
                }

                currentBean = entry
                PDFViewerHelper.openViewer(clickedFile, item, requireActivity())
            }
        }
        return false
    }

    open fun remove(entry: FileBean) {
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

        @JvmField
        val PREF_TAG = "ChooseFileActivity"

        @JvmField
        val PREF_HOME = "Home"

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
