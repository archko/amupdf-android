package cn.archko.pdf.core.component

import android.graphics.RectF
import cn.archko.pdf.core.entity.APage

/**
 * PageNode 对象池，用于减少高倍缩放时频繁创建几万个对象的开销
 * 经过优化后,最多只有十几个
 */
class PageNodePool {
    private val pool = ArrayDeque<PageNode>(32) // 预留初始容量

    // 获取一个 Node，如果没有则新建
    fun acquire(pageViewState: PageViewState, bounds: RectF, aPage: APage): PageNode {
        val node = pool.removeLastOrNull()
        return if (node != null) {
            // 先重置状态再重新赋值
            node.recycle()
            node.update(bounds, aPage)
            node
        } else {
            PageNode(pageViewState, bounds, aPage)
        }
    }

    // 回收 Node
    fun release(node: PageNode) {
        node.recycle() // 释放位图和 Job
        if (pool.size < 32) { // 限制池子大小，防止极端内存占用
            pool.addLast(node)
        }
    }

    fun clear() {
        pool.forEach { it.recycle() }
        pool.clear()
    }
}