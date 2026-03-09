package cn.archko.pdf.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cn.archko.pdf.base.BaseFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.R
import cn.archko.pdf.core.entity.ABookmark
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
class BookmarkFragment : Fragment() {

    private var bookmarkViewModel: BookmarkViewModel? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var adapter: BookmarkAdapter
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var bookmarksJob: Job? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    companion object {
        private const val ARG_BOOKMARK_VIEW_MODEL = "bookmark_view_model"

        fun newInstance(bookmarkViewModel: BookmarkViewModel?): BookmarkFragment {
            return BookmarkFragment().apply {
                arguments = Bundle().apply {
                    // 由于BookmarkViewModel不是Parcelable，我们通过父Fragment传递
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 从父Fragment获取BookmarkViewModel
        parentFragment?.let {
            if (it is OutlineTabFragment) {
                bookmarkViewModel = it.bookmarkViewModel
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bookmark_tab, container, false)
        
        recyclerView = view.findViewById(R.id.recycler_view)
        emptyView = view.findViewById(R.id.tv_empty)
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = BookmarkAdapter(requireContext(), emptyList(),
            onItemClick = { bookmark ->
                // 点击书签项，跳转到对应页面
                parentFragment?.let {
                    if (it is OutlineTabFragment) {
                        it.onBookmarkClick(bookmark)
                    }
                }
            },
            onEditClick = { bookmark ->
                // 编辑书签
                parentFragment?.let {
                    if (it is OutlineTabFragment) {
                        it.onEditBookmark(bookmark)
                    }
                }
            },
            onDeleteClick = { bookmark ->
                // 删除书签
                bookmarkViewModel?.deleteBookmark(bookmark)
            }
        )
        recyclerView.adapter = adapter
        
        return view
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
            bookmarkViewModel?.currentPathBookmarks?.collectLatest { bookmarks ->
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
            val tvPage: android.widget.TextView = view.findViewById(R.id.tv_page)
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
            
            holder.tvPage.text = context.getString(R.string.page_label, item.pageIndex + 1)
            
            if (!item.title.isNullOrBlank()) {
                holder.tvTitle.text = item.title
                holder.tvTitle.visibility = View.VISIBLE
            } else {
                holder.tvTitle.visibility = View.GONE
            }
            
            holder.tvTime.text = dateFormat.format(Date(item.createAt))
            
            if (!item.note.isNullOrBlank()) {
                holder.tvNote.text = item.note
                holder.tvNote.visibility = View.VISIBLE
            } else {
                holder.tvNote.visibility = View.GONE
            }
            
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