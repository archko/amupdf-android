package cn.archko.pdf.entity

import cn.archko.pdf.tree.RvTree
import java.io.Serializable

/**
 * @author: archko 2020/10/31 :11:07 AM
 */
class OutlineItem(var id: Int, private var pid: Int, private var title: String) : RvTree,
    Serializable {

    constructor(title: String, page: Int) : this(0, 0, title) {
        this.title = title
        this.page = page
    }

    var page = 0
    var resId = 0
    override fun getNid(): Long {
        return id.toLong()
    }

    override fun getPid(): Long {
        return pid.toLong()
    }

    fun setPid(pid: Int) {
        this.pid = pid
    }

    override fun getTitle(): String {
        return title
    }

    fun setTitle(title: String) {
        this.title = title
    }

    override fun getImageResId(): Int {
        return 0
    }

    override fun toString(): String {
        return "OutlineItem(id=$id, pid=$pid, title='$title', page=$page, resId=$resId)"
    }

}