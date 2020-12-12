package cn.archko.pdf.common

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.R
import cn.archko.pdf.activities.MuPDFRecyclerViewActivity
import cn.archko.pdf.adapters.BaseRecyclerAdapter
import cn.archko.pdf.adapters.MenuAdapter
import cn.archko.pdf.entity.MenuBean
import cn.archko.pdf.fragments.OutlineFragment
import cn.archko.pdf.listeners.MenuListener
import cn.archko.pdf.utils.FileUtils

/**
 * @author: archko 2019/7/12 :19:47
 */
class MenuHelper public constructor(
    var mLeftDrawer: RecyclerView?,
    private var outlineHelper: OutlineHelper?,
    private var supportFragmentManager: FragmentManager
) {

    private var outlineFragment: OutlineFragment? = null

    fun setupMenu(mPath: String?, context: Context, menuListener: MenuListener?) {
        val menus = ArrayList<MenuBean>()
        var bean = MenuBean(FileUtils.getRealPath(mPath), MuPDFRecyclerViewActivity.TYPE_TITLE)
        menus.add(bean)
        bean = MenuBean(
            context.resources.getString(R.string.menu_progress_outline),
            MuPDFRecyclerViewActivity.TYPE_PROGRESS
        )
        menus.add(bean)
        //bean = MenuBean(context.resources.getString(R.string.menu_zoom), MuPDFRecyclerViewActivity.TYPE_ZOOM)
        //menus.add(bean)
        bean = MenuBean(
            context.resources.getString(R.string.menu_settings),
            MuPDFRecyclerViewActivity.TYPE_SETTINGS
        )
        menus.add(bean)
        bean = MenuBean(
            context.resources.getString(R.string.menu_exit),
            MuPDFRecyclerViewActivity.TYPE_CLOSE
        )
        menus.add(bean)

        val adapter = MenuAdapter(menuListener, context)
        mLeftDrawer?.adapter = adapter
        (adapter as BaseRecyclerAdapter<*>).data = menus
        adapter.notifyDataSetChanged()
    }

    fun setupOutline(currentPos: Int?) {
        if (null == outlineFragment) {
            outlineFragment = OutlineFragment()
            val bundle = Bundle()
            if (outlineHelper!!.hasOutline()) {
                //bundle.putSerializable("OUTLINE", outlineHelper?.getOutline())
                bundle.putSerializable("out", outlineHelper?.getOutlineItems())
            }
            bundle.putSerializable("POSITION", currentPos)
            outlineFragment?.arguments = bundle
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.layout_outline, outlineFragment!!)
            .commit()
    }

    fun updateSelection(pos: Int) {
        outlineFragment?.updateSelection(pos)
    }
}
