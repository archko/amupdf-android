package cn.archko.pdf.fragments

import android.os.Environment
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.archko.pdf.common.BookProgressParser
import cn.archko.pdf.common.Graph
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.entity.FileBean
import cn.archko.pdf.utils.DateUtils
import cn.archko.pdf.utils.FileUtils
import cn.archko.pdf.utils.StreamUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File

/**
 * @author: archko 2020/11/16 :11:23
 */
class HistoryViewModel : ViewModel() {
    companion object {

        const val PAGE_SIZE = 20
        const val MAX_TIME = 1300L
    }

    var curPage = 0
    var list: MutableList<FileBean> = mutableListOf()

    private val progressDao by lazy { Graph.database.progressDao() }

    private val _uiFileModel = MutableLiveData<Array<Any>>()
    val uiFileModel: LiveData<Array<Any>>
        get() = _uiFileModel

    private val _uiBackupModel = MutableLiveData<String?>()
    val uiBackupModel: LiveData<String?>
        get() = _uiBackupModel

    private val _uiRestorepModel = MutableLiveData<Boolean>()
    val uiRestorepModel: LiveData<Boolean>
        get() = _uiRestorepModel

    fun reset() {
        curPage = 0
    }

    fun loadFiles(page: Int, showExtension: Boolean) =
        viewModelScope.launch {
            val args = withContext(Dispatchers.IO) {
                val totalCount: Int = progressDao.progressCount()
                val progresses = progressDao.getProgresses(
                    PAGE_SIZE * (page),
                    PAGE_SIZE
                )

                Logcat.d("loadFiles:$page, $curPage, total:$totalCount, ${progresses?.size}")

                val entryList = ArrayList<FileBean>()

                var entry: FileBean
                val path = Environment.getExternalStorageDirectory().path
                progresses?.map {
                    entry = FileBean(it, FileBean.RECENT, path + "/" + it.path)
                    entryList.add(entry)
                }

                val nList = arrayListOf<FileBean>()
                if (page > 0) {
                    nList.addAll(list)
                }
                if ((progresses?.size ?: 0) > 0) {
                    curPage++
                }
                nList.addAll(entryList)
                return@withContext arrayOf<Any>(totalCount, nList)
            }

            withContext(Dispatchers.Main) {
                list = args[1] as MutableList<FileBean>
                _uiFileModel.value = args
            }
        }

    fun backupFromDb() {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                val name = "mupdf_" + DateUtils.formatTime(
                    System.currentTimeMillis(),
                    "yyyy-MM-dd-HH-mm-ss"
                )
                var filepath: String? = null

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
                        filepath = dir.absolutePath + File.separator + name
                        Logcat.d("backup.name:$filepath root:$root")
                        StreamUtils.copyStringToFile(root.toString(), filepath)
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
                return@withContext filepath
            }

            withContext(Dispatchers.Main) {
                _uiBackupModel.value = path
            }
        }
    }

    fun restoreToDb(file: File) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val flag = withContext(Dispatchers.IO) {
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

                var newTime = System.currentTimeMillis() - now
                if (newTime < MAX_TIME) {
                    newTime = MAX_TIME - newTime
                } else {
                    newTime = 0
                }

                delay(newTime)
                return@withContext true
            }
            withContext(Dispatchers.Main) {
                _uiRestorepModel.value = flag
            }
        }
    }

    fun removeRecent(absolutePath: String) {
        viewModelScope.launch {
            val args = withContext(Dispatchers.IO) {
                val path = FileUtils.getName(absolutePath)
                val count = progressDao.deleteProgress(path)
                if (count < 1) { //maybe path is absolutepath,not /book/xx.pdf
                    progressDao.deleteProgress(absolutePath)
                }

                var fb: FileBean? = null
                list.forEach {
                    if (null != it.file && TextUtils.equals(it.file!!.absolutePath, absolutePath)) {
                        fb = it
                        return@forEach
                    }
                }
                fb?.let {
                    list.remove(fb)
                }

                return@withContext arrayOf<Any>(list.size + 1, list)
            }
            withContext(Dispatchers.Main) {
                _uiFileModel.value = args
            }
        }
    }
}