package cn.archko.pdf.fragments

//import com.umeng.analytics.MobclickAgent
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import cn.archko.mupdf.R
import cn.archko.pdf.activities.EditActivity
import cn.archko.pdf.adapters.BaseBookAdapter
import cn.archko.pdf.common.PDFViewerHelper
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.BookProgress
import cn.archko.pdf.core.entity.FileBean
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.core.listeners.OnItemClickListener
import cn.archko.pdf.core.utils.FileUtils
import cn.archko.pdf.core.widgets.ColorItemDecoration
import java.io.File

/**
 * @description:file browser
 *
 * @author: archko 11-11-17
 */
open class BrowserFragment : RefreshableFragment(), SwipeRefreshLayout.OnRefreshListener,
    PopupMenu.OnMenuItemClickListener {

    protected val mHandler = Handler(Looper.getMainLooper())
    private var mCurrentPath: String = "/storage/emulated/0"

    protected lateinit var mSwipeRefreshWidget: SwipeRefreshLayout
    protected lateinit var pathTextView: TextView
    protected lateinit var recyclerView: RecyclerView
    protected var bookAdapter: BaseBookAdapter? = null

    private val dirsFirst = true
    protected var showExtension: Boolean = true

    private var mPathMap: MutableMap<String, Int> = HashMap()
    var selectedBean: FileBean? = null
    protected var currentBean: FileBean? = null
    protected lateinit var bookViewModel: BookViewModel

    protected val beanItemCallback: DiffUtil.ItemCallback<FileBean> =
        object : DiffUtil.ItemCallback<FileBean>() {
            override fun areItemsTheSame(oldItem: FileBean, newItem: FileBean): Boolean {
                if (null == oldItem.bookProgress || null == newItem.bookProgress) {
                    return false
                }
                return oldItem.bookProgress!!.equals(newItem.bookProgress)
                        && oldItem.fileSize == newItem.fileSize
                        && oldItem.label == newItem.label
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: FileBean, newItem: FileBean): Boolean {
                if (null == oldItem.bookProgress || null == newItem.bookProgress) {
                    return false
                }
                return oldItem.bookProgress!!.equals(newItem.bookProgress)
                        && oldItem.fileSize == newItem.fileSize
                        && oldItem.label == newItem.label
                        && oldItem.file == newItem.file
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mCurrentPath = getHome()
        bookAdapter = initAdapter()
        bookViewModel = BookViewModel()
    }

    open fun initAdapter(): BaseBookAdapter {
        return BaseBookAdapter(
            activity as Context,
            beanItemCallback,
            itemClickListener
        )
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_set_as_home -> setAsHome()
            R.id.action_extract -> extractImage()
            R.id.action_create -> createPdf()
            R.id.action_convert_epub -> convertToEpub()
        }
        return super.onOptionsItemSelected(menuItem)
    }

    fun extractImage() {
        PdfOperationFragment.showCreateDialog(
            PdfOperationFragment.TYPE_EXTRACT_IMAGES,
            requireActivity(),
            null
        )
    }

    fun createPdf() {
        PdfCreationFragment.showCreateDialog(requireActivity(), null)
    }

    fun convertToEpub() {
        ConvertToEpubFragment.showCreateDialog(requireActivity(), null)
    }

    private fun editPdf(path: String?) {
        if (path != null) {
            EditActivity.start(path, requireActivity())
        }
    }

    private fun setAsHome() {
        val edit = activity?.getSharedPreferences(PREF_TAG, 0)?.edit()
        edit?.putString(PREF_HOME, mCurrentPath)
        edit?.apply()
    }

    open fun onBackPressed(): Boolean {
        val path = Environment.getExternalStorageDirectory().absolutePath
        if (this.mCurrentPath != path && (this.mCurrentPath != "/" || this.mCurrentPath != "/storage/emulated/0")) {
            val upFolder = File(this.mCurrentPath).parentFile
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
        this.recyclerView = view.findViewById(R.id.files)
        setupUi()

        mSwipeRefreshWidget = view.findViewById(R.id.swipe_refresh_widget)!!
        mSwipeRefreshWidget.apply {
            setOnRefreshListener(this@BrowserFragment)
        }

        addObserver()
        return view
    }

    open fun setupUi() {
        recyclerView.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        recyclerView.addItemDecoration(ColorItemDecoration(requireContext()))
    }

    override fun onRefresh() {
        loadData()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        this.recyclerView.adapter = this.bookAdapter
        mHandler.postDelayed({ onRefresh() }, 80L)
    }

    private fun addObserver() {
        bookViewModel.uiFileModel.observe(viewLifecycleOwner) { fileList -> emitFileBeans(fileList) }

        bookViewModel.uiItemModel.observe(viewLifecycleOwner) {
            bookAdapter?.notifyDataSetChanged()
            currentBean = null
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
            bookAdapter?.currentList?.let { it1 -> bookViewModel.updateItem(it, it1) }
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
        bookAdapter?.submitList(fileList)
        if (null != mPathMap[mCurrentPath]) {
            val pos = mPathMap[mCurrentPath]
            if (pos!! < fileList.size) {
                (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                    pos,
                    0
                )
            }
        } else {
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
        }
        mSwipeRefreshWidget.isRefreshing = false

        bookViewModel.startGetProgress(fileList, mCurrentPath)
        pathTextView.text = mCurrentPath
    }

    private fun emitScannerBean(args: Array<Any?>) {
        val path = args[0] as String
        if (TextUtils.equals(mCurrentPath, path)) {
            bookAdapter?.submitList(args[1] as ArrayList<FileBean>)
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
            ).show()
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
            override fun onItemClick(view: View?, data: FileBean, position: Int) {
                clickItem(data)
            }

            override fun onItemClick2(view: View?, data: FileBean, position: Int) {
                clickItem2(data, view!!)
            }
        }

    //点击后,位置不对,这是listadapter交换对比的结果,不想修正了.
    private fun clickItem(clickedEntry: FileBean) {
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
                (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            if (pos < 0) {
                pos = 0
            }
            mPathMap.put(mCurrentPath, pos)
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
            PDFViewerHelper.openAMupdf(clickedFile, requireActivity())
        }
    }

    open fun clickItem2(entry: FileBean, view: View) {
        if (entry.type != FileBean.HOME) {
            selectedBean = entry
            prepareMenu(view, entry)
            return
        }
        selectedBean = null
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
        if (entry.isDirectory) {
            menuBuilder.menu.add(
                0,
                PDFViewerHelper.albumContextMenuItem,
                0,
                getString(R.string.menu_album)
            )
            return
        }

        /*menuBuilder.menu.add(
            0,
            PDFViewerHelper.mupdfNoCropContextMenuItem,
            0,
            getString(R.string.menu_mupdf)
        )*/
        menuBuilder.menu.add(
            0,
            PDFViewerHelper.infoContextMenuItem,
            0,
            getString(R.string.menu_info)
        )
        menuBuilder.menu.add(
            0,
            PDFViewerHelper.otherContextMenuItem,
            0,
            getString(R.string.menu_other)
        )

        if (entry.type == FileBean.NORMAL
            && (IntentFile.isMuPdf(entry.file!!.absolutePath)
                    || IntentFile.isDjvu(entry.file!!.absolutePath))
        ) {
            menuBuilder.menu.add(
                0,
                PDFViewerHelper.editContextMenuItem,
                0,
                getString(R.string.menu_edit)
            )
        }

        if (entry.type == FileBean.RECENT) {
            menuBuilder.menu.add(
                0,
                PDFViewerHelper.removeContextMenuItem,
                0,
                getString(R.string.menu_remove_from_recent)
            )
            menuBuilder.menu.add(
                0,
                PDFViewerHelper.removeAndClearContextMenuItem,
                0,
                getString(R.string.menu_remove_and_clear)
            )
        } else if (!entry.isDirectory && entry.type != FileBean.HOME) {
            if (entry.bookProgress?.isFavorited == 0) {
                menuBuilder.menu.add(
                    0,
                    PDFViewerHelper.deleteContextMenuItem,
                    0,
                    getString(R.string.menu_delete)
                )
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
                PDFViewerHelper.addToFavoriteContextMenuItem,
                0,
                getString(R.string.menu_add_to_fav)
            )
        } else {
            menuBuilder.menu.add(
                0,
                PDFViewerHelper.removeFromFavoriteContextMenuItem,
                0,
                getString(R.string.menu_remove_from_fav)
            )
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        if (selectedBean == null) {
            return true
        }
        val entry = selectedBean!!
        if (item.itemId == PDFViewerHelper.deleteContextMenuItem) {
            Logcat.d(TAG, "delete:$entry")
            //MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, "delete")
            if (entry.type == FileBean.NORMAL && !entry.isDirectory) {
                entry.file?.delete()
                //update()
                val list: ArrayList<FileBean> = arrayListOf()
                bookAdapter?.currentList?.let { list.addAll(it) }
                list.remove(entry)
                bookAdapter?.submitList(list)
            }
            return true
        } else if (item.itemId == PDFViewerHelper.removeContextMenuItem) {
            remove(entry)
        } else if (item.itemId == PDFViewerHelper.removeAndClearContextMenuItem) {
            removeAndClear(entry)
        } else if (item.itemId == PDFViewerHelper.editContextMenuItem) {
            editPdf(entry.file?.absolutePath)
        } else if (item.itemId == PDFViewerHelper.albumContextMenuItem) {
            PDFViewerHelper.openAlbum(entry.file!!, requireActivity())
        } else {
            val clickedFile: File = entry.file!!

            if (clickedFile.exists()) {
                if (item.itemId == PDFViewerHelper.infoContextMenuItem) {
                    //val map = HashMap<String, String>()
                    //map.put("type", "info")
                    //map.put("name", clickedFile.name)
                    //MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                    showFileInfoDiaLog(entry)
                    return true
                }
                if (item.itemId == PDFViewerHelper.addToFavoriteContextMenuItem) {
                    //val map = HashMap<String, String>()
                    //map.put("type", "addToFavorite")
                    //map.put("name", clickedFile.name)
                    //MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                    bookViewModel.favorite(entry, 1)
                    return true
                }
                if (item.itemId == PDFViewerHelper.removeFromFavoriteContextMenuItem) {
                    //val map = HashMap<String, String>()
                    //map.put("type", "removeFromFavorite")
                    //map.put("name", clickedFile.name)
                    //MobclickAgent.onEvent(activity, AnalysticsHelper.A_MENU, map)

                    bookViewModel.favorite(entry, 0)
                    return true
                }

                currentBean = entry
                PDFViewerHelper.openViewerWithMenu(clickedFile, item, requireActivity())
            }
        }
        return false
    }

    open fun remove(entry: FileBean) {
    }

    open fun removeAndClear(entry: FileBean) {
    }

    private fun showFileInfoDiaLog(entry: FileBean) {
        FileInfoFragment.showInfoDialog(activity, entry, object :
            DataListener {
            override fun onSuccess(vararg args: Any?) {
                val fileEntry = args[0] as FileBean
                recyclerView.let { prepareMenu(it, fileEntry) }
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
    }
}
