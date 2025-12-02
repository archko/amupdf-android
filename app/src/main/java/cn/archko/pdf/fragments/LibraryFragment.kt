package cn.archko.pdf.fragments

import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.ListLibraryBinding
import cn.archko.pdf.adapters.AdapterUtils
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.core.adapters.BaseRecyclerAdapter
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.core.common.Event.Companion.ACTION_DONOT_SCAN
import cn.archko.pdf.core.common.Event.Companion.ACTION_SCAN
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.common.ScanEvent
import cn.archko.pdf.core.entity.FileBean
import cn.archko.pdf.core.entity.ResponseHandler
import cn.archko.pdf.core.utils.CompareUtils
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.utils.FetcherUtils
import cn.archko.pdf.viewmodel.HistoryViewModel.Companion.STYLE_GRID
import cn.archko.pdf.viewmodel.HistoryViewModel.Companion.STYLE_LIST
import cn.archko.pdf.viewmodel.LibraryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.util.*

/**
 * @description:library list
 *
 * @author: archko 2024/12/12 :12:43
 */
class LibraryFragment : RefreshableFragment(), PopupMenu.OnMenuItemClickListener {

    private var mStyle: Int = STYLE_LIST
    private var bookAdapter: BaseRecyclerAdapter<FileBean>? = null
    private lateinit var libraryViewModel: LibraryViewModel
    private lateinit var binding: ListLibraryBinding
    private var scanFolder: String? = null
    private var autoScan = true

    private var coverWidth = 135
    private var coverHeight = 180

    override fun update() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        libraryViewModel = LibraryViewModel()

        vn.chungha.flowbus.collectFlowBus<ScanEvent>(scope = this, isSticky = true) {
            Logcat.d(HistoryFragment.TAG, "action_scan:${it.name}")
            autoScan = PdfOptionRepository.getAutoScan()
            if (TextUtils.equals(ACTION_SCAN, it.name)) {
                scan()
            } else if (TextUtils.equals(ACTION_DONOT_SCAN, it.name)) {
                libraryViewModel.shutdown()
            }
        }

        scanFolder = PdfOptionRepository.getScanFolder()
        autoScan = PdfOptionRepository.getAutoScan()
        mStyle = PdfOptionRepository.getLibraryStyle()

        val screenHeight: Int = Utils.getScreenHeightPixelWithOrientation(context)
        val screenWidth: Int = Utils.getScreenWidthPixelWithOrientation(context)
        var h: Int = (screenHeight - Utils.dipToPixel(12f)) / 3
        var w: Int = (screenWidth - Utils.dipToPixel(12f)) / 3
        if (h < w) {
            w = h
        }
        h = w * 4 / 3
        coverWidth = w
        coverHeight = h
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ListLibraryBinding.inflate(inflater)
        addDecoration()

        addObserver()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun onOptionSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            /*R.id.action_extract -> extractImage(requireActivity())
            R.id.action_create -> createPdf(requireActivity())
            R.id.action_convert_epub -> convertToEpub(requireActivity())*/
            R.id.action_style -> {
                if (mStyle == STYLE_LIST) {
                    mStyle = STYLE_GRID
                } else {
                    mStyle = STYLE_LIST
                }
                PdfOptionRepository.setStyle(mStyle)
                applyStyle()
                return true
            }
        }
        return false
    }

    private fun applyStyle() {
        removeItemDecorations()
        if (mStyle == STYLE_LIST) {
            addDecoration()
            binding.recyclerView.layoutManager =
                LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        } else {
            addDecoration()
            binding.recyclerView.layoutManager = GridLayoutManager(activity, 3)
        }
        if (null == bookAdapter) {
            bookAdapter = object : BaseRecyclerAdapter<FileBean>(activity) {
                override fun getItemViewType(position: Int): Int {
                    return mStyle
                }

                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    viewType: Int
                ): BaseViewHolder<FileBean> {
                    if (viewType == STYLE_LIST) {
                        val view = mInflater.inflate(R.layout.item_book_normal, parent, false)
                        return ViewHolder(view)
                    } else {
                        val view = mInflater.inflate(R.layout.item_book_grid, parent, false)
                        return GridViewHolder(view)
                    }
                }
            }
            binding.recyclerView.adapter = bookAdapter
        } else {
            bookAdapter?.notifyDataSetChanged()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyStyle()

        binding.sort.setOnClickListener { v -> prepareMenu(v, R.menu.menu_sort) }
        binding.style.setOnClickListener { v -> prepareMenu(v, R.menu.menu_style) }
        binding.imgClose.setOnClickListener {
            binding.keyword.setText(null)
            search("")
        }
        scan()

        binding.keyword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if (TextUtils.isEmpty(s.toString())) {
                    binding.imgClose.visibility = View.GONE
                } else {
                    binding.imgClose.visibility = View.VISIBLE
                    search(s.toString())
                }
            }
        })
    }

    private fun search(text: String) {
        lifecycleScope.launch {
            libraryViewModel.search(text)
                .flowOn(Dispatchers.IO)
                .collectLatest { res ->
                    if (res is ResponseHandler.Success) {
                        emitFileBeans(res.data)
                    }
                }
        }
    }

    private fun prepareMenu(anchorView: View?, menuId: Int) {
        val popupMenu = PopupMenu(requireContext(), anchorView)
        popupMenu.inflate(menuId)
        popupMenu.setOnMenuItemClickListener(this)
        popupMenu.show()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_style_list -> {
                PdfOptionRepository.setLibraryStyle(STYLE_LIST)
                mStyle = PdfOptionRepository.getLibraryStyle()
                applyStyle()
            }

            R.id.action_style_grid -> {
                PdfOptionRepository.setLibraryStyle(STYLE_GRID)
                mStyle = PdfOptionRepository.getLibraryStyle()
                applyStyle()
            }

            R.id.action_sort_name -> {
                sort(0, CompareUtils.NAME_ASC)
            }

            R.id.action_sort_name_desc -> {
                sort(1, CompareUtils.NAME_DESC)
            }

            R.id.action_sort_create -> {
                sort(2, CompareUtils.MODIFY_ASC)
            }

            R.id.action_sort_create_desc -> {
                sort(3, CompareUtils.MODIFY_DESC)
            }

            R.id.action_sort_size -> {
                sort(4, CompareUtils.SIZE_ASC)
            }

            R.id.action_sort_size_desc -> {
                sort(5, CompareUtils.SIZE_DESC)
            }
        }
        return false
    }

    private fun sort(sort: Int, comp: Comparator<FileBean>) {
        lifecycleScope.launch {
            PdfOptionRepository.setSort(sort)
            libraryViewModel.sort(comp)
                .flowOn(Dispatchers.IO)
                .collectLatest { res ->
                    if (res is ResponseHandler.Success) {
                        emitFileBeans(res.data)
                    }
                }
        }
    }

    private fun addDecoration() {
        val decoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        AppCompatResources.getDrawable(requireContext(), cn.archko.pdf.R.drawable.divider_light)
            ?.let { decoration.setDrawable(it) }
        binding.recyclerView.addItemDecoration(decoration)
    }

    private fun removeItemDecorations() {
        for (i in 0 until binding.recyclerView.itemDecorationCount) {
            if (i < binding.recyclerView.itemDecorationCount) {
                binding.recyclerView.removeItemDecorationAt(i)
            }
        }
    }

    private fun scan() {
        if (!autoScan) {
            libraryViewModel.shutdown()
            return
        }
        if (TextUtils.isEmpty(scanFolder)) {
            val defaultHome = Environment.getExternalStorageDirectory().absolutePath
            scanFolder = "$defaultHome/book"
        }
        lifecycleScope.launch {
            libraryViewModel.scan(scanFolder)
        }
    }

    private fun addObserver() {
        libraryViewModel.uiFileModel.observe(viewLifecycleOwner) { fileList ->
            emitFileBeans(fileList)
        }
    }

    private fun emitFileBeans(fileList: List<FileBean>) {
        bookAdapter?.data = fileList
        bookAdapter?.notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : BaseViewHolder<FileBean>(itemView) {

        private var mName: TextView? = null
        private var mIcon: ImageView? = null
        private var mSize: TextView? = null
        private var mProgressBar: ProgressBar? = null

        init {
            mIcon = itemView.findViewById(R.id.icon)
            mName = itemView.findViewById(R.id.name)
            mSize = itemView.findViewById(R.id.size)
            mProgressBar = itemView.findViewById(R.id.progressbar)
        }

        override fun onBind(entry: FileBean, position: Int) {
            itemView.setOnClickListener {
            }
            mName!!.text = entry.label
            val bookProgress = entry.bookProgress
            if (null != bookProgress) {
                if (bookProgress.page > 0) {
                    mProgressBar!!.visibility = View.VISIBLE
                    mProgressBar!!.max = bookProgress.pageCount
                    mProgressBar!!.progress = bookProgress.page
                } else {
                    mProgressBar!!.visibility = View.INVISIBLE
                }
                mSize!!.text = Utils.getFileSize(bookProgress.size)
            } else {
                mProgressBar!!.visibility = View.INVISIBLE
                mSize!!.text = null
            }

            if (entry.type == FileBean.HOME) {
                mIcon!!.setImageResource(cn.archko.pdf.R.drawable.ic_book_dir_home)
            } else if (entry.type == FileBean.NORMAL && entry.isDirectory && !entry.isUpFolder) {
                mIcon!!.setImageResource(cn.archko.pdf.R.drawable.ic_book_folder)
            } else if (entry.isUpFolder) {
                mIcon!!.setImageResource(cn.archko.pdf.R.drawable.ic_book_folder)
            } else {
                if (bookProgress?.ext != null) {
                    val ext = bookProgress.ext!!.lowercase(Locale.ROOT)

                    AdapterUtils.setIcon(".$ext", mIcon!!)
                }
            }
        }
    }

    inner class GridViewHolder(itemView: View) : BaseViewHolder<FileBean>(itemView) {

        var mName: TextView? = null
        var mIcon: ImageView? = null

        var mProgressBar: ProgressBar? = null

        init {
            mIcon = itemView.findViewById(R.id.icon)
            mName = itemView.findViewById(R.id.name)
            //mSize = itemView.findViewById(R.id.size)
            mProgressBar = itemView.findViewById(R.id.progressbar)
        }

        override fun onBind(entry: FileBean, position: Int) {
            itemView.setOnClickListener {
            }
            mName!!.text = entry.label
            val bookProgress = entry.bookProgress
            if (null != bookProgress) {
                if (bookProgress.page > 0) {
                    mProgressBar!!.visibility = View.VISIBLE
                    mProgressBar!!.max = bookProgress.pageCount
                    mProgressBar!!.progress = bookProgress.page
                } else {
                    mProgressBar!!.visibility = View.INVISIBLE
                }
                //mSize!!.text = Utils.getFileSize(bookProgress.size)
            } else {
                mProgressBar!!.visibility = View.INVISIBLE
                //mSize!!.text = null
            }

            if (bookProgress?.ext != null) {
                val ext = bookProgress.ext!!.lowercase(Locale.ROOT)

                AdapterUtils.setIcon(".$ext", mIcon!!)
            }

            var lp = mIcon!!.layoutParams
            if (null == lp) {
                lp = LinearLayout.LayoutParams(coverWidth, coverHeight)
                mIcon!!.setLayoutParams(lp)
            } else {
                lp.width = coverWidth
                lp.height = coverHeight
            }
            entry.file?.absolutePath?.let { FetcherUtils.load(it, mIcon!!.context, mIcon!!) }
        }
    }

    companion object {

        const val TAG = "LibraryFragment"
    }
}
