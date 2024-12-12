package cn.archko.pdf.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import cn.archko.mupdf.databinding.ListLibraryBinding
import cn.archko.pdf.adapters.BaseBookAdapter
import cn.archko.pdf.adapters.BookAdapter
import cn.archko.pdf.adapters.GridBookAdapter
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
class LibraryFragment : RefreshableFragment() {

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyStyle()
        scan()
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
        val defaultHome = Environment.getExternalStorageDirectory().absolutePath
        val path = "$defaultHome/book"
        lifecycleScope.launch {
            libraryViewModel.scan(path)
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
