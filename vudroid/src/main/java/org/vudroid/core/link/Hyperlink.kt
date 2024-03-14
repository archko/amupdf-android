package org.vudroid.core.link

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.text.TextUtils
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Link
import com.artifex.mupdf.fitz.Location
import org.vudroid.core.Page

class Hyperlink {
    var linkType = LINKTYPE_PAGE
    var url: String? = null
    var page = 0
    var bbox: Rect? = null
    override fun toString(): String {
        return "Hyperlink{" +
                "linkType=" + linkType +
                ", page=" + page +
                ", bbox=" + bbox +
                ", url='" + url + '\'' +
                '}'
    }

    companion object {

        const val LINKTYPE_PAGE = 0
        const val LINKTYPE_URL = 1

        //documentview
        fun mapPointToPage(page: Page, atX: Float, atY: Float): Hyperlink? {
            if (null == page.links) {
                return null
            }
            for (hyper in page.links) {
                if (null != hyper.bbox && hyper.bbox!!.contains(atX.toInt(), atY.toInt())) {
                    return hyper
                }
            }
            return null
        }

        //controller
        fun mapPointToPage(
            doc: Document?,
            pdfPage: com.artifex.mupdf.fitz.Page,
            atX: Float,
            atY: Float
        ): Hyperlink? {
            if (null == doc) {
                return null
            }
            val links: Array<Link>? = pdfPage.links
            if (links.isNullOrEmpty()) {
                return null
            }
            for (link in links) {
                if (link.bounds.contains(atX, atY)) {
                    val hyper = Hyperlink()
                    val loc: Location = doc.resolveLink(link)
                    val page: Int = doc.pageNumberFromLocation(loc)
                    hyper.page = page
                    if (page >= 0) {
                        hyper.bbox = Rect(0, 0, 0, 0)
                        hyper.url = null
                        hyper.linkType = Hyperlink.LINKTYPE_PAGE
                    } else {
                        hyper.bbox = null
                        hyper.url = link.uri
                        hyper.linkType = Hyperlink.LINKTYPE_URL
                    }
                    return hyper
                }
            }
            return null
        }

        fun openSystemBrowser(context: Context?, url: String?) {
            if (context != null && !TextUtils.isEmpty(url)) {
                try {
                    val intent = Intent()
                    intent.setAction(Intent.ACTION_VIEW)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val uri = Uri.parse(url)
                    intent.setData(uri)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }


        /*fun mapPointToPage(
            doc: PdfDocument?,
            pdfPage: PdfPage,
            atX: Float,
            atY: Float,
            width: Int,
            height: Int
        ): Hyperlink? {
            if (null == doc) {
                return null
            }
            val links: List<PdfDocument.Link> = pdfPage.getPageLinks()
            if (links.isEmpty()) {
                return null
            }
            for (link in links) {
                *//*val mapped: Rect = pdfPage.mapRectToDevice(
                    pdfPage.pageIndex, 0, 0, width,
                    height, link.bounds
                )
                mapped.sort()*//*
                val rect =
                    RectF(link.bounds.left, link.bounds.bottom, link.bounds.right, link.bounds.top)
                if (rect.contains(atX, atY)) {
                    val hyper = Hyperlink()
                    val page: Int? = link.destPageIdx
                    if (page != null && page >= 0) {
                        hyper.page = page
                        //hyper.bbox = link.bounds
                        hyper.url = null
                        hyper.linkType = Hyperlink.LINKTYPE_PAGE
                    } else {
                        hyper.bbox = null
                        hyper.url = link.uri
                        hyper.linkType = Hyperlink.LINKTYPE_URL
                    }
                    return hyper
                }
            }
            return null
        }*/
    }
}