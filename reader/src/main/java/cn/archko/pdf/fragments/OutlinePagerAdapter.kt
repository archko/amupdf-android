package cn.archko.pdf.fragments

import android.os.Bundle
import android.util.SparseArray
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import cn.archko.pdf.common.AnnotationManager
import cn.archko.pdf.viewmodel.BookmarkViewModel
import org.vudroid.core.codec.OutlineLink
import java.lang.ref.WeakReference

/**
 * OutlineTabFragment的ViewPager适配器
 * @author: archko 2026/3/9
 */
class OutlinePagerAdapter(
    fragment: Fragment,
    var bookmarkViewModel: BookmarkViewModel,
    var annotationManager: AnnotationManager?,
    var outlineItems: List<OutlineLink>?,
    val arguments: Bundle?
) : FragmentStateAdapter(fragment) {

    companion object {
        const val TAB_COUNT = 3

        const val TAB_OUTLINE = 0
        const val TAB_ANNOTATION = 1
        const val TAB_BOOKMARK = 2
    }

    override fun getItemCount(): Int = TAB_COUNT

    private val mFragmentArray = SparseArray<WeakReference<Fragment>>()

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun createFragment(position: Int): Fragment {
        val mWeakFragment = mFragmentArray.get(position)
        if (mWeakFragment?.get() != null) {
            if (mWeakFragment.get() is OutlineFragment) {
                val fragment = mWeakFragment.get() as OutlineFragment
                OutlineFragment.updateArgs(fragment, arguments)
            }
            return mWeakFragment.get()!!
        }

        val fragment = when (position) {
            TAB_OUTLINE -> {
                OutlineFragment.newInstance(arguments, outlineItems)
            }

            TAB_ANNOTATION -> {
                AnnotationFragment.newInstance(annotationManager)
            }

            TAB_BOOKMARK -> {
                val path = arguments?.getString(OutlineTabFragment.ARG_PATH) ?: ""
                BookmarkFragment.newInstance(bookmarkViewModel, path)
            }

            else -> throw IllegalArgumentException("Invalid tab position: $position")
        }
        mFragmentArray.put(position, WeakReference(fragment))
        return fragment
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