package cn.archko.pdf.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.R
import cn.archko.pdf.core.entity.ABookmark
import cn.archko.pdf.core.widgets.ColorItemDecoration
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
class BookmarkFragment(private var bookmarkViewModel: BookmarkViewModel) : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bookmark, container, false)

        recyclerView = view.findViewById(R.id.recycler_view)
        emptyView = view.findViewById(R.id.tv_empty)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val itemDecoration = ColorItemDecoration(requireContext())
        itemDecoration.setColor(resources.getColor(cn.archko.pdf.common.R.color.extract_dialog_bg))
        recyclerView.addItemDecoration(itemDecoration)
        adapter = BookmarkAdapter(
            requireContext(), emptyList(),
            onItemClick = { bookmark ->
                parentFragment?.let {
                    if (it is OutlineTabFragment) {
                        it.onBookmarkClick(bookmark)
                    }
                }
            },
            onEditClick = { bookmark ->
                showEditDialog(bookmark)
            },
            onDeleteClick = { bookmark ->
                bookmarkViewModel.deleteBookmark(bookmark)
            }
        )
        recyclerView.adapter = adapter

        return view
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
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
            adapter.updateData(bookmarks)
        }
    }

    class BookmarkAdapter(
        private val context: android.content.Context,
        private var items: List<ABookmark>,
        private val onItemClick: (ABookmark) -> Unit,
        private val onEditClick: (ABookmark) -> Unit,
        private val onDeleteClick: (ABookmark) -> Unit
    ) : RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: android.widget.TextView = view.findViewById(R.id.tv_title)
            val tvTime: android.widget.TextView = view.findViewById(R.id.tv_time)
            val tvNote: android.widget.TextView = view.findViewById(R.id.tv_note)
            val btnEdit: android.widget.ImageButton = view.findViewById(R.id.btn_edit)
            val btnDelete: android.widget.ImageButton = view.findViewById(R.id.btn_delete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context)
                .inflate(R.layout.item_bookmark_tab, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]

            holder.tvTitle.text = String.format(
                "%s - %s",
                context.getString(R.string.page_label, item.pageIndex + 1),
                item.title
            )

            holder.tvTime.text = dateFormat.format(Date(item.createAt))
            holder.tvNote.text = item.note

            holder.itemView.setOnClickListener {
                onItemClick(item)
            }

            holder.btnEdit.setOnClickListener {
                onEditClick(item)
            }

            holder.btnDelete.setOnClickListener {
                onDeleteClick(item)
            }
        }

        override fun getItemCount(): Int = items.size

        fun updateData(newItems: List<ABookmark>) {
            items = newItems
            notifyDataSetChanged()
        }
    }
}