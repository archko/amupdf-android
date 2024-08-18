package cn.archko.pdf.utils

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.geometry.Offset
import cn.archko.pdf.core.decode.MupdfDocument
import cn.archko.pdf.core.entity.APage
import cn.archko.pdf.core.link.Hyperlink
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Link
import com.artifex.mupdf.fitz.Location
import com.artifex.mupdf.fitz.Page

class LinkUtils {

    companion object {

        fun mapPointToPage(doc: Document?, pdfPage: Page, atX: Float, atY: Float): Hyperlink? {
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
                    intent.action = "android.intent.action.VIEW"
                    val content_url = Uri.parse(url)
                    intent.data = content_url
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        suspend fun hyperLink(
            context: Context,
            offset: Offset,
            index: Int,
            mupdfDocument: MupdfDocument,
            listState: LazyListState,
            aPage: APage,
            lazyListItemInfo: LazyListItemInfo?
        ): Boolean {
            val page = mupdfDocument.loadPage(index)
            if (null != page) {
                if (lazyListItemInfo == null) {
                    return false
                }
                val scale = getFactor(page, aPage)
                val x = (offset.x - 0) / scale
                val y = (offset.y - lazyListItemInfo.offset) / scale
                val link: Hyperlink? =
                    mapPointToPage(mupdfDocument.getDocument(), page, x, y)
                if (link != null) {
                    Log.d(
                        "PDFSDK", "link:$link"
                    )
                    return if (Hyperlink.LINKTYPE_URL == link.linkType) {
                        openSystemBrowser(context, link.url)
                        true
                    } else {
                        listState.scrollToItem(link.page, 0)
                        true
                    }
                }
            }
            return false;
        }

        private fun getFactor(page: Page, aPage: APage): Float {
            return 1f * aPage.getCropWidth() / (page.bounds.x1 - page.bounds.x0)
        }
    }
}