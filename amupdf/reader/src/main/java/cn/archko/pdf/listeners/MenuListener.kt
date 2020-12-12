package cn.archko.pdf.listeners

import cn.archko.pdf.entity.MenuBean

/**
 * @author: archko 2019/7/12 :17:32
 */
interface MenuListener {

    fun onMenuSelected(data: MenuBean?, position: Int)
}