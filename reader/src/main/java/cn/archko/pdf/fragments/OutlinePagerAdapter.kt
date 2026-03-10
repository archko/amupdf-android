package cn.archko.pdf.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import cn.archko.pdf.common.AnnotationManager
import cn.archko.pdf.viewmodel.BookmarkViewModel
import org.vudroid.core.codec.OutlineLink

/**
 * OutlineTabFragment的ViewPager适配器
 * @author: archko 2026/3/9
 */
class OutlinePagerAdapter(
    fragmentActivity: FragmentActivity,
    var bookmarkViewModel: BookmarkViewModel,
    var annotationManager: AnnotationManager?,
    var outlineItems: List<OutlineLink>?,
    val arguments: Bundle?
) : FragmentStateAdapter(fragmentActivity) {

    companion object {
        const val TAB_COUNT = 3

        const val TAB_OUTLINE = 0
        const val TAB_ANNOTATION = 1
        const val TAB_BOOKMARK = 2
    }

    override fun getItemCount(): Int = TAB_COUNT

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            TAB_OUTLINE -> {
                OutlineFragment.newInstance(arguments, outlineItems)
            }

            TAB_ANNOTATION -> {
                AnnotationFragment.newInstance(annotationManager)
            }

            TAB_BOOKMARK -> {
                BookmarkFragment.newInstance(bookmarkViewModel)
            }

            else -> throw IllegalArgumentException("Invalid tab position: $position")
        }
    }

    fun getTabTitle(position: Int): String {
        return when (position) {
            TAB_OUTLINE -> "大纲"
            TAB_ANNOTATION -> "批注"
            TAB_BOOKMARK -> "书签"
            else -> ""
        }
    }
}