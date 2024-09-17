package cn.archko.pdf.viewmodel

import android.text.TextUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.core.common.Graph
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.BookProgress
import cn.archko.pdf.core.entity.Bookmark
import cn.archko.pdf.core.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DocViewModel : ViewModel() {

    var bookmarks: List<Bookmark>? = null

    //every book should have a progress,event if not store in db.
    var bookProgress: BookProgress? = null
        private set

    private fun loadBookmarks(): List<Bookmark>? {
        try {
            val progressDao = Graph.database.progressDao()
            if (null != bookProgress && !TextUtils.isEmpty(bookProgress!!.path)) {
                return progressDao.getBookmark(bookProgress!!.path!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Logcat.i(Logcat.TAG, "deleteBookmark failed:$e")
        }
        return null
    }

    private fun loadProgressAndBookmark(absolutePath: String, autoCrop: Int) {
        val file = File(absolutePath)
        if (file.isDirectory) {
            return
        }
        val progress = Graph.database.progressDao().getProgress(file.name)
        bookProgress = progress
        if (null == bookProgress) {
            bookProgress = BookProgress(FileUtils.getRealPath(absolutePath))
            bookProgress!!.autoCrop = autoCrop
            bookProgress!!._id = Graph.database.progressDao().addProgress(bookProgress!!).toInt()
        }
        bookProgress!!.readTimes += 1
        bookProgress!!.inRecent = BookProgress.IN_RECENT

        bookmarks = loadBookmarks()
        Logcat.i(
            Logcat.TAG,
            String.format(
                "loadProgressAndBookmark autoCrop:%s, path:%s, progress:%s,bookmark:%s",
                autoCrop,
                absolutePath,
                bookProgress,
                bookmarks
            )
        )
    }

    fun getCurrentPage(): Int {
        if (bookProgress == null) {
            return 0
        }
        var currentPage = 0
        if (0 < bookProgress!!.page) {
            currentPage = bookProgress!!.page
        }
        return currentPage
    }

    fun setCurrentPage(pos: Int) {
        bookProgress?.page = pos
    }

    fun getCurrentPage(pageCount: Int): Int {
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

    private fun addToDb(progress: BookProgress?) {
        if (null == progress || TextUtils.isEmpty(progress.path) || TextUtils.isEmpty(progress.name)) {
            Logcat.d("", "path is null.$progress")
            return
        }
        try {
            val progressDao = Graph.database.progressDao()
            val filepath = FileUtils.getStoragePath(progress.path)
            val file = File(filepath)
            var old = progressDao.getProgress(file.name)
            Logcat.i(Logcat.TAG, "old:$old")
            if (old == null) {
                old = progress
                old.lastTimestampe = System.currentTimeMillis()
                progressDao.addProgress(old)
            } else {
                progress.lastTimestampe = System.currentTimeMillis()
                progress.isFavorited = old.isFavorited
                progressDao.updateProgress(progress)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Logcat.i(Logcat.TAG, "onFailed:$e")
        }
    }

    suspend fun loadBookProgressByPath(path: String): BookProgress? {
        val bookProgress = withContext(Dispatchers.IO) {
            val crop = PdfOptionRepository.getAutocrop()

            var autoCrop = 0
            if (!crop) {
                autoCrop = 1
            }
            loadProgressAndBookmark(path, autoCrop)
            bookProgress
        }
        return bookProgress
    }

    suspend fun deleteBookmark(bookmark: Bookmark?) = flow {
        if (null != bookmark && bookProgress != null) {
            val progressDao = Graph.database.progressDao()
            progressDao.deleteBookmark(bookmark._id)
            bookmarks = bookmarks?.minus(bookmark)
        }
        emit(bookmarks)
    }.flowOn(Dispatchers.IO)

    suspend fun addBookmark(page: Int) = flow {
        val bookProgress = bookProgress
        bookProgress?.let {
            val bookmark = Bookmark()
            bookmark.page = page
            bookmark.progressId = bookProgress._id
            bookmark.path = bookProgress.path
            bookmark.createAt = System.currentTimeMillis()

            Graph.database.progressDao().addBookmark(bookmark)
            bookmarks = loadBookmarks()
        }
        emit(bookmarks)
    }.flowOn(Dispatchers.IO)

    fun storeCrop(crop: Boolean) {
        if (crop) {
            bookProgress?.autoCrop = 0
        } else {
            bookProgress?.autoCrop = 1
        }
    }

    fun storeReflow(reflow: Int) {
        bookProgress?.reflow = reflow
    }

    fun checkCrop(): Boolean {
        if (null == bookProgress) {
            return PdfOptionRepository.getAutocrop()
        }
        return bookProgress!!.autoCrop == 0
    }

    fun getReflow(): Int {
        return bookProgress?.reflow ?: 0
    }

    fun saveBookProgress(
        absolutePath: String?,
        pageCount: Int,
        currentPage: Int,
        zoom: Float,
        scrollX: Int,
        scrollY: Int
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (null == bookProgress) {
                    bookProgress = BookProgress(FileUtils.getRealPath(absolutePath))
                } else {
                    bookProgress!!.path = FileUtils.getRealPath(absolutePath)
                }
                var pc = pageCount
                if (pc < 1) {
                    pc = 1
                }
                bookProgress!!.inRecent = 0
                bookProgress!!.pageCount = pc
                bookProgress!!.page = currentPage
                //if (zoom != 1000f) {
                bookProgress!!.zoomLevel = zoom
                //}
                if (scrollX >= 0) { //for mupdfrecycleractivity,don't modify scrollx
                    bookProgress!!.offsetX = scrollX
                }
                bookProgress!!.offsetY = scrollY
                bookProgress!!.progress = currentPage * 100 / pc
                Logcat.i(
                    Logcat.TAG,
                    String.format(
                        "saveBookProgress:%s, :%s",
                        currentPage,
                        bookProgress
                    )
                )
                addToDb(bookProgress)
            }
        }
    }

    fun saveBookProgress(currentPage: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (null == bookProgress) {
                    return@withContext
                } else {
                    bookProgress!!.page = currentPage
                }
                bookProgress!!.progress = currentPage * 100 / bookProgress!!.pageCount
                Logcat.i(
                    Logcat.TAG,
                    String.format(
                        "saveBookProgress:%s, :%s",
                        currentPage,
                        bookProgress
                    )
                )
                addToDb(bookProgress)
            }
        }
    }

    /*suspend fun preparePageSize(width: Int) = flow {
        var pageSizeBean: APageSizeLoader.PageSizeBean? = null
        if (bookProgress != null) {
            pageSizeBean = APageSizeLoader.loadPageSizeFromFile(
                bookProgress!!.pageCount,
                FileUtils.getDiskCacheDir(
                    App.instance,
                    bookProgress?.name
                )
            )
        }
        emit(pageSizeBean)
    }.flowOn(Dispatchers.IO)*/

}