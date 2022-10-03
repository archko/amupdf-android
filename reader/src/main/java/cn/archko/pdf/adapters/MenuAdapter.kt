package cn.archko.pdf.adapters

import android.content.Context
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import cn.archko.pdf.R
import cn.archko.pdf.databinding.ItemOutlineBinding
import cn.archko.pdf.entity.MenuBean
import cn.archko.pdf.listeners.MenuListener
import cn.archko.pdf.utils.Utils

/**
 * @author: archko 2019/7/12 :19:52
 */
class MenuAdapter public constructor(
    var menuListener: MenuListener?,
    private var context: Context?
) : BaseRecyclerAdapter<MenuBean>(context) {

    lateinit var binding: ItemOutlineBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<MenuBean> {
        binding = DataBindingUtil.inflate(inflater, R.layout.item_outline, parent, false)
        return MenuHolder(binding)
    }

    inner class MenuHolder(private val binding: ItemOutlineBinding) :
        BaseViewHolder<MenuBean>(binding.root) {

        init {
            itemView.minimumHeight = Utils.dipToPixel(48f)
        }

        override fun onBind(data: MenuBean?, position: Int) {
            binding.title.setText(data?.title)
            itemView.setOnClickListener {
                menuListener?.onMenuSelected(data, position)
            }
        }
    }

}