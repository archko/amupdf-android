package cn.archko.pdf.fragments

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.archko.pdf.common.Event
import cn.archko.pdf.common.Graph
import cn.archko.pdf.common.IntentFile
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.ProgressScaner
import cn.archko.pdf.entity.BookProgress
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.entity.LoadResult
import cn.archko.pdf.entity.State
import cn.archko.pdf.utils.FileUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileFilter
import java.util.Arrays
import java.util.Locale

/**
 * @author: archko 2020/11/16 :11:23
 */
class BookViewModel : ViewModel() {
    companion object {

        const val PAGE_SIZE = 20
        const val MAX_TIME = 1300L
    }

    private val progressDao by lazy { Graph.database.progressDao() }

    private var mScanner: ProgressScaner = ProgressScaner()

    private val _uiFileModel = MutableLiveData<List<FileBean>>()
    val uiFileModel: LiveData<List<FileBean>>
        get() = _uiFileModel

    private val _uiScannerModel = MutableLiveData<Array<Any?>>()
    val uiScannerModel: LiveData<Array<Any?>>
        get() = _uiScannerModel

    private val _uiItemModel = MutableLiveData<Boolean>()
    val uiItemModel: LiveData<Boolean>
        get() = _uiItemModel

    private val _uiFavoritiesModel =
        MutableStateFlow<LoadResult<Any, FileBean>>(LoadResult(State.INIT))
    val uiFavoritiesModel: StateFlow<LoadResult<Any, FileBean>>
        get() = _uiFavoritiesModel

    var sdcardRoot: String = "/sdcard/"
    private var dirsFirst: Boolean = true
    private var showExtension: Boolean = true
    private val fileFilter: FileFilter = FileFilter { file ->
        //return (file.isDirectory() || file.getName().toLowerCase().endsWith(".pdf"));
        if (file.isDirectory)
            return@FileFilter true
        val fname = file.name.toLowerCase(Locale.ROOT)

        return@FileFilter IntentFile.isPdf(fname)
                || IntentFile.isImage(fname)
                || IntentFile.isText(fname)
    }

    fun loadFiles(home: String, mCurrentPath: String?, dirsFirst: Boolean, showExtension: Boolean) =
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val fileList: ArrayList<FileBean> = ArrayList()
                var entry: FileBean

