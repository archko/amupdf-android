package cn.archko.pdf.viewmodel

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.archko.mupdf.R
import cn.archko.pdf.common.ProgressScaner
import cn.archko.pdf.core.App
import cn.archko.pdf.core.common.BookProgressParser
import cn.archko.pdf.core.common.Event
import cn.archko.pdf.core.common.GlobalEvent
import cn.archko.pdf.core.common.Graph
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.BookProgress
import cn.archko.pdf.core.entity.FileBean
import cn.archko.pdf.core.entity.LoadResult
import cn.archko.pdf.core.entity.State
import cn.archko.pdf.core.utils.DateUtils
import cn.archko.pdf.core.utils.FileUtils
import cn.archko.pdf.core.utils.LengthUtils
import cn.archko.pdf.core.utils.StreamUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import vn.chungha.flowbus.busEvent
import java.io.File
import java.io.FileFilter
import java.util.Arrays
import java.util.Collections
import java.util.Locale
import java.util.Stack

/**
 * @author: archko 2021/4/11 :8:14 上午
 */
class FileViewModel() : ViewModel() {
    companion object {

        const val PAGE_SIZE = 20
        const val MAX_TIME = 1300L

        @JvmField
        val PREF_TAG = "ChooseFileActivity"

        @JvmField
        val PREF_HOME = "Home"
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

    private var sdcardRoot: String = Environment.getExternalStorageDirectory().getPath()
    private var homePath: String

    /**
     * 存储文件目录对应当前列表显示第一个元素的位置,这样在进入下一级目录再返回可以定位到上次列表的位置.
     */
    private var mPathMap: MutableMap<String, Int> = HashMap()

    fun getCurrentPos(): Int {
        return mPathMap[mCurrentPath] ?: -1
    }

    fun setCurrentPos(pos: Int) {
        Logcat.d("setCurrentPos:$mCurrentPath, pos:$pos")
        mCurrentPath?.let { mPathMap[mCurrentPath!!] = pos }
    }

    fun setCurrentPos(path: String, pos: Int) {
        Logcat.d("setCurrentPos.path:$path, pos:$pos")
        mCurrentPath?.let { mPathMap[path] = pos }
    }

    private val progressDao by lazy { Graph.database.progressDao() }

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
        if (file.isDirectory)
            return@FileFilter true
        val fname = file.name.lowercase(Locale.ROOT)

        return@FileFilter IntentFile.isText(fname)
                || IntentFile.isImage(fname)
                || IntentFile.isPdf(fname)
    }

    init {
        sdcardRoot = FileUtils.getStorageDirPath()
        Logcat.d("sdcardRoot:$sdcardRoot")

        homePath = initHomePath()

    }

    private fun initHomePath(): String {
        var path: String? =
            App.instance!!.getSharedPreferences(PREF_TAG, 0)!!
                .getString(PREF_HOME, null)
        if (null == path) {
            Toast.makeText(
                App.instance,
                App.instance!!.getString(R.string.toast_set_as_home),
                Toast.LENGTH_SHORT
            )
            path = sdcardRoot
        }
        if (path.length > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length - 2)
        }

        val pathFile = File(path)

        return if (pathFile.exists() && pathFile.isDirectory)
            path
        else
            sdcardRoot
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

    fun getCurrentItem(): FileBean? {
        return if (mCurrentPath == null) {
            null
        } else {
            FileBean(FileBean.HOME, mCurrentPath)
        }
    }

