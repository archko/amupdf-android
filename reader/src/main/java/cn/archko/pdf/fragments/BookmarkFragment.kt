package cn.archko.pdf.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import cn.archko.pdf.R
import cn.archko.pdf.adapters.BaseRecyclerAdapter
import cn.archko.pdf.adapters.BaseViewHolder
import cn.archko.pdf.databinding.FragmentBookmarkBinding
import cn.archko.pdf.databinding.ItemBookmarkBinding
import cn.archko.pdf.entity.Bookmark
import com.thuypham.ptithcm.editvideo.base.BaseFragment

/**
 * @author: archko 2019/7/11 :17:55
 */
open class BookmarkFragment : BaseFragment<FragmentBookmarkBinding>(R.layout.fragment_bookmark) {

    private lateinit var adapter: BaseRecyclerAdapter<Bookmark>
    private var bookmarks = ArrayList<Bookmark>()
    private var page = 0
    var itemListener: ItemListener? = null
        set(value) {
            field = value
        }

    interface ItemListener {
        fun onClick(data: Bookmark, position: Int)
        fun onDelete(data: Bookmark, position: Int)
        fun onAdd(page: Int)
    }

    override fun setupView() {
        binding.recyclerView.itemAnimator = null
        val itemDecoration = DividerItemDecoration(
            context,
            DividerItemDecoration.VERTICAL
        )
        context?.getDrawable(R.drawable.bg_divider)?.let { itemDecoration.setDrawable(it) }
        binding.recyclerView.addItemDecoration(
            itemDecoration
        )
        binding.add.setOnClickListener {
            itemListener?.onAdd(page)
        }

        adapter = object : BaseRecyclerAdapter<Bookmark>(activity, bookmarks) {

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): BaseViewHolder<Bookmark> {
                val binding =
                    ItemBookmarkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return ViewHolder(binding)
            }
        }
        binding.recyclerView.adapter = adapter

        if (bookmarks.size == 0) {
            noBookmark()
        } else {
            binding.nodataView.visibility = View.GONE
        }
    }

    private fun noBookmark() {
        binding.nodataView.visibility = View.VISIBLE
    }

    open fun updateBookmark(page: Int, list: List<Bookmark>) {
        if (!isResumed) {
            return
        }
        this.page = page
        bookmarks.let {
            this.bookmarks.clear()
            this.bookmarks.addAll(list)
            if (this.bookmarks.size > 0) {
                binding.nodataView.visibility = View.GONE
                adapter.notifyDataSetChanged()
            } else {
                noBookmark()
            }
        }
    }

    inner class ViewHolder(private val binding: ItemBookmarkBinding) :
        BaseViewHolder<Bookmark>(binding.root) {

        override fun onBind(data: Bookmark, position: Int) {
            binding.page.text = (data.page + 1).toString()
            binding.delete.setOnClickListener { itemListener?.onDelete(data, position) }
            itemView.setOnClickListener { itemListener?.onClick(data, position) }
        }
    }
}