                entry = FileBean(FileBean.HOME, home)
                fileList.add(entry)
                if (mCurrentPath != "/") {
                    val upFolder = File(mCurrentPath!!).parentFile
                    entry = FileBean(FileBean.NORMAL, upFolder!!, "..")
                    fileList.add(entry)
                }
                val files = File(mCurrentPath).listFiles(fileFilter)
                if (files != null) {
                    try {
                        Arrays.sort(files, Comparator<File> { f1, f2 ->
                            if (f1 == null) throw RuntimeException("f1 is null inside sort")
                            if (f2 == null) throw RuntimeException("f2 is null inside sort")
                            try {
                                if (dirsFirst && f1.isDirectory != f2.isDirectory) {
                                    if (f1.isDirectory)
                                        return@Comparator -1
                                    else
                                        return@Comparator 1
                                }
                                return@Comparator f2.lastModified().compareTo(f1.lastModified())
                            } catch (e: NullPointerException) {
                                throw RuntimeException("failed to compare $f1 and $f2", e)
                            }
                        })
                    } catch (e: NullPointerException) {
                        throw RuntimeException(
                            "failed to sort file list " + files + " for path " + mCurrentPath,
                            e
                        )
                    }

                    for (file in files) {
                        entry = FileBean(FileBean.NORMAL, file, showExtension)
                        fileList.add(entry)
                    }
                }
                withContext(Dispatchers.Main) {
                    _uiFileModel.value = fileList
                }
            }
        }

    fun startGetProgress(fileList: List<FileBean>?, currentPath: String) {
        viewModelScope.launch {
            val args = withContext(Dispatchers.IO) {
                mScanner.startScan(fileList, progressDao)
                return@withContext arrayOf(currentPath, fileList)
            }

            withContext(Dispatchers.Main) {
                _uiScannerModel.value = args
            }
        }
    }

    fun loadFavorities(refresh: Boolean = false) {
        _uiFavoritiesModel.value = _uiFavoritiesModel.value.copy(State.LOADING)
        viewModelScope.launch {
            flow {
                val count = progressDao.getFavoriteProgressCount(1)
                var nKey = _uiFavoritiesModel.value.nextKey ?: 0
                if (refresh) {
                    nKey = 0
                }
                val progresses: List<BookProgress>? = progressDao.getFavoriteProgresses(
                    PAGE_SIZE * nKey,
                    PAGE_SIZE,
                    "1"
                )

                val entryList = ArrayList<FileBean>()

                var entry: FileBean
                var file: File
                val path = sdcardRoot
                progresses?.map {
                    file = File(path + "/" + it.path)
                    entry = FileBean(FileBean.FAVORITE, file, showExtension)
                    entry.bookProgress = it
                    entryList.add(entry)
                }

                var hasMore = false
                val oldSize = _uiFavoritiesModel.value.list?.size
                if (entryList.size > 0 && oldSize != null) {
                    if (count > (oldSize + entryList.size)) {
                        hasMore = true
                    }
                }
                if (hasMore) {
                    _uiFavoritiesModel.value.nextKey = nKey.plus(1)
                } else {
                    _uiFavoritiesModel.value.nextKey = null
                }
                Logcat.d("loadFavorities, nKey:$nKey,count:$count, hasMore:$hasMore .value:${_uiFavoritiesModel.value.nextKey}")
                val oldList = if (refresh) ArrayList() else _uiFavoritiesModel.value.list
                val nList = ArrayList(oldList)
                nList.addAll(entryList)
                emit(nList)
            }.catch { e ->
                Logcat.d("Exception:$e")
                emit(ArrayList<FileBean>())
            }.flowOn(Dispatchers.IO)
                .collect { list ->
                    _uiFavoritiesModel.value =
                        LoadResult(
                            State.FINISHED,
                            list = list,
                            prevKey = _uiFavoritiesModel.value.prevKey,
                            nextKey = _uiFavoritiesModel.value.nextKey
                        )
                }
        }
    }

    fun favorite(entry: FileBean, isFavorited: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val filepath = FileUtils.getStoragePath(entry.bookProgress!!.path)
                    val file = File(filepath)
                    var bookProgress = progressDao.getProgress(file.name)
                    if (null == bookProgress) {
                        if (isFavorited == 0) {
                            Logcat.w("", "some error:$entry")
                            return@withContext
                        }
                        bookProgress = BookProgress(FileUtils.getRealPath(file.absolutePath))
                        entry.bookProgress = bookProgress
                        entry.bookProgress!!.inRecent = BookProgress.NOT_IN_RECENT
                        entry.bookProgress!!.isFavorited = isFavorited
                        Logcat.d("add favorite entry:${entry.bookProgress}")
                        progressDao.addProgress(entry.bookProgress!!)
                    } else {
                        entry.bookProgress = bookProgress
                        entry.bookProgress!!.isFavorited = isFavorited
                        Logcat.d("update favorite entry:${entry.bookProgress}")
                        progressDao.updateProgress(entry.bookProgress!!)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            postFavoriteEvent(entry, isFavorited)
            //loadFavorities(true)
        }
    }

    private fun postFavoriteEvent(entry: FileBean, isFavorited: Int) {
        if (isFavorited == 1) {
            LiveEventBus
                .get<FileBean>(Event.ACTION_FAVORITED)
                .post(entry)
        } else {
            LiveEventBus
                .get<FileBean>(Event.ACTION_UNFAVORITED)
                .post(entry)
        }
    }

    fun updateItem(file: File, list: List<FileBean>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val progress = progressDao.getProgress(file.absolutePath)
                    if (null != progress) {
                        Logcat.d(BrowserFragment.TAG, "refresh entry:${progress}")
                        for (fb in list) {
                            if (null != fb.bookProgress && fb.bookProgress!!.name.equals(progress.name)) {
                                if (fb.bookProgress!!._id == 0) {
                                    fb.bookProgress = progress
                                    Logcat.d(
                                        BrowserFragment.TAG,
                                        "update new entry:${fb.bookProgress}"
                                    )
                                    progressDao.updateProgress(fb.bookProgress!!)
                                } else {
                                    fb.bookProgress!!.page = progress.page
                                    fb.bookProgress!!.isFavorited = progress.isFavorited
                                    fb.bookProgress!!.readTimes = progress.readTimes
                                    fb.bookProgress!!.pageCount = progress.pageCount
                                    fb.bookProgress!!.inRecent = progress.inRecent
                                    Logcat.d(
                                        BrowserFragment.TAG,
                                        "add new entry:${fb.bookProgress}"
                                    )
                                }
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            withContext(Dispatchers.Main) {
                if (_uiItemModel.value == null) {
                    _uiItemModel.value = true
                } else {
                    _uiItemModel.value = !_uiItemModel.value!!
                }
            }
        }
    }

    fun removeRecent(absolutePath: String) {
        val path = FileUtils.getName(absolutePath)
        progressDao.deleteProgress(path)
        viewModelScope.launch {
            val path = FileUtils.getName(absolutePath)
            withContext(Dispatchers.IO) {
                progressDao.deleteProgress(path)
            }

            var fileList: List<FileBean>? = _uiFileModel.value
            var fb: FileBean? = null
            _uiFileModel.value?.forEach {
                if (TextUtils.equals(it.label, path)) {
                    fb = it
                    return@forEach
                }
            }
            if (null == fileList) {
                fileList = ArrayList()
            }
            if (null != fb) {
                (fileList as ArrayList<FileBean>).remove(fb)
            }
            withContext(Dispatchers.Main) {
                _uiFileModel.value = fileList!!
            }
        }
    }
}