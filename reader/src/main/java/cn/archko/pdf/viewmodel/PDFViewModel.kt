package cn.archko.pdf.viewmodel

import android.util.SparseArray
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.archko.pdf.App
import cn.archko.pdf.common.APageSizeLoader
import cn.archko.pdf.common.PDFBookmarkManager
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.entity.APage
import cn.archko.pdf.entity.BookProgress
import cn.archko.pdf.entity.Bookmark
import cn.archko.pdf.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PDFViewModel : ViewModel() {

    protected var pdfBookmarkManager: PDFBookmarkManager = PDFBookmarkManager()
    var bookmarks: List<Bookmark>? = arrayListOf()
        get() = field

    val uiBookmarksLiveData = MutableLiveData<List<Bookmark>?>()
        get() = field

    suspend fun loadBookmark(path: String, optionRepository: PdfOptionRepository): BookProgress? {
        val bookProgress = withContext(Dispatchers.IO) {
            val mCrop = optionRepository.pdfOptionFlow.first().autocrop

            var autoCrop = 0
            if (!mCrop) {
                autoCrop = 1
            }
            pdfBookmarkManager.setReadProgress(path, autoCrop)
            val bookProgress: BookProgress? = pdfBookmarkManager.bookProgress
            bookmarks = pdfBookmarkManager.getBookmarks()
            bookProgress
        }
        return bookProgress
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                pdfBookmarkManager.deleteBookmark(bookmark)
                bookmarks?.minus(bookmark)
                uiBookmarksLiveData.postValue(bookmarks)
            }
        }
    }

    suspend fun pause(mCrop: Boolean, mReflow: Boolean) {
        withContext(Dispatchers.IO) {
            if (mCrop) {
                pdfBookmarkManager.bookProgress?.autoCrop = 0
            } else {
                pdfBookmarkManager.bookProgress?.autoCrop = 1
            }
            if (mReflow) {
                pdfBookmarkManager.bookProgress?.reflow = 1
            } else {
                pdfBookmarkManager.bookProgress?.reflow = 0
            }
        }
    }

    fun savePageSize(mCrop: Boolean, mPageSizes: SparseArray<APage>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                APageSizeLoader.savePageSizeToFile(
                    mCrop,
                    pdfBookmarkManager.bookProgress!!.size,
                    mPageSizes,
                    FileUtils.getDiskCacheDir(
                        App.instance,
                        pdfBookmarkManager.bookProgress?.name
                    )
                )
            }
        }
    }

    suspend fun preparePageSize(width: Int): APageSizeLoader.PageSizeBean? {
        return withContext(Dispatchers.IO) {
            var pageSizeBean: APageSizeLoader.PageSizeBean? = null
            if (pdfBookmarkManager.bookProgress != null) {
                pageSizeBean = APageSizeLoader.loadPageSizeFromFile(
                    width,
                    pdfBookmarkManager.bookProgress!!.pageCount,
                    pdfBookmarkManager.bookProgress!!.size,
                    FileUtils.getDiskCacheDir(
                        App.instance,
                        pdfBookmarkManager.bookProgress?.name
                    )
                )
            }
            pageSizeBean
        }
    }

    fun restoreReadProgress(pageCount: Int): Int {
        return pdfBookmarkManager.restoreReadProgress(pageCount)
    }

    fun readPage(): Int {
        if (pdfBookmarkManager.bookProgress == null) {
            return 0
        }
        var currentPage = 0
        if (0 < pdfBookmarkManager.bookProgress!!.page) {
            currentPage = pdfBookmarkManager.bookProgress!!.page
        }
        return currentPage
    }

    fun getBookProgress(): BookProgress? {
        return pdfBookmarkManager.bookProgress
    }

    fun saveCurrentPage(
        absolutePath: String?,
        pageCount: Int,
        currentPage: Int,
        zoom: Float,
        scrollX: Int,
        scrollY: Int
    ) {
        pdfBookmarkManager.saveCurrentPage(
            absolutePath,
            pageCount,
            currentPage,
            zoom,
            scrollX,
            scrollY
        )
    }
}