    fun getHomeItem(): FileBean {
        return FileBean(FileBean.HOME, homePath)
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
        if (_uiFileModel.value.state == State.LOADING) {
            return
        }
        _uiFileModel.value = _uiFileModel.value.copy(State.LOADING)
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

                entry = FileBean(FileBean.HOME, File(homePath), homePath)
                fileList.add(entry)
                if (mCurrentPath != "/") {
                    val upFolder = File(mCurrentPath!!).parentFile
                    entry = FileBean(FileBean.NORMAL, upFolder!!, "..")
                    fileList.add(entry)
                }
                val files = File(mCurrentPath).listFiles(fileFilter)
                if (files != null) {
                    Arrays.sort(files, fileComparator)

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

                val oldList = if (refresh) ArrayList() else _historyFileModel.value.list
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
                newTime = if (newTime < MAX_TIME) {
                    MAX_TIME - newTime
                } else {
                    0
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
                var flag: Boolean
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
                newTime = if (newTime < MAX_TIME) {
                    MAX_TIME - newTime
                } else {
                    0
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
        val edit = activity.getSharedPreferences(PREF_TAG, 0)?.edit()
        edit?.putString(PREF_HOME, mCurrentPath)
        edit?.apply()
    }

    fun loadBackupFiles() {
        viewModelScope.launch {
            flow {
                var files: Array<File>? = null
                val dir = FileUtils.getStorageDir("amupdf")
                if (dir.exists()) {
                    files = dir.listFiles { pathname: File -> pathname.name.startsWith("mupdf_") }
                    if (files != null) {
                        Arrays.sort(files) { f1: File?, f2: File? ->
                            if (f1 == null) throw RuntimeException("f1 is null inside sort")
                            if (f2 == null) throw RuntimeException("f2 is null inside sort")
                            return@sort f2.lastModified().compareTo(f1.lastModified())
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
                emit(ArrayList())
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
        //val map = HashMap<String, String>()
        //if (isFavorited == 1) {
        //    map["type"] = "addToFavorite"
        //} else {
        //    map["type"] = "removeFromFavorite"
        //}
        //map["name"] = entry.file!!.name
        //MobclickAgent.onEvent(context, AnalysticsHelper.A_MENU, map)
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
            if (isCurrentTab) {
            }
            loadFavorities(true)
        }
    }

    /*fun compress(
        context: Context,
        entry: FileBean,
    ) {
        Toast.makeText(App.instance, "开始压缩,请稍候", Toast.LENGTH_SHORT).show()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                //val map = HashMap<String, String>()
                //map["type"] = "compress"
                //map["name"] = entry.file!!.name
                //MobclickAgent.onEvent(context, AnalysticsHelper.A_MENU, map)
                val filepath = FileUtils.getStoragePath(entry.bookProgress!!.path)
                val file = File(filepath)

                val pdfDoc = com.radaee.pdf.Document()
                val fullPath: String = file.absolutePath
                val ret = pdfDoc.Open(fullPath, "")
                if (ret == 0) {
                    val path = fullPath.substring(0, fullPath.lastIndexOf("/"))
                    var name = fullPath.substring(
                        fullPath.lastIndexOf("/") + 1,
                        fullPath.lastIndexOf(".")
                    )
                    val finalPath = path + File.separatorChar + name + "_compressed.pdf"
                    PDFUtilities.CompressPDF(
                        finalPath,
                        object : OnOperationListener {
                            override fun onDone(result: Any?, requestCode: Int) {
                                AppExecutors.instance.mainThread().execute {
                                    Toast.makeText(App.instance, "压缩成功", Toast.LENGTH_SHORT)
                                        .show()
                                }

                                viewModelScope.launch {
                                    if (preferencesRepository.pdfOptionFlow.first().overrideFile) {
                                        val newFile = File(finalPath)
                                        if (newFile.length() > 0) {
                                            file.delete()
                                            newFile.renameTo(file)
                                        } else {
                                            Logcat.e("", "compress file is null:$finalPath")
                                        }
                                    }
                                }
                                loadFiles(null, true)
                            }

                            override fun onError(error: String?, requestCode: Int) {
                                Logcat.e("", "compress error:$error")
                                AppExecutors.instance.mainThread().execute {
                                    Toast.makeText(App.instance, "压缩失败", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        },
                        pdfDoc
                    )
                }
            }
        }
    }*/

    fun convertToPDF(
        context: Context,
        entry: FileBean,
    ) {
        //Toast.makeText(App.instance, "开始生成,请稍候", Toast.LENGTH_SHORT).show()
        /*viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                //val map = HashMap<String, String>()
                //map["type"] = "convert image"
                //map["name"] = entry.file!!.name
                //MobclickAgent.onEvent(context, AnalysticsHelper.A_MENU, map)
                val fullPath = entry.file!!.absolutePath

                val path = fullPath.substring(0, fullPath.lastIndexOf("/"))
                var name = fullPath.substring(
                    fullPath.lastIndexOf("/") + 1,
                    fullPath.lastIndexOf(".")
                )
                val finalPath = path + File.separatorChar + name + ".pdf"
                val images = arrayListOf<String>()
                images.add(fullPath)
                PDFCreaterHelper.createPdf(finalPath, images)
            }
            if (result) {
                Toast.makeText(App.instance, R.string.create_pdf_success, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(App.instance, R.string.create_pdf_error, Toast.LENGTH_SHORT).show()
            }
        }*/
    }

    private fun postFavoriteEvent(entry: FileBean, isFavorited: Int) {
        if (isFavorited == 1) {
            busEvent(GlobalEvent(Event.ACTION_FAVORITED, entry))
        } else {
            busEvent(GlobalEvent(Event.ACTION_UNFAVORITED, entry))
        }
    }

    // ============================ search ==========================

    private fun doSearchFile(fileList: ArrayList<FileBean>, keyword: String, dir: File) {
        if (dir.isDirectory) {
            val files = dir.listFiles(this.fileFilter)

            if (files != null && files.size > 0) {
                for (f in files) {
                    if (f.isFile) {
                        if (f.name.lowercase(Locale.getDefault()).contains(keyword)) {
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
        val progresses = progressDao.searchHistory(keyword)
        val fileBeans = ArrayList<FileBean>()
        progresses?.run {
            for (progress in progresses) {
                val fileBean = FileBean(progress, FileBean.NORMAL)
                fileBeans.add(fileBean)
            }
        }
        return fileBeans
    }

    private fun doSearchFavorite(keyword: String): ArrayList<FileBean> {
        val progresses = progressDao.searchFavorite(keyword)
        val fileBeans = ArrayList<FileBean>()
        progresses?.run {
            for (progress in progresses) {
                val fileBean = FileBean(progress, FileBean.NORMAL)
                fileBeans.add(fileBean)
            }
        }
        return fileBeans
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
                delay(250L)
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