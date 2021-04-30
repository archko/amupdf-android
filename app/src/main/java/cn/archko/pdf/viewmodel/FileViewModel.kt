package cn.archko.pdf.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.archko.mupdf.R
import cn.archko.pdf.App
import cn.archko.pdf.activities.ChooseFileFragmentActivity
import cn.archko.pdf.common.AnalysticsHelper
import cn.archko.pdf.common.BookProgressParser
import cn.archko.pdf.common.Event
import cn.archko.pdf.common.Graph
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.ProgressScaner
import cn.archko.pdf.entity.BookProgress
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.model.SearchSuggestionGroup
import cn.archko.pdf.paging.LoadResult
import cn.archko.pdf.paging.State
import cn.archko.pdf.utils.DateUtils
import cn.archko.pdf.ui.home.searchTypeFavorite
import cn.archko.pdf.ui.home.searchTypeFile
import cn.archko.pdf.ui.home.searchTypeHistory
import cn.archko.pdf.utils.FileUtils
import cn.archko.pdf.utils.LengthUtils
import cn.archko.pdf.utils.StreamUtils
import com.jeremyliao.liveeventbus.LiveEventBus
import com.umeng.analytics.MobclickAgent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileFilter
import java.util.*
import kotlin.collections.ArrayList

/**
 * @author: archko 2021/4/11 :8:14 上午
 */
class FileViewModel() : ViewModel() {
    companion object {

        const val PAGE_SIZE = 20
        const val MAX_TIME = 1300L
    }

    private val _uiFileModel = MutableStateFlow<LoadResult<Any, FileBean>>(LoadResult(State.INIT))
    val uiFileModel: StateFlow<LoadResult<Any, FileBean>>
        get() = _uiFileModel

    private var _historyFileModel =
        MutableStateFlow<LoadResult<Any, FileBean>>(LoadResult(State.INIT))
    val historyFileModel: StateFlow<LoadResult<Any, FileBean>>
        get() = _historyFileModel

    private val _uiFavoritiesModel =
        MutableStateFlow<LoadResult<Any, FileBean>>(LoadResult(State.INIT))
    val uiFavoritiesModel: StateFlow<LoadResult<Any, FileBean>>
        get() = _uiFavoritiesModel

    private val _uiBackupModel = MutableStateFlow<LoadResult<Any, Any>>(LoadResult(State.INIT))
    val uiBackupModel: StateFlow<LoadResult<Any, Any>>
        get() = _uiBackupModel

    private val _uiBackupFileModel = MutableStateFlow<LoadResult<Any, File>>(LoadResult(State.INIT))
    val uiBackupFileModel: StateFlow<LoadResult<Any, File>>
        get() = _uiBackupFileModel

    private var mScanner: ProgressScaner = ProgressScaner()
    private var dirsFirst: Boolean = true
    private var showExtension: Boolean = true

    var sdcardRoot: String = "/sdcard/"
    var homePath: String
    var selectionIndex = 0
    val progressDao by lazy { Graph.database.progressDao() }

    private val fileComparator = Comparator<File> { f1, f2 ->
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
    }

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

    init {
        sdcardRoot = FileUtils.getStorageDirPath()
        Logcat.d("sdcardRoot:$sdcardRoot")

        homePath = initHomePath()

    }

    private fun initHomePath(): String {
        var path: String? =
            App.instance!!.getSharedPreferences(ChooseFileFragmentActivity.PREF_TAG, 0)!!
                .getString(ChooseFileFragmentActivity.PREF_HOME, null)
        if (null == path) {
            Toast.makeText(
                App.instance,
                App.instance!!.getString(R.string.toast_set_as_home),
                Toast.LENGTH_SHORT
            )
            path = sdcardRoot
        }
        if (path!!.length > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length - 2)
        }

        val pathFile = File(path)

