package cn.archko.pdf.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import cn.archko.pdf.R
import cn.archko.pdf.core.adapters.BaseRecyclerListAdapter
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.core.entity.ABookmark
import cn.archko.pdf.core.widgets.ColorItemDecoration
import cn.archko.pdf.databinding.FragmentBookmarkBinding
import cn.archko.pdf.databinding.ItemBookmarkBinding
import cn.archko.pdf.viewmodel.BookmarkViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 书签Tab Fragment
 * @author: archko 2026/3/9
 */
class BookmarkFragment(private var bookmarkViewModel: BookmarkViewModel) :
    Fragment(R.layout.fragment_bookmark) {

    private lateinit var binding: FragmentBookmarkBinding
    private lateinit var adapter: BookmarkAdapter

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var bookmarksJob: Job? = null
    private var path: String? = null

    companion object {

        fun newInstance(bookmarkViewModel: BookmarkViewModel, path: String): BookmarkFragment {
            return BookmarkFragment(bookmarkViewModel).apply {
                this.path = path
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBookmarkBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val itemDecoration = ColorItemDecoration(requireContext())
        binding.recyclerView.addItemDecoration(itemDecoration)
        adapter = BookmarkAdapter(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun onItemClick(bookmark: ABookmark) {
        parentFragment?.let {
            if (it is OutlineTabFragment) {
                it.onBookmarkClick(bookmark)
            }
        }
    }

    private fun onEditClick(bookmark: ABookmark) {
        showEditDialog(bookmark)
    }

    private fun onDeleteClick(bookmark: ABookmark) {
        bookmarkViewModel.deleteBookmark(bookmark)
    }

    private fun showEditDialog(bookmark: ABookmark) {
        val dialog = BookmarkEditDialog.showDialog(
            pageIndex = bookmark.pageIndex,
            path = bookmark.path,
            bookmarkViewModel = bookmarkViewModel,
            existingBookmark = bookmark
        )
        dialog.show(childFragmentManager, "BookmarkEditDialog")
    }

    override fun onResume() {
        super.onResume()
        startObservingBookmarks()
    }

    override fun onPause() {
        super.onPause()
        stopObservingBookmarks()
    }

    private fun startObservingBookmarks() {
        bookmarksJob?.cancel()
        bookmarksJob = coroutineScope.launch {
            bookmarkViewModel.currentPathBookmarks.collectLatest { bookmarks ->
                updateBookmarks(bookmarks)
            }
        }
    }

    private fun stopObservingBookmarks() {
        bookmarksJob?.cancel()
        bookmarksJob = null
    }

    private fun updateBookmarks(bookmarks: List<ABookmark>) {
        if (bookmarks.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.tvEmpty.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE
            adapter.submitList(bookmarks)
        }
    }

    private inner class BookmarkAdapter(
        context: Context,
    ) : BaseRecyclerListAdapter<ABookmark>(
        context,
        object : DiffUtil.ItemCallback<ABookmark>() {
            override fun areItemsTheSame(
                oldItem: ABookmark,
                newItem: ABookmark
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: ABookmark,
                newItem: ABookmark
            ): Boolean {
                return oldItem.path == newItem.path &&
                        oldItem.pageIndex == newItem.pageIndex &&
                        oldItem.createAt == newItem.createAt
            }
        }) {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<ABookmark> {
            val binding = ItemBookmarkBinding.inflate(layoutInflater, parent, false)
            return ViewHolder(binding)
        }
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private inner class ViewHolder(
        private val binding: ItemBookmarkBinding
    ) : BaseViewHolder<ABookmark>(binding.root) {

        override fun onBind(item: ABookmark, position: Int) {
            binding.tvTitle.text = String.format(
                "%s - %s",
                context!!.getString(R.string.page_label, item.pageIndex + 1),
                item.title
            )

            binding.tvTime.text = dateFormat.format(Date(item.createAt))
            binding.tvNote.text = item.note

            binding.root.setOnClickListener {
                onItemClick(item)
            }

            binding.btnEdit.setOnClickListener {
                onEditClick(item)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }
}