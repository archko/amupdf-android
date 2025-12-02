package cn.archko.pdf.adapters

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import cn.archko.mupdf.R
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.core.entity.FileBean
import cn.archko.pdf.core.listeners.OnItemClickListener

/**
 * @author: archko 2024/8/12 :15:43
 */
class BookAdapter(
    context: Context,
    diffCallback: DiffUtil.ItemCallback<FileBean>,
    itemClickListener: OnItemClickListener<FileBean>?
) :
    BaseBookAdapter(context, diffCallback, itemClickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<FileBean> {
        val view = mInflater.inflate(R.layout.item_book_normal, parent, false)
        return ViewHolder(view)
    }
}
