package cn.archko.pdf.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import cn.archko.pdf.R
import cn.archko.pdf.core.common.AnnotationManager
import cn.archko.pdf.core.entity.ABookmark
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.viewmodel.BookmarkViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.vudroid.core.codec.OutlineLink

/**
 * 大纲Tab容器Fragment - 包含大纲、批注、书签等多个tab
 * @author: archko 2026/3/9
 */
class OutlineTabFragment : DialogFragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var pagerAdapter: OutlinePagerAdapter

    private var bookmarkViewModel: BookmarkViewModel? = null
    private var annotationManager: AnnotationManager? = null
    private var outlineItems: List<OutlineLink>? = null

    // 回调接口
    var onItemClick: ((Int) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themeId = R.style.AppTheme
        setStyle(STYLE_NORMAL, themeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.apply {
            val lp: WindowManager.LayoutParams = window!!.attributes
            lp.dimAmount = 0f
            setCanceledOnTouchOutside(true)
            setCancelable(true)
            // 设置宽度为屏幕宽度的 85%
            val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
            val height = (resources.displayMetrics.heightPixels * 0.85).toInt()
            window?.setLayout(width, height)
        }

        val view = inflater.inflate(R.layout.fragment_outline_tabs, container, false)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        tabLayout = view.findViewById(R.id.tab_layout)
        viewPager = view.findViewById(R.id.view_pager)

        toolbar.setNavigationOnClickListener({ dismiss() })

        pagerAdapter = OutlinePagerAdapter(
            this,
            bookmarkViewModel!!,
            annotationManager,
            outlineItems,
            arguments
        )
        viewPager.adapter = pagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = pagerAdapter.getTabTitle(position)
        }.attach()

        return view
    }

    fun onListItemClick(targetPage: Int) {
        (activity as? OutlineListener)?.onSelectedOutline(targetPage)
        dismiss()
    }

    // 新增：批注点击回调
    fun onAnnotationClick(pageIndex: Int) {
        onItemClick?.invoke(pageIndex)
        dismiss()
    }

    // 新增：书签点击回调
    fun onBookmarkClick(bookmark: ABookmark) {
        onItemClick?.invoke(bookmark.pageIndex)
        dismiss()
    }

    companion object {
        const val TAG = "outline_tabs_dialog"
        const val ARG_OUTLINE = "outline"
        const val ARG_CURRENT_PAGE = "current_page"
        const val ARG_PATH = "path"

        /**
         * 3. 标准工厂方法：每次显示都调用这个，返回全新实例
         */
        fun newInstance(currentPage: Int, path: String): OutlineTabFragment {
            val fragment = OutlineTabFragment()
            val args = Bundle()
            args.putInt(ARG_CURRENT_PAGE, currentPage)
            args.putString(ARG_PATH, path)
            fragment.arguments = args
            return fragment
        }

        fun showDialog(
            activity: FragmentActivity,
            bookmarkViewModel: BookmarkViewModel,
            annotationManager: AnnotationManager?,
            currentPage: Int,
            outlineItems: List<OutlineLink>?,
            path: String,
            onItemClick: ((Int) -> Unit)? = null,
        ) {
            val fragmentManager = activity.supportFragmentManager

            val existing = fragmentManager.findFragmentByTag(TAG)
            if (existing is OutlineTabFragment && existing.isVisible) {
                return
            }

            val newDialog = newInstance(currentPage, path)
            val args = Bundle().apply {
                putInt(ARG_CURRENT_PAGE, currentPage)
                putString(ARG_PATH, path)
            }
            newDialog.bookmarkViewModel = bookmarkViewModel
            newDialog.annotationManager = annotationManager
            newDialog.outlineItems = outlineItems
            newDialog.arguments = args

            newDialog.onItemClick = onItemClick

            newDialog.show(fragmentManager, TAG)
        }
    }
}