        if (pathFile.exists() && pathFile.isDirectory)
            return path
        else
            return sdcardRoot
    }

    fun isHome(path: String): Boolean {
        return path == homePath
    }

    var mCurrentPath: String? = null

    var stack: Stack<String> = Stack<String>()
    fun isTop(): Boolean {
        if (stack.isEmpty()) {
            return true
        }
        val top = stack.peek()
        return top == sdcardRoot
    }

    fun loadFiles(
        currentPath: String?,
        refresh: Boolean = false
    ) {
        val path = currentPath ?: homePath
        val f = File(path)
        if (!f.exists() || !f.isDirectory) {
            return
        }
        if (!refresh) {
            mCurrentPath = path
            if (!stack.contains(mCurrentPath)) {
                stack.push(mCurrentPath)
            }
        }
        Logcat.d("loadFiles, path:$mCurrentPath")
        _uiFileModel.value = _uiFileModel.value.copy(State.LOADING)
        viewModelScope.launch {
            flow {
                val fileList: ArrayList<FileBean> = ArrayList()
                var entry: FileBean

                entry = FileBean(FileBean.HOME, homePath)
                fileList.add(entry)
                if (mCurrentPath != "/") {
                    val upFolder = File(mCurrentPath!!).parentFile
                    entry = FileBean(FileBean.NORMAL, upFolder!!, "..")
                    fileList.add(entry)
                }
                Logcat.d("loadFiles, path:$mCurrentPath")
                val files = File(mCurrentPath).listFiles(fileFilter)
                if (files != null) {
                    try {
                        Arrays.sort(files, fileComparator)
                    } catch (e: NullPointerException) {
                        throw RuntimeException(
                            "failed to sort file list $files for path $mCurrentPath",
                            e
                        )
                    }

                    for (file in files) {
                        entry = FileBean(FileBean.NORMAL, file, showExtension)
                        fileList.add(entry)
                    }
                    mScanner.startScan(fileList, progressDao)
                }
                emit(fileList)
            }.catch { e ->
                Logcat.d("Exception:$e")
                emit(ArrayList())
            }.flowOn(Dispatchers.IO)
                .collect { list ->
                    _uiFileModel.value =
                        LoadResult(
                            State.FINISHED,
                            list = list,
                            prevKey = null,
                            nextKey = null
                        )
                }
        }
    }

    fun loadHistories(refresh: Boolean = false) {
        _historyFileModel.value = _historyFileModel.value.copy(State.LOADING)
        viewModelScope.launch {
            flow {
                val count = progressDao.progressCount()
                var nKey = _historyFileModel.value.nextKey ?: 0
                if (refresh) {
                    nKey = 0
                }
                val progresses: List<BookProgress>? = progressDao.getProgresses(
                    PAGE_SIZE * nKey,
                    PAGE_SIZE,
                    BookProgress.IN_RECENT
                )

                val entryList = ArrayList<FileBean>()

                var entry: FileBean
                var file: File
                val path = sdcardRoot
                progresses?.map {
                    file = File(path + "/" + it.path)
                    entry = FileBean(FileBean.RECENT, file, true)
                    entry.bookProgress = it
                    entryList.add(entry)
                }

                var hasMore = false
                val oldSize = _historyFileModel.value.list?.size
                if (entryList.size > 0 && oldSize != null) {
                    if (count > (oldSize + entryList.size)) {
                        hasMore = true
                    }
                }
                if (hasMore) {
                    _historyFileModel.value.nextKey = nKey.plus(1)
                } else {
                    _historyFileModel.value.nextKey = null
                }
                Logcat.d("loadHistories, nKey:$nKey,count:$count, hasMore:$hasMore .value:${_historyFileModel.value.nextKey}")

                val oldList = if (refresh) ArrayList<FileBean>() else _historyFileModel.value.list
                val nList = ArrayList(oldList)
                nList.addAll(entryList)
                emit(nList)
            }.catch { e ->
                Logcat.d("Exception:$e")
                emit(ArrayList())
            }.flowOn(Dispatchers.IO)
                .collect { list ->
                    _historyFileModel.value = _historyFileModel.value.copy(
                        State.FINISHED,
                        list = list,
                    )
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
                val oldList = if (refresh) ArrayList<FileBean>() else _uiFavoritiesModel.value.list
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

    fun deleteFile(fb: FileBean) {
        if (fb.type == FileBean.NORMAL && !fb.isDirectory) {
            fb.file?.delete()
            loadFiles(null, true)
        }
    }

    fun deleteHistory(file: File) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val bookProgress = progressDao.getProgress(file.name)
                Logcat.d("old:$bookProgress")

                bookProgress?.run {
                    firstTimestampe = 0
                    page = 0
                    progress = 0
                    inRecent = -1
                }
                if (bookProgress != null) {
                    progressDao.updateProgress(bookProgress)
                    Logcat.d("new:$bookProgress")
                }
            }
            loadHistories(true)
        }
    }

    fun backupFromDb() {
        _uiBackupModel.value = _uiBackupModel.value.copy(State.LOADING)
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            flow {
                val name =
                    "mupdf_" + DateUtils.formatTime(
                        System.currentTimeMillis(),
                        "yyyy-MM-dd-HH-mm-ss"
                    )
                var filePath: String? = null

                try {
                    val list = progressDao.getAllProgress()
                    val root = JSONObject()
                    val ja = JSONArray()
                    root.put("root", ja)
                    root.put("name", name)
                    list?.run {
                        for (progress in list) {
                            BookProgressParser.addProgressToJson(progress, ja)
                        }
                    }
                    val dir = FileUtils.getStorageDir("amupdf")
                    if (dir != null && dir.exists()) {
                        filePath = dir.absolutePath + File.separator + name
                        Logcat.d("backup.name:$filePath root:$root")
                        StreamUtils.copyStringToFile(root.toString(), filePath)
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                var newTime = System.currentTimeMillis() - now
                if (newTime < MAX_TIME) {
                    newTime = MAX_TIME - newTime
                } else {
                    newTime = 0
                }

                delay(newTime)
                emit(filePath)
            }.catch { e ->
                Logcat.d("restoreToDb error:$e")
                emit(null)
            }.flowOn(Dispatchers.IO)
                .collect { filepath ->
                    if (!LengthUtils.isEmpty(filepath)) {
                        Logcat.d("", "file:$filepath")
                        Toast.makeText(App.instance, "备份成功:$filepath", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(App.instance, "备份失败", Toast.LENGTH_LONG).show()
                    }

                    _uiBackupModel.value = LoadResult(State.FINISHED)
                }
        }
    }

    fun restoreToDb(file: File) {
        _uiBackupModel.value = _uiBackupModel.value.copy(State.LOADING)
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            flow {
                var flag = false
                try {
                    val content = StreamUtils.readStringFromFile(file)
                    Logcat.longLog(
                        Logcat.TAG,
                        "restore.file:" + file.absolutePath + " content:" + content
                    )
                    val progresses = BookProgressParser.parseProgresses(content)
                    Graph.database.runInTransaction {
                        Graph.database.progressDao().deleteAllProgress()
                        Graph.database.progressDao().addProgresses(progresses)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                flag = true

                var newTime = System.currentTimeMillis() - now
                if (newTime < MAX_TIME) {
                    newTime = MAX_TIME - newTime
                } else {
                    newTime = 0
                }

                delay(newTime)
                emit(flag)
            }.catch { e ->
                Logcat.d("restoreToDb error:$e")
                emit(false)
            }.flowOn(Dispatchers.IO)
                .collect { flag ->
                    if (flag) {
                        Toast.makeText(App.instance, "恢复成功:$flag", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(App.instance, "恢复失败", Toast.LENGTH_LONG).show()
                    }
                    _uiBackupModel.value = LoadResult(State.FINISHED)

                    loadHistories(true)
                }
        }
    }

    fun setAsHome(activity: Context) {
        val edit = activity.getSharedPreferences(ChooseFileFragmentActivity.PREF_TAG, 0)?.edit()
        edit?.putString(ChooseFileFragmentActivity.PREF_HOME, mCurrentPath)
        edit?.apply()
    }

    fun loadBackupFiles() {
        viewModelScope.launch {
            flow {
                var files: Array<File>? = null
                val dir = FileUtils.getStorageDir("amupdf")
                if (dir.exists()) {
                    files = dir.listFiles { pathname: File -> pathname.name.startsWith("mupdf_") }
                    Arrays.sort(files) { f1: File?, f2: File? ->
                        if (f1 == null) throw RuntimeException("f1 is null inside sort")
                        if (f2 == null) throw RuntimeException("f2 is null inside sort")
                        try {
                            return@sort f2.lastModified().compareTo(f1.lastModified())
                        } catch (e: NullPointerException) {
                            throw RuntimeException("failed to compare $f1 and $f2", e)
                        }
                    }
                }
                val list = ArrayList<File>()
                if (files != null) {
                    for (f in files) {
                        list.add(f)
                    }
                }
                emit(list)
            }.catch { e ->
                Logcat.d("backupFiles error:$e")
                emit(ArrayList<File>())
            }.flowOn(Dispatchers.IO)
                .collect { list ->
                    _uiBackupFileModel.value =
                        LoadResult(
                            State.FINISHED,
                            list = list,
                            prevKey = null,
                            nextKey = null
                        )
                }
        }
    }

    fun favorite(
        context: Context,
        entry: FileBean,
        isFavorited: Int,
        isCurrentTab: Boolean = false
    ) {
        val map = HashMap<String, String>()
        if (isFavorited == 1) {
            map["type"] = "addToFavorite"
        } else {
            map["type"] = "removeFromFavorite"
        }
        map["name"] = entry.file!!.name
        MobclickAgent.onEvent(context, AnalysticsHelper.A_MENU, map)
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

            //postFavoriteEvent(entry, isFavorited)
            if (isCurrentTab) {
            }
            loadFavorities(true)
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

    // ============================ search ==========================

    fun getSuggestions(): List<SearchSuggestionGroup> = searchSuggestions

    private val searchSuggestions = listOf(
        SearchSuggestionGroup(
            id = 0L,
            name = "Recent searches",
            suggestions = listOf(
                "",
            )
        )
    )

    suspend fun search(query: String, searchType: Int): MutableList<FileBean> =
        withContext(Dispatchers.Default) {
            Logcat.d("search:$searchType,query:$query")
            var fileList = ArrayList<FileBean>()
            when (searchType) {
                searchTypeFile -> {
                    doSearchFile(fileList, query, File(homePath))
                }
                searchTypeHistory -> {
                    fileList = doSearchHistory(query)
                }
                searchTypeFavorite -> {
                    fileList = doSearchFavorite(query)
                }
            }
            fileList
        }

    private fun doSearchFile(fileList: ArrayList<FileBean>, keyword: String, dir: File) {
        if (dir.isDirectory) {
            val files = dir.listFiles(this.fileFilter)

            if (files != null && files.size > 0) {
                for (f in files) {
                    if (f.isFile) {
                        if (f.name.toLowerCase().contains(keyword)) {
                            fileList.add(FileBean(FileBean.NORMAL, f, true))
                        }
                    } else {
                        doSearchFile(fileList, keyword, f)
                    }
                }
            }
        } else {
            if (dir.name.contains(keyword)) {
                fileList.add(FileBean(FileBean.NORMAL, dir, true))
            }
        }
    }

    private fun doSearchHistory(keyword: String): ArrayList<FileBean> {
        return progressDao.searchHistory(keyword)
    }

    private fun doSearchFavorite(keyword: String): ArrayList<FileBean> {
        return progressDao.searchFavorite(keyword)
    }

    /**
     * currentSection 1->history, 2->favorite
     */
    fun onReadBook(path: String?, currentSection: Int) {
        if (path == null) {
            return
        }
        if (currentSection == 1) {
            val result = _historyFileModel.value
            var findBean: FileBean? = null

            if (result.list != null) {
                for (fb in result.list!!) {
                    if (fb.file?.path.equals(path)) {
                        findBean = fb
                        break
                    }
                }
            }

            Logcat.d("onReadBook:currentSection:$currentSection,path:$path $findBean")
            if (findBean != null) {
                updateHistory(findBean, path)
            } else {
                updateHistory(null, path)
            }
        } else if (currentSection == 2) {
            loadFiles(null, refresh = true)
        } else if (currentSection == 3) {
            loadFavorities(true)
        }
    }

    private val beanComparator = Comparator<FileBean> { f1, f2 ->
        if (f1 == null || f2 == null) return@Comparator 0

        return@Comparator f1.bookProgress?.lastTimestampe?.let {
            f2.bookProgress?.lastTimestampe?.compareTo(
                it
            )
        }!!
    }

    private fun updateHistory(findBean: FileBean?, path: String) {
        _historyFileModel.value = _historyFileModel.value.copy(
            State.LOADING,
        )
        viewModelScope.launch {
            flow {
                val nList = ArrayList<FileBean>(_historyFileModel.value.list)
                val file = File(path)
                if (null == findBean) {
                    val fb = FileBean(FileBean.RECENT, file, true)
                    fb.bookProgress = progressDao.getProgress(file.name, BookProgress.IN_RECENT)
                    nList.add(0, fb)
                    Logcat.d("onReadBook insert:$fb")
                } else {
                    findBean.bookProgress = progressDao.getProgress(file.name)
                    Logcat.d("onReadBook update:$findBean")
                }


                Collections.sort(nList, beanComparator)
                emit(nList)
            }.catch { e ->
                Logcat.d("Exception:$e")
                emit(ArrayList())
            }.flowOn(Dispatchers.IO)
                .collect { list ->
                    _historyFileModel.value = _historyFileModel.value.copy(
                        State.FINISHED,
                        list = list,
                    )
                }
        }
    }
}