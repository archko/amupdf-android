package cn.archko.pdf.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import cn.archko.pdf.R
import cn.archko.pdf.common.AnnotationManager
import cn.archko.pdf.core.entity.ABookmark
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.viewmodel.BookmarkViewModel
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
    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView

    private var bookmarkViewModel: BookmarkViewModel? = null
    private var annotationManager: AnnotationManager? = null
    private var outlineItems: List<OutlineLink>?=null

    // 回调接口
    var onAnnotationClick: ((Int) -> Unit)? = null
    var onBookmarkClick: ((ABookmark) -> Unit)? = null
    var onEditBookmark: ((ABookmark) -> Unit)? = null

    companion object {
        const val ARG_OUTLINE = "outline"
        const val ARG_CURRENT_PAGE = "current_page"
        const val ARG_PATH = "path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themeId = R.style.AppTheme
        setStyle(STYLE_NORMAL, themeId)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // 不保存状态，避免ViewPager2恢复Fragment时崩溃
        // super.onSaveInstanceState(outState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val lp: WindowManager.LayoutParams = window!!.attributes
            lp.dimAmount = 0f
            lp.height =
                ((Utils.getScreenHeightPixelWithOrientation(requireActivity()) * 0.9f).toInt())
            lp.width = (Utils.getScreenWidthPixelWithOrientation(requireActivity()) * 0.9f).toInt()
            window!!.attributes = lp
            setCanceledOnTouchOutside(true)
            setCancelable(true)
        }

        val view = inflater.inflate(R.layout.fragment_outline_tabs, container, false)

        btnBack = view.findViewById(R.id.btn_back)
        tvTitle = view.findViewById(R.id.tv_title)
        tabLayout = view.findViewById(R.id.tab_layout)
        viewPager = view.findViewById(R.id.view_pager)

        btnBack.setOnClickListener {
            dismiss()
        }

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
        val ac = activity as OutlineListener
        ac.onSelectedOutline(targetPage)
        dismiss()
    }

    // 新增：批注点击回调
    fun onAnnotationClick(pageIndex: Int) {
        onAnnotationClick?.invoke(pageIndex)
        dismiss()
    }

    // 新增：书签点击回调
    fun onBookmarkClick(bookmark: ABookmark) {
        onBookmarkClick?.invoke(bookmark)
        dismiss()
    }

    // 新增：编辑书签回调
    fun onEditBookmark(bookmark: ABookmark) {
        onEditBookmark?.invoke(bookmark)
    }

    fun showDialog(
        activity: FragmentActivity?,
        bookmarkViewModel: BookmarkViewModel,
        annotationManager: AnnotationManager?,
        currentPage: Int,
        outlineItems: List<OutlineLink>?,
        path: String,
        onAnnotationClick: ((Int) -> Unit)? = null,
        onBookmarkClick: ((ABookmark) -> Unit)? = null,
        onEditBookmark: ((ABookmark) -> Unit)? = null
    ) {
        val ft = activity?.supportFragmentManager?.beginTransaction()
        val prev = activity?.supportFragmentManager?.findFragmentByTag("outline_tabs_dialog")
        if (prev != null) {
            ft?.remove(prev)
        }
        ft?.addToBackStack(null)

        val args = Bundle().apply {
            putInt(ARG_CURRENT_PAGE, currentPage)
            putString(ARG_PATH, path)
        }
        this.bookmarkViewModel = bookmarkViewModel
        this.annotationManager = annotationManager
        this.outlineItems = outlineItems
        arguments = args

        this.onAnnotationClick = onAnnotationClick
        this.onBookmarkClick = onBookmarkClick
        this.onEditBookmark = onEditBookmark

        show(ft!!, "outline_tabs_dialog")
    }
}