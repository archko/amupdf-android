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
    var bookProgress: BookProgress? = null
        private set

    fun setReadProgress(absolutePath: String?, autoCrop: Int) {
        val file = File(absolutePath)
        val progress = Graph.database.progressDao().getProgress(file.name)
        bookProgress = progress
        if (null == bookProgress) {
            bookProgress = BookProgress(FileUtils.getRealPath(absolutePath))
            bookProgress!!.autoCrop = autoCrop
        }
        bookProgress!!.readTimes = bookProgress!!.readTimes
        bookProgress!!.inRecent = 0
    }

    val readPage: Int
        get() {
            if (bookProgress == null) {
                return 0
            }
            var currentPage = 0
            if (0 < bookProgress!!.page) {
                currentPage = bookProgress!!.page
            }
            return currentPage
        }

    fun restoreReadProgress(pageCount: Int): Int {
        if (bookProgress == null) {
            return 0
        }
        var currentPage = 0
        if (bookProgress!!.pageCount != pageCount || bookProgress!!.page > pageCount) {
            bookProgress!!.pageCount = pageCount
            bookProgress!!.page =
                if (bookProgress!!.page >= pageCount) 0 else bookProgress!!.page
            return currentPage
        }
        if (0 < bookProgress!!.page) {
            currentPage = bookProgress!!.page
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
        if (null == bookProgress) {
            bookProgress = BookProgress(FileUtils.getRealPath(absolutePath))
        } else {
            bookProgress!!.path = FileUtils.getRealPath(absolutePath)
            bookProgress!!.readTimes = bookProgress!!.readTimes + 1
        }
        bookProgress!!.inRecent = 0
        bookProgress!!.pageCount = pageCount
        bookProgress!!.page = currentPage
        //if (zoom != 1000f) {
        bookProgress!!.zoomLevel = zoom
        //}
        if (scrollX >= 0) { //for mupdfrecycleractivity,don't modify scrollx
            bookProgress!!.offsetX = scrollX
        }
        bookProgress!!.offsetY = scrollY
        bookProgress!!.progress = currentPage * 100 / pageCount
        Logcat.i(
            Logcat.TAG,
            String.format("last page saved for currentPage:%s, :%s", currentPage, bookProgress)
        )

        AppExecutors.instance.diskIO().execute(Runnable {
            addToDb(bookProgress)
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