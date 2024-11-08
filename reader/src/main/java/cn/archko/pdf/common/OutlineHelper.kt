package cn.archko.pdf.common

import android.app.Activity
import cn.archko.pdf.core.decode.MupdfDocument
import cn.archko.pdf.entity.OutlineItem
import com.artifex.mupdf.fitz.Outline

/**
 * @author: archko 2018/12/15 :9:11
 */
class OutlineHelper(
    private var mupdfDocument: MupdfDocument?,
    private var activity: Activity?
) {

    private var outline: Array<Outline>? = null
    private var items: ArrayList<OutlineItem>? = null
    private var outlineItems: ArrayList<OutlineItem>? = null

    fun getOutline(): ArrayList<OutlineItem> {
        if (null != items) {
            return items!!
        } else {
            items = ArrayList<OutlineItem>()
            flattenOutlineNodes(items!!, outline, " ")
        }
        return items!!
    }

    private fun flattenOutlineNodes(
        result: ArrayList<OutlineItem>,
        list: Array<Outline>?,
        indent: String
    ) {
        for (node in list!!) {
            if (node.title != null) {
                val page = mupdfDocument?.pageNumberFromLocation(node)
                result.add(OutlineItem(indent + node.title, page!!))
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
        if (mupdfDocument == null) {
            return
        }
        for (node in list!!) {
            val element = OutlineItem(nodeId++, parent.id, node.title)
            result.add(element)
            if (node.title != null) {
                val page = mupdfDocument!!.pageNumberFromLocation(node)
                element.page = page
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
