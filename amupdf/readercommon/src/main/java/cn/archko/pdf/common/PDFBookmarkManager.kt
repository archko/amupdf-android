package cn.archko.pdf.common

import android.util.Log
import cn.archko.pdf.entity.BookProgress
import cn.archko.pdf.listeners.DataListener
import cn.archko.pdf.utils.FileUtils

/**
 * @author: archko 2018/7/22 :12:43
 */
class PDFBookmarkManager {
    var bookmarkToRestore: BookProgress? = null
        private set

    fun setStartBookmark(absolutePath: String?, autoCrop: Int) {
        val progress = RecentManager.instance.readRecentFromDb(absolutePath, BookProgress.ALL)
        bookmarkToRestore = progress
        if (null == bookmarkToRestore) {
            bookmarkToRestore = BookProgress(FileUtils.getRealPath(absolutePath))
            bookmarkToRestore!!.autoCrop = autoCrop
        }
        bookmarkToRestore!!.readTimes = bookmarkToRestore!!.readTimes
        bookmarkToRestore!!.inRecent = 0
    }

    val bookmark: Int
        get() {
            if (bookmarkToRestore == null) {
                return 0
            }
            var currentPage = 0
            if (0 < bookmarkToRestore!!.page) {
                currentPage = bookmarkToRestore!!.page
            }
            return currentPage
        }

    fun restoreBookmark(pageCount: Int): Int {
        if (bookmarkToRestore == null) {
            return 0
        }
        var currentPage = 0
        if (bookmarkToRestore!!.pageCount != pageCount || bookmarkToRestore!!.page > pageCount) {
            bookmarkToRestore!!.pageCount = pageCount
            bookmarkToRestore!!.page =
                if (bookmarkToRestore!!.page >= pageCount) 0 else bookmarkToRestore!!.page
            return currentPage
        }
        if (0 < bookmarkToRestore!!.page) {
            currentPage = bookmarkToRestore!!.page
        }
        return currentPage
    }

    fun saveCurrentPage(
        absolutePath: String?,
        pageCount: Int,
        currentPage: Int,
        zoom: Float,
        scrollX: Int,
        scrollY: Int
    ) {
        if (null == bookmarkToRestore) {
            bookmarkToRestore = BookProgress(FileUtils.getRealPath(absolutePath))
        } else {
            bookmarkToRestore!!.path = FileUtils.getRealPath(absolutePath)
            bookmarkToRestore!!.readTimes = bookmarkToRestore!!.readTimes + 1
        }
        bookmarkToRestore!!.inRecent = 0
        bookmarkToRestore!!.pageCount = pageCount
        bookmarkToRestore!!.page = currentPage
        //if (zoom != 1000f) {
        bookmarkToRestore!!.zoomLevel = zoom
        //}
        if (scrollX >= 0) { //for mupdfrecycleractivity,don't modify scrollx
            bookmarkToRestore!!.offsetX = scrollX
        }
        bookmarkToRestore!!.offsetY = scrollY
        bookmarkToRestore!!.progress = currentPage * 100 / pageCount
        Log.i(
            RecentManager.TAG,
            String.format("last page saved for currentPage:%s, :%s", currentPage, bookmarkToRestore)
        )
        RecentManager.instance.addAsyncToDB(bookmarkToRestore,
            object : DataListener {
                override fun onSuccess(vararg args: Any) {
                    Log.i(RecentManager.TAG, "onSuccess")
                }

                override fun onFailed(vararg args: Any) {
                    Log.i(RecentManager.TAG, "onFailed")
                }
            })
    }
}