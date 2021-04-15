package cn.archko.pdf.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import cn.archko.pdf.R
import cn.archko.pdf.entity.MenuBean
import cn.archko.pdf.listeners.MenuListener
import cn.archko.pdf.utils.Utils

/**
 * @author: archko 2019/7/12 :19:52
 */
class MenuAdapter public constructor(
    var menuListener: MenuListener?,
    private var context: Context?
) : BaseRecyclerAdapter<String>(context) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<MenuBean> {
        val view = mInflater.inflate(R.layout.item_outline, parent, false)
        return MenuHolder(view)
    }

    inner class MenuHolder(itemView: View?) : BaseViewHolder<MenuBean>(itemView) {

        var title: TextView = itemView!!.findViewById(R.id.title)

        init {
            itemView!!.minimumHeight = Utils.dipToPixel(48f)
        }

        override fun onBind(data: MenuBean?, position: Int) {
            title.setText(data?.title)
            itemView.setOnClickListener {
                menuListener?.onMenuSelected(data, position)
            }
        }
    }

}