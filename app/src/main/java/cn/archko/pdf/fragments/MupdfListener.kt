package cn.archko.pdf.fragments

import cn.archko.pdf.entity.APage
import cn.archko.pdf.common.MupdfDocument

interface MupdfListener {

    fun getPageCount(): Int
    fun getDocument(): MupdfDocument?
    fun getPageList(): List<APage>
}