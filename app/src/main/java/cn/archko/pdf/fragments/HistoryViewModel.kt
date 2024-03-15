package cn.archko.pdf.fragments

import android.os.Environment
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

    private val progressDao by lazy { Graph.database.progressDao() }

    private val _uiFileModel = MutableLiveData<Array<Any?>>()
    val uiFileModel: LiveData<Array<Any?>>
        get() = _uiFileModel

    private val _uiBackupModel = MutableLiveData<String?>()
    val uiBackupModel: LiveData<String?>
        get() = _uiBackupModel

    private val _uiRestorepModel = MutableLiveData<Boolean>()
    val uiRestorepModel: LiveData<Boolean>
        get() = _uiRestorepModel

    fun loadFiles(curPage: Int, showExtension: Boolean) =
        viewModelScope.launch {
            val args = withContext(Dispatchers.IO) {
                var totalCount = 0

                totalCount = progressDao.progressCount()
                val progresses = progressDao.getProgresses(
                    PAGE_SIZE * (curPage),
                    PAGE_SIZE
                )

                val entryList = ArrayList<FileBean>()

                var entry: FileBean
                var file: File
                val path = Environment.getExternalStorageDirectory().path
                progresses?.map {
                    file = File(path + "/" + it.path)
                    entry = FileBean(FileBean.RECENT, file, showExtension)
                    entry.bookProgress = it
                    entryList.add(entry)
                }
                return@withContext arrayOf<Any?>(totalCount, entryList)
            }

            withContext(Dispatchers.Main) {
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
                if (newTime < MAX_TIME) {
                    newTime = MAX_TIME - newTime
                } else {
                    newTime = 0
                }

                delay(newTime)
                return@withContext flag
            }
            withContext(Dispatchers.Main) {
                _uiRestorepModel.value = flag
            }
        }
    }
}