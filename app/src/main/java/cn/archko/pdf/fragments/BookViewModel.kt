package cn.archko.pdf.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.archko.pdf.common.Event
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.ProgressScaner
import cn.archko.pdf.common.RecentManager
import cn.archko.pdf.entity.BookProgress
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.utils.FileUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileFilter
import java.util.*
import kotlin.Comparator

/**
 * @author: archko 2020/11/16 :11:23
 */
class BookViewModel : ViewModel() {

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

    private val fileFilter: FileFilter = FileFilter { file ->
        //return (file.isDirectory() || file.getName().toLowerCase().endsWith(".pdf"));
        if (file.isDirectory)
            return@FileFilter true
        val fname = file.name.toLowerCase(Locale.ROOT)

        if (fname.endsWith(".pdf"))
            return@FileFilter true
        if (fname.endsWith(".xps"))
            return@FileFilter true
        if (fname.endsWith(".cbz"))
            return@FileFilter true
        if (fname.endsWith(".png"))
            return@FileFilter true
        if (fname.endsWith(".jpe"))
            return@FileFilter true
        if (fname.endsWith(".jpeg"))
            return@FileFilter true
        if (fname.endsWith(".jpg"))
            return@FileFilter true
        if (fname.endsWith(".jfif"))
            return@FileFilter true
        if (fname.endsWith(".jfif-tbnl"))
            return@FileFilter true
        if (fname.endsWith(".tif"))
            return@FileFilter true
        if (fname.endsWith(".tiff"))
            return@FileFilter true
        if (fname.endsWith(".epub"))
            return@FileFilter true
        if (fname.endsWith(".txt"))
            return@FileFilter true
        false
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

    fun startGetProgress(fileList: List<FileBean>, currentPath: String) {
        viewModelScope.launch {
            val args = withContext(Dispatchers.IO) {
                return@withContext mScanner.startScan(fileList, currentPath)
            }

            withContext(Dispatchers.Main) {
                _uiScannerModel.value = args
            }
        }
    }

    fun favorite(entry: FileBean, isFavorited: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val recentManager = RecentManager.instance.recentTableManager
                    val filepath = FileUtils.getStoragePath(entry.bookProgress!!.path)
                    val file = File(filepath)
                    var bookProgress = recentManager.getProgress(file.name, BookProgress.ALL)
                    if (null == bookProgress) {
                        if (isFavorited == 0) {
                            Logcat.w(BrowserFragment.TAG, "some error:$entry")
                            return@withContext
                        }
                        bookProgress = BookProgress(FileUtils.getRealPath(file.absolutePath))
                        entry.bookProgress = bookProgress
                        entry.bookProgress!!.inRecent = BookProgress.NOT_IN_RECENT
                        entry.bookProgress!!.isFavorited = isFavorited
                        Logcat.d(BrowserFragment.TAG, "add favorite entry:${entry.bookProgress}")
                        recentManager.addProgress(entry.bookProgress!!)
                    } else {
                        entry.bookProgress = bookProgress
                        entry.bookProgress!!.isFavorited = isFavorited
                        Logcat.d(BrowserFragment.TAG, "update favorite entry:${entry.bookProgress}")
                        recentManager.updateProgress(entry.bookProgress!!)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            postFavoriteEvent(entry, isFavorited)
        }
    }

    private fun postFavoriteEvent(entry: FileBean, isFavorited: Int) {
        if (isFavorited == 1) {
            LiveEventBus
                .get(Event.ACTION_FAVORITED)
                .post(entry)
        } else {
            LiveEventBus
                .get(Event.ACTION_UNFAVORITED)
                .post(entry)
        }
    }

    fun updateItem(file: File, list: List<FileBean>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val recentManager = RecentManager.instance
                    val progress =
                        recentManager.readRecentFromDb(file.absolutePath, BookProgress.ALL);
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
                                    recentManager.recentTableManager.updateProgress(fb.bookProgress!!)
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
}