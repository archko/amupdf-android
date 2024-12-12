package cn.archko.pdf.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.ListLibraryBinding
import cn.archko.pdf.adapters.BaseBookAdapter
import cn.archko.pdf.adapters.BookAdapter
import cn.archko.pdf.adapters.GridBookAdapter
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.core.entity.FileBean
import cn.archko.pdf.core.listeners.OnItemClickListener
import cn.archko.pdf.viewmodel.HistoryViewModel.Companion.STYLE_LIST
import cn.archko.pdf.viewmodel.LibraryViewModel
import kotlinx.coroutines.launch

/**
 * @description:library list
 *
 * @author: archko 2024/12/12 :12:43
 */
class LibraryFragment : RefreshableFragment(), PopupMenu.OnMenuItemClickListener {

    private var mStyle: Int = STYLE_LIST
    protected var bookAdapter: BaseBookAdapter? = null
    protected lateinit var libraryViewModel: LibraryViewModel
    private lateinit var binding: ListLibraryBinding

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

    val itemClickListener: OnItemClickListener<FileBean> =
        object : OnItemClickListener<FileBean> {
            override fun onItemClick(view: View?, data: FileBean, position: Int) {
                //clickItem(data)
            }

            override fun onItemClick2(view: View?, data: FileBean, position: Int) {
                //clickItem2(data, view!!)
            }
        }

    override fun update() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        libraryViewModel = LibraryViewModel()
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

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun applyStyle() {
        removeItemDecorations()
        if (mStyle == STYLE_LIST) {
            addDecoration()
            binding.recyclerView.layoutManager =
                LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            if (null == bookAdapter || bookAdapter is GridBookAdapter) {
                bookAdapter = BookAdapter(
                    activity as Context,
                    beanItemCallback,
                    itemClickListener
                )
                binding.recyclerView.adapter = bookAdapter
                bookAdapter?.notifyItemInserted(0)
                binding.recyclerView.smoothScrollToPosition(0)
            }
        } else {
            addDecoration()
            binding.recyclerView.layoutManager = GridLayoutManager(activity, 3)
            if (null == bookAdapter || bookAdapter is BookAdapter) {
                bookAdapter = GridBookAdapter(
                    activity as Context,
                    beanItemCallback,
                    itemClickListener
                )
                binding.recyclerView.adapter = bookAdapter
                bookAdapter?.notifyItemInserted(0)
                binding.recyclerView.smoothScrollToPosition(0)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyStyle()

        binding.sort.setOnClickListener { v -> prepareMenu(v, R.menu.menu_sort) }
        binding.style.setOnClickListener { v -> prepareMenu(v, R.menu.menu_style) }
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
                }
            }
        })
    }

    private fun prepareMenu(anchorView: View?, menuId: Int) {
        val popupMenu = PopupMenu(requireContext(), anchorView)
        popupMenu.inflate(menuId)
        popupMenu.setOnMenuItemClickListener(this)
        popupMenu.show()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_style_list -> {}
            R.id.action_style_grid -> {}
            R.id.action_sort_name -> {}
            R.id.action_sort_name_desc -> {}
            R.id.action_sort_create -> {}
            R.id.action_sort_create_desc -> {}
            R.id.action_sort_size -> {}
            R.id.action_sort_size_desc -> {}
        }
        return false
    }

    private fun addDecoration() {
        val decoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        AppCompatResources.getDrawable(requireContext(), cn.archko.pdf.R.drawable.divider_light)
            ?.let { decoration.setDrawable(it) }
        binding.recyclerView.addItemDecoration(decoration)
    }

    private fun removeItemDecorations() {
        for (i in 0 until binding.recyclerView.itemDecorationCount) {
            binding.recyclerView.removeItemDecorationAt(i)
        }
    }

    private fun scan() {
        var scanFolder = PdfOptionRepository.getScanFolder()
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

    fun emitFileBeans(fileList: List<FileBean>) {
        bookAdapter?.submitList(fileList)

        //bookViewModel.startGetProgress(fileList, mCurrentPath)
    }

    companion object {

        const val TAG = "LibraryFragment"
    }
}
