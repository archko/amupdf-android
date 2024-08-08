package cn.archko.pdf.fragments

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.archko.pdf.core.App
import cn.archko.pdf.core.common.BookProgressParser
import cn.archko.pdf.core.common.Graph
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.utils.FileUtils
import cn.archko.pdf.utils.SardineHelper
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.Arrays

/**
 * @author: archko 2020/11/16 :11:23
 */
class BackupViewModel : ViewModel() {

    private val progressDao by lazy { Graph.database.progressDao() }
    private val _uiFileModel = MutableLiveData<List<File>>()
    val uiFileModel: LiveData<List<File>>
        get() = _uiFileModel

    private val _uiDavResourceModel = MutableLiveData<List<DavResource>?>()
    val uiDavResourceModel: LiveData<List<DavResource>?>
        get() = _uiDavResourceModel

    private val _uiRestorepModel = MutableLiveData<Boolean>()
    val uiRestoreModel: LiveData<Boolean>
        get() = _uiRestorepModel

    fun backupFiles() =
        viewModelScope.launch {
            val files: List<File> = withContext(Dispatchers.IO) {
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
                return@withContext list
            }

            withContext(Dispatchers.Main) {
                _uiFileModel.value = files
            }
        }

    fun backupToWebdav(username: String, password: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val root = JSONObject()
                try {
                    val list = progressDao.getAllProgress()
                    val ja = JSONArray()
                    root.put("root", ja)
                    //root.put("name", name)
                    list?.run {
                        for (progress in list) {
                            BookProgressParser.addProgressToJson(progress, ja)
                        }
                    }
                    val sardine = OkHttpSardine()
                    sardine.setCredentials(username, password)
                    val content = root.toString()
                    Logcat.d("backupToWebdav.content:$content")

                    SardineHelper.uploadFile(
                        sardine,
                        content.toByteArray(),
                        SardineHelper.DEFAULT_JSON
                    )
                    return@withContext true
                } catch (e: JSONException) {
                    Logcat.e(Logcat.TAG, e.message)
                } catch (e: Exception) {
                    Logcat.e(Logcat.TAG, e.message)
                }

                return@withContext false
            }

            withContext(Dispatchers.Main) {
                if (result) {
                    Toast.makeText(App.instance, "Success", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(App.instance, "Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun restoreFromWebdav(username: String, password: String, name: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val sardine = OkHttpSardine()
                    sardine.setCredentials(username, password)
                    val content = SardineHelper.downloadFile(sardine, name)
                    return@withContext restore(content)
                } catch (e: JSONException) {
                    e.printStackTrace()
                } catch (e: Exception) {
                    Logcat.e(Logcat.TAG, e.message)
                }

                return@withContext false
            }

            withContext(Dispatchers.Main) {
                _uiRestorepModel.value = result
            }
        }
    }

    fun webdavBackupFiles(username: String, password: String, name: String) =
        viewModelScope.launch {
            val resources: List<DavResource>? = withContext(Dispatchers.IO) {
                var list: MutableList<DavResource>? = null
                try {
                    val sardine = OkHttpSardine()
                    sardine.setCredentials(username, password)
                    list = SardineHelper.listFiles(sardine, name)
                } catch (e: Exception) {
                    Logcat.e(Logcat.TAG, e.message)
                }

                return@withContext list
            }

            withContext(Dispatchers.Main) {
                _uiDavResourceModel.value = resources
            }
        }

    fun testAuth(username: String?, password: String?) {
        viewModelScope.launch {
            val result: Boolean = withContext(Dispatchers.IO) {
                try {
                    val sardine = OkHttpSardine()
                    sardine.setCredentials(username, password)
                    val result = SardineHelper.testAuth(sardine)
                    Logcat.d("result:$result")
                    return@withContext result
                } catch (e: Exception) {
                    Logcat.e(Logcat.TAG, e.message)
                }
                return@withContext false
            }

            withContext(Dispatchers.Main) {
                if (result) {
                    Toast.makeText(App.instance, "Success", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(App.instance, "Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        fun restore(content: String): Boolean {
            try {
                val progresses = BookProgressParser.parseProgresses(content)
                Graph.database.runInTransaction {
                    Graph.database.progressDao().deleteAllProgress()
                    Graph.database.progressDao().addProgresses(progresses)
                }
                return true
            } catch (e: Exception) {
                Logcat.e(Logcat.TAG, e.message)
            }
            return false
        }
    }
}