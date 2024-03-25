package cn.archko.pdf.fragments

import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.decode.MupdfDocument

interface MupdfListener {

    fun getPageCount(): Int
    fun getDocument(): MupdfDocument?
    fun getPageList(): List<APage>
}