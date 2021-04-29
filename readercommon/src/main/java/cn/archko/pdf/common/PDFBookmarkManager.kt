package cn.archko.pdf.common

import android.text.TextUtils
import cn.archko.pdf.AppExecutors
import cn.archko.pdf.entity.BookProgress
import cn.archko.pdf.utils.FileUtils
import java.io.File

/**
 * @author: archko 2018/7/22 :12:43
 */
class PDFBookmarkManager {
    var bookmarkToRestore: BookProgress? = null
        private set

    fun setStartBookmark(absolutePath: String?, autoCrop: Int) {
        val file = File(absolutePath)
        val progress = Graph.database.progressDao().getProgress(file.name)
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
        Logcat.i(
            Logcat.TAG,
            String.format("last page saved for currentPage:%s, :%s", currentPage, bookmarkToRestore)
        )

        AppExecutors.instance.diskIO().execute(Runnable {
            addToDb(bookmarkToRestore)
        })
    }

    fun addToDb(progress: BookProgress?) {
        if (null == progress || TextUtils.isEmpty(progress.path) || TextUtils.isEmpty(progress.name)) {
            Logcat.d("", "path is null.$progress")
            return
        }
        try {
            val progressDao = Graph.database.progressDao()
            val filepath = FileUtils.getStoragePath(progress.path)
            val file = File(filepath)
            var old = progressDao.getProgress(file.name)
            if (old == null) {
                old = progress
                old.lastTimestampe = System.currentTimeMillis()
                progressDao.addProgress(old)
            } else {
                progress.lastTimestampe = System.currentTimeMillis()
                progress.isFavorited = old.isFavorited
                progressDao.updateProgress(progress)
            }
            Logcat.i(Logcat.TAG, "onSuccess")
        } catch (e: Exception) {
            e.printStackTrace()
            Logcat.i(Logcat.TAG, "onFailed:$e")
        }
    }
}