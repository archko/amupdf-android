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
import androidx.lifecycle.ViewModelProvider
import cn.archko.pdf.R
import cn.archko.pdf.common.AnnotationManager
import cn.archko.pdf.core.entity.ABookmark
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.viewmodel.BookmarkViewModel
import com.google.android.material.tabs.TabLayoutMediator
import org.vudroid.core.codec.OutlineLink

/**
 * 大纲Tab容器Fragment - 包含大纲、批注、书签等多个tab
 * @author: archko 2026/3/9
 */
class OutlineTabFragment : DialogFragment() {

    private lateinit var tabLayout: com.google.android.material.tabs.TabLayout
    private lateinit var viewPager: androidx.viewpager2.widget.ViewPager2
    private lateinit var pagerAdapter: OutlinePagerAdapter
    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView

    // ViewModel和Manager
    var bookmarkViewModel: BookmarkViewModel? = null
    var annotationManager: AnnotationManager? = null
    private var currentPath: String = ""
    var outlineItems: ArrayList<OutlineLink>? = null
    var currentPage: Int = 0

    // 回调接口
    var onAnnotationClick: ((Int) -> Unit)? = null
    var onBookmarkClick: ((ABookmark) -> Unit)? = null
    var onEditBookmark: ((ABookmark) -> Unit)? = null

    companion object {
        private const val ARG_OUTLINE = "outline"
        private const val ARG_CURRENT_PAGE = "current_page"
        private const val ARG_PATH = "path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = R.style.AppTheme
        setStyle(STYLE_NORMAL, themeId)

        arguments?.let {
            currentPage = it.getInt(ARG_CURRENT_PAGE, 0)
            currentPath = it.getString(ARG_PATH, "")
            outlineItems = it.getSerializable(ARG_OUTLINE) as? ArrayList<OutlineLink>
        }

        // 初始化ViewModel
        bookmarkViewModel = ViewModelProvider(requireActivity()).get(BookmarkViewModel::class.java)

        // 初始化AnnotationManager
        if (currentPath.isNotEmpty()) {
            annotationManager = AnnotationManager(currentPath)
        }
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

        // 使用多tab布局文件
        val view = inflater.inflate(R.layout.fragment_outline_tabs, container, false)

        // 初始化UI组件
        btnBack = view.findViewById(R.id.btn_back)
        tvTitle = view.findViewById(R.id.tv_title)
        tabLayout = view.findViewById(R.id.tab_layout)
        viewPager = view.findViewById(R.id.view_pager)

        // 设置返回按钮点击事件
        btnBack.setOnClickListener {
            dismiss()
        }

        // 初始化ViewPager和Adapter
        pagerAdapter = OutlinePagerAdapter(requireActivity(), this)
        viewPager.adapter = pagerAdapter

        // 设置TabLayout和ViewPager的联动
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = pagerAdapter.getTabTitle(position)
        }.attach()

        // 加载书签数据
        if (currentPath.isNotEmpty()) {
            bookmarkViewModel?.loadBookmarks(currentPath)
        }

        return view
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
        // 不关闭对话框，让用户编辑
    }

    fun showDialog(
        activity: FragmentActivity?,
        currentPage: Int,
        outlineItems: ArrayList<OutlineLink>?,
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

        // 设置参数
        val args = Bundle().apply {
            putInt(ARG_CURRENT_PAGE, currentPage)
            putString(ARG_PATH, path)
            if (outlineItems != null) {
                putSerializable(ARG_OUTLINE, outlineItems)
            }
        }
        arguments = args

        // 设置回调
        this.onAnnotationClick = onAnnotationClick
        this.onBookmarkClick = onBookmarkClick
        this.onEditBookmark = onEditBookmark

        show(ft!!, "outline_tabs_dialog")
    }
}