package cn.archko.pdf.common

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.R
import cn.archko.pdf.activities.MuPDFRecyclerViewActivity
import cn.archko.pdf.adapters.BaseRecyclerAdapter
import cn.archko.pdf.adapters.MenuAdapter
import cn.archko.pdf.entity.Bookmark
import cn.archko.pdf.entity.MenuBean
import cn.archko.pdf.fragments.BookmarkFragment
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
    private var bookmarkFragment: BookmarkFragment? = null
    var itemListener: BookmarkFragment.ItemListener? = null
        set(value) {
            field = value
        }

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
        if (null == bookmarkFragment) {
            bookmarkFragment = BookmarkFragment()
            bookmarkFragment!!.itemListener = itemListener
        }
        supportFragmentManager.beginTransaction()
            .add(R.id.layout_outline, bookmarkFragment!!)
            .commit()

        if (null == outlineFragment) {
            outlineFragment = OutlineFragment()
            val bundle = Bundle()
            if (outlineHelper!!.hasOutline()) {
                bundle.putSerializable("OUTLINE", outlineHelper?.getOutline())
                ///bundle.putSerializable("out", outlineHelper?.getOutlineItems())
            }
            bundle.putSerializable("POSITION", currentPos)
            outlineFragment?.arguments = bundle
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.layout_outline, outlineFragment!!)
            .commit()
    }

    fun updateSelection(pos: Int) {
        supportFragmentManager.beginTransaction()
            .hide(bookmarkFragment!!)
            .show(outlineFragment!!)
            .commit()
        outlineFragment?.updateSelection(pos)
    }

    fun showBookmark(page: Int, list: List<Bookmark>?) {
        supportFragmentManager.beginTransaction()
            .hide(outlineFragment!!)
            .show(bookmarkFragment!!)
            .commit()

        if (list != null && list.isNotEmpty()) {
            bookmarkFragment?.updateBookmark(page, list)
        }
    }

    fun updateBookmark(page: Int, list: List<Bookmark>?) {
        if (list != null) {
            bookmarkFragment?.updateBookmark(page, list)
        }
    }

    fun isOutline(): Boolean {
        val fragment = supportFragmentManager.findFragmentById(R.id.layout_outline)
        return fragment is OutlineFragment && fragment.isVisible
    }
}
