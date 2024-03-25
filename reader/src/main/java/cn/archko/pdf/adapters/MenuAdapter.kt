package cn.archko.pdf.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import cn.archko.pdf.R
import cn.archko.pdf.core.adapters.BaseRecyclerAdapter
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.entity.MenuBean
import cn.archko.pdf.listeners.MenuListener
import cn.archko.pdf.core.utils.Utils

/**
 * @author: archko 2019/7/12 :19:52
 */
class MenuAdapter(
    var menuListener: MenuListener?,
    context: Context?
) : BaseRecyclerAdapter<MenuBean>(context) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<MenuBean> {
        val binding = inflater.inflate(R.layout.item_outline, parent, false)
        return MenuHolder(binding)
    }

    inner class MenuHolder(root: View) :
        BaseViewHolder<MenuBean>(root) {
        private var title: TextView? = null

        init {
            itemView.minimumHeight = Utils.dipToPixel(48f)
            title = root.findViewById(R.id.title)
        }

        override fun onBind(data: MenuBean?, position: Int) {
            title?.text = data?.title
            itemView.setOnClickListener {
                menuListener?.onMenuSelected(data, position)
            }
        }
    }

}