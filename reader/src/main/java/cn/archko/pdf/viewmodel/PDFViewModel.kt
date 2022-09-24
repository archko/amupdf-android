package cn.archko.pdf.viewmodel

import android.content.Context
import android.graphics.PointF
import android.text.TextUtils
import android.util.SparseArray
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.archko.pdf.App
import cn.archko.pdf.common.APageSizeLoader
import cn.archko.pdf.common.Graph
import cn.archko.pdf.common.IntentFile
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.OutlineHelper
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.common.TextHelper
import cn.archko.pdf.entity.APage
import cn.archko.pdf.entity.BookProgress
import cn.archko.pdf.entity.Bookmark
import cn.archko.pdf.entity.LoadResult
import cn.archko.pdf.entity.OutlineItem
import cn.archko.pdf.entity.ReflowBean
import cn.archko.pdf.entity.State
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.utils.FileUtils
import com.artifex.mupdf.fitz.Page
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PDFViewModel : ViewModel() {

    var pdfPath: String? = null
    var bookmarks: List<Bookmark>? = null

    var bookProgress: BookProgress? = null
        private set
    var mupdfDocument: MupdfDocument? = null
    val mPageSizes = mutableListOf<APage>()
    var txtPageCount: Int = 1
    var outlineHelper: OutlineHelper? = null

    fun loadPdfDoc(context: Context, path: String, password: String?) =
        flow {
            var state: State
            try {
                pdfPath = path
                if (null == mupdfDocument) {
                    mupdfDocument = MupdfDocument(context)
                }
                mupdfDocument!!.newDocument(path, password)
                mupdfDocument!!.let {
                    if (it.document.needsPassword()) {
                        Logcat.d(Logcat.TAG, "needsPassword")
                        if (TextUtils.isEmpty(password)) {
                            emit(
                                LoadResult(
                                    State.PASS,
                                )
                            )
                            return@flow
                        }
                        it.document.authenticatePassword(password)
                    }
                }
                state = State.FINISHED
            } catch (e: Exception) {
                e.printStackTrace()
                state = State.ERROR
            }
            val result = LoadResult<MupdfDocument, Any>(
                state,
                obj = mupdfDocument,
            )
            emit(result)
        }.flowOn(Dispatchers.IO)

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

    private fun loadProgressAndBookmark(absolutePath: String?, autoCrop: Int) {
        val file = File(absolutePath)
        val progress = Graph.database.progressDao().getProgress(file.name)
        bookProgress = progress
        if (null == bookProgress) {
            bookProgress = BookProgress(FileUtils.getRealPath(absolutePath))
            bookProgress!!.autoCrop = autoCrop
            bookProgress!!._id = Graph.database.progressDao().addProgress(bookProgress!!).toInt()
        }
        bookProgress!!.readTimes = bookProgress!!.readTimes + 1
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

    suspend fun loadBookProgressByPath(
        path: String,
        optionRepository: PdfOptionRepository
    ): BookProgress? {
        val bookProgress = withContext(Dispatchers.IO) {
            val crop = optionRepository.pdfOptionFlow.first().autocrop

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

    fun storeCropAndReflow(crop: Boolean, reflow: Boolean) {
        if (crop) {
            bookProgress?.autoCrop = 0
        } else {
            bookProgress?.autoCrop = 1
        }
        if (reflow) {
            bookProgress?.reflow = 1
        } else {
            bookProgress?.reflow = 0
        }
    }

    suspend fun savePageSize(crop: Boolean, pageSizes: SparseArray<APage>) = flow {
        APageSizeLoader.savePageSizeToFile(
            crop,
            bookProgress!!.size,
            pageSizes,
            FileUtils.getDiskCacheDir(
                App.instance,
                bookProgress?.name
            )
        )
        emit(null)
    }.flowOn(Dispatchers.IO)

    suspend fun preparePageSize(width: Int) = flow {
        var pageSizeBean: APageSizeLoader.PageSizeBean? = null
        if (bookProgress != null) {
            pageSizeBean = APageSizeLoader.loadPageSizeFromFile(
                width,
                bookProgress!!.pageCount,
                bookProgress!!.size,
                FileUtils.getDiskCacheDir(
                    App.instance,
                    bookProgress?.name
                )
            )
        }
        emit(pageSizeBean)
    }.flowOn(Dispatchers.IO)

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

    private val _textFlow = MutableStateFlow<LoadResult<Any, ReflowBean>>(LoadResult(State.INIT))
    val textFlow: StateFlow<LoadResult<Any, ReflowBean>>
        get() = _textFlow

    suspend fun loadTextDoc(path: String) = flow {
        pdfPath = path
        val reflowBeans = TextHelper.readString(path)
        txtPageCount = reflowBeans.size
        emit(reflowBeans)
    }.flowOn(Dispatchers.IO)
        .collectLatest {
            val state = if (it.isNotEmpty()) {
                State.FINISHED
            } else {
                State.ERROR
            }
            _textFlow.value = LoadResult(
                state,
                list = it
            )
        }

    private val _pageFlow = MutableStateFlow<LoadResult<Any, APage>>(LoadResult(State.INIT))
    val pageFlow: StateFlow<LoadResult<Any, APage>>
        get() = _pageFlow
    private val _outlineFlow =
        MutableStateFlow<LoadResult<Any, OutlineItem>>(LoadResult(State.INIT))
    val outlineFlow: StateFlow<LoadResult<Any, OutlineItem>>
        get() = _outlineFlow

    suspend fun loadPdfDoc2(context: Context, path: String, password: String?) = flow {
        try {
            pdfPath = path
            if (mupdfDocument == null) {
                mupdfDocument = MupdfDocument(context)
            }
            Logcat.d(Logcat.TAG, "loadPdfDoc2:$password")
            mupdfDocument!!.newDocument(path, password)
            mupdfDocument!!.let {
                if (it.document.needsPassword()) {
                    Logcat.d(Logcat.TAG, "needsPassword")
                    if (TextUtils.isEmpty(password)) {
                        emit(null)
                        return@flow
                    }
                    it.document.authenticatePassword(password)
                }
            }

            outlineHelper = OutlineHelper(mupdfDocument, null)
            val cp = mupdfDocument!!.countPages()
            emit(loadAllPageSize(cp))
        } catch (e: Exception) {
            e.printStackTrace()
            emit(listOf())
        }
    }.flowOn(Dispatchers.IO)
        .collectLatest {
            val state = if (null == it) {
                State.PASS
            } else if (it.isNotEmpty()) {
                State.FINISHED
            } else {
                State.ERROR
            }
            _pageFlow.value = LoadResult(
                state,
                list = it
            )
        }

    suspend fun loadOutline() = flow {
        if (mupdfDocument != null && null != outlineHelper && outlineHelper!!.hasOutline()) {
            emit(outlineHelper!!.getOutline())
            return@flow
        }
        emit(arrayListOf())
    }.flowOn(Dispatchers.IO)
        .collectLatest {
            _outlineFlow.value = LoadResult(
                State.FINISHED,
                list = it
            )
        }

    private fun loadAllPageSize(cp: Int): List<APage> {
        for (i in 0 until cp) {
            val pointF = loadPageSize(i)
            if (pointF != null) {
                mPageSizes.add(pointF)
            }
        }
        return mPageSizes
    }

    private fun loadPageSize(pageNum: Int): APage? {
        val p = mupdfDocument?.loadPage(pageNum) ?: return null

        //Logcat.d(TAG, "open:getPageSize.$pageNum page:$p")
        val b = p.bounds
        val w = b.x1 - b.x0
        val h = b.y1 - b.y0
        val pointf = PointF(w, h)
        p.destroy()
        return APage(pageNum, pointf, 1.0f/*zoomModel!!.zoom*/, 0)
    }

    fun destroy() {
        mupdfDocument?.destroy()
    }

    fun countPages(): Int {
        if (!TextUtils.isEmpty(pdfPath) && IntentFile.isText(pdfPath!!)) {
            return txtPageCount
        }
        val pc = mupdfDocument?.countPages() ?: 0
        bookProgress?.pageCount = pc
        return pc
    }

    fun loadPage(pageNum: Int): Page? {
        return mupdfDocument?.loadPage(pageNum)
    }
}