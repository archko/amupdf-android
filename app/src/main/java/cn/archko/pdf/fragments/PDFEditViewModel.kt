package cn.archko.pdf.fragments

import android.content.Context
import android.graphics.PointF
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import cn.archko.pdf.core.App
import cn.archko.pdf.common.PDFCreaterHelper
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.decode.MupdfDocument
import com.artifex.mupdf.fitz.Outline
import com.artifex.mupdf.fitz.PDFDocument
import com.artifex.mupdf.fitz.Page

class PDFEditViewModel : MupdfListener {

    var pdfPath: String? = null
    var mupdfDocument: MupdfDocument? = null
    val aPageList = mutableListOf<APage>()
    var zoom = 1.0f
    var isEdit = false

    fun loadPdfDoc(context: Context, path: String, password: String?): List<APage>? {
        try {
            pdfPath = path
            if (null == mupdfDocument) {
                mupdfDocument = MupdfDocument(context)
            }
            Log.d("TAG", "loadPdfDoc.password:$password")
            mupdfDocument!!.newDocument(path, password)
            mupdfDocument!!.let {
                if (it.getDocument()!!.needsPassword()) {
                    Log.d("TAG", "needsPassword")
                    if (TextUtils.isEmpty(password)) {
                        return null
                    }
                    it.getDocument()!!.authenticatePassword(password)
                }
            }

            val cp = mupdfDocument!!.countPages()
            loadAllPageSize(cp)
            Log.d("TAG", "loadPdfDoc.cp:$cp, size:${aPageList.size}")
            return aPageList
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun loadAllPageSize(cp: Int): List<APage> {
        val aPage = loadPageSize(0)
        if (aPage != null) {
            for (i in 0 until cp) {
                val pointf = PointF(aPage.width, aPage.height)
                val page = APage(i, pointf, 1f, 0)
                aPageList.add(page)
            }
        }
        return aPageList
    }

    private fun loadPageSize(page: Int): APage? {
        val p = mupdfDocument?.loadPage(page) ?: return null

        //Logcat.d(TAG, "open:getPageSize.$pageNum page:$p")
        val b = p.bounds
        val w = b.x1 - b.x0
        val h = b.y1 - b.y0
        p.destroy()
        return APage(page, PointF(w, h), 1.0f, 0)
    }

    fun destroy() {
        mupdfDocument?.destroy()
    }

    fun loadPage(page: Int): Page? {
        return mupdfDocument?.loadPage(page)
    }

    fun countPages(): Int {
        return if (null == mupdfDocument) {
            0
        } else {
            mupdfDocument!!.countPages()
        }
    }

    fun loadOutline(): Array<Outline>? {
        return mupdfDocument?.loadOutline()
    }

    fun deletePage(page: Int) {
        if (null != mupdfDocument) {
            isEdit = true
            val pdfDocument = mupdfDocument!!.getDocument() as PDFDocument
            pdfDocument.deletePage(page)
            aPageList.removeAt(page)
            Log.d(
                "TAG",
                "deletePage.$page, cp:${pdfDocument.countPages()}, size:${aPageList.size}"
            )
        }
    }

    fun insertPage(page: Int, path: String) {
        if (null != mupdfDocument) {
            isEdit = true
            val pdfDocument = mupdfDocument!!.getDocument() as PDFDocument
            Log.d(
                "TAG",
                "insertPage.$page, cp:${pdfDocument.countPages()}, size:${aPageList.size}"
            )
        }
    }

    fun save() {
        if (null != mupdfDocument && isEdit) {
            val pdfDocument = mupdfDocument!!.getDocument() as PDFDocument
            pdfDocument.save(pdfPath, PDFCreaterHelper.OPTS)
            Toast.makeText(App.instance, "保存成功", Toast.LENGTH_SHORT).show()
            isEdit = false
        } else {
            Toast.makeText(App.instance, "未修改pdf", Toast.LENGTH_SHORT).show()
        }
    }

    fun extract(position: Int) {
    }

    override fun getPageCount(): Int {
        return return aPageList.size
    }

    override fun getDocument(): MupdfDocument? {
        return mupdfDocument
    }

    override fun getPageList(): List<APage> {
        return aPageList
    }

}