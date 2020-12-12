package cn.archko.pdf.common

import android.app.Activity
import cn.archko.pdf.entity.OutlineItem
import cn.archko.pdf.mupdf.MupdfDocument
import com.artifex.mupdf.fitz.Outline
import com.artifex.mupdf.viewer.OutlineActivity

/**
 * @author: archko 2018/12/15 :9:11
 */
class OutlineHelper public constructor(
    private var mupdfDocument: MupdfDocument?,
    private var activity: Activity?
) {

    private var outline: Array<Outline>? = null
    private var items: ArrayList<OutlineActivity.Item>? = null
    private var outlineItems: ArrayList<OutlineItem>? = null

    fun getOutline(): ArrayList<OutlineActivity.Item> {
        if (null != items) {
            return items!!
        } else {
            items = ArrayList<OutlineActivity.Item>()
            flattenOutlineNodes(items!!, outline, " ")
        }
        return items!!
    }

    private fun flattenOutlineNodes(
        result: ArrayList<OutlineActivity.Item>,
        list: Array<Outline>?,
        indent: String
    ) {
        for (node in list!!) {
            if (node.title != null) {
                val page = mupdfDocument?.pageNumberFromLocation(node)
                result.add(OutlineActivity.Item(indent + node.title, page!!))
            }
            if (node.down != null) {
                flattenOutlineNodes(result, node.down, "$indent  ")
            }
        }
    }

    companion object {
        var nodeId: Int = 0
    }

    fun getOutlineItems(): ArrayList<OutlineItem> {
        if (null != outlineItems) {
            return outlineItems!!
        } else {
            outlineItems = ArrayList()
            nodeId = 1
            flattenOutlineItems(OutlineItem(0, 0, "Content"), outlineItems!!, outline, " ")
        }
        return outlineItems!!
    }

    private fun flattenOutlineItems(
        parent: OutlineItem,
        result: ArrayList<OutlineItem>,
        list: Array<Outline>?,
        indent: String
    ) {
        for (node in list!!) {
            val element = OutlineItem(nodeId++, parent.id, node.title)
            result.add(element)
            if (node.title != null) {
                val page = mupdfDocument?.pageNumberFromLocation(node)
                element.page = page!!
            }
            if (node.down != null) {
                flattenOutlineItems(element, result, node.down, "$indent  ")
            }
        }
    }

    fun hasOutline(): Boolean {
        if (outline == null) {
            outline = mupdfDocument?.loadOutline()
        }
        return outline != null
    }